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

import androidx.lifecycle.viewmodel.compose.viewModel
import com.enosh.fincalc.viewmodel.HistoryViewModel
import com.enosh.fincalc.data.model.HistoryItem
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.ValidationUtils

import com.enosh.fincalc.viewmodel.AssistantMessageType
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.viewmodel.AssistantViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TipCalculatorScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var billAmount by remember { mutableStateOf("") }
    var tipPercentage by remember { mutableStateOf("15") }
    var numberOfPeople by remember { mutableStateOf("1") }
    var billAmountError by remember { mutableStateOf<String?>(null) }
    var tipPercentageError by remember { mutableStateOf<String?>(null) }
    var numberOfPeopleError by remember { mutableStateOf<String?>(null) }
    var totalTip by remember { mutableStateOf<Double?>(null) }
    var totalPerPerson by remember { mutableStateOf<Double?>(null) }

    val isValid = billAmount.isNotEmpty() && billAmountError == null && tipPercentageError == null && numberOfPeopleError == null

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["tip"] ?: emptyList()

    val coroutineScope = rememberCoroutineScope()

    CalculatorScreenScaffold(
        title = "Tip & Split Calculator",
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
                ValidatedTextField(
                    value = billAmount,
                    onValueChange = {
                        billAmount = ValidationUtils.formatNumericInput(it)
                        billAmountError = if (billAmount.isEmpty()) "Enter bill amount" else null
                    },
                    label = "Bill Amount",
                    error = billAmountError,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter total bill amount. Currently: $billAmount"
                    }
                )

                Spacer(Modifier.height(16.dp))

                ValidatedTextField(
                    value = tipPercentage,
                    onValueChange = {
                        tipPercentage = ValidationUtils.formatNumericInput(it)
                        tipPercentageError = if (tipPercentage.isEmpty()) "Enter tip %" else null
                    },
                    label = "Tip Percentage (%)",
                    error = tipPercentageError,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter tip percentage. Currently: $tipPercentage percent"
                    }
                )

                Spacer(Modifier.height(16.dp))

                ValidatedTextField(
                    value = numberOfPeople,
                    onValueChange = {
                        numberOfPeople = it.filter { char -> char.isDigit() }
                        numberOfPeopleError = when {
                            numberOfPeople.isEmpty() -> "Enter number of people"
                            numberOfPeople.toIntOrNull() == 0 -> "Must be at least 1"
                            else -> null
                        }
                    },
                    label = "Number of People",
                    error = numberOfPeopleError,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter number of people to split the bill. Currently: $numberOfPeople"
                    }
                )

                Spacer(Modifier.height(32.dp))

                BouncyButton(
                    onClick = {
                        val bill = billAmount.toDoubleOrNull() ?: 0.0
                        val tip = (tipPercentage.toDoubleOrNull() ?: 0.0) / 100
                        val people = numberOfPeople.toIntOrNull() ?: 1

                        if (bill > 0 && people > 0) {
                            val calculatedTip = bill * tip
                            val calculatedTotalPerPerson = (bill + calculatedTip) / people
                            totalTip = calculatedTip
                            totalPerPerson = calculatedTotalPerPerson

                            assistantViewModel.showMessage("Calculating the split for you...", AssistantState.THINKING, AssistantMessageType.THOUGHT, durationMs = 1500)
                            
                            coroutineScope.launch {
                                delay(1500)
                                assistantViewModel.showMessage("Done! That was quick 💡", AssistantState.HAPPY)
                            }

                            historyViewModel.addToHistory(
                                "tip",
                                HistoryItem(
                                    title = "Bill: $${String.format(Locale.getDefault(), "%.2f", bill)}",
                                    result = "Total per Person: $${String.format(Locale.getDefault(), "%.2f", calculatedTotalPerPerson)}",
                                    details = "Tip: $${String.format(Locale.getDefault(), "%.2f", calculatedTip)} ($tipPercentage%) | People: $people"
                                )
                            )
                        } else {
                            assistantViewModel.showMessage("Oops! Check your numbers 😅", AssistantState.ERROR)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics {
                        contentDescription = "Calculate tip and split the bill"
                    },
                    enabled = isValid
                ) {
                    Text("Calculate", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (totalTip != null && totalPerPerson != null) {
                    Spacer(Modifier.height(32.dp))
                    ResultDisplay(label = "Total Tip", value = "${String.format(Locale.getDefault(), "%.2f", totalTip)}", isDarkMode = isDarkMode)
                    Spacer(Modifier.height(16.dp))
                    ResultDisplay(label = "Total Per Person", value = "${String.format(Locale.getDefault(), "%.2f", totalPerPerson)}", isDarkMode = isDarkMode)
                }
            }
            
            HistorySection(
                screenKey = "tip",
                history = screenHistory,
                isDarkMode = isDarkMode,
                onClearHistory = { historyViewModel.clearHistory("tip") }
            )
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
