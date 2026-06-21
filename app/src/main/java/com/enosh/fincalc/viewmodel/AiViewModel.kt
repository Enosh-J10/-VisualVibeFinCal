package com.enosh.fincalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.data.model.*
import com.enosh.fincalc.domain.repository.AiRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class AiViewModel(private val repository: AiRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            repository.getAllConversations().collect {
                _conversations.value = it
            }
        }
    }

    fun selectChat(chatId: String) {
        _currentChatId.value = chatId
        viewModelScope.launch {
            repository.getMessagesForChat(chatId).collect { messages ->
                _uiState.value = AiChatUiState.Success(messages)
            }
        }
    }

    fun startNewChat() {
        _uiState.value = AiChatUiState.Idle
        viewModelScope.launch {
            val newConversation = repository.createConversation("New Chat ${System.currentTimeMillis()}")
            selectChat(newConversation.id)
        }
    }

    fun clearError() {
        if (_uiState.value is AiChatUiState.Error) {
            val currentMessages = (_uiState.value as? AiChatUiState.Success)?.messages ?: emptyList()
            _uiState.value = if (currentMessages.isEmpty()) AiChatUiState.Idle else AiChatUiState.Success(currentMessages)
        }
    }

    fun sendMessage(content: String, imageBase64: String? = null) {
        val chatId = _currentChatId.value ?: return
        if (content.isBlank() && imageBase64 == null) return

        viewModelScope.launch {
            _isTyping.value = true
            val result = repository.sendMessage(chatId, content, imageBase64)
            _isTyping.value = false
            
            if (result.isFailure) {
                // Handle error
                val currentMessages = (_uiState.value as? AiChatUiState.Success)?.messages 
                                     ?: (_uiState.value as? AiChatUiState.Error)?.messages 
                                     ?: emptyList()
                _uiState.value = AiChatUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error", currentMessages)
            } else {
                // Check if it's the first message, maybe rename the chat
                val messages = (_uiState.value as? AiChatUiState.Success)?.messages ?: emptyList()
                if (messages.size <= 2) { // 1 user + 1 ai
                    val title = if (content.length > 20) content.take(20) + "..." else content
                    repository.updateConversationTitle(chatId, title)
                }
            }
        }
    }

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            repository.deleteConversation(conversation)
            if (_currentChatId.value == conversation.id) {
                _currentChatId.value = null
                _uiState.value = AiChatUiState.Idle
            }
        }
    }

    fun renameConversation(conversation: Conversation, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.updateConversationTitle(conversation.id, newTitle)
        }
    }
    
    fun clearAllChats() {
        viewModelScope.launch {
            repository.clearAllChats()
            _currentChatId.value = null
            _uiState.value = AiChatUiState.Idle
        }
    }
}
