package com.enosh.fincalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.data.model.BusinessIncome
import com.enosh.fincalc.data.model.BusinessTarget
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.enosh.fincalc.data.local.AppDatabase
import com.enosh.fincalc.data.local.entity.BusinessIncomeEntity
import com.enosh.fincalc.data.local.entity.BusinessTargetEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

import kotlinx.coroutines.flow.map

class SmartBusinessViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.businessDao()
    private val currentUid = com.enosh.fincalc.utils.UserUtils.getEffectiveUid(application)

    val incomes = dao.getAllIncomes(currentUid).map { entities ->
        entities.map { entity ->
            BusinessIncome(
                incomeId = entity.incomeId,
                amount = entity.amount,
                date = entity.date,
                source = entity.source,
                reason = entity.reason,
                category = entity.category,
                paymentMethod = entity.paymentMethod,
                notes = entity.notes,
                uid = entity.uid
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyTarget = dao.getTarget(getCurrentMonth(), currentUid).map { entity ->
        entity?.let {
            BusinessTarget(
                month = it.month,
                targetAmount = it.targetAmount,
                uid = it.uid
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun getCurrentMonth(): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    fun addIncome(income: BusinessIncome) {
        val entity = BusinessIncomeEntity(
            incomeId = if (income.incomeId.isBlank()) UUID.randomUUID().toString() else income.incomeId,
            amount = income.amount,
            date = income.date,
            source = income.source,
            reason = income.reason,
            category = income.category,
            paymentMethod = income.paymentMethod,
            notes = income.notes,
            uid = currentUid
        )
        viewModelScope.launch {
            dao.insertIncome(entity)
        }
    }

    fun updateTarget(amount: Double) {
        val target = BusinessTargetEntity(
            month = getCurrentMonth(),
            targetAmount = amount,
            uid = currentUid
        )
        viewModelScope.launch {
            dao.insertTarget(target)
        }
    }

    fun deleteIncome(incomeId: String) {
        viewModelScope.launch {
            // Since we need the entity to delete, and we only have ID, we might need a deleteById query.
            // Or just use a dummy entity with the same ID.
            dao.deleteIncome(
                BusinessIncomeEntity(
                    incomeId = incomeId,
                    amount = 0.0,
                    date = 0L,
                    source = "",
                    category = "",
                    paymentMethod = "",
                    uid = currentUid
                )
            )
        }
    }
}
