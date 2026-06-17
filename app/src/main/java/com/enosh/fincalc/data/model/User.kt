package com.enosh.fincalc.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val finCalcId: String = "",
    val searchableName: String = "",
    val searchableEmail: String = "",
    val profilePic: String? = null,
    val updatedAt: Timestamp? = null
)
