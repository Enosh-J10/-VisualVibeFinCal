package com.example.visualvibefincal.data.repository

import com.example.visualvibefincal.data.local.dao.BudgetDao
import com.example.visualvibefincal.data.local.dao.ExpenseDao
import com.example.visualvibefincal.data.local.dao.GoalDao
import com.example.visualvibefincal.data.local.entity.Budget
import com.example.visualvibefincal.data.local.entity.Expense
import com.example.visualvibefincal.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

class FinancialRepository(
    private val expenseDao: ExpenseDao,
    private val goalDao: GoalDao,
    private val budgetDao: BudgetDao
) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal)

    fun getBudgetForMonth(month: String): Flow<Budget?> = budgetDao.getBudgetForMonth(month)
    suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)
}
