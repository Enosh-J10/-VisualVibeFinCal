package com.enosh.fincalc.data.model

import com.google.firebase.Timestamp

data class ChatRoom(
    val chatId: String = "",
    val memberUids: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val lastMessageSenderUid: String = "",
    val unreadCounts: Map<String, Int> = emptyMap(), // UID to count
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val type: String = "text", // text, image, video, document
    val text: String = "",
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileMimeType: String? = null,
    val fileSize: Long = 0,
    val createdAt: Timestamp? = null,
    val readBy: List<String> = emptyList(),
    val deletedFor: List<String> = emptyList(),
    val replyToId: String? = null,
    val replyToText: String? = null
)
