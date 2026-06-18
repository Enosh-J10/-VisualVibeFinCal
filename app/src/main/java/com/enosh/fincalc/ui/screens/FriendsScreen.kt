package com.enosh.fincalc.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.enosh.fincalc.data.model.User
import com.enosh.fincalc.data.model.FriendRequest
import com.enosh.fincalc.viewmodel.FriendsViewModel
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.enosh.fincalc.utils.QRUtils

@Composable
fun FriendsScreen(
    navController: NavController,
    isDarkMode: Boolean,
    viewModel: FriendsViewModel = viewModel(),
    initialSearch: String? = null
) {
    val context = LocalContext.current
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    var selectedTab by remember { mutableIntStateOf(if (initialSearch != null) 3 else 0) }
    val tabs = listOf("Friends", "Pending", "Sent", "Add Friend")
    
    LaunchedEffect(initialSearch) {
        if (initialSearch != null) {
            viewModel.searchUsers(initialSearch)
        }
    }

    CalculatorScreenScaffold(
        title = "Friends",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDarkMode) Color(0xFF0F2027) else Color(0xFFF0F4F8))
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White,
                contentColor = Color(0xFF00D1B2),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00D1B2)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 14.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTab) {
                0 -> FriendsListTab(viewModel, isDarkMode)
                1 -> PendingRequestsTab(viewModel, isDarkMode)
                2 -> SentRequestsTab(viewModel, isDarkMode)
                3 -> AddFriendTab(viewModel, isDarkMode, initialSearch)
            }
        }
    }
}

@Composable
fun FriendsListTab(viewModel: FriendsViewModel, isDarkMode: Boolean) {
    val friends by viewModel.friends.collectAsState()
    val nicknames by viewModel.friendNicknames.collectAsState()
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences(com.enosh.fincalc.utils.UserUtils.PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val finCalcIdKey = com.enosh.fincalc.utils.UserUtils.getFinCalcIdKey(currentUid)
    val finCalcId = sharedPref.getString(finCalcIdKey, "") ?: ""
    
    var showQRDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (finCalcId.isBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFFFA500))
                    Spacer(Modifier.width(8.dp))
                    Text("Profile is still syncing. Please try again.", fontSize = 12.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF00D1B2).copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Share Your Invite", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BouncyButton(
                        onClick = {
                            if (finCalcId.isBlank()) {
                                Toast.makeText(context, "FinCalc ID is still being generated. Please try again.", Toast.LENGTH_SHORT).show()
                            } else {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    val shareMessage = """
                                        Hey 👋 Add me on FinCalc!
                                        
                                        My FinCalc ID: $finCalcId
                                        
                                        Open FinCalc → Settings → Friends / Add Friends → search my ID and send a request.
                                    """.trimIndent()
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Link", fontSize = 12.sp)
                    }
                    
                    BouncyButton(
                        onClick = {
                            if (finCalcId.isBlank()) {
                                Toast.makeText(context, "FinCalc ID is still being generated. Please try again.", Toast.LENGTH_SHORT).show()
                            } else {
                                showQRDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        containerColor = Color.Transparent
                    ) {
                        Icon(Icons.Default.QrCode, null, modifier = Modifier.size(16.dp), tint = Color(0xFF00D1B2))
                        Spacer(Modifier.width(8.dp))
                        Text("QR Code", fontSize = 12.sp, color = Color(0xFF00D1B2))
                    }
                }
            }
        }

        if (friends.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No friends yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(friends) { friend ->
                    FriendItem(
                        user = friend, 
                        nickname = nicknames[friend.uid],
                        isDarkMode = isDarkMode,
                        onSetNickname = { viewModel.setNickname(friend.uid, it) },
                        onRemove = { viewModel.removeFriend(friend.uid) },
                        onBlock = { viewModel.blockUser(friend.uid) }
                    )
                }
            }
        }
    }

    if (showQRDialog) {
        AlertDialog(
            onDismissRequest = { showQRDialog = false },
            title = { Text("My QR Code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    val qrBitmap = remember { QRUtils.generateQRCode("fincalc://add-friend?id=$finCalcId") }
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp).background(Color.White).padding(8.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("FinCalc ID: $finCalcId", fontWeight = FontWeight.Bold)
                    Text("Let others scan this to add you.", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("FinCalc ID", finCalcId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "ID Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f), contentColor = if (isDarkMode) Color.White else Color.Black)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy ID", fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    val shareMessage = """
                                        Hey 👋 Add me on FinCalc!
                                        
                                        My FinCalc ID: $finCalcId
                                        
                                        Open FinCalc → Settings → Friends / Add Friends → search my ID and send a request.
                                    """.trimIndent()
                                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQRDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun FriendItem(
    user: User, 
    nickname: String?,
    isDarkMode: Boolean,
    onSetNickname: (String) -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                if (!nickname.isNullOrBlank()) {
                    Text(nickname, fontWeight = FontWeight.Bold)
                    Text("${user.name} • ${user.finCalcId}", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Text(user.name, fontWeight = FontWeight.Bold)
                    Text(user.finCalcId, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Set Nickname") },
                        onClick = { 
                            showMenu = false
                            showNicknameDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove Friend", color = Color.Red) },
                        onClick = { 
                            showMenu = false
                            showRemoveDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = Color.Red) }
                    )
                    DropdownMenuItem(
                        text = { Text("Block User", color = Color.Red) },
                        onClick = { 
                            showMenu = false
                            showBlockDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Block, null, tint = Color.Red) }
                    )
                }
            }
        }
    }

    if (showNicknameDialog) {
        var newNickname by remember { mutableStateOf(nickname ?: "") }
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Set Nickname") },
            text = {
                Column {
                    Text("Official Name: ${user.name}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    ValidatedTextField(
                        value = newNickname,
                        onValueChange = { newNickname = it },
                        label = "Nickname",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetNickname(newNickname)
                        showNicknameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showNicknameDialog = false }) { Text("Cancel") } }
        )
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove ${nickname ?: user.name} from your friends?") },
            confirmButton = {
                Button(onClick = {
                    onRemove()
                    showRemoveDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") } }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block User") },
            text = { Text("Are you sure you want to block ${nickname ?: user.name}? They will no longer be able to find you or chat with you.") },
            confirmButton = {
                Button(onClick = {
                    onBlock()
                    showBlockDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PendingRequestsTab(viewModel: FriendsViewModel, isDarkMode: Boolean) {
    val requests by viewModel.pendingRequests.collectAsState()

    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending requests.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(requests) { request ->
                PendingRequestItem(request, isDarkMode, 
                    onAccept = { viewModel.acceptRequest(request) },
                    onReject = { viewModel.rejectRequest(request) }
                )
            }
        }
    }
}

@Composable
fun PendingRequestItem(request: FriendRequest, isDarkMode: Boolean, onAccept: () -> Unit, onReject: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.fromName, fontWeight = FontWeight.Bold)
                    Text(request.fromFinCalcId, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
                }
            }
        }
    }
}

@Composable
fun SentRequestsTab(viewModel: FriendsViewModel, isDarkMode: Boolean) {
    val requests by viewModel.sentRequests.collectAsState()

    if (requests.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sent requests.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp), 
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(requests) { request ->
                SentRequestItem(request, isDarkMode)
            }
        }
    }
}

