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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import java.util.Locale
import kotlin.math.pow

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.viewmodel.HistoryViewModel
import com.example.visualvibefincal.data.model.HistoryItem
import com.example.visualvibefincal.ui.components.ValidatedTextField
import com.example.visualvibefincal.utils.ValidationUtils

import com.example.visualvibefincal.viewmodel.AssistantMessageType
import com.example.visualvibefincal.viewmodel.AssistantState
import com.example.visualvibefincal.utils.NotificationHelper
import com.example.visualvibefincal.viewmodel.AssistantViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoanCalculatorScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var principal by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var loanTerm by remember { mutableStateOf("") }
    var principalError by remember { mutableStateOf<String?>(null) }
    var interestRateError by remember { mutableStateOf<String?>(null) }
    var loanTermError by remember { mutableStateOf<String?>(null) }
    var monthlyPayment by remember { mutableStateOf<Double?>(null) }
    var totalInterest by remember { mutableStateOf<Double?>(null) }

    val isValid = principal.isNotEmpty() && interestRate.isNotEmpty() && loanTerm.isNotEmpty() &&
            principalError == null && interestRateError == null && loanTermError == null &&
            ValidationUtils.isValidPositiveNumeric(principal) && ValidationUtils.isValidPositiveNumeric(interestRate)

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["loan"] ?: emptyList()
    var isLoadingHistory by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1000)
        isLoadingHistory = false
    }

    val scrollState = rememberScrollState()

    val coroutineScope = rememberCoroutineScope()

    CalculatorScreenScaffold(
        title = "Loan Calculator",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                CalculatorCard(isDarkMode = isDarkMode) {
                    ValidatedTextField(
                        value = principal,
                        onValueChange = {
                            principal = ValidationUtils.formatNumericInput(it, allowNegative = false)
                            principalError = if (principal.isEmpty()) "Enter principal" 
                                             else if (!ValidationUtils.isValidPositiveNumeric(principal)) "Invalid amount"
                                             else null
                        },
                        label = "Principal Amount",
                        error = principalError,
                        modifier = Modifier.semantics {
                            contentDescription = "Enter principal amount. Currently: $principal"
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ValidatedTextField(
                        value = interestRate,
                        onValueChange = {
                            interestRate = ValidationUtils.formatNumericInput(it, allowNegative = false)
                            interestRateError = if (interestRate.isEmpty()) "Enter interest rate" 
                                                 else if (!ValidationUtils.isValidPositiveNumeric(interestRate)) "Invalid rate"
                                                 else null
                        },
                        label = "Annual Interest Rate (%)",
                        error = interestRateError,
                        modifier = Modifier.semantics {
                            contentDescription = "Enter annual interest rate. Currently: $interestRate percent"
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                ValidatedTextField(
                    value = loanTerm,
                    onValueChange = {
                        loanTerm = it.filter { char -> char.isDigit() }
                        loanTermError = when {
                            loanTerm.isEmpty() -> "Enter loan term"
                            loanTerm.toIntOrNull() == 0 -> "Must be at least 1 year"
                            else -> null
                        }
                    },
                    label = "Loan Term (Years)",
                    error = loanTermError,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.padding(horizontal = 24.dp).semantics {
                        contentDescription = "Enter loan term in years. Currently: $loanTerm"
                    }
                )

                Spacer(Modifier.height(32.dp))

                BouncyButton(
                    onClick = {
                        val p = principal.toDoubleOrNull() ?: 0.0
                        val rPercent = interestRate.toDoubleOrNull() ?: 0.0
                        val r = rPercent / 100 / 12
                        val nYears = loanTerm.toIntOrNull() ?: 0
                        val n = nYears * 12

                        if (p > 0 && n > 0) {
                            val emi = if (r > 0) {
                                (p * r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
                            } else {
                                p / n
                            }
                            val interest = (emi * n) - p
                            monthlyPayment = emi
                            totalInterest = interest

                            assistantViewModel.showMessage("Calculating loan details...", AssistantState.THINKING, AssistantMessageType.THOUGHT, durationMs = 2000)
                            
                            coroutineScope.launch {
                                delay(2000)
                                assistantViewModel.showMessage("All set! Here's your plan 📝", AssistantState.HAPPY)
                                NotificationHelper.showNotification(context, "Loan Calculated", "Monthly Payment: ${String.format(Locale.getDefault(), "%.2f", emi)}")
                            }
                            
                            historyViewModel.addToHistory(
                                "loan",
                                HistoryItem(
                                    title = "Principal: ${String.format(Locale.getDefault(), "%.2f", p)}",
                                    result = "Monthly: ${String.format(Locale.getDefault(), "%.2f", emi)}",
                                    details = "Interest: $rPercent% | Term: ${loanTerm} yrs | Total Interest: ${String.format(Locale.getDefault(), "%.2f", interest)}"
                                )
                            )
                        } else {
                            assistantViewModel.showMessage("Check your inputs! 😅", AssistantState.ERROR)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp).semantics {
                        contentDescription = "Calculate loan payment"
                    },
                    enabled = isValid
                ) {
                    Text("Calculate", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (monthlyPayment != null) {
                    Spacer(Modifier.height(32.dp))
                    ResultDisplay(label = "Monthly Payment", value = "${String.format(Locale.getDefault(), "%.2f", monthlyPayment)}", isDarkMode = isDarkMode)
                    Spacer(Modifier.height(16.dp))
                    ResultDisplay(label = "Total Interest", value = "${String.format(Locale.getDefault(), "%.2f", totalInterest)}", isDarkMode = isDarkMode)
                }
            }

            HistorySection(
                screenKey = "loan",
                history = screenHistory,
                isDarkMode = isDarkMode,
                isLoading = isLoadingHistory,
                onClearHistory = { historyViewModel.clearHistory("loan") }
            )
            
            Spacer(Modifier.height(24.dp))

            VerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).padding(end = 2.dp)
            )
        }
    }
}
