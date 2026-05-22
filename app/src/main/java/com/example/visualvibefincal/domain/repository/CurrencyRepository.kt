package com.example.visualvibefincal.domain.repository

import com.example.visualvibefincal.data.model.CurrencyResponse

interface CurrencyRepository {
    suspend fun getLatestRates(base: String): Result<CurrencyResponse>
}