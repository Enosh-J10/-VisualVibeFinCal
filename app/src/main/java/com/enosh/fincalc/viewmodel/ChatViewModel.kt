package com.enosh.fincalc.viewmodel

import android.util.Log
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.utils.NotificationHelper
import com.enosh.fincalc.data.model.ChatRoom
import com.enosh.fincalc.data.model.Message
import com.enosh.fincalc.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        logFirebaseDiagnostic("Init")
        NotificationHelper.createNotificationChannel(application)
    }

    private fun logFirebaseDiagnostic(tag: String) {
        try {
            val app = FirebaseApp.getInstance()
            val user = auth.currentUser
            Log.d("FirebaseDiagnostic", "[$tag] ProjectId: ${app.options.projectId}")
            Log.d("FirebaseDiagnostic", "[$tag] AppId: ${app.options.applicationId}")
            Log.d("FirebaseDiagnostic", "[$tag] Uid: ${user?.uid}")
            Log.d("FirebaseDiagnostic", "[$tag] Email: ${user?.email}")
            Log.d("FirebaseDiagnostic", "[$tag] IsAnonymous: ${user?.isAnonymous}")
            Log.d("FirebaseDiagnostic", "[$tag] PackageName: com.enosh.fincalc")
        } catch (e: Exception) {
            Log.e("FirebaseDiagnostic", "[$tag] Failed to log diagnostic", e)
        }
    }

    private val _chats = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chats: StateFlow<List<ChatRoom>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var chatsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var statusListener: ListenerRegistration? = null

    private val _friendStatus = MutableStateFlow<String?>(null) // "Online", "Typing...", or null
    val friendStatus: StateFlow<String?> = _friendStatus

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    var currentlyOpenChatId: String? = null

    fun clearError() {
        _errorMessage.value = null
    }

    init {
        listenToChats()
    }

    fun setTypingStatus(chatId: String, isTyping: Boolean) {
        logFirebaseDiagnostic("setTypingStatus")
        val currentUid = auth.currentUser?.uid ?: return
        if (chatId.isBlank()) return
        
        db.collection("chats").document(chatId)
            .collection("status").document(currentUid)
            .set(mapOf(
                "isTyping" to isTyping,
                "lastActive" to FieldValue.serverTimestamp()
            ))
    }

    fun listenToFriendStatus(chatId: String, friendUid: String) {
        logFirebaseDiagnostic("listenToFriendStatus")
        if (chatId.isBlank() || friendUid.isBlank()) return
        
        statusListener?.remove()
        statusListener = db.collection("chats").document(chatId)
            .collection("status").document(friendUid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val isTyping = snapshot.getBoolean("isTyping") ?: false
                    val lastActive = snapshot.getTimestamp("lastActive")
                    
                    if (isTyping) {
                        _friendStatus.value = "Typing..."
                    } else if (lastActive != null) {
                        val diff = System.currentTimeMillis() - lastActive.toDate().time
                        if (diff < 60000) { // 1 minute
                            _friendStatus.value = "Online"
                        } else {
                            _friendStatus.value = null
                        }
                    } else {
                        _friendStatus.value = null
                    }
                } else {
                    _friendStatus.value = null
                }
            }
    }

    suspend fun getUserProfile(uid: String): User? {
        logFirebaseDiagnostic("getUserProfile")
        if (uid.isBlank()) return null
        return try {
            db.collection("users").document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to fetch user profile for $uid", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
        messagesListener?.remove()
        statusListener?.remove()
    }

    private fun listenToChats() {
        logFirebaseDiagnostic("listenToChats")
        val currentUid = auth.currentUser?.uid ?: return
        chatsListener?.remove()
        chatsListener = db.collection("chats")
            .whereArrayContains("memberUids", currentUid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                val chatList = snapshot?.documents?.mapNotNull { it.toObject(ChatRoom::class.java) } ?: emptyList()
                _chats.value = chatList
            }
    }

    private var isFirstLoad = true
    private val notifiedMessageIds = mutableSetOf<String>()

    fun listenToMessages(chatId: String) {
        logFirebaseDiagnostic("listenToMessages")
        if (chatId.isBlank()) return
        
        messagesListener?.remove()
        isFirstLoad = true
        
        messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                
                val msgList = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()
                _messages.value = msgList

                val currentUid = auth.currentUser?.uid ?: return@addSnapshotListener

                // Initialize notified IDs on first load to prevent notifying history
                if (isFirstLoad) {
                    msgList.forEach { notifiedMessageIds.add(it.messageId) }
                    isFirstLoad = false
                    Log.d("NotificationDebug", "Initial messages loaded: ${notifiedMessageIds.size}")
                }

                // Mark as read
                msgList.forEach { msg ->
                    if (msg.senderUid != currentUid && !msg.readBy.contains(currentUid)) {
                        db.collection("chats").document(chatId)
                            .collection("messages").document(msg.messageId)
                            .update("readBy", FieldValue.arrayUnion(currentUid))
                    }
                }

                // Notification logic for truly new messages
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val msg = change.document.toObject(Message::class.java)
                        if (msg.senderUid != currentUid && !notifiedMessageIds.contains(msg.messageId)) {
                            notifiedMessageIds.add(msg.messageId)
                            
                            // Don't notify if user is already in this chat room
                            if (msg.chatId == currentlyOpenChatId) {
                                Log.d("NotificationDebug", "Suppressing notification: user in chat $currentlyOpenChatId")
                                return@forEach
                            }
                            
                            Log.d("NotificationDebug", "incomingMessageDetected: ${msg.messageId} from ${msg.senderUid}")
                            
                            viewModelScope.launch {
                                val senderProfile = getUserProfile(msg.senderUid)
                                val senderName = senderProfile?.name ?: "New Message"
                                val previewText = when(msg.type) {
                                    "text" -> msg.text
                                    "image" -> "Sent an image"
                                    "video" -> "Sent a video"
                                    else -> "Sent a file"
                                }
                                NotificationHelper.showChatNotification(
                                    context = getApplication(),
                                    senderName = senderName,
                                    messageText = previewText,
                                    chatId = chatId,
                                    friendUid = msg.senderUid
                                )
                            }
                        }
                    }
                }
            }
    }

    fun sendMessage(chatId: String, text: String, receiverUid: String, replyTo: Message? = null, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        logFirebaseDiagnostic("sendMessage")
        val currentUid = auth.currentUser?.uid ?: ""
        
        if (currentUid.isBlank()) {
            onResult(false, "Please sign in again.")
            return
        }

        if (receiverUid.isBlank() || chatId.isBlank() || text.isBlank()) {
            onResult(false, "Invalid message parameters")
            return
        }

        val messageId = UUID.randomUUID().toString()

        val messageData = mapOf(
            "messageId" to messageId,
            "chatId" to chatId,
            "senderUid" to currentUid,
            "receiverUid" to receiverUid,
            "text" to text.trim(),
            "type" to "text",
            "fileUrl" to "",
            "fileName" to "",
            "fileMimeType" to "",
            "fileSize" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "readBy" to listOf(currentUid),
            "deletedFor" to emptyList<String>(),
            "replyToId" to replyTo?.messageId,
            "replyToText" to replyTo?.text
        )

        val chatData = mapOf(
            "chatId" to chatId,
            "memberUids" to listOf(currentUid, receiverUid).sorted(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastMessage" to text.trim(),
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "lastMessageSenderUid" to currentUid
        )

        Log.d("AttachmentDebug", "sendMessage: authUid=$currentUid, chatId=$chatId, receiverUid=$receiverUid")

        viewModelScope.launch {
            try {
                val batch = db.batch()
                val chatRef = db.collection("chats").document(chatId)
                val msgRef = chatRef.collection("messages").document(messageId)

                batch.set(chatRef, chatData, com.google.firebase.firestore.SetOptions.merge())
                batch.set(msgRef, messageData)
                batch.commit().await()
                
                // For background notification trigger (Cloud Function) - decoupled from message send success
                launch {
                    try {
                        val notificationRef = db.collection("notifications").document(receiverUid)
                            .collection("items").document(messageId)
                        
                        val notificationData = mapOf(
                            "notificationId" to messageId,
                            "type" to "chat_message",
                            "chatId" to chatId,
                            "fromUid" to currentUid,
                            "toUid" to receiverUid,
                            "title" to "New Message", 
                            "body" to text.trim(),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "read" to false
                        )
                        notificationRef.set(notificationData).await()
                    } catch (e: Exception) {
                        Log.e("NotificationDebug", "Failed to create notification document", e)
                    }
                }
                
                Log.d("AttachmentDebug", "sendMessage: Succeeded!")
                onResult(true, null)
            } catch (e: FirebaseFirestoreException) {
                val errorMsg = "${e.code} - ${e.message}"
                Log.e("AttachmentDebug", "sendMessage: Firestore failed - $errorMsg", e)
                onResult(false, "Send failed: $errorMsg")
            } catch (e: Exception) {
                val errorMsg = "${e.javaClass.simpleName} - ${e.message}"
                Log.e("AttachmentDebug", "sendMessage: General failure - $errorMsg", e)
                onResult(false, "Send failed: $errorMsg")
            }
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        logFirebaseDiagnostic("deleteMessage")
        if (chatId.isBlank() || messageId.isBlank()) return
        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .delete()
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
        logFirebaseDiagnostic("editMessage")
        if (chatId.isBlank() || messageId.isBlank()) return
        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .update("text", newText, "updatedAt", FieldValue.serverTimestamp())
    }

    suspend fun ensureChatExists(chatId: String, friendUid: String) {
        logFirebaseDiagnostic("ensureChatExists")
        val currentUid = auth.currentUser?.uid ?: return
        if (chatId.isBlank() || friendUid.isBlank()) return
        
        try {
            val chatRef = db.collection("chats").document(chatId)
            val doc = chatRef.get().await()
            
            if (!doc.exists()) {
                val chatData = mapOf(
                    "chatId" to chatId,
                    "memberUids" to listOf(currentUid, friendUid).sorted(),
                    "lastMessage" to "Chat started",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastMessageSenderUid" to ""
                )
                chatRef.set(chatData).await()
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to ensure chat exists for $chatId", e)
        }
    }
}
