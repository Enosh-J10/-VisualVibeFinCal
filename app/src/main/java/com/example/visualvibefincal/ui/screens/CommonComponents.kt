package com.example.visualvibefincal.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun SettingsItem(
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 16.sp)
        if (trailing != null) {
            trailing()
        } else if (value != null) {
            Text(value, fontSize = 16.sp, color = Color.Gray)
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    return@composed this.background(brush)
}

@Composable
fun ScanItemSkeleton(isDarkMode: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).shimmerEffect())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
            Box(Modifier.width(60.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        }
    }
}
@Composable
fun HistorySkeleton(isDarkMode: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(Modifier.height(16.dp))
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.05f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(Modifier.width(150.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(100.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(200.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                }
            }
        }
    }
}

fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

@Composable
fun BouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    containerColor: Color = Color(0xFF00D1B2),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreenScaffold(
    title: String,
    navController: NavController,
    isDarkMode: Boolean,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = if (isDarkMode) Color.White else Color.Black,
                    navigationIconContentColor = if (isDarkMode) Color.White else Color.Black
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
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
        ) {
            content(innerPadding)
        }
    }
}

@Composable
fun CalculatorCard(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = (if (isDarkMode) Color(0xFF1B2C33) else Color.White).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            content = content
        )
    }
}

@Composable
fun ResultDisplay(
    label: String,
    value: String,
    isDarkMode: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Gray,
            textAlign = TextAlign.Center
        )
        Text(
            value,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF00D1B2),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistorySection(
    screenKey: String,
    history: List<com.example.visualvibefincal.data.model.HistoryItem>,
    isDarkMode: Boolean,
    isLoading: Boolean = false,
    onClearHistory: () -> Unit
) {
    if (isLoading) {
        HistorySkeleton(isDarkMode)
    } else if (history.isNotEmpty()) {
        Spacer(Modifier.height(32.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                TextButton(onClick = onClearHistory) {
                    Text("Clear", color = Color(0xFF00D1B2))
                }
            }

            history.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = (if (isDarkMode) Color.White else Color.Black).copy(alpha = 0.05f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            item.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        Text(
                            item.result,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D1B2)
                        )
                        Text(
                            item.details,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(16.dp))
        }
        Text(
            text = message,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontSize = 16.sp
        )
    }
}

@Composable
fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    BoxWithConstraints(modifier = modifier.fillMaxHeight().width(12.dp)) {
        val maxHeight = constraints.maxHeight.toFloat()
        val scrollValue = scrollState.value.toFloat()
        val scrollMax = scrollState.maxValue.toFloat()
        
        if (scrollMax > 0) {
            val visibleHeight = maxHeight
            val totalHeight = scrollMax + visibleHeight
            val thumbHeight = (visibleHeight / totalHeight) * maxHeight
            val thumbOffset = (scrollValue / scrollMax) * (maxHeight - thumbHeight)
            
            Box(
                modifier = Modifier
                    .offset(y = with(LocalDensity.current) { thumbOffset.toDp() })
                    .height(with(LocalDensity.current) { thumbHeight.toDp() })
                    .width(12.dp)
                    .padding(horizontal = 3.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            val scrollPerPixel = scrollMax / (maxHeight - thumbHeight)
                            coroutineScope.launch {
                                scrollState.scrollTo((scrollState.value + delta * scrollPerPixel).toInt().coerceIn(0, scrollMax.toInt()))
                            }
                        }
                    )
            )
        }
    }
}
