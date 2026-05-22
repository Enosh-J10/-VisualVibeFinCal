package com.example.visualvibefincal.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.ui.viewmodel.HistoryViewModel
import com.example.visualvibefincal.data.model.HistoryItem
import com.example.visualvibefincal.ui.viewmodel.AssistantViewModel
import com.example.visualvibefincal.ui.viewmodel.AssistantState
import com.example.visualvibefincal.ui.viewmodel.AssistantMessageType

@Composable
fun DateTimeScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf(Calendar.getInstance()) }
    var endDate by remember { mutableStateOf(Calendar.getInstance()) }
    var diffResult by remember { mutableStateOf<String?>(null) }
    
    var timeValue by remember { mutableStateOf("") }
    var fromTimeUnit by remember { mutableStateOf("Hours") }
    var toTimeUnit by remember { mutableStateOf("Minutes") }
    var timeConvResult by remember { mutableStateOf<Double?>(null) }

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["datetime"] ?: emptyList()

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val scrollState = rememberScrollState()

    CalculatorScreenScaffold(
        title = "Date/Time Calculator",
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
                    Text("Calculate difference between dates", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                    Spacer(Modifier.height(24.dp))

                    Text("Start Date", fontSize = 14.sp, color = Color.Gray)
                    OutlinedButton(
                        onClick = {
                            val d = DatePickerDialog(context, { _, y, m, day ->
                                val cal = Calendar.getInstance()
                                cal.set(y, m, day)
                                startDate = cal
                            }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH))
                            d.show()
                        },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Select start date. Currently: ${sdf.format(startDate.time)}"
                        }
                    ) {
                        Text(sdf.format(startDate.time), color = if (isDarkMode) Color.White else Color.Black)
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("End Date", fontSize = 14.sp, color = Color.Gray)
                    OutlinedButton(
                        onClick = {
                            val d = DatePickerDialog(context, { _, y, m, day ->
                                val cal = Calendar.getInstance()
                                cal.set(y, m, day)
                                endDate = cal
                            }, endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH), endDate.get(Calendar.DAY_OF_MONTH))
                            d.show()
                        },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Select end date. Currently: ${sdf.format(endDate.time)}"
                        }
                    ) {
                        Text(sdf.format(endDate.time), color = if (isDarkMode) Color.White else Color.Black)
                    }

                    Spacer(Modifier.height(32.dp))

                    BouncyButton(
                        onClick = {
                            val diff = endDate.timeInMillis - startDate.timeInMillis
                            if (diff < 0) {
                                diffResult = "Invalid range: End date must be after start date."
                                assistantViewModel.showMessage("End date must be after start date!", AssistantState.ERROR)
                                return@BouncyButton
                            }
                            
                            assistantViewModel.showMessage("Calculating time difference...", AssistantState.THINKING, AssistantMessageType.THOUGHT)

                            val startCal = Calendar.getInstance().apply { timeInMillis = startDate.timeInMillis }
                            val endCal = Calendar.getInstance().apply { timeInMillis = endDate.timeInMillis }
                            
                            var years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)
                            var months = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH)
                            var days = endCal.get(Calendar.DAY_OF_MONTH) - startCal.get(Calendar.DAY_OF_MONTH)
                            
                            if (days < 0) {
                                months -= 1
                                val tempCal = Calendar.getInstance().apply {
                                    timeInMillis = endCal.timeInMillis
                                    add(Calendar.MONTH, -1)
                                }
                                days += tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            }
                            
                            if (months < 0) {
                                years -= 1
                                months += 12
                            }
                            
                            val result = "$years years, $months months, $days days"
                            diffResult = result

                            assistantViewModel.showMessage("It's a duration of $years years and $months months!", AssistantState.HAPPY)

                            historyViewModel.addToHistory(
                                "datetime",
                                HistoryItem(
                                    title = "Date Difference",
                                    result = result,
                                    details = "From: ${sdf.format(startDate.time)} | To: ${sdf.format(endDate.time)}"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).semantics {
                            contentDescription = "Calculate date difference"
                        }
                    ) {
                        Text("Calculate Difference", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    if (diffResult != null) {
                        Spacer(Modifier.height(32.dp))
                        ResultDisplay(label = "Time Duration", value = diffResult!!, isDarkMode = isDarkMode)
                    }
                }

            Spacer(Modifier.height(16.dp))

            CalculatorCard(isDarkMode = isDarkMode) {
                Text("Time Conversion", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Spacer(Modifier.height(24.dp))

                val timeUnits = listOf("Seconds", "Minutes", "Hours", "Days", "Weeks", "Months (30d)", "Years (365d)")

                com.example.visualvibefincal.ui.components.ValidatedTextField(
                    value = timeValue,
                    onValueChange = {
                        timeValue = com.example.visualvibefincal.utils.ValidationUtils.formatNumericInput(it)
                    },
                    label = "Value",
                    error = null,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter time value to convert. Currently: $timeValue"
                    }
                )

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("From", fontSize = 12.sp, color = Color.Gray)
                        UnitDropdown(
                            selectedUnit = fromTimeUnit, 
                            units = timeUnits, 
                            isDarkMode = isDarkMode,
                            modifier = Modifier.semantics {
                                contentDescription = "Select from unit. Currently: $fromTimeUnit"
                            }
                        ) { fromTimeUnit = it }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("To", fontSize = 12.sp, color = Color.Gray)
                        UnitDropdown(
                            selectedUnit = toTimeUnit, 
                            units = timeUnits, 
                            isDarkMode = isDarkMode,
                            modifier = Modifier.semantics {
                                contentDescription = "Select to unit. Currently: $toTimeUnit"
                            }
                        ) { toTimeUnit = it }
                    }
                }

                Spacer(Modifier.height(32.dp))

                BouncyButton(
                    onClick = {
                        assistantViewModel.showMessage("Converting time units...", AssistantState.THINKING, AssistantMessageType.THOUGHT)
                        val v = timeValue.toDoubleOrNull() ?: 0.0
                        val result = convertTime(v, fromTimeUnit, toTimeUnit)
                        timeConvResult = result

                        assistantViewModel.showMessage("Converted! It's ${String.format(Locale.getDefault(), "%.2f", result)} $toTimeUnit", AssistantState.HAPPY)

                        historyViewModel.addToHistory(
                            "datetime",
                            HistoryItem(
                                title = "Time Conversion",
                                result = "${String.format(Locale.getDefault(), "%.4f", result)} $toTimeUnit",
                                details = "$v $fromTimeUnit to $toTimeUnit"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics {
                        contentDescription = "Perform time unit conversion"
                    },
                    enabled = timeValue.isNotEmpty()
                ) {
                    Text("Convert Time", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (timeConvResult != null) {
                    Spacer(Modifier.height(32.dp))
                    ResultDisplay(
                        label = "Converted Value", 
                        value = "${String.format(Locale.getDefault(), "%.4f", timeConvResult)} $toTimeUnit", 
                        isDarkMode = isDarkMode
                    )
                }
            }

            HistorySection(
                screenKey = "datetime",
                history = screenHistory,
                isDarkMode = isDarkMode,
                onClearHistory = { historyViewModel.clearHistory("datetime") }
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

@Composable
fun UnitDropdown(
    selectedUnit: String,
    units: List<String>,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkMode) Color.White else Color.Black)
        ) {
            Text(selectedUnit)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = androidx.compose.ui.Modifier.background(if (isDarkMode) Color(0xFF203A43) else Color.White)
        ) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit, color = if (isDarkMode) Color.White else Color.Black) },
                    onClick = { onSelect(unit); expanded = false }
                )
            }
        }
    }
}

fun convertTime(value: Double, from: String, to: String): Double {
    if (from == to) return value
    val inSeconds = when (from) {
        "Seconds" -> value
        "Minutes" -> value * 60
        "Hours" -> value * 3600
        "Days" -> value * 86400
        "Weeks" -> value * 604800
        "Months (30d)" -> value * 2592000
        "Years (365d)" -> value * 31536000
        else -> value
    }
    return when (to) {
        "Seconds" -> inSeconds
        "Minutes" -> inSeconds / 60
        "Hours" -> inSeconds / 3600
        "Days" -> inSeconds / 86400
        "Weeks" -> inSeconds / 604800
        "Months (30d)" -> inSeconds / 2592000
        "Years (365d)" -> inSeconds / 31536000
        else -> inSeconds
    }
}
