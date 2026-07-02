package com.enosh.fincalc.ui.screens

import android.content.Context
import android.util.Log
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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.enosh.fincalc.utils.UserUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    assistantViewModel: AssistantViewModel,
    settingsViewModel: SettingsViewModel = viewModel(),
    initialResetPin: Boolean = false
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences(UserUtils.PREFS_NAME, Context.MODE_PRIVATE) }
    val isGuest = remember { sharedPref.getBoolean("is_guest", false) }
    val coroutineScope = rememberCoroutineScope()
    val assistantPrefs by assistantViewModel.prefs.collectAsState()
    
    val auth = remember { FirebaseAuth.getInstance() }
    val firebaseUser = auth.currentUser
    val isVerified = firebaseUser?.isEmailVerified ?: false

    val userUid = firebaseUser?.uid ?: ""
    val email = firebaseUser?.email ?: "No Email"
    val initialName = firebaseUser?.displayName ?: "User"
    
    var userData by remember { mutableStateOf<com.enosh.fincalc.data.model.User?>(null) }
    
    LaunchedEffect(userUid) {
        if (userUid.isNotBlank()) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = db.collection("users").document(userUid).get().await()
                userData = doc.toObject(com.enosh.fincalc.data.model.User::class.java)
            } catch (e: Exception) {
                Log.e("Settings", "Failed to fetch user data", e)
            }
        }
    }

    var userName by remember { mutableStateOf(initialName) }
    var profilePicUrl by remember { mutableStateOf(userData?.profilePictureUrl) }
    
    LaunchedEffect(userData) {
        userData?.let {
            userName = it.name.ifBlank { initialName }
            profilePicUrl = it.profilePictureUrl
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showRoastWarningDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showGuestLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showProfilePreview by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    isUploading = true
                    try {
                        UserUtils.ensureFinCalcUserProfile(context, profilePicUri = uri)
                        // Refresh URL
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val doc = db.collection("users").document(userUid).get().await()
                        val newUrl = doc.getString("profilePictureUrl")
                        profilePicUrl = newUrl
                        Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isUploading = false
                    }
                }
            }
        }
    )

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
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))
                        .clickable { if (!isGuest) showProfilePreview = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = Color(0xFF00D1B2))
                    } else if (profilePicUrl != null) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(profilePicUrl)
                                .crossfade(true)
                                .memoryCacheKey(profilePicUrl)
                                .diskCacheKey(profilePicUrl)
                                .build(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = if (isGuest) android.R.drawable.ic_menu_help else R.drawable.ic_tip)
                        )
                    } else {
                        Icon(
                            if (isGuest) Icons.Default.Face else Icons.Default.Person, 
                            contentDescription = null, 
                            modifier = Modifier.size(60.dp),
                            tint = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray
                        )
                    }
                }
                
                if (!isGuest) {
                    Surface(
                        onClick = { 
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(32.dp),
                        shape = CircleShape,
                        color = Color(0xFF00D1B2),
                        tonalElevation = 4.dp
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture",
                            modifier = Modifier.padding(8.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            if (showProfilePreview) {
                AlertDialog(
                    onDismissRequest = { showProfilePreview = false },
                    title = { Text("Profile Preview", color = if (isDarkMode) Color.White else Color.Black) },
                    text = {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (profilePicUrl != null) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(profilePicUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.size(250.dp).clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, null, Modifier.size(200.dp), Color.Gray)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showProfilePreview = false }) { Text("Close", color = Color(0xFF00D1B2)) }
                    },
                    containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(if (isGuest) "Guest User" else userName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
            Text(if (isGuest) "Register to unlock all features" else email, fontSize = 14.sp, color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray)

            if (!isGuest) {
                val finCalcId = remember(userUid) { UserUtils.generateStableFinCalcId(userUid) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "FinCalc ID: $finCalcId", fontSize = 14.sp, color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray)
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("FinCalc ID", finCalcId))
                        Toast.makeText(context, "ID Copied", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = Color(0xFF00D1B2))
                    }
                }
                
                TextButton(onClick = { showEditDialog = true }) {
                    Text(stringResource(R.string.edit_profile), color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
                }

                BouncyButton(
                    onClick = { navController.navigate("friends") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = Color(0xFF00D1B2).copy(alpha = 0.1f)
                ) {
                    Icon(Icons.Default.GroupAdd, null, tint = Color(0xFF00D1B2))
                    Spacer(Modifier.width(8.dp))
                    Text("Friends / Add Friends", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                }
            } else {
                Spacer(Modifier.height(16.dp))
                BouncyButton(
                    onClick = { onLogout() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sign Up / Login to unlock more", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            SettingsItem(
                title = stringResource(R.string.dark_mode),
                isDarkMode = isDarkMode,
                trailing = {
                    Switch(checked = isDarkMode, onCheckedChange = onDarkModeChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D1B2)))
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f))

            // Assistant Settings
            CalculatorCard(isDarkMode = isDarkMode) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = stringResource(R.string.assistant), modifier = Modifier.fillMaxWidth(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))

                    SettingsItem(
                        title = stringResource(R.string.show_assistant),
                        isDarkMode = isDarkMode,
                        trailing = {
                            Switch(checked = assistantPrefs.isEnabled, onCheckedChange = { assistantViewModel.setEnabled(it, context) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D1B2)))
                        }
                    )

                    if (assistantPrefs.isEnabled) {
                        SettingsItem(
                            title = "Mute Assistant",
                            isDarkMode = isDarkMode,
                            trailing = {
                                Switch(checked = assistantPrefs.isMuted, onCheckedChange = { assistantViewModel.setMuted(it, context) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D1B2)))
                            }
                        )

                        SettingsItem(
                            title = "Roast Mode",
                            subtitle = "Playful savage AI responses",
                            isDarkMode = isDarkMode,
                            trailing = {
                                Switch(
                                    checked = assistantPrefs.isRoastMode,
                                    onCheckedChange = { 
                                        if (it) showRoastWarningDialog = true
                                        else assistantViewModel.setRoastMode(false, context)
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D1B2))
                                )
                            }
                        )

                        Text("Appearance Theme", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf(
                                AssistantTheme.DEFAULT,
                                AssistantTheme.OCEAN,
                                AssistantTheme.BERRY,
                                AssistantTheme.NEON_NIGHT,
                                AssistantTheme.SUNSET,
                                AssistantTheme.READABLE_DARK,
                                AssistantTheme.READABLE_LIGHT,
                                AssistantTheme.CUSTOM
                            )
                            themes.forEach { theme ->
                                FilterChip(
                                    selected = assistantPrefs.theme == theme && !assistantPrefs.isCustomMode,
                                    onClick = { 
                                        assistantViewModel.setCustomMode(false, context)
                                        assistantViewModel.setTheme(theme, context) 
                                    },
                                    label = { Text(theme.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00D1B2).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(0xFF00D1B2)
                                    )
                                )
                            }
                        }

                        if (assistantPrefs.theme == AssistantTheme.CUSTOM || assistantPrefs.isCustomMode) {
                            Text("Custom Theme Colors", fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray)
                            // Simplified color pickers for brevity in this response, 
                            // in a real app these would be color selection dialogs or a grid
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(AssistantColor.BLUE, AssistantColor.PINK, AssistantColor.ORANGE, AssistantColor.CYAN, AssistantColor.NEON).forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(color.hex))
                                            .clickable { 
                                                assistantViewModel.setCustomMode(true, context)
                                                assistantViewModel.setCustomColors(color, color, color, context) 
                                            }
                                    )
                                }
                            }
                        }

                        SettingsItem(
                            title = "Assistant Type",
                            isDarkMode = isDarkMode,
                            trailing = {
                                Row {
                                    FilterChip(
                                        selected = assistantPrefs.gender == AssistantGender.MALE,
                                        onClick = { assistantViewModel.setGender(AssistantGender.MALE, context) },
                                        label = { Text("Male") },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00D1B2).copy(alpha = 0.2f), selectedLabelColor = Color(0xFF00D1B2))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    FilterChip(
                                        selected = assistantPrefs.gender == AssistantGender.FEMALE,
                                        onClick = { assistantViewModel.setGender(AssistantGender.FEMALE, context) },
                                        label = { Text("Female") },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00D1B2).copy(alpha = 0.2f), selectedLabelColor = Color(0xFF00D1B2))
                                    )
                                }
                            }
                        )

                        SettingsItem(
                            title = "Assistant Animation",
                            isDarkMode = isDarkMode,
                            trailing = {
                                Switch(checked = assistantPrefs.isAnimated, onCheckedChange = { assistantViewModel.setAnimated(it, context) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00D1B2)))
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
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Notifications Info (Spark Plan)
            CalculatorCard(isDarkMode = isDarkMode) {
                Text("Notifications", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    "Notifications work while FinCalc is running in the background. Full closed-app push notifications may be added later.",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray
                )
            }

            Spacer(Modifier.height(24.dp))

            // Backup & Restore (Registered only)
            if (!isGuest) {
                CalculatorCard(isDarkMode = isDarkMode) {
                    Text(text = stringResource(R.string.backup_restore), modifier = Modifier.fillMaxWidth(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                    Spacer(Modifier.height(16.dp))
                    BouncyButton(
                        onClick = { navController.navigate(com.enosh.fincalc.ui.navigation.Screen.BackupRestore.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) { 
                        Icon(Icons.Default.Backup, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Backup & Restore Data")
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Account Section
            CalculatorCard(isDarkMode = isDarkMode) {
                Text("Account", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                
                BouncyButton(
                    onClick = { 
                        if (isGuest) showGuestLogoutConfirmDialog = true
                        else showLogoutConfirmDialog = true 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Red.copy(alpha = 0.1f)
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isGuest) "Leave Guest Mode" else "Logout", fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (showEditDialog) {
        var newName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.edit_profile), color = if (isDarkMode) Color.White else Color.Black) },
            text = {
                ValidatedTextField(
                    value = newName, 
                    onValueChange = { newName = it }, 
                    label = stringResource(R.string.full_name),
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                )
            },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        UserUtils.uploadCurrentUser(providedName = newName)
                        userName = newName
                        showEditDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))) { Text(stringResource(R.string.save), color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text(stringResource(R.string.cancel), color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray) } },
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("Logout", color = if (isDarkMode) Color.White else Color.Black) },
            text = { Text("Are you sure you want to log out?", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black) },
            confirmButton = {
                Button(onClick = { onLogout() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Logout", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showLogoutConfirmDialog = false }) { Text("Cancel", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray) } },
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
        )
    }

    if (showGuestLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showGuestLogoutConfirmDialog = false },
            title = { Text("Leaving Guest Mode", color = if (isDarkMode) Color.White else Color.Black) },
            text = { Text("You are using a guest account. Guest data is not saved. If you leave now, all guest notes, preferences, history, and temporary data will be permanently deleted.", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black) },
            confirmButton = {
                Button(onClick = { 
                    showGuestLogoutConfirmDialog = false
                    onLogout() 
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Leave Guest", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showGuestLogoutConfirmDialog = false }) { Text("Cancel", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray) } },
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
        )
    }

    if (showRoastWarningDialog) {
        AlertDialog(
            onDismissRequest = { showRoastWarningDialog = false },
            title = { Text("Roast Mode", color = if (isDarkMode) Color.White else Color.Black) },
            text = { Text("Roast Mode is for jokes only. Responses may sound sarcastic or rude. Do not take them seriously.", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black) },
            confirmButton = {
                Button(onClick = { 
                    showRoastWarningDialog = false
                    assistantViewModel.setRoastMode(true, context)
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))) { Text("Turn On", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showRoastWarningDialog = false }) { Text("Cancel", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray) } },
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
        )
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String? = null, isDarkMode: Boolean = true, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = if (isDarkMode) Color.White else Color.Black)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray)
        }
        trailing()
    }
}
