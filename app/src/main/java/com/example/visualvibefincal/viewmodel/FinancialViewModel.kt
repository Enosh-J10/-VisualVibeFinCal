package com.example.visualvibefincal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.visualvibefincal.data.local.AppDatabase
import com.example.visualvibefincal.data.local.entity.Budget
import com.example.visualvibefincal.data.local.entity.Expense
import com.example.visualvibefincal.data.local.entity.Goal
import com.example.visualvibefincal.data.repository.FinancialRepository
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

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinancialRepository(db.expenseDao(), db.goalDao(), db.budgetDao())
        allExpenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allGoals = repository.allGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun insertExpense(expense: Expense) = viewModelScope.launch { repository.insertExpense(expense) }
    fun updateExpense(expense: Expense) = viewModelScope.launch { repository.updateExpense(expense) }
    fun deleteExpense(expense: Expense) = viewModelScope.launch { repository.deleteExpense(expense) }

    fun insertGoal(goal: Goal) = viewModelScope.launch { repository.insertGoal(goal) }
    fun updateGoal(goal: Goal) = viewModelScope.launch { repository.updateGoal(goal) }
    fun deleteGoal(goal: Goal) = viewModelScope.launch { repository.deleteGoal(goal) }

    fun getBudgetForMonth(month: String): Flow<Budget?> = repository.getBudgetForMonth(month)
    fun setBudget(budget: Budget) = viewModelScope.launch { repository.insertBudget(budget) }
    
    fun getCurrentMonth(): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    fun getSmartSuggestions(expenses: List<Expense>, budget: Budget?): List<String> {
        val suggestions = mutableListOf<String>()
        if (expenses.isEmpty()) {
            suggestions.add("Welcome! Start by scanning a receipt to track your spending. 📸")
            suggestions.add("Tip: Setting a monthly budget helps you save more. 💰")
            return suggestions
        }

        val currentMonth = getCurrentMonth()
        val currentMonthExpenses = expenses.filter { 
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.date)) == currentMonth 
        }

        val totalSpent = currentMonthExpenses.sumOf { it.amount }
        if (budget != null && totalSpent > budget.amount) {
            suggestions.add("You've exceeded your monthly budget! Try to cut back on non-essentials. 📉")
        }

        val foodExpenses = currentMonthExpenses.filter { it.category == "Food & Dining" }.sumOf { it.amount }
        if (foodExpenses > totalSpent * 0.4 && totalSpent > 0) {
            suggestions.add("You are spending a lot on food this month (over 40% of total). 🍕")
        }

        if (currentMonthExpenses.size > 5 && totalSpent < 50) {
            suggestions.add("Great job keeping your spending low this month! 🌟")
        }

        return suggestions
    }
}
