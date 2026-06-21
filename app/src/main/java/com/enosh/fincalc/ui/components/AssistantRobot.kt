package com.enosh.fincalc.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enosh.fincalc.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AssistantRobot(
    viewModel: AssistantViewModel,
    isDarkMode: Boolean,
    isPreview: Boolean = false,
    onOpenSettings: () -> Unit = {},
    onOpenAiChat: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs by viewModel.prefs.collectAsState()
    val message by viewModel.message.collectAsState()
    val messageType by viewModel.messageType.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val robotState by viewModel.robotState.collectAsState()

    var showAiChatPopup by remember { mutableStateOf(false) }

    if (!prefs.isEnabled && !isPreview) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenPaddingPx = with(density) { 16.dp.toPx() }
    
    val navigationBarsHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()

    val robotWidth = 70.dp
    val robotHeight = 90.dp
    val robotWidthPx = with(density) { robotWidth.toPx() }
    val robotHeightPx = with(density) { robotHeight.toPx() }

    val bubbleMaxWidth = 220.dp
    val bubbleMaxWidthPx = with(density) { bubbleMaxWidth.toPx() }

    // Starts in the middle right, away from bottom navigation and FAB
    val initialX = screenWidthPx - robotWidthPx - screenPaddingPx
    val initialY = (screenHeightPx / 2) - (robotHeightPx / 2)

    val animX = remember { Animatable(if (prefs.lastPosX != -1f) prefs.lastPosX else initialX) }
    val animY = remember { Animatable(if (prefs.lastPosY != -1f) prefs.lastPosY else initialY) }

    LaunchedEffect(prefs.lastPosX, prefs.lastPosY) {
        if (!isPreview && (prefs.lastPosX == -1f || prefs.lastPosY == -1f)) {
            animX.animateTo(initialX, spring())
            animY.animateTo(initialY, spring())
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val isRoastMode = prefs.isRoastMode
    val roastAccent = Color(0xFF9C27B0) // Purple for roast mode
    val roastGlow = Color(0xFFFF5252) // Reddish for roast glow

    val headColor = if (isRoastMode) Color(0xFF212121) else Color(if (prefs.isCustomMode) prefs.customHeadColor.hex else prefs.theme.headColor.hex)
    val bodyColor = if (isRoastMode) Color(0xFF303030) else Color(if (prefs.isCustomMode) prefs.customBodyColor.hex else prefs.theme.bodyColor.hex)
    val accentColor = if (isRoastMode) roastAccent else Color(if (prefs.isCustomMode) prefs.customAccentColor.hex else prefs.theme.accentColor.hex)

    // Floating and breathing animations
    val infiniteTransition = rememberInfiniteTransition(label = "robot_idle")
    
    val floatAnim by if (prefs.isAnimated) infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "float"
    ) else remember { mutableStateOf(0f) }

    val breathingAnim by if (prefs.isAnimated) infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "breathing"
    ) else remember { mutableStateOf(1f) }

    val armIdleAnim by if (prefs.isAnimated) infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "arm_idle"
    ) else remember { mutableStateOf(0f) }

    val waveOscillation by if (prefs.isAnimated) infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "wave_oscillation"
    ) else remember { mutableStateOf(0f) }

    val waveBounce by animateFloatAsState(
        targetValue = if (robotState == AssistantState.WAVING && prefs.isAnimated) -5f else 0f,
        animationSpec = if (robotState == AssistantState.WAVING)
            infiniteRepeatable(tween(250, easing = EaseInOutSine), RepeatMode.Reverse)
            else spring(),
        label = "waveBounce"
    )

    // Reactions like jumping or waving
    val jumpAnim = remember { Animatable(0f) }
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(robotState) {
        if (!isPreview && prefs.isAnimated) {
            when (robotState) {
                AssistantState.EXCITED, AssistantState.HAPPY -> {
                    jumpAnim.animateTo(-25f, tween(200, easing = EaseOutQuad))
                    jumpAnim.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                AssistantState.WAVING -> {
                    rotationAnim.animateTo(10f, tween(150))
                    rotationAnim.animateTo(-10f, tween(150))
                    rotationAnim.animateTo(0f, tween(150))
                }
                else -> {}
            }
        }
    }

    // Place the message bubble so it doesn't go off screen
    val isNearRight = animX.value > (screenWidthPx - robotWidthPx - bubbleMaxWidthPx)
    val isNearLeft = animX.value < (bubbleMaxWidthPx + screenPaddingPx)
    val isNearBottom = animY.value > (screenHeightPx - navigationBarsHeightPx - robotHeightPx - 100f)
    val isNearTop = animY.value < (statusBarHeightPx + 100f)

    val bubbleAlignment = when {
        isNearRight -> Alignment.CenterEnd
        isNearLeft -> Alignment.CenterStart
        else -> if (animX.value > screenWidthPx / 2) Alignment.CenterEnd else Alignment.CenterStart
    }

    Box(
        modifier = if (isPreview) Modifier.size(width = robotWidth + 40.dp, height = robotHeight + 40.dp) else Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        // Message Bubble
        if (!isPreview) {
            AnimatedVisibility(
                visible = message != null || isTyping,
                enter = fadeIn() + scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin.Center),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier
                    .graphicsLayer {
                        val xPos = if (bubbleAlignment == Alignment.CenterEnd) {
                            animX.value - bubbleMaxWidthPx - 8.dp.toPx()
                        } else {
                            animX.value + robotWidthPx + 8.dp.toPx()
                        }

                        val yPos = if (isNearBottom) {
                            animY.value - 40.dp.toPx()
                        } else if (isNearTop) {
                            animY.value + 40.dp.toPx()
                        } else {
                            animY.value - 20.dp.toPx()
                        }

                        translationX = xPos.coerceIn(screenPaddingPx, screenWidthPx - bubbleMaxWidthPx - screenPaddingPx)
                        translationY = yPos + floatAnim
                    }
                    .widthIn(max = bubbleMaxWidth)
                    .semantics {
                        contentDescription = if (isTyping) "Assistant is typing" else "Assistant message: ${message ?: ""}"
                    }
            ) {
                MessageBubble(
                    message = message,
                    isTyping = isTyping,
                    type = messageType,
                    isDarkMode = isDarkMode,
                    alignment = bubbleAlignment,
                    isRoastMode = isRoastMode
                )
            }

            AnimatedVisibility(
                visible = showAiChatPopup,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .graphicsLayer {
                        val xPos = animX.value + (robotWidthPx / 2) - 50.dp.toPx()
                        val yPos = animY.value - 45.dp.toPx()
                        translationX = xPos.coerceIn(screenPaddingPx, screenWidthPx - 100.dp.toPx() - screenPaddingPx)
                        translationY = yPos + floatAnim
                    }
            ) {
                Button(
                    onClick = { 
                        showAiChatPopup = false
                        onOpenAiChat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRoastMode) roastAccent else Color(0xFF00D1B2)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("AI Chat?", fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // Robot Character
        Box(
            modifier = Modifier
                .then(
                    if (isPreview) {
                        Modifier.align(Alignment.Center)
                    } else {
                        Modifier.graphicsLayer {
                            translationX = animX.value
                            translationY = animY.value + floatAnim + jumpAnim.value
                        }
                    }
                )
                .size(width = robotWidth, height = robotHeight)
                .graphicsLayer {
                    scaleX = breathingAnim
                    scaleY = breathingAnim
                    rotationZ = rotationAnim.value
                    translationY = (translationY + waveBounce) // Combined Y translation
                }
                .semantics {
                    contentDescription = "Robot assistant. Tap for a tip, hold for settings."
                    role = androidx.compose.ui.semantics.Role.Button
                }
                .pointerInput(Unit) {
                    if (!isPreview) {
                        detectDragGestures(
                            onDragEnd = {
                                val centerX = animX.value + robotWidthPx / 2
                                val snapX = if (centerX < screenWidthPx / 2) screenPaddingPx else screenWidthPx - robotWidthPx - screenPaddingPx

                                coroutineScope.launch {
                                    animX.animateTo(snapX, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
                                    viewModel.updatePosition(animX.value, animY.value, context)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    animX.snapTo((animX.value + dragAmount.x).coerceIn(0f, screenWidthPx - robotWidthPx))
                                    animY.snapTo((animY.value + dragAmount.y).coerceIn(statusBarHeightPx, screenHeightPx - robotHeightPx - navigationBarsHeightPx))
                                }
                            }
                        )
                    }
                }
                .pointerInput(Unit) {
                    if (!isPreview) {
                        detectTapGestures(
                            onTap = {
                                if (!showAiChatPopup) {
                                    showAiChatPopup = true
                                    coroutineScope.launch {
                                        delay(4000)
                                        showAiChatPopup = false
                                    }
                                } else {
                                    showAiChatPopup = false
                                }

                                if (message == null && !isTyping) {
                                    viewModel.triggerWave()
                                    viewModel.triggerRandomTip()
                                }
                            },
                            onLongPress = {
                                 viewModel.showMessage("Opening Settings...", AssistantState.THINKING, AssistantMessageType.THOUGHT)
                                 onOpenSettings()
                            }
                        )
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {

            // Shadow
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 12.dp)
                    .size(width = 45.dp, height = 8.dp)
                    .graphicsLayer {
                        alpha = (0.2f - (floatAnim / 150f)).coerceIn(0.05f, 0.2f)
                        scaleX = 1f + (floatAnim / 100f)
                    }
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            )

            // Glow Effect
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp)
                    .graphicsLayer {
                        alpha = 0.15f + (breathingAnim - 1f) * 5f
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(if (isRoastMode) roastGlow else accentColor, Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            // Full Body Character
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize()
            ) {
                // Arms (behind body)
                // Left Arm
                Box(modifier = Modifier.offset(x = (-44).dp, y = 52.dp)) {
                    LeftArm(robotState, accentColor, armIdleAnim)
                }
                // Right Arm
                Box(modifier = Modifier.offset(x = 20.dp, y = 52.dp)) {
                    RightArm(robotState, accentColor, armIdleAnim, waveOscillation)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    RobotHead(robotState, headColor, accentColor, prefs.gender, isRoastMode)
                    RobotBody(bodyColor, accentColor, prefs.gender)
                }
            }
        }
    }
}

@Composable
fun LeftArm(state: AssistantState, accentColor: Color, idleOffset: Float) {
    RobotArm(side = ArmSide.LEFT, state = state, accentColor = accentColor, idleOffset = idleOffset, waveRotation = 0f)
}

@Composable
fun RightArm(state: AssistantState, accentColor: Color, idleOffset: Float, waveRotation: Float) {
    RobotArm(side = ArmSide.RIGHT, state = state, accentColor = accentColor, idleOffset = idleOffset, waveRotation = waveRotation)
}

enum class ArmSide { LEFT, RIGHT }

@Composable
fun RobotArm(
    side: ArmSide,
    state: AssistantState,
    accentColor: Color,
    idleOffset: Float,
    waveRotation: Float
) {
    val isRight = side == ArmSide.RIGHT
    val isWaving = state == AssistantState.WAVING && isRight

    // Right arm pivot -> top-left corner (0,0)
    // Left arm pivot -> top-right corner (1,0)
    val pivot = if (isRight) TransformOrigin(0f, 0f) else TransformOrigin(1f, 0f)

    val baseRotation = if (isRight) 20f else -20f
    val idleBase = if (isRight) -idleOffset else idleOffset

    val shoulderRotation by animateFloatAsState(
        targetValue = if (isWaving) -40f else baseRotation + idleBase,
        animationSpec = if (isWaving) tween(200, easing = FastOutSlowInEasing) else spring(),
        label = "ShoulderRotation"
    )

    val forearmRotation by animateFloatAsState(
        targetValue = if (isWaving) -15f else 0f,
        animationSpec = if (isWaving) tween(200, easing = FastOutSlowInEasing) else spring(),
        label = "ForearmRotation"
    )

    Box(
        modifier = Modifier
            .size(width = 24.dp, height = 10.dp)
            .graphicsLayer {
                // Apply rotation and wave oscillation
                rotationZ = shoulderRotation + (if (isWaving) waveRotation else 0f)
                transformOrigin = pivot
            }
            .clip(RoundedCornerShape(5.dp))
            .background(accentColor.copy(alpha = 0.8f))
    ) {
        // Forearm
        Box(
            modifier = Modifier
                .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
                .size(width = 16.dp, height = 10.dp)
                .graphicsLayer {
                    rotationZ = forearmRotation
                    transformOrigin = if (isRight) TransformOrigin(0f, 0.5f) else TransformOrigin(1f, 0.5f)
                }
                .background(accentColor, RoundedCornerShape(5.dp))
        ) {
            // Hand
            RobotHand(
                modifier = Modifier
                    .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
                    .offset(x = if (isRight) 6.dp else (-6).dp),
                color = accentColor
            )
        }
    }
}

@Composable
fun RobotHand(modifier: Modifier, color: Color) {
    Box(
        modifier = modifier
            .size(12.dp)
            .background(color.copy(alpha = 0.4f), CircleShape)
            .padding(1.dp)
            .background(Color.White, CircleShape)
    )
}

@Composable
fun RobotHead(state: AssistantState, headColor: Color, accentColor: Color, gender: AssistantGender, isRoastMode: Boolean = false) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(headColor, headColor.copy(alpha = 0.8f))
                )
            )
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (isRoastMode) {
            // Devil Horns
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hornPath = Path().apply {
                    // Left Horn
                    moveTo(size.width * 0.2f, size.height * 0.2f)
                    lineTo(size.width * 0.1f, size.height * 0.05f)
                    lineTo(size.width * 0.35f, size.height * 0.15f)
                    close()
                    // Right Horn
                    moveTo(size.width * 0.8f, size.height * 0.2f)
                    lineTo(size.width * 0.9f, size.height * 0.05f)
                    lineTo(size.width * 0.65f, size.height * 0.15f)
                    close()
                }
                drawPath(hornPath, color = Color(0xFFFF5252))
            }
        }

        // Accessory
        if (gender == AssistantGender.FEMALE && !isRoastMode) {
             Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(10.dp)
                    .background(Color(0xFFFF80AB), CircleShape)
                    .shadow(1.dp, CircleShape)
            )
        }

        // Face
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isRoastMode) Color(0xFF1A1A1A) else Color.White.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center
        ) {
            RobotFace(state, if (isRoastMode) Color(0xFFFF5252) else accentColor, isRoastMode)
        }
    }
}

