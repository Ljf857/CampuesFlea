package com.example.campusflea.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.local.ChatDatabase
import com.example.campusflea.data.local.ChatMessageEntity
import com.example.campusflea.data.model.MessageInfo
import com.example.campusflea.data.model.MessageSendState
import com.example.campusflea.data.remote.WebSocketManager
import com.example.campusflea.data.remote.WebSocketManager.WsEvent
import com.example.campusflea.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 单会话聊天。
 *
 * 三条数据来源要合并到同一个列表里，这是本类最核心的复杂度：
 *   1. HTTP 拉取的历史记录（分页）
 *   2. WebSocket 推送的新消息
 *   3. 本地乐观插入的待确认消息
 *
 * 旧实现的做法是「谁后到谁整体覆盖」，于是推送到的消息会被紧接着返回的
 * HTTP 结果抹掉、本地缓存也会被只含第一页的远程结果截断。
 * 现在统一走 [merge]：以服务端 id 为键做并集，本地待确认消息单独保留。
 *
 * [conversationId] 是会话的权威标识，一旦从任意来源获知就全程携带给服务端，
 * 避免服务端在「同一对用户存在多个商品会话」时投递到错误的会话。
 */
class ChatViewModel : ViewModel() {

    private val repo = MessageRepository()

    private val _messages = MutableStateFlow<List<MessageInfo>>(emptyList())
    val messages: StateFlow<List<MessageInfo>> = _messages

    /** 首屏 / 重新加载 */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** 向上翻页加载更早的历史 */
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearError() { _error.value = null }

    private var db: ChatDatabase? = null

    /** 已初始化的会话标识，用于避免重复 init */
    private var initKey = ""
    private var targetUserId = 0L
    private var goodsId: Long? = null

    /** 服务端会话 ID：会话的权威标识，0 表示尚未获知 */
    private var conversationId = 0L

    private var page = 1
    private var wsJob: Job? = null

    /** 本地临时 ID 种子，递减产生负数，与服务端正数 ID 天然隔离 */
    private var localIdSeed = 0L

    /** 待确认消息的超时任务：localId -> Job */
    private val sendTimeouts = mutableMapOf<Long, Job>()

    private val timeFormat = SimpleDateFormat(SERVER_TIME_PATTERN, Locale.getDefault())

    /**
     * 进入某个会话。
     *
     * [conversationId] 从消息列表进入时**必须**传入：同一对用户会因不同商品存在多个会话，
     * 只给 [targetId]（外加 goodsId）时服务端只能「猜」一个，猜错的后果很具体：
     * 两个条目点开内容一模一样、清除聊天记录删不掉、发消息凭空多出一个重复会话。
     * 传进来即作为本会话的权威标识，之后读、写、删全程携带。
     */
    fun init(
        context: Context,
        targetId: Long,
        goodsId: Long? = null,
        conversationId: Long? = null
    ) {
        val knownConversationId = conversationId?.takeIf { it > 0 } ?: 0L
        val key = "u$targetId-g${goodsId ?: 0L}-c$knownConversationId"
        if (db != null && initKey == key) return

        db = ChatDatabase.getInstance(context)
        initKey = key
        targetUserId = targetId
        this.goodsId = goodsId

        // 切到另一个会话：清空上一段的状态，避免串台
        this.conversationId = knownConversationId
        page = 1
        _hasMore.value = false
        _messages.value = emptyList()
        cancelAllTimeouts()

        WebSocketManager.connect()
        observeWebSocket()
        loadMessages(targetId, goodsId)
    }

    // ------------------------------------------------------------------
    // 事件订阅
    // ------------------------------------------------------------------
    private fun observeWebSocket() {
        wsJob?.cancel()
        wsJob = viewModelScope.launch {
            WebSocketManager.events.collect { event ->
                when (event) {
                    is WsEvent.NewMessage -> onIncomingMessage(event.message)
                    // 错误与会话失效由全局层处理（见 CampusFleaApp），此处不重复打扰用户
                    else -> Unit
                }
            }
        }
    }

