package com.example.visualvibefincal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.visualvibefincal.ui.viewmodel.HistoryViewModel
import com.example.visualvibefincal.data.model.HistoryItem
import com.example.visualvibefincal.ui.viewmodel.AssistantViewModel
import com.example.visualvibefincal.ui.viewmodel.AssistantState
import com.example.visualvibefincal.ui.viewmodel.AssistantMessageType
import com.example.visualvibefincal.ui.components.ValidatedTextField
import com.example.visualvibefincal.utils.ValidationUtils

@Composable
fun PercentageScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }
    var value1Error by remember { mutableStateOf<String?>(null) }
    var value2Error by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Double?>(null) }
    var selectedOperation by remember { mutableIntStateOf(0) } // 0: X is what % of Y, 1: What is X% of Y, 2: % Increase/Decrease

    fun validateInputs(
        v1: String,
        v2: String,
        op: Int,
        setError1: (String?) -> Unit,
        setError2: (String?) -> Unit
    ) {
        setError1(if (v1.isEmpty()) "Enter a value" 
                  else if (!ValidationUtils.isValidNumeric(v1)) "Invalid number"
                  else null)
        setError2(if (v2.isEmpty()) "Enter a value" 
                  else if (!ValidationUtils.isValidNumeric(v2)) "Invalid number"
                  else null)
        
        if (v1.isNotEmpty() && op == 2 && v1.toDoubleOrNull() == 0.0) {
            setError1("Original value cannot be zero")
        }
        if (v2.isNotEmpty() && op == 0 && v2.toDoubleOrNull() == 0.0) {
            setError2("Value Y cannot be zero")
        }
    }

    val isValid = value1.isNotEmpty() && value2.isNotEmpty() && value1Error == null && value2Error == null

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["percentage"] ?: emptyList()

    CalculatorScreenScaffold(
        title = "Percentage",
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
                val operations = listOf("X is what % of Y?", "What is X% of Y?", "% Increase/Decrease")
                
                Text("Select Operation", fontSize = 14.sp, color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray)
                Spacer(Modifier.height(8.dp))
                
                operations.forEachIndexed { index, op ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                selectedOperation = index
                                result = null
                                // Re-validate when operation changes
                                validateInputs(value1, value2, index, { value1Error = it }, { value2Error = it })
                            }
                            .semantics {
                                contentDescription = "Select operation: $op"
                            },
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOperation == index,
                            onClick = { 
                                selectedOperation = index
                                result = null
                                // Re-validate when operation changes
                                validateInputs(value1, value2, index, { value1Error = it }, { value2Error = it })
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00D1B2))
                        )
                        Text(op, color = if (isDarkMode) Color.White else Color.Black)
                    }
                }

                Spacer(Modifier.height(16.dp))

                val label1 = when(selectedOperation) {
                    2 -> "Original Value (X)"
                    else -> "Value X"
                }
                val label2 = when(selectedOperation) {
                    2 -> "New Value (Y)"
                    else -> "Value Y"
                }

                ValidatedTextField(
                    value = value1,
                    onValueChange = {
                        value1 = ValidationUtils.formatNumericInput(it)
                        validateInputs(value1, value2, selectedOperation, { value1Error = it }, { value2Error = it })
                    },
                    label = label1,
                    error = value1Error,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter $label1. Currently: $value1"
                    }
                )

                Spacer(Modifier.height(16.dp))

                ValidatedTextField(
                    value = value2,
                    onValueChange = {
                        value2 = ValidationUtils.formatNumericInput(it)
                        validateInputs(value1, value2, selectedOperation, { value1Error = it }, { value2Error = it })
                    },
                    label = label2,
                    error = value2Error,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter $label2. Currently: $value2"
                    }
                )

                Spacer(Modifier.height(32.dp))

                BouncyButton(
                    onClick = {
                        val v1 = value1.toDoubleOrNull() ?: 0.0
                        val v2 = value2.toDoubleOrNull() ?: 0.0
                        
                        assistantViewModel.showMessage("Calculating percentage...", AssistantState.THINKING, AssistantMessageType.THOUGHT)

                        val calculatedResult = when(selectedOperation) {
                            0 -> if (v2 != 0.0) (v1 / v2) * 100 else 0.0
                            1 -> (v1 / 100) * v2
                            2 -> if (v1 != 0.0) ((v2 - v1) / v1) * 100 else 0.0
                            else -> 0.0
                        }
                        
                        if (calculatedResult.isInfinite() || calculatedResult.isNaN()) {
                            result = null
                            value1Error = "Result out of range"
                            assistantViewModel.showMessage("Hmm, that's complex!", AssistantState.ERROR)
                            return@BouncyButton
                        }

                        result = calculatedResult
                        assistantViewModel.showMessage("Result is ${String.format(Locale.getDefault(), "%.2f", calculatedResult)}", AssistantState.HAPPY)

                        val resultSuffix = if (selectedOperation == 1) "" else "%"
                        val operationText = when(selectedOperation) {
                            0 -> "$v1 is what % of $v2?"
                            1 -> "What is $v1% of $v2?"
                            2 -> "Increase/Decrease from $v1 to $v2"
                            else -> ""
                        }

                        historyViewModel.addToHistory(
                            "percentage",
                            HistoryItem(
                                title = operationText,
                                result = "${String.format(Locale.getDefault(), "%.2f", calculatedResult)}$resultSuffix",
                                details = "X: $v1 | Y: $v2"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics {
                        contentDescription = "Calculate percentage operation"
                    },
                    enabled = isValid
                ) {
                    Text("Calculate", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (result != null) {
                    Spacer(Modifier.height(32.dp))
                    val resultSuffix = if (selectedOperation == 1) "" else "%"
                    ResultDisplay(label = "Result", value = "${String.format(Locale.getDefault(), "%.2f", result)}$resultSuffix", isDarkMode = isDarkMode)
                }
            }

            HistorySection(
                screenKey = "percentage",
                history = screenHistory,
                isDarkMode = isDarkMode,
                onClearHistory = { historyViewModel.clearHistory("percentage") }
            )
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
