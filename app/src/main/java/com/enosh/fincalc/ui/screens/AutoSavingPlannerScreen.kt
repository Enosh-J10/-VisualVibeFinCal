package com.enosh.fincalc.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
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
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    var result by remember { mutableStateOf<PlannerResult?>(null) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = Calendar.getInstance().apply { timeInMillis = it }
                        val today = Calendar.getInstance().apply { 
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        
                        if (selectedDate.before(today)) {
                            Toast.makeText(context, "Please select a valid future date.", Toast.LENGTH_SHORT).show()
                        } else {
                            targetDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = targetDate,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D1B2),
                            unfocusedBorderColor = if (targetDate.isNotEmpty()) Color(0xFF00D1B2) else Color.Gray
                        )
                    )
                    // Invisible clickable layer
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            BouncyButton(
                onClick = {
                    val s = salary.toDoubleOrNull() ?: 0.0
                    val e = currentExpenses.toDoubleOrNull() ?: 0.0
                    val g = goalAmount.toDoubleOrNull() ?: 0.0
                    
                    if (s > 0 && g > 0) {
                        val availableForSaving = (s - e).coerceAtLeast(0.0)
                        val suggestedSavings = s * 0.20
                        
                        var monthlyTarget = suggestedSavings.coerceAtMost(availableForSaving)
                        var dateStr = ""
                        var isAchievable = true

                        if (targetDate.isNotEmpty()) {
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val target = sdf.parse(targetDate)
                                val today = Calendar.getInstance().apply { 
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                
                                val monthsRemaining = ((target.time - today.timeInMillis) / (1000L * 60 * 60 * 24 * 30)).coerceAtLeast(1L).toDouble()
                                val requiredSaving = g / monthsRemaining
                                
                                if (requiredSaving > availableForSaving) {
                                    isAchievable = false
                                    monthlyTarget = availableForSaving
                                    assistantViewModel.showMessage("To reach your goal by then, you'd need to save ${CurrencyUtils.formatCurrency(context, requiredSaving)}/mo, but you only have ${CurrencyUtils.formatCurrency(context, availableForSaving)} available.", AssistantState.THINKING)
                                } else {
                                    monthlyTarget = requiredSaving
                                }
                                dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(target)
                            } catch (ex: Exception) {
                                targetDate = ""
                            }
                        }
                        
                        if (dateStr.isEmpty()) {
                            // Calculate completion date based on monthlyTarget
                            if (monthlyTarget > 0) {
                                val monthsToReach = g / monthlyTarget
                                val calendar = Calendar.getInstance()
                                calendar.add(Calendar.DAY_OF_YEAR, (monthsToReach * 30).toInt())
                                dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
                            } else {
                                dateStr = "Never (No savings capacity)"
                                isAchievable = false
                            }
                        }

                        result = PlannerResult(
                            monthlySaving = monthlyTarget,
                            dailySpendLimit = (s - e - monthlyTarget) / 30,
                            completionDate = dateStr,
                            isAchievable = isAchievable
                        )
                        
                        if (isAchievable) {
                            assistantViewModel.showMessage("Planning complete! You need to save ${CurrencyUtils.formatCurrency(context, monthlyTarget)} per month. 🚀", AssistantState.HAPPY)
                        }
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
                    
                    SummaryItem("Monthly Savings Target:", CurrencyUtils.formatCurrency(context, result!!.monthlySaving), isDarkMode, highlight = true)
                    SummaryItem("Daily Spending Limit:", CurrencyUtils.formatCurrency(context, result!!.dailySpendLimit), isDarkMode)
                    SummaryItem("Estimated Completion:", result!!.completionDate, isDarkMode)
                    
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
