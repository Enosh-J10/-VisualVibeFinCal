package com.enosh.fincalc.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetExtraAmountDao(): BudgetExtraAmountDao
    abstract fun businessDao(): BusinessDao
    abstract fun aiChatDao(): AiChatDao

    companion object {
        private const val DATABASE_NAME = "fincalc_master_db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safely add uid columns with NOT NULL and DEFAULT values
                try {
                    db.execSQL("ALTER TABLE expenses ADD COLUMN uid TEXT NOT NULL DEFAULT 'guest'")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Migration 7-8: expenses.uid failed", e)
                }
                
                try {
                    db.execSQL("ALTER TABLE goals ADD COLUMN uid TEXT NOT NULL DEFAULT 'guest'")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Migration 7-8: goals.uid failed", e)
                }
                
                try {
                    db.execSQL("ALTER TABLE budgets ADD COLUMN uid TEXT NOT NULL DEFAULT 'guest'")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Migration 7-8: budgets.uid failed", e)
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_7_8)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
