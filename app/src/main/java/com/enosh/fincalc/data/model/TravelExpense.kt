package com.enosh.fincalc.data.model

data class TravelExpense(
    val expenseId: String = "",
    val tripId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val currencyCode: String = "",
    val currencySymbol: String = "",
    val paidByUid: String = "",
    val createdByUid: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val category: String = "Other",
    val notes: String = "",
    val receiptUrl: String? = null,
    val splitType: String = "EQUAL", // EQUAL, CUSTOM, EXCLUDE
    val customSplits: Map<String, Double> = emptyMap(), // UID to amount
    val excludedMembers: List<String> = emptyList(), // List of UIDs
    val originalAmount: Double = 0.0,
    val originalCurrency: String = ""
)
