package com.example.campusflea.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM chat_messages WHERE conversationKey = :conversationKey ORDER BY createTime ASC, localId ASC")
    fun getMessages(conversationKey: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationKey = :conversationKey ORDER BY createTime ASC, localId ASC")
    suspend fun getMessagesOnce(conversationKey: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE conversationKey = :conversationKey")
    suspend fun deleteConversation(conversationKey: String)

    @Query("DELETE FROM chat_messages WHERE remoteId = :remoteId")
    suspend fun deleteByRemoteId(remoteId: Long)
}
