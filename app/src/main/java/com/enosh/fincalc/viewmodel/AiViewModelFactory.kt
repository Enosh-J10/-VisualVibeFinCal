package com.enosh.fincalc.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.enosh.fincalc.data.api.AiApiServiceFactory
import com.enosh.fincalc.data.local.AppDatabase
import com.enosh.fincalc.data.repository.AiRepositoryImpl

class AiViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = AiRepositoryImpl(AiApiServiceFactory.geminiApiService, database.aiChatDao(), context.applicationContext)
            @Suppress("UNCHECKED_CAST")
            return AiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
