package com.enosh.fincalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.enosh.fincalc.data.model.BusinessIncome
import com.enosh.fincalc.viewmodel.SmartBusinessViewModel
import com.enosh.fincalc.ui.components.ValidatedTextField
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBusinessScreen(
    navController: NavController,
    isDarkMode: Boolean,
    viewModel: SmartBusinessViewModel = viewModel()
) {
    val incomes by viewModel.incomes.collectAsState()
    val target by viewModel.monthlyTarget.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var editingIncome by remember { mutableStateOf<BusinessIncome?>(null) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Records", "Reports")

    val filteredIncomes = remember(incomes, selectedFilterCategory) {
        if (selectedFilterCategory == "All") incomes
        else incomes.filter { it.category == selectedFilterCategory }
    }

    val totalIncome = filteredIncomes.sumOf { it.amount }
    val targetAmount = target?.targetAmount ?: 0.0
    val progress = if (targetAmount > 0) (totalIncome / targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    CalculatorScreenScaffold(
        title = "Smart Business",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF00D1B2)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        TargetProgressCard(totalIncome, targetAmount, progress, isDarkMode) {
                            showTargetDialog = true
                        }
                    }

                    item {
                        val categories = listOf("All", "Product Sales", "Repairs", "Services", "Commission", "Freelance", "Other")
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedFilterCategory == cat,
                                    onClick = { selectedFilterCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Income Records", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            TextButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, null)
                                Text("Add Income")
                            }
                        }
                    }

                    if (filteredIncomes.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No income records found.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(filteredIncomes, key = { it.incomeId }) { income ->
                            IncomeItem(
                                income = income, 
                                isDarkMode = isDarkMode, 
                                onEdit = { editingIncome = income },
                                onDelete = { showDeleteConfirm = income.incomeId }
                            )
                        }
                    }
                }
            } else {
                BusinessReportsTab(incomes, isDarkMode)
            }
        }
    }

    if (showAddDialog || editingIncome != null) {
        AddIncomeDialog(
            existingIncome = editingIncome,
            onDismiss = { 
                showAddDialog = false
                editingIncome = null
            },
            onSave = { 
                viewModel.addIncome(it)
                showAddDialog = false
                editingIncome = null
            }
        )
    }

    if (showTargetDialog) {
        SetTargetDialog(
            currentTarget = targetAmount,
            onDismiss = { showTargetDialog = false },
            onSave = { 
                viewModel.updateTarget(it)
                showTargetDialog = false
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Record?") },
            text = { Text("Are you sure you want to delete this income record?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm?.let { viewModel.deleteIncome(it) }
                    showDeleteConfirm = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TargetProgressCard(total: Double, target: Double, progress: Float, isDarkMode: Boolean, onEditTarget: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00D1B2).copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Monthly Target", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp).clickable { onEditTarget() }, tint = Color(0xFF00D1B2))
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                color = Color(0xFF00D1B2)
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Received", fontSize = 12.sp, color = Color.Gray)
                    Text(com.enosh.fincalc.utils.CurrencyUtils.formatCurrency(context, total), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target", fontSize = 12.sp, color = Color.Gray)
                    Text(com.enosh.fincalc.utils.CurrencyUtils.formatCurrency(context, target), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${(progress * 100).toInt()}% achieved", fontSize = 12.sp, color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IncomeItem(income: BusinessIncome, isDarkMode: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFF00D1B2))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(income.source, fontWeight = FontWeight.Bold)
                    if (income.reason.isNotBlank()) {
                        Text(income.reason, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Text("${income.category} • ${income.paymentMethod}", fontSize = 12.sp, color = Color.Gray)
                    Text(sdf.format(Date(income.date)), fontSize = 10.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(com.enosh.fincalc.utils.CurrencyUtils.formatCurrency(context, income.amount), fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            if (income.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = income.notes,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 56.dp)
                )
            }
        }
    }
}

@Composable
fun AddIncomeDialog(existingIncome: BusinessIncome? = null, onDismiss: () -> Unit, onSave: (BusinessIncome) -> Unit) {
    var amount by remember { mutableStateOf(existingIncome?.amount?.toString() ?: "") }
    var source by remember { mutableStateOf(existingIncome?.source ?: "") }
    var reason by remember { mutableStateOf(existingIncome?.reason ?: "") }
    var selectedCategory by remember { mutableStateOf(existingIncome?.category ?: "Services") }
    var selectedMethod by remember { mutableStateOf(existingIncome?.paymentMethod ?: "Cash") }
    var notes by remember { mutableStateOf(existingIncome?.notes ?: "") }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    var methodExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Product Sales", "Repairs", "Services", "Commission", "Freelance", "Other")
    val methods = listOf("Cash", "Card", "Bank Transfer", "GPay / UPI", "PayPal", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingIncome == null) "Add Income" else "Edit Income") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ValidatedTextField(
                    value = amount, 
                    onValueChange = { amount = it }, 
                    label = "Amount", 
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
                ValidatedTextField(
                    value = source, 
                    onValueChange = { if (it.length <= 100) source = it }, 
                    label = "Customer",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                )
                ValidatedTextField(
                    value = reason, 
                    onValueChange = { if (it.length <= 100) reason = it }, 
                    label = "Reason",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                )
                
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCategory)
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { selectedCategory = cat; categoryExpanded = false })
                        }
                    }
                }

                Text("Payment Method", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Box {
                    OutlinedButton(onClick = { methodExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedMethod)
                    }
                    DropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        methods.forEach { met ->
                            DropdownMenuItem(text = { Text(met) }, onClick = { selectedMethod = met; methodExpanded = false })
                        }
                    }
                }

                ValidatedTextField(
                    value = notes, 
                    onValueChange = { if (it.length <= 500) notes = it },
                    label = "Notes (Optional)",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    BusinessIncome(
                        incomeId = existingIncome?.incomeId ?: "",
                        amount = amount.toDoubleOrNull() ?: 0.0, 
                        source = source, 
                        reason = reason,
                        category = selectedCategory, 
                        paymentMethod = selectedMethod,
                        notes = notes,
                        date = existingIncome?.date ?: System.currentTimeMillis()
                    )
                )
            }, enabled = amount.isNotBlank() && source.isNotBlank()) { Text(if (existingIncome == null) "Add" else "Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SetTargetDialog(currentTarget: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var amount by remember { mutableStateOf(currentTarget.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Monthly Target") },
        text = {
            ValidatedTextField(value = amount, onValueChange = { amount = it }, label = "Target Amount", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
        },
        confirmButton = {
            Button(onClick = { onSave(amount.toDoubleOrNull() ?: 0.0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BusinessReportsTab(incomes: List<BusinessIncome>, isDarkMode: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoryTotals = incomes.groupBy { it.category }.mapValues { it.value.sumOf { inc -> inc.amount } }
    val total = incomes.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Category Breakdown", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        item {
            com.enosh.fincalc.ui.screens.CalculatorCard(isDarkMode = isDarkMode) {
                if (incomes.isEmpty()) {
                    Text("No data available for charts", color = Color.Gray)
                } else {
                    categoryTotals.forEach { (cat, amount) ->
                        val catProgress = (amount / total).toFloat()
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(cat, fontSize = 14.sp)
                                Text(com.enosh.fincalc.utils.CurrencyUtils.formatCurrency(context, amount), fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { catProgress },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = Color(0xFF00D1B2)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        item {
            Text("Monthly Summary", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }

        val monthlyTotals = incomes.groupBy { 
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it.date))
        }.mapValues { it.value.sumOf { inc -> inc.amount } }

        items(monthlyTotals.toList().sortedByDescending { it.first }) { (month, amount) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White)
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(month, fontWeight = FontWeight.Medium)
                    Text(com.enosh.fincalc.utils.CurrencyUtils.formatCurrency(context, amount), fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                }
            }
        }
    }
}

