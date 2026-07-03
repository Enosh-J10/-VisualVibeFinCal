package com.enosh.fincalc.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.enosh.fincalc.HomeActivity
import com.enosh.fincalc.R

object NotificationHelper {
    private const val CHAT_CHANNEL_ID = "chat_messages"
    private const val CHAT_CHANNEL_NAME = "Chat Messages"
    private const val GENERAL_CHANNEL_ID = "general_notifications"
    private const val GENERAL_CHANNEL_NAME = "General Notifications"
    private const val FUN_CHANNEL_ID = "fun_reminders"
    private const val FUN_CHANNEL_NAME = "Fun Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Chat channel
            val chatChannel = NotificationChannel(CHAT_CHANNEL_ID, CHAT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for incoming chat messages"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(chatChannel)

            // General channel
            val generalChannel = NotificationChannel(GENERAL_CHANNEL_ID, GENERAL_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "General app notifications"
            }
            notificationManager.createNotificationChannel(generalChannel)

            // Fun Reminders channel
            val funChannel = NotificationChannel(FUN_CHANNEL_ID, FUN_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily reminders to keep you on track"
            }
            notificationManager.createNotificationChannel(funChannel)
        }
    }

    fun showFunReminderNotification(context: Context, category: ReminderCategory, message: String) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", category.navigateTo)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, category.ordinal, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, FUN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calc)
            .setContentTitle("FinCalc Reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notify(category.ordinal + 1000, builder.build())
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationDebug", "Failed to show fun reminder", e)
            }
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calc)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                if (androidx.core.app.ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    notify(title.hashCode(), builder.build())
                }
            } catch (e: SecurityException) {
                android.util.Log.e("NotificationDebug", "Permission denied for general notification")
            }
        }
    }

    fun showChatNotification(context: Context, senderName: String, messageText: String, chatId: String, friendUid: String) {
        android.util.Log.d("NotificationDebug", "incomingMessageDetected: from $senderName")
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chatId", chatId)
            putExtra("friendUid", friendUid)
            putExtra("navigate_to", "chat_room")
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_calc)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        with(NotificationManagerCompat.from(context)) {
            try {
                var granted = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    granted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                
                android.util.Log.d("NotificationDebug", "permissionGranted: $granted")
                
                if (granted) {
                    notify(chatId.hashCode(), builder.build())
                    android.util.Log.d("NotificationDebug", "notificationPosted: $chatId")
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationDebug", "Failed to post notification", e)
            }
        }
    }
}