@Composable
fun SentRequestItem(request: FriendRequest, isDarkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(request.toName, fontWeight = FontWeight.Bold)
                Text(request.toFinCalcId, fontSize = 12.sp, color = Color.Gray)
            }
            Surface(
                color = Color(0xFF00D1B2).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Sent", color = Color(0xFF00D1B2), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun AddFriendTab(viewModel: FriendsViewModel, isDarkMode: Boolean, initialSearch: String? = null) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf(if (initialSearch == "{search}") "" else initialSearch ?: "") }
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // Update search query if initialSearch changes (deep link)
    LaunchedEffect(initialSearch) {
        if (initialSearch != null && initialSearch != "{search}") {
            searchQuery = initialSearch
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (initialSearch != null && initialSearch != "{search}") {
            Text("Add Friend from Link", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2), modifier = Modifier.padding(bottom = 8.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ValidatedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchUsers(it)
                },
                label = "Search by Name, Email, or ID",
                modifier = Modifier.weight(1f),
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = ""; viewModel.searchUsers("") }) { Icon(Icons.Default.Close, null) } }
                } else null
            )
        }

        Spacer(Modifier.height(8.dp))
        
        Spacer(Modifier.height(16.dp))

        if (isSearching) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00D1B2))
            }
        } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().height(150.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("No user found.", color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text("Try searching by exact email or ID.", fontSize = 12.sp, color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(searchResults) { user ->
                    SearchResultItem(user, isDarkMode, onSendRequest = { viewModel.sendFriendRequest(user) })
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(user: User, isDarkMode: Boolean, onSendRequest: () -> Unit) {
    var requestSent by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold)
                Text(user.finCalcId, fontSize = 12.sp, color = Color.Gray)
            }
            
            if (requestSent) {
                Text("Sent", color = Color(0xFF00D1B2), fontWeight = FontWeight.Bold)
            } else {
                IconButton(onClick = { 
                    onSendRequest()
                    requestSent = true
                }) {
                    Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF00D1B2))
                }
            }
        }
    }
}
