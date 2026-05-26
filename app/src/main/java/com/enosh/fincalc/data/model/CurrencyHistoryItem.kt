package com.enosh.fincalc.data.model

data class CurrencyHistoryItem(
    val fromCode: String,
    val toCode: String,
    val amount: Double,
    val result: Double,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis()
)
