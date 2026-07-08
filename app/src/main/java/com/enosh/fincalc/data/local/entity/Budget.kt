package com.enosh.fincalc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: String, // e.g., "2023-10"
    val amount: Double,
    val uid: String = "guest"
)
