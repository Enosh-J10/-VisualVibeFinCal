package com.enosh.fincalc.domain.repository

import com.enosh.fincalc.data.model.CurrencyResponse

interface CurrencyRepository {
    suspend fun getLatestRates(base: String): Result<CurrencyResponse>
}