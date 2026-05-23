package com.example.visualvibefincal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.pow

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.viewmodel.HistoryViewModel
import com.example.visualvibefincal.data.model.HistoryItem
import com.example.visualvibefincal.viewmodel.AssistantViewModel
import com.example.visualvibefincal.viewmodel.AssistantState
import com.example.visualvibefincal.viewmodel.AssistantMessageType
import com.example.visualvibefincal.ui.components.ValidatedTextField
import com.example.visualvibefincal.utils.ValidationUtils

@Composable
fun UnitConverterScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var inputValue by remember { mutableStateOf("") }
    var inputValueError by remember { mutableStateOf<String?>(null) }
    var outputValue by remember { mutableStateOf<Double?>(null) }
    var selectedCategory by remember { mutableStateOf("Length") }
    var fromUnit by remember { mutableStateOf("Meters") }
    var toUnit by remember { mutableStateOf("Feet") }
    
    var resultValue by remember { mutableStateOf("") }
    var resultFrom by remember { mutableStateOf("") }
    var resultTo by remember { mutableStateOf("") }
    var resultCategory by remember { mutableStateOf("") }

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["unit"] ?: emptyList()

    var isConverting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val categories = mapOf(
        "Length" to listOf("Meters", "Feet", "Inches", "Kilometers", "Miles", "Centimeters", "Millimeters", "Yards"),
        "Weight" to listOf("Kilograms", "Pounds", "Ounces", "Grams", "Milligrams", "Stone", "Tons"),
        "Temp" to listOf("Celsius", "Fahrenheit", "Kelvin"),
        "Area" to listOf("Sq Meters", "Sq Feet", "Sq Kilometers", "Sq Miles", "Acres", "Hectares"),
        "Volume" to listOf("Liters", "Milliliters", "Gallons", "Quarts", "Pints", "Cups", "Cubic Meters"),
        "Speed" to listOf("m/s", "km/h", "mph", "Knots", "ft/s"),
        "Data" to listOf("Bits", "Bytes", "Kilobytes", "Megabytes", "Gigabytes", "Terabytes", "Petabytes")
    )

    CalculatorScreenScaffold(
        title = "Unit Converter",
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
                    if (isConverting) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            color = Color(0xFF00D1B2),
                            trackColor = Color(0xFF00D1B2).copy(alpha = 0.1f)
                        )
                    }

                    // Category Selector (Tabs or Row)
                    val tabScrollState = rememberScrollState()
                    val tabScrollPercentage by remember {
                        derivedStateOf {
                            if (tabScrollState.maxValue > 0) tabScrollState.value.toFloat() / tabScrollState.maxValue else 0f
                        }
                    }
                    
                    Column {
                        ScrollableTabRow(
                            selectedTabIndex = categories.keys.toList().indexOf(selectedCategory),
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF00D1B2),
                            edgePadding = 0.dp,
                            divider = {}
                        ) {
                            categories.keys.forEach { cat ->
                                Tab(
                                    selected = selectedCategory == cat,
                                    onClick = { 
                                        if (!isConverting) {
                                            selectedCategory = cat
                                            fromUnit = categories[cat]!![0]
                                            toUnit = categories[cat]!![1]
                                            outputValue = null
                                        }
                                    },
                                    text = { Text(cat, color = if (selectedCategory == cat) Color(0xFF00D1B2) else if (isDarkMode) Color.White else Color.Black) },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Category $cat"
                                    }
                                )
                            }
                        }
                        
                        // Custom Scroll Bar (Draggable) for Tabs
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(12.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        scope.launch {
                                            val totalWidth = size.width
                                            if (totalWidth > 0) {
                                                val scrollAmount = (dragAmount.x / totalWidth) * tabScrollState.maxValue
                                                tabScrollState.scrollTo((tabScrollState.value + scrollAmount).toInt())
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                            )
                            
                            // Thumb
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.2f)
                                    .height(4.dp)
                                    .align(Alignment.CenterStart)
                                    .graphicsLayer {
                                        translationX = tabScrollPercentage * (size.width * 0.8f)
                                    }
                                    .background(Color(0xFF00D1B2), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    ValidatedTextField(
                        value = inputValue,
                        onValueChange = {
                            inputValue = ValidationUtils.formatNumericInput(it, allowNegative = (selectedCategory == "Temp"))
                            inputValueError = if (inputValue.isEmpty()) "Enter value" else null
                        },
                        label = "Value",
                        error = inputValueError,
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Enter value to convert. Currently: $inputValue"
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("From", fontSize = 12.sp, color = Color.Gray)
                            UnitDropdown(
                                selectedUnit = fromUnit, 
                                units = categories[selectedCategory]!!, 
                                isDarkMode = isDarkMode,
                                enabled = !isConverting,
                                modifier = Modifier.semantics {
                                    contentDescription = "Select from unit. Currently: $fromUnit"
                                }
                            ) { fromUnit = it }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("To", fontSize = 12.sp, color = Color.Gray)
                            UnitDropdown(
                                selectedUnit = toUnit, 
                                units = categories[selectedCategory]!!, 
                                isDarkMode = isDarkMode,
                                enabled = !isConverting,
                                modifier = Modifier.semantics {
                                    contentDescription = "Select to unit. Currently: $toUnit"
                                }
                            ) { toUnit = it }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    BouncyButton(
                        onClick = {
                            scope.launch {
                                isConverting = true
                                outputValue = null
                                assistantViewModel.showMessage("Converting units...", AssistantState.THINKING, AssistantMessageType.THOUGHT)
                                delay(600) // Simulate processing time
                                val v = inputValue.toDoubleOrNull() ?: 0.0
                                resultValue = inputValue
                                resultFrom = fromUnit
                                resultTo = toUnit
                                resultCategory = selectedCategory
                                val calculatedOutput = convertUnits(v, fromUnit, toUnit, selectedCategory)
                                outputValue = calculatedOutput
                                isConverting = false

                                assistantViewModel.showMessage("Done! ${String.format(Locale.getDefault(), "%.2f", calculatedOutput)} $toUnit", AssistantState.HAPPY)

                                historyViewModel.addToHistory(
                                    "unit",
                                    HistoryItem(
                                        title = "$selectedCategory Conversion",
                                        result = "${String.format(Locale.getDefault(), "%.4f", calculatedOutput)} $toUnit",
                                        details = "${String.format(Locale.getDefault(), "%.2f", v)} $fromUnit"
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).semantics {
                            contentDescription = "Perform unit conversion"
                        },
                        enabled = !isConverting && inputValue.isNotEmpty() && inputValueError == null
                    ) {
                        if (isConverting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Convert", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (outputValue != null && !isConverting) {
                        Spacer(Modifier.height(32.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.05f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val baseRate = convertUnits(1.0, resultFrom, resultTo, resultCategory)
                                Text(
                                    "Conversion Rate",
                                    fontSize = 12.sp,
                                    color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray
                                )
                                Text(
                                    "1 $resultFrom = ${String.format(Locale.getDefault(), "%.4f", baseRate)} $resultTo",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00D1B2)
                                )
                                
                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.1f))
                                Spacer(Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("From", fontSize = 12.sp, color = Color.Gray)
                                        val formattedVal = resultValue.toDoubleOrNull()?.let { 
                                            String.format(Locale.getDefault(), "%.2f", it) 
                                        } ?: resultValue
                                        Text("$formattedVal $resultFrom", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color.Black)
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color(0xFF00D1B2)
                                    )
                                    
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("To", fontSize = 12.sp, color = Color.Gray)
                                        Text("${String.format(Locale.getDefault(), "%.4f", outputValue)} $resultTo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                                    }
                                }
                            }
                        }
                    }
                }

                HistorySection(
                    screenKey = "unit",
                    history = screenHistory,
                    isDarkMode = isDarkMode,
                    onClearHistory = { historyViewModel.clearHistory("unit") }
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
    enabled: Boolean = true,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkMode) Color.White else Color.Black)
        ) {
            Text(selectedUnit)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(if (isDarkMode) Color(0xFF203A43) else Color.White)
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

fun convertUnits(value: Double, from: String, to: String, category: String): Double {
    if (from == to) return value
    return when (category) {
        "Length" -> {
            val inMeters = when (from) {
                "Meters" -> value
                "Feet" -> value * 0.3048
                "Inches" -> value * 0.0254
                "Kilometers" -> value * 1000.0
                "Miles" -> value * 1609.34
                "Centimeters" -> value * 0.01
                "Millimeters" -> value * 0.001
                "Yards" -> value * 0.9144
                else -> value
            }
            when (to) {
                "Meters" -> inMeters
                "Feet" -> inMeters / 0.3048
                "Inches" -> inMeters / 0.0254
                "Kilometers" -> inMeters / 1000.0
                "Miles" -> inMeters / 1609.34
                "Centimeters" -> inMeters / 0.01
                "Millimeters" -> inMeters / 0.001
                "Yards" -> inMeters / 0.9144
                else -> inMeters
            }
        }
        "Weight" -> {
            val inKg = when (from) {
                "Kilograms" -> value
                "Pounds" -> value * 0.453592
                "Ounces" -> value * 0.0283495
                "Grams" -> value / 1000.0
                "Milligrams" -> value / 1000000.0
                "Stone" -> value * 6.35029
                "Tons" -> value * 907.185
                else -> value
            }
            when (to) {
                "Kilograms" -> inKg
                "Pounds" -> inKg / 0.453592
                "Ounces" -> inKg / 0.0283495
                "Grams" -> inKg * 1000.0
                "Milligrams" -> inKg * 1000000.0
                "Stone" -> inKg / 6.35029
                "Tons" -> inKg / 907.185
                else -> inKg
            }
        }
        "Temp" -> {
            val inCelsius = when (from) {
                "Celsius" -> value
                "Fahrenheit" -> (value - 32) * 5/9
                "Kelvin" -> value - 273.15
                else -> value
            }
            when (to) {
                "Celsius" -> inCelsius
                "Fahrenheit" -> (inCelsius * 9/5) + 32
                "Kelvin" -> inCelsius + 273.15
                else -> inCelsius
            }
        }
        "Area" -> {
            val inSqMeters = when (from) {
                "Sq Meters" -> value
                "Sq Feet" -> value * 0.092903
                "Sq Kilometers" -> value * 1_000_000.0
                "Sq Miles" -> value * 2_589_988.11
                "Acres" -> value * 4046.86
                "Hectares" -> value * 10000.0
                else -> value
            }
            when (to) {
                "Sq Meters" -> inSqMeters
                "Sq Feet" -> inSqMeters / 0.092903
                "Sq Kilometers" -> inSqMeters / 1_000_000.0
                "Sq Miles" -> inSqMeters / 2_589_988.11
                "Acres" -> inSqMeters / 4046.86
                "Hectares" -> inSqMeters / 10000.0
                else -> inSqMeters
            }
        }
        "Volume" -> {
            val inLiters = when (from) {
                "Liters" -> value
                "Milliliters" -> value * 0.001
                "Gallons" -> value * 3.78541
                "Quarts" -> value * 0.946353
                "Pints" -> value * 0.473176
                "Cups" -> value * 0.24
                "Cubic Meters" -> value * 1000.0
                else -> value
            }
            when (to) {
                "Liters" -> inLiters
                "Milliliters" -> inLiters / 0.001
                "Gallons" -> inLiters / 3.78541
                "Quarts" -> inLiters / 0.946353
                "Pints" -> inLiters / 0.473176
                "Cups" -> inLiters / 0.24
                "Cubic Meters" -> inLiters / 1000.0
                else -> inLiters
            }
        }
        "Speed" -> {
            val inMs = when (from) {
                "m/s" -> value
                "km/h" -> value / 3.6
                "mph" -> value * 0.44704
                "Knots" -> value * 0.514444
                "ft/s" -> value * 0.3048
                else -> value
            }
            when (to) {
                "m/s" -> inMs
                "km/h" -> inMs * 3.6
                "mph" -> inMs / 0.44704
                "Knots" -> inMs / 0.514444
                "ft/s" -> inMs / 0.3048
                else -> inMs
            }
        }
        "Data" -> {
            val inBits = when (from) {
                "Bits" -> value
                "Bytes" -> value * 8.0
                "Kilobytes" -> value * 8.0 * 1024.0
                "Megabytes" -> value * 8.0 * 1024.0.pow(2.0)
                "Gigabytes" -> value * 8.0 * 1024.0.pow(3.0)
                "Terabytes" -> value * 8.0 * 1024.0.pow(4.0)
                "Petabytes" -> value * 8.0 * 1024.0.pow(5.0)
                else -> value
            }
            when (to) {
                "Bits" -> inBits
                "Bytes" -> inBits / 8.0
                "Kilobytes" -> inBits / (8.0 * 1024.0)
                "Megabytes" -> inBits / (8.0 * 1024.0.pow(2.0))
                "Gigabytes" -> inBits / (8.0 * 1024.0.pow(3.0))
                "Terabytes" -> inBits / (8.0 * 1024.0.pow(4.0))
                "Petabytes" -> inBits / (8.0 * 1024.0.pow(5.0))
                else -> inBits
            }
        }
        else -> value
    }
}
