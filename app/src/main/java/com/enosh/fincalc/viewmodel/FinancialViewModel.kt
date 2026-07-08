package com.enosh.fincalc.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enosh.fincalc.data.local.AppDatabase
import com.enosh.fincalc.data.local.entity.Budget
import com.enosh.fincalc.data.local.entity.BudgetExtraAmount
import com.enosh.fincalc.data.local.entity.Expense
import com.enosh.fincalc.data.local.entity.Goal
import com.enosh.fincalc.data.repository.FinancialRepository
import com.enosh.fincalc.utils.UserUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinancialViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinancialRepository
    val allExpenses: StateFlow<List<Expense>>
    val allGoals: StateFlow<List<Goal>>
    private val uid: String = UserUtils.getEffectiveUid(application)

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinancialRepository(db.expenseDao(), db.goalDao(), db.budgetDao(), db.budgetExtraAmountDao())
        allExpenses = repository.getAllExpenses(uid).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allGoals = repository.getAllGoals(uid).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun insertExpense(expense: Expense) = viewModelScope.launch { 
        repository.insertExpense(expense.copy(uid = uid)) 
    }
    fun updateExpense(expense: Expense) = viewModelScope.launch { 
        repository.updateExpense(expense.copy(uid = uid)) 
    }
    fun deleteExpense(expense: Expense) = viewModelScope.launch { repository.deleteExpense(expense) }

    fun insertGoal(goal: Goal) = viewModelScope.launch { 
        repository.insertGoal(goal.copy(uid = uid)) 
    }
    fun updateGoal(goal: Goal) = viewModelScope.launch { 
        repository.updateGoal(goal.copy(uid = uid)) 
    }
    fun deleteGoal(goal: Goal) = viewModelScope.launch { repository.deleteGoal(goal) }

    fun getBudgetForMonth(month: String): Flow<Budget?> = repository.getBudgetForMonth(month, uid)
    fun setBudget(budget: Budget) = viewModelScope.launch { 
        repository.insertBudget(budget.copy(uid = uid)) 
    }

    fun getExtraAmountsForMonth(month: String): Flow<List<BudgetExtraAmount>> = repository.getExtraAmountsForMonth(month, uid)
    fun insertExtraAmount(extraAmount: BudgetExtraAmount) = viewModelScope.launch { 
        repository.insertExtraAmount(extraAmount.copy(uid = uid)) 
    }
    fun deleteExtraAmount(extraAmount: BudgetExtraAmount) = viewModelScope.launch { repository.deleteExtraAmount(extraAmount) }
    
    fun getCurrentMonth(): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    fun getSmartSuggestions(expenses: List<Expense>, budget: Budget?): List<String> {
        val suggestions = mutableListOf<String>()
        if (expenses.isEmpty() && (budget == null || budget.amount == 0.0)) {
            suggestions.add("Welcome! Start by scanning a receipt to track your spending. 📸")
            suggestions.add("Tip: Setting a monthly budget helps you save more. 💰")
            suggestions.add("Recommendation: Use the Saving Planner to map out your goals! 🚀")
            return suggestions
        }

        if (budget != null && budget.amount > 0 && expenses.isEmpty()) {
            suggestions.add("Great! Your budget is set. Now scan a receipt to start tracking! 📸")
            return suggestions
        }

        val currentMonth = getCurrentMonth()
        val currentMonthExpenses = expenses.filter { 
            try {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date)) == currentMonth 
            } catch (e: Exception) { false }
        }

        if (currentMonthExpenses.isEmpty() && budget != null && budget.amount > 0) {
            suggestions.add("New month, new goals! Your budget is ready for your first expense. 🗓️")
            return suggestions
        }

        val totalSpent = currentMonthExpenses.sumOf { it.amount }
        if (budget != null && budget.amount > 0) {
            val budgetAmount = budget.amount
            if (totalSpent > budgetAmount) {
                suggestions.add("You went over your budget! Try to cut down on non-essential spending. 📉")
                suggestions.add("Suggestion: Review your 'Shopping' category for potential savings. 🛍️")
            } else if (totalSpent > budgetAmount * 0.9) {
                suggestions.add("Warning: You've used 90% of your budget. Be careful! ⚠️")
            } else if (totalSpent > budgetAmount * 0.7) {
                suggestions.add("You've used 70% of your budget. Keep an eye on your spending. 📊")
            } else if (totalSpent < budgetAmount * 0.4 && Calendar.getInstance().get(Calendar.DAY_OF_MONTH) > 15) {
                suggestions.add("Great job! You're well under budget for mid-month. 🌟")
            }
        } else {
            suggestions.add("Next Action: Set a monthly budget to get better insights! 📊")
        }

        val foodExpenses = currentMonthExpenses.filter { it.category == "Food & Dining" }.sumOf { it.amount }
        if (foodExpenses > totalSpent * 0.4 && totalSpent > 50) {
            suggestions.add("Expense Reduction: You're spending a lot on food—maybe try some home cooking? 🍕")
        }

        val shoppingExpenses = currentMonthExpenses.filter { it.category == "Shopping" }.sumOf { it.amount }
        if (shoppingExpenses > totalSpent * 0.3) {
            suggestions.add("Recommendation: Consider waiting 24 hours before making unplanned purchases. 🛒")
        }

        if (currentMonthExpenses.size > 5 && totalSpent < 50) {
            suggestions.add("Saving Option: Since your spending is low, maybe put an extra £20 into your goals? 🎯")
        }

        return suggestions
    }
}
