package com.enosh.fincalc.ui.screens.smartscan

import android.Manifest
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.enosh.fincalc.data.local.AppDatabase
import com.enosh.fincalc.data.local.entity.Expense
import com.enosh.fincalc.data.model.TravelExpense
import com.enosh.fincalc.ui.screens.CalculatorScreenScaffold
import com.enosh.fincalc.ui.screens.ScanItemSkeleton
import com.enosh.fincalc.ui.screens.BouncyButton
import com.enosh.fincalc.ui.screens.CalculatorCard
import com.enosh.fincalc.utils.NotificationHelper
import com.enosh.fincalc.viewmodel.*
import com.enosh.fincalc.utils.CurrencyUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.Text.Line
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

import androidx.compose.ui.res.stringResource
import com.enosh.fincalc.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartScanScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenseDao = remember { AppDatabase.getDatabase(context).expenseDao() }
    val expenses by expenseDao.getAllExpenses().collectAsState(initial = emptyList())

    var showScanner by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var showLowConfidenceDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var pendingScanResult by remember { mutableStateOf<Expense?>(null) }

    var showSaveTargetDialog by remember { mutableStateOf(false) }
    val travelViewModel: SmartTravelViewModel = viewModel()
    val trips by travelViewModel.trips.collectAsState()

    val analyzingBillMsg = stringResource(R.string.msg_analyzing_bill)
    val foundReceiptMsg = stringResource(R.string.msg_found_receipt)
    val couldNotReadMsg = stringResource(R.string.msg_could_not_read)
    val openingCameraMsg = stringResource(R.string.msg_opening_camera)
    val chooseFileMsg = stringResource(R.string.msg_choose_file)
    val processingScanMsg = stringResource(R.string.msg_processing_scan)
    val scanCompleteMsg = stringResource(R.string.msg_scan_complete)
    val scanFailedMsg = stringResource(R.string.msg_scan_failed)
    val couldNotReadReceiptToast = stringResource(R.string.could_not_read_receipt)
    val expenseSavedToast = stringResource(R.string.expense_saved)
    val expenseAddedTitle = stringResource(R.string.expense_added_notif_title)
    val expenseAddedDesc = stringResource(R.string.expense_added_notif_desc)

    val otherCat = stringResource(R.string.cat_other)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isProcessing = true
                assistantViewModel.showMessage(analyzingBillMsg, AssistantState.THINKING, AssistantMessageType.THOUGHT)
                val result = processUri(context, it)
                if (result != null) {
                    assistantViewModel.showMessage(foundReceiptMsg, AssistantState.HAPPY)
                    val newExpense = Expense(
                        amount = result.amount,
                        date = result.date,
                        merchant = result.merchant,
                        category = result.category,
                        source = "upload",
                        notes = "VAT: ${CurrencyUtils.formatCurrency(context, result.vat)}"
                    )

                    if (result.confidenceLow) {
                        pendingScanResult = newExpense
                        showLowConfidenceDialog = true
                    } else {
                        val duplicate = expenseDao.findDuplicate(newExpense.amount, newExpense.date, newExpense.merchant)
                        if (duplicate != null) {
                            pendingScanResult = newExpense
                            showDuplicateDialog = true
                        } else {
                            pendingScanResult = newExpense
                            showSaveTargetDialog = true
                        }
                    }
                } else {
                    assistantViewModel.showMessage(couldNotReadMsg, AssistantState.ERROR)
                    Toast.makeText(context, couldNotReadReceiptToast, Toast.LENGTH_SHORT).show()
                }
                isProcessing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        travelViewModel.fetchTrips()
        delay(1000)
        isLoading = false
    }

    CalculatorScreenScaffold(
        title = stringResource(R.string.smart_scan),
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDarkMode) Color(0xFF0F2027) else Color(0xFFF0F4F8))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Action Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            BouncyButton(
                                onClick = { 
                                    assistantViewModel.showMessage(openingCameraMsg, AssistantState.IDLE)
                                    showScanner = true 
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .semantics {
                                        contentDescription = "Scan a receipt using your camera"
                                    },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.scan_receipt), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            BouncyButton(
                                onClick = { 
                                    assistantViewModel.showMessage(chooseFileMsg, AssistantState.IDLE)
                                    filePickerLauncher.launch("*/*") 
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .semantics {
                                        contentDescription = "Upload a receipt from your phone"
                                    },
                                shape = RoundedCornerShape(12.dp),
                                containerColor = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFF00D1B2))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.upload_bill), fontWeight = FontWeight.SemiBold, color = Color(0xFF00D1B2), fontSize = 12.sp)
                                }
                            }
                        }

                        BouncyButton(
                            onClick = { 
                                editingExpense = Expense(
                                    amount = 0.0,
                                    date = System.currentTimeMillis(),
                                    merchant = "",
                                    category = otherCat,
                                    source = "manual"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            containerColor = Color(0xFF00D1B2).copy(alpha = 0.1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00D1B2))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Manual Expense", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                        }
                    }
                }

                // Charts Section
                if (expenses.isNotEmpty()) {
                    item {
                        ChartsSection(expenses, isDarkMode)
                    }
                }

                // Recent Scans Header
                item {
                    Text(
                        stringResource(R.string.recent_scans),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }

                if (isLoading) {
                    items(count = 3) {
                        ScanItemSkeleton(isDarkMode)
                    }
                } else if (expenses.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_scans_yet), color = Color.Gray)
                        }
                    }
                } else {
                    items(items = expenses) { expense ->
                        ExpenseItem(expense, isDarkMode, 
                            onDelete = { scope.launch { expenseDao.deleteExpense(expense) } },
                            onEdit = { editingExpense = expense }
                        )
                    }
                }
            }
        }
    }

    if (showScanner) {
        CameraScannerDialog(
            onDismiss = { showScanner = false },
                onImageCaptured = { uri ->
                showScanner = false
                scope.launch {
                    isProcessing = true
                    assistantViewModel.showMessage(processingScanMsg, AssistantState.THINKING, AssistantMessageType.THOUGHT)
                    val result = processUri(context, uri)
                    if (result != null) {
                        assistantViewModel.showMessage(scanCompleteMsg, AssistantState.HAPPY)
                        val newExpense = Expense(
                            amount = result.amount,
                            date = result.date,
                            merchant = result.merchant,
                            category = result.category,
                            source = "scan",
                            notes = "VAT: ${CurrencyUtils.formatCurrency(context, result.vat)}"
                        )

                        if (result.confidenceLow) {
                            pendingScanResult = newExpense
                            showLowConfidenceDialog = true
                        } else {
                            val duplicate = expenseDao.findDuplicate(newExpense.amount, newExpense.date, newExpense.merchant)
                            if (duplicate != null) {
                                pendingScanResult = newExpense
                                showDuplicateDialog = true
                            } else {
                                pendingScanResult = newExpense
                                showSaveTargetDialog = true
                            }
                        }
                    } else {
                        assistantViewModel.showMessage(scanFailedMsg, AssistantState.ERROR)
                        Toast.makeText(context, couldNotReadReceiptToast, Toast.LENGTH_SHORT).show()
                    }
                    isProcessing = false
                }
            }
        )
    }

    editingExpense?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            isDarkMode = isDarkMode,
            onDismiss = { editingExpense = null },
            onSave = { updatedExpense ->
                scope.launch {
                    if (updatedExpense.id == 0) {
                        expenseDao.insertExpense(updatedExpense)
                    } else {
                        expenseDao.updateExpense(updatedExpense)
                    }
                    editingExpense = null
                    Toast.makeText(context, expenseSavedToast, Toast.LENGTH_SHORT).show()
                    NotificationHelper.showNotification(
                        context, 
                        expenseAddedTitle, 
                        String.format(expenseAddedDesc, CurrencyUtils.formatCurrency(context, updatedExpense.amount), updatedExpense.merchant)
                    )
                }
            }
        )
    }

    if (isProcessing) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00D1B2))
        }
    }

    if (showLowConfidenceDialog) {
        AlertDialog(
            onDismissRequest = { showLowConfidenceDialog = false },
            title = { Text(stringResource(R.string.low_confidence_title)) },
            text = { Text(stringResource(R.string.low_confidence_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLowConfidenceDialog = false
                        editingExpense = pendingScanResult
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                ) { Text(stringResource(R.string.yes_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showLowConfidenceDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            title = { Text(stringResource(R.string.duplicate_found_title)) },
            text = { Text(stringResource(R.string.duplicate_found_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDuplicateDialog = false
                        showSaveTargetDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                ) { Text(stringResource(R.string.yes_save_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showSaveTargetDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTargetDialog = false },
            title = { Text("Save Expense") },
            text = { Text("Where would you like to save this expense?") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showSaveTargetDialog = false
                            editingExpense = pendingScanResult
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                    ) { Text("Personal Expenses") }
                    
                    if (trips.isNotEmpty()) {
                        Text("Save to Trip:", fontSize = 12.sp, color = Color.Gray)
                        trips.forEach { trip ->
                            OutlinedButton(
                                onClick = {
                                    showSaveTargetDialog = false
                                    val expense = pendingScanResult!!
                                    travelViewModel.addExpense(trip.tripId, TravelExpense(
                                        title = expense.merchant,
                                        amount = expense.amount,
                                        paidByUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                        category = expense.category,
                                        tripId = trip.tripId,
                                        currencyCode = trip.currencyCode,
                                        currencySymbol = trip.currencySymbol,
                                        createdAt = expense.date
                                    ))
                                    Toast.makeText(context, "Saved to ${trip.name}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(trip.name) }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTargetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ChartsSection(expenses: List<Expense>, isDarkMode: Boolean) {
    val context = LocalContext.current
    val total = expenses.sumOf { it.amount }
    val categories = expenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }
    
    CalculatorCard(isDarkMode = isDarkMode) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.total_spending), fontSize = 12.sp, color = Color.Gray)
                Text(CurrencyUtils.formatCurrency(context, total), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
            }
            Icon(Icons.Default.PieChart, contentDescription = null, tint = Color(0xFF00D1B2))
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Simple Bar Chart
        Row(Modifier.fillMaxWidth().height(100.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly) {
            val maxVal = categories.values.maxOrNull() ?: 1.0
            categories.forEach { (cat, amount) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .width(30.dp)
                            .fillMaxHeight((amount / maxVal).toFloat())
                            .background(Color(0xFF00D1B2), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    )
                    Text(cat.take(3), fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ExpenseItem(expense: Expense, isDarkMode: Boolean, onDelete: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .semantics {
                contentDescription = "Expense from ${expense.merchant} on $date for ${CurrencyUtils.formatCurrency(context, expense.amount)}. Tap to edit."
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (expense.source == "scan") Icons.Default.CameraAlt else Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF00D1B2),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.merchant, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(date, fontSize = 12.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyUtils.formatCurrency(context, expense.amount), fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun EditExpenseDialog(expense: Expense, isDarkMode: Boolean, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    var amount by remember { mutableStateOf(if (expense.amount == 0.0) "" else String.format(Locale.getDefault(), "%.2f", expense.amount)) }
    var merchant by remember { mutableStateOf(expense.merchant) }
    var category by remember { mutableStateOf(expense.category) }
    var notes by remember { mutableStateOf(expense.notes) }
    
    val merchantRequiredError = stringResource(R.string.merchant_required)
    val amountRequiredError = stringResource(R.string.amount_required)
    val invalidAmountError = stringResource(R.string.invalid_amount)
    
    var amountError by remember { mutableStateOf<String?>(null) }
    var merchantError by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        stringResource(R.string.cat_food),
        stringResource(R.string.cat_transport),
        stringResource(R.string.cat_shopping),
        stringResource(R.string.cat_bills),
        stringResource(R.string.cat_health),
        stringResource(R.string.cat_travel),
        stringResource(R.string.cat_other)
    )
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense.id == 0) stringResource(R.string.save_expense) else stringResource(R.string.edit_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.enosh.fincalc.ui.components.ValidatedTextField(
                    value = merchant,
                    onValueChange = { 
                        merchant = it
                        merchantError = if (it.isBlank()) merchantRequiredError else null
                    },
                    label = stringResource(R.string.merchant),
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    error = merchantError
                )
                com.enosh.fincalc.ui.components.ValidatedTextField(
                    value = amount,
                    onValueChange = { 
                        amount = com.enosh.fincalc.utils.ValidationUtils.formatNumericInput(it)
                        amountError = if (amount.isBlank()) amountRequiredError
                                      else if (amount.toDoubleOrNull() == null) invalidAmountError
                                      else null
                    },
                    label = stringResource(R.string.amount),
                    keyboardType = KeyboardType.Decimal,
                    error = amountError
                )
                
                // Category Dropdown
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.category)) },
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                        trailingIcon = { 
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D1B2),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                com.enosh.fincalc.ui.components.ValidatedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.notes_optional),
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences,
                    error = null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val finalAmount = amount.toDoubleOrNull() ?: 0.0
                    onSave(expense.copy(amount = finalAmount, merchant = merchant, category = category, notes = notes))
                },
                enabled = merchant.isNotBlank() && amount.isNotBlank() && amountError == null && merchantError == null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun CameraScannerDialog(onDismiss: () -> Unit, onImageCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraPermissionDeniedMsg = stringResource(R.string.camera_permission_denied)
    
    var hasCameraPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            startCamera(context, lifecycleOwner, previewView, imageCapture)
        } else {
            Toast.makeText(context, cameraPermissionDeniedMsg, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera(context, lifecycleOwner, previewView, imageCapture)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { 
                            previewView.apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Crop Guide Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val cornerLength = 40.dp.toPx()
                    val padding = 40.dp.toPx()
                    val rectWidth = size.width - (padding * 2)
                    val rectHeight = size.height * 0.4f
                    val top = (size.height - rectHeight) / 2
                    val left = padding
                    val right = size.width - padding
                    val bottom = top + rectHeight

                    // Semi-transparent background outside the guide
                    val path = Path().apply {
                        addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                        addRect(androidx.compose.ui.geometry.Rect(left, top, right, bottom))
                        fillType = PathFillType.EvenOdd
                    }
                    drawPath(path, Color.Black.copy(alpha = 0.5f))

                    // Corner guides
                    val paint = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                    val guideColor = Color(0xFF00D1B2)
                    
                    // Top Left
                    drawLine(guideColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
                    drawLine(guideColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
                    // Top Right
                    drawLine(guideColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
                    drawLine(guideColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
                    // Bottom Left
                    drawLine(guideColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
                    drawLine(guideColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)
                    // Bottom Right
                    drawLine(guideColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
                    drawLine(guideColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)
                }

                // Overlay UI
                Box(
                    modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    // Capture Button
                    Surface(
                        onClick = {
                            val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                            imageCapture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val savedUri = Uri.fromFile(file)
                                        onImageCaptured(savedUri)
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                    }
                                }
                            )
                        },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .size(72.dp),
                        border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFF00D1B2))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Capture",
                                tint = Color(0xFF00D1B2),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun startCamera(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun isReceipt(text: String): Boolean {
    val keywords = listOf("RECEIPT", "INVOICE", "BILL", "TAX INVOICE", "TOTAL", "CASHIER", "MERCHANT", "VAT", "GST", "THANK YOU")
    val upper = text.uppercase()
    return keywords.any { upper.contains(it) }
}

private const val TAG = "SmartScan"

data class ScanResult(
    val amount: Double,
    val vat: Double,
    val beforeVat: Double,
    val merchant: String,
    val date: Long,
    val category: String,
    val confidenceLow: Boolean = false
)

suspend fun processUri(context: Context, uri: Uri): ScanResult? = withContext(Dispatchers.IO) {
    try {
        var bitmap = if (uri.toString().endsWith(".pdf")) {
            renderPdfToBitmap(context, uri)
        } else {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        }
        
        bitmap = bitmap?.let { preprocessImage(it) }

        bitmap?.let {
            val image = InputImage.fromBitmap(it, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText: Text = recognizer.process(image).await()
            
            val fullText = visionText.text
            val lines = visionText.textBlocks.flatMap { block -> block.lines }
            
            val totalData = detectTotal(lines)
            val merchant = extractMerchant(lines)
            val date = extractDate(fullText)
            val category = detectCategory(fullText)
            
            val vat = detectVAT(lines)

            val isReceiptDetected = isReceipt(fullText)

            return@withContext ScanResult(
                amount = totalData.first,
                vat = vat,
                beforeVat = totalData.first - vat,
                merchant = merchant,
                date = date,
                category = category,
                confidenceLow = totalData.second || !isReceiptDetected || totalData.first == 0.0
            )
        }
    } catch (e: Exception) {
        Log.e(TAG, "Processing error", e)
    }
    null
}

private fun preprocessImage(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = Paint()
    
    // Grayscale + Contrast
    val contrast = 1.4f
    val brightness = -15f
    val cm = ColorMatrix(floatArrayOf(
        contrast, 0f, 0f, 0f, brightness,
        0f, contrast, 0f, 0f, brightness,
        0f, 0f, contrast, 0f, brightness,
        0f, 0f, 0f, 1f, 0f
    ))
    
    val grayMatrix = ColorMatrix()
    grayMatrix.setSaturation(0f)
    cm.preConcat(grayMatrix)
    
    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return bmp
}

private fun detectTotal(lines: List<Line>): Pair<Double, Boolean> {
    val candidates = mutableListOf<Triple<Double, Int, Int>>() 
    
    val totalKeywords = listOf("TOTAL", "GRAND TOTAL", "NET PAYABLE", "AMOUNT DUE", "TOTAL DUE", "PAYABLE", "TOTAL TO PAY", "AMOUNT", "BALANCE DUE", "SUM")
    val ignoreKeywords = listOf("TAX", "VAT", "CASH", "CHANGE", "SAVED", "DISCOUNT", "SUBTOTAL", "ITEMS", "QTY", "METER", "ACCOUNT", "CUSTOMER")

    lines.forEachIndexed { index, line ->
        val text = line.text.uppercase()
        val amount = extractAmount(text)

        if (amount != null) {
            if (text.contains("/") || (text.contains("-") && text.length > 10)) return@forEachIndexed
            val digitsOnly = text.replace(Regex("[^0-9]"), "")
            if (digitsOnly.length >= 10 && !text.contains(".") && !text.contains(",")) return@forEachIndexed

            var priority = 0
            if (totalKeywords.any { text.contains(it) }) {
                priority = 10
                if (ignoreKeywords.any { text.contains(it) }) priority = 5
            } else {
                priority = 1
            }
            candidates.add(Triple(amount, priority, index))
        } else {
            if (totalKeywords.any { text.contains(it) } && !ignoreKeywords.any { text.contains(it) }) {
                // Check following lines for the amount
                for (i in 1..3) {
                    if (index + i < lines.size) {
                        val nextText = lines[index + i].text.uppercase()
                        val nextAmount = extractAmount(nextText)
                        if (nextAmount != null) {
                            candidates.add(Triple(nextAmount, 9, index + i))
                            break
                        }
                    }
                }
            }
        }
    }

    val sorted = candidates.sortedWith(compareByDescending<Triple<Double, Int, Int>> { it.second }.thenByDescending { it.first })

    return if (sorted.isNotEmpty()) {
        val best = sorted.first()
        Pair(best.first, best.second < 6)
    } else {
        Pair(0.0, true)
    }
}

private fun detectVAT(lines: List<Line>): Double {
    val vatKeywords = listOf("VAT", "TAX", "TAXABLE", "GST")
    lines.forEachIndexed { index, line ->
        val text = line.text.uppercase()
        if (vatKeywords.any { text.contains(it) }) {
            val amount = extractAmount(text)
            if (amount != null) return amount
            
            // Check next lines
            for (i in 1..2) {
                if (index + i < lines.size) {
                    val nextAmount = extractAmount(lines[index + i].text)
                    if (nextAmount != null) return nextAmount
                }
            }
        }
    }
    return 0.0
}

private fun extractAmount(text: String): Double? {
    val regex = Regex("""(?i)(?:[$£€₹¥]|USD|EUR|GBP|INR|LKR)?\s*(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})(?:\s*(?:USD|EUR|GBP|INR|LKR))?""")
    val match = regex.find(text)
    if (match != null) {
        var valueStr = match.groupValues[1]
        val lastDot = valueStr.lastIndexOf('.')
        val lastComma = valueStr.lastIndexOf(',')
        
        valueStr = if (lastComma > lastDot) {
            valueStr.replace(".", "").replace(",", ".")
        } else if (lastDot > lastComma) {
            valueStr.replace(",", "")
        } else {
            valueStr.replace(",", ".")
        }
        return valueStr.toDoubleOrNull()
    }
    val fallbackRegex = Regex("""\b\d+[.,]\d{2}\b""")
    val fallbackMatch = fallbackRegex.find(text)
    return fallbackMatch?.value?.replace(",", ".")?.toDoubleOrNull()
}

private fun extractMerchant(lines: List<Line>): String {
    val ignore = listOf("RECEIPT", "INVOICE", "THANK YOU", "TAX", "WELCOME", "ORDER", "CASHIER", "DATE", "TIME", "TEL", "PHONE", "ADDRESS", "VAT", "ACCOUNT", "METER", "CUSTOMER")
    
    // Most likely merchant is at the very top
    for (line in lines.take(5)) {
        val text = line.text.trim()
        if (text.length > 2 && 
            !text.any { it.isDigit() } && 
            !ignore.any { text.uppercase().contains(it) } &&
            text.all { it.isLetterOrDigit() || it.isWhitespace() || it == '&' || it == '.' || it == '\'' || it == '-' }) {
            return text
        }
    }
    
    // Fallback: search for company keywords
    val companyKeywords = listOf("LTD", "INC", "CORP", "CO.", "PLC", "LIMITED", "SERVICES", "SHOP", "STORE", "RESTAURANT")
    for (line in lines.take(10)) {
        val text = line.text.trim()
        if (companyKeywords.any { text.uppercase().contains(it) }) {
            return text
        }
    }

    return lines.firstOrNull()?.text ?: "Unknown Merchant"
}

private fun extractDate(text: String): Long {
    val formats = listOf(
        "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "yyyy-MM-dd",
        "dd/MM/yy", "dd-MM-yy", "MM/dd/yy",
        "MMM dd, yyyy", "dd MMM yyyy"
    )
    val datePatterns = listOf(
        Regex("""(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})"""),
        Regex("""(\d{4}-\d{2}-\d{2})"""),
        Regex("""([a-zA-Z]{3} \d{1,2}, \d{4})""")
    )
    for (pattern in datePatterns) {
        val match = pattern.find(text)
        if (match != null) {
            val dateStr = match.groupValues[1]
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    val date = sdf.parse(dateStr)
                    if (date != null) return date.time
                } catch (e: Exception) {}
            }
        }
    }
    return System.currentTimeMillis()
}

private fun detectCategory(text: String): String {
    val lower = text.lowercase()
    val categories = mapOf(
        "Food & Dining" to listOf("restaurant", "cafe", "food", "takeaway", "pizza", "burger", "coffee", "mcdonald", "starbucks", "eat", "grocery", "bakery", "deli"),
        "Fuel / Transport" to listOf("petrol", "fuel", "diesel", "station", "shell", "bp", "esso", "texaco", "uber", "train", "bus", "transport", "parking", "garage"),
        "Shopping" to listOf("store", "mart", "supermarket", "shop", "tesco", "asda", "sainsbury", "lidl", "aldi", "amazon", "retail", "clothing", "fashion", "electronics"),
        "Bills & Utilities" to listOf("electricity", "water", "gas", "internet", "bill", "invoice", "utility", "phone", "mobile", "rent", "insurance", "subscription", "telecom"),
        "Health" to listOf("pharmacy", "medical", "clinic", "boots", "hospital", "doctor", "dentist", "health", "gym", "fitness"),
        "Travel" to listOf("hotel", "flight", "airline", "booking", "holiday", "resort")
    )
    for ((cat, keywords) in categories) {
        if (keywords.any { lower.contains(it) }) return cat
    }
    return "Other"
}

private fun renderPdfToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val renderer = PdfRenderer(fd)
        if (renderer.pageCount == 0) return null
        val page = renderer.openPage(0)
        val scale = 3f
        val width = (page.width * scale).toInt()
        val height = (page.height * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val matrix = Matrix()
        matrix.setScale(scale, scale)
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
        page.close()
        renderer.close()
        fd.close()
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
