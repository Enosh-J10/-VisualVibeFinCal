package com.enosh.fincalc.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enosh.fincalc.R
import com.enosh.fincalc.domain.model.Category
import com.enosh.fincalc.domain.model.Tool
import com.enosh.fincalc.utils.UserUtils
import com.enosh.fincalc.viewmodel.AssistantMessageType
import com.enosh.fincalc.viewmodel.AssistantState
import com.enosh.fincalc.viewmodel.AssistantViewModel
import com.enosh.fincalc.viewmodel.FinancialViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onNavigateToChat: () -> Unit,
    onNavigateToTool: (String) -> Unit,
    assistantViewModel: AssistantViewModel,
    financialViewModel: FinancialViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences(UserUtils.PREFS_NAME, Context.MODE_PRIVATE) }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "guest" }

    val recentToolsKey = remember(uid) { UserUtils.getScopedKey(uid, "recent_tools") }
    val favoriteToolsKey = remember(uid) { UserUtils.getScopedKey(uid, "favorite_tools") }

    val isGuest = remember { sharedPref.getBoolean("is_guest", false) }

    val expenses by financialViewModel.allExpenses.collectAsState()
    val currentMonth = financialViewModel.getCurrentMonth()
    val budget by financialViewModel.getBudgetForMonth(currentMonth).collectAsState(initial = null)

    val allTools = remember(isGuest) {
        listOf(
            Tool("ai_chat", "FinCalc AI", R.drawable.ic_calc),
            Tool("curr", "Currency", R.drawable.ic_currency),
            Tool("loan", "Loan Calculator", R.drawable.ic_loan),
            Tool("tip", "Tip & Split", R.drawable.ic_tip),
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
            Tool("goals", "Savings Goals", R.drawable.ic_calc),
            Tool("saving_planner", "Auto Planner", R.drawable.ic_salary),
            Tool("smart_travel", "Smart Travel", R.drawable.ic_tip),
            Tool("smart_business", "Smart Business", R.drawable.ic_tax)
        ).filter {
            if (isGuest) it.id !in listOf("smart_travel", "friends", "ai_chat") else true
        }
    }

    val favoritesState = remember(favoriteToolsKey) {
        val initialIds = sharedPref.getString(favoriteToolsKey, "") ?: ""
        val initialList = initialIds.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { id -> allTools.find { it.id == id } }
        mutableStateOf(initialList)
    }
    val favoriteTools = favoritesState.value

    val recentToolsState = remember(recentToolsKey) {
        val initialIds = sharedPref.getString(recentToolsKey, "") ?: ""
        val initialList = initialIds.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { id -> allTools.find { it.id == id } }
        mutableStateOf(initialList)
    }
    val recentTools = recentToolsState.value

    val handleNavigateToTool: (String) -> Unit = { toolId ->
        val currentRecent = sharedPref.getString(recentToolsKey, "") ?: ""
        val recentList = currentRecent.split(",").filter { it.isNotBlank() }.toMutableList()
        recentList.remove(toolId)
        recentList.add(0, toolId)
        val updatedRecent = recentList.take(5).joinToString(",")
        sharedPref.edit { putString(recentToolsKey, updatedRecent) }

        recentToolsState.value = updatedRecent.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { id -> allTools.find { it.id == id } }

        onNavigateToTool(toolId)
    }

    val toggleFavorite: (String) -> Unit = { toolId ->
        val currentFavorites = sharedPref.getString(favoriteToolsKey, "") ?: ""
        val favList = currentFavorites.split(",").filter { it.isNotBlank() }.toMutableList()
        if (favList.contains(toolId)) {
            favList.remove(toolId)
        } else {
            favList.add(toolId)
        }
        val updatedFavorites = favList.joinToString(",")
        sharedPref.edit { putString(favoriteToolsKey, updatedFavorites) }

        favoritesState.value = updatedFavorites.split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { id -> allTools.find { it.id == id } }
    }

    val userName = remember(isGuest) {
        if (isGuest) "Guest"
        else sharedPref.getString("name", "User") ?: "User"
    }
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredTools = remember(searchQuery) {
        if (searchQuery.isBlank()) allTools
        else allTools.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    (it.id == "smart_travel" && listOf("travel", "trip", "split", "group", "expense", "settlement").any { tag -> tag.contains(searchQuery, ignoreCase = true) })
        }
    }

    val categories = remember(searchQuery, isDarkMode) {
        listOf(
            Category("Insights & Planning", filteredTools.filter { it.id in listOf("ai_chat", "insights", "budget", "goals", "saving_planner") }, if (isDarkMode) Color(0xFF1B2C33) else Color(0xFFE3F2FD)),
            Category("Finance Basics", filteredTools.filter { it.id in listOf("curr", "loan", "tip") }, if (isDarkMode) Color(0xFF1B2C33) else Color(0xFFF0F4F8)),
            Category("Advanced", filteredTools.filter { it.id in listOf("tax", "perc", "smart_scan", "smart_travel", "smart_business") }, if (isDarkMode) Color(0xFF1E322E) else Color(0xFFE8F5E9)),
            Category("Personal", filteredTools.filter { it.id in listOf("unit", "date", "bmi", "calc", "salary", "notes") }, if (isDarkMode) Color(0xFF2E1B33) else Color(0xFFF3E5F5))
        ).filter { it.tools.isNotEmpty() }
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    var lastRefreshTime by remember { mutableLongStateOf(0L) }

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
            Column(modifier = Modifier.background(if (isDarkMode) Color(0xFF0F2027) else Color.White)) {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.fincalc), fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (isDarkMode) Color.White else Color.Black) },
                    navigationIcon = {
                        if (isGuest) {
                            IconButton(onClick = onLogout) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFF00D1B2))
                            }
                        } else {
                            IconButton(onClick = onNavigateToChat) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color(0xFF00D1B2))
                            }
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
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = if (isDarkMode) Color.White else Color.Black)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    windowInsets = WindowInsets.statusBars
                )

                // Header Content
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(
                        "$greeting, $userName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Text(
                        "Let's get on track for today.",
                        fontSize = 13.sp,
                        color = Color.Gray.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    // Modern Search Bar
                    Surface(
                        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Search tools...", color = Color.Gray, fontSize = 14.sp)
                                }
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        color = if (isDarkMode) Color.White else Color.Black,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
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
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Recent & Favorites integrated cleaner
                    if (favoriteTools.isNotEmpty() || recentTools.isNotEmpty()) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            if (favoriteTools.isNotEmpty()) {
                                SectionLabel("Favorites", isDarkMode)
                                ToolRow(favoriteTools, isDarkMode, handleNavigateToTool)
                            }
                            if (recentTools.isNotEmpty()) {
                                SectionLabel(stringResource(R.string.recent_tools), isDarkMode)
                                ToolRow(recentTools, isDarkMode, handleNavigateToTool)
                            }
                        }
                    }

                    if (categories.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            Text("No tools found", color = Color.Gray)
                        }
                    } else {
                        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.padding(horizontal = 8.dp)) {
                            var firstRowHeight by remember { mutableIntStateOf(0) }
                            val density = androidx.compose.ui.platform.LocalDensity.current

                            Column {
                                val chunkedCategories = categories.chunked(2)
                                chunkedCategories.forEachIndexed { rowIndex, rowCategories ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Max)
                                            .onGloballyPositioned {
                                                if (rowIndex == 0) firstRowHeight = it.size.height
                                            }
                                    ) {
                                        rowCategories.forEach { cat ->
                                            CategoryCard(
                                                category = cat,
                                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                                visible = visible,
                                                index = categories.indexOf(cat),
                                                isDarkMode = isDarkMode,
                                                onNavigateToTool = handleNavigateToTool,
                                                favoriteTools = favoriteTools,
                                                onToggleFavorite = toggleFavorite
                                            )
                                        }
                                        if (rowCategories.size == 1) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            if (categories.size >= 4 && firstRowHeight > 0) {
                                val yOffset = with(density) { firstRowHeight.toDp() }
                                PremiumFab(
                                    visible = visible,
                                    onClick = {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastRefreshTime > 2000) {
                                            lastRefreshTime = currentTime
                                            refreshTrigger++
                                            assistantViewModel.showMessage("Refreshing the page! ⚡", state = AssistantState.HAPPY, type = AssistantMessageType.THOUGHT)
                                            Toast.makeText(context, "Page Refreshed", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .offset(y = yOffset - 24.dp)
                                        .semantics { contentDescription = "Refresh the home screen tools" }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, isDarkMode: Boolean) {
    Text(
        text = text,
        color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
fun ToolRow(tools: List<Tool>, isDarkMode: Boolean, onNavigate: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(tools, key = { it.id }) { tool ->
            RecentToolChip(tool, isDarkMode) { onNavigate(tool.id) }
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
        animationSpec = tween(400, delayMillis = index * 80), label = ""
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = ""
    )

    ElevatedCard(
        modifier = modifier
            .padding(6.dp)
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDarkMode) category.color.copy(alpha = 0.15f) else category.color.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                category.title,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F2027),
                modifier = Modifier.padding(bottom = 10.dp)
            )
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
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 5.dp, horizontal = 2.dp)
    ) {
        Icon(
            painter = painterResource(tool.iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF00D1B2)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            tool.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onToggleFavorite() },
            modifier = Modifier.size(22.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Toggle Favorite",
                tint = if (isFavorite) Color.Red else Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
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
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Box(
        modifier = modifier
            .scale(scale * pulseScale)
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF00D1B2), Color(0xFF00BFA5))))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_fincalc_logo_vector),
            contentDescription = "Refresh",
            tint = Color.Unspecified,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun RecentToolChip(tool: Tool, isDarkMode: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(tool.iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF00D1B2)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                tool.name,
                color = if (isDarkMode) Color.White else Color.Black,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
