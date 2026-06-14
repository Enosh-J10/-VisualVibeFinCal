package com.enosh.fincalc.data.model

data class TravelTrip(
    val id: String = "",
    val name: String = "",
    val destination: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val currency: String = "INR",
    val description: String = "",
    val adminId: String = "",
    val members: List<String> = emptyList(), // List of UIDs
    val memberDetails: Map<String, MemberInfo> = emptyMap(),
    val isFinalized: Boolean = false
)

data class MemberInfo(
    val name: String = "",
    val email: String = "",
    val status: String = "JOINED" // INVITED, JOINED
)
