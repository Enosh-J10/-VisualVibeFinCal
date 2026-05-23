package com.example.visualvibefincal.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.visualvibefincal.R
import com.example.visualvibefincal.ui.components.AssistantRobot
import com.example.visualvibefincal.ui.components.ValidatedTextField
import com.example.visualvibefincal.viewmodel.*
import com.example.visualvibefincal.utils.SecurityUtils
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
    val sharedPref = remember { context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE) }
    val coroutineScope = rememberCoroutineScope()
    val assistantPrefs by assistantViewModel.prefs.collectAsState()

    val email = sharedPref.getString("email", "No Email") ?: "No Email"
    val initialName = sharedPref.getString("name", email.split("@").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "User") ?: "User"
    val initialProfilePic = sharedPref.getString("profile_pic", null)

    var userName by remember { mutableStateOf(initialName) }
    var userEmail by remember { mutableStateOf(email) }
    var profilePicUri by remember { mutableStateOf(initialProfilePic?.toUri()) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var isSendingFeedback by remember { mutableStateOf(false) }

    var appLockEnabled by remember { mutableStateOf(SecurityUtils.isAppLockEnabled(context)) }
    var biometricEnabled by remember { mutableStateOf(SecurityUtils.isBiometricEnabled(context)) }
    var showPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                profilePicUri = uri
                sharedPref.edit { putString("profile_pic", uri.toString()) }
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
        title = "Settings",
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
            Text(userEmail, fontSize = 14.sp, color = Color.Gray)
            
            TextButton(onClick = { showEditDialog = true }) {
                Text("Edit Profile", color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))

            SettingsItem(
                title = "Dark Mode",
                trailing = {
                    Switch(checked = isDarkMode, onCheckedChange = onDarkModeChange)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Assistant Settings Section ---
            CalculatorCard(isDarkMode = isDarkMode) {
                Text(
                    text = "🤖 Assistant Settings",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00D1B2)
                )

                Spacer(Modifier.height(16.dp))

                // Visibility Toggle
                SettingsItem(
                    title = "Show Assistant",
                    trailing = {
                        Switch(
                            checked = assistantPrefs.isEnabled,
                            onCheckedChange = { assistantViewModel.setEnabled(it, context) }
                        )
                    }
                )

                if (assistantPrefs.isEnabled) {
                    // Mute Toggle
                    SettingsItem(
                        title = "Mute Assistant",
                        trailing = {
                            Switch(
                                checked = assistantPrefs.isMuted,
                                onCheckedChange = { assistantViewModel.setMuted(it, context) }
                            )
                        }
                    )

                    // Robot Preview Box
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
                        Text("Style Preview", modifier = Modifier.align(Alignment.TopStart).padding(8.dp), fontSize = 10.sp, color = Color.Gray)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Appearance Theme
                    Text("Appearance Theme", modifier = Modifier.fillMaxWidth(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
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

                    // Custom Mode Toggle
                    SettingsItem(
                        title = "Custom Mode",
                        trailing = {
                            Switch(
                                checked = assistantPrefs.isCustomMode,
                                onCheckedChange = { assistantViewModel.setCustomMode(it, context) }
                            )
                        }
                    )

                    if (assistantPrefs.isCustomMode) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            CustomColorPicker("Head Color", assistantPrefs.customHeadColor) {
                                assistantViewModel.setCustomColors(it, assistantPrefs.customBodyColor, assistantPrefs.customAccentColor, context)
                            }
                            CustomColorPicker("Body Color", assistantPrefs.customBodyColor) {
                                assistantViewModel.setCustomColors(assistantPrefs.customHeadColor, it, assistantPrefs.customAccentColor, context)
                            }
                            CustomColorPicker("Accent Color", assistantPrefs.customAccentColor) {
                                assistantViewModel.setCustomColors(assistantPrefs.customHeadColor, assistantPrefs.customBodyColor, it, context)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Message Frequency
                    Text("Interaction Frequency", modifier = Modifier.fillMaxWidth(), fontSize = 14.sp, fontWeight = FontWeight.Medium)
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

                    // Reset Position Button
                    SettingsItem(
                        title = "Reset Position",
                        trailing = {
                            IconButton(onClick = { assistantViewModel.resetPosition(context) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset Position", tint = Color(0xFF00D1B2))
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingsItem(title = "Feedback & Review", trailing = {
                IconButton(onClick = { showFeedbackDialog = true }) {
                    Icon(painterResource(R.drawable.ic_calc), contentDescription = "Feedback", modifier = Modifier.size(20.dp), tint = Color(0xFF00D1B2))
                }
            })
            
            SettingsItem(title = "Help & Troubleshoot", trailing = {
                Button(
                    onClick = { 
                        sendEmail("Help & Troubleshoot - Visual Vibe FinCal", "Hi Enosh,\n\nI need help with...")
                    }, 
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("HELP")
                }
            })

            Spacer(Modifier.height(24.dp))

            CalculatorCard(isDarkMode = isDarkMode) {
                Text("Security", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                SettingsItem(
                    title = "App Lock (PIN)",
                    trailing = {
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { 
                                if (it) {
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
                        title = "Biometric Login",
                        trailing = {
                            Switch(
                                checked = biometricEnabled,
                                onCheckedChange = { 
                                    biometricEnabled = it
                                    SecurityUtils.setBiometricEnabled(context, it)
                                }
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Change PIN",
                        trailing = {
                            TextButton(onClick = { showPinDialog = true }) {
                                Text("CHANGE", color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
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
                    Text("Developer Credits", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                    Spacer(Modifier.height(4.dp))
                    Text("Designed & Developed by", fontSize = 12.sp)
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
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ValidatedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        label = "Name",
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
                    sharedPref.edit {
                        putString("name", userName)
                        putString("email", userEmail)
                    }
                    showEditDialog = false
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                }) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    userName = initialName
                    userEmail = email
                    showEditDialog = false 
                }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingFeedback) showFeedbackDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White,
            title = { Text("Feedback", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your feedback helps me improve!", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    ValidatedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = "Describe your experience...",
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
                            assistantViewModel.showMessage("Sending feedback...", AssistantState.THINKING)
                            delay(1500)
                            sendEmail("Feedback - Visual Vibe FinCal", feedbackText)
                            assistantViewModel.showMessage("Feedback ready to send!", AssistantState.HAPPY)
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
                        Text("Send", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }, enabled = !isSendingFeedback) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White,
            title = { Text("Set 4-Digit PIN", fontWeight = FontWeight.Bold) },
            text = {
                ValidatedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = "Enter PIN",
                    keyboardType = KeyboardType.NumberPassword,
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                BouncyButton(
                    enabled = newPin.length == 4,
                    onClick = {
                        SecurityUtils.setAppPin(context, newPin)
                        SecurityUtils.setAppLockEnabled(context, true)
                        appLockEnabled = true
                        showPinDialog = false
                        newPin = ""
                        Toast.makeText(context, "PIN set successfully", Toast.LENGTH_SHORT).show()
                    }
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
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
