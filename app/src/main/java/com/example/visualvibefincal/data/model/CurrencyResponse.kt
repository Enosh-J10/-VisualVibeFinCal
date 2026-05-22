package com.example.visualvibefincal.data.model

import com.google.gson.annotations.SerializedName

data class CurrencyResponse(
    val result: String,
    @SerializedName("time_last_update_utc")
    val timeLastUpdateUtc: String? = null,
    @SerializedName("base_code")
    val baseCode: String? = null,
    @SerializedName("conversion_rates")
    val conversionRates: Map<String, Double>? = null,
    @SerializedName("rates")
    val rates: Map<String, Double>? = null,
    @SerializedName("error-type")
    val errorType: String? = null
) {
    // Helper to get rates regardless of which field the API used
    val allRates: Map<String, Double>
        get() = conversionRates ?: rates ?: emptyMap()
}