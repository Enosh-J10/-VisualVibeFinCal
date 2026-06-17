package com.enosh.fincalc.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.core.content.edit
import com.enosh.fincalc.data.model.HistoryItem
import com.enosh.fincalc.utils.UserUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val uid = UserUtils.getEffectiveUid(application)
    private val sharedPrefs = application.getSharedPreferences("app_history_prefs_$uid", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _histories = MutableStateFlow<Map<String, List<HistoryItem>>>(emptyMap())
    val histories: StateFlow<Map<String, List<HistoryItem>>> = _histories.asStateFlow()

    init {
        loadAllHistories()
    }

    private fun loadAllHistories() {
        val allPrefs = sharedPrefs.all
        val loadedHistories = mutableMapOf<String, List<HistoryItem>>()
        allPrefs.forEach { (key, value) ->
            if (value is String) {
                try {
                    val type = object : TypeToken<List<HistoryItem>>() {}.type
                    val historyList: List<HistoryItem> = gson.fromJson(value, type)
                    loadedHistories[key] = historyList
                } catch (_: Exception) {
                    // Ignore malformed history
                }
            }
        }
        _histories.value = loadedHistories
    }

    fun addToHistory(screenKey: String, item: HistoryItem) {
        val currentList = _histories.value[screenKey]?.toMutableList() ?: mutableListOf()
        currentList.add(0, item)
        if (currentList.size > 20) {
            currentList.removeAt(currentList.size - 1)
        }
        
        val newHistories = _histories.value.toMutableMap()
        newHistories[screenKey] = currentList
        _histories.value = newHistories

        val json = gson.toJson(currentList)
        sharedPrefs.edit { putString(screenKey, json) }
    }

    fun clearHistory(screenKey: String) {
        val newHistories = _histories.value.toMutableMap()
        newHistories.remove(screenKey)
        _histories.value = newHistories
        sharedPrefs.edit { remove(screenKey) }
    }
}
