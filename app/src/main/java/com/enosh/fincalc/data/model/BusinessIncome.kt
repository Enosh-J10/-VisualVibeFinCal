package com.enosh.fincalc.data.model

data class BusinessIncome(
    val incomeId: String = "",
    val amount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val source: String = "",
    val reason: String = "",
    val category: String = "Other",
    val paymentMethod: String = "Cash",
    val notes: String = "",
    val attachmentUrl: String? = null,
    val uid: String = ""
)

data class BusinessTarget(
    val month: String = "", // e.g., "2024-03"
    val targetAmount: Double = 0.0,
    val uid: String = ""
)
