package com.example.visualvibefincal.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val title = inputData.getString("title") ?: "Track your expenses today 💰"
        val message = inputData.getString("message") ?: "Keep your finances in check with Visual Vibe FinCal!"
        
        NotificationHelper.showNotification(applicationContext, title, message)
        return Result.success()
    }
}