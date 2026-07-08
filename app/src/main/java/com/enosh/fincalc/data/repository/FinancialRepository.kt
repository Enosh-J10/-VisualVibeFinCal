package com.enosh.fincalc.data.repository

import com.enosh.fincalc.data.local.dao.BudgetDao
import com.enosh.fincalc.data.local.dao.BudgetExtraAmountDao
import com.enosh.fincalc.data.local.dao.ExpenseDao
import com.enosh.fincalc.data.local.dao.GoalDao
import com.enosh.fincalc.data.local.entity.Budget
import com.enosh.fincalc.data.local.entity.BudgetExtraAmount
import com.enosh.fincalc.data.local.entity.Expense
import com.enosh.fincalc.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

class FinancialRepository(
    private val expenseDao: ExpenseDao,
    private val goalDao: GoalDao,
    private val budgetDao: BudgetDao,
    private val budgetExtraAmountDao: BudgetExtraAmountDao
) {
    fun getAllExpenses(uid: String): Flow<List<Expense>> = expenseDao.getAllExpenses(uid)
    fun getAllGoals(uid: String): Flow<List<Goal>> = goalDao.getAllGoals(uid)

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal)

    fun getBudgetForMonth(month: String, uid: String): Flow<Budget?> = budgetDao.getBudgetForMonth(month, uid)
    suspend fun insertBudget(budget: Budget) = budgetDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = budgetDao.updateBudget(budget)

    fun getExtraAmountsForMonth(month: String, uid: String): Flow<List<BudgetExtraAmount>> = budgetExtraAmountDao.getExtraAmountsForMonth(month, uid)
    suspend fun insertExtraAmount(extraAmount: BudgetExtraAmount) = budgetExtraAmountDao.insert(extraAmount)
    suspend fun updateExtraAmount(extraAmount: BudgetExtraAmount) = budgetExtraAmountDao.update(extraAmount)
    suspend fun deleteExtraAmount(extraAmount: BudgetExtraAmount) = budgetExtraAmountDao.delete(extraAmount)
}
