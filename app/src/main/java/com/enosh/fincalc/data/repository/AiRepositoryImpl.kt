package com.enosh.fincalc.data.repository

import com.enosh.fincalc.data.api.*
import com.enosh.fincalc.data.local.AiChatDao
import com.enosh.fincalc.data.local.ConversationEntity
import com.enosh.fincalc.data.local.MessageEntity
import com.enosh.fincalc.data.model.*
import com.enosh.fincalc.domain.repository.AiRepository
import com.enosh.fincalc.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import retrofit2.HttpException
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

class AiRepositoryImpl(
    private val apiService: GeminiApiService,
    private val dao: AiChatDao,
    private val context: Context
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

            // Fetch previous messages for context (last 10 messages)
            val previousMessages = dao.getMessagesForChatOnce(chatId).filter { it.id != userMessage.id }.takeLast(10)
            
            val isRoastMode = context.getSharedPreferences("AssistantPrefs_${FirebaseAuth.getInstance().currentUser?.uid ?: "guest"}", android.content.Context.MODE_PRIVATE).getBoolean("isRoastMode", false)

            val contents = mutableListOf<Content>()
            var systemInstruction: Content? = null
            
            if (isRoastMode) {
                systemInstruction = Content(
                    parts = listOf(Part(text = "You are a playful, sarcastic, and funny financial assistant. You love to lightly roast the user about their finances, BMI, or math skills, but you NEVER use hate speech, slurs, sexual insults, body-shaming, threats, or abuse. Keep it safe but savage. If the user asks for health advice (like BMI), tease them but don't be abusive. Example: 'Your BMI is high. Time to treat stairs like a side quest.'"))
                )
            }

            previousMessages.forEach { msg ->
                contents.add(
                    Content(
                        role = if (msg.role == MessageRole.USER) "user" else "model",
                        parts = listOf(Part(text = msg.content))
                    )
                )
            }
            
            // Prepare Current Parts (Text + optional Image)
            val currentParts = mutableListOf<Part>()
            if (content.isNotBlank()) {
                currentParts.add(Part(text = content))
            }
            if (imageBase64 != null) {
                currentParts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)))
            }

            // Add current message to context
            contents.add(
                Content(
                    role = "user",
                    parts = currentParts
                )
            )

            // Prepare Gemini Request
            val request = GeminiRequest(
                contents = contents,
                generationConfig = GenerationConfig(
                    temperature = 0.7,
                    topK = 40,
                    topP = 0.95,
                    maxOutputTokens = 1024
                ),
                systemInstruction = systemInstruction
            )

            val models = listOf("gemini-flash-latest")

            var lastError: Exception? = null
            
            for (modelName in models) {
                try {
                    val response = apiService.generateContent(modelName, apiKey, request)
                    
                    val aiText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: continue

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
                    } else if (code == 429) {
                        return Result.failure(Exception("AI service temporarily unavailable (Rate Limit)."))
                    } else if (code == 404) {
                        lastError = Exception("Model not found ($modelName).")
                    } else if (code == 403) {
                        return Result.failure(Exception("Access Denied (Invalid API Key or Model restricted)."))
                    } else {
                        lastError = Exception("Service Error ($code)")
                    }
                } catch (e: Exception) {
                    Log.e("GeminiError", "Model failed = $modelName", e)
                    lastError = e
                }
            }

            return Result.failure(lastError ?: Exception("AI models failed to respond. Please check your connection."))

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
