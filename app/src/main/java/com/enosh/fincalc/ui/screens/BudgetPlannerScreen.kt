package com.enosh.fincalc.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.enosh.fincalc.data.local.entity.Budget
import com.enosh.fincalc.data.local.entity.BudgetExtraAmount
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.ValidationUtils
import com.enosh.fincalc.viewmodel.AssistantMessageType
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.FinancialViewModel
import com.enosh.fincalc.utils.CurrencyUtils
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
    val extraAmounts by financialViewModel.getExtraAmountsForMonth(currentMonth).collectAsState(initial = emptyList())
    val expenses by financialViewModel.allExpenses.collectAsState()
    
    val totalExtra = extraAmounts.sumOf { it.amount }
    val totalSpent = expenses.filter { 
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        sdf.format(java.util.Date(it.date)) == currentMonth
    }.sumOf { it.amount }

    var showEditDialog by remember { mutableStateOf(false) }
    var showExtraDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    CalculatorScreenScaffold(
        title = "Budget Planner",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CalculatorCard(isDarkMode = isDarkMode) {
                    Text("Monthly Budget Status", fontSize = 14.sp, color = Color.Gray)
                    val budgetAmount = budget?.amount ?: 0.0
                    val availableBudget = budgetAmount + totalExtra
                    
                    Text(CurrencyUtils.formatCurrency(context, availableBudget), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Base Budget: ${CurrencyUtils.formatCurrency(context, budgetAmount)}", fontSize = 12.sp, color = Color.Gray)
                        Text("Extra: +${CurrencyUtils.formatCurrency(context, totalExtra)}", fontSize = 12.sp, color = Color(0xFF00D1B2))
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    val progress = if (availableBudget > 0) (totalSpent / availableBudget).toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(12.dp),
                        color = if (progress > 1f) Color.Red else Color(0xFF00D1B2),
                        trackColor = Color.Gray.copy(alpha = 0.2f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Spent: ${CurrencyUtils.formatCurrency(context, totalSpent)}", fontSize = 12.sp)
                        Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Remaining: ${CurrencyUtils.formatCurrency(context, (availableBudget - totalSpent).coerceAtLeast(0.0))}", fontSize = 12.sp)
                    }

                    if (totalSpent > availableBudget && availableBudget > 0) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            color = Color.Red.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Warning: You have exceeded your adjusted budget!",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BouncyButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Set Budget", fontSize = 14.sp)
                    }
                    BouncyButton(
                        onClick = { showExtraDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        containerColor = Color.Transparent
                    ) {
                        Text("Add Extra", color = Color(0xFF00D1B2), fontSize = 14.sp)
                    }
                }
            }

            if (extraAmounts.isNotEmpty()) {
                item {
                    Text("Extra Amounts / Cash Flow", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
                items(extraAmounts) { extra ->
                    ExtraAmountItem(extra, isDarkMode, onDelete = { financialViewModel.deleteExtraAmount(extra) })
                }
            }
        }
    }

    if (showEditDialog) {
        SetBudgetDialog(
            currentAmount = budget?.amount ?: 0.0,
            onDismiss = { showEditDialog = false },
            onSave = { amount ->
                financialViewModel.setBudget(
                    Budget(
                        month = currentMonth,
                        amount = amount
                    )
                )
                showEditDialog = false
            }
        )
    }

    if (showExtraDialog) {
        AddExtraAmountDialog(
            onDismiss = { showExtraDialog = false },
            onSave = { amount, reason, category ->
                financialViewModel.insertExtraAmount(
                    BudgetExtraAmount(
                        month = currentMonth,
                        amount = amount,
                        date = System.currentTimeMillis(),
                        reason = reason,
                        category = category
                    )
                )
                showExtraDialog = false
            }
        )
    }
}

@Composable
fun SetBudgetDialog(currentAmount: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var amount by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This is your base budget for the month. Extra amounts will be added to this.", fontSize = 12.sp, color = Color.Gray)
                ValidatedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = "Budget Amount", 
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = amount.toDoubleOrNull() ?: 0.0
                if (value > 0) {
                    onSave(value)
                }
            }, enabled = amount.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExtraAmountItem(extra: BudgetExtraAmount, isDarkMode: Boolean, onDelete: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(extra.reason, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${extra.category} • ${java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(extra.date))}", fontSize = 12.sp, color = Color.Gray)
            }
            Text("+${com.enosh.fincalc.utils.CurrencyUtils.formatCurrency(context, extra.amount)}", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
            IconButton(onClick = onDelete) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AddExtraAmountDialog(onDismiss: () -> Unit, onSave: (Double, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Returned Money") }
    var expanded by remember { mutableStateOf(false) }
    
    val categories = listOf("Returned Money", "Refund", "Salary Extra", "Gift", "Business Income", "Cashback", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Extra Amount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ValidatedTextField(value = amount, onValueChange = { amount = it }, label = "Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                ValidatedTextField(value = reason, onValueChange = { reason = it }, label = "Reason / Note")
                
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(category)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(amount.toDoubleOrNull() ?: 0.0, reason, category)
            }, enabled = amount.isNotBlank() && reason.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
