package com.enosh.fincalc.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.enosh.fincalc.data.model.AiProvider
import com.enosh.fincalc.data.model.MessageRole

@Entity(tableName = "ai_conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val uid: String = "guest"
)

@Entity(tableName = "ai_messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val provider: AiProvider
)
