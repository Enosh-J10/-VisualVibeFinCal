package com.example.visualvibefincal.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visualvibefincal.data.model.CurrencyHistoryItem
import com.example.visualvibefincal.data.model.CurrencyResponse
import com.example.visualvibefincal.domain.repository.CurrencyRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CurrencyUiState {
    object Idle : CurrencyUiState()
    object Loading : CurrencyUiState()
    data class Success(val data: CurrencyResponse) : CurrencyUiState()
    data class Error(val message: String) : CurrencyUiState()
}

class CurrencyViewModel(
    private val repository: CurrencyRepository,
    private val context: Context
) : ViewModel() {
    private val sharedPrefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _uiState = MutableStateFlow<CurrencyUiState>(CurrencyUiState.Idle)
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    private val _convertedAmount = MutableStateFlow<Double?>(null)
    val convertedAmount: StateFlow<Double?> = _convertedAmount.asStateFlow()

    private val _availableCurrencies = MutableStateFlow<List<String>>(listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD"))
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    private val _history = MutableStateFlow<List<CurrencyHistoryItem>>(emptyList())
    val history: StateFlow<List<CurrencyHistoryItem>> = _history.asStateFlow()

    private val _fromCurrency = MutableStateFlow(sharedPrefs.getString("fav_from", "USD") ?: "USD")
    val fromCurrency: StateFlow<String> = _fromCurrency.asStateFlow()

    private val _toCurrency = MutableStateFlow(sharedPrefs.getString("fav_to", "GBP") ?: "GBP")
    val toCurrency: StateFlow<String> = _toCurrency.asStateFlow()

    init {
        loadHistory()
        fetchRates(_fromCurrency.value)
    }

    fun setFromCurrency(code: String) {
        _fromCurrency.value = code
        sharedPrefs.edit().putString("fav_from", code).apply()
        fetchRates(code)
    }

    fun setToCurrency(code: String) {
        _toCurrency.value = code
        sharedPrefs.edit().putString("fav_to", code).apply()
    }

    private fun loadHistory() {
        val historyJson = sharedPrefs.getString("history", null)
        if (historyJson != null) {
            try {
                val type = object : TypeToken<List<CurrencyHistoryItem>>() {}.type
                _history.value = gson.fromJson(historyJson, type)
            } catch (e: Exception) {
                Log.e("CurrencyViewModel", "Error loading history", e)
            }
        }
    }

    private fun saveHistory(history: List<CurrencyHistoryItem>) {
        val historyJson = gson.toJson(history)
        sharedPrefs.edit().putString("history", historyJson).apply()
    }

    fun addToHistory(item: CurrencyHistoryItem) {
        val currentHistory = _history.value.toMutableList()
        currentHistory.add(0, item)
        if (currentHistory.size > 20) {
            currentHistory.removeAt(currentHistory.size - 1)
        }
        _history.value = currentHistory
        saveHistory(currentHistory)
    }

    fun clearHistory() {
        _history.value = emptyList()
        sharedPrefs.edit().remove("history").apply()
    }

    fun fetchRates(base: String) {
        val cleanBase = base.uppercase().trim()
        Log.d("CurrencyViewModel", "fetchRates initiated for: $cleanBase")
        viewModelScope.launch {
            _uiState.value = CurrencyUiState.Loading
            repository.getLatestRates(cleanBase)
                .onSuccess { response ->
                    Log.d("CurrencyViewModel", "API Response Success: ${response.result}")
                    val rates = response.allRates
                    if (rates.isNotEmpty()) {
                        _availableCurrencies.value = rates.keys.toList().sorted()
                        Log.d("CurrencyViewModel", "Updated available currencies: ${rates.size} items")
                    }
                    _uiState.value = CurrencyUiState.Success(response)
                }
                .onFailure { error ->
                    Log.e("CurrencyViewModel", "API Response Failure: ${error.message}")
                    _uiState.value = CurrencyUiState.Error(error.message ?: "Unknown Error")
                }
        }
    }

    fun convert(amount: Double, toRate: Double?) {
        Log.d("CurrencyViewModel", "convert called: amount=$amount, toRate=$toRate")
        if (toRate == null) {
            Log.w("CurrencyViewModel", "toRate is null, cannot convert")
            _convertedAmount.value = null
            return
        }
        _convertedAmount.value = amount * toRate
        Log.d("CurrencyViewModel", "convertedAmount updated: ${_convertedAmount.value}")
    }
}