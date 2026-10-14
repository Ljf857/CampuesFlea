package com.example.campusflea.data.remote

import android.util.Log
import com.example.campusflea.BuildConfig
import com.example.campusflea.data.model.MessageInfo
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

object WebSocketManager {

    /**
     * WebSocket 地址。由 gradle.properties 的 api.wsUrl 注入；
     * 该项留空时按 api.baseUrl 自动推导（http->ws / https->wss，末尾补 /ws）。
     */
    private val BASE_WS_URL: String = BuildConfig.WS_BASE_URL
    private const val RECONNECT_BASE_DELAY_MS = 1000L
    private const val MAX_RECONNECT_ATTEMPTS = 5

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectAttempts = 0
    private var shouldReconnect = false

    // 连接已建立：避免重复 connect() 反复重建 socket
    @Volatile
    private var connected = false

    // 正在发起连接：避免并发 connect() 创建出多个 socket
    @Volatile
    private var connecting = false

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events

    sealed class WsEvent {
        data class NewMessage(val message: MessageInfo) : WsEvent()
        data class Error(val message: String) : WsEvent()
        data object Connected : WsEvent()
        data object Disconnected : WsEvent()

        /**
         * 登录态不可用（token 缺失或已失效，且刷新失败）。
         * 与 [Error] 分开是因为它的处理方式完全不同：错误提示给页面，
         * 而会话失效必须由全局层接管并引导回登录页，否则用户会停留在一个
         * 所有请求都静默 401 的界面上，无从判断发生了什么。
         */
        data object SessionExpired : WsEvent()
    }

    fun connect() {
        if (connecting) return
        scope.launch {
            if (connecting) return@launch
            connecting = true
            try {
                // 用未过期的 token 建连：过期先自动刷新，避免被服务器以 POLICY_VIOLATION 拒绝
                val token = RetrofitClient.getValidAccessToken()
                if (token.isNullOrEmpty()) {
                    _events.tryEmit(WsEvent.SessionExpired)
                    return@launch
                }
                if (connected && webSocket != null) return@launch // 已连接，无需重建
                shouldReconnect = true
                reconnectAttempts = 0
                // 关闭旧的连接，避免多个 socket 并存造成泄漏
                webSocket?.close(1000, "重新连接")
                webSocket = null
                doConnect(token)
            } finally {
                connecting = false
            }
        }
    }

    private fun doConnect(token: String) {
        val request = Request.Builder()
            .url("$BASE_WS_URL?token=$token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket 已连接")
                connected = true
                reconnectAttempts = 0
                _events.tryEmit(WsEvent.Connected)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket 已关闭: $reason")
                connected = false
                _events.tryEmit(WsEvent.Disconnected)
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket 连接失败", t)
                connected = false
                _events.tryEmit(WsEvent.Error(t.message ?: "连接失败"))
                scheduleReconnect()
            }
        })
    }

    fun disconnect() {
        shouldReconnect = false
        connected = false
        reconnectAttempts = MAX_RECONNECT_ATTEMPTS
        webSocket?.close(1000, "用户断开")
        webSocket = null
    }

    /**
     * 发送消息。
     *
     * @param conversationId 会话的权威标识。一旦客户端已知就必须携带，
     *        服务端据此把消息落到确定的会话；为空时服务端退回按
     *        (接收者, 商品) 定位，同一对用户存在多个商品会话时可能落错。
     * @return 是否成功交给 socket。false 表示连接未建立，调用方应把消息标记为发送失败，
     *         不能静默丢弃——send() 只是入队，真正的失败由服务端 error 帧或超时判定。
     */
    fun sendMessage(
        receiverId: Long,
        content: String,
        msgType: Int = 1,
        goodsId: Long? = null,
        conversationId: Long? = null
    ): Boolean {
        val json = buildSendMessageJson(receiverId, content, msgType, goodsId, conversationId)
        return webSocket?.send(json) == true
    }

    /** @return 是否成功交给 socket */
    fun markRead(conversationId: Long): Boolean {
        if (conversationId <= 0) return false
        val json = """{"type":"mark_read","conversationId":$conversationId}"""
        return webSocket?.send(json) == true
    }

    fun ping() {
        webSocket?.send("""{"type":"ping"}""")
    }

    private fun handleMessage(text: String) {
        try {
            val json = JsonParser.parseString(text).asJsonObject
            val type = json.get("type")?.asString ?: return

            when (type) {
                "new_message" -> {
                    // data 缺失或为 null 时 gson 会返回 null，直接构造 NewMessage 会在
                    // Kotlin 的非空参数校验上抛 NPE —— 而这个回调跑在 OkHttp 线程上，会直接崩溃
                    val data = json.getAsJsonObject("data") ?: return
                    val message = gson.fromJson(data, MessageInfo::class.java) ?: return
                    _events.tryEmit(WsEvent.NewMessage(message))
                }
                "pong" -> { /* 心跳回复，不需要处理 */ }
                "error" -> {
                    val msg = json.get("message")?.asString ?: "未知错误"
                    _events.tryEmit(WsEvent.Error(msg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "消息解析失败", e)
        }
    }

    private fun buildSendMessageJson(
        receiverId: Long,
        content: String,
        msgType: Int,
        goodsId: Long?,
        conversationId: Long?
    ): String {
        val obj = JsonObject().apply {
            addProperty("type", "send_message")
            addProperty("receiverId", receiverId)
            addProperty("content", content)
            addProperty("msgType", msgType)
            if (conversationId != null && conversationId > 0) addProperty("conversationId", conversationId)
            if (goodsId != null && goodsId > 0) addProperty("goodsId", goodsId)
        }
        return obj.toString()
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
        reconnectAttempts++
        // 指数退避：固定 3 秒会在断网期间持续空转，而真正的弱网（如宿舍 WiFi 抖动）
        // 往往需要十几秒以上才恢复。1s / 2s / 4s / 8s / 16s 覆盖约半分钟。
        val backoff = RECONNECT_BASE_DELAY_MS shl (reconnectAttempts - 1)
        scope.launch {
            delay(backoff)
            if (!shouldReconnect || reconnectAttempts > MAX_RECONNECT_ATTEMPTS) return@launch
            // 重连同样使用未过期的 token
            val token = RetrofitClient.getValidAccessToken()
            if (token.isNullOrEmpty()) {
                _events.tryEmit(WsEvent.SessionExpired)
                return@launch
            }
            doConnect(token)
        }
    }

    private const val TAG = "WebSocketManager"
}
