package com.enosh.fincalc.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AiApiServiceFactory {
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    val geminiApiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}
