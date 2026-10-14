package com.example.campusflea.apm

import android.app.Application
import okhttp3.Interceptor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * APM 采集门面。
 *
 * 对外只暴露这一个入口，内部把网络、帧率、启动、崩溃四条采集链路收敛到一起。
 * 所有采集方法的开头都做了「未初始化 / 已关闭」的短路返回，
 * 保证接入方（Application、拦截器、业务埋点）可以无条件调用而不用担心时序。
 *
 * 线程模型：网络拦截器在 OkHttp 的线程池上调用 [recordNetwork]，帧率回调在主线程，
 * 崩溃回调在任意线程，上报在 `apm-flush` 线程。因此所有共享状态统一由 [lock] 保护，
 * 对外暴露的 [snapshot] 返回拷贝，避免面板遍历时被采集线程改写。
 */
object Apm {

    // 缓冲区上限：都是"看一眼就够"的展示型数据，留太长只会推高内存而不会提升可读性
    private const val MAX_DURATION_SAMPLES = 1000
    private const val MAX_SLOW_RECORDS = 30
    private const val MAX_CRASH_RECORDS = 20
    private const val MAX_SCENE_RECORDS = 20
    private const val MAX_FRAME_RECORDS = 12

    private val lock = Any()

    @Volatile
    private var config: ApmConfig? = null

    @Volatile
    private var store: ApmStore? = null

    private var reporter: ApmReporter? = null
    private var frameTracker: ApmFrameTracker? = null
    private var crashTracker: ApmCrashTracker? = null
    private var scheduler: ScheduledExecutorService? = null
    private val flushing = AtomicBoolean(false)

    /** 进程会话标识，同时作为网络数据的链路归属，便于把同一进程内的记录归到一次会话。 */
    private val sessionId: String = UUID.randomUUID().toString().replace("-", "").take(12)

    private val sceneStarts = ConcurrentHashMap<String, Long>()

    // ---------------- 采集状态（受 lock 保护） ----------------

    private var pending = ApmPending()
    private var totalRequests = 0L
    private var failedRequests = 0L

    /** 其中 401/403 的次数。单独计数是为了把它们从成功率分母里剔除，同时又能在面板上单独展示。 */
    private var authFailedRequests = 0L
    private var uploadedTotal = 0L
    private var lastUploadAt = 0L
    private var lastUploadOk = false

    private val durationSamples = ArrayDeque<Long>()
    private val recentSlow = ArrayDeque<ApmNetworkRecord>()
    private val recentCrashes = ArrayDeque<ApmCrashRecord>()
    private val recentScenes = ArrayDeque<ApmSceneRecord>()
    private val recentFrames = ArrayDeque<ApmFrameRecord>()
    private var lastStartup: ApmStartupRecord? = null

    @Volatile
    private var foreground = true

    // ---------------- 生命周期 ----------------

    val isEnabled: Boolean get() = config?.enabled == true

    /** 是否处于前台，供崩溃记录标注现场。 */
    val isForeground: Boolean get() = foreground

    /** 当前上报地址；为空表示只在端上采集，不产生任何外发请求。 */
    val uploadUrl: String get() = config?.uploadUrl.orEmpty()

