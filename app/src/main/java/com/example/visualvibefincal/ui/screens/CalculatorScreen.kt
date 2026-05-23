package com.example.visualvibefincal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.viewmodel.HistoryViewModel
import com.example.visualvibefincal.data.model.HistoryItem
import com.example.visualvibefincal.viewmodel.AssistantViewModel
import com.example.visualvibefincal.viewmodel.AssistantState
import java.util.Locale

@Composable
fun CalculatorScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var shouldResetDisplay by remember { mutableStateOf(false) }

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["calc"] ?: emptyList()
    
    var isAdvanced by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    CalculatorScreenScaffold(
        title = "Calculator",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { isAdvanced = !isAdvanced }) {
                    Icon(
                        Icons.Default.Science, 
                        contentDescription = "Advanced", 
                        tint = if (isAdvanced) Color(0xFF00D1B2) else if (isDarkMode) Color.White else Color.Black
                    )
                }
                IconButton(onClick = { 
                    scope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }) {
                    Icon(
                        Icons.Default.History, 
                        contentDescription = "History", 
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Display Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(expression, fontSize = 24.sp, color = if (isDarkMode) Color.Gray else Color.DarkGray)
                    Text(display, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black, maxLines = 1)
                }

                // Buttons
                val standardButtons = listOf(
                    listOf("C", "(", ")", "/"),
                    listOf("7", "8", "9", "*"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "DEL", "=")
                )
                
                val advancedButtons = listOf(
                    listOf("sin", "cos", "tan", "log"),
                    listOf("ln", "sqrt", "^", "pi"),
                    listOf("C", "(", ")", "/"),
                    listOf("7", "8", "9", "*"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("0", ".", "DEL", "=")
                )

                val currentButtons = if (isAdvanced) advancedButtons else standardButtons

                currentButtons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { char ->
                            CalcButton(
                                text = char,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    when (char) {
                                        "C" -> { 
                                            display = "0"
                                            expression = "" 
                                            shouldResetDisplay = false
                                            assistantViewModel.showMessage("Cleared!", AssistantState.IDLE)
                                        }
                                        "DEL" -> { 
                                            if (display == "Error") {
                                                display = "0"
                                            } else if (display.length > 1) {
                                                display = display.dropLast(1)
                                            } else {
                                                display = "0"
                                            }
                                        }
                                        "=" -> {
                                            val result = evaluateExpression(display)
                                            if (result != "Error") {
                                                assistantViewModel.showMessage("Calculated!", AssistantState.HAPPY)
                                                historyViewModel.addToHistory(
                                                    "calc",
                                                    HistoryItem(
                                                        title = display,
                                                        result = result,
                                                        details = if (isAdvanced) "Scientific Calculation" else "Standard Calculation"
                                                    )
                                                )
                                            } else {
                                                assistantViewModel.showMessage("That looks like an error...", AssistantState.ERROR)
                                            }
                                            expression = "$display="
                                            display = result
                                            shouldResetDisplay = true
                                        }
                                        "pi" -> {
                                            if (display == "0" || shouldResetDisplay) display = Math.PI.toString()
                                            else display += Math.PI.toString()
                                            shouldResetDisplay = false
                                        }
                                        "sin", "cos", "tan", "log", "ln", "sqrt" -> {
                                            if (display == "0" || shouldResetDisplay) display = "$char("
                                            else display += "$char("
                                            shouldResetDisplay = false
                                        }
                                        else -> {
                                            val isOperator = char in listOf("+", "-", "*", "/", "^")
                                            val lastChar = if (display.isNotEmpty()) display.last() else ' '
                                            val isLastCharOperator = lastChar in listOf('+', '-', '*', '/', '^')

                                            if (shouldResetDisplay && !isOperator) {
                                                display = char
                                            } else if (display == "0" && !isOperator && char != ".") {
                                                display = char
                                            } else if (display == "Error") {
                                                display = if (isOperator) "0$char" else char
                                            } else if (isOperator && isLastCharOperator) {
                                                // Switch out the operator
                                                display = display.dropLast(1) + char
                                            } else if (char == "." ) {
                                                // Only allow one decimal point
                                                val lastNumber = display.split(Regex("[+\\-*/^()]")).last()
                                                if (!lastNumber.contains(".")) {
                                                    display += char
                                                }
                                            } else {
                                                display += char
                                            }
                                            shouldResetDisplay = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            
            HistorySection(
                screenKey = "calc",
                history = screenHistory,
                isDarkMode = isDarkMode,
                onClearHistory = { historyViewModel.clearHistory("calc") }
            )
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun CalcButton(text: String, isDarkMode: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isOperator = text in listOf("/", "*", "-", "+", "=", "^")
    val isScientific = text in listOf("sin", "cos", "tan", "log", "ln", "sqrt", "pi")
    val isClear = text == "C" || text == "DEL"
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    val semanticsDescription = when (text) {
        "C" -> "Clear all"
        "DEL" -> "Delete last"
        "=" -> "Calculate result"
        "/" -> "Divide"
        "*" -> "Multiply"
        "-" -> "Subtract"
        "+" -> "Add"
        "." -> "Decimal point"
        else -> text
    }

    Box(
        modifier = modifier
            .scale(scale)
            .aspectRatio(if (isScientific) 1.5f else 1f)
            .clip(RoundedCornerShape(if (isScientific) 16.dp else 24.dp))
            .background(
                when {
                    isOperator -> Color(0xFF00D1B2)
                    isScientific -> if (isDarkMode) Color(0xFF2C5364).copy(alpha = 0.5f) else Color(0xFFE0F2F1)
                    isClear -> if (isDarkMode) Color(0xFF332A1B) else Color(0xFFFFCCBC)
                    else -> if (isDarkMode) Color(0xFF1B2C33) else Color(0xFFF0F4F8)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                contentDescription = semanticsDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = if (isScientific) 16.sp else 24.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                isOperator -> Color.White
                isScientific -> Color(0xFF00D1B2)
                isClear -> if (isDarkMode) Color(0xFFFF9800) else Color.Red
                else -> if (isDarkMode) Color.White else Color.Black
            }
        )
    }
}

private fun evaluateExpression(expression: String): String {
    return try {
        val result = object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expression.length) expression[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expression.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = expression.substring(startPos, pos).toDouble()
                } else if (ch >= 'a'.code && ch <= 'z'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = expression.substring(startPos, pos)
                    x = parseFactor()
                    x = when (func) {
                        "sin" -> Math.sin(Math.toRadians(x))
                        "cos" -> Math.cos(Math.toRadians(x))
                        "tan" -> Math.tan(Math.toRadians(x))
                        "sqrt" -> Math.sqrt(x)
                        "log" -> Math.log10(x)
                        "ln" -> Math.log(x)
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                
                if (eat('^'.code)) x = Math.pow(x, parseFactor())

                return x
            }
        }.parse()
        
        if (result.isInfinite() || result.isNaN()) return "Error"
        
        if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format(Locale.getDefault(), "%.4f", result).trimEnd('0').trimEnd('.')
        }
    } catch (e: Exception) {
        "Error"
    }
}
