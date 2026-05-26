package com.enosh.fincalc.data.model

data class HistoryItem(
    val title: String,
    val result: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
