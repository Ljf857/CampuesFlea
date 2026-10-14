package com.example.campusflea.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["conversationKey", "remoteId"], unique = true)]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val remoteId: Long = 0,
    val conversationKey: String,
    val conversationId: Long = 0,
    val senderId: Long = 0,
    val receiverId: Long = 0,
    val goodsId: Long = 0,
    val content: String = "",
    val type: Int = 1,
    val typeDesc: String = "",
    val isRead: Int = 0,
    val createTime: String = "",
    val isMine: Boolean = false,
    val senderName: String = "",
    val senderAvatar: String = ""
)
