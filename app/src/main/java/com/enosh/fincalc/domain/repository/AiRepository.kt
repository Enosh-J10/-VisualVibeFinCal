package com.enosh.fincalc.domain.repository

import com.enosh.fincalc.data.model.ChatMessage
import com.enosh.fincalc.data.model.Conversation
import kotlinx.coroutines.flow.Flow

interface AiRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(chatId: String, content: String, imageBase64: String? = null): Result<ChatMessage>
    suspend fun createConversation(title: String): Conversation
    suspend fun deleteConversation(conversation: Conversation)
    suspend fun clearAllChats()
    suspend fun updateConversationTitle(chatId: String, title: String)
}
