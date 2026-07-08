package com.enosh.fincalc.data.local.dao

import androidx.room.*
import com.enosh.fincalc.data.local.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month AND uid = :uid LIMIT 1")
    fun getBudgetForMonth(month: String, uid: String): Flow<Budget?>

    @Query("SELECT * FROM budgets WHERE uid = :uid")
    fun getAllBudgets(uid: String): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE uid = :uid")
    suspend fun deleteAllBudgets(uid: String)
}
