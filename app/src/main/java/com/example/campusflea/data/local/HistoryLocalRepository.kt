package com.example.campusflea.data.local

import android.content.Context
import com.example.campusflea.data.model.GoodsInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HistoryLocalRepository private constructor(context: Context) {

    companion object {
        private const val MAX_RECORDS = 50

        @Volatile
        private var INSTANCE: HistoryLocalRepository? = null

        fun getInstance(context: Context): HistoryLocalRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HistoryLocalRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val gson = Gson()
    private val file = File(context.filesDir, "browse_history.json")
    private val _flow = MutableStateFlow<List<Record>>(emptyList())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loaded = false

    init {
        // 异步加载，避免在主线程同步读文件
        scope.launch { ensureLoaded() }
    }

    fun getAll(): Flow<List<GoodsInfo>> {
        return _flow.map { records -> records.map { it.toGoodsInfo() } }
    }

    suspend fun insert(goodsId: Long, title: String, price: Double, coverImage: String) = withContext(Dispatchers.IO) {
        ensureLoaded()
        val list = _flow.value.toMutableList()
        list.removeAll { it.goodsId == goodsId }
        list.add(0, Record(goodsId, title, price, coverImage, System.currentTimeMillis()))
        save(list.take(MAX_RECORDS))
    }

    suspend fun delete(goodsId: Long) = withContext(Dispatchers.IO) {
        ensureLoaded()
        save(_flow.value.filter { it.goodsId != goodsId })
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        ensureLoaded()
        save(emptyList())
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        loaded = true
        try {
            if (file.exists()) {
                val json = file.readText()
                val type = object : TypeToken<List<Record>>() {}.type
                val list: List<Record> = gson.fromJson(json, type) ?: emptyList()
                _flow.value = list
            }
        } catch (_: Exception) {
            _flow.value = emptyList()
        }
    }

    private suspend fun save(list: List<Record>) {
        _flow.value = list
        file.writeText(gson.toJson(list))
    }

    private data class Record(
        val goodsId: Long,
        val title: String,
        val price: Double,
        val coverImage: String,
        val viewTime: Long
    ) {
        fun toGoodsInfo() = GoodsInfo(id = goodsId, title = title, price = price, coverImage = coverImage)
    }
}
