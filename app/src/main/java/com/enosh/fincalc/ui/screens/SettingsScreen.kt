package com.enosh.fincalc.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.enosh.fincalc.R
import com.enosh.fincalc.ui.components.AssistantRobot
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.viewmodel.*
import com.enosh.fincalc.utils.SecurityUtils
import com.enosh.fincalc.utils.BackupUtils
import com.enosh.fincalc.utils.CurrencyUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    assistantViewModel: AssistantViewModel
) {
    val context = LocalContext.current
    val securePrefs = remember { SecurityUtils.getEncryptedPrefs(context) }
    val sharedPref = remember { context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    val assistantPrefs by assistantViewModel.prefs.collectAsState()
    
    val auth = remember { FirebaseAuth.getInstance() }
    val firebaseUser = auth.currentUser
    val isVerified = firebaseUser?.isEmailVerified ?: false

    val email = securePrefs.getString("email", "No Email") ?: "No Email"
    val initialName = securePrefs.getString("name", email.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User") ?: "User"
    val initialProfilePic = sharedPref.getString("profile_pic", null)

    var userName by remember { mutableStateOf(initialName) }
    var userEmail by remember { mutableStateOf(email) }
    var profilePicUri by remember { mutableStateOf(initialProfilePic?.toUri()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var feedbackText by remember { mutableStateOf("") }
    var isSendingFeedback by remember { mutableStateOf(false) }

    var appLockEnabled by remember { mutableStateOf(SecurityUtils.isAppLockEnabled(context)) }
    var biometricEnabled by remember { mutableStateOf(SecurityUtils.isBiometricEnabled(context)) }
    var showPinDialog by remember { mutableStateOf(false) }
    
    // PIN Setup/Reset State
    var pinStep by remember { mutableStateOf(1) } // 1: Old PIN (if exists), 2: New PIN, 3: Confirm PIN
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    val pinLengthError = stringResource(R.string.pin_length_error)
    val incorrectPinMsg = stringResource(R.string.incorrect_current_pin)
    val pinMismatchMsg = stringResource(R.string.pin_mismatch)

    val hasExistingPin = remember { SecurityUtils.getAppPin(context) != null }

    val biometricManager = remember { BiometricManager.from(context) }
    val canUseBiometric = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val profileUpdatedMsg = stringResource(R.string.profile_updated)
    val sendingFeedbackMsg = stringResource(R.string.sending_feedback)
    val feedbackReadyMsg = stringResource(R.string.feedback_ready)
    val pinSetSuccessMsg = stringResource(R.string.pin_set_success)
    val backupSuccessMsg = stringResource(R.string.backup_successful)
    val backupFailedMsg = stringResource(R.string.backup_failed)
    val restoreSuccessMsg = stringResource(R.string.restore_successful)
    val restoreFailedMsg = stringResource(R.string.restore_failed)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    profilePicUri = uri
                    sharedPref.edit { putString("profile_pic", uri.toString()) }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save profile picture", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    val developerEmail = "enoshjaques@gmail.com"

    fun sendEmail(subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (_: Exception) {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    CalculatorScreenScaffold(
        title = stringResource(R.string.settings_title),
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .bounceClick { 
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (profilePicUri != null) {
                    AsyncImage(
                        model = profilePicUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(userEmail, fontSize = 14.sp, color = Color.Gray)
                if (isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = android.R.drawable.checkbox_on_background),
                        contentDescription = stringResource(R.string.verified_badge),
                        tint = Color(0xFF00D1B2),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (!isVerified && firebaseUser != null) {
                TextButton(onClick = {
                    firebaseUser.sendEmailVerification().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, context.getString(R.string.verification_email_sent), Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text(stringResource(R.string.resend_verification), color = Color(0xFF00D1B2), fontSize = 12.sp)
                }
            }
            
            TextButton(onClick = { showEditDialog = true }) {
                Text(stringResource(R.string.edit_profile), color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            SettingsItem(
                title = stringResource(R.string.dark_mode),
                trailing = {
                    Switch(checked = isDarkMode, onCheckedChange = onDarkModeChange)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Assistant Settings
            CalculatorCard(isDarkMode = isDarkMode) {
                Text(
                    text = stringResource(R.string.assistant),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D1B2)
                )

                Spacer(Modifier.height(16.dp))

                SettingsItem(
                    title = stringResource(R.string.show_assistant),
                    trailing = {
                        Switch(
                            checked = assistantPrefs.isEnabled,
                            onCheckedChange = { assistantViewModel.setEnabled(it, context) }
                        )
                    }
                )

                if (assistantPrefs.isEnabled) {
                    SettingsItem(
                        title = stringResource(R.string.mute_assistant),
                        trailing = {
                            Switch(
                                checked = assistantPrefs.isMuted,
                                onCheckedChange = { assistantViewModel.setMuted(it, context) }
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDarkMode) Color.Black.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(100.dp)) {
                            AssistantRobot(viewModel = assistantViewModel, isDarkMode = isDarkMode, isPreview = true)
                        }
                        Text(stringResource(R.string.style_preview), modifier = Modifier.align(Alignment.TopStart).padding(8.dp), fontSize = 10.sp, color = Color.Gray)
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(stringResource(R.string.appearance_theme), modifier = Modifier.fillMaxWidth(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        AssistantTheme.entries.forEach { theme ->
                            FilterChip(
                                selected = assistantPrefs.theme == theme && !assistantPrefs.isCustomMode,
                                onClick = { assistantViewModel.setTheme(theme, context) },
                                label = { Text(theme.label) },
                                leadingIcon = if (theme != AssistantTheme.CUSTOM) {
                                    {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(theme.accentColor.hex))
                                        )
                                    }
                                } else null
                            )
                        }
                    }

                    SettingsItem(
                        title = stringResource(R.string.custom_mode),
                        trailing = {
                            Switch(
                                checked = assistantPrefs.isCustomMode,
                                onCheckedChange = { assistantViewModel.setCustomMode(it, context) }
                            )
                        }
                    )

                    if (assistantPrefs.isCustomMode) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            CustomColorPicker(stringResource(R.string.head_color), assistantPrefs.customHeadColor) {
                                assistantViewModel.setCustomColors(it, assistantPrefs.customBodyColor, assistantPrefs.customAccentColor, context)
                            }
                            CustomColorPicker(stringResource(R.string.body_color), assistantPrefs.customBodyColor) {
                                assistantViewModel.setCustomColors(assistantPrefs.customHeadColor, it, assistantPrefs.customAccentColor, context)
                            }
                            CustomColorPicker(stringResource(R.string.accent_color_label), assistantPrefs.customAccentColor) {
                                assistantViewModel.setCustomColors(assistantPrefs.customHeadColor, assistantPrefs.customBodyColor, it, context)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(stringResource(R.string.interaction_frequency), modifier = Modifier.fillMaxWidth(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistantFrequency.entries.forEach { freq ->
                            FilterChip(
                                selected = assistantPrefs.frequency == freq,
                                onClick = { assistantViewModel.setFrequency(freq, context) },
                                label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    SettingsItem(
                        title = stringResource(R.string.reset_position),
                        trailing = {
                            IconButton(onClick = { assistantViewModel.resetPosition(context) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset Position", tint = Color(0xFF00D1B2))
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Backup & Restore
            CalculatorCard(isDarkMode = isDarkMode) {
                Text(
                    text = stringResource(R.string.backup_restore),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D1B2)
                )

                Spacer(Modifier.height(16.dp))

                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    uri?.let {
                        coroutineScope.launch {
                            val success = com.enosh.fincalc.utils.BackupUtils.exportData(context, it)
                            if (success) Toast.makeText(context, backupSuccessMsg, Toast.LENGTH_SHORT).show()
                            else Toast.makeText(context, backupFailedMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    uri?.let {
                        pendingRestoreUri = it
                        showRestoreConfirmDialog = true
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BouncyButton(
                        onClick = { exportLauncher.launch("fincalc_backup_${System.currentTimeMillis()}.json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.export_json))
                    }
                    BouncyButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent
                    ) {
                        Text(stringResource(R.string.import_json), color = Color(0xFF00D1B2))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Privacy
            CalculatorCard(isDarkMode = isDarkMode) {
                Text(
                    text = stringResource(R.string.privacy),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D1B2)
                )

                Spacer(Modifier.height(8.dp))
                
                PrivacyNote(stringResource(R.string.data_stays_device))
                PrivacyNote(stringResource(R.string.camera_usage))
                PrivacyNote(stringResource(R.string.security_usage))
                
                Spacer(Modifier.height(8.dp))
                
                TextButton(onClick = { 
                    context.startActivity(Intent(context, com.enosh.fincalc.TermsActivity::class.java))
                }) {
                    Text(stringResource(R.string.privacy_terms), color = Color(0xFF00D1B2), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Default Country / Currency
            CalculatorCard(isDarkMode = isDarkMode) {
                Text(
                    text = stringResource(R.string.default_currency),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D1B2)
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.select_currency),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val selectedCurrency = remember { mutableStateOf(CurrencyUtils.getSelectedCurrency(context)) }

                Column {
                    CurrencyUtils.SUPPORTED_CURRENCIES.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    CurrencyUtils.setSelectedCurrency(context, currency.code)
                                    selectedCurrency.value = currency
                                    Toast.makeText(context, "Currency updated to ${currency.code}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCurrency.value.code == currency.code,
                                onClick = {
                                    CurrencyUtils.setSelectedCurrency(context, currency.code)
                                    selectedCurrency.value = currency
                                    Toast.makeText(context, "Currency updated to ${currency.code}", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${currency.country} / ${currency.code} / ${currency.symbol}",
                                fontSize = 16.sp,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            SettingsItem(title = stringResource(R.string.feedback_review), trailing = {
                IconButton(onClick = { showFeedbackDialog = true }) {
                    Icon(painterResource(R.drawable.ic_calc), contentDescription = "Feedback", modifier = Modifier.size(20.dp), tint = Color(0xFF00D1B2))
                }
            })
            
            SettingsItem(title = stringResource(R.string.help_troubleshoot), trailing = {
                Button(
                    onClick = { 
                        sendEmail("Help & Troubleshoot - Visual Vibe FinCal", "Hi Enosh,\n\nI need help with...")
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.help).uppercase())
                }
            })

            Spacer(Modifier.height(24.dp))

            CalculatorCard(isDarkMode = isDarkMode) {
                Text(stringResource(R.string.security), fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                SettingsItem(
                    title = stringResource(R.string.app_lock_pin),
                    trailing = {
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { 
                                if (it) {
                                    pinStep = if (hasExistingPin) 1 else 2
                                    showPinDialog = true
                                } else {
                                    appLockEnabled = false
                                    SecurityUtils.setAppLockEnabled(context, false)
                                }
                            }
                        )
                    }
                )

                if (appLockEnabled) {
                    SettingsItem(
                        title = stringResource(R.string.biometric_login),
                        trailing = {
                            Switch(
                                checked = biometricEnabled,
                                enabled = canUseBiometric,
                                onCheckedChange = { 
                                    biometricEnabled = it
                                    SecurityUtils.setBiometricEnabled(context, it)
                                }
                            )
                        }
                    )
                    
                    if (!canUseBiometric) {
                        Text(
                            text = stringResource(R.string.biometric_not_available),
                            fontSize = 12.sp,
                            color = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    SettingsItem(
                        title = stringResource(R.string.change_pin),
                        trailing = {
                            TextButton(onClick = { 
                                pinStep = if (hasExistingPin) 1 else 2
                                showPinDialog = true 
                            }) {
                                Text(stringResource(R.string.change_pin).uppercase().split(" ").last(), color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF00D1B2).copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.made_by), fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                    Spacer(Modifier.height(4.dp))
                    Text("Enosh Jaques", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White,
            title = { Text(stringResource(R.string.edit_profile), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ValidatedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = stringResource(R.string.full_name),
                        keyboardType = KeyboardType.Text
                    )
                    Spacer(Modifier.height(12.dp))
                    ValidatedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = "Email",
                        keyboardType = KeyboardType.Email
                    )
                }
            },
            confirmButton = {
                BouncyButton(onClick = { 
                    securePrefs.edit {
                        putString("name", userName)
                        putString("email", userEmail)
                    }
                    showEditDialog = false
                    Toast.makeText(context, profileUpdatedMsg, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    userName = initialName
                    userEmail = email
                    showEditDialog = false 
                }) { Text(stringResource(R.string.cancel), color = Color.Gray) }
            }
        )
    }

    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingFeedback) showFeedbackDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White,
            title = { Text(stringResource(R.string.feedback_review), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.feedback_help), fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    ValidatedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = stringResource(R.string.describe_experience),
                        modifier = Modifier.height(120.dp),
                        keyboardType = KeyboardType.Text,
                        singleLine = false
                    )
                }
            },
            confirmButton = {
                BouncyButton(
                    onClick = {
                        coroutineScope.launch {
                            isSendingFeedback = true
                            assistantViewModel.showMessage(sendingFeedbackMsg, AssistantState.THINKING)
                            delay(1500)
                            sendEmail("Feedback - Visual Vibe FinCal", feedbackText)
                            assistantViewModel.showMessage(feedbackReadyMsg, AssistantState.HAPPY)
                            isSendingFeedback = false
                            showFeedbackDialog = false
                            feedbackText = ""
                        }
                    },
                    enabled = feedbackText.isNotBlank() && !isSendingFeedback
                ) {
                    if (isSendingFeedback) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.sign_up).split(" ").last(), fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }, enabled = !isSendingFeedback) {
                    Text(stringResource(R.string.cancel), color = Color.Gray)
                }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinDialog = false
                currentPinInput = ""
                newPinInput = ""
                confirmPinInput = ""
                pinError = null
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White,
            title = { 
                Text(
                    text = when(pinStep) {
                        1 -> stringResource(R.string.enter_current_pin)
                        2 -> if (hasExistingPin) stringResource(R.string.create_new_pin) else stringResource(R.string.set_pin)
                        3 -> if (hasExistingPin) stringResource(R.string.confirm_new_pin) else stringResource(R.string.confirm_pin)
                        else -> "PIN Setup"
                    }, 
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column {
                    ValidatedTextField(
                        value = when(pinStep) {
                            1 -> currentPinInput
                            2 -> newPinInput
                            3 -> confirmPinInput
                            else -> ""
                        },
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                when(pinStep) {
                                    1 -> currentPinInput = input
                                    2 -> newPinInput = input
                                    3 -> confirmPinInput = input
                                }
                                pinError = if (input.length > 0 && input.length < 4) pinLengthError else null
                            }
                        },
                        label = when(pinStep) {
                            1 -> stringResource(R.string.enter_current_pin)
                            2 -> stringResource(R.string.enter_pin)
                            3 -> if (hasExistingPin) stringResource(R.string.confirm_new_pin) else stringResource(R.string.confirm_pin)
                            else -> "PIN"
                        },
                        keyboardType = KeyboardType.NumberPassword,
                        visualTransformation = PasswordVisualTransformation(),
                        error = pinError
                    )
                }
            },
            confirmButton = {
                BouncyButton(
                    enabled = (when(pinStep) {
                        1 -> currentPinInput.length == 4
                        2 -> newPinInput.length == 4
                        3 -> confirmPinInput.length == 4
                        else -> false
                    }),
                    onClick = {
                        when(pinStep) {
                            1 -> {
                                if (SecurityUtils.verifyPin(context, currentPinInput)) {
                                    pinStep = 2
                                    pinError = null
                                } else {
                                    pinError = incorrectPinMsg
                                    currentPinInput = ""
                                }
                            }
                            2 -> {
                                pinStep = 3
                            }
                            3 -> {
                                if (newPinInput == confirmPinInput) {
                                    SecurityUtils.setAppPin(context, newPinInput)
                                    SecurityUtils.setAppLockEnabled(context, true)
                                    appLockEnabled = true
                                    showPinDialog = false
                                    currentPinInput = ""
                                    newPinInput = ""
                                    confirmPinInput = ""
                                    Toast.makeText(context, pinSetSuccessMsg, Toast.LENGTH_SHORT).show()
                                } else {
                                    pinError = pinMismatchMsg
                                    confirmPinInput = ""
                                }
                            }
                        }
                    }
                ) { 
                    Text(
                        if (pinStep == 3) stringResource(R.string.save) else "Next",
                        fontWeight = FontWeight.Bold
                    ) 
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPinDialog = false
                    currentPinInput = ""
                    newPinInput = ""
                    confirmPinInput = ""
                    pinError = null
                }) { Text(stringResource(R.string.cancel), color = Color.Gray) }
            }
        )
    }

    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("Restore Data") },
            text = { Text("This will overwrite your current expenses, goals, and budgets. This cannot be undone. Proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        pendingRestoreUri?.let { uri ->
                            coroutineScope.launch {
                                val success = com.enosh.fincalc.utils.BackupUtils.importData(context, uri, overwrite = true)
                                if (success) Toast.makeText(context, restoreSuccessMsg, Toast.LENGTH_SHORT).show()
                                else Toast.makeText(context, restoreFailedMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun PrivacyNote(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(Color(0xFF00D1B2), CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun CustomColorPicker(label: String, selectedColor: AssistantColor, onColorSelected: (AssistantColor) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistantColor.entries.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(color.hex))
                        .clickable { onColorSelected(color) }
                        .padding(2.dp)
                ) {
                    if (selectedColor == color) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}
