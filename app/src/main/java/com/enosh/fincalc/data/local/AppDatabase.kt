package com.enosh.fincalc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.enosh.fincalc.data.local.dao.BudgetDao
import com.enosh.fincalc.data.local.dao.BudgetExtraAmountDao
import com.enosh.fincalc.data.local.dao.ExpenseDao
import com.enosh.fincalc.data.local.dao.GoalDao
import com.enosh.fincalc.data.local.dao.BusinessDao
import com.enosh.fincalc.data.local.entity.Budget
import com.enosh.fincalc.data.local.entity.BudgetExtraAmount
import com.enosh.fincalc.data.local.entity.Expense
import com.enosh.fincalc.data.local.entity.Goal
import com.enosh.fincalc.data.local.entity.BusinessIncomeEntity
import com.enosh.fincalc.data.local.entity.BusinessTargetEntity
import com.enosh.fincalc.data.local.ConversationEntity
import com.enosh.fincalc.data.local.MessageEntity
import com.enosh.fincalc.data.local.AiChatDao

@Database(
    entities = [
        Expense::class, 
        Goal::class, 
        Budget::class, 
        BudgetExtraAmount::class, 
        BusinessIncomeEntity::class, 
        BusinessTargetEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ], 
    version = 7
)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetExtraAmountDao(): BudgetExtraAmountDao
    abstract fun businessDao(): BusinessDao
    abstract fun aiChatDao(): AiChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun resetInstance() {
            INSTANCE = null
        }

        fun getDatabase(context: Context): AppDatabase {
            val prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            val isGuest = prefs.getBoolean("is_guest", false)
            val userId = try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            } catch (e: Throwable) {
                "anonymous"
            }
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
