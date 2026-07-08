package com.enosh.fincalc.data.local.dao

import androidx.room.*
import com.enosh.fincalc.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE uid = :uid ORDER BY date DESC")
    fun getAllExpenses(uid: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE amount = :amount AND date = :date AND merchant = :merchant AND uid = :uid LIMIT 1")
    suspend fun findDuplicate(amount: Double, date: Long, merchant: String, uid: String): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE uid = :uid")
    suspend fun deleteAllExpenses(uid: String)
}