    private suspend fun onIncomingMessage(incoming: MessageInfo) {
        if (!belongsToCurrentChat(incoming)) return

        // 服务端对收发双方推同一份 VO。现已按接收方分别下发 isMine，
        // 但仍以 senderId 为准做一次兜底推导：字段缺失或对接旧版服务端时气泡方向不会反。
        val normalized = incoming.copy(
            isMine = incoming.senderId != targetUserId,
            sendState = MessageSendState.SENT
        )

        adoptConversationId(normalized.conversationId)

        // 如果这条正是我刚发出、还在等确认的消息，用它替换本地占位而不是再追加一条
        val pending = _messages.value.lastOrNull { candidate ->
            candidate.sendState == MessageSendState.SENDING && candidate.content == normalized.content
        }
        if (pending != null) {
            sendTimeouts.remove(pending.localId)?.cancel()
            _messages.value = _messages.value.map {
                if (it.localId == pending.localId) normalized else it
            }
        } else {
            if (_messages.value.any { it.id > 0 && it.id == normalized.id }) return
            _messages.value = _messages.value + normalized
        }

        persist(listOf(normalized))
        // 正在聊天时收到消息立即标记已读，避免未读角标残留
        if (conversationId > 0) repo.markRead(conversationId)
    }

    private fun belongsToCurrentChat(msg: MessageInfo): Boolean {
        if (conversationId > 0 && msg.conversationId > 0) return msg.conversationId == conversationId
        return msg.senderId == targetUserId || msg.receiverId == targetUserId
    }

    // ------------------------------------------------------------------
    // 加载
    // ------------------------------------------------------------------
    fun loadMessages(userId: Long, goodsId: Long? = null) {
        targetUserId = userId
        this.goodsId = goodsId
        viewModelScope.launch {
            _isLoading.value = true
            page = 1

            // 先上屏本地缓存（秒开）。只有会话已知时才有稳定的分组键可用。
            localKey()?.let { key ->
                db?.messageDao()?.getMessagesOnce(key)
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { cached ->
                        _messages.value = merge(_messages.value, cached.map { it.toMessageInfo() })
                    }
            }

            repo.getMessages(
                userId = userId,
                conversationId = conversationId.takeIf { it > 0 },
                goodsId = goodsId,
                page = page,
                size = PAGE_SIZE
            )
                .onSuccess { result ->
                    result.list.firstOrNull()?.let { adoptConversationId(it.conversationId) }
                    _messages.value = merge(_messages.value, result.list)
                    _hasMore.value = result.totalPages > page
                    persist(_messages.value)
                    if (conversationId > 0) repo.markRead(conversationId)
                }
                .onFailure { _error.value = it.message ?: "消息加载失败" }

            _isLoading.value = false
        }
    }

