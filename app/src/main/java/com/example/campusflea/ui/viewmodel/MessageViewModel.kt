package com.example.campusflea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.ConversationInfo
import com.example.campusflea.data.model.NotificationInfo
import com.example.campusflea.data.remote.WebSocketManager
import com.example.campusflea.data.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MessageViewModel : ViewModel() {
    private val repo = MessageRepository()
    private val _conversations = MutableStateFlow<List<ConversationInfo>>(emptyList())
    val conversations: StateFlow<List<ConversationInfo>> = _conversations
    private val _notifications = MutableStateFlow<List<NotificationInfo>>(emptyList())
    val notifications: StateFlow<List<NotificationInfo>> = _notifications
    private val _notificationsError = MutableStateFlow<String?>(null)
    val notificationsError: StateFlow<String?> = _notificationsError
    private val _notificationsLoading = MutableStateFlow(false)
    val notificationsLoading: StateFlow<Boolean> = _notificationsLoading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var loadJob: Job? = null
    private var debounceJob: Job? = null
    private var notificationsJob: Job? = null

    init {
        loadConversations()
        // 实时刷新：收到新消息重载会话列表，让新会话与未读角标即时出现
        viewModelScope.launch {
            WebSocketManager.events.collect { event ->
                if (event is WebSocketManager.WsEvent.NewMessage) scheduleReload()
            }
        }
    }

    /**
     * 合并短时间内的多次触发。
     * 对话密集时（对方连发几条）每条消息都立刻打一次请求毫无意义，
     * 300ms 内的多次触发只保留最后一次。
     */
    private fun scheduleReload() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(RELOAD_DEBOUNCE_MS)
            loadConversations()
        }
    }

    /**
     * 加载会话列表。
     *
     * 先取消上一次未完成的请求：并发请求的返回顺序无法保证，旧请求后到会把新结果覆盖掉；
     * 而且每个请求结束都会写 `_isLoading`，先返回的那个会把加载态提前置 false。
     * 失败时保留上一次的列表，不要在用户眼前清空。
     */
    fun loadConversations() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            repo.getConversations().onSuccess { _conversations.value = it }
            _isLoading.value = false
        }
    }
    /**
     * 加载系统通知。
     *
     * 与 [loadConversations] 一样先取消上一次未完成的请求，避免旧结果覆盖新结果。
     *
     * 失败必须让 UI 知道：以前只挂了 onSuccess，401 或断网都被无声吞掉，
     * 用户看到的是「一片空白」，既分不清「没有通知」和「请求失败」，也没有重试入口。
     * 失败时保留上一次的列表，不在用户眼前清空。
     */
    fun loadNotifications() {
        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            _notificationsLoading.value = true
            val result = repo.getNotifications()
            // 仓库层把 CancellationException 也一并 catch 成了失败结果，
            // 所以「被自己 cancel 掉的上一次请求」会带着一个失败结果回来。
            // 用 isActive 把它挡掉，否则连点两次重试就会弹出一条并不存在的错误。
            // 此时不重置 loading：接管的新请求正在跑，它自己会置回 false。
            if (!isActive) return@launch
            result
                .onSuccess {
                    _notifications.value = it.list
                    _notificationsError.value = null
                }
                .onFailure {
                    _notificationsError.value = it.message ?: "加载失败，请重试"
                }
            _notificationsLoading.value = false
        }
    }
    fun markRead(id: Long) { viewModelScope.launch { repo.markRead(id); loadConversations() } }

    fun deleteConversation(userId: Long, conversationId: Long) {
        viewModelScope.launch {
            // 带上 conversationId：只靠 userId 会删掉这对用户之间「某个」会话，
            // 而他们可能因不同商品存在多个会话
            repo.deleteConversation(userId, conversationId)
            loadConversations()
        }
    }

    private companion object {
        const val RELOAD_DEBOUNCE_MS = 300L
    }
}
