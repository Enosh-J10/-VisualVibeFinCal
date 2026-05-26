package com.enosh.fincalc.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enosh.fincalc.R
import com.enosh.fincalc.domain.model.Category
import com.enosh.fincalc.domain.model.Tool
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.AssistantMessageType
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.utils.ReminderWorker
import androidx.work.*
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

import com.enosh.fincalc.viewmodel.FinancialViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkMode: Boolean, 
    onLogout: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    assistantViewModel: AssistantViewModel,
    financialViewModel: FinancialViewModel = viewModel()
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE) }
    
    LaunchedEffect(Unit) {
        val workManager = WorkManager.getInstance(context)
        val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .addTag("daily_reminder")
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }

    val isGuest = remember { sharedPref.getBoolean("is_guest", false) }

    val expenses by financialViewModel.allExpenses.collectAsState()
    val currentMonth = financialViewModel.getCurrentMonth()
    val budget by financialViewModel.getBudgetForMonth(currentMonth).collectAsState(initial = null)

    val allTools = remember {
        listOf(
            Tool("curr", "Currency", R.drawable.ic_currency),
            Tool("loan", "Loan Calculator", R.drawable.ic_loan),
            Tool("tip", "Tip & Split Calculator", R.drawable.ic_tip),
            Tool("tax", "Tax & Disc", R.drawable.ic_tax),
            Tool("perc", "Percentage", R.drawable.ic_percent),
            Tool("unit", "Unit Conversion", R.drawable.ic_unit),
            Tool("date", "Date/Time", R.drawable.ic_date),
            Tool("bmi", "BMI Calculator", R.drawable.ic_bmi),
            Tool("calc", "Calculator", R.drawable.ic_calc),
            Tool("salary", "Salary Calculator", R.drawable.ic_salary),
            Tool("notes", "Note Book", R.drawable.ic_calc),
            Tool("smart_scan", "Smart Scan", R.drawable.ic_calc),
            Tool("insights", "Insights", R.drawable.ic_calc),
            Tool("budget", "Budget Planner", R.drawable.ic_calc),
            Tool("goals", "Savings Goals", R.drawable.ic_calc)
        )
    }

    val favoritesState = remember {
        val initialIds = sharedPref.getString("favorite_tools", "") ?: ""
        val initialList = initialIds.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { id -> allTools.find { it.id == id } }
        mutableStateOf(initialList)
    }
    val favoriteTools = favoritesState.value

    val recentToolsState = remember {
        val initialIds = sharedPref.getString("recent_tools", "") ?: ""
        val initialList = initialIds.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { id -> allTools.find { it.id == id } }
        mutableStateOf(initialList)
    }
    val recentTools = recentToolsState.value

    val handleNavigateToTool: (String) -> Unit = { toolId ->
        val currentRecent = sharedPref.getString("recent_tools", "") ?: ""
        val recentList = currentRecent.split(",").filter { it.isNotBlank() }.toMutableList()
        recentList.remove(toolId)
        recentList.add(0, toolId)
        val updatedRecent = recentList.take(5).joinToString(",")
        sharedPref.edit { putString("recent_tools", updatedRecent) }
        
        recentToolsState.value = updatedRecent.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { id -> allTools.find { it.id == id } }
            
        onNavigateToTool(toolId)
    }

    val toggleFavorite: (String) -> Unit = { toolId ->
        val currentFavorites = sharedPref.getString("favorite_tools", "") ?: ""
        val favList = currentFavorites.split(",").filter { it.isNotBlank() }.toMutableList()
        if (favList.contains(toolId)) {
            favList.remove(toolId)
        } else {
            favList.add(toolId)
        }
        val updatedFavorites = favList.joinToString(",")
        sharedPref.edit { putString("favorite_tools", updatedFavorites) }
        
        favoritesState.value = updatedFavorites.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { id -> allTools.find { it.id == id } }
    }

    val categories = listOf(
        Category("Insights & Planning", allTools.filter { it.id in listOf("insights", "budget", "goals") }, if (isDarkMode) Color(0xFF1B2C33) else Color(0xFFE3F2FD)),
        Category("Finance Basics", allTools.filter { it.id in listOf("curr", "loan", "tip") }, if (isDarkMode) Color(0xFF1B2C33) else Color(0xFFF0F4F8)),
        Category("Advanced", allTools.filter { it.id in listOf("tax", "perc", "smart_scan") }, if (isDarkMode) Color(0xFF1E322E) else Color(0xFFE8F5E9)),
        Category("Personal", allTools.filter { it.id in listOf("unit", "date", "bmi", "calc", "salary", "notes") }, if (isDarkMode) Color(0xFF2E1B33) else Color(0xFFF3E5F5))
    )

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(refreshTrigger) {
        visible = false
        delay(100)
        visible = true
    }

    LaunchedEffect(Unit) {
        delay(1000)
        assistantViewModel.triggerWave()
        val suggestions = financialViewModel.getSmartSuggestions(expenses, budget)
        if (suggestions.isNotEmpty()) {
            assistantViewModel.showMessage(suggestions.random(), AssistantState.HAPPY)
        } else {
            assistantViewModel.showMessage("Welcome back! Ready to save some money? 💰")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.fincalc), fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black) },
                navigationIcon = {
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.logout), color = Color(0xFF00D1B2))
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (isGuest) {
                            Toast.makeText(context, "Guest users don't have settings", Toast.LENGTH_LONG).show()
                        } else {
                            handleNavigateToTool("settings")
                        }
                    }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkMode) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f))
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp)
            ) {
                if (favoriteTools.isNotEmpty()) {
                    Text(
                        "Favorites",
                        color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(favoriteTools, key = { "fav_${it.id}" }) { tool ->
                            RecentToolChip(tool, isDarkMode) {
                                handleNavigateToTool(tool.id)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    stringResource(R.string.recent_tools),
                    color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentTools, key = { it.id }) { tool ->
                        RecentToolChip(tool, isDarkMode) {
                            handleNavigateToTool(tool.id)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        key(refreshTrigger) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDarkMode) {
                            Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF0F4F8)))
                        }
                    )
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(Modifier.fillMaxWidth().weight(1f)) {
                            CategoryCard(categories[0], Modifier.weight(1f).fillMaxHeight(), visible, 0, isDarkMode, handleNavigateToTool, favoriteTools, toggleFavorite)
                            CategoryCard(categories[1], Modifier.weight(1f).fillMaxHeight(), visible, 1, isDarkMode, handleNavigateToTool, favoriteTools, toggleFavorite)
                        }
                        Row(Modifier.fillMaxWidth().weight(1f)) {
                            CategoryCard(categories[2], Modifier.weight(1f).fillMaxHeight(), visible, 2, isDarkMode, handleNavigateToTool, favoriteTools, toggleFavorite)
                            CategoryCard(categories[3], Modifier.weight(1f).fillMaxHeight(), visible, 3, isDarkMode, handleNavigateToTool, favoriteTools, toggleFavorite)
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 20.dp), contentAlignment = Alignment.Center) {
                    PremiumFab(visible = visible, onClick = { 
                        refreshTrigger++ 
                        assistantViewModel.showMessage("Refreshing the page for you! ⚡", state = AssistantState.HAPPY, type = AssistantMessageType.THOUGHT)
                        Toast.makeText(context, "Page Refreshed", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.semantics {
                        contentDescription = "Refresh the home screen tools"
                    })
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category, 
    modifier: Modifier, 
    visible: Boolean, 
    index: Int, 
    isDarkMode: Boolean,
    onNavigateToTool: (String) -> Unit,
    favoriteTools: List<Tool>,
    onToggleFavorite: (String) -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = index * 100), label = ""
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 50f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = ""
    )

    Card(
        modifier = modifier
            .padding(4.dp)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = category.color.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                category.title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F2027)
            )
            Spacer(Modifier.height(8.dp))
            category.tools.forEach { tool ->
                ToolItem(
                    tool = tool, 
                    isDarkMode = isDarkMode, 
                    isFavorite = favoriteTools.any { it.id == tool.id },
                    onToggleFavorite = { onToggleFavorite(tool.id) },
                    onClick = { onNavigateToTool(tool.id) }
                )
            }
        }
    }
}

@Composable
fun ToolItem(
    tool: Tool, 
    isDarkMode: Boolean, 
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Icon(
            painter = painterResource(tool.iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color(0xFF00D1B2)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            tool.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onToggleFavorite() },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Toggle Favorite",
                tint = if (isFavorite) Color.Red else Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PremiumFab(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = ""
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(
        modifier = modifier
            .scale(scale * pulseScale)
            .size(60.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(Color(0xFF00D1B2), Color(0xFF00BFA5)))
            )
            .clickable { onClick() }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fincalc_logo_vector),
            contentDescription = "Refresh",
            tint = Color.Unspecified,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun RecentToolChip(tool: Tool, isDarkMode: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(tool.iconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF00D1B2)
            )
            Spacer(Modifier.width(6.dp))
            Text(tool.name, color = if (isDarkMode) Color.White else Color.Black, fontSize = 12.sp)
        }
    }
}
