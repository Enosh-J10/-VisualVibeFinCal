package com.example.visualvibefincal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.visualvibefincal.data.local.entity.Budget
import com.example.visualvibefincal.ui.components.ValidatedTextField
import com.example.visualvibefincal.utils.ValidationUtils
import com.example.visualvibefincal.viewmodel.AssistantMessageType
import com.example.visualvibefincal.viewmodel.AssistantState
import com.example.visualvibefincal.viewmodel.AssistantViewModel
import com.example.visualvibefincal.viewmodel.FinancialViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlannerScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    financialViewModel: FinancialViewModel = viewModel()
) {
    val currentMonth = financialViewModel.getCurrentMonth()
    val budget by financialViewModel.getBudgetForMonth(currentMonth).collectAsState(initial = null)
    val expenses by financialViewModel.allExpenses.collectAsState()
    
    val totalSpent = expenses.filter { 
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        sdf.format(java.util.Date(it.date)) == currentMonth
    }.sumOf { it.amount }

    var showEditDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    CalculatorScreenScaffold(
        title = "Budget Planner",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            CalculatorCard(isDarkMode = isDarkMode) {
                Text("Monthly Budget ($currentMonth)", fontSize = 14.sp, color = Color.Gray)
                val budgetAmount = budget?.amount ?: 0.0
                Text("$${String.format("%.2f", budgetAmount)}", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                
                Spacer(Modifier.height(24.dp))
                
                val progress = if (budgetAmount > 0) (totalSpent / budgetAmount).toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = if (progress > 1f) Color.Red else Color(0xFF00D1B2),
                    trackColor = Color.Gray.copy(alpha = 0.2f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Spent: $${String.format("%.2f", totalSpent)}", fontSize = 12.sp)
                    Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Remaining: $${String.format("%.2f", (budgetAmount - totalSpent).coerceAtLeast(0.0))}", fontSize = 12.sp)
                }

                if (totalSpent > budgetAmount && budgetAmount > 0) {
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = Color.Red.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Warning: You have exceeded your budget!",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    
                    LaunchedEffect(Unit) {
                        assistantViewModel.showMessage("Watch out! You've gone over your budget this month. 💸", AssistantState.THINKING, AssistantMessageType.THOUGHT)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            BouncyButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Set Monthly Budget")
            }
        }
    }

    if (showEditDialog) {
        var amountStr by remember { mutableStateOf(budget?.amount?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Set Budget for $currentMonth") },
            text = {
                ValidatedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = ValidationUtils.formatNumericInput(it) },
                    label = "Monthly Budget Amount"
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    financialViewModel.setBudget(Budget(id = budget?.id ?: 0, month = currentMonth, amount = amount))
                    showEditDialog = false
                    scope.launch {
                        assistantViewModel.showMessage("Budget updated! Let's stick to it. 💪", AssistantState.HAPPY)
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }
}
