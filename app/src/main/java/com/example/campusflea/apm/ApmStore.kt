package com.example.campusflea.apm

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * 待上报队列。
 *
 * 按类型分桶而不是揉成一个列表：上报时崩溃和启动数据必须优先送出，
 * 它们量小但价值最高；网络与帧率数据量大，被截断也无所谓。
 */
internal data class ApmPending(
    var network: MutableList<ApmNetworkRecord> = mutableListOf(),
    var crashes: MutableList<ApmCrashRecord> = mutableListOf(),
    var startups: MutableList<ApmStartupRecord> = mutableListOf(),
    var frames: MutableList<ApmFrameRecord> = mutableListOf(),
    var scenes: MutableList<ApmSceneRecord> = mutableListOf()
) {
    val size: Int get() = network.size + crashes.size + startups.size + frames.size + scenes.size

    val isEmpty: Boolean get() = size == 0

    /** 按优先级取出一批数据，取走的条目从队列头部移除。 */
    fun takeBatch(max: Int): ApmPending {
        val out = ApmPending()
        var budget = max
        while (budget > 0 && crashes.isNotEmpty()) { out.crashes.add(crashes.removeAt(0)); budget-- }
        while (budget > 0 && startups.isNotEmpty()) { out.startups.add(startups.removeAt(0)); budget-- }
        while (budget > 0 && network.isNotEmpty()) { out.network.add(network.removeAt(0)); budget-- }
        while (budget > 0 && frames.isNotEmpty()) { out.frames.add(frames.removeAt(0)); budget-- }
        while (budget > 0 && scenes.isNotEmpty()) { out.scenes.add(scenes.removeAt(0)); budget-- }
        return out
    }

    /** 上报失败时把整批数据插回队列头部，保持原有先后顺序，下次重试。 */
    fun mergeFront(other: ApmPending) {
        network.addAll(0, other.network)
        crashes.addAll(0, other.crashes)
        startups.addAll(0, other.startups)
        frames.addAll(0, other.frames)
        scenes.addAll(0, other.scenes)
    }

    /** 超出缓冲上限时从最旧的网络数据开始丢弃——网络数据量大且可重建，崩溃数据不可重建。 */
    fun trim(maxSize: Int) {
        if (size <= maxSize) return
        while (size > maxSize && network.isNotEmpty()) network.removeAt(0)
        if (size > maxSize) while (size > maxSize && frames.isNotEmpty()) frames.removeAt(0)
        if (size > maxSize) while (size > maxSize && scenes.isNotEmpty()) scenes.removeAt(0)
    }
}

/** 上报累计状态。 */
internal data class ApmUploadStats(
    val total: Long = 0L,
    val lastAt: Long = 0L,
    val lastOk: Boolean = false
)

/**
 * 网络请求的累计计数。
 *
 * [failed] 是原始失败数（含鉴权失败），[authFailed] 是其中 401/403 的次数。
 * 两个数分开存，才能算出「排除鉴权失败后」的成功率，又不必把全部记录留在内存里。
 */
internal data class ApmNetworkCounters(
    val total: Long = 0L,
    val failed: Long = 0L,
    val authFailed: Long = 0L
)

/**
 * 本地持久化。
 *
 * 分两类数据区别对待：
 * 1. 待上报队列——弱网或离线时会堆积，必须落盘，不能让进程被杀就丢；
 * 2. 累计计数与耗时样本——体积有上界，重启后仍要能算出"本次安装以来"的整体水位。
 *
 * 除崩溃路径外一律使用 apply()（异步落盘），避免每次网络请求都同步写磁盘——
 * 采集本身不能成为性能问题。
 */
