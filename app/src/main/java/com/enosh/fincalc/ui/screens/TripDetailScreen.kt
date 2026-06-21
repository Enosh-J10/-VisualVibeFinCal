package com.enosh.fincalc.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.enosh.fincalc.data.model.TravelExpense
import com.enosh.fincalc.data.model.TravelTrip
import com.enosh.fincalc.data.model.User
import com.enosh.fincalc.viewmodel.SmartTravelViewModel
import com.enosh.fincalc.ui.components.ValidatedTextField
import com.google.firebase.auth.FirebaseAuth
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

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Expenses", "Members", "Insights", "Settlement")

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<TravelExpense?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val effectiveTripId = trip?.tripId ?: tripId

    LaunchedEffect(effectiveTripId) {
        travelViewModel.fetchTrips()
        travelViewModel.fetchExpenses(effectiveTripId)
    }

    if (trip == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    CalculatorScreenScaffold(
        title = trip.name,
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
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
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ExpensesTab(
                        expenses = expenses, 
                        isDarkMode = isDarkMode, 
                        onAdd = { showAddExpenseDialog = true },
                        onEdit = { editingExpense = it },
                        onDelete = { travelViewModel.deleteExpense(effectiveTripId, it.expenseId) },
                        currentUid = currentUid,
                        isAdmin = trip.createdByUid == currentUid,
                        currency = trip.currencyCode
                    )
                    1 -> MembersTab(trip, isDarkMode, onAdd = { showAddMemberDialog = true })
                    2 -> InsightsTab(trip, expenses, isDarkMode)
                    3 -> SettlementTab(trip, expenses, onFinalize = { travelViewModel.finalizeTrip(effectiveTripId) })
                }
            }
        }
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
    expenses: List<TravelExpense>, 
    isDarkMode: Boolean, 
    onAdd: () -> Unit,
    onEdit: (TravelExpense) -> Unit,
    onDelete: (TravelExpense) -> Unit,
    currentUid: String,
    isAdmin: Boolean,
    currency: String
) {
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
                        isDarkMode = isDarkMode,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        canEdit = isAdmin || expense.paidByUid == currentUid,
                        currency = currency
                    )
                }
            }
        }

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

@Composable
fun TravelExpenseItem(
    expense: TravelExpense, 
    isDarkMode: Boolean, 
    onEdit: (TravelExpense) -> Unit, 
    onDelete: (TravelExpense) -> Unit,
    canEdit: Boolean,
    currency: String
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEdit) { showMenu = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF00D1B2), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Bold)
                Text(expense.category, fontSize = 12.sp, color = Color.Gray)
            }
            Text(
                "$currency ${String.format(Locale.getDefault(), "%.2f", expense.amount)}", 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF00D1B2)
            )

            if (canEdit) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                    }
                }
            }
        }
    }
}

@Composable
fun MembersTab(trip: TravelTrip, isDarkMode: Boolean, onAdd: () -> Unit) {
    val members = trip.memberUids
    val friendsViewModel: com.enosh.fincalc.viewmodel.FriendsViewModel = viewModel()
    val nicknames by friendsViewModel.friendNicknames.collectAsState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(members) { memberId ->
                val detail = trip.memberDetails[memberId]
                val nickname = nicknames[memberId]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
                    )
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).background(Color.Gray.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            if (!nickname.isNullOrBlank()) {
                                Text(nickname, fontWeight = FontWeight.Bold)
                                Text(detail?.name ?: "Unknown", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                Text(detail?.name ?: "Unknown", fontWeight = FontWeight.Bold)
                                Text(detail?.email ?: "", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(detail?.status ?: "", fontSize = 10.sp, color = Color(0xFF00D1B2))
                    }
                }
            }
        }

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

@Composable
fun InsightsTab(trip: TravelTrip, expenses: List<TravelExpense>, isDarkMode: Boolean) {
    val totalCost = expenses.sumOf { it.amount }
    val membersCount = trip.memberUids.size
    val perPerson = if (membersCount > 0) totalCost / membersCount else 0.0
    val currency = trip.currencyCode
    
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CalculatorCard(isDarkMode = isDarkMode) {
            Text("Trip Summary", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Cost")
                Text(String.format(Locale.getDefault(), "%.2f %s", totalCost, currency), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cost Per Person")
                Text(String.format(Locale.getDefault(), "%.2f %s", perPerson, currency), fontWeight = FontWeight.Bold)
            }
        }
        
        // Category Breakdown
        val categories = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
        CalculatorCard(isDarkMode = isDarkMode) {
            Text("Category Breakdown", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
            Spacer(Modifier.height(8.dp))
            categories.forEach { (cat, amount) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(cat)
                    Text(String.format(Locale.getDefault(), "%.2f", amount))
                }
            }
        }
    }
}

@Composable
fun SettlementTab(trip: TravelTrip, expenses: List<TravelExpense>, onFinalize: () -> Unit) {
    val travelViewModel: SmartTravelViewModel = viewModel()
    val settlements = travelViewModel.calculateSettlements(trip, expenses)

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Who Owes Whom", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
        }
        
        items(settlements) { settlement ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00D1B2).copy(alpha = 0.05f))
            ) {
                Text(settlement, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium)
            }
        }

        item {
            Spacer(Modifier.height(32.dp))
            if (trip.createdByUid == FirebaseAuth.getInstance().currentUser?.uid && !trip.isFinalized) {
                Button(
                    onClick = onFinalize,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
                ) {
                    Text("Finalize Trip & Settle")
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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    var receiptUrl by remember { mutableStateOf(expense?.receiptUrl) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val ref = storage.reference.child("receipts/${UUID.randomUUID()}.jpg")
                try {
                    ref.putFile(it).await()
                    receiptUrl = ref.downloadUrl.await().toString()
                    Toast.makeText(context, "Image uploaded!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
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
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
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
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        )
                        ValidatedTextField(
                            value = originalCurrency,
                            onValueChange = { originalCurrency = it.uppercase() },
                            label = "Currency (e.g. USD)",
                            modifier = Modifier.weight(1f),
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
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
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )

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
                            updatedAt = System.currentTimeMillis(),
                            originalAmount = if (isDifferentCurrency) originalAmount.toDoubleOrNull() ?: 0.0 else 0.0,
                            originalCurrency = if (isDifferentCurrency) originalCurrency else "",
                            receiptUrl = receiptUrl
                        ) ?: TravelExpense(
                            title = title,
                            amount = finalAmount,
                            paidByUid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                            createdAt = System.currentTimeMillis(),
                            category = selectedCategory,
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
                                    Icon(Icons.Default.Person, null, tint = Color(0xFF00D1B2))
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
                            Row(
                                Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, null)
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
