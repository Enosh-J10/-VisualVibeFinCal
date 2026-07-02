package com.enosh.fincalc.data.model

import com.google.firebase.Timestamp

data class TravelExpenseFlag(
    val flagId: String = "",
    val expenseId: String = "",
    val tripId: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val reasonType: String = "", // Wrong amount, Wrong payer, Wrong split, Duplicate expense, Other
    val note: String = "",
    val createdAt: Timestamp? = null,
    val status: String = "open" // open, resolved
)
