package com.enosh.fincalc.data.model

import java.util.UUID

enum class AiProvider {
    GEMINI,
    OPENAI,
    CLAUDE,
    GROK,
    DEEPSEEK
}

object AiConfig {
    var provider = AiProvider.GEMINI
    var currentGeminiModel = "gemini-flash-latest"
    var streamingMode = true
    var saveHistory = true
    var voiceReplies = false
    var markdownRendering = true
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: AiProvider = AiConfig.provider
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

sealed class AiChatUiState {
    object Idle : AiChatUiState()
    object Loading : AiChatUiState()
    data class Success(val messages: List<ChatMessage>) : AiChatUiState()
    data class Error(val message: String, val messages: List<ChatMessage> = emptyList()) : AiChatUiState()
}
