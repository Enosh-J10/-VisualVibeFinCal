package com.enosh.fincalc.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
    val friends by friendsViewModel.friends.collectAsState()
    val nicknames by friendsViewModel.friendNicknames.collectAsState()
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    
    // Improved name resolution
    var resolvedFriendName by remember { mutableStateOf("Chat") }
    var friendProfile by remember { mutableStateOf<com.enosh.fincalc.data.model.User?>(null) }
    
    LaunchedEffect(friendUid, friends, nicknames) {
        val friendFromList = friends.find { it.uid == friendUid }
        friendProfile = friendFromList
        
        val nickname = nicknames[friendUid]
        if (nickname != null) {
            resolvedFriendName = nickname
        } else if (friendFromList != null) {
            resolvedFriendName = friendFromList.name.ifBlank { friendFromList.email.substringBefore("@") }
        } else {
            // Fetch from Firestore if not in friends list
            val profile = chatViewModel.getUserProfile(friendUid)
            if (profile != null) {
                friendProfile = profile
                resolvedFriendName = profile.name.ifBlank { profile.email.substringBefore("@") }
                if (resolvedFriendName.isBlank()) resolvedFriendName = profile.finCalcId.ifBlank { "Friend" }
            }
        }
        android.util.Log.d("ChatDebug", "Resolved friend name: $resolvedFriendName for $friendUid")
    }

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (friendUid.isBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unable to open chat. Friend profile missing.", color = Color.Red)
        }
        return
    }

    LaunchedEffect(chatId, friendUid) {
        android.util.Log.d("ChatDebug", "Opening ChatRoom: chatId=$chatId, friendUid=$friendUid, currentUid=$currentUid, friendName=$resolvedFriendName")
        chatViewModel.ensureChatExists(chatId, friendUid)
        chatViewModel.listenToMessages(chatId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            chatViewModel.uploadFile(chatId, friendUid, it, "document", "file_${System.currentTimeMillis()}")
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            chatViewModel.uploadFile(chatId, friendUid, it, "image", "img_${System.currentTimeMillis()}.jpg")
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            chatViewModel.uploadFile(chatId, friendUid, it, "video", "vid_${System.currentTimeMillis()}.mp4")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF00D1B2).copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2), modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(resolvedFriendName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Online", fontSize = 11.sp, color = Color(0xFF00D1B2))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding().imePadding(),
                color = if (isDarkMode) Color(0xFF1B2C33) else Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    var showMoreActions by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { showMoreActions = !showMoreActions },
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            if (showMoreActions) Icons.Default.Close else Icons.Default.Add, 
                            contentDescription = "More", 
                            tint = Color(0xFF00D1B2)
                        )
                    }

                    if (showMoreActions) {
                        Row {
                            IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color(0xFF00D1B2))
                            }
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Image", tint = Color(0xFF00D1B2))
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        placeholder = { Text("Type a message...", fontSize = 14.sp) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00D1B2).copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                            focusedContainerColor = if (isDarkMode) Color(0xFF24343D) else Color(0xFFF0F2F5),
                            unfocusedContainerColor = if (isDarkMode) Color(0xFF24343D) else Color(0xFFF0F2F5),
                        ),
                        maxLines = 5
                    )
                    
                    Spacer(Modifier.width(4.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                android.util.Log.d("ChatDebug", "Send FAB clicked")
                                chatViewModel.sendMessage(chatId, messageText, friendUid) { success, error ->
                                    if (success) {
                                        messageText = ""
                                    } else {
                                        android.widget.Toast.makeText(context, "Message failed: $error", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        containerColor = Color(0xFF00D1B2),
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp).padding(bottom = 4.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Chat, 
                        null, 
                        modifier = Modifier.size(64.dp), 
                        tint = Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No messages yet.", color = Color.Gray)
                    Text("Say hello to start the chat.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.messageId }) { message ->
                        MessageItem(message, currentUid, isDarkMode)
                    }
                }
            }
            
            val progress by chatViewModel.uploadProgress.collectAsState()
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress!! },
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Color(0xFF00D1B2)
                )
            }
        }
    }
}

@Composable
fun MessageItem(message: Message, currentUid: String, isDarkMode: Boolean) {
    val isMine = message.senderUid == currentUid
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = message.createdAt?.toDate()?.let { sdf.format(it) } ?: ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMine) Color(0xFF00D1B2) else if (isDarkMode) Color(0xFF1B2C33) else Color(0xFFF0F0F0),
            contentColor = if (isMine) Color.White else if (isDarkMode) Color.White else Color.Black,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 0.dp,
                bottomEnd = if (isMine) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                when (message.type) {
                    "text" -> {
                        Text(message.text)
                    }
                    "image" -> {
                        AsyncImage(
                            model = message.fileUrl,
                            contentDescription = "Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    "video" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Video", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(message.fileName ?: "video.mp4", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                    else -> { // document
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Document", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(message.fileName ?: "file", fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        timeStr, 
                        fontSize = 10.sp, 
                        color = (if (isMine) Color.White.copy(alpha = 0.7f) else Color.Gray)
                    )
                    if (isMine) {
                        Spacer(Modifier.width(4.dp))
                        val isRead = message.readBy.isNotEmpty()
                        Icon(
                            if (isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isRead) Color(0xFF34B7F1) else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