internal class ApmStore(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    /** 设备标识：仅本机随机生成，不采集 IMEI/AndroidId 等可关联到真实用户的信息。 */
    val deviceId: String
        get() {
            sp.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotEmpty() }?.let { return it }
            val generated = UUID.randomUUID().toString()
            sp.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }

    // ---------------- 待上报队列 ----------------

    fun loadPending(): ApmPending =
        read<ApmPending>(KEY_PENDING, object : TypeToken<ApmPending>() {}.type) ?: ApmPending()

    fun savePending(pending: ApmPending, async: Boolean = true) {
        write(KEY_PENDING, pending, async)
    }

    // ---------------- 最近崩溃记录（面板展示） ----------------

    fun loadCrashes(): List<ApmCrashRecord> =
        read<List<ApmCrashRecord>>(KEY_CRASHES, object : TypeToken<List<ApmCrashRecord>>() {}.type) ?: emptyList()

    fun saveCrashes(list: List<ApmCrashRecord>, async: Boolean = true) {
        write(KEY_CRASHES, list, async)
    }

    // ---------------- 最近一次启动 ----------------

    fun loadStartup(): ApmStartupRecord? =
        read<ApmStartupRecord>(KEY_STARTUP, object : TypeToken<ApmStartupRecord>() {}.type)

    fun saveStartup(record: ApmStartupRecord) {
        write(KEY_STARTUP, record, true)
    }

    // ---------------- 累计计数 ----------------

    fun loadNetworkCounters(): ApmNetworkCounters = ApmNetworkCounters(
        total = sp.getLong(KEY_TOTAL, 0L),
        failed = sp.getLong(KEY_FAILED, 0L),
        authFailed = sp.getLong(KEY_AUTH_FAILED, 0L)
    )

    fun saveNetworkCounters(counters: ApmNetworkCounters) {
        sp.edit()
            .putLong(KEY_TOTAL, counters.total)
            .putLong(KEY_FAILED, counters.failed)
            .putLong(KEY_AUTH_FAILED, counters.authFailed)
            .apply()
    }

    fun loadUploadStats(): ApmUploadStats = ApmUploadStats(
        total = sp.getLong(KEY_UPLOADED, 0L),
        lastAt = sp.getLong(KEY_LAST_UPLOAD_AT, 0L),
        lastOk = sp.getBoolean(KEY_LAST_UPLOAD_OK, false)
    )

    fun saveUploadStats(stats: ApmUploadStats) {
        sp.edit()
            .putLong(KEY_UPLOADED, stats.total)
            .putLong(KEY_LAST_UPLOAD_AT, stats.lastAt)
            .putBoolean(KEY_LAST_UPLOAD_OK, stats.lastOk)
            .apply()
    }

    // ---------------- 耗时样本 ----------------

    fun loadDurations(): List<Long> {
        val raw = sp.getString(KEY_DURATIONS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(',').mapNotNull { it.toLongOrNull() }
    }

    fun saveDurations(list: List<Long>) {
        sp.edit().putString(KEY_DURATIONS, list.joinToString(",")).apply()
    }

    // ---------------- 清空 ----------------

    /** 清空采集数据，但保留设备标识——面板上点"清空"是重置统计，不该让这台设备变成一台新设备。 */
    fun clearAll() {
        sp.edit()
            .remove(KEY_PENDING)
            .remove(KEY_CRASHES)
            .remove(KEY_STARTUP)
            .remove(KEY_TOTAL)
            .remove(KEY_FAILED)
            .remove(KEY_AUTH_FAILED)
            .remove(KEY_UPLOADED)
            .remove(KEY_LAST_UPLOAD_AT)
            .remove(KEY_LAST_UPLOAD_OK)
            .remove(KEY_DURATIONS)
            .apply()
    }

    // ---------------- 内部读写 ----------------

    /**
     * 反序列化失败一律降级为 null 交给调用方兜底。
     * 本地缓存属于"丢了也不影响正确性"的数据，不值得为它让 App 起不来。
     */
    private fun <T> read(key: String, type: java.lang.reflect.Type): T? {
        val raw = sp.getString(key, null) ?: return null
        return try {
            gson.fromJson<T>(raw, type)
        } catch (t: Throwable) {
            ApmLog.w("读取本地缓存失败 key=$key，已丢弃", t)
            sp.edit().remove(key).apply()
            null
        }
    }

    private fun write(key: String, value: Any?, async: Boolean) {
        try {
            val editor = sp.edit().putString(key, gson.toJson(value))
            if (async) editor.apply() else editor.commit()
        } catch (t: Throwable) {
            ApmLog.w("写入本地缓存失败 key=$key", t)
        }
    }

    private companion object {
        const val PREFS_NAME = "apm_store"

        const val KEY_DEVICE_ID = "device_id"
        const val KEY_PENDING = "pending"
        const val KEY_CRASHES = "recent_crashes"
        const val KEY_STARTUP = "last_startup"
        const val KEY_TOTAL = "total_requests"
        const val KEY_FAILED = "failed_requests"
        const val KEY_AUTH_FAILED = "auth_failed_requests"
        const val KEY_UPLOADED = "uploaded_total"
        const val KEY_LAST_UPLOAD_AT = "last_upload_at"
        const val KEY_LAST_UPLOAD_OK = "last_upload_ok"
        const val KEY_DURATIONS = "duration_samples"
    }
}