    @JvmOverloads
    fun init(application: Application, config: ApmConfig = ApmConfig()) {
        if (this.config != null) return
        this.config = config
        ApmLog.enabled = config.debugLog

        if (!config.enabled) {
            ApmLog.d("APM 已关闭，跳过初始化")
            return
        }

        val localStore = ApmStore(application)
        store = localStore

        synchronized(lock) {
            pending = localStore.loadPending()
            val counters = localStore.loadNetworkCounters()
            totalRequests = counters.total
            failedRequests = counters.failed
            authFailedRequests = counters.authFailed

            val uploadStats = localStore.loadUploadStats()
            uploadedTotal = uploadStats.total
            lastUploadAt = uploadStats.lastAt
            lastUploadOk = uploadStats.lastOk

            localStore.loadDurations().takeLast(MAX_DURATION_SAMPLES).forEach { durationSamples.addLast(it) }
            localStore.loadCrashes().takeLast(MAX_CRASH_RECORDS).forEach { recentCrashes.addLast(it) }
            lastStartup = localStore.loadStartup()
        }

        reporter = ApmReporter(config)

        frameTracker = ApmFrameTracker(
            slowFrameMs = config.slowFrameMs,
            frozenFrameMs = config.frozenFrameMs
        ) { record -> onFrameReported(record) }.also { it.start() }

        crashTracker = ApmCrashTracker(
            anrBlockMs = config.anrBlockMs,
            isForeground = { foreground },
            onCrash = { record -> recordCrash(record) },
            flushBeforeExit = { flushNow(blocking = true) }
        ).also { it.install() }

        startScheduler(config)

        ApmLog.d(
            "APM 初始化完成 appId=${config.appId} " +
                "上报地址=${config.uploadUrl.ifBlank { "未配置，仅端上采集" }}"
        )
    }

