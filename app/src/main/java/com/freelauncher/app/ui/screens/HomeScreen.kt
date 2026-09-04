package com.freelauncher.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import android.view.View
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.ui.components.ClockStyle
import com.freelauncher.app.ui.components.MinimalistClock
import com.freelauncher.app.ui.components.getContextualGreeting
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    state: LauncherUiState,
    currentTime: Date,
    onNavigate: (LauncherScreen) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenDialer: () -> Unit = {},
    modifier: Modifier = Modifier,
    onClockStyleChanged: (ClockStyle) -> Unit = {},
    onTimeCardOffsetChanged: (Float, Float) -> Unit = { _, _ -> },
    onResetTimeCardOffset: () -> Unit = {},
    onTimeCardScaleChanged: (Float) -> Unit = {},
    onClockEditModeToggled: (Boolean) -> Unit = {},
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }
    val dateText = remember(currentTime) { dateFormat.format(currentTime) }
    val greetingText = remember(state.customGreeting, currentTime) {
        if (state.customGreeting == "auto") {
            getContextualGreeting(Calendar.getInstance().apply { time = currentTime })
        } else {
            state.customGreeting
        }
    }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    // Realtime draggable coordinates for the Time Card
    var localOffsetX by remember(state.timeCardOffsetX) { mutableFloatStateOf(state.timeCardOffsetX) }
    var localOffsetY by remember(state.timeCardOffsetY) { mutableFloatStateOf(state.timeCardOffsetY) }
    var isDraggingCard by remember { mutableStateOf(value = false) }

    // Clock swipe tracking in Edit Mode (Style carousel)
    var clockSwipeAccumulator by remember { mutableFloatStateOf(0f) }
    val allClockStyles = remember { ClockStyle.entries.toList() }
    val currentStyleIndex = allClockStyles.indexOf(state.clockStyle).coerceAtLeast(0)
    // Use rememberUpdatedState to prevent stale capture in gesture lambdas
    val latestStyleIndex by rememberUpdatedState(currentStyleIndex)

    val isEditMode = state.isClockEditMode
    val clockAnimScale by animateFloatAsState(
        targetValue = state.timeCardScale * (if (isDraggingCard) 1.06f else if (isEditMode) 1.02f else 1f),
        animationSpec = spring(),
        label = "clock_edit_scale",
    )

    // Active edit tab state in edit mode (0: Style, 1: Resize)
    var editTab by remember { mutableIntStateOf(0) }

    // Request high refresh rate for smooth transitions when this screen is active
    val currentView = LocalView.current
    SideEffect {
        if (android.os.Build.VERSION.SDK_INT >= 35) { // Android 15+
            try {
                currentView.requestedFrameRate = 120f
            } catch (e: Exception) { /* Fallback */ }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .pointerInput(isEditMode) {
                if (!isEditMode) {
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDragEnd = {
                            val threshold = 60f
                            if (abs(totalDragX) > abs(totalDragY)) {
                                // Horizontal Swipe
                                if (totalDragX < -threshold && state.showNewsFeed) {
                                    // Swiped Left -> News Feed screen
                                    onNavigate(LauncherScreen.RSS_FEED)
                                } else if (totalDragX > threshold && state.showTimeAway) {
                                    // Swiped Right -> Time Away screen
                                    onNavigate(LauncherScreen.TIME_AWAY)
                                }
                            } else {
                                // Vertical Swipe
                                if (totalDragY < -threshold) {
                                    // Swiped Up -> Six Apps screen
                                    onNavigate(LauncherScreen.SIX_APPS)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isEditMode) {
                        onClockEditModeToggled(false)
                    } else {
                        onNavigate(LauncherScreen.SIX_APPS)
                    }
                },
                onLongClick = {
                    if (!isEditMode) {
                        onOpenSettings()
                    }
                }
            )
            .testTag("home_screen_root")
    ) {
        // Edge gesture hints when not editing (Only shown if gesture hints setting is enabled)
        if (!isEditMode && state.showGestureHints) {
            // Left hint: Time Away Screen
            if (state.showTimeAway) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .width(3.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                )
            }
            // Right hint: News Feed
            if (state.showNewsFeed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .width(3.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                )
            }
        }

        // Top Edit Mode Header Badge & Drag Indicator
        if (isEditMode) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .testTag("clock_edit_mode_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenWith,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "DRAG TO MOVE • CUSTOMIZE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Main Time Card Container with Free Movement / Drag Offset & Scaling
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(localOffsetX.roundToInt(), localOffsetY.roundToInt())
                }
                .pointerInput(isEditMode) {
                    if (isEditMode) {
                        detectDragGestures(
                            onDragStart = {
                                isDraggingCard = true
                            },
                            onDragEnd = {
                                isDraggingCard = false
                                onTimeCardOffsetChanged(localOffsetX, localOffsetY)
                            },
                            onDragCancel = {
                                isDraggingCard = false
                                onTimeCardOffsetChanged(localOffsetX, localOffsetY)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            localOffsetX += dragAmount.x
                            localOffsetY += dragAmount.y
                        }
                    }
                }
                .testTag("time_card_moveable_container")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 12.dp)
            ) {
                // Interactive Clock Card
                Box(
                    modifier = Modifier
                        .scale(clockAnimScale)
                        .clip(RoundedCornerShape(28.dp))
                        .then(
                            if (isEditMode) {
                                Modifier
                                    .background(
                                        if (isDraggingCard)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    )
                                    .border(
                                        if (isDraggingCard) 2.5.dp else 1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(28.dp)
                                    )
                                    .padding(vertical = 16.dp, horizontal = 18.dp)
                            } else {
                                Modifier.padding(vertical = 6.dp, horizontal = 8.dp)
                            }
                        )
                        .pointerInput(isEditMode) {
                            if (isEditMode) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        clockSwipeAccumulator = 0f
                                    },
                                    onDragEnd = {
                                        val swipeThreshold = 50f
                                        if (clockSwipeAccumulator < -swipeThreshold) {
                                            val nextIndex = (latestStyleIndex + 1) % allClockStyles.size
                                            onClockStyleChanged(allClockStyles[nextIndex])
                                        } else if (clockSwipeAccumulator > swipeThreshold) {
                                            val prevIndex = if ((latestStyleIndex - 1) < 0) allClockStyles.size - 1 else latestStyleIndex - 1
                                            onClockStyleChanged(allClockStyles[prevIndex])
                                        }
                                        clockSwipeAccumulator = 0f
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    clockSwipeAccumulator += dragAmount
                                }
                            }
                        }
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isEditMode) {
                                    onNavigate(LauncherScreen.SIX_APPS)
                                }
                            },
                            onLongClick = {
                                // Long-pressing time enters Clock Edit Mode to drag & place anywhere
                                onClockEditModeToggled(true)
                            }
                        )
                        .testTag("minimalist_clock_container"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MinimalistClock(
                            currentTime = currentTime,
                            clockStyle = state.clockStyle,
                            modifier = Modifier.testTag("minimalist_clock")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .scale(clockAnimScale.coerceAtMost(1.2f))
                        .testTag("home_date_text")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Action Icons Row: Search Lens & Dialer Icon (Between Date and Greetings)
                if (!isEditMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .scale(clockAnimScale.coerceAtMost(1.1f))
                            .padding(vertical = 4.dp)
                    ) {
                        // Search Lens Icon Button
                        Surface(
                            onClick = onOpenSearch,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.size(38.dp).testTag("home_search_lens_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Lens",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Dialer Icon Button
                        Surface(
                            onClick = onOpenDialer,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.size(38.dp).testTag("home_dialer_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Call,
                                    contentDescription = "Open Dialer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Contextual Greeting
                Text(
                    text = greetingText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Light,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .scale(clockAnimScale.coerceAtMost(1.15f))
                        .testTag("contextual_greeting_text")
                )

                // Phone-Free Today Time Indicator
                if (state.showTimeAway && state.hasUsagePermission && state.timeAwayStats != null) {
                    val todayMins = state.timeAwayStats.todayPhoneFreeMinutes
                    val hrs = todayMins / 60
                    val mins = todayMins % 60
                    val timeFormatted = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$timeFormatted phone-free today",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .scale(clockAnimScale.coerceAtMost(1.1f))
                            .testTag("home_phone_free_today_text")
                    )
                }
            }
        }

        // Clock & Time Card Edit Overlay Panel at Bottom (Only Style & Resize Tabs)
        if (isEditMode) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("time_card_editor_panel"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Tab Row: Style & Resize only (Position is replaced by free drag & placement)
                    TabRow(
                        selectedTabIndex = editTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = editTab == 0,
                            onClick = { editTab = 0 },
                            text = { Text("Style", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
                        )
                        Tab(
                            selected = editTab == 1,
                            onClick = { editTab = 1 },
                            text = { Text("Resize", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
                        )
                    }

                    when (editTab) {
                        // TAB 0: Style Carousel
                        0 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val prevIndex = if ((latestStyleIndex - 1) < 0) allClockStyles.size - 1 else latestStyleIndex - 1
                                            onClockStyleChanged(allClockStyles[prevIndex])
                                        }
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Style")
                                    }
                                    Text(
                                        text = "${state.clockStyle.displayName} (${latestStyleIndex + 1}/${allClockStyles.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = {
                                            val nextIndex = (latestStyleIndex + 1) % allClockStyles.size
                                            onClockStyleChanged(allClockStyles[nextIndex])
                                        }
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Style")
                                    }
                                }
                                Text(
                                    text = "Swipe horizontally on the clock or use arrows to change style",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        // TAB 1: Resize & Scaling Option + Drag-to-Place Guide
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Preset size chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val sizePresets = listOf(
                                        "75%" to 0.75f,
                                        "100%" to 1.0f,
                                        "125%" to 1.25f,
                                        "150%" to 1.5f
                                    )
                                    sizePresets.forEach { (label, scale) ->
                                        val isSelected = abs(state.timeCardScale - scale) < 0.05f
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onTimeCardScaleChanged(scale) },
                                            label = { Text(label, fontSize = 12.sp) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                                // Interactive Slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Scale:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Slider(
                                        value = state.timeCardScale,
                                        onValueChange = { onTimeCardScaleChanged(it) },
                                        valueRange = 0.6f..1.6f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${(state.timeCardScale * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.width(44.dp)
                                    )
                                }
                                // Reset position button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Touch & drag clock to place anywhere",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontSize = 11.sp
                                    )
                                    TextButton(
                                        onClick = {
                                            localOffsetX = 0f
                                            localOffsetY = 0f
                                            onResetTimeCardOffset()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Center Card", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Save / Apply Done Button
                    Button(
                        onClick = { onClockEditModeToggled(false) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("exit_clock_edit_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply & Save Setup", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Bottom gesture hint when not in edit mode (Only shown if gesture hints setting is enabled)
        if (!isEditMode && state.showGestureHints) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Tap for apps • Long press for settings • Long press time to drag & edit",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
