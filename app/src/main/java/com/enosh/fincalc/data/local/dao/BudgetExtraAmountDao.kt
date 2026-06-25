package com.enosh.fincalc.data.local.dao

import androidx.room.*
import com.enosh.fincalc.data.local.entity.BudgetExtraAmount
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetExtraAmountDao {
    @Query("SELECT * FROM budget_extra_amounts WHERE month = :month")
    fun getExtraAmountsForMonth(month: String): Flow<List<BudgetExtraAmount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(extraAmount: BudgetExtraAmount)

    @Update
    suspend fun update(extraAmount: BudgetExtraAmount)

    @Delete
    suspend fun delete(extraAmount: BudgetExtraAmount)

    @androidx.room.Query("SELECT * FROM budget_extra_amounts")
    fun getAllExtraAmounts(): kotlinx.coroutines.flow.Flow<List<BudgetExtraAmount>>
}
