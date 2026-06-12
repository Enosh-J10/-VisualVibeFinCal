package com.enosh.fincalc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.ui.screens.SummaryItem
import com.enosh.fincalc.ui.screens.CalculatorScreenScaffold
import com.enosh.fincalc.ui.screens.CalculatorCard
import com.enosh.fincalc.ui.screens.BouncyButton
import com.enosh.fincalc.utils.CurrencyUtils
import com.enosh.fincalc.utils.ValidationUtils
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.viewmodel.AssistantViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSavingPlannerScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel
) {
    var salary by remember { mutableStateOf("") }
    var goalAmount by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }
    var currentExpenses by remember { mutableStateOf("") }
    
    var result by remember { mutableStateOf<PlannerResult?>(null) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    CalculatorScreenScaffold(
        title = "Auto Saving Planner",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            CalculatorCard(isDarkMode = isDarkMode) {
                Text("Your Financial Goal", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                Spacer(Modifier.height(16.dp))
                
                ValidatedTextField(
                    value = salary,
                    onValueChange = { salary = ValidationUtils.formatNumericInput(it) },
                    label = "Monthly Salary (${CurrencyUtils.getSelectedCurrency(context).symbol})"
                )
                
                Spacer(Modifier.height(12.dp))
                
                ValidatedTextField(
                    value = currentExpenses,
                    onValueChange = { currentExpenses = ValidationUtils.formatNumericInput(it) },
                    label = "Monthly Fixed Expenses"
                )

                Spacer(Modifier.height(12.dp))

                ValidatedTextField(
                    value = goalAmount,
                    onValueChange = { goalAmount = ValidationUtils.formatNumericInput(it) },
                    label = "Savings Goal Target"
                )
                
                Spacer(Modifier.height(12.dp))

                Text("Target Date (Optional)", fontSize = 14.sp, color = Color.Gray)
                // Simple date picker placeholder or just text for now
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00D1B2))
                )
            }

            Spacer(Modifier.height(24.dp))

            BouncyButton(
                onClick = {
                    val s = salary.toDoubleOrNull() ?: 0.0
                    val e = currentExpenses.toDoubleOrNull() ?: 0.0
                    val g = goalAmount.toDoubleOrNull() ?: 0.0
                    
                    if (s > 0 && g > 0) {
                        val needs = s * 0.50
                        val wants = s * 0.30
                        val savings = s * 0.20
                        
                        val monthlyTarget = if (savings > (s - e)) (s - e).coerceAtLeast(0.0) else savings
                        val daysToReach = if (monthlyTarget > 0) ceil((g / monthlyTarget) * 30).toInt() else -1
                        
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, if (daysToReach > 0) daysToReach else 3650)
                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)

                        result = PlannerResult(
                            monthlySaving = monthlyTarget,
                            dailySpendLimit = (s - e - monthlyTarget) / 30,
                            completionDate = dateStr,
                            isAchievable = monthlyTarget > 0
                        )
                        
                        assistantViewModel.showMessage("Planning complete! You need to save ${CurrencyUtils.formatCurrency(context, monthlyTarget)} per month. 🚀", AssistantState.HAPPY)
                    } else {
                        assistantViewModel.showMessage("Please enter your salary and goal. 😊", AssistantState.THINKING)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Calculate Plan", fontWeight = FontWeight.Bold)
            }

            if (result != null) {
                Spacer(Modifier.height(32.dp))
                CalculatorCard(isDarkMode = isDarkMode) {
                    Text("50/30/20 Rule Analysis", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF00D1B2))
                    Spacer(Modifier.height(16.dp))
                    
                    Text("Based on your income, you should aim to save 20% (${CurrencyUtils.formatCurrency(context, (salary.toDoubleOrNull() ?: 0.0) * 0.2)}).", fontSize = 14.sp)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    SummaryItem("Monthly Savings Target", CurrencyUtils.formatCurrency(context, result!!.monthlySaving), isDarkMode, highlight = true)
                    SummaryItem("Daily Spending Limit", CurrencyUtils.formatCurrency(context, result!!.dailySpendLimit), isDarkMode)
                    SummaryItem("Estimated Completion", result!!.completionDate, isDarkMode)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Surface(
                        color = Color(0xFF00D1B2).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "You need to save ${CurrencyUtils.formatCurrency(context, result!!.monthlySaving)}/month to reach your goal.",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00D1B2)
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

data class PlannerResult(
    val monthlySaving: Double,
    val dailySpendLimit: Double,
    val completionDate: String,
    val isAchievable: Boolean
)
