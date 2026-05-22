package com.example.visualvibefincal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import java.util.Locale

import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.ui.viewmodel.HistoryViewModel
import com.example.visualvibefincal.data.model.HistoryItem
import com.example.visualvibefincal.ui.components.ValidatedTextField
import com.example.visualvibefincal.utils.ValidationUtils
import com.example.visualvibefincal.R
import com.example.visualvibefincal.ui.viewmodel.AssistantViewModel
import com.example.visualvibefincal.ui.viewmodel.AssistantState
import com.example.visualvibefincal.ui.viewmodel.AssistantMessageType

@Composable
fun SalaryScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var annualSalary by remember { mutableStateOf("") }
    var annualError by remember { mutableStateOf<String?>(null) }
    var monthlyResult by remember { mutableStateOf<Double?>(null) }
    var weeklyResult by remember { mutableStateOf<Double?>(null) }
    var dailyResult by remember { mutableStateOf<Double?>(null) }

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["salary"] ?: emptyList()
    var isLoadingHistory by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1000)
        isLoadingHistory = false
    }

    val isInputValid = ValidationUtils.isValidPositiveNumeric(annualSalary)

    CalculatorScreenScaffold(
        title = "Salary Calculator",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            CalculatorCard(isDarkMode = isDarkMode) {
                val emptyError = stringResource(R.string.field_cannot_be_empty)
                val invalidError = "Must be a positive number"

                ValidatedTextField(
                    value = annualSalary,
                    onValueChange = { 
                        annualSalary = ValidationUtils.formatNumericInput(it, allowNegative = false)
                        annualError = if (annualSalary.isEmpty()) emptyError 
                                      else if (!ValidationUtils.isValidPositiveNumeric(annualSalary)) invalidError
                                      else null
                    },
                    label = "Annual Gross Salary",
                    error = annualError,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter your annual gross salary. Currently: $annualSalary"
                    }
                )

                Spacer(Modifier.height(32.dp))

                BouncyButton(
                    onClick = {
                        if (!isInputValid) return@BouncyButton
                        
                        assistantViewModel.showMessage("Calculating salary breakdown...", AssistantState.THINKING, AssistantMessageType.THOUGHT)
                        
                        val gross = annualSalary.toDoubleOrNull() ?: 0.0
                        if (gross > 0) {
                            val monthly = gross / 12
                            val weekly = gross / 52
                            val daily = gross / 260
                            monthlyResult = monthly
                            weeklyResult = weekly
                            dailyResult = daily

                            assistantViewModel.showMessage("Wow, ${String.format(Locale.getDefault(), "%.2f", monthly)} per month!", AssistantState.HAPPY)

                            historyViewModel.addToHistory(
                                "salary",
                                HistoryItem(
                                    title = "Annual: ${String.format(Locale.getDefault(), "%.2f", gross)}",
                                    result = "Monthly: ${String.format(Locale.getDefault(), "%.2f", monthly)}",
                                    details = "Weekly: ${String.format(Locale.getDefault(), "%.2f", weekly)} | Daily: ${String.format(Locale.getDefault(), "%.2f", daily)}"
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics {
                        contentDescription = "Calculate salary breakdown"
                    },
                    enabled = isInputValid
                ) {
                    Text("Calculate", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (monthlyResult != null) {
                    Spacer(Modifier.height(32.dp))
                    ResultDisplay(label = "Monthly", value = "${String.format(Locale.getDefault(), "%.2f", monthlyResult)}", isDarkMode = isDarkMode)
                    Spacer(Modifier.height(16.dp))
                    ResultDisplay(label = "Weekly", value = "${String.format(Locale.getDefault(), "%.2f", weeklyResult)}", isDarkMode = isDarkMode)
                    Spacer(Modifier.height(16.dp))
                    ResultDisplay(label = "Daily (approx)", value = "${String.format(Locale.getDefault(), "%.2f", dailyResult)}", isDarkMode = isDarkMode)
                }
            }

            HistorySection(
                screenKey = "salary",
                history = screenHistory,
                isDarkMode = isDarkMode,
                isLoading = isLoadingHistory,
                onClearHistory = { historyViewModel.clearHistory("salary") }
            )
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
