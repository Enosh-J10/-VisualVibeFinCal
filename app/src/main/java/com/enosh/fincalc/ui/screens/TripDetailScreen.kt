package com.enosh.fincalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.enosh.fincalc.data.model.TravelExpense
import com.enosh.fincalc.data.model.TravelTrip
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.SmartTravelViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    travelViewModel: SmartTravelViewModel = viewModel()
) {
    val trips by travelViewModel.trips.collectAsState()
    val trip = trips.find { it.id == tripId }
    val expenses by travelViewModel.currentTripExpenses.collectAsState()
    val searchResults by travelViewModel.searchResults.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Expenses", "Members", "Insights", "Settlement")

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tripId) {
        travelViewModel.fetchTrips()
        travelViewModel.fetchExpenses(tripId)
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
                    0 -> ExpensesTab(expenses, isDarkMode, onAdd = { showAddExpenseDialog = true })
                    1 -> MembersTab(trip, isDarkMode, onAdd = { showAddMemberDialog = true })
                    2 -> InsightsTab(trip, expenses, isDarkMode)
                    3 -> SettlementTab(trip, expenses, isDarkMode, onFinalize = { travelViewModel.finalizeTrip(trip.id) })
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddTravelExpenseDialog(
            trip = trip,
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { expense ->
                travelViewModel.addExpense(trip.id, expense)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddMemberDialog) {
        AddMemberDialog(
            searchResults = searchResults,
            onSearch = { travelViewModel.searchUsers(it) },
            onAdd = { user ->
                travelViewModel.addMember(trip.id, user["uid"] as String, user["name"] as String, user["email"] as String)
                showAddMemberDialog = false
            },
            onDismiss = { showAddMemberDialog = false }
        )
    }
}

@Composable
fun ExpensesTab(expenses: List<TravelExpense>, isDarkMode: Boolean, onAdd: () -> Unit) {
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
                    TravelExpenseItem(expense, isDarkMode)
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
fun TravelExpenseItem(expense: TravelExpense, isDarkMode: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                String.format("%.2f", expense.amount), 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF00D1B2)
            )
        }
    }
}

@Composable
fun MembersTab(trip: TravelTrip, isDarkMode: Boolean, onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(trip.members) { memberId ->
                val detail = trip.memberDetails[memberId]
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
                            Text(detail?.name ?: "Unknown", fontWeight = FontWeight.Bold)
                            Text(detail?.email ?: "", fontSize = 12.sp, color = Color.Gray)
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
    val perPerson = if (trip.members.isNotEmpty()) totalCost / trip.members.size else 0.0
    
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CalculatorCard(isDarkMode = isDarkMode) {
            Text("Trip Summary", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Cost")
                Text(String.format("%.2f %s", totalCost, trip.currency), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cost Per Person")
                Text(String.format("%.2f %s", perPerson, trip.currency), fontWeight = FontWeight.Bold)
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
                    Text(String.format("%.2f", amount))
                }
            }
        }
    }
}

@Composable
fun SettlementTab(trip: TravelTrip, expenses: List<TravelExpense>, isDarkMode: Boolean, onFinalize: () -> Unit) {
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
            if (trip.adminId == FirebaseAuth.getInstance().currentUser?.uid && !trip.isFinalized) {
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
fun AddTravelExpenseDialog(trip: TravelTrip, onDismiss: () -> Unit, onAdd: (TravelExpense) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    val categories = listOf("Food", "Fuel", "Hotel", "Tickets", "Shopping", "Transport", "Activities", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                // Category simplified for now
                Text("Category", fontSize = 12.sp, color = Color.Gray)
                LazyColumn(Modifier.height(100.dp)) {
                    items(categories) { cat ->
                        Row(Modifier.fillMaxWidth().clickable { selectedCategory = cat }.padding(4.dp)) {
                            RadioButton(selected = selectedCategory == cat, onClick = { selectedCategory = cat })
                            Text(cat)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onAdd(TravelExpense(
                        title = title,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        paidBy = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        date = System.currentTimeMillis(),
                        category = selectedCategory,
                        tripId = trip.id
                    ))
                },
                enabled = title.isNotBlank() && amount.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddMemberDialog(
    searchResults: List<Map<String, Any>>,
    onSearch: (String) -> Unit,
    onAdd: (Map<String, Any>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, 
                    onValueChange = { 
                        query = it
                        onSearch(it)
                    }, 
                    label = { Text("Search by name or email") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                LazyColumn(Modifier.height(200.dp)) {
                    items(searchResults) { user ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onAdd(user) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(user["name"] as String, fontWeight = FontWeight.Bold)
                                Text(user["email"] as String, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
