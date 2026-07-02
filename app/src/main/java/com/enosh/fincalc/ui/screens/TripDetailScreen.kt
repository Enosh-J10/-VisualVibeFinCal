package com.enosh.fincalc.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.enosh.fincalc.data.model.*
import com.enosh.fincalc.viewmodel.SmartTravelViewModel
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    navController: NavController,
    isDarkMode: Boolean,
    travelViewModel: SmartTravelViewModel = viewModel()
) {
    val trips by travelViewModel.trips.collectAsState()
    val trip = trips.find { it.tripId == tripId }
    val expenses by travelViewModel.currentTripExpenses.collectAsState()
    val searchResults by travelViewModel.searchResults.collectAsState()
    val expenseFlags by travelViewModel.expenseFlags.collectAsState()
    val memberProfiles by travelViewModel.memberProfiles.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Expenses", "Members", "Insights", "Settlement")

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<TravelExpense?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showEditTripDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val currentUid = currentUser?.uid ?: ""
    val effectiveTripId = trip?.tripId ?: tripId

    LaunchedEffect(effectiveTripId) {
        travelViewModel.fetchTrips()
        travelViewModel.fetchExpenses(effectiveTripId)
    }

    LaunchedEffect(trip?.memberUids, trip?.invitedUids) {
        val allUids = mutableListOf<String>()
        trip?.memberUids?.let { allUids.addAll(it) }
        trip?.invitedUids?.let { allUids.addAll(it) }
        if (allUids.isNotEmpty()) {
            travelViewModel.fetchMemberProfiles(allUids)
        }
    }

    if (trip == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Join logic
    LaunchedEffect(trip) {
        if (trip.invitedUids.contains(currentUid)) {
            travelViewModel.joinTrip(effectiveTripId)
        }
    }

    CalculatorScreenScaffold(
        title = trip.name,
        navController = navController,
        isDarkMode = isDarkMode,
        actions = {
            if (trip.createdByUid == currentUid) {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Trip Settings", tint = if (isDarkMode) Color.White else Color.Black)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit Trip Details") },
                            onClick = { 
                                showMenu = false
                                showEditTripDialog = true 
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Trip", color = Color.Red) },
                            onClick = { 
                                showMenu = false
                                showDeleteConfirm = true
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF00D1B2),
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF00D1B2)
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ExpensesTab(
                        trip = trip,
                        expenses = expenses, 
                        expenseFlags = expenseFlags,
                        isDarkMode = isDarkMode, 
                        onAdd = { showAddExpenseDialog = true },
                        onEdit = { editingExpense = it },
                        onDelete = { travelViewModel.deleteExpense(effectiveTripId, it.expenseId) },
                        onFlag = { exp, reason, note -> travelViewModel.flagExpense(effectiveTripId, exp.expenseId, reason, note) },
                        onResolveFlag = { expId, flagId -> travelViewModel.resolveFlag(effectiveTripId, expId, flagId) },
                        currentUid = currentUid
                    )
                    1 -> MembersTab(trip, memberProfiles, isDarkMode, onAdd = { showAddMemberDialog = true }, onRemove = { travelViewModel.removeMember(effectiveTripId, it) }, currentUid = currentUid)
                    2 -> InsightsTab(trip, expenses, isDarkMode)
                    3 -> SettlementTab(trip, expenses, memberProfiles, isDarkMode, onFinalize = { travelViewModel.finalizeTrip(effectiveTripId) }, onReopen = { travelViewModel.reopenTrip(effectiveTripId) })
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Trip?", color = if (isDarkMode) Color.White else Color.Black) },
            text = { Text("This will permanently delete this trip, expenses, members, flags, and settlement data. This cannot be undone.", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black) },
            confirmButton = {
                Button(onClick = { 
                    travelViewModel.deleteTrip(effectiveTripId) {
                        navController.popBackStack()
                    }
                    showDeleteConfirm = false
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray) }
            },
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
        )
    }

    if (showEditTripDialog) {
        EditTripDialog(
            trip = trip,
            onDismiss = { showEditTripDialog = false },
            onSave = { name, country, city, code, symbol, desc ->
                travelViewModel.updateTrip(effectiveTripId, name, country, city, code, symbol, desc)
                showEditTripDialog = false
            }
        )
    }

    if (showAddExpenseDialog || editingExpense != null) {
        AddTravelExpenseDialog(
            trip = trip,
            expense = editingExpense,
            effectiveTripId = effectiveTripId,
            onDismiss = { 
                showAddExpenseDialog = false
                editingExpense = null
            },
            onSave = { expense ->
                if (expense.expenseId.isEmpty()) {
                    travelViewModel.addExpense(effectiveTripId, expense)
                } else {
                    travelViewModel.updateExpense(effectiveTripId, expense)
                }
                showAddExpenseDialog = false
                editingExpense = null
            }
        )
    }

    if (showAddMemberDialog) {
        val friendsViewModel: com.enosh.fincalc.viewmodel.FriendsViewModel = viewModel()
        val friends by friendsViewModel.friends.collectAsState()
        
        AddMemberDialog(
            friends = friends,
            searchResults = searchResults,
            onSearch = { travelViewModel.searchUsers(it) },
            onAdd = { user ->
                travelViewModel.addMember(effectiveTripId, user.uid, user.name, user.email)
                showAddMemberDialog = false
            },
            onAddFriend = { friend ->
                travelViewModel.addMember(effectiveTripId, friend.uid, friend.name, friend.email)
                showAddMemberDialog = false
            },
            onSendFriendRequest = { user ->
                friendsViewModel.sendFriendRequest(user)
            },
            onDismiss = { showAddMemberDialog = false }
        )
    }
}

