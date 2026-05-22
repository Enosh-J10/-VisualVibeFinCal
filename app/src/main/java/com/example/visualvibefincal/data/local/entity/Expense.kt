package com.example.visualvibefincal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val date: Long,
    val merchant: String,
    val category: String,
    val source: String, // "scan" or "upload"
    val notes: String = ""
)
