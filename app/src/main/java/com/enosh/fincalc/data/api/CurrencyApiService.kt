package com.enosh.fincalc.data.api

import com.enosh.fincalc.data.model.CurrencyResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApiService {
    @GET("v6/latest/{base}")
    suspend fun getLatestRates(@Path("base") base: String): Response<CurrencyResponse>

    companion object {
        const val BASE_URL = "https://open.er-api.com/"
    }
}