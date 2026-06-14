package com.enosh.fincalc.data.model

data class TravelExpense(
    val id: String = "",
    val tripId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "", // UID
    val date: Long = 0,
    val category: String = "Other",
    val notes: String = "",
    val receiptUrl: String? = null,
    val splitType: String = "EQUAL", // EQUAL, CUSTOM, EXCLUDE
    val customSplits: Map<String, Double> = emptyMap(), // UID to amount
    val excludedMembers: List<String> = emptyList() // List of UIDs
)
