package com.enosh.fincalc.utils

import android.content.Context
import android.net.Uri
import com.enosh.fincalc.data.local.AppDatabase
import com.enosh.fincalc.data.local.entity.Budget
import com.enosh.fincalc.data.local.entity.Expense
import com.enosh.fincalc.data.local.entity.Goal
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.util.*

object BackupUtils {
    data class BackupData(
        val expenses: List<Expense>,
        val goals: List<Goal>,
        val budgets: List<Budget>,
        val exportDate: Long
    )

    suspend fun exportData(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val expenses = db.expenseDao().getAllExpenses().first()
            val goals = db.goalDao().getAllGoals().first()
            val budgets = db.budgetDao().getAllBudgets().first()
            
            val data = BackupData(expenses, goals, budgets, System.currentTimeMillis())
            val json = Gson().toJson(data)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importData(context: Context, uri: Uri, overwrite: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader()?.use { it.readText() }
            val data = Gson().fromJson(json, BackupData::class.java) ?: return@withContext false
            
            val db = AppDatabase.getDatabase(context)
            
            if (overwrite) {
                db.expenseDao().deleteAllExpenses()
                db.goalDao().deleteAllGoals()
                db.budgetDao().deleteAllBudgets()
            }

            data.expenses.forEach { db.expenseDao().insertExpense(it.copy(id = 0)) }
            data.goals.forEach { db.goalDao().insertGoal(it.copy(id = 0)) }
            data.budgets.forEach { db.budgetDao().insertBudget(it.copy(id = 0)) }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
