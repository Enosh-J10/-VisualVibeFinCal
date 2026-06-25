package com.enosh.fincalc.data.local.dao

import androidx.room.*
import com.enosh.fincalc.data.local.entity.BusinessIncomeEntity
import com.enosh.fincalc.data.local.entity.BusinessTargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM business_incomes WHERE uid = :uid ORDER BY date DESC")
    fun getAllIncomes(uid: String): Flow<List<BusinessIncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: BusinessIncomeEntity)

    @Delete
    suspend fun deleteIncome(income: BusinessIncomeEntity)

    @Query("SELECT * FROM business_targets WHERE month = :month AND uid = :uid")
    fun getTarget(month: String, uid: String): Flow<BusinessTargetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: BusinessTargetEntity)

    @Delete
    suspend fun deleteTarget(target: BusinessTargetEntity)

    @androidx.room.Query("SELECT * FROM business_targets WHERE uid = :uid")
    fun getAllTargets(uid: String): kotlinx.coroutines.flow.Flow<List<BusinessTargetEntity>>
}
