package com.example.campusflea.data.repository

import com.example.campusflea.data.model.*
import com.example.campusflea.data.remote.PageResult
import com.example.campusflea.data.remote.RetrofitClient
import com.example.campusflea.data.remote.toFault
import okhttp3.MultipartBody

class MessageRepository {
    private val api = RetrofitClient.apiService

    suspend fun getConversations(): Result<List<ConversationInfo>> = try {
        val r = api.getConversations()
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    /**
     * 拉取聊天记录。
     *
     * @param conversationId 会话的权威标识。已知时务必传入，否则服务端只能按
     *        (目标用户, 商品) 定位，同一对用户存在多个商品会话时可能读错会话。
     */
    suspend fun getMessages(
        userId: Long,
        conversationId: Long? = null,
        goodsId: Long? = null,
        page: Int = 1,
        size: Int = 20
    ): Result<PageResult<MessageInfo>> = try {
        val r = api.getMessages(userId, conversationId, goodsId, page, size)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun sendMessage(
        receiverId: Long,
        content: String,
        type: Int = 1,
        conversationId: Long? = null,
        goodsId: Long? = null
    ): Result<MessageInfo> = try {
        val r = api.sendMessage(SendMessageRequest(receiverId, conversationId, goodsId, content, type))
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun uploadFile(part: MultipartBody.Part): Result<String> = try {
        val r = api.uploadImage(part)
        if (r.isSuccess() && r.data != null) Result.success(r.data.url) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getNotifications(page: Int = 1, size: Int = 20): Result<PageResult<NotificationInfo>> = try {
        val r = api.getNotifications(page, size)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun markRead(conversationId: Long): Result<Unit> = try {
        val r = api.markRead(conversationId)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun deleteConversation(userId: Long, conversationId: Long? = null): Result<Unit> = try {
        val r = api.deleteConversation(userId, conversationId)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }
}
