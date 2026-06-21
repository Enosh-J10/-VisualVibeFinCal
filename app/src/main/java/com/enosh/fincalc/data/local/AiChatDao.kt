package com.enosh.fincalc.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)

    @Query("DELETE FROM ai_conversations")
    suspend fun deleteAllConversations()

    @Query("SELECT * FROM ai_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatOnce(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM ai_messages WHERE chatId = :chatId ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRecentMessages(chatId: String, limit: Int): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM ai_messages WHERE chatId = :chatId")
    suspend fun deleteMessagesForChat(chatId: String)

    @Query("UPDATE ai_conversations SET title = :title WHERE id = :chatId")
    suspend fun updateConversationTitle(chatId: String, title: String)

    @Query("UPDATE ai_conversations SET updatedAt = :timestamp WHERE id = :chatId")
    suspend fun updateConversationTimestamp(chatId: String, timestamp: Long)
}
