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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import java.util.Locale
import kotlin.math.pow

import androidx.lifecycle.viewmodel.compose.viewModel
import com.enosh.fincalc.viewmodel.HistoryViewModel
import com.enosh.fincalc.data.model.HistoryItem
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.ValidationUtils

import com.enosh.fincalc.viewmodel.AssistantMessageType
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.utils.NotificationHelper
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.utils.CurrencyUtils
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch

data class LoanSummary(
    val principal: Double,
    val downPayment: Double,
    val loanAmount: Double,
    val monthlyPayment: Double,
    val totalInterest: Double
)

@Composable
fun LoanCalculatorScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var principal by remember { mutableStateOf("") }
    var downPayment by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var loanTerm by remember { mutableStateOf("") }
    var principalError by remember { mutableStateOf<String?>(null) }
    var downPaymentError by remember { mutableStateOf<String?>(null) }
    var interestRateError by remember { mutableStateOf<String?>(null) }
    var loanTermError by remember { mutableStateOf<String?>(null) }
    var loanSummary by remember { mutableStateOf<LoanSummary?>(null) }

    val isValid = principal.isNotEmpty() && interestRate.isNotEmpty() && loanTerm.isNotEmpty() &&
            principalError == null && downPaymentError == null && interestRateError == null && loanTermError == null &&
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
                            
                            // Validate down payment against new principal
                            val p = principal.toDoubleOrNull() ?: 0.0
                            val dp = downPayment.toDoubleOrNull() ?: 0.0
                            downPaymentError = if (dp > p) "Down payment cannot exceed principal" else null
                        },
                        label = "Principal Amount",
                        error = principalError,
                        modifier = Modifier.semantics {
                            contentDescription = "Enter principal amount. Currently: $principal"
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ValidatedTextField(
                        value = downPayment,
                        onValueChange = {
                            downPayment = ValidationUtils.formatNumericInput(it, allowNegative = false)
                            val p = principal.toDoubleOrNull() ?: 0.0
                            val dp = downPayment.toDoubleOrNull() ?: 0.0
                            downPaymentError = when {
                                dp > p -> "Down payment cannot exceed principal"
                                it.isNotEmpty() && !ValidationUtils.isValidPositiveNumeric(it) -> "Invalid amount"
                                else -> null
                            }
                        },
                        label = "Down Payment (Optional)",
                        error = downPaymentError,
                        modifier = Modifier.semantics {
                            contentDescription = "Enter down payment amount. Currently: $downPayment"
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
                        val pOrig = principal.toDoubleOrNull() ?: 0.0
                        val dp = downPayment.toDoubleOrNull() ?: 0.0
                        val p = pOrig - dp
                        val rPercent = interestRate.toDoubleOrNull() ?: 0.0
                        val r = rPercent / 100 / 12
                        val nYears = loanTerm.toIntOrNull() ?: 0
                        val n = nYears * 12

                        if (p >= 0 && n > 0 && pOrig > 0) {
                            val emi = if (r > 0) {
                                (p * r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
                            } else {
                                if (n > 0) p / n else 0.0
                            }
                            val interest = (emi * n) - p
                            
                            loanSummary = LoanSummary(
                                principal = pOrig,
                                downPayment = dp,
                                loanAmount = p,
                                monthlyPayment = emi,
                                totalInterest = interest
                            )

                            assistantViewModel.showMessage("Calculating loan details...", AssistantState.THINKING, AssistantMessageType.THOUGHT, durationMs = 2000)
                            
                            coroutineScope.launch {
                                delay(2000)
                                assistantViewModel.showMessage("All set! Here's your plan 📝", AssistantState.HAPPY)
                                NotificationHelper.showNotification(context, "Loan Calculated", "Monthly Payment: ${CurrencyUtils.formatCurrency(context, emi)}")
                            }
                            
                            historyViewModel.addToHistory(
                                "loan",
                                HistoryItem(
                                    title = "Loan: ${CurrencyUtils.formatCurrency(context, p)}",
                                    result = "Monthly: ${CurrencyUtils.formatCurrency(context, emi)}",
                                    details = "Int: $rPercent% | Term: ${loanTerm} yrs | Total Int: ${CurrencyUtils.formatCurrency(context, interest)}"
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

                if (loanSummary != null) {
                    Spacer(Modifier.height(32.dp))
                    CalculatorCard(isDarkMode = isDarkMode) {
                        Text("Loan Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF00D1B2))
                        Spacer(Modifier.height(16.dp))
                        SummaryItem("Principal Amount", CurrencyUtils.formatCurrency(context, loanSummary!!.principal), isDarkMode)
                        SummaryItem("Down Payment", CurrencyUtils.formatCurrency(context, loanSummary!!.downPayment), isDarkMode)
                        SummaryItem("Loan Amount After Down Payment", CurrencyUtils.formatCurrency(context, loanSummary!!.loanAmount), isDarkMode)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
                        SummaryItem("Monthly Payment", CurrencyUtils.formatCurrency(context, loanSummary!!.monthlyPayment), isDarkMode, highlight = true)
                        SummaryItem("Total Interest", CurrencyUtils.formatCurrency(context, loanSummary!!.totalInterest), isDarkMode)
                    }
                }

                HistorySection(
                    screenKey = "loan",
                    history = screenHistory,
                    isDarkMode = isDarkMode,
                    isLoading = isLoadingHistory,
                    onClearHistory = { historyViewModel.clearHistory("loan") }
                )
                
                Spacer(Modifier.height(32.dp))
            }

            VerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).padding(end = 2.dp)
            )
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, isDarkMode: Boolean, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray)
        Text(
            value, 
            fontSize = if (highlight) 18.sp else 16.sp, 
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) Color(0xFF00D1B2) else (if (isDarkMode) Color.White else Color.Black)
        )
    }
}
