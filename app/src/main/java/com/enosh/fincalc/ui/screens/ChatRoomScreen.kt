package com.enosh.fincalc.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.enosh.fincalc.data.model.Message
import com.enosh.fincalc.viewmodel.ChatViewModel
import com.enosh.fincalc.viewmodel.FriendsViewModel
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    friendUid: String,
    navController: NavController,
    isDarkMode: Boolean,
    chatViewModel: ChatViewModel = viewModel(),
    friendsViewModel: FriendsViewModel = viewModel()
) {
    val messages by chatViewModel.messages.collectAsState()
    val friendStatus by chatViewModel.friendStatus.collectAsState()
    val errorMessage by chatViewModel.errorMessage.collectAsState()
    val friends by friendsViewModel.friends.collectAsState()
    val nicknames by friendsViewModel.friendNicknames.collectAsState()
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    
    val context = LocalContext.current
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            chatViewModel.clearError()
        }
    }
    
    var resolvedFriendName by remember { mutableStateOf("Chat") }
    
    LaunchedEffect(friendUid, friends, nicknames) {
        val friendFromList = friends.find { it.uid == friendUid }
        val nickname = nicknames[friendUid]
        if (nickname != null) {
            resolvedFriendName = nickname
        } else if (friendFromList != null) {
            resolvedFriendName = friendFromList.name.ifBlank { friendFromList.email.substringBefore("@") }
        } else {
            val profile = chatViewModel.getUserProfile(friendUid)
            if (profile != null) {
                resolvedFriendName = profile.name.ifBlank { profile.email.substringBefore("@") }
                if (resolvedFriendName.isBlank()) resolvedFriendName = profile.finCalcId.ifBlank { "Friend" }
            }
        }
    }

    var messageText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var editText by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(chatId, friendUid) {
        chatViewModel.currentlyOpenChatId = chatId
        chatViewModel.ensureChatExists(chatId, friendUid)
        chatViewModel.listenToMessages(chatId)
        chatViewModel.listenToFriendStatus(chatId, friendUid)
    }

    DisposableEffect(chatId) {
        onDispose {
            chatViewModel.currentlyOpenChatId = null
            chatViewModel.setTypingStatus(chatId, false)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        try {
            uri?.let { chatViewModel.uploadFile(chatId, friendUid, it, "file", "attachment_${System.currentTimeMillis()}") }
        } catch (e: Exception) {
            Log.e("ChatAttachmentError", "File picker failure", e)
            Toast.makeText(context, "Failed to select file", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        try {
            uri?.let { chatViewModel.uploadFile(chatId, friendUid, it, "image", "image_${System.currentTimeMillis()}.jpg") }
        } catch (e: Exception) {
            Log.e("ChatAttachmentError", "Gallery picker failure", e)
            Toast.makeText(context, "Failed to select image", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        try {
            if (success && tempCameraUri != null) {
                chatViewModel.uploadFile(chatId, friendUid, tempCameraUri!!, "image", "camera_${System.currentTimeMillis()}.jpg")
            }
        } catch (e: Exception) {
            Log.e("ChatAttachmentError", "Camera capture failure", e)
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        try {
            val fileName = "camera_${System.currentTimeMillis()}.jpg"
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            tempCameraUri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (tempCameraUri != null) {
                cameraLauncher.launch(tempCameraUri!!)
            } else {
                Toast.makeText(context, "Could not create file for camera", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ChatAttachmentError", "Camera launch failure", e)
            Toast.makeText(context, "Cannot open camera", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2), modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(resolvedFriendName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            if (friendStatus != null) Text(friendStatus!!, fontSize = 11.sp, color = Color(0xFF00D1B2))
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, modifier = Modifier.navigationBarsPadding().imePadding(), color = if (isDarkMode) Color(0xFF1B2C33) else Color.White) {
                Column {
                    if (replyingTo != null) {
                        Surface(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), color = Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Replying to", fontSize = 10.sp, color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
                                    Text(replyingTo!!.text, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        var showMore by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMore = !showMore }, modifier = Modifier.padding(bottom = 4.dp)) {
                            Icon(if (showMore) Icons.Default.Close else Icons.Default.Add, null, tint = Color(0xFF00D1B2))
                        }
                        if (showMore) {
                            IconButton(onClick = { filePickerLauncher.launch("*/*") }) { Icon(Icons.Default.AttachFile, null, tint = Color(0xFF00D1B2)) }
                            IconButton(onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFF00D1B2)) }
                            IconButton(onClick = { launchCamera() }) { Icon(Icons.Default.PhotoCamera, null, tint = Color(0xFF00D1B2)) }
                            IconButton(onClick = { chatViewModel.uploadTestFile() }) { Icon(Icons.Default.BugReport, null, tint = Color.Red) }
                            IconButton(onClick = { 
                                com.enosh.fincalc.utils.NotificationHelper.showChatNotification(
                                    context, "System Test", "This is a test notification 🔔", chatId, friendUid
                                )
                            }) { Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFFFA500)) }
                        }
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it; chatViewModel.setTypingStatus(chatId, it.isNotBlank()) },
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                            placeholder = { Text("Type a message...", fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 5
                        )
                        Spacer(Modifier.width(4.dp))
                        FloatingActionButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    val currentMsg = messageText
                                    chatViewModel.sendMessage(chatId, currentMsg, friendUid, replyTo = replyingTo) { success, error ->
                                        if (success) {
                                            messageText = ""
                                            replyingTo = null
                                            chatViewModel.setTypingStatus(chatId, false)
                                        } else {
                                            Toast.makeText(context, "Send failed: $error", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            containerColor = Color(0xFF00D1B2), contentColor = Color.White, modifier = Modifier.size(48.dp).padding(bottom = 4.dp), shape = androidx.compose.foundation.shape.CircleShape
                        ) { Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Chat, null, Modifier.size(64.dp), Color.Gray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))
                    Text("No messages yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(messages, key = { it.messageId }) { msg ->
                        MessageItem(
                            message = msg, currentUid = currentUid, isDarkMode = isDarkMode,
                            onDelete = { chatViewModel.deleteMessage(chatId, msg.messageId) },
                            onEdit = { editingMessage = msg; editText = msg.text },
                            onReply = { replyingTo = msg },
                            onCopy = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Message", msg.text))
                                Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            val progress by chatViewModel.uploadProgress.collectAsState()
            progress?.let { currentProgress ->
                LinearProgressIndicator(
                    progress = { currentProgress },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Color(0xFF00D1B2)
                )
            }
        }
    }

    if (editingMessage != null) {
        AlertDialog(
            onDismissRequest = { editingMessage = null },
            title = { Text("Edit Message") },
            text = { OutlinedTextField(value = editText, onValueChange = { editText = it }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = { chatViewModel.editMessage(chatId, editingMessage!!.messageId, editText); editingMessage = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingMessage = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun MessageItem(
    message: Message, currentUid: String, isDarkMode: Boolean,
    onDelete: () -> Unit, onEdit: () -> Unit, onReply: () -> Unit, onCopy: () -> Unit
) {
    val isMine = message.senderUid == currentUid
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = message.createdAt?.toDate()?.let { sdf.format(it) } ?: ""
    var showMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
        Surface(
            color = if (isMine) Color(0xFF00D1B2) else if (isDarkMode) Color(0xFF1B2C33) else Color(0xFFF0F0F0),
            contentColor = if (isMine) Color.White else if (isDarkMode) Color.White else Color.Black,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMine) 16.dp else 0.dp, bottomEnd = if (isMine) 0.dp else 16.dp),
            modifier = Modifier.widthIn(max = 280.dp).combinedClickable(onClick = {}, onLongClick = { showMenu = true })
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Reply") }, onClick = { onReply(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Reply, null) })
                    DropdownMenuItem(text = { Text("Copy") }, onClick = { onCopy(); showMenu = false }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                    if (isMine && message.type == "text") DropdownMenuItem(text = { Text("Edit") }, onClick = { onEdit(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    if (isMine) DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = { onDelete(); showMenu = false }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) })
                }
                
                if (message.replyToText != null) {
                    Surface(Modifier.padding(bottom = 4.dp), color = Color.Black.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                        Text(message.replyToText, fontSize = 11.sp, modifier = Modifier.padding(4.dp), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }

                when (message.type) {
                    "text" -> Text(message.text)
                    "image" -> AsyncImage(model = message.fileUrl, contentDescription = "Image", modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(8.dp)))
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (message.type == "video") Icons.Default.VideoLibrary else Icons.Default.Description, null)
                        Spacer(Modifier.width(8.dp))
                        Text(message.fileName ?: "File", fontSize = 14.sp)
                    }
                }
                Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    Text(timeStr, fontSize = 10.sp, color = if (isMine) Color.White.copy(alpha = 0.7f) else Color.Gray)
                    if (isMine) {
                        Spacer(Modifier.width(4.dp))
                        val isRead = message.readBy.any { it != currentUid }
                        Icon(if (isRead) Icons.Default.DoneAll else Icons.Default.Done, null, Modifier.size(12.dp), tint = if (isRead) Color(0xFF34B7F1) else Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}
