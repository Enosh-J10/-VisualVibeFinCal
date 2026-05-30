package com.enosh.fincalc.ui.screens

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
import com.enosh.fincalc.viewmodel.HistoryViewModel
import com.enosh.fincalc.data.model.HistoryItem
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.ValidationUtils
import com.enosh.fincalc.R
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.viewmodel.AssistantMessageType
import com.enosh.fincalc.utils.CurrencyUtils

@Composable
fun SalaryScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

                            assistantViewModel.showMessage("Wow, ${CurrencyUtils.formatCurrency(context, monthly)} per month!", AssistantState.HAPPY)

                            historyViewModel.addToHistory(
                                "salary",
                                HistoryItem(
                                    title = "Annual: ${CurrencyUtils.formatCurrency(context, gross)}",
                                    result = "Monthly: ${CurrencyUtils.formatCurrency(context, monthly)}",
                                    details = "Weekly: ${CurrencyUtils.formatCurrency(context, weekly)} | Daily: ${CurrencyUtils.formatCurrency(context, daily)}"
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
                    ResultDisplay(label = "Monthly", value = CurrencyUtils.formatCurrency(context, monthlyResult!!), isDarkMode = isDarkMode)
                    Spacer(Modifier.height(16.dp))
                    ResultDisplay(label = "Weekly", value = CurrencyUtils.formatCurrency(context, weeklyResult!!), isDarkMode = isDarkMode)
                    Spacer(Modifier.height(16.dp))
                    ResultDisplay(label = "Daily (approx)", value = CurrencyUtils.formatCurrency(context, dailyResult!!), isDarkMode = isDarkMode)
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
