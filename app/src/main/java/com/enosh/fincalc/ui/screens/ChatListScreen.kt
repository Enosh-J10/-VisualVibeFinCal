package com.enosh.fincalc.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
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
import com.enosh.fincalc.data.model.ChatRoom
import com.enosh.fincalc.viewmodel.ChatViewModel
import com.enosh.fincalc.viewmodel.FriendsViewModel
import com.enosh.fincalc.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatListItemData(
    val chatId: String?,
    val friendUid: String,
    val friendName: String,
    val lastMessage: String,
    val lastMessageAt: com.google.firebase.Timestamp?,
    val isNew: Boolean,
    val unreadCount: Int = 0,
    val profilePic: String? = null
)

@Composable
fun ChatListScreen(
    navController: NavController,
    isDarkMode: Boolean,
    chatViewModel: ChatViewModel = viewModel(),
    friendsViewModel: FriendsViewModel = viewModel()
) {
    val chats by chatViewModel.chats.collectAsState()
    val friends by friendsViewModel.friends.collectAsState()
    val nicknames by friendsViewModel.friendNicknames.collectAsState()
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val scope = rememberCoroutineScope()

    val displayList = remember(chats, friends, nicknames) {
        val list = mutableListOf<ChatListItemData>()
        
        // Add existing chats
        chats.forEach { chat ->
            val otherUid = chat.memberUids.find { it != currentUid } ?: ""
            if (otherUid.isNotBlank()) {
                val friend = friends.find { it.uid == otherUid }
                val unreadCount = chat.unreadCounts[currentUid] ?: 0
                list.add(
                    ChatListItemData(
                        chatId = chat.chatId,
                        friendUid = otherUid,
                        friendName = nicknames[otherUid] ?: friend?.name ?: "",
                        lastMessage = chat.lastMessage,
                        lastMessageAt = chat.lastMessageAt,
                        isNew = unreadCount > 0,
                        unreadCount = unreadCount,
                        profilePic = friend?.profilePic
                    )
                )
            }
        }
        
        // Add friends who don't have chats yet
        friends.forEach { friend ->
            if (list.none { it.friendUid == friend.uid }) {
                list.add(
                    ChatListItemData(
                        chatId = null,
                        friendUid = friend.uid,
                        friendName = nicknames[friend.uid] ?: friend.name,
                        lastMessage = "Tap to start chatting",
                        lastMessageAt = null,
                        isNew = true,
                        profilePic = friend.profilePic
                    )
                )
            }
        }
        
        // Sort: Latest message first, then alphabetical
        list.sortWith(compareByDescending<ChatListItemData> { it.lastMessageAt?.seconds ?: 0L }
            .thenBy { it.friendName.lowercase() })
        list
    }

    CalculatorScreenScaffold(
        title = "Messages",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (displayList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("No friends yet", color = Color.Gray)
                        TextButton(onClick = { navController.navigate(Screen.Friends.route) }) {
                            Text("Find friends to start chatting", color = Color(0xFF00D1B2))
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayList, key = { it.friendUid }) { item ->
                        val context = LocalContext.current
                        ChatListItem(
                            item = item,
                            isDarkMode = isDarkMode,
                            chatViewModel = chatViewModel,
                            onClick = {
                                try {
                                    val friendUid = item.friendUid
                                    if (friendUid.isBlank()) {
                                        Toast.makeText(context, "Unable to open chat. Friend profile missing.", Toast.LENGTH_SHORT).show()
                                        return@ChatListItem
                                    }
                                    
                                    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                    val chatId = listOf(currentUid, friendUid).sorted().joinToString("_")
                                    
                                    navController.navigate(Screen.ChatRoom.createRoute(chatId, friendUid))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    item: ChatListItemData,
    isDarkMode: Boolean,
    chatViewModel: ChatViewModel,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = item.lastMessageAt?.toDate()?.let { sdf.format(it) } ?: ""

    var displayName by remember { mutableStateOf(item.friendName.ifBlank { "Loading..." }) }
    
    LaunchedEffect(item.friendUid, item.friendName) {
        if (item.friendName.isBlank()) {
            val profile = chatViewModel.getUserProfile(item.friendUid)
            if (profile != null) {
                displayName = profile.name.ifBlank { 
                    profile.email.substringBefore("@").ifBlank { 
                        profile.finCalcId.ifBlank { "Friend" } 
                    } 
                }
            } else {
                displayName = "Friend"
            }
        } else {
            displayName = item.friendName
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!item.profilePic.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = item.profilePic,
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFF00D1B2).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2), modifier = Modifier.size(28.dp))
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (timeStr.isNotBlank()) {
                    Text(timeStr, fontSize = 12.sp, color = if (item.unreadCount > 0) Color(0xFF00D1B2) else Color.Gray)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.lastMessage,
                    fontSize = 14.sp,
                    color = if (item.unreadCount > 0) (if (isDarkMode) Color.White else Color.Black) else Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontWeight = if (item.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (item.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFF00D1B2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            item.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