@Composable
fun ExpensesTab(
    trip: TravelTrip,
    expenses: List<TravelExpense>, 
    expenseFlags: Map<String, List<TravelExpenseFlag>>,
    isDarkMode: Boolean, 
    onAdd: () -> Unit,
    onEdit: (TravelExpense) -> Unit,
    onDelete: (TravelExpense) -> Unit,
    onFlag: (TravelExpense, String, String) -> Unit,
    onResolveFlag: (String, String) -> Unit,
    currentUid: String
) {
    val isAdmin = trip.createdByUid == currentUid

    Box(Modifier.fillMaxSize()) {
        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No expenses yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    TravelExpenseItem(
                        expense = expense, 
                        trip = trip,
                        flags = expenseFlags[expense.expenseId] ?: emptyList(),
                        isDarkMode = isDarkMode,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onFlag = onFlag,
                        onResolveFlag = onResolveFlag,
                        canEdit = isAdmin || expense.createdByUid == currentUid,
                        currentUid = currentUid
                    )
                }
            }
        }

        if (!trip.isFinalized) {
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = Color(0xFF00D1B2),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    }
}

@Composable
fun TravelExpenseItem(
    expense: TravelExpense, 
    trip: TravelTrip,
    flags: List<TravelExpenseFlag>,
    isDarkMode: Boolean, 
    onEdit: (TravelExpense) -> Unit, 
    onDelete: (TravelExpense) -> Unit,
    onFlag: (TravelExpense, String, String) -> Unit,
    onResolveFlag: (String, String) -> Unit,
    canEdit: Boolean,
    currentUid: String
) {
    var showMenu by remember { mutableStateOf(false) }
    var showFlagDialog by remember { mutableStateOf(false) }
    var showFlagsDialog by remember { mutableStateOf(false) }

    val creatorName = if (expense.createdByUid == currentUid) "Me" else trip.memberDetails[expense.createdByUid]?.name ?: "Someone"
    val openFlagsCount = flags.count { it.status == "open" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF00D1B2), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(expense.title, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                    Text(expense.category, fontSize = 12.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${trip.currencyCode} ${String.format(Locale.getDefault(), "%.2f", expense.amount)}", 
                        fontWeight = FontWeight.Bold, 
                        color = Color(0xFF00D1B2),
                        fontSize = 16.sp
                    )
                    if (openFlagsCount > 0) {
                        Surface(
                            color = Color.Red.copy(alpha = 0.1f), 
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { if (canEdit) showFlagsDialog = true }
                        ) {
                            Text(
                                if (openFlagsCount > 1) "FLAGGED ($openFlagsCount)" else "FLAGGED", 
                                color = Color.Red, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Bold, 
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = if (isDarkMode) Color.White else Color.Gray)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!trip.isFinalized) {
                            if (canEdit) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    onClick = { 
                                        showMenu = false
                                        onEdit(expense)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete", color = Color.Red) },
                                    onClick = { 
                                        showMenu = false
                                        onDelete(expense)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                )
                                if (openFlagsCount > 0) {
                                    DropdownMenuItem(
                                        text = { Text("View Issues") },
                                        onClick = { 
                                            showMenu = false
                                            showFlagsDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Flag, null, tint = Color.Red) }
                                    )
                                }
                            }
                            if (expense.createdByUid != currentUid) {
                                DropdownMenuItem(
                                    text = { Text("Flag Issue") },
                                    onClick = { 
                                        showMenu = false
                                        showFlagDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Flag, null) }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Added by $creatorName", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.width(8.dp))
                val date = Date(expense.createdAt)
                val dateStr = java.text.SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
                Text(dateStr, fontSize = 11.sp, color = Color.Gray)
            }
            if (expense.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(expense.notes, fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.DarkGray)
            }
        }
    }

    if (showFlagDialog) {
        FlagExpenseDialog(
            onDismiss = { showFlagDialog = false },
            onFlag = { reason, note ->
                onFlag(expense, reason, note)
                showFlagDialog = false
            }
        )
    }

    if (showFlagsDialog) {
        ViewFlagsDialog(
            flags = flags.filter { it.status == "open" },
            isDarkMode = isDarkMode,
            canResolve = canEdit,
            onResolve = { onResolveFlag(expense.expenseId, it) },
            onDismiss = { showFlagsDialog = false }
        )
    }
}

