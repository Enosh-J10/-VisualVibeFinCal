package com.enosh.fincalc.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.enosh.fincalc.viewmodel.HistoryViewModel
import com.enosh.fincalc.data.model.HistoryItem
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.viewmodel.AssistantMessageType
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.ValidationUtils

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
    val inputSdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val dateOnlySdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var startText by remember { mutableStateOf(inputSdf.format(startDate.time)) }
    var endText by remember { mutableStateOf(inputSdf.format(endDate.time)) }
    var startError by remember { mutableStateOf<String?>(null) }
    var endError by remember { mutableStateOf<String?>(null) }

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

                    Text("Start Date & Time", fontSize = 14.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ValidatedTextField(
                            value = startText,
                            onValueChange = { 
                                startText = it
                                try {
                                    val date = if (it.length <= 10) dateOnlySdf.parse(it) else inputSdf.parse(it)
                                    if (date != null) {
                                        startDate = Calendar.getInstance().apply { time = date }
                                        startError = null
                                    }
                                } catch (e: Exception) {
                                    startError = "Format: DD/MM/YYYY HH:MM:SS"
                                }
                            },
                            label = "DD/MM/YYYY HH:MM:SS",
                            modifier = Modifier.weight(1f),
                            error = startError,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                        )
                        IconButton(onClick = {
                            DatePickerDialog(context, { _, y, m, day ->
                                TimePickerDialog(context, { _, h, min ->
                                    val cal = Calendar.getInstance()
                                    cal.set(y, m, day, h, min, 0)
                                    startDate = cal
                                    startText = inputSdf.format(cal.time)
                                    startError = null
                                }, startDate.get(Calendar.HOUR_OF_DAY), startDate.get(Calendar.MINUTE), true).show()
                            }, startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DAY_OF_MONTH)).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date & Time")
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("End Date & Time", fontSize = 14.sp, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ValidatedTextField(
                            value = endText,
                            onValueChange = { 
                                endText = it
                                try {
                                    val date = if (it.length <= 10) dateOnlySdf.parse(it) else inputSdf.parse(it)
                                    if (date != null) {
                                        endDate = Calendar.getInstance().apply { time = date }
                                        endError = null
                                    }
                                } catch (e: Exception) {
                                    endError = "Format: DD/MM/YYYY HH:MM:SS"
                                }
                            },
                            label = "DD/MM/YYYY HH:MM:SS",
                            modifier = Modifier.weight(1f),
                            error = endError,
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                        )
                        IconButton(onClick = {
                            DatePickerDialog(context, { _, y, m, day ->
                                TimePickerDialog(context, { _, h, min ->
                                    val cal = Calendar.getInstance()
                                    cal.set(y, m, day, h, min, 0)
                                    endDate = cal
                                    endText = inputSdf.format(cal.time)
                                    endError = null
                                }, endDate.get(Calendar.HOUR_OF_DAY), endDate.get(Calendar.MINUTE), true).show()
                            }, endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH), endDate.get(Calendar.DAY_OF_MONTH)).show()
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date & Time")
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    BouncyButton(
                        onClick = {
                            if (startError != null || endError != null) return@BouncyButton
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
                            var hours = endCal.get(Calendar.HOUR_OF_DAY) - startCal.get(Calendar.HOUR_OF_DAY)
                            var minutes = endCal.get(Calendar.MINUTE) - startCal.get(Calendar.MINUTE)
                            var seconds = endCal.get(Calendar.SECOND) - startCal.get(Calendar.SECOND)

                            if (seconds < 0) {
                                minutes -= 1
                                seconds += 60
                            }
                            if (minutes < 0) {
                                hours -= 1
                                minutes += 60
                            }
                            if (hours < 0) {
                                days -= 1
                                hours += 24
                            }
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
                            
                            val resultParts = mutableListOf<String>()
                            if (years > 0) resultParts.add("$years years")
                            if (months > 0) resultParts.add("$months months")
                            if (days > 0) resultParts.add("$days days")
                            if (hours > 0) resultParts.add("$hours hours")
                            if (minutes > 0) resultParts.add("$minutes minutes")
                            if (seconds > 0) resultParts.add("$seconds seconds")

                            val result = if (resultParts.isEmpty()) "0 seconds" else resultParts.joinToString(", ")
                            diffResult = result

                            assistantViewModel.showMessage("Difference: $result", AssistantState.HAPPY)

                            historyViewModel.addToHistory(
                                "datetime",
                                HistoryItem(
                                    title = "Date Difference",
                                    result = result,
                                    details = "From: ${inputSdf.format(startDate.time)} | To: ${inputSdf.format(endDate.time)}"
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

                com.enosh.fincalc.ui.components.ValidatedTextField(
                    value = timeValue,
                    onValueChange = {
                        timeValue = com.enosh.fincalc.utils.ValidationUtils.formatNumericInput(it)
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
