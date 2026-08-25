package com.freelauncher.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.db.CalendarEventEntity
import com.freelauncher.app.data.db.NoteEntity
import com.freelauncher.app.data.service.DigitalWellbeingService
import com.freelauncher.app.data.service.FocusDayUsageData
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class ProductivityTab(val title: String, val icon: ImageVector) {
    NOTES("Notes", Icons.Outlined.EditNote),
    CALENDAR("Calendar", Icons.Outlined.CalendarMonth),
    FOCUS("Focus", Icons.Outlined.SelfImprovement),
    CALCULATOR("Calc", Icons.Outlined.Calculate)
}

@Composable
fun ProductivityScreen(
    state: LauncherUiState,
    onNavigate: (LauncherScreen) -> Unit,
    onAddNote: (String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onAddEvent: (String, Long) -> Unit,
    onToggleEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onStartFocusTimer: (Int) -> Unit,
    onStopFocusTimer: () -> Unit,
    onRequestUsagePermission: () -> Unit = {},
    onRefreshUsageStats: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(ProductivityTab.NOTES) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        if (abs(totalDragX) > 70f) {
                            onNavigate(LauncherScreen.HOME)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                    }
                )
            }
            .testTag("productivity_screen_root")
    ) {
        // Header with Back arrow & Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PRODUCTIVITY",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Quick Tools & Focus Hub",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = { onNavigate(LauncherScreen.HOME) },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("productivity_back_to_home")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Sub-tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductivityTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(200),
                    label = "tabContainerColor"
                )
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedTab = tab }
                        .testTag("prod_tab_${tab.name.lowercase(Locale.ROOT)}"),
                    shape = RoundedCornerShape(12.dp),
                    color = containerColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = contentColor
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool Content Area
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = "prod_tab_content"
        ) { tab ->
            when (tab) {
                ProductivityTab.NOTES -> NotesTool(
                    notes = state.notes,
                    onAddNote = onAddNote,
                    onDeleteNote = onDeleteNote
                )
                ProductivityTab.CALENDAR -> CalendarTool(
                    events = state.events,
                    onAddEvent = onAddEvent,
                    onToggleEvent = onToggleEvent,
                    onDeleteEvent = onDeleteEvent
                )
                ProductivityTab.FOCUS -> FocusTool(
                    isTimerRunning = state.isFocusTimerRunning,
                    secondsLeft = state.focusTimerSecondsLeft,
                    focusSessions = state.focusSessions,
                    hasUsagePermission = state.hasUsagePermission,
                    weeklyHistory = state.weeklyFocusHistory,
                    onRequestUsagePermission = onRequestUsagePermission,
                    onRefreshUsageStats = onRefreshUsageStats,
                    onStartTimer = onStartFocusTimer,
                    onStopTimer = onStopFocusTimer
                )
                ProductivityTab.CALCULATOR -> CalculatorTool()
            }
        }
    }
}

