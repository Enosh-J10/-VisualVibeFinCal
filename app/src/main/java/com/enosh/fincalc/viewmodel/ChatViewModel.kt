package com.enosh.fincalc.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.data.model.ChatRoom
import com.enosh.fincalc.data.model.Message
import com.enosh.fincalc.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _chats = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chats: StateFlow<List<ChatRoom>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress

    private var chatsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var statusListener: ListenerRegistration? = null

    private val _friendStatus = MutableStateFlow<String?>(null) // "Online", "Typing...", or null
    val friendStatus: StateFlow<String?> = _friendStatus

    init {
        listenToChats()
    }

    fun setTypingStatus(chatId: String, isTyping: Boolean) {
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

    fun listenToMessages(chatId: String) {
        if (chatId.isBlank()) return
        
        messagesListener?.remove()
        messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                val msgList = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()
                _messages.value = msgList
                
                // Mark as read
                val currentUid = auth.currentUser?.uid ?: return@addSnapshotListener
                msgList.forEach { msg ->
                    if (msg.senderUid != currentUid && !msg.readBy.contains(currentUid)) {
                        db.collection("chats").document(chatId)
                            .collection("messages").document(msg.messageId)
                            .update("readBy", FieldValue.arrayUnion(currentUid))
                    }
                }
            }
    }

    fun sendMessage(chatId: String, text: String, receiverUid: String, replyTo: Message? = null, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val currentUid = auth.currentUser?.uid ?: ""
        val messageId = UUID.randomUUID().toString()
        
        if (currentUid.isBlank() || receiverUid.isBlank() || chatId.isBlank() || text.isBlank() || messageId.isBlank()) {
            onResult(false, "Invalid message parameters")
            return
        }

        if (chatId.contains("/") || messageId.contains("/")) {
            onResult(false, "Invalid ID format")
            return
        }

        val messageData = mapOf(
            "messageId" to messageId,
            "chatId" to chatId,
            "senderUid" to currentUid,
            "receiverUid" to receiverUid,
            "text" to text.trim(),
            "type" to "text",
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

        viewModelScope.launch {
            try {
                val batch = db.batch()
                val chatRef = db.collection("chats").document(chatId)
                val msgRef = chatRef.collection("messages").document(messageId)

                batch.set(chatRef, chatData, com.google.firebase.firestore.SetOptions.merge())
                batch.set(msgRef, messageData)
                batch.commit().await()
                
                onResult(true, null)
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                onResult(false, "${e.code} - ${e.message}")
            } catch (e: Exception) {
                onResult(false, e.message ?: e.toString())
            }
        }
    }

    fun uploadFile(chatId: String, receiverUid: String, uri: Uri, type: String, fileName: String) {
        val currentUid = auth.currentUser?.uid ?: return
        if (chatId.isBlank()) return
        
        val messageId = UUID.randomUUID().toString()
        val path = "chat_uploads/$chatId/$messageId/$fileName"
        val storageRef = storage.getReference(path)

        _uploadProgress.value = 0f
        
        val uploadTask = storageRef.putFile(uri)
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat()
            _uploadProgress.value = progress / 100f
        }.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val messageData = mapOf(
                    "messageId" to messageId,
                    "chatId" to chatId,
                    "senderUid" to currentUid,
                    "receiverUid" to receiverUid,
                    "type" to type,
                    "text" to "Sent a $type",
                    "fileUrl" to downloadUri.toString(),
                    "fileName" to fileName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "readBy" to listOf(currentUid),
                    "deletedFor" to emptyList<String>()
                )
                
                val chatData = mapOf(
                    "chatId" to chatId,
                    "memberUids" to listOf(currentUid, receiverUid).sorted(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to "Sent a $type",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "lastMessageSenderUid" to currentUid
                )

                viewModelScope.launch {
                    try {
                        val batch = db.batch()
                        val chatRef = db.collection("chats").document(chatId)
                        val msgRef = chatRef.collection("messages").document(messageId)

                        batch.set(chatRef, chatData, com.google.firebase.firestore.SetOptions.merge())
                        batch.set(msgRef, messageData)
                        batch.commit().await()
                        _uploadProgress.value = null
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Upload commit failed", e)
                        _uploadProgress.value = null
                    }
                }
            }
        }.addOnFailureListener {
            Log.e("ChatViewModel", "Upload failed", it)
            _uploadProgress.value = null
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        if (chatId.isBlank() || messageId.isBlank()) return
        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .delete()
    }

    fun editMessage(chatId: String, messageId: String, newText: String) {
        if (chatId.isBlank() || messageId.isBlank()) return
        db.collection("chats").document(chatId)
            .collection("messages").document(messageId)
            .update("text", newText, "updatedAt", FieldValue.serverTimestamp())
    }

    suspend fun ensureChatExists(chatId: String, friendUid: String) {
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
