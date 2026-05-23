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

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.visualvibefincal.viewmodel.HistoryViewModel
import com.example.visualvibefincal.data.model.HistoryItem
import com.example.visualvibefincal.ui.components.ValidatedTextField
import com.example.visualvibefincal.utils.ValidationUtils

import com.example.visualvibefincal.viewmodel.AssistantViewModel
import com.example.visualvibefincal.viewmodel.AssistantState
import com.example.visualvibefincal.viewmodel.AssistantMessageType

@Composable
fun TaxDiscountScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    historyViewModel: HistoryViewModel = viewModel()
) {
    var originalPrice by remember { mutableStateOf("") }
    var discountPercent by remember { mutableStateOf("0") }
    var taxPercent by remember { mutableStateOf("0") }
    var originalPriceError by remember { mutableStateOf<String?>(null) }
    var discountPercentError by remember { mutableStateOf<String?>(null) }
    var taxPercentError by remember { mutableStateOf<String?>(null) }
    var finalPrice by remember { mutableStateOf<Double?>(null) }
    var discountAmount by remember { mutableStateOf<Double?>(null) }
    var taxAmount by remember { mutableStateOf<Double?>(null) }

    val isValid = originalPrice.isNotEmpty() && originalPriceError == null && discountPercentError == null && taxPercentError == null &&
            ValidationUtils.isValidNumeric(originalPrice) && ValidationUtils.isValidNumeric(discountPercent) && ValidationUtils.isValidNumeric(taxPercent)

    val history by historyViewModel.histories.collectAsState()
    val screenHistory = history["tax"] ?: emptyList()

    CalculatorScreenScaffold(
        title = "Tax & Discount",
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
                    value = originalPrice,
                    onValueChange = {
                        originalPrice = ValidationUtils.formatNumericInput(it, allowNegative = false)
                        originalPriceError = if (originalPrice.isEmpty()) "Enter price" 
                                             else if (!ValidationUtils.isValidNumeric(originalPrice)) "Invalid price"
                                             else null
                    },
                    label = "Original Price",
                    error = originalPriceError,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter original price. Currently: $originalPrice"
                    }
                )

                Spacer(Modifier.height(16.dp))

                ValidatedTextField(
                    value = discountPercent,
                    onValueChange = {
                        discountPercent = ValidationUtils.formatNumericInput(it, allowNegative = false)
                        val value = discountPercent.toDoubleOrNull()
                        discountPercentError = when {
                            discountPercent.isEmpty() -> "Enter discount"
                            value != null && value > 100 -> "Discount cannot exceed 100%"
                            value == null && discountPercent.isNotEmpty() -> "Invalid percentage"
                            else -> null
                        }
                    },
                    label = "Discount (%)",
                    error = discountPercentError,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter discount percentage. Currently: $discountPercent percent"
                    }
                )

                Spacer(Modifier.height(16.dp))

                ValidatedTextField(
                    value = taxPercent,
                    onValueChange = {
                        taxPercent = ValidationUtils.formatNumericInput(it, allowNegative = false)
                        taxPercentError = if (taxPercent.isEmpty()) "Enter tax" 
                                          else if (!ValidationUtils.isValidNumeric(taxPercent)) "Invalid percentage"
                                          else null
                    },
                    label = "Tax (%)",
                    error = taxPercentError,
                    modifier = Modifier.semantics {
                        contentDescription = "Enter tax percentage. Currently: $taxPercent percent"
                    }
                )

                Spacer(Modifier.height(32.dp))

                BouncyButton(
                    onClick = {
                        val original = originalPrice.toDoubleOrNull() ?: 0.0
                        val dPercent = discountPercent.toDoubleOrNull() ?: 0.0
                        val tPercent = taxPercent.toDoubleOrNull() ?: 0.0

                        assistantViewModel.showMessage("Calculating savings...", AssistantState.THINKING, AssistantMessageType.THOUGHT)

                        if (original >= 0) {
                            val calculatedDiscount = original * (dPercent / 100)
                            val priceAfterDiscount = original - calculatedDiscount
                            val calculatedTax = priceAfterDiscount * (tPercent / 100)
                            val calculatedFinal = priceAfterDiscount + calculatedTax
                            
                            if (calculatedFinal.isInfinite() || calculatedFinal.isNaN()) {
                                originalPriceError = "Result out of range"
                                assistantViewModel.showMessage("That's a huge number!", AssistantState.ERROR)
                                return@BouncyButton
                            }

                            discountAmount = calculatedDiscount
                            taxAmount = calculatedTax
                            finalPrice = calculatedFinal

                            assistantViewModel.showMessage("Calculated! You saved ${String.format(Locale.getDefault(), "%.2f", calculatedDiscount)}", AssistantState.HAPPY)

                            historyViewModel.addToHistory(
                                "tax",
                                HistoryItem(
                                    title = "Price: ${String.format(Locale.getDefault(), "%.2f", original)}",
                                    result = "Final: ${String.format(Locale.getDefault(), "%.2f", calculatedFinal)}",
                                    details = "Discount: $dPercent% (${String.format(Locale.getDefault(), "%.2f", calculatedDiscount)}) | Tax: $tPercent% (${String.format(Locale.getDefault(), "%.2f", calculatedTax)})"
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).semantics {
                        contentDescription = "Calculate final price with tax and discount"
                    },
                    enabled = isValid
                ) {
                    Text("Calculate", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                if (finalPrice != null) {
                    Spacer(Modifier.height(32.dp))
                    ResultDisplay(label = "Final Price", value = "${String.format(Locale.getDefault(), "%.2f", finalPrice)}", isDarkMode = isDarkMode)
                    Spacer(Modifier.height(16.dp))
                    ResultDisplay(label = "Savings", value = "${String.format(Locale.getDefault(), "%.2f", discountAmount)}", isDarkMode = isDarkMode)
                    Spacer(Modifier.height(16.dp))
                    ResultDisplay(label = "Tax", value = "${String.format(Locale.getDefault(), "%.2f", taxAmount)}", isDarkMode = isDarkMode)
                }
            }

            HistorySection(
                screenKey = "tax",
                history = screenHistory,
                isDarkMode = isDarkMode,
                onClearHistory = { historyViewModel.clearHistory("tax") }
            )
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
