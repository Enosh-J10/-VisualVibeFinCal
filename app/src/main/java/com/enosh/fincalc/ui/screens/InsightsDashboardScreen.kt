package com.enosh.fincalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingUp
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
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.FinancialViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsDashboardScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    financialViewModel: FinancialViewModel = viewModel()
) {
    val expenses by financialViewModel.allExpenses.collectAsState()
    val currentMonth = financialViewModel.getCurrentMonth()
    val budget by financialViewModel.getBudgetForMonth(currentMonth).collectAsState(initial = null)
    
    val suggestions = financialViewModel.getSmartSuggestions(expenses, budget)

    val totalSpending = expenses.sumOf { it.amount }
    val topCategory = expenses.groupBy { it.category }
        .maxByOrNull { it.value.sumOf { exp -> exp.amount } }?.key ?: "None"

    CalculatorScreenScaffold(
        title = "Insights Dashboard",
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color(0xFFE3F2FD)
                    )
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Total Spending", fontSize = 14.sp, color = Color.Gray)
                        Text("$${String.format("%.2f", totalSpending)}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF00D1B2), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Top Category: $topCategory", fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Text("Monthly Trend", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            item {
                CalculatorCard(isDarkMode = isDarkMode) {
                    if (expenses.isEmpty()) {
                        Text("No data available for charts", modifier = Modifier.padding(16.dp), color = Color.Gray)
                    } else {
                        // Simple bar chart placeholder
                        Row(Modifier.fillMaxWidth().height(150.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly) {
                            val categories = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
                            val maxVal = categories.values.maxOrNull() ?: 1.0
                            categories.forEach { (cat, amount) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        Modifier
                                            .width(40.dp)
                                            .fillMaxHeight((amount / maxVal).toFloat())
                                            .background(Color(0xFF00D1B2), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    )
                                    Text(cat.take(5), fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            if (suggestions.isNotEmpty()) {
                item {
                    Text("Smart Suggestions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                items(suggestions) { suggestion ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00D1B2).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, null, tint = Color(0xFF00D1B2), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(suggestion, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Text("Recent Expenses", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            items(expenses.takeLast(5).reversed()) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White)
                ) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(expense.merchant, fontWeight = FontWeight.Bold)
                            Text(expense.category, fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("$${String.format("%.2f", expense.amount)}", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                    }
                }
            }
        }
    }
}