@Composable
fun ViewFlagsDialog(
    flags: List<TravelExpenseFlag>,
    isDarkMode: Boolean,
    canResolve: Boolean,
    onResolve: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reported Issues", color = if (isDarkMode) Color.White else Color.Black) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(flags) { flag ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Gray.copy(alpha = 0.05f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(flag.reasonType, fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 14.sp)
                                Spacer(Modifier.weight(1f))
                                if (canResolve) {
                                    TextButton(onClick = { onResolve(flag.flagId) }) {
                                        Text("Resolve", fontSize = 12.sp, color = Color(0xFF00D1B2))
                                    }
                                }
                            }
                            if (flag.note.isNotBlank()) {
                                Text(flag.note, fontSize = 13.sp, color = if (isDarkMode) Color.White else Color.Black)
                            }
                            Text("By ${flag.createdByName}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFF00D1B2)) } },
        containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
    )
}

@Composable
fun FlagExpenseDialog(onDismiss: () -> Unit, onFlag: (String, String) -> Unit) {
    var reason by remember { mutableStateOf("Wrong amount") }
    var note by remember { mutableStateOf("") }
    val reasons = listOf("Wrong amount", "Wrong payer", "Wrong split", "Duplicate expense", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Flag Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Why are you flagging this expense?", fontSize = 14.sp)
                reasons.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { reason = r }) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r, fontSize = 14.sp)
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Additional Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onFlag(reason, note) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Flag Expense", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MembersTab(trip: TravelTrip, profiles: Map<String, User>, isDarkMode: Boolean, onAdd: () -> Unit, onRemove: (String) -> Unit, currentUid: String) {
    val members = trip.memberUids
    val invited = trip.invitedUids
    val isAdmin = trip.createdByUid == currentUid

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Members", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2)) }
            items(members) { memberId ->
                MemberItem(memberId, trip, profiles[memberId], isDarkMode, isAdmin, onRemove, currentUid, isInvited = false)
            }
            if (invited.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)); Text("Invited", fontWeight = FontWeight.Bold, color = Color.Gray) }
                items(invited) { memberId ->
                    MemberItem(memberId, trip, profiles[memberId], isDarkMode, isAdmin, onRemove, currentUid, isInvited = true)
                }
            }
        }

        if (isAdmin && !trip.isFinalized) {
            FloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = Color(0xFF00D1B2),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
            }
        }
    }
}

