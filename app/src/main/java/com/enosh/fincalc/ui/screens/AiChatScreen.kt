package com.enosh.fincalc.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.enosh.fincalc.data.model.*
import com.enosh.fincalc.ui.navigation.Screen
import com.enosh.fincalc.viewmodel.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    viewModel: AiViewModel = viewModel(factory = AiViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val currentChatId by viewModel.currentChatId.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    
    val assistantPrefs by assistantViewModel.prefs.collectAsState()
    val isRoastMode = assistantPrefs.isRoastMode
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    var showRenameDialog by remember { mutableStateOf<Conversation?>(null) }
    var renameText by remember { mutableStateOf("") }
    
    // TTS
    val tts = remember {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
        ttsInstance
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Helper to get file name
    fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = it.getString(index)
            }
        }
        return name
    }

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedFileUri = null
            selectedFileName = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = tempCameraUri
            selectedFileUri = null
            selectedFileName = null
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = getFileName(uri)
            selectedImageUri = null
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val spokenText = results?.get(0) ?: ""
        if (spokenText.isNotBlank()) {
            inputText = spokenText
            viewModel.clearError()
        }
    }

    // Permission Launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "camera_image_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            }
            speechLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AiChatUiState.Success) {
            val messages = (uiState as AiChatUiState.Success).messages
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxHeight().padding(16.dp)) {
                    Text("Chat History", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF00D1B2))
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            viewModel.startNewChat()
                            coroutineScope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("New Chat")
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    LazyColumn(Modifier.weight(1f)) {
                        items(conversations) { conv ->
                            val isSelected = conv.id == currentChatId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF00D1B2).copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { 
                                        viewModel.selectChat(conv.id)
                                        coroutineScope.launch { drawerState.close() }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = if (isSelected) Color(0xFF00D1B2) else Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    conv.title, 
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    color = if (isSelected) Color(0xFF00D1B2) else if (isDarkMode) Color.White else Color.Black,
                                    fontSize = 14.sp
                                )
                                IconButton(onClick = { 
                                    showRenameDialog = conv
                                    renameText = conv.title
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteConversation(conv) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = { viewModel.clearAllChats() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.DeleteSweep, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All History")
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize()
                .imePadding()
                .safeDrawingPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Color(0xFF00D1B2)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "FinCalc AI",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color.Black
                                )
                                Text(
                                    "Powered by Gemini • ${AiConfig.currentGeminiModel}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack() 
                            } else {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                        IconButton(onClick = { viewModel.startNewChat() }) {
                            Icon(Icons.Default.AddComment, contentDescription = "New Chat")
                        }
                        IconButton(onClick = { navController.navigate(Screen.AiSettings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDarkMode) Color(0xFF121212) else Color.White
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(if (isDarkMode) Color(0xFF0B0B0B) else Color(0xFFF7F9FB))
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    val messages = when (val state = uiState) {
                        is AiChatUiState.Success -> state.messages
                        is AiChatUiState.Error -> state.messages
                        else -> emptyList()
                    }

                    if (uiState is AiChatUiState.Idle || (uiState is AiChatUiState.Success && messages.isEmpty())) {
                        EmptyChatContent(onSuggestionClick = { 
                            if (currentChatId == null) {
                                coroutineScope.launch {
                                    viewModel.startNewChat()
                                    viewModel.sendMessage(it)
                                }
                            } else {
                                viewModel.sendMessage(it)
                            }
                        }, isDarkMode = isDarkMode)
                    } else if (uiState is AiChatUiState.Loading && messages.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF00D1B2))
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(messages) { message ->
                                    AiChatMessageItem(
                                    message = message,
                                    isDarkMode = isDarkMode,
                                    isRoastMode = isRoastMode,
                                    onCopy = { clipboardManager.setText(AnnotatedString(message.content)) },
                                    onShare = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, message.content)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    onRegenerate = {
                                        val lastUserMsg = messages.lastOrNull { it.role == MessageRole.USER }
                                        if (lastUserMsg != null) {
                                            viewModel.sendMessage(lastUserMsg.content)
                                        }
                                    },
                                    onSpeak = {
                                        tts?.speak(message.content, TextToSpeech.QUEUE_FLUSH, null, null)
                                    }
                                )
                                }
                                if (isTyping) {
                                    item {
                                        TypingIndicator(isDarkMode)
                                    }
                                }
                            }
                            
                            if (uiState is AiChatUiState.Error) {
                                Surface(
                                    color = Color.Red.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            (uiState as AiChatUiState.Error).message, 
                                            color = Color.Red, 
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { viewModel.clearError() }) {
                                            Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Area
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = if (isDarkMode) Color(0xFF121212) else Color.White
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Attachment Previews
                        if (selectedImageUri != null || selectedFileUri != null) {
                            Row(
                                modifier = Modifier.padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(60.dp)) {
                                    if (selectedImageUri != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(selectedImageUri),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else if (selectedFileUri != null) {
                                        Surface(
                                            color = Color.Gray.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(Icons.Default.Description, null, modifier = Modifier.padding(12.dp), tint = Color.Gray)
                                        }
                                    }
                                    IconButton(
                                        onClick = { 
                                            selectedImageUri = null
                                            selectedFileUri = null
                                            selectedFileName = null
                                        },
                                        modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                                if (selectedFileName != null) {
                                    Text(
                                        selectedFileName!!, 
                                        modifier = Modifier.padding(start = 8.dp), 
                                        fontSize = 12.sp, 
                                        maxLines = 1,
                                        color = if (isDarkMode) Color.White else Color.Black
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.Image, "Gallery", tint = Color.Gray)
                            }
                            IconButton(onClick = { 
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    val file = File(context.cacheDir, "camera_image_${System.currentTimeMillis()}.jpg")
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }) {
                                Icon(Icons.Default.PhotoCamera, "Camera", tint = Color.Gray)
                            }
                            IconButton(onClick = { 
                                fileLauncher.launch(arrayOf("application/pdf", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                            }) {
                                Icon(Icons.Default.AttachFile, "File", tint = Color.Gray)
                            }
                            
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { 
                                    inputText = it 
                                    if (it.isNotBlank()) viewModel.clearError()
                                },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Ask anything...") },
                                maxLines = 6,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00D1B2),
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                    focusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF1F3F4),
                                    unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF1F3F4)
                                )
                            )
                            
                            Spacer(Modifier.width(8.dp))
                            
                            val isSendActive = inputText.isNotBlank() || selectedImageUri != null || selectedFileUri != null
                            
                            if (!isSendActive) {
                                IconButton(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                            }
                                            speechLauncher.launch(intent)
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF00D1B2), CircleShape)
                                ) {
                                    Icon(Icons.Default.Mic, "Voice", tint = Color.White)
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val chatId = currentChatId ?: run {
                                                viewModel.startNewChat()
                                                viewModel.currentChatId.filterNotNull().first()
                                            }
                                            
                                            var base64Image: String? = null
                                            if (selectedImageUri != null) {
                                                try {
                                                    val bytes = context.contentResolver.openInputStream(selectedImageUri!!)?.readBytes()
                                                    if (bytes != null) {
                                                        base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("AiChat", "Image process error", e)
                                                }
                                            }
                                            
                                            var finalContent = inputText
                                            if (selectedFileUri != null) {
                                                if (selectedFileName?.endsWith(".txt") == true) {
                                                    val text = context.contentResolver.openInputStream(selectedFileUri!!)?.bufferedReader()?.use { it.readText() }
                                                    if (text != null) {
                                                        finalContent += "\n\nFile Content:\n$text"
                                                    }
                                                } else {
                                                    finalContent += "\n\n[File attached: $selectedFileName - Extraction coming soon]"
                                                }
                                            }

                                            viewModel.sendMessage(finalContent, base64Image)
                                            inputText = ""
                                            selectedImageUri = null
                                            selectedFileUri = null
                                            selectedFileName = null
                                        }
                                    },
                                    enabled = !isTyping,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(if (isTyping) Color.Gray else Color(0xFF00D1B2), CircleShape)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename Chat") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Chat Title") }
                )
            },
            confirmButton = {
                Button(onClick = { 
                    viewModel.renameConversation(showRenameDialog!!, renameText)
                    showRenameDialog = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AiChatMessageItem(
    message: ChatMessage,
    isDarkMode: Boolean,
    isRoastMode: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.role == MessageRole.USER
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = if (isRoastMode) Color(0xFF9C27B0) else Color(0xFF00D1B2)
                ) {
                    Icon(
                        if (isRoastMode) Icons.Default.Whatshot else Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            
            Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
                Surface(
                    color = if (isUser) Color(0xFF00D1B2) else if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    ),
                    tonalElevation = if (isUser) 0.dp else 2.dp,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp).widthIn(max = 280.dp)) {
                        MarkdownText(
                            text = message.content,
                            color = if (isUser) Color.White else if (isDarkMode) Color.White else Color.Black
                        )
                        
                        Text(
                            text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                            fontSize = 10.sp,
                            color = if (isUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                        )
                    }
                }
                
                if (!isUser) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.ContentCopy, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Share, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onRegenerate, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Refresh, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onSpeak, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.VolumeUp, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatContent(onSuggestionClick: (String) -> Unit, isDarkMode: Boolean) {
    val categories = listOf(
        "Finance" to listOf("Create a budget", "Explain SIP", "Save money tips"),
        "Business" to listOf("Grow my business", "Marketing ideas", "Income tracking"),
        "Travel" to listOf("Plan London trip", "Dubai budget", "Split expenses"),
        "General" to listOf("Who invented internet?", "Explain black holes", "Current date and time"),
        "Coding" to listOf("Kotlin tutorial", "Explain MVVM", "Fix this error")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(40.dp))
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = Color(0xFF00D1B2).copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF00D1B2),
                    modifier = Modifier.padding(20.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "How can I help you today?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else Color.Black
            )
            Spacer(Modifier.height(32.dp))
        }

        items(categories) { (category, suggestions) ->
            Text(
                category,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D1B2),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = { Text(suggestion) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator(isDarkMode: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .background(if (isDarkMode) Color(0xFF1E1E1E) else Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = dot1Alpha)))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = dot2Alpha)))
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = dot3Alpha)))
            Spacer(Modifier.width(8.dp))
            Text("Thinking...", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MarkdownText(text: String, color: Color) {
    // A slightly improved markdown renderer
    val annotatedString = remember(text) {
        val builder = AnnotatedString.Builder()
        val lines = text.split("\n")
        lines.forEachIndexed { index, line ->
            var currentLine = line
            
            // Bullet points
            if (currentLine.trim().startsWith("•") || currentLine.trim().startsWith("*") || currentLine.trim().startsWith("-")) {
                builder.append("  • ")
                currentLine = currentLine.trim().substring(1).trim()
            }
            
            // Bold (simplified: **text**)
            val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
            var lastMatchEnd = 0
            boldRegex.findAll(currentLine).forEach { match ->
                builder.append(currentLine.substring(lastMatchEnd, match.range.first))
                builder.pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(match.groupValues[1])
                builder.pop()
                lastMatchEnd = match.range.last + 1
            }
            builder.append(currentLine.substring(lastMatchEnd))
            
            if (index < lines.size - 1) {
                builder.append("\n")
            }
        }
        builder.toAnnotatedString()
    }
    
    Text(
        text = annotatedString,
        color = color,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
}
