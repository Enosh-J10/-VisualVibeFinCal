package com.enosh.fincalc.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class FriendRequest(
    val requestId: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromEmail: String = "",
    val fromFinCalcId: String = "",
    val toUid: String = "",
    val toName: String = "",
    val toEmail: String = "",
    val toFinCalcId: String = "",
    val status: String = "pending", // pending, accepted, rejected
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
