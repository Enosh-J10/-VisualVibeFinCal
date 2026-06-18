package com.enosh.fincalc.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.enosh.fincalc.data.model.AiConfig
import com.enosh.fincalc.data.model.AiProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    navController: NavController,
    isDarkMode: Boolean
) {
    var selectedProvider by remember { mutableStateOf(AiConfig.provider) }
    var streamingMode by remember { mutableStateOf(AiConfig.streamingMode) }
    var saveHistory by remember { mutableStateOf(AiConfig.saveHistory) }
    var voiceReplies by remember { mutableStateOf(AiConfig.voiceReplies) }
    var markdownRendering by remember { mutableStateOf(AiConfig.markdownRendering) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("AI Assistant Settings") 
                        Text(
                            "Powered by Gemini • ${AiConfig.currentGeminiModel}", 
                            fontSize = 12.sp, 
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("AI Provider", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            AiProvider.entries.forEach { provider ->
                val isAvailable = provider == AiProvider.GEMINI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isAvailable) { 
                            selectedProvider = provider
                            AiConfig.provider = provider
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedProvider == provider,
                        onClick = { 
                            selectedProvider = provider
                            AiConfig.provider = provider
                        },
                        enabled = isAvailable
                    )
                    Column {
                        Text(
                            provider.name, 
                            modifier = Modifier.padding(start = 8.dp),
                            color = if (isAvailable) Color.Unspecified else Color.Gray
                        )
                        if (!isAvailable) {
                            Text(
                                "Coming Soon", 
                                fontSize = 10.sp, 
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text("Features", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), fontSize = 16.sp)
            
            SettingsSwitchRow(
                title = "Streaming Mode",
                checked = streamingMode,
                onCheckedChange = { 
                    streamingMode = it
                    AiConfig.streamingMode = it
                }
            )
            
            SettingsSwitchRow(
                title = "Save Chat History",
                checked = saveHistory,
                onCheckedChange = { 
                    saveHistory = it
                    AiConfig.saveHistory = it
                }
            )
            
            SettingsSwitchRow(
                title = "Voice Replies",
                checked = voiceReplies,
                onCheckedChange = { 
                    voiceReplies = it
                    AiConfig.voiceReplies = it
                }
            )
            
            SettingsSwitchRow(
                title = "Markdown Rendering",
                checked = markdownRendering,
                onCheckedChange = { 
                    markdownRendering = it
                    AiConfig.markdownRendering = it
                }
            )

            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = { /* Export */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Export Chats")
            }
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = { /* Reset */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
            ) {
                Text("Reset AI Settings", color = Color.White)
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

