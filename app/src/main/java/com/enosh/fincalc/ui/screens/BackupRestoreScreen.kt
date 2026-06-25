package com.enosh.fincalc.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.enosh.fincalc.viewmodel.BackupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    navController: NavController,
    isDarkMode: Boolean,
    viewModel: BackupViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoading by viewModel.isBackupLoading.collectAsState()
    val message by viewModel.backupMessage.collectAsState()
    val isGuest = remember { 
        context.getSharedPreferences(com.enosh.fincalc.utils.UserUtils.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean("is_guest", false) 
    }

    var showCloudRestoreConfirm by remember { mutableStateOf(false) }
    var showLocalRestoreConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.localExport(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            pendingImportUri = it
            showLocalRestoreConfirm = true
        }
    }

    CalculatorScreenScaffold(
        title = "Backup & Restore",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).background(if (isDarkMode) Color(0xFF0F2027) else Color(0xFFF0F4F8))) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                if (isGuest) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.1f))
                    ) {
                        Text(
                            "Backup is available only for signed-in users.",
                            modifier = Modifier.padding(16.dp),
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                } else {
                    Text("Cloud Backup", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    CalculatorCard(isDarkMode = isDarkMode) {
                        Column(Modifier.fillMaxWidth()) {
                            Text("Store your data safely in your private cloud storage.", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BouncyButton(
                                    onClick = { viewModel.cloudBackup() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cloud Backup", fontSize = 12.sp)
                                }
                                BouncyButton(
                                    onClick = { showCloudRestoreConfirm = true },
                                    modifier = Modifier.weight(1f),
                                    containerColor = Color.Transparent,
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp), tint = Color(0xFF00D1B2))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Restore Cloud", fontSize = 12.sp, color = Color(0xFF00D1B2))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text("Local Backup", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    CalculatorCard(isDarkMode = isDarkMode) {
                        Column(Modifier.fillMaxWidth()) {
                            Text("Export or import data as a JSON file.", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BouncyButton(
                                    onClick = { exportLauncher.launch("fincalc_backup_${System.currentTimeMillis()}.json") },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Export JSON", fontSize = 12.sp)
                                }
                                BouncyButton(
                                    onClick = { importLauncher.launch("application/json") },
                                    modifier = Modifier.weight(1f),
                                    containerColor = Color.Transparent,
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp), tint = Color(0xFF00D1B2))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Import JSON", fontSize = 12.sp, color = Color(0xFF00D1B2))
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                Surface(Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.3f)) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00D1B2))
                    }
                }
            }
        }
    }

    if (showCloudRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showCloudRestoreConfirm = false },
            title = { Text("Cloud Restore") },
            text = { Text("This will overwrite your current local data with the latest cloud backup. Continue?") },
            confirmButton = {
                Button(onClick = {
                    showCloudRestoreConfirm = false
                    viewModel.cloudRestore()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Restore from Cloud") }
            },
            dismissButton = { TextButton(onClick = { showCloudRestoreConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showLocalRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showLocalRestoreConfirm = false },
            title = { Text("Local Restore") },
            text = { Text("This will overwrite your current local data with the contents of this file. Continue?") },
            confirmButton = {
                Button(onClick = {
                    showLocalRestoreConfirm = false
                    pendingImportUri?.let { viewModel.localImport(it) }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Import & Overwrite") }
            },
            dismissButton = { TextButton(onClick = { showLocalRestoreConfirm = false }) { Text("Cancel") } }
        )
    }
}
