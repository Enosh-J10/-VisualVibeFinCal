package com.example.visualvibefincal.ui.screens.smartscan

import android.Manifest
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.media.ThumbnailUtils
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.visualvibefincal.data.local.AppDatabase
import com.example.visualvibefincal.data.local.entity.Expense
import com.example.visualvibefincal.ui.screens.CalculatorScreenScaffold
import com.example.visualvibefincal.ui.screens.ScanItemSkeleton
import com.example.visualvibefincal.ui.screens.BouncyButton
import com.example.visualvibefincal.ui.screens.CalculatorCard
import com.example.visualvibefincal.utils.NotificationHelper
import com.example.visualvibefincal.viewmodel.AssistantViewModel
import com.example.visualvibefincal.viewmodel.AssistantState
import com.example.visualvibefincal.viewmodel.AssistantMessageType
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
import com.example.visualvibefincal.R

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

    LaunchedEffect(Unit) {
        delay(1000)
        isLoading = false
    }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                        editingExpense = Expense(
                                            amount = result.amount,
                                            date = result.date,
                                            merchant = result.merchant,
                                            category = result.category,
                                            source = "upload",
                                            notes = ""
                                        )
                                    } else {
                                        assistantViewModel.showMessage(couldNotReadMsg, AssistantState.ERROR)
                                        Toast.makeText(context, couldNotReadReceiptToast, Toast.LENGTH_SHORT).show()
                                    }
                                    isProcessing = false
                                }
                            }
                        }

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
                            Text(stringResource(R.string.scan_receipt), fontWeight = FontWeight.SemiBold)
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
                                Text(stringResource(R.string.upload_bill), fontWeight = FontWeight.SemiBold, color = Color(0xFF00D1B2))
                            }
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
                    items(3) {
                        ScanItemSkeleton(isDarkMode)
                    }
                } else if (expenses.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_scans_yet), color = Color.Gray)
                        }
                    }
                } else {
                    items(expenses) { expense ->
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
                        editingExpense = Expense(
                            amount = result.amount,
                            date = result.date,
                            merchant = result.merchant,
                            category = result.category,
                            source = "scan",
                            notes = ""
                        )
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
                        String.format(expenseAddedDesc, String.format("%.2f", updatedExpense.amount), updatedExpense.merchant)
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
}

@Composable
fun ScanButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, outline: Boolean = false) {
    val contentColor = if (outline) Color(0xFF00D1B2) else Color.White
    val containerColor = if (outline) Color.Transparent else Color(0xFF00D1B2)
    val border = if (outline) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D1B2)) else null

    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = border,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ChartsSection(expenses: List<Expense>, isDarkMode: Boolean) {
    val total = expenses.sumOf { it.amount }
    val categories = expenses.groupBy { it.category }.mapValues { it.value.sumOf { exp -> exp.amount } }
    
    CalculatorCard(isDarkMode = isDarkMode) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(R.string.total_spending), fontSize = 12.sp, color = Color.Gray)
                Text("$${String.format("%.2f", total)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
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
    val date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .semantics {
                contentDescription = "Expense from ${expense.merchant} on $date for $${String.format("%.2f", expense.amount)}. Tap to edit."
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
                Text("$${String.format("%.2f", expense.amount)}", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
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
        stringResource(R.string.cat_other)
    )
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense.id == 0) stringResource(R.string.save_expense) else stringResource(R.string.edit_expense)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.example.visualvibefincal.ui.components.ValidatedTextField(
                    value = merchant,
                    onValueChange = { 
                        merchant = it
                        merchantError = if (it.isBlank()) merchantRequiredError else null
                    },
                    label = stringResource(R.string.merchant),
                    error = merchantError
                )
                com.example.visualvibefincal.ui.components.ValidatedTextField(
                    value = amount,
                    onValueChange = { 
                        amount = com.example.visualvibefincal.utils.ValidationUtils.formatNumericInput(it)
                        amountError = if (amount.isBlank()) amountRequiredError
                                      else if (amount.toDoubleOrNull() == null) invalidAmountError
                                      else null
                    },
                    label = stringResource(R.string.amount),
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

                com.example.visualvibefincal.ui.components.ValidatedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.notes_optional),
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Overlay UI
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Capture Button
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
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
                            modifier = Modifier.size(72.dp),
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

private const val TAG = "SmartScan"

data class ScanResult(
    val amount: Double,
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
            
            // Log.d(TAG, "Raw OCR Text: $fullText") // Removed for security

            val amountData = detectTotal(lines)
            val merchant = extractMerchant(lines)
            val date = extractDate(fullText)
            val category = detectCategory(fullText)

            return@withContext ScanResult(
                amount = amountData.first,
                merchant = merchant,
                date = date,
                category = category,
                confidenceLow = amountData.second
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
    val canvas = Canvas(bmp)
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
    val candidates = mutableListOf<Triple<Double, Int, Int>>() // amount, priority, index
    
    val totalKeywords = listOf("TOTAL", "GRAND TOTAL", "AMOUNT", "BALANCE DUE", "NET", "SUM", "TOTAL DUE", "PAYABLE")
    val ignoreKeywords = listOf("TAX", "VAT", "CASH", "CHANGE", "SAVED", "DISCOUNT", "SUBTOTAL", "ITEMS", "QTY")

    lines.forEachIndexed { index, line ->
        val text = line.text.uppercase()
        val amount = extractAmount(text)

        if (amount != null) {
            // Skip obvious non-totals like dates or phone numbers
            if (text.contains("/") || (text.contains("-") && text.length > 10)) return@forEachIndexed
            
            // Filter out phone numbers or long codes
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
            // If no amount on this line, check if it's a keyword and amount is on next lines
            if (totalKeywords.any { text.contains(it) } && !ignoreKeywords.any { text.contains(it) }) {
                for (i in 1..2) {
                    if (index + i < lines.size) {
                        val nextText = lines[index + i].text.uppercase()
                        val nextAmount = extractAmount(nextText)
                        if (nextAmount != null) {
                            candidates.add(Triple(nextAmount, 8, index + i))
                            break
                        }
                    }
                }
            }
        }
    }

    // Rank candidates: Priority first, then largest value
    val sorted = candidates.sortedWith(compareByDescending<Triple<Double, Int, Int>> { it.second }.thenByDescending { it.first })

    Log.d(TAG, "Detected amount candidates: $sorted")

    return if (sorted.isNotEmpty()) {
        val best = sorted.first()
        Pair(best.first, best.second < 5)
    } else {
        Pair(0.0, true)
    }
}

private fun extractAmount(text: String): Double? {
    // Regex for different currencies and formats
    val regex = Regex("""(?i)(?:[$£€¥]|USD|EUR|GBP)?\s*(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})(?:\s*(?:USD|EUR|GBP))?""")
    val match = regex.find(text)
    if (match != null) {
        var valueStr = match.groupValues[1]
        
        // Normalize: detect separator style
        val lastDot = valueStr.lastIndexOf('.')
        val lastComma = valueStr.lastIndexOf(',')
        
        valueStr = if (lastComma > lastDot) {
            // EU Style: 1.234,56 -> 1234.56
            valueStr.replace(".", "").replace(",", ".")
        } else if (lastDot > lastComma) {
            // US Style: 1,234.56 -> 1234.56
            valueStr.replace(",", "")
        } else {
            // No separators or simple decimal
            valueStr.replace(",", ".")
        }
        
        return valueStr.toDoubleOrNull()
    }
    
    // Fallback: look for any number followed by decimal (priority to 2-decimal digits)
    val fallbackRegex = Regex("""\b\d+[.,]\d{2}\b""")
    val fallbackMatch = fallbackRegex.find(text)
    return fallbackMatch?.value?.replace(",", ".")?.toDoubleOrNull()
}

private fun extractMerchant(lines: List<Line>): String {
    val ignore = listOf("RECEIPT", "INVOICE", "THANK YOU", "TAX", "WELCOME", "ORDER", "CASHIER", "DATE", "TIME", "TEL", "PHONE", "ADDRESS")
    
    // Look at first 8 lines for something that looks like a name
    for (line in lines.take(8)) {
        val text = line.text.trim()
        if (text.length > 2 && 
            !text.any { it.isDigit() } && 
            !ignore.any { text.uppercase().contains(it) } &&
            text.all { it.isLetterOrDigit() || it.isWhitespace() || it == '&' || it == '.' || it == '\'' || it == '-' }) {
            return text
        }
    }
    
    // Fallback: search for first non-ignored line even if it has digits
    for (line in lines.take(5)) {
        val text = line.text.trim()
        if (text.length > 2 && !ignore.any { text.uppercase().contains(it) }) {
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
        "Bills" to listOf("electricity", "water", "gas", "internet", "bill", "invoice", "utility", "phone", "mobile", "rent", "insurance", "subscription"),
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
        
        // Scale up for better OCR (3x scaling)
        val scale = 3f
        val width = (page.width * scale).toInt()
        val height = (page.height * scale).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
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