    /** 向上翻页，加载更早的历史消息 */
    fun loadMoreHistory() {
        if (_isLoadingMore.value || !_hasMore.value || targetUserId == 0L) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            val next = page + 1
            repo.getMessages(
                userId = targetUserId,
                conversationId = conversationId.takeIf { it > 0 },
                goodsId = goodsId,
                page = next,
                size = PAGE_SIZE
            )
                .onSuccess { result ->
                    page = next
                    _hasMore.value = result.totalPages > next
                    _messages.value = merge(_messages.value, result.list)
                    persist(result.list)
                }
                .onFailure { _error.value = "加载更多失败" }
            _isLoadingMore.value = false
        }
    }

    // ------------------------------------------------------------------
    // 发送
    // ------------------------------------------------------------------
    /**
     * 发送文本消息。
     *
     * 乐观插入一条 [MessageSendState.SENDING] 的消息并立刻上屏，
     * 服务端回推同内容消息时替换它；超时未确认则标记为失败并提供重试。
     * 旧实现是直接发出去、把输入框清空、失败只丢一个被忽略的 Error 事件，
     * 用户会以为消息发出去了，实际对方永远收不到。
     */
    fun sendMessage(receiverId: Long, content: String, goodsId: Long? = null) {
        val text = content.trim()
        if (text.isEmpty()) return
        if (goodsId != null) this.goodsId = goodsId
        dispatchOutgoing(receiverId, text, type = TYPE_TEXT)
    }

    fun uploadAndSendFile(context: Context, receiverId: Long, uri: Uri, isImage: Boolean) {
        viewModelScope.launch {
            _uploading.value = true
            try {
                val payload = withContext(Dispatchers.IO) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext null
                    val mime = context.contentResolver.getType(uri)
                        ?: if (isImage) "image/jpeg" else "application/octet-stream"
                    bytes to mime
                }
                if (payload == null) {
                    _error.value = "无法读取文件"
                    return@launch
                }

                val (bytes, mimeType) = payload
                val ext = mimeType.substringAfterLast("/", "bin")
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = if (isImage) "chat_img_$timestamp.$ext" else "chat_file_$timestamp.$ext"
                val part = MultipartBody.Part.createFormData(
                    "file", filename, bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                )

                repo.uploadFile(part)
                    .onSuccess { url ->
                        // 上传成功后同样走乐观发送：先上屏再等服务端确认
                        dispatchOutgoing(receiverId, url, type = if (isImage) TYPE_IMAGE else TYPE_FILE)
                    }
                    .onFailure { _error.value = it.message ?: "文件上传失败，请重试" }
            } catch (e: Exception) {
                // 以前这里是空 catch，任何 IO / FileProvider 异常都被吞掉，用户毫无反馈
                _error.value = e.message ?: "发送失败，请重试"
            } finally {
                _uploading.value = false
            }
        }
    }

    private fun dispatchOutgoing(receiverId: Long, content: String, type: Int) {
        val localId = --localIdSeed
        val optimistic = MessageInfo(
            id = 0,
            localId = localId,
            conversationId = conversationId,
            senderId = 0, // 自己的 userId 本地无需保存，isMine 已足够渲染
            receiverId = receiverId,
            goodsId = goodsId ?: 0L,
            content = content,
            type = type,
            createTime = timeFormat.format(Date()),
            isMine = true,
            sendState = MessageSendState.SENDING
        )
        _messages.value = _messages.value + optimistic

        val handed = WebSocketManager.sendMessage(
            receiverId = receiverId,
            content = content,
            msgType = type,
            goodsId = goodsId,
            conversationId = conversationId.takeIf { it > 0 }
        )
        if (handed) armSendTimeout(localId) else markSendFailed(localId, "连接不可用，消息未发出")
    }

    fun retrySend(localId: Long) {
        val target = _messages.value.firstOrNull { it.localId == localId } ?: return
        if (target.sendState != MessageSendState.FAILED) return

        _messages.value = _messages.value.map {
            if (it.localId == localId) it.copy(sendState = MessageSendState.SENDING) else it
        }
        val handed = WebSocketManager.sendMessage(
            receiverId = target.receiverId,
            content = target.content,
            msgType = target.type,
            goodsId = goodsId,
            conversationId = conversationId.takeIf { it > 0 }
        )
        if (handed) armSendTimeout(localId) else markSendFailed(localId, "仍无法发送，请检查网络")
    }

    private fun armSendTimeout(localId: Long) {
        sendTimeouts.remove(localId)?.cancel()
        sendTimeouts[localId] = viewModelScope.launch {
            delay(SEND_TIMEOUT_MS)
            val stillSending = _messages.value.any {
                it.localId == localId && it.sendState == MessageSendState.SENDING
            }
            if (stillSending) markSendFailed(localId, "发送超时，点击重试")
        }
    }

    private fun markSendFailed(localId: Long, reason: String) {
        sendTimeouts.remove(localId)?.cancel()
        val changed = _messages.value.any {
            it.localId == localId && it.sendState == MessageSendState.SENDING
        }
        if (!changed) return
        _messages.value = _messages.value.map {
            if (it.localId == localId) it.copy(sendState = MessageSendState.FAILED) else it
        }
        _error.value = reason
    }

    private fun cancelAllTimeouts() {
        sendTimeouts.values.forEach { it.cancel() }
        sendTimeouts.clear()
    }

    // ------------------------------------------------------------------
    // 其他
    // ------------------------------------------------------------------
    fun clearChat(context: Context, userId: Long) {
        viewModelScope.launch {
            localKey()?.let { db?.messageDao()?.deleteConversation(it) }
            _messages.value = emptyList()
            // 服务端删除失败要告诉用户：否则本地已清空、下次进入历史又回来，会以为删除功能坏了
            repo.deleteConversation(userId, conversationId.takeIf { it > 0 })
                .onFailure { _error.value = it.message ?: "清空失败，请稍后重试" }
        }
    }

    fun createCameraTempUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera")
        dir.mkdirs()
        val file = File(dir, "chat_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    override fun onCleared() {
        super.onCleared()
        wsJob?.cancel()
        cancelAllTimeouts()
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /**
     * 本地缓存的分组键。
     *
     * 用 conversationId 而不是 targetUserId：服务端按 (userA, userB, goodsId) 建会话，
     * 同一对用户因不同商品会有多个会话。用 userId 当键会让这些会话在本地缓存里串成一条，
     * 表现为「切到另一个商品再进来，看到的是上一个商品的聊天记录」。
     *
     * 会话未知（首次从商品详情发起）时返回 null，此时不读也不写缓存——
     * 那个会话本来也没有历史可言。
     */
    private fun localKey(): String? = conversationId.takeIf { it > 0 }?.let { "c$it" }

    private fun adoptConversationId(id: Long) {
        if (id > 0 && id != conversationId) conversationId = id
    }

    /**
     * 按服务端 id 合并，而不是整体覆盖。
     *
     * 这是本类修复的核心：任何一方后到都不应该抹掉另一方已经拿到的数据。
     */
    private fun merge(current: List<MessageInfo>, incoming: List<MessageInfo>): List<MessageInfo> {
        val byId = LinkedHashMap<Long, MessageInfo>()
        current.forEach { if (it.id > 0) byId[it.id] = it }
        incoming.forEach {
            if (it.id > 0) byId[it.id] = it.copy(sendState = MessageSendState.SENT)
        }
        // 尚未确认的本地消息（id == 0）单独保留，它们的 createTime 是当前时刻，排序后自然落在末尾
        val pendingLocals = current.filter { it.id == 0L }
        return (byId.values + pendingLocals)
            .sortedWith(compareBy({ it.createTime }, { it.localId }))
    }

    private suspend fun persist(messages: List<MessageInfo>) {
        val key = localKey() ?: return
        val dao = db?.messageDao() ?: return
        dao.insertAll(messages.filter { it.id > 0 }.map { it.toEntity(key) })
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val SEND_TIMEOUT_MS = 10_000L
        const val TYPE_TEXT = 1
        const val TYPE_IMAGE = 2
        const val TYPE_FILE = 3

        /** 与服务端 MessageVO.createTime 的 @JsonFormat 对齐，保证本地消息能正确参与排序 */
        const val SERVER_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
    }
}

private fun MessageInfo.toEntity(conversationKey: String) = ChatMessageEntity(
    remoteId = id,
    conversationKey = conversationKey,
    conversationId = conversationId,
    senderId = senderId,
    receiverId = receiverId,
    goodsId = goodsId,
    content = content,
    type = type,
    typeDesc = typeDesc,
    isRead = isRead,
    createTime = createTime,
    isMine = isMine,
    senderName = senderName,
    senderAvatar = senderAvatar
)

private fun ChatMessageEntity.toMessageInfo() = MessageInfo(
    id = remoteId,
    conversationId = conversationId,
    senderId = senderId,
    receiverId = receiverId,
    goodsId = goodsId,
    content = content,
    type = type,
    typeDesc = typeDesc,
    isRead = isRead,
    createTime = createTime,
    isMine = isMine,
    senderName = senderName,
    senderAvatar = senderAvatar
)
