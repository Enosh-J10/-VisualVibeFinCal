package com.enosh.fincalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Search
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
import com.enosh.fincalc.data.model.TravelTrip
import com.enosh.fincalc.ui.navigation.Screen
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.SmartTravelViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTravelScreen(
    navController: NavController,
    isDarkMode: Boolean,
    assistantViewModel: AssistantViewModel,
    travelViewModel: SmartTravelViewModel = viewModel()
) {
    val trips by travelViewModel.trips.collectAsState()
    val errorMessage by travelViewModel.errorMessage.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            travelViewModel.clearError()
        }
    }
    val sharedPref = remember { context.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE) }
    var showOnboarding by remember { mutableStateOf(!sharedPref.getBoolean("travel_onboarding_complete", false)) }

    LaunchedEffect(Unit) {
        travelViewModel.fetchTrips()
    }

    if (showOnboarding) {
        AlertDialog(
            onDismissRequest = { 
                showOnboarding = false 
                sharedPref.edit().putBoolean("travel_onboarding_complete", true).apply()
            },
            title = { Text("Smart Travel Guide", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Create a trip.")
                    Text("2. Add friends.")
                    Text("3. Add shared expenses.")
                    Text("4. View who owes whom.")
                    Text("5. Export settlement summary.")
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showOnboarding = false 
                    sharedPref.edit().putBoolean("travel_onboarding_complete", true).apply()
                }) { Text("Got it!") }
            }
        )
    }

    CalculatorScreenScaffold(
        title = "Smart Travel",
        navController = navController,
        isDarkMode = isDarkMode
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (trips.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No trips yet. Create one to start!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trips) { trip ->
                        TripItem(trip, isDarkMode) {
                            navController.navigate(Screen.TripDetail.createRoute(trip.tripId))
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = Color(0xFF00D1B2),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Trip")
            }
        }
    }

    if (showCreateDialog) {
        CreateTripDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showCreateDialog = false },
            onCreate = { name, country, city, code, symbol ->
                travelViewModel.createTrip(name, country, city, code, symbol)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun TripItem(trip: TravelTrip, isDarkMode: Boolean, onClick: () -> Unit) {
    val tripName = trip.name.ifEmpty { "Unnamed Trip" }
    val destination = "${trip.destinationCountry}${if (trip.destinationCity.isNotEmpty()) " - ${trip.destinationCity}" else ""}"
    
    val dateRange = "Shared Trip"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(Color(0xFF00D1B2).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CardTravel, contentDescription = null, tint = Color(0xFF00D1B2))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(tripName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(destination, fontSize = 14.sp, color = Color.Gray)
                Text(dateRange, fontSize = 12.sp, color = Color.Gray)
            }
            if (trip.isFinalized) {
                Surface(
                    color = Color.Green.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Settled", color = Color.Green, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun CreateTripDialog(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(com.enosh.fincalc.utils.CurrencyUtils.SUPPORTED_CURRENCIES[0]) }
    var destinationCity by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    var countryExpanded by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(countrySearchQuery) {
        if (countrySearchQuery.isBlank()) {
            com.enosh.fincalc.utils.CurrencyUtils.SUPPORTED_CURRENCIES
        } else {
            com.enosh.fincalc.utils.CurrencyUtils.SUPPORTED_CURRENCIES.filter {
                it.country.contains(countrySearchQuery, ignoreCase = true) ||
                it.code.contains(countrySearchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Trip", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Trip Name") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words
                    )
                )
                
                // Country Dropdown
                Box {
                    OutlinedTextField(
                        value = "${selectedCountry.flag} ${selectedCountry.country}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Country") },
                        modifier = Modifier.fillMaxWidth().clickable { countryExpanded = true },
                        trailingIcon = { IconButton(onClick = { countryExpanded = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f).heightIn(max = 300.dp)
                    ) {
                        OutlinedTextField(
                            value = countrySearchQuery,
                            onValueChange = { countrySearchQuery = it },
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            placeholder = { Text("Search country...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )
                        filteredCountries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text("${country.flag} ${country.country}") },
                                onClick = {
                                    selectedCountry = country
                                    countryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(value = destinationCity, onValueChange = { destinationCity = it }, label = { Text("City / Place (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00D1B2).copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Currency: ${selectedCountry.code} (${selectedCountry.symbol})", fontWeight = FontWeight.Bold, color = Color(0xFF00D1B2))
                    }
                }

                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, "${selectedCountry.flag} ${selectedCountry.country}", destinationCity, selectedCountry.code, selectedCountry.symbol) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D1B2))
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
