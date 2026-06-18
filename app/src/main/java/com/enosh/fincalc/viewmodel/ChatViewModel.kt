package com.enosh.fincalc.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.data.model.Chat
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

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress

    private var chatsListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    init {
        listenToChats()
    }

    suspend fun getUserProfile(uid: String): User? {
        return try {
            db.collection("users").document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("ChatDebug", "Failed to fetch user profile for $uid", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
        messagesListener?.remove()
    }

    private fun listenToChats() {
        val currentUid = auth.currentUser?.uid ?: return
        chatsListener?.remove()
        chatsListener = db.collection("chats")
            .whereArrayContains("memberUids", currentUid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Listen chats failed", e)
                    return@addSnapshotListener
                }
                val chatList = snapshot?.documents?.mapNotNull { it.toObject(Chat::class.java) } ?: emptyList()
                _chats.value = chatList
            }
    }

    fun listenToMessages(chatId: String) {
        Log.d("ChatDebug", "Listening to messages for chatId: $chatId")
        messagesListener?.remove()
        messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatDebug", "Listen messages failed for $chatId", e)
                    return@addSnapshotListener
                }
                val msgList = snapshot?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()
                Log.d("ChatDebug", "Messages received for $chatId: ${msgList.size} messages")
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

    fun sendMessage(chatId: String, text: String, receiverUid: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val authUser = auth.currentUser
        val currentUid = authUser?.uid ?: ""
        
        Log.d("ChatDebug", "auth uid = $currentUid")
        
        if (authUser == null) {
            Log.e("ChatDebug", "Send failed: user is not authenticated")
            onResult(false, "User not authenticated")
            return
        }

        if (receiverUid.isBlank()) {
            Log.e("ChatDebug", "Send failed: receiverUid is blank")
            onResult(false, "Receiver UID is blank")
            return
        }
        
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            Log.d("ChatDebug", "Send skipped: empty message")
            return
        }

        val messageId = UUID.randomUUID().toString()
        val chatDocPath = "chats/$chatId"
        val messageDocPath = "chats/$chatId/messages/$messageId"
        
        val message = Message(
            messageId = messageId,
            chatId = chatId,
            senderUid = currentUid,
            receiverUid = receiverUid,
            text = trimmedText,
            type = "text",
            createdAt = null,
            readBy = listOf(currentUid)
        )

        Log.d("ChatDebug", "send clicked: currentUid=$currentUid, receiverUid=$receiverUid, chatId=$chatId")
        Log.d("ChatDebug", "chat path = $chatDocPath")
        Log.d("ChatDebug", "message path = $messageDocPath")

        viewModelScope.launch {
            try {
                // Ensure chat exists with proper metadata before sending
                ensureChatExists(chatId, receiverUid)

                val chatRef = db.collection("chats").document(chatId)
                val msgRef = chatRef.collection("messages").document(messageId)

                db.runTransaction { transaction ->
                    transaction.set(msgRef, message)
                    transaction.update(msgRef, "createdAt", FieldValue.serverTimestamp())
                    
                    transaction.update(chatRef, mapOf(
                        "lastMessage" to trimmedText,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "lastMessageSenderUid" to currentUid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "memberUids" to listOf(currentUid, receiverUid).sorted()
                    ))
                }.await()

                Log.d("ChatDebug", "Firestore write success: ID=$messageId")
                onResult(true, null)
            } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                Log.e("ChatDebug", "Firestore error code=${e.code}, message=${e.message}", e)
                onResult(false, "Firestore error: ${e.code}")
            } catch (e: Exception) {
                Log.e("ChatDebug", "Firestore write failure: ${e.message}", e)
                onResult(false, e.message)
            }
        }
    }

    fun uploadFile(chatId: String, receiverUid: String, uri: Uri, type: String, fileName: String) {
        val currentUid = auth.currentUser?.uid ?: return
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
                val message = Message(
                    messageId = messageId,
                    senderUid = currentUid,
                    receiverUid = receiverUid,
                    type = type,
                    text = "Sent a $type",
                    fileUrl = downloadUri.toString(),
                    fileName = fileName,
                    createdAt = null
                )
                
                viewModelScope.launch {
                    try {
                        val batch = db.batch()
                        val chatRef = db.collection("chats").document(chatId)
                        val msgRef = chatRef.collection("messages").document(messageId)

                        batch.set(msgRef, message)
                        batch.update(msgRef, "createdAt", FieldValue.serverTimestamp())
                        batch.update(chatRef, mapOf(
                            "lastMessage" to "Sent a $type",
                            "lastMessageAt" to FieldValue.serverTimestamp(),
                            "lastMessageSenderUid" to currentUid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                        batch.commit().await()
                        _uploadProgress.value = null
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Save file message failed", e)
                        _uploadProgress.value = null
                    }
                }
            }
        }.addOnFailureListener {
            Log.e("ChatViewModel", "Upload failed", it)
            _uploadProgress.value = null
        }
    }

    suspend fun getOrCreateChatId(friendUid: String): String {
        val currentUid = auth.currentUser?.uid ?: return ""
        val uids = listOf(currentUid, friendUid).sorted()
        val chatId = uids.joinToString("_")
        ensureChatExists(chatId, friendUid)
        return chatId
    }

    suspend fun ensureChatExists(chatId: String, friendUid: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val uids = listOf(currentUid, friendUid).sorted()
        
        Log.d("ChatDebug", "ensureChatExists: currentUid=$currentUid, friendUid=$friendUid, chatId=$chatId")
        
        try {
            val chatRef = db.collection("chats").document(chatId)
            val doc = chatRef.get().await()
            
            Log.d("ChatDebug", "Chat document exists at ${chatRef.path}: ${doc.exists()}")
            
            if (!doc.exists()) {
                val chatData = Chat(
                    chatId = chatId,
                    memberUids = uids,
                    lastMessage = "Chat started",
                    lastMessageAt = null,
                    lastMessageSenderUid = "",
                    createdAt = null,
                    updatedAt = null
                )
                chatRef.set(chatData).await()
                chatRef.update(
                    mapOf(
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastMessageAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Log.d("ChatDebug", "Chat document created successfully: $chatId")
            }
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Log.e("ChatDebug", "ensureChatExists Firestore error code=${e.code}, message=${e.message}", e)
        } catch (e: Exception) {
            Log.e("ChatDebug", "Failed to ensure chat exists for $chatId", e)
        }
    }
}
