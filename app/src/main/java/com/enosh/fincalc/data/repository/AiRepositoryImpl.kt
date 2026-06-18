package com.enosh.fincalc.data.repository

import com.enosh.fincalc.data.api.*
import com.enosh.fincalc.data.local.AiChatDao
import com.enosh.fincalc.data.local.ConversationEntity
import com.enosh.fincalc.data.local.MessageEntity
import com.enosh.fincalc.data.model.*
import com.enosh.fincalc.domain.repository.AiRepository
import com.enosh.fincalc.BuildConfig
import retrofit2.HttpException
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

class AiRepositoryImpl(
    private val apiService: GeminiApiService,
    private val dao: AiChatDao
) : AiRepository {

    override fun getAllConversations(): Flow<List<Conversation>> {
        return dao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMessagesForChat(chatId: String): Flow<List<ChatMessage>> {
        return dao.getMessagesForChat(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun sendMessage(chatId: String, content: String, imageBase64: String?): Result<ChatMessage> {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            
            if (apiKey.isBlank()) {
                return Result.failure(Exception("Gemini API key not loaded. Please verify local.properties."))
            }

            // Save User Message
            val userMessage = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                role = MessageRole.USER,
                content = content,
                timestamp = System.currentTimeMillis(),
                provider = AiProvider.GEMINI
            )
            dao.insertMessage(userMessage)
            dao.updateConversationTimestamp(chatId, System.currentTimeMillis())

            // Prepare Gemini Request
            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = content)
                        )
                    )
                )
            )

            val models = listOf(
                AiConfig.currentGeminiModel,
                "gemini-1.5-flash",
                "gemini-1.5-flash-8b"
            ).distinct()

            var lastError: Exception? = null
            
            for (modelName in models) {
                try {
                    val response = apiService.generateContent(modelName, apiKey, request)
                    
                    val aiText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: continue

                    AiConfig.currentGeminiModel = modelName

                    val assistantMessage = MessageEntity(
                        id = UUID.randomUUID().toString(),
                        chatId = chatId,
                        role = MessageRole.ASSISTANT,
                        content = aiText,
                        timestamp = System.currentTimeMillis(),
                        provider = AiProvider.GEMINI
                    )
                    dao.insertMessage(assistantMessage)
                    dao.updateConversationTimestamp(chatId, System.currentTimeMillis())

                    return Result.success(assistantMessage.toDomain())
                } catch (e: HttpException) {
                    val code = e.code()
                    Log.e("GeminiError", "Model failed = $modelName, code = $code")
                    
                    if (code == 503) {
                        lastError = Exception("AI models are busy right now. Please try again soon.")
                        continue // Try next model
                    } else if (code == 429) {
                        return Result.failure(Exception("Rate limit reached. Please wait."))
                    } else if (code == 403) {
                        return Result.failure(Exception("Access Denied (Invalid API Key)."))
                    } else {
                        return Result.failure(Exception("Service Error ($code)"))
                    }
                } catch (e: Exception) {
                    Log.e("GeminiError", "Model failed = $modelName", e)
                    lastError = e
                    continue
                }
            }

            return Result.failure(lastError ?: Exception("All models failed"))

        } catch (e: Exception) {
            Log.e("AiRepository", "General Error", e)
            return Result.failure(e)
        }
    }

    override suspend fun createConversation(title: String): Conversation {
        val conversation = ConversationEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertConversation(conversation)
        return conversation.toDomain()
    }

    override suspend fun deleteConversation(conversation: Conversation) {
        dao.deleteConversation(conversation.toEntity())
        dao.deleteMessagesForChat(conversation.id)
    }

    override suspend fun clearAllChats() {
        dao.deleteAllConversations()
    }

    override suspend fun updateConversationTitle(chatId: String, title: String) {
        dao.updateConversationTitle(chatId, title)
    }

    private fun ConversationEntity.toDomain() = Conversation(id, title, createdAt, updatedAt)
    private fun Conversation.toEntity() = ConversationEntity(id, title, createdAt, updatedAt)
    private fun MessageEntity.toDomain() = ChatMessage(id, chatId, role, content, timestamp, provider)
}
