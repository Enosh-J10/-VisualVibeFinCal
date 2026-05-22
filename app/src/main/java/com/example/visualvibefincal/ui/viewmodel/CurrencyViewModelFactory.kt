package com.example.visualvibefincal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.visualvibefincal.data.api.CurrencyApiService
import com.example.visualvibefincal.data.repository.CurrencyRepositoryImpl
import com.example.visualvibefincal.domain.repository.CurrencyRepository
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CurrencyViewModelFactory : ViewModelProvider.Factory {
    private val apiService: CurrencyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(CurrencyApiService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApiService::class.java)
    }

    private val repository: CurrencyRepository by lazy {
        CurrencyRepositoryImpl(apiService)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: androidx.lifecycle.viewmodel.CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        if (modelClass.isAssignableFrom(CurrencyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CurrencyViewModel(repository, application!!) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
