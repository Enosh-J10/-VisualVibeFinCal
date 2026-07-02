package com.enosh.fincalc.data.model

import com.google.firebase.Timestamp

data class TravelTrip(
    val tripId: String = "",
    val name: String = "",
    val destinationCountry: String = "",
    val destinationCity: String = "",
    val currencyCode: String = "",
    val currencySymbol: String = "",
    val createdByUid: String = "",
    val memberUids: List<String> = emptyList(),
    val invitedUids: List<String> = emptyList(),
    val memberDetails: Map<String, MemberInfo> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val description: String = "",
    val isFinalized: Boolean = false
)

data class MemberInfo(
    val name: String = "",
    val email: String = "",
    val status: String = "JOINED" // INVITED, JOINED
)
