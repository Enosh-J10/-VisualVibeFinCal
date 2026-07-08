package com.enosh.fincalc.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.enosh.fincalc.data.local.AppDatabase
import com.enosh.fincalc.data.local.entity.Budget
import com.enosh.fincalc.data.local.entity.Expense
import com.enosh.fincalc.data.local.entity.Goal
import com.enosh.fincalc.data.local.entity.BusinessIncomeEntity
import com.enosh.fincalc.data.local.entity.BusinessTargetEntity
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.util.*

object BackupUtils {
    data class BackupData(
        val expenses: List<Expense> = emptyList(),
        val goals: List<Goal> = emptyList(),
        val budgets: List<Budget> = emptyList(),
        val budgetExtraAmounts: List<com.enosh.fincalc.data.local.entity.BudgetExtraAmount> = emptyList(),
        val businessIncomes: List<BusinessIncomeEntity> = emptyList(),
        val businessTargets: List<BusinessTargetEntity> = emptyList(),
        val notes: Map<String, String> = emptyMap(),
        val preferences: Map<String, *> = emptyMap<String, Any>(),
        val exportDate: Long = System.currentTimeMillis()
    )

    suspend fun createBackupData(context: Context): BackupData = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val uid = UserUtils.getEffectiveUid(context)
        
        val expenses = db.expenseDao().getAllExpenses(uid).first()
        val goals = db.goalDao().getAllGoals(uid).first()
        val budgets = db.budgetDao().getAllBudgets(uid).first()
        val budgetExtraAmounts = db.budgetExtraAmountDao().getAllExtraAmounts(uid).first()
        val businessIncomes = db.businessDao().getAllIncomes(uid).first()
        val businessTargets = db.businessDao().getAllTargets(uid).first()
        
        val notesPrefs = context.getSharedPreferences("NotesPrefs_$uid", Context.MODE_PRIVATE)
        val notes = notesPrefs.all.mapValues { it.value.toString() }
        
        val userPrefs = context.getSharedPreferences(UserUtils.PREFS_NAME, Context.MODE_PRIVATE)
        val sensitiveKeys = listOf("password", "token", "api_key", "credentials", "gemini")
        val preferences = userPrefs.all.filterKeys { key -> 
            sensitiveKeys.none { sensitive -> key.contains(sensitive, ignoreCase = true) }
        }
        
        BackupData(
            expenses = expenses,
            goals = goals,
            budgets = budgets,
            budgetExtraAmounts = budgetExtraAmounts,
            businessIncomes = businessIncomes,
            businessTargets = businessTargets,
            notes = notes,
            preferences = preferences,
            exportDate = System.currentTimeMillis()
        )
    }

    suspend fun exportData(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val data = createBackupData(context)
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
            restoreData(context, data, overwrite)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreData(context: Context, data: BackupData, overwrite: Boolean) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val uid = UserUtils.getEffectiveUid(context)
            
            if (overwrite) {
                db.expenseDao().deleteAllExpenses(uid)
                db.goalDao().deleteAllGoals(uid)
                db.budgetDao().deleteAllBudgets(uid)
                // Clear business data
                db.businessDao().getAllIncomes(uid).first().forEach { db.businessDao().deleteIncome(it) }
                db.businessDao().getAllTargets(uid).first().forEach { db.businessDao().deleteTarget(it) }
            }

            data.expenses.forEach { db.expenseDao().insertExpense(it.copy(id = 0, uid = uid)) }
            data.goals.forEach { db.goalDao().insertGoal(it.copy(id = 0, uid = uid)) }
            data.budgets.forEach { db.budgetDao().insertBudget(it.copy(id = 0, uid = uid)) }
            data.budgetExtraAmounts.forEach { db.budgetExtraAmountDao().insert(it.copy(id = 0, uid = uid)) }
            data.businessIncomes.forEach { db.businessDao().insertIncome(it.copy(uid = uid)) }
            data.businessTargets.forEach { db.businessDao().insertTarget(it.copy(uid = uid)) }
            
            val notesPrefs = context.getSharedPreferences("NotesPrefs_$uid", Context.MODE_PRIVATE)
            notesPrefs.edit(commit = true) {
                data.notes.forEach { (k, v) -> putString(k, v) }
            }
            
            val userPrefs = context.getSharedPreferences(UserUtils.PREFS_NAME, Context.MODE_PRIVATE)
            userPrefs.edit(commit = true) {
                data.preferences.forEach { (k, v) ->
                    when (v) {
                        is Boolean -> putBoolean(k, v)
                        is String -> putString(k, v)
                        is Double -> putFloat(k, v.toFloat()) 
                        is Int -> putInt(k, v)
                        is Float -> putFloat(k, v)
                        is Long -> putLong(k, v)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun cloudBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return@withContext false
        val uid = user.uid
        android.util.Log.d("BackupDebug", "Starting cloud backup for uid: $uid")
        try {
            val data = createBackupData(context)
            val json = Gson().toJson(data)
            
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val backupDoc = mapOf(
                "backupId" to "latest",
                "uid" to uid,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "versionCode" to 19, // Hardcoded for this fix
                "versionName" to "1.9.8",
                "data" to json
            )
            
            db.collection("users").document(uid)
                .collection("backups").document("latest")
                .set(backupDoc)
                .await()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("BackupDebug", "Cloud backup failed for $uid", e)
            false
        }
    }

    suspend fun cloudRestore(context: Context): Boolean = withContext(Dispatchers.IO) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return@withContext false
        val uid = user.uid
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = db.collection("users").document(uid)
                .collection("backups").document("latest")
                .get()
                .await()
            
            if (!snapshot.exists()) return@withContext false
            
            val json = snapshot.getString("data") ?: return@withContext false
            val data = Gson().fromJson(json, BackupData::class.java) ?: return@withContext false
            
            restoreData(context, data, overwrite = true)
            true
        } catch (e: Exception) {
            android.util.Log.e("BackupDebug", "Cloud restore failed for $uid", e)
            false
        }
    }
}
