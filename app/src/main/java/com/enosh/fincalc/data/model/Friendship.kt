package com.enosh.fincalc.data.model

import com.google.firebase.Timestamp

data class Friendship(
    val friendshipId: String = "",
    val memberUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val nicknames: Map<String, String> = emptyMap(), // UID to Nickname
    val blockedUids: List<String> = emptyList()
)