@Composable
fun MemberItem(memberId: String, trip: TravelTrip, user: User?, isDarkMode: Boolean, isAdmin: Boolean, onRemove: (String) -> Unit, currentUid: String, isInvited: Boolean) {
    val isMe = memberId == currentUid
    val isCreator = memberId == trip.createdByUid
    
    val photoUrl = user?.profilePictureUrl ?: user?.profilePic
    val displayName = user?.name ?: trip.memberDetails[memberId]?.name ?: "Unknown"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(40.dp).background(Color.Gray.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(displayName, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(user?.email ?: trip.memberDetails[memberId]?.email ?: "", fontSize = 12.sp, color = Color.Gray)
            }
            
            if (isMe) {
                Surface(color = Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text("ME", color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
                Spacer(Modifier.width(4.dp))
            }
            if (isCreator) {
                Surface(color = Color(0xFF00D1B2).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text("CREATOR", color = Color(0xFF00D1B2), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            } else if (isInvited) {
                Surface(color = Color(0xFFFFA500).copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text("INVITED", color = Color(0xFFFFA500), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            } else {
                Surface(color = Color.Blue.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                    Text("JOINED", color = Color.Blue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
            
            if (isAdmin && !isCreator && !trip.isFinalized) {
                IconButton(onClick = { onRemove(memberId) }) {
                    Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun InsightsTab(trip: TravelTrip, expenses: List<TravelExpense>, isDarkMode: Boolean) {
    val totalCost = expenses.sumOf { it.amount }
    val membersCount = trip.memberUids.size
    val perPerson = if (membersCount > 0) totalCost / membersCount else 0.0
    val currency = trip.currencyCode
    
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CalculatorCard(isDarkMode = isDarkMode) {
            Text("Trip Summary", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Cost", color = if (isDarkMode) Color.White else Color.Black)
                Text(String.format(Locale.getDefault(), "%.2f %s", totalCost, currency), fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cost Per Person", color = if (isDarkMode) Color.White else Color.Black)
                Text(String.format(Locale.getDefault(), "%.2f %s", perPerson, currency), fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
            }
        }
        
        // Category Breakdown
        val categories = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
        CalculatorCard(isDarkMode = isDarkMode) {
            Text("Category Breakdown", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
            Spacer(Modifier.height(8.dp))
            categories.forEach { (cat, amount) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat, color = if (isDarkMode) Color.White else Color.Black)
                    Text(String.format(Locale.getDefault(), "%.2f", amount), color = if (isDarkMode) Color.White else Color.Black)
                }
            }
        }
    }
}

@Composable
fun SettlementTab(trip: TravelTrip, expenses: List<TravelExpense>, profiles: Map<String, User>, isDarkMode: Boolean, onFinalize: () -> Unit, onReopen: () -> Unit) {
    val travelViewModel: SmartTravelViewModel = viewModel()
    val settlements = travelViewModel.calculateSettlements(trip, expenses, profiles)
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    val isCreator = trip.createdByUid == currentUid
    val context = LocalContext.current

    var showConfirmFinalize by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Settlement", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF00D1B2))
            Text("Calculation based on all trip expenses", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
        }
        
        items(settlements) { settlement ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00D1B2).copy(alpha = 0.05f))
            ) {
                Text(settlement, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium, color = if (isDarkMode) Color.White else Color.Black)
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            if (isCreator) {
                if (!trip.isFinalized) {
                    Button(
                        onClick = {
                            if (expenses.isEmpty()) {
                                Toast.makeText(context, "Add expenses before finalizing.", Toast.LENGTH_SHORT).show()
                            } else {
                                showConfirmFinalize = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                    ) {
                        Text("Finalize Trip & Settle", color = Color.White)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = Color.Green.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("This trip is finalized.", color = Color.Green, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onReopen) {
                            Text("Reopen Trip", color = Color.Gray)
                        }
                    }
                }
            } else if (trip.isFinalized) {
                Surface(
                    color = Color.Green.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("This trip is finalized.", color = Color.Green, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showConfirmFinalize) {
        AlertDialog(
            onDismissRequest = { showConfirmFinalize = false },
            title = { Text("Finalize Trip?", color = if (isDarkMode) Color.White else Color.Black) },
            text = { Text("This will mark the trip as completed and calculate final settlements. You can still reopen it later if needed.", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black) },
            confirmButton = {
                Button(onClick = { 
                    onFinalize()
                    showConfirmFinalize = false
                    Toast.makeText(context, "Trip finalized.", Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))) {
                    Text("Finalize", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmFinalize = false }) { Text("Cancel", color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray) }
            },
            containerColor = if (isDarkMode) Color(0xFF1B2C33) else Color.White
        )
    }
}

@Composable
fun EditTripDialog(
    trip: TravelTrip,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(trip.name) }
    var country by remember { mutableStateOf(trip.destinationCountry) }
    var city by remember { mutableStateOf(trip.destinationCity) }
    var code by remember { mutableStateOf(trip.currencyCode) }
    var symbol by remember { mutableStateOf(trip.currencySymbol) }
    var desc by remember { mutableStateOf(trip.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Trip Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                ValidatedTextField(value = name, onValueChange = { name = it }, label = "Trip Name", keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words)
                ValidatedTextField(value = country, onValueChange = { country = it }, label = "Country", keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words)
                ValidatedTextField(value = city, onValueChange = { city = it }, label = "City", keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Words)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValidatedTextField(value = code, onValueChange = { code = it.uppercase() }, label = "Currency Code", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Characters)
                    ValidatedTextField(value = symbol, onValueChange = { symbol = it }, label = "Symbol", modifier = Modifier.weight(0.5f), keyboardType = KeyboardType.Text)
                }
                ValidatedTextField(value = desc, onValueChange = { desc = it }, label = "Description", keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, country, city, code, symbol, desc) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))) {
                Text("Save Changes", color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddTravelExpenseDialog(
    trip: TravelTrip, 
    expense: TravelExpense? = null,
    effectiveTripId: String,
    onDismiss: () -> Unit, 
    onSave: (TravelExpense) -> Unit
) {
    var title by remember { mutableStateOf(expense?.title ?: "") }
    var amount by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(expense?.category ?: "Food") }
    val categories = listOf("Food", "Fuel", "Hotel", "Tickets", "Shopping", "Transport", "Activities", "Other")
    
    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    var isDifferentCurrency by remember { mutableStateOf(expense?.originalCurrency?.isNotEmpty() == true) }
    var originalAmount by remember { mutableStateOf(expense?.originalAmount?.toString() ?: "") }
    var originalCurrency by remember { mutableStateOf(expense?.originalCurrency ?: "") }
    var notes by remember { mutableStateOf(expense?.notes ?: "") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var receiptUrl by remember { mutableStateOf(expense?.receiptUrl) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                try {
                    val safeFileName = "receipt_${System.currentTimeMillis()}.jpg"
                    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                    val path = "trip_uploads/$effectiveTripId/receipts/${expense?.expenseId ?: "new"}/$safeFileName"
                    val ref = storage.reference.child(path)
                    
                    val stream = context.contentResolver.openInputStream(it) ?: throw Exception("Failed to open stream")
                    ref.putStream(stream).await()
                    receiptUrl = ref.downloadUrl.await().toString()
                    Toast.makeText(context, "Receipt uploaded!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("SmartTravel", "Upload failed", e)
                    Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (expense == null) "Add Expense" else "Edit Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                ValidatedTextField(
                    value = title, 
                    onValueChange = { 
                        title = it
                        titleError = if (it.isBlank()) "Expense title is required." else null
                    }, 
                    label = "Title", 
                    modifier = Modifier.fillMaxWidth(),
                    error = titleError,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Paid in different currency?", fontSize = 12.sp, color = Color.Gray)
                    Checkbox(checked = isDifferentCurrency, onCheckedChange = { isDifferentCurrency = it })
                }

                if (isDifferentCurrency) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ValidatedTextField(
                            value = originalAmount,
                            onValueChange = { originalAmount = it },
                            label = "Amount",
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Decimal
                        )
                        ValidatedTextField(
                            value = originalCurrency,
                            onValueChange = { originalCurrency = it.uppercase() },
                            label = "Currency (e.g. USD)",
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Characters
                        )
                    }
                    Text("Enter final amount in ${trip.currencyCode} below:", fontSize = 11.sp, color = Color(0xFF00D1B2))
                }

                ValidatedTextField(
                    value = amount, 
                    onValueChange = { input ->
                        val filtered = com.enosh.fincalc.utils.ValidationUtils.formatNumericInput(input)
                        amount = filtered
                        val value = filtered.toDoubleOrNull()
                        amountError = when {
                            filtered.isBlank() -> "Please enter a valid amount."
                            value == null -> "Please enter a valid amount."
                            value <= 0 -> "Amount must be greater than 0."
                            else -> null
                        }
                    }, 
                    label = "Final Amount (${trip.currencyCode})", 
                    modifier = Modifier.fillMaxWidth(),
                    error = amountError,
                    keyboardType = KeyboardType.Decimal
                )

                ValidatedTextField(value = notes, onValueChange = { notes = it }, label = "Note (Optional)", keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.Sentences)

                Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00D1B2).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00D1B2)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                
                if (receiptUrl != null) {
                    AsyncImage(model = receiptUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    TextButton(onClick = { receiptUrl = null }) { Text("Remove Image", color = Color.Red) }
                } else {
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isUploading) {
                        if (isUploading) CircularProgressIndicator(Modifier.size(20.dp))
                        else {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Attach Receipt / Image")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val finalAmount = amount.toDoubleOrNull() ?: 0.0
                    onSave(
                        expense?.copy(
                            title = title,
                            amount = finalAmount,
                            category = selectedCategory,
                            notes = notes,
                            updatedAt = System.currentTimeMillis(),
                            originalAmount = if (isDifferentCurrency) originalAmount.toDoubleOrNull() ?: 0.0 else 0.0,
                            originalCurrency = if (isDifferentCurrency) originalCurrency else "",
                            receiptUrl = receiptUrl
                        ) ?: TravelExpense(
                            title = title,
                            amount = finalAmount,
                            paidByUid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            createdByUid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            createdAt = System.currentTimeMillis(),
                            category = selectedCategory,
                            notes = notes,
                            tripId = effectiveTripId,
                            currencyCode = trip.currencyCode,
                            currencySymbol = trip.currencySymbol,
                            originalAmount = if (isDifferentCurrency) originalAmount.toDoubleOrNull() ?: 0.0 else 0.0,
                            originalCurrency = if (isDifferentCurrency) originalCurrency else "",
                            receiptUrl = receiptUrl
                        )
                    )
                },
                enabled = title.isNotBlank() && amount.isNotBlank() && titleError == null && amountError == null && !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
            ) { Text(if (expense == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddMemberDialog(
    friends: List<User>,
    searchResults: List<User>,
    onSearch: (String) -> Unit,
    onAdd: (User) -> Unit,
    onAddFriend: (User) -> Unit,
    onSendFriendRequest: (User) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF00D1B2)
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Friends", fontSize = 12.sp) })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Search", fontSize = 12.sp) })
                }
                
                Spacer(Modifier.height(8.dp))

                if (selectedTab == 0) {
                    if (friends.isEmpty()) {
                        Box(Modifier.height(200.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No friends found.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(Modifier.height(200.dp)) {
                            items(friends) { friend ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { onAddFriend(friend) }.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    val photoUrl = friend.profilePictureUrl ?: friend.profilePic
                                    if (!photoUrl.isNullOrBlank()) {
                                        AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(friend.name, fontWeight = FontWeight.Bold)
                                        Text(friend.finCalcId, fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = query, 
                        onValueChange = { 
                            query = it
                            onSearch(it)
                        }, 
                        label = { Text("Search FinCalc Users") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    LazyColumn(Modifier.height(200.dp)) {
                        items(searchResults) { user ->
                            val isFriend = friends.any { it.uid == user.uid }
                            val photoUrl = user.profilePictureUrl ?: user.profilePic
                            Row(
                                Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!photoUrl.isNullOrBlank()) {
                                    AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, null)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(user.name, fontWeight = FontWeight.Bold)
                                    Text(user.email, fontSize = 12.sp, color = Color.Gray)
                                }
                                
                                if (isFriend) {
                                    IconButton(onClick = { onAdd(user) }) {
                                        Icon(Icons.Default.Add, null, tint = Color(0xFF00D1B2))
                                    }
                                } else {
                                    IconButton(onClick = { onSendFriendRequest(user) }) {
                                        Icon(Icons.Default.PersonAdd, null, tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Text("You can only add accepted friends to trips.", fontSize = 10.sp, color = Color.Gray)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
