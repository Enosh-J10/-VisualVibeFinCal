package com.enosh.fincalc.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_incomes")
data class BusinessIncomeEntity(
    @PrimaryKey val incomeId: String,
    val amount: Double,
    val date: Long,
    val source: String,
    val reason: String = "",
    val category: String,
    val paymentMethod: String,
    val notes: String = "",
    val uid: String
)

@Entity(tableName = "business_targets")
data class BusinessTargetEntity(
    @PrimaryKey val month: String, // yyyy-MM
    val targetAmount: Double,
    val uid: String
)
