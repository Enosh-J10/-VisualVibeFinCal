package com.enosh.fincalc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.enosh.fincalc.data.local.dao.BudgetDao
import com.enosh.fincalc.data.local.dao.ExpenseDao
import com.enosh.fincalc.data.local.dao.GoalDao
import com.enosh.fincalc.data.local.entity.Budget
import com.enosh.fincalc.data.local.entity.Expense
import com.enosh.fincalc.data.local.entity.Goal

@Database(entities = [Expense::class, Goal::class, Budget::class], version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val isGuest = prefs.getBoolean("is_guest", false)
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            val dbName = if (isGuest) "fincalc_database_guest" else "fincalc_database_$userId"

            val currentInstance = INSTANCE
            if (currentInstance != null && currentInstance.openHelper.databaseName == dbName) {
                return currentInstance
            }

            return synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                // TODO: Replace fallbackToDestructiveMigration with explicit Room migrations before production release.
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
