package com.enosh.fincalc.data.repository

import com.enosh.fincalc.data.api.CurrencyApiService
import com.enosh.fincalc.data.model.CurrencyResponse
import com.enosh.fincalc.domain.repository.CurrencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.IOException
import java.net.UnknownHostException

class CurrencyRepositoryImpl(private val apiService: CurrencyApiService) : CurrencyRepository {
    override suspend fun getLatestRates(base: String): Result<CurrencyResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getLatestRates(base)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.result == "error") {
                    Result.failure(Exception("API Error: ${body.errorType ?: "Unknown"}"))
                } else {
                    Result.success(body)
                }
            } else {
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: IOException) {
            Result.failure(Exception("Network error. Please try again later."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}