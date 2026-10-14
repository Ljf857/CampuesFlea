package com.example.campusflea.data.model

import com.google.gson.annotations.SerializedName
import kotlin.jvm.Transient

/**
 * 本地发送状态。
 *
 * 仅存在于客户端，不参与网络序列化：服务端不返回该字段，Room 也不持久化它——
 * 进程重启后所有未确认的消息都不该继续显示「发送中」，那会变成永远转不完的圈。
 */
enum class MessageSendState {
    /** 已交给 WebSocket，等待服务端回推确认 */
    SENDING,

    /** 服务端已确认（回推了带服务端 id 的消息） */
    SENT,

    /** 连接不可用或超时未确认，可点击重试 */
    FAILED
}

data class ConversationInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("targetUser") val targetUser: TargetUser? = null,
    @SerializedName("lastMessage") val lastMessage: String = "",
    @SerializedName("lastTime") val lastTime: String = "",
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("goodsId") val goodsId: Long = 0,
    @SerializedName("goodsTitle") val goodsTitle: String = ""
)

data class TargetUser(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("avatar") val avatar: String = ""
)

data class MessageInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("conversationId") val conversationId: Long = 0,
    @SerializedName("senderId") val senderId: Long = 0,
    @SerializedName("receiverId") val receiverId: Long = 0,
    @SerializedName("goodsId") val goodsId: Long = 0,
    @SerializedName("content") val content: String = "",
    @SerializedName("type") val type: Int = 1,
    @SerializedName("typeDesc") val typeDesc: String = "",
    @SerializedName("isRead") val isRead: Int = 0,
    @SerializedName("createTime") val createTime: String = "",
    @SerializedName("isMine") val isMine: Boolean = false,
    @SerializedName("senderName") val senderName: String = "",
    @SerializedName("senderAvatar") val senderAvatar: String = "",
    /** 本地临时 ID（负数）。乐观发送期间用于定位这一条，服务端确认后清零。不参与序列化。 */
    @Transient val localId: Long = 0,
    /** 本地发送状态。不参与序列化。 */
    @Transient val sendState: MessageSendState = MessageSendState.SENT
)

data class SendMessageRequest(
    @SerializedName("receiverId") val receiverId: Long,
    @SerializedName("conversationId") val conversationId: Long? = null,
    @SerializedName("goodsId") val goodsId: Long? = null,
    @SerializedName("content") val content: String,
    @SerializedName("type") val type: Int = 1
)

data class NotificationInfo(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("content") val content: String = "",
    @SerializedName("type") val type: Int = 1,
    @SerializedName("typeDesc") val typeDesc: String = "",
    @SerializedName("bizType") val bizType: String = "",
    @SerializedName("bizId") val bizId: Long = 0,
    @SerializedName("isRead") val isRead: Int = 0,
    @SerializedName("createTime") val createTime: String = ""
)
