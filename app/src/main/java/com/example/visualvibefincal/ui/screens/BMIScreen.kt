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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.viewmodel.HistoryViewModel
import com.example.visualvibefincal.data.model.HistoryItem
import com.example.visualvibefincal.ui.components.ValidatedTextField
import com.example.visualvibefincal.utils.ValidationUtils
import com.example.visualvibefincal.R
import com.example.visualvibefincal.viewmodel.AssistantMessageType
import com.example.visualvibefincal.viewmodel.AssistantState
import com.example.visualvibefincal.viewmodel.AssistantViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import java.util.Locale

@Composable
fun BMIScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var bmiResult by remember { mutableStateOf<Double?>(null) }
    var bmiCategory by remember { mutableStateOf("") }

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["bmi"] ?: emptyList()
    var isLoadingHistory by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1000)
        isLoadingHistory = false
    }

    val isInputValid = ValidationUtils.isValidPositiveNumeric(weight) && 
                     ValidationUtils.isValidPositiveNumeric(height) &&
                     ValidationUtils.isValidPositiveNumeric(age)

    val coroutineScope = rememberCoroutineScope()

    CalculatorScreenScaffold(
        title = stringResource(R.string.bmi_calculator),
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                CalculatorCard(isDarkMode = isDarkMode) {
                    val emptyError = stringResource(R.string.field_cannot_be_empty)
                    val invalidError = stringResource(R.string.invalid_number)
                    val requiredError = stringResource(R.string.required)

                    ValidatedTextField(
                        value = weight,
                        onValueChange = { 
                            weight = ValidationUtils.formatNumericInput(it, allowNegative = false)
                            weightError = if (weight.isEmpty()) emptyError 
                                          else if (!ValidationUtils.isValidPositiveNumeric(weight)) invalidError
                                          else null
                        },
                        label = stringResource(R.string.weight_kg),
                        error = weightError,
                        modifier = Modifier.semantics {
                            contentDescription = "Enter your weight in kilograms. Currently: $weight"
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ValidatedTextField(
                        value = height,
                        onValueChange = { 
                            height = ValidationUtils.formatNumericInput(it, allowNegative = false)
                            heightError = if (height.isEmpty()) emptyError
                                          else if (!ValidationUtils.isValidPositiveNumeric(height)) invalidError
                                          else null
                        },
                        label = stringResource(R.string.height_cm),
                        error = heightError,
                        modifier = Modifier.semantics {
                            contentDescription = "Enter your height in centimeters. Currently: $height"
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    ValidatedTextField(
                        value = age,
                        onValueChange = { 
                            age = ValidationUtils.formatNumericInput(it, allowNegative = false)
                            ageError = if (age.isEmpty()) emptyError
                                       else if (!ValidationUtils.isValidPositiveNumeric(age)) invalidError
                                       else null
                        },
                        label = stringResource(R.string.age),
                        error = ageError,
                        modifier = Modifier.semantics {
                            contentDescription = "Enter your age. Currently: $age"
                        }
                    )

                    Spacer(Modifier.height(32.dp))

                    BouncyButton(
                        onClick = {
                            if (!isInputValid) {
                                if (weight.isEmpty()) weightError = requiredError
                                if (height.isEmpty()) heightError = requiredError
                                if (age.isEmpty()) ageError = requiredError
                                return@BouncyButton
                            }
                            val w = weight.toDoubleOrNull() ?: 0.0
                            val h = (height.toDoubleOrNull() ?: 0.0) / 100 // convert cm to meters
                            val a = age.toIntOrNull() ?: 0
                            if (w > 0 && h > 0) {
                                val bmi = w / (h * h)
                                bmiResult = bmi
                                
                                // Standard categories for adults
                                bmiCategory = when {
                                    bmi < 18.5 -> "Underweight"
                                    bmi < 25 -> "Healthy weight"
                                    bmi < 30 -> "Overweight"
                                    else -> "Obese"
                                }

                                assistantViewModel.showMessage("Checking your BMI for age $a...", AssistantState.THINKING, AssistantMessageType.THOUGHT, durationMs = 1500)
                                
                                coroutineScope.launch {
                                    delay(1500)
                                    val msg = when (bmiCategory) {
                                        "Healthy weight" -> "You're in the healthy range! 🌟"
                                        "Underweight" -> "You might need a more balanced diet to reach a healthy weight. 🍎"
                                        "Overweight" -> "Small changes in diet and activity can make a big difference! 🏃‍♂️"
                                        else -> "It's worth chatting with a doctor about your health goals. ❤️"
                                    }
                                    assistantViewModel.showMessage(msg, AssistantState.HAPPY)
                                }

                                historyViewModel.addToHistory(
                                    "bmi",
                                    HistoryItem(
                                        title = "BMI: ${String.format(Locale.getDefault(), "%.1f", bmi)}",
                                        result = "$bmiCategory (Age: $a)",
                                        details = "Weight: $w kg | Height: ${height} cm"
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = isInputValid
                    ) {
                        Text(stringResource(R.string.calculate_bmi), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    if (bmiResult != null) {
                        Spacer(Modifier.height(32.dp))
                        ResultDisplay(label = stringResource(R.string.your_bmi), value = "${String.format(Locale.getDefault(), "%.1f", bmiResult)}", isDarkMode = isDarkMode)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            when(bmiCategory) {
                                "Underweight" -> stringResource(R.string.underweight)
                                "Healthy weight" -> stringResource(R.string.healthy_weight)
                                "Overweight" -> stringResource(R.string.overweight)
                                else -> stringResource(R.string.obese)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (bmiCategory) {
                                "Healthy weight" -> Color(0xFF00D1B2)
                                "Underweight" -> Color.Blue
                                "Overweight" -> Color.Yellow
                                else -> Color.Red
                            }
                        )
                        Text(
                            stringResource(R.string.bmi_guidelines),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 12.sp,
                            color = if (isDarkMode) Color.Gray else Color.DarkGray
                        )
                    }
                }
                
                HistorySection(
                    screenKey = "bmi",
                    history = screenHistory,
                    isDarkMode = isDarkMode,
                    isLoading = isLoadingHistory,
                    onClearHistory = { historyViewModel.clearHistory("bmi") }
                )
                
                Spacer(Modifier.height(24.dp))
            }

            VerticalScrollbar(
                scrollState = scrollState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd).padding(end = 2.dp)
            )
        }
    }
}