    private fun startScheduler(c: ApmConfig) {
        if (scheduler != null) return
        val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "apm-flush").apply { isDaemon = true }
        }
        executor.scheduleWithFixedDelay(
            { runCatching { flush() } },
            c.flushIntervalMs,
            c.flushIntervalMs,
            TimeUnit.MILLISECONDS
        )
        scheduler = executor
    }

    // ---------------- 埋点入口 ----------------

    /** 在 Application.attachBaseContext 中调用。 */
    fun markAttach() = ApmStartupTracker.markAttach()

    /** 在 Application.onCreate 开头调用。 */
    fun markAppCreate() = ApmStartupTracker.markAppCreate()

    /**
     * 首屏绘制完成后调用（建议在首个 Activity 的 decorView.post 中）。
     * 重复调用是安全的，只有第一次会生成记录。
     */
    fun markFirstFrame() {
        val record = ApmStartupTracker.markFirstFrame() ?: return
        ApmLog.d("冷启动完成：${record.totalMs}ms，阶段=${record.stages}")
        synchronized(lock) {
            lastStartup = record
            pending.startups.add(record)
            pending.trim(config?.maxBufferSize ?: Int.MAX_VALUE)
            store?.saveStartup(record)
            store?.savePending(pending)
        }
    }

    fun onForeground() {
        foreground = true
        // 后台期间 Choreographer 收不到 vsync，回前台必须重置窗口，否则第一帧会记成超长耗时
        frameTracker?.onForeground()
    }

    fun onBackground() {
        foreground = false
        // 切后台是上报的天然时机：用户已离开，上报流量不会和前台业务争抢带宽，
        // 也避免进程随后被系统回收导致内存里的数据直接丢失
        synchronized(lock) {
            store?.saveDurations(durationSamples.toList())
            store?.saveNetworkCounters(ApmNetworkCounters(totalRequests, failedRequests, authFailedRequests))
            store?.savePending(pending)
        }
        flushNow()
    }

    /** 挂到 OkHttp 上；未初始化或已关闭时该拦截器直接透传，不产生开销。 */
    fun networkInterceptor(): Interceptor = ApmNetworkInterceptor()

    /**
     * 开始一段自定义区间埋点，与 [endScene] 成对使用。
     * 用于业务侧关心的耗时，例如一次下单流程、一个页面的可见停留时长。
     */
    fun beginScene(name: String) {
        if (!isEnabled) return
        sceneStarts[name] = System.nanoTime()
    }

    fun endScene(name: String) {
        val c = config ?: return
        if (!c.enabled) return
        val startNs = sceneStarts.remove(name) ?: return
        val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        val record = ApmSceneRecord(name, durationMs, System.currentTimeMillis())
        ApmLog.d("场景 $name 耗时 ${durationMs}ms")
        synchronized(lock) {
            recentScenes.addFirst(record)
            while (recentScenes.size > MAX_SCENE_RECORDS) recentScenes.removeLast()
            pending.scenes.add(record)
            pending.trim(c.maxBufferSize)
        }
    }

    // ---------------- 采集写入 ----------------

    internal fun recordNetwork(
        path: String,
        method: String,
        code: Int,
        durationMs: Long,
        requestBytes: Long,
        responseBytes: Long,
        errorType: String?,
        timestamp: Long
    ) {
        val c = config ?: return
        if (!c.enabled) return

        val record = ApmNetworkRecord(
            url = path,
            method = method,
            code = code,
            durationMs = durationMs,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            errorType = errorType,
            traceId = sessionId,
            timestamp = timestamp
        )

        synchronized(lock) {
            totalRequests++
            if (!record.success) failedRequests++
            if (record.isAuthFailure) authFailedRequests++
            durationSamples.addLast(durationMs)
            while (durationSamples.size > MAX_DURATION_SAMPLES) durationSamples.removeFirst()

            pending.network.add(record)

            // 慢请求和失败请求都单独留痕：它们是排障时真正会被翻出来的那几条
            if (durationMs >= c.slowRequestMs || !record.success) {
                recentSlow.addFirst(record)
                while (recentSlow.size > MAX_SLOW_RECORDS) recentSlow.removeLast()
            }
            pending.trim(c.maxBufferSize)
        }
    }

    internal fun recordCrash(record: ApmCrashRecord) {
        val c = config ?: return
        if (!c.enabled) return

        ApmLog.e("捕获${if (record.isAnr) "ANR" else "崩溃"}：${record.message}")
        synchronized(lock) {
            pending.crashes.add(record)
            recentCrashes.addFirst(record)
            while (recentCrashes.size > MAX_CRASH_RECORDS) recentCrashes.removeLast()
            // 崩溃路径必须同步落盘：进程随时可能结束，异步写盘来不及
            store?.saveCrashes(recentCrashes.toList(), async = false)
            store?.savePending(pending, async = false)
        }
    }

    private fun onFrameReported(record: ApmFrameRecord) {
        val c = config ?: return
        synchronized(lock) {
            recentFrames.addLast(record)
            while (recentFrames.size > MAX_FRAME_RECORDS) recentFrames.removeFirst()
            pending.frames.add(record)
            pending.trim(c.maxBufferSize)
        }
    }

    // ---------------- 上报 ----------------

    /**
     * 触发一次上报。
     * [blocking] = true 用于崩溃兜底：此时进程马上结束，必须同步把数据送出去。
     */
    @JvmOverloads
    fun flushNow(blocking: Boolean = false) {
        if (blocking) {
            flush()
            return
        }
        val executor = scheduler
        if (executor == null) flush() else executor.execute { runCatching { flush() } }
    }

    private fun flush() {
        val c = config ?: return
        if (!c.enabled) return

        // 未配置上报地址：数据留在端上供面板查看，不做无意义的搬运，也不丢弃
        if (c.uploadUrl.isBlank()) return

        // 同时只允许一次上报在途：并发上报会在失败时把同一批数据重复回填，越积越多
        if (!flushing.compareAndSet(false, true)) return

        try {
            val batch = takeBatch(c.batchSize) ?: return

            val ok = reporter?.upload(batch, store?.deviceId ?: "unknown", sessionId) ?: false

            synchronized(lock) {
                if (ok) {
                    uploadedTotal += batch.size
                } else {
                    pending.mergeFront(batch)
                }
                lastUploadAt = System.currentTimeMillis()
                lastUploadOk = ok
                store?.savePending(pending)
                store?.saveUploadStats(ApmUploadStats(uploadedTotal, lastUploadAt, lastUploadOk))
                store?.saveNetworkCounters(ApmNetworkCounters(totalRequests, failedRequests, authFailedRequests))
                store?.saveDurations(durationSamples.toList())
            }
        } finally {
            flushing.set(false)
        }
    }

    /** 取出一批待上报数据并从待上报队列中移除；队列为空时返回 null。 */
    private fun takeBatch(max: Int): ApmPending? = synchronized(lock) {
        if (pending.isEmpty) null else pending.takeBatch(max)
    }

    // ---------------- 读取 ----------------

    /** 返回当前采集状态的只读拷贝，供监控面板渲染。 */
    fun snapshot(): ApmSnapshot {
        val c = config
        synchronized(lock) {
            val sorted = durationSamples.sorted()
            // 可判定请求数：排除掉「当时没有登录态」的 401/403。
            // 口径与后端 successRateIgnoringAuth 一致，两边的成功率才对得上。
            val judged = totalRequests - authFailedRequests
            val summary = ApmNetworkSummary(
                total = totalRequests,
                failure = failedRequests,
                authFailure = authFailedRequests,
                successRate = if (judged <= 0L) {
                    null
                } else {
                    (judged - (failedRequests - authFailedRequests)).toFloat() / judged
                },
                p50 = percentile(sorted, 50),
                p90 = percentile(sorted, 90),
                p99 = percentile(sorted, 99),
                avg = if (sorted.isEmpty()) 0L else sorted.average().roundToInt().toLong(),
                max = sorted.lastOrNull() ?: 0L
            )

            val frames = recentFrames.toList()
            val totalFrames = frames.sumOf { it.totalFrames }
            val jankyFrames = frames.sumOf { it.jankyFrames }

            return ApmSnapshot(
                enabled = c?.enabled == true,
                startup = lastStartup,
                latestFrame = frames.lastOrNull(),
                avgFps = if (frames.isEmpty()) 0f else (frames.sumOf { it.fps.toDouble() } / frames.size).toFloat(),
                jankRate = if (totalFrames == 0) 0f else jankyFrames.toFloat() / totalFrames,
                network = summary,
                slowRequests = recentSlow.toList(),
                crashes = recentCrashes.toList(),
                scenes = recentScenes.toList(),
                pendingUpload = pending.size,
                uploadedTotal = uploadedTotal,
                lastUploadAt = lastUploadAt,
                lastUploadOk = lastUploadOk
            )
        }
    }

    /** 重置统计。设备标识会保留，不会因为点了一次"清空"就变成一台新设备。 */
    fun reset() {
        synchronized(lock) {
            pending = ApmPending()
            durationSamples.clear()
            recentSlow.clear()
            recentCrashes.clear()
            recentScenes.clear()
            recentFrames.clear()
            totalRequests = 0L
            failedRequests = 0L
            authFailedRequests = 0L
            uploadedTotal = 0L
            lastUploadAt = 0L
            lastUploadOk = false
            lastStartup = null
            store?.clearAll()
        }
        sceneStarts.clear()
        ApmLog.d("采集数据已重置")
    }

    // ---------------- 工具 ----------------

    /**
     * 归一化请求路径，把动态 id 段替换为 `{id}`。
     *
     * 不做归一化的话 /goods/1001 与 /goods/1002 会被算成两个接口，
     * 每个接口只有个位数样本，P99 会退化成随机数，慢接口排名也就失去意义。
     */
    internal fun normalizePath(path: String): String {
        if (path.isEmpty()) return "/"
        return path
            .replace(LONG_HEX_SEGMENT, "/{id}")
            .replace(NUMERIC_SEGMENT, "/{id}")
    }

    private fun percentile(sorted: List<Long>, p: Int): Long {
        if (sorted.isEmpty()) return 0L
        val index = ((p / 100.0) * (sorted.size - 1)).roundToInt().coerceIn(0, sorted.size - 1)
        return sorted[index]
    }

    private val NUMERIC_SEGMENT = Regex("/\\d+")
    private val LONG_HEX_SEGMENT = Regex("/[0-9a-fA-F]{16,}")
}
