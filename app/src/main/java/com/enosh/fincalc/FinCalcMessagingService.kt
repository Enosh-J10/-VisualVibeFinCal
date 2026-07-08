package com.enosh.fincalc

import android.util.Log
import com.enosh.fincalc.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FinCalcMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("NotificationDebug", "Refreshed token: $token")
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("fcmToken", token)
                    .addOnFailureListener {
                        Log.e("NotificationDebug", "Failed to update FCM token", it)
                    }
            }
        } catch (e: Throwable) {
            Log.e("NotificationDebug", "Firebase not ready for token update", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("NotificationDebug", "onMessageReceived from: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data
            Log.d("NotificationDebug", "Message data payload: $data")
            
            val type = data["type"]
            if (type == "chat_message") {
                val senderName = data["senderName"] ?: "New Message"
                val text = data["text"] ?: "Sent an attachment"
                val chatId = data["chatId"] ?: ""
                val friendUid = data["senderUid"] ?: ""
                
                if (chatId.isNotBlank() && friendUid.isNotBlank()) {
                    NotificationHelper.showChatNotification(
                        context = this,
                        senderName = senderName,
                        messageText = text,
                        chatId = chatId,
                        friendUid = friendUid
                    )
                }
            }
        }

        remoteMessage.notification?.let {
            Log.d("NotificationDebug", "Message Notification Body: ${it.body}")
        }
    }
}
