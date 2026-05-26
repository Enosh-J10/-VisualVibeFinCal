package com.enosh.fincalc.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.util.Locale
import com.enosh.fincalc.data.model.CurrencyData
import com.enosh.fincalc.viewmodel.CurrencyUiState
import com.enosh.fincalc.viewmodel.CurrencyViewModel
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.ValidationUtils

import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.viewmodel.AssistantMessageType

import androidx.compose.ui.res.stringResource
import com.enosh.fincalc.R

@Composable
fun CurrencyConverterScreen(
    navController: NavController,
    isDarkMode: Boolean,
    viewModel: CurrencyViewModel = viewModel(),
    assistantViewModel: AssistantViewModel
) {
    var amount by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    val fromCurrency by viewModel.fromCurrency.collectAsState()
    val toCurrency by viewModel.toCurrency.collectAsState()
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }
    
    var resultAmount by remember { mutableStateOf("") }
    var resultFrom by remember { mutableStateOf("") }
    var resultTo by remember { mutableStateOf("") }
    var resultRate by remember { mutableStateOf<Double?>(null) }
    
    var isConversionRequested by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    val convertedAmount by viewModel.convertedAmount.collectAsState()
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    val history by viewModel.history.collectAsState()

    val fetchingRatesMsg = stringResource(R.string.msg_fetching_rates)
    val conversionResultMsg = stringResource(R.string.msg_conversion_result)
    val checkConnectionMsg = stringResource(R.string.msg_check_connection)
    val currencyNotSupportedMsg = stringResource(R.string.currency_not_supported)
    val networkErrorMsg = stringResource(R.string.network_error)

    LaunchedEffect(uiState) {
        if (uiState is CurrencyUiState.Success) {
            val response = (uiState as CurrencyUiState.Success).data
            val allRates = response.allRates
            
            if (isConversionRequested) {
                val targetCode = toCurrency.uppercase().trim()
                val rate = allRates[targetCode]
                
                if (rate != null) {
                    resultAmount = amount
                    resultFrom = fromCurrency
                    resultTo = targetCode
                    resultRate = rate
                    val amountDbl = amount.toDoubleOrNull() ?: 0.0
                    viewModel.convert(amountDbl, rate)
                    
                    assistantViewModel.showMessage(conversionResultMsg, AssistantState.HAPPY)
                    
                    // Add to history
                    viewModel.addToHistory(
                        com.enosh.fincalc.data.model.CurrencyHistoryItem(
                            fromCode = fromCurrency,
                            toCode = targetCode,
                            amount = amountDbl,
                            result = amountDbl * rate,
                            rate = rate
                        )
                    )
                } else {
                    Log.e("CurrencyDebug", "ERROR: $targetCode not found in API keys!")
                    val errorMsg = String.format(currencyNotSupportedMsg, targetCode)
                    assistantViewModel.showMessage(errorMsg, AssistantState.ERROR)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
                isConversionRequested = false
            }
        } else if (uiState is CurrencyUiState.Error) {
            Log.e("CurrencyDebug", "API ERROR: ${(uiState as CurrencyUiState.Error).message}")
            if (isConversionRequested) {
                assistantViewModel.showMessage(checkConnectionMsg, AssistantState.ERROR)
                Toast.makeText(context, networkErrorMsg, Toast.LENGTH_LONG).show()
                isConversionRequested = false
            }
        } else if (uiState is CurrencyUiState.Loading && isConversionRequested) {
            assistantViewModel.showMessage(fetchingRatesMsg, AssistantState.THINKING, AssistantMessageType.THOUGHT)
        }
    }

    val currencies = availableCurrencies

    LaunchedEffect(fromCurrency) {
        viewModel.fetchRates(fromCurrency)
    }

    CalculatorScreenScaffold(
        title = stringResource(R.string.currency_converter),
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CalculatorCard(isDarkMode = isDarkMode) {
                val enterAmountError = stringResource(R.string.enter_amount)
                val validAmountError = stringResource(R.string.valid_amount)

                ValidatedTextField(
                    value = amount,
                    onValueChange = {
                        amount = ValidationUtils.formatNumericInput(it)
                        amountError = if (amount.isEmpty()) enterAmountError else null
                    },
                    label = stringResource(R.string.amount),
                    error = amountError
                )

                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    CurrencySelector(
                        label = stringResource(R.string.from),
                        selected = fromCurrency,
                        expanded = expandedFrom,
                        onExpandedChange = { expandedFrom = it },
                        onSelect = { 
                            viewModel.setFromCurrency(it.uppercase())
                        },
                        currencies = currencies,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.enosh.fincalc.R.drawable.ic_currency),
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 8.dp).size(24.dp),
                        tint = Color(0xFF00D1B2)
                    )

                    CurrencySelector(
                        label = stringResource(R.string.to),
                        selected = toCurrency,
                        expanded = expandedTo,
                        onExpandedChange = { expandedTo = it },
                        onSelect = { viewModel.setToCurrency(it.uppercase()) },
                        currencies = currencies,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(32.dp))

                BouncyButton(
                    onClick = {
                        val amountDbl = amount.toDoubleOrNull()
                        
                        if (amountDbl != null && amountDbl > 0) {
                            isConversionRequested = true
                            Toast.makeText(context, fetchingRatesMsg, Toast.LENGTH_SHORT).show()
                            viewModel.fetchRates(fromCurrency.uppercase().trim())
                        } else {
                            amountError = validAmountError
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            contentDescription = "Convert $amount from $fromCurrency to $toCurrency"
                        },
                    enabled = amount.isNotBlank() && amountError == null && uiState !is CurrencyUiState.Loading
                ) {
                    if (uiState is CurrencyUiState.Loading && isConversionRequested) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.convert), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (convertedAmount != null && resultRate != null) {
                    Spacer(Modifier.height(32.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Conversion result: ${String.format(Locale.getDefault(), "%.2f", convertedAmount)} $resultTo. Rate is 1 $resultFrom = ${String.format(Locale.getDefault(), "%.4f", resultRate)} $resultTo"
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.exchange_rate),
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray
                            )
                            Text(
                                "1 $resultFrom = ${String.format(Locale.getDefault(), "%.4f", resultRate)} $resultTo",
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
                                    val fromInfo = CurrencyData.getCurrency(resultFrom)
                                    Text(stringResource(R.string.amount), fontSize = 12.sp, color = Color.Gray)
                                    val formattedAmount = resultAmount.toDoubleOrNull()?.let { 
                                        String.format(Locale.getDefault(), "%.2f", it) 
                                    } ?: resultAmount
                                    Text("${fromInfo.flag} $formattedAmount ${fromInfo.code}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (isDarkMode) Color.White else Color.Black)
                                }
                                
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF00D1B2)
                                )
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val toInfo = CurrencyData.getCurrency(resultTo)
                                    Text(stringResource(R.string.converted), fontSize = 12.sp, color = Color.Gray)
                                    Text("${toInfo.flag} ${toInfo.symbol}${String.format(Locale.getDefault(), "%.2f", convertedAmount)} ${toInfo.code}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                                }
                            }
                        }
                    }
                }
            }

            if (uiState is CurrencyUiState.Loading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(color = Color(0xFF00D1B2))
            } else if (uiState is CurrencyUiState.Error) {
                Spacer(Modifier.height(16.dp))
                Text(
                    (uiState as CurrencyUiState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            if (history.isNotEmpty()) {
                Spacer(Modifier.height(32.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.history),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text(stringResource(R.string.clear), color = Color(0xFF00D1B2))
                        }
                    }
                    
                    history.forEach { item ->
                        HistoryItem(item, isDarkMode)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(item: com.enosh.fincalc.data.model.CurrencyHistoryItem, isDarkMode: Boolean) {
    val fromInfo = CurrencyData.getCurrency(item.fromCode)
    val toInfo = CurrencyData.getCurrency(item.toCode)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "${fromInfo.flag} ${String.format(Locale.getDefault(), "%.2f", item.amount)} ${item.fromCode}",
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF00D1B2)
                )
                Text(
                    "${toInfo.flag} ${toInfo.symbol}${String.format(Locale.getDefault(), "%.2f", item.result)} ${item.toCode}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D1B2)
                )
            }
            
            Text(
                "Rate: ${String.format(Locale.getDefault(), "%.4f", item.rate)}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CurrencySelector(
    label: String,
    selected: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    currencies: List<String>,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = currencies.filter { code ->
        val info = CurrencyData.getCurrency(code)
        code.contains(searchQuery, ignoreCase = true) || 
        info.name.contains(searchQuery, ignoreCase = true)
    }

    val selectedInfo = CurrencyData.getCurrency(selected)

    Column(modifier = modifier) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    searchQuery = ""
                    onExpandedChange(true) 
                }
                .padding(vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${selectedInfo.flag} ${selectedInfo.code}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isDarkMode) Color.White else Color.Black
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .width(280.dp)
                    .heightIn(max = 400.dp)
                    .background(if (isDarkMode) Color(0xFF203A43) else Color.White)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    placeholder = { Text("Search name or code...", fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = if (isDarkMode) Color.White else Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00D1B2),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
                
                HorizontalDivider(color = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.1f))

                filteredCurrencies.forEach { code ->
                    val info = CurrencyData.getCurrency(code)
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(info.flag, fontSize = 20.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        info.name, 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDarkMode) Color.White else Color.Black
                                    )
                                    Text(
                                        info.code, 
                                        fontSize = 12.sp, 
                                        color = Color.Gray
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelect(code)
                            onExpandedChange(false)
                        }
                    )
                }
                
                if (filteredCurrencies.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No results", fontSize = 12.sp, color = Color.Gray) },
                        onClick = { },
                        enabled = false
                    )
                }
            }
        }
    }
}