@Composable
fun NotesTool(
    notes: List<NoteEntity>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    var noteInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .testTag("note_text_input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (noteInput.isNotBlank()) {
                        onAddNote(noteInput)
                        noteInput = ""
                    }
                }),
                decorationBox = { inner ->
                    if (noteInput.isEmpty()) {
                        Text(
                            text = "Write a quick thought...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                    inner()
                }
            )

            IconButton(
                onClick = {
                    if (noteInput.isNotBlank()) {
                        onAddNote(noteInput)
                        noteInput = ""
                    }
                },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("add_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Save Note",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("notes_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (notes.isEmpty()) {
                item(key = "empty_notes") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, bottom = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No notes yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Type above to capture a quick distraction-free thought",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    val dateFormatted = remember(note.createdAt) {
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(note.createdAt))
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                0.75.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                clipboard.setText(AnnotatedString(note.content))
                                Toast.makeText(context, "Copied note to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("note_item_${note.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "$dateFormatted • Tap to copy",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteNote(note.id) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("delete_note_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarTool(
    events: List<CalendarEventEntity>,
    onAddEvent: (String, Long) -> Unit,
    onToggleEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (Long) -> Unit
) {
    var eventTitleInput by remember { mutableStateOf("") }
    val today = remember { Calendar.getInstance() }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currentDay = remember { today.get(Calendar.DAY_OF_MONTH) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(
                    0.75.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(14.dp)
                ),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthFormat.format(today.time).uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Day $currentDay",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                    val currentDayOfWeekIndex = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7
                    daysOfWeek.forEachIndexed { index, dayLetter ->
                        val isToday = index == currentDayOfWeekIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = dayLetter,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isToday) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = eventTitleInput,
                onValueChange = { eventTitleInput = it },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .testTag("event_title_input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (eventTitleInput.isNotBlank()) {
                        onAddEvent(eventTitleInput, System.currentTimeMillis())
                        eventTitleInput = ""
                    }
                }),
                decorationBox = { inner ->
                    if (eventTitleInput.isEmpty()) {
                        Text(
                            text = "Add upcoming event or reminder...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                    inner()
                }
            )

            IconButton(
                onClick = {
                    if (eventTitleInput.isNotBlank()) {
                        onAddEvent(eventTitleInput, System.currentTimeMillis())
                        eventTitleInput = ""
                    }
                },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("add_event_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Event",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("events_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (events.isEmpty()) {
                item(key = "empty_events") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp, bottom = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No upcoming events scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                0.75.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("event_item_${event.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onToggleEvent(event) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            1.5.dp,
                                            if (event.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .background(if (event.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (event.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (event.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (event.isCompleted) FontWeight.Normal else FontWeight.Medium
                                    )
                                )
                            }
                            IconButton(
                                onClick = { onDeleteEvent(event.id) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("delete_event_${event.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Event",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FocusTool(
    isTimerRunning: Boolean,
    secondsLeft: Int,
    focusSessions: List<com.freelauncher.app.data.db.FocusSessionEntity>,
    hasUsagePermission: Boolean = false,
    weeklyHistory: List<FocusDayUsageData> = emptyList(),
    onRequestUsagePermission: () -> Unit = {},
    onRefreshUsageStats: () -> Unit = {},
    onStartTimer: (Int) -> Unit,
    onStopTimer: () -> Unit
) {
    val context = LocalContext.current

    // Use live weekly history from state or compute dynamically via DigitalWellbeingService
    val history = remember(weeklyHistory, focusSessions, hasUsagePermission) {
        if (weeklyHistory.isNotEmpty()) {
            weeklyHistory
        } else {
            DigitalWellbeingService.getWeeklyStats(context, focusSessions)
        }
    }

    var selectedDayIndex by remember { mutableIntStateOf(6) } // Defaults to today (last element)
    val selectedDay = history.getOrElse(selectedDayIndex) { history.lastOrNull() ?: FocusDayUsageData("T", "today", 0, 0, 0, true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Digital Wellbeing Permission Notice Banner (if not yet granted)
        if (!hasUsagePermission) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("digital_wellbeing_permission_banner"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DIGITAL WELLBEING ACCESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Grant Usage Access to obtain your exact live screen time, unlock frequency, and accurate daily focus score directly from Android's Digital Wellbeing subsystem.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onRequestUsagePermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("grant_usage_permission_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Enable Usage Access",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Weekly Focus History Card (powered by Digital Wellbeing UsageStats)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(
                    0.75.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(18.dp)
                )
                .testTag("weekly_focus_history_card"),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Weekly Focus History",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                        Text(
                            text = if (hasUsagePermission) "Android Digital Wellbeing • Live" else "Baseline • Enable usage access",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (hasUsagePermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(
                        onClick = onRefreshUsageStats,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("refresh_focus_stats_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Focus Stats",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Three metric columns with dividers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Column 1: FOCUS SCORE
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "FOCUS SCORE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${selectedDay.focusScore}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    )

                    // Column 2: UNLOCKS
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "UNLOCKS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${selectedDay.unlocks}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    )

                    // Column 3: SCREEN TIME
                    Column(
                        modifier = Modifier.weight(1.1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "SCREEN TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val hours = selectedDay.screenTimeMinutes / 60
                        val mins = selectedDay.screenTimeMinutes % 60
                        Text(
                            text = "${hours}h ${mins}m",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Focus Quality Rating & Session Highlight
                Spacer(modifier = Modifier.height(14.dp))
                val focusRatingText = when {
                    selectedDay.focusScore >= 80 -> "OPTIMAL FOCUS"
                    selectedDay.focusScore >= 60 -> "GOOD FOCUS"
                    selectedDay.focusScore > 0 -> "HIGH DISTRACTION"
                    else -> "NO DATA RECORDED"
                }
                val focusRatingColor = when {
                    selectedDay.focusScore >= 80 -> MaterialTheme.colorScheme.primary
                    selectedDay.focusScore >= 60 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = focusRatingText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            fontSize = 9.5.sp
                        ),
                        color = focusRatingColor
                    )

                    if (selectedDay.focusSessionMinutes > 0) {
                        Text(
                            text = "${selectedDay.focusSessionMinutes}m focus locked",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 7-day pill indicator row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    history.forEachIndexed { index, day ->
                        val isSelected = (index == selectedDayIndex)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDayIndex = index }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("focus_history_day_$index")
                        ) {
                            // Pill Indicator colored by focus health
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.onSurfaceVariant
                                            day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                            day.focusScore >= 75 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                            day.focusScore in 50..74 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                                        }
                                    )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = day.dayLetter,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Distraction-Free Focus Session Timer
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    0.75.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DISTRACTION-FREE SESSION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                val mins = secondsLeft / 60
                val secs = secondsLeft % 60
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
                Text(
                    text = if (isTimerRunning) timeString else "25:00",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = if (isTimerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                if (isTimerRunning) {
                    Button(
                        onClick = onStopTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("end_focus_session_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "End Focus Session",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(15 to "15m", 25 to "25m", 45 to "45m").forEach { (duration, label) ->
                            Button(
                                onClick = { onStartTimer(duration) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("start_timer_${duration}m"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CalculatorTool() {
    var display by remember { mutableStateOf("0") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var resetOnNextDigit by remember { mutableStateOf(false) }

    fun onDigit(d: String) {
        if (resetOnNextDigit || display == "0") {
            display = d
            resetOnNextDigit = false
        } else {
            if (display.length < 12) display += d
        }
    }

    fun onOp(op: String) {
        val current = display.toDoubleOrNull() ?: 0.0
        if (operand1 != null && pendingOp != null && !resetOnNextDigit) {
            val result = when (pendingOp) {
                "+" -> operand1!! + current
                "-" -> operand1!! - current
                "×" -> operand1!! * current
                "÷" -> if (current != 0.0) operand1!! / current else 0.0
                else -> current
            }
            operand1 = result
            display = if (result % 1 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
        } else {
            operand1 = current
        }
        pendingOp = op
        resetOnNextDigit = true
    }

    fun onEquals() {
        val current = display.toDoubleOrNull() ?: 0.0
        if (operand1 != null && pendingOp != null) {
            val result = when (pendingOp) {
                "+" -> operand1!! + current
                "-" -> operand1!! - current
                "×" -> operand1!! * current
                "÷" -> if (current != 0.0) operand1!! / current else 0.0
                else -> current
            }
            display = if (result % 1 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
            operand1 = null
            pendingOp = null
            resetOnNextDigit = true
        }
    }

    fun onClear() {
        display = "0"
        operand1 = null
        pendingOp = null
        resetOnNextDigit = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("calculator_tool"),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    0.75.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (pendingOp != null && operand1 != null) {
                        val opText = if (operand1!! % 1 == 0.0) operand1!!.toLong().toString() else operand1.toString()
                        Text(
                            text = "$opText $pendingOp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = display,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val buttons = listOf(
            listOf("C", "±", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isOp = key in listOf("÷", "×", "-", "+", "=")
                        val isClear = key == "C"
                        val weight = if (key == "0") 2f else 1f

                        Surface(
                            modifier = Modifier
                                .weight(weight)
                                .height(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    0.5.dp,
                                    if (isOp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    when (key) {
                                        "C" -> onClear()
                                        "±" -> {
                                            val d = display.toDoubleOrNull() ?: 0.0
                                            display = if ((-d) % 1 == 0.0) (-d).toLong().toString() else (-d).toString()
                                        }
                                        "%" -> {
                                            val d = display.toDoubleOrNull() ?: 0.0
                                            display = (d / 100.0).toString()
                                        }
                                        "÷", "×", "-", "+" -> onOp(key)
                                        "=" -> onEquals()
                                        "." -> {
                                            if (!display.contains(".")) display += "."
                                        }
                                        else -> onDigit(key)
                                    }
                                }
                                .testTag("calc_key_$key"),
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                key == "=" -> MaterialTheme.colorScheme.primary
                                isOp -> MaterialTheme.colorScheme.primaryContainer
                                isClear -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = if (isOp) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 20.sp
                                    ),
                                    color = when {
                                        key == "=" -> MaterialTheme.colorScheme.onPrimary
                                        isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
