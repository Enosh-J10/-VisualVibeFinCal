package com.enosh.fincalc.worker

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enosh.fincalc.utils.NotificationHelper
import com.enosh.fincalc.utils.ReminderCategory
import com.enosh.fincalc.utils.ReminderMessages
import com.enosh.fincalc.utils.UserUtils
import java.util.Calendar

class FunReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val sharedPref = context.getSharedPreferences(UserUtils.PREFS_NAME, Context.MODE_PRIVATE)
        val currentUid = UserUtils.getEffectiveUid(context)
        
        val isEnabled = sharedPref.getBoolean("notification_fun_enabled_$currentUid", true)
        if (!isEnabled) return Result.success()

        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Quiet hours: 10 PM to 8 AM
        if (hour < 8 || hour >= 22) {
            Log.d("FunReminderWorker", "Skipping reminder due to quiet hours: $hour")
            return Result.success()
        }

        val isGuest = sharedPref.getBoolean("is_guest", false)
        val assistantPrefName = if (isGuest) "AssistantPrefs_guest" else "AssistantPrefs_$currentUid"
        val assistantPref = context.getSharedPreferences(assistantPrefName, Context.MODE_PRIVATE)
        val isRoastMode = assistantPref.getBoolean("isRoastMode", false)

        val categories = if (isGuest) {
            listOf(ReminderCategory.EXPENSE, ReminderCategory.BUDGET, ReminderCategory.NOTES, ReminderCategory.UNIT_CONVERTER)
        } else {
            ReminderCategory.entries.toList()
        }

        val category = categories.random()
        var message = ReminderMessages.getRandomMessage(category, isRoastMode)
        
        val lastMessage = sharedPref.getString("last_fun_reminder_text_$currentUid", "")
        if (message == lastMessage) {
            // Try one more time with a different category to be extra random
            val fallbackCategory = categories.filter { it != category }.randomOrNull() ?: category
            message = ReminderMessages.getRandomMessage(fallbackCategory, isRoastMode)
        }

        NotificationHelper.showFunReminderNotification(context, category, message)
        
        sharedPref.edit {
            putString("last_fun_reminder_text_$currentUid", message)
        }
        Log.d("FunReminderWorker", "Notification shown: $message")

        return Result.success()
    }
}
