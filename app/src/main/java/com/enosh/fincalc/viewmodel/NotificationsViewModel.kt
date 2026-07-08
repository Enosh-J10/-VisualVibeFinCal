package com.enosh.fincalc.viewmodel

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.enosh.fincalc.utils.UserUtils
import com.enosh.fincalc.worker.FunReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

enum class ReminderFrequency {
    LOW, MEDIUM, HIGH
}

data class NotificationSettings(
    val funRemindersEnabled: Boolean = true,
    val frequency: ReminderFrequency = ReminderFrequency.MEDIUM
)

class NotificationsViewModel : ViewModel() {
    private val _settings = MutableStateFlow(NotificationSettings())
    val settings = _settings.asStateFlow()

    private val WORK_NAME = "FinCalcFunReminderWorker"

    fun loadSettings(context: Context) {
        val uid = UserUtils.getEffectiveUid(context)
        val sharedPref = context.getSharedPreferences(UserUtils.PREFS_NAME, Context.MODE_PRIVATE)
        _settings.value = NotificationSettings(
            funRemindersEnabled = sharedPref.getBoolean("notification_fun_enabled_$uid", true),
            frequency = try { ReminderFrequency.valueOf(sharedPref.getString("notification_fun_frequency_$uid", ReminderFrequency.MEDIUM.name) ?: ReminderFrequency.MEDIUM.name) } catch (e: Exception) { ReminderFrequency.MEDIUM }
        )
        
        if (_settings.value.funRemindersEnabled) {
            scheduleWorker(context, ExistingPeriodicWorkPolicy.KEEP)
        }
    }

    fun setFunRemindersEnabled(enabled: Boolean, context: Context) {
        val uid = UserUtils.getEffectiveUid(context)
        val sharedPref = context.getSharedPreferences(UserUtils.PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { putBoolean("notification_fun_enabled_$uid", enabled) }
        _settings.value = _settings.value.copy(funRemindersEnabled = enabled)
        
        if (enabled) {
            scheduleWorker(context, ExistingPeriodicWorkPolicy.REPLACE)
        } else {
            cancelWorker(context)
        }
    }

    fun setFrequency(frequency: ReminderFrequency, context: Context) {
        val uid = UserUtils.getEffectiveUid(context)
        val sharedPref = context.getSharedPreferences(UserUtils.PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { putString("notification_fun_frequency_$uid", frequency.name) }
        _settings.value = _settings.value.copy(frequency = frequency)
        
        if (_settings.value.funRemindersEnabled) {
            scheduleWorker(context, ExistingPeriodicWorkPolicy.REPLACE)
        }
    }

    private fun scheduleWorker(context: Context, policy: ExistingPeriodicWorkPolicy) {
        try {
            val intervalHours = when (_settings.value.frequency) {
                ReminderFrequency.LOW -> 24L
                ReminderFrequency.MEDIUM -> 5L
                ReminderFrequency.HIGH -> 3L
            }

            val workRequest = PeriodicWorkRequestBuilder<FunReminderWorker>(intervalHours, TimeUnit.HOURS)
                .setInitialDelay(intervalHours / 2, TimeUnit.HOURS) 
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
                workRequest
            )
        } catch (e: Throwable) {
            android.util.Log.e("NotificationsVM", "Failed to schedule worker", e)
        }
    }

    private fun cancelWorker(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        } catch (e: Throwable) {
            android.util.Log.e("NotificationsVM", "Failed to cancel worker", e)
        }
    }
}