@Composable
fun RobotBody(bodyColor: Color, accentColor: Color, gender: AssistantGender) {
    Box(
        modifier = Modifier
            .offset(y = (-4).dp)
            .size(width = 40.dp, height = 35.dp)
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp, topStart = 10.dp, topEnd = 10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(bodyColor.copy(alpha = 0.9f), bodyColor.copy(alpha = 0.7f))
                )
            )
    ) {
        // Chest Detail
        if (gender == AssistantGender.MALE) {
             Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 2.dp)
                    .size(width = 6.dp, height = 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.DarkGray.copy(alpha = 0.6f))
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 15.dp, height = 4.dp)
                    .background(accentColor.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: String?,
    isTyping: Boolean,
    type: AssistantMessageType,
    isDarkMode: Boolean,
    alignment: Alignment,
    isRoastMode: Boolean = false
) {
    val bubbleColor = when (type) {
        AssistantMessageType.SPEECH -> if (isRoastMode) Color(0xFF2E1B33) else if (isDarkMode) Color(0xFF2C2C2E) else Color.White
        AssistantMessageType.THOUGHT -> if (isRoastMode) Color(0xFF1B1B1B) else if (isDarkMode) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    }

    val textColor = if (isRoastMode || isDarkMode) Color.White else Color.Black

    Surface(
        color = bubbleColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            if (isTyping) {
                TypingIndicator(isDarkMode)
            } else {
                Text(
                    text = message ?: "",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun TypingIndicator(isDarkMode: Boolean) {
    val dotColor = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val anim = rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(6.dp)
                    .graphicsLayer { alpha = anim.value }
                    .background(dotColor, CircleShape)
            )
        }
    }
}

@Composable
fun RobotFace(state: AssistantState, color: Color, isRoastMode: Boolean = false) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val eyeColor = color
        val eyeY = size.height * 0.4f
        
        if (isRoastMode) {
            // Cheeky Devil Eyes
            val eyePathLeft = Path().apply {
                moveTo(size.width * 0.2f, eyeY + 5f)
                lineTo(size.width * 0.4f, eyeY)
                lineTo(size.width * 0.4f, eyeY + 10f)
                close()
            }
            val eyePathRight = Path().apply {
                moveTo(size.width * 0.8f, eyeY + 5f)
                lineTo(size.width * 0.6f, eyeY)
                lineTo(size.width * 0.6f, eyeY + 10f)
                close()
            }
            drawPath(eyePathLeft, color = eyeColor)
            drawPath(eyePathRight, color = eyeColor)
            
            // Smirk
            drawArc(
                color = eyeColor,
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(size.width * 0.35f, size.height * 0.6f),
                size = androidx.compose.ui.geometry.Size(12f, 6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
            )
        } else {
            when (state) {
                AssistantState.HAPPY, AssistantState.EXCITED -> {
                    // Arched Eyes
                    drawArc(eyeColor, -180f, 180f, false, Offset(size.width * 0.25f, eyeY), size = androidx.compose.ui.geometry.Size(12f, 12f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    drawArc(eyeColor, -180f, 180f, false, Offset(size.width * 0.55f, eyeY), size = androidx.compose.ui.geometry.Size(12f, 12f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    // Smile
                    drawArc(eyeColor, 0f, 180f, false, Offset(size.width * 0.35f, size.height * 0.6f), size = androidx.compose.ui.geometry.Size(12f, 8f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
                AssistantState.WAVING -> {
                    // Wider Eyes + Glow
                    drawCircle(eyeColor, radius = 5f, center = Offset(size.width * 0.35f, eyeY + 4f))
                    drawCircle(eyeColor, radius = 5f, center = Offset(size.width * 0.65f, eyeY + 4f))
                    drawCircle(eyeColor.copy(alpha = 0.3f), radius = 8f, center = Offset(size.width * 0.35f, eyeY + 4f))
                    drawCircle(eyeColor.copy(alpha = 0.3f), radius = 8f, center = Offset(size.width * 0.65f, eyeY + 4f))
                    // Smile
                    drawArc(eyeColor, 0f, 180f, false, Offset(size.width * 0.35f, size.height * 0.65f), size = androidx.compose.ui.geometry.Size(8f, 4f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
                AssistantState.THINKING -> {
                    // Dot Eyes
                    drawCircle(eyeColor, radius = 3f, center = Offset(size.width * 0.35f, eyeY + 4f))
                    drawCircle(eyeColor, radius = 3f, center = Offset(size.width * 0.65f, eyeY + 4f))
                    // Straight Mouth
                    drawLine(eyeColor, Offset(size.width * 0.4f, size.height * 0.7f), Offset(size.width * 0.6f, size.height * 0.7f), strokeWidth = 2f)
                }
                AssistantState.ERROR -> {
                    // X Eyes
                    drawLine(eyeColor, Offset(size.width * 0.3f, eyeY), Offset(size.width * 0.4f, eyeY + 8f), strokeWidth = 2f)
                    drawLine(eyeColor, Offset(size.width * 0.4f, eyeY), Offset(size.width * 0.3f, eyeY + 8f), strokeWidth = 2f)
                    drawLine(eyeColor, Offset(size.width * 0.6f, eyeY), Offset(size.width * 0.7f, eyeY + 8f), strokeWidth = 2f)
                    drawLine(eyeColor, Offset(size.width * 0.7f, eyeY), Offset(size.width * 0.6f, eyeY + 8f), strokeWidth = 2f)
                    // O Mouth
                    drawCircle(eyeColor, radius = 4f, center = Offset(size.width * 0.5f, size.height * 0.75f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
                AssistantState.SHUSH -> {
                     // Dot Eyes
                    drawCircle(eyeColor, radius = 4f, center = Offset(size.width * 0.35f, eyeY + 4f))
                    drawCircle(eyeColor, radius = 4f, center = Offset(size.width * 0.65f, eyeY + 4f))
                    // Shush Mouth (O)
                    drawCircle(eyeColor, radius = 3f, center = Offset(size.width * 0.5f, size.height * 0.7f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
                else -> {
                    // Default Eyes
                    drawCircle(eyeColor, radius = 4f, center = Offset(size.width * 0.35f, eyeY + 4f))
                    drawCircle(eyeColor, radius = 4f, center = Offset(size.width * 0.65f, eyeY + 4f))
                    // Small Smile
                    drawArc(eyeColor, 0f, 180f, false, Offset(size.width * 0.4f, size.height * 0.65f), size = androidx.compose.ui.geometry.Size(8f, 4f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }
            }
        }
    }
}