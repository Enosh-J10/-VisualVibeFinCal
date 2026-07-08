package com.enosh.fincalc.data.local.dao

import androidx.room.*
import com.enosh.fincalc.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE uid = :uid")
    fun getAllGoals(uid: String): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE uid = :uid")
    suspend fun deleteAllGoals(uid: String)
}
