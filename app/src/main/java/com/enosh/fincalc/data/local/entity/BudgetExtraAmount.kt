package com.enosh.fincalc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_extra_amounts")
data class BudgetExtraAmount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val month: String,
    val amount: Double,
    val date: Long,
    val reason: String,
    val category: String,
    val paymentMethod: String? = null,
    val uid: String = ""
)
