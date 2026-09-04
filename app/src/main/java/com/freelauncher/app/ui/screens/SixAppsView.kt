package com.freelauncher.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SixAppsView(
    state: LauncherUiState,
    currentTime: Date,
    onLaunchApp: (AppItem) -> Unit,
    onLongPressApp: (AppItem) -> Unit,
    onNavigate: (LauncherScreen) -> Unit,
    onToggleLock: () -> Unit,
    onOpenMultiPin: () -> Unit = {},
    onClearLockFeedback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    val isLocked = state.isPinnedOnlyLocked

    // Request high refresh rate for smooth transitions when this screen is active
    val currentView = LocalView.current
    SideEffect {
        if (android.os.Build.VERSION.SDK_INT >= 35) { // Android 15+
            try {
                currentView.requestedFrameRate = 120f
            } catch (e: Exception) { /* Fallback */ }
        }
    }

    val hourFormat = remember { java.text.SimpleDateFormat("hh", java.util.Locale.getDefault()) }
    val minFormat = remember { java.text.SimpleDateFormat("mm", java.util.Locale.getDefault()) }
    val hourString = remember(currentTime) { hourFormat.format(currentTime) }
    val minString = remember(currentTime) { minFormat.format(currentTime) }

    // Auto-clear feedback message after 2.5s
    LaunchedEffect(state.pinnedLockFeedbackMessage) {
        if (state.pinnedLockFeedbackMessage != null) {
            delay(2500)
            onClearLockFeedback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .statusBarsPadding()
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectDragGestures(
                        onDragStart = {
                            totalDragY = 0f
                            totalDragX = 0f
                        },
                        onDragEnd = {
                            val threshold = 60f
                            if (abs(totalDragY) > abs(totalDragX)) {
                                if (totalDragY > threshold) {
                                    // Dragged down -> back to Home
                                    onNavigate(LauncherScreen.HOME)
                                } else if (totalDragY < -threshold) {
                                    // Dragged up -> All Apps & Search
                                    onNavigate(LauncherScreen.ALL_APPS)
                                }
                            } else {
                                if (totalDragX < -threshold && state.showNewsFeed) {
                                    // Dragged left -> RSS Feed
                                    onNavigate(LauncherScreen.RSS_FEED)
                                } else if (totalDragX > threshold && state.showTimeAway) {
                                    // Dragged right -> Time Away
                                    onNavigate(LauncherScreen.TIME_AWAY)
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragY += dragAmount.y
                            totalDragX += dragAmount.x
                        }
                    )
                }
            }
            .testTag("six_apps_view_root")
    ) {
        // Top Section: Back Button (when unlocked)
        if (!isLocked) {
            // Top Return Hint when unlocked
            IconButton(
                onClick = { onNavigate(LauncherScreen.HOME) },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .testTag("back_to_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Center Content: Pinned Apps List (1 to 6 apps)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Small time icon displayed exclusively in the marked location, matching app monogram size
            Box(
                modifier = Modifier
                    .padding(start = 4.dp, bottom = 2.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(6.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("pinned_small_time_icon"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = hourString,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = minString,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.pinnedApps.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Your quick access slots are empty.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = if (isLocked) "Triple-tap to unlock and select apps." else "Add up to 6 apps for a distraction-free home.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Start
                    )
                    if (!isLocked) {
                        Surface(
                            onClick = onOpenMultiPin,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("add_first_app_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Add Apps (0/6)",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                val pinned = state.pinnedApps.take(6)
                pinned.forEachIndexed { index, app ->
                    SixAppRowItem(
                        app = app,
                        showMonogram = state.showMonograms,
                        isLocked = isLocked,
                        onClick = { onLaunchApp(app) },
                        onLongClick = {
                            if (!isLocked) {
                                // Changed: long press now opens the selection panel
                                onOpenMultiPin()
                            }
                        },
                        onTripleTap = {
                            LauncherHaptics.playClick(context)
                            onToggleLock()
                        },
                        testTag = "six_app_item_$index"
                    )
                }

                // Removed the "Add App" slot here since it should only show when zero apps are added
            }
        }

        // Bottom Section: Swipe affordance when unlocked OR Discreet Unlock Hint when locked
        if (!isLocked) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onNavigate(LauncherScreen.ALL_APPS) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "All Apps",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        } else {
            // Subtle unlock hint at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                ) {
                    val isTimerActive = state.isFocusTimerRunning && state.focusTimerSecondsLeft > 0
                    val timerText = if (isTimerActive) {
                        val mins = state.focusTimerSecondsLeft / 60
                        val secs = state.focusTimerSecondsLeft % 60
                        String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)
                    } else ""

                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isTimerActive) Icons.Outlined.SelfImprovement else Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = if (isTimerActive) "FOCUS $timerText • TRIPLE-TAP TO EXIT" else "LOCKED • TRIPLE-TAP TO EXIT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.8.sp,
                                fontSize = 10.sp,
                                fontWeight = if (isTimerActive) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Animated Toast / Feedback Banner when mode changes
        AnimatedVisibility(
            visible = state.pinnedLockFeedbackMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isLocked) 90.dp else 40.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = state.pinnedLockFeedbackMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        }
    }
}

/**
 * Small, elegant clock header displayed when in Locked Pinned-Only mode.
 */
@Composable
fun SmallClockHeader(
    timeString: String,
    amPmString: String,
    dateString: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = timeString,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = fontFamily,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 1.5.sp,
                    fontSize = 38.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (amPmString.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmString,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.3.sp
            ),
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SixAppRowItem(
    app: AppItem,
    showMonogram: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTripleTap: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var itemTapCount by remember { mutableIntStateOf(0) }
    var itemLastTapTime by remember { mutableLongStateOf(0L) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - itemLastTapTime < 450) {
                        itemTapCount++
                    } else {
                        itemTapCount = 1
                    }
                    itemLastTapTime = now

                    if (itemTapCount >= 3) {
                        itemTapCount = 0
                        onTripleTap()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (showMonogram) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(6.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.monogram,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 25.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
