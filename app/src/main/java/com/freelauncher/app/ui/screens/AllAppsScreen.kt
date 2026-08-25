package com.freelauncher.app.ui.screens

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.models.AppCategory
import com.freelauncher.app.data.models.AppCategoryInfo
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.service.ContactSearchResult
import com.freelauncher.app.data.service.MessageSearchResult
import com.freelauncher.app.data.service.SettingSearchResult
import com.freelauncher.app.data.service.UniversalSearchManager
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.util.TrackScrollHaptics
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsScreen(
    state: LauncherUiState,
    onLaunchApp: (AppItem) -> Unit,
    onLongPressApp: (AppItem) -> Unit,
    onNavigate: (LauncherScreen) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenCategoryManager: () -> Unit = {},
    onOpenAddCategory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigate(LauncherScreen.HOME)
    }

    val context = LocalContext.current
    var selectedCategoryFilterId by remember { mutableStateOf<String?>(null) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    val lazyListState = rememberLazyListState()
    TrackScrollHaptics(lazyListState)

    // Permission launcher for contacts search
    var hasContactsPerm by remember {
        mutableStateOf(UniversalSearchManager.hasContactsPermission(context))
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPerm = isGranted
    }

    // Permission launcher for SMS search
    var hasSmsPerm by remember {
        mutableStateOf(UniversalSearchManager.hasSmsPermission(context))
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPerm = isGranted
    }

    val isSearching = state.searchQuery.isNotBlank()

    // 1. Filtered and Sorted Apps
    val matchingApps = remember(state.installedApps, state.searchQuery, selectedCategoryFilterId) {
        val list = if (selectedCategoryFilterId != null) {
            state.installedApps.filter { 
                it.categoryId == selectedCategoryFilterId || 
                (it.category.name == selectedCategoryFilterId && it.categoryId.isBlank()) 
            }
        } else {
            state.installedApps
        }

        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase(Locale.ROOT)
            list.filter {
                it.label.lowercase(Locale.ROOT).contains(q) ||
                        it.packageName.lowercase(Locale.ROOT).contains(q)
            }.sortedBy { it.label.lowercase(Locale.ROOT) }
        } else {
            list.sortedBy { it.label.lowercase(Locale.ROOT) }
        }
    }

    // 2. Universal Search: Settings
    val matchingSettings = remember(state.searchQuery) {
        if (state.searchQuery.isNotBlank()) {
            UniversalSearchManager.searchSettings(state.searchQuery)
        } else {
            emptyList()
        }
    }

    // 3. Universal Search: Contacts
    val matchingContacts = remember(state.searchQuery, hasContactsPerm) {
        if (state.searchQuery.isNotBlank() && hasContactsPerm) {
            UniversalSearchManager.searchContacts(context, state.searchQuery)
        } else {
            emptyList()
        }
    }

    // 4. Universal Search: Messages
    val matchingMessages = remember(state.searchQuery, hasSmsPerm) {
        if (state.searchQuery.isNotBlank() && hasSmsPerm) {
            UniversalSearchManager.searchMessages(context, state.searchQuery)
        } else {
            emptyList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 44.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        totalDragY = 0f
                        totalDragX = 0f
                    },
                    onDragEnd = {
                        val threshold = 60f
                        // Swipe Down -> Home
                        if (totalDragY > threshold && abs(totalDragY) > abs(totalDragX) * 0.7f) {
                            onNavigate(LauncherScreen.HOME)
                        }
                        // Swipe Left -> Home
                        else if (totalDragX < -threshold && abs(totalDragX) > abs(totalDragY) * 0.7f) {
                            onNavigate(LauncherScreen.HOME)
                        }
                        // Swipe Right -> Home
                        else if (totalDragX > threshold && abs(totalDragX) > abs(totalDragY) * 0.7f) {
                            onNavigate(LauncherScreen.HOME)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        totalDragY += dragAmount.y
                        totalDragX += dragAmount.x
                    }
                )
            }
            .testTag("all_apps_screen_root")
    ) {
        // Top Return Hint Button and Search Bar Container (lowered well below punch hole camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(LauncherScreen.HOME) },
                modifier = Modifier
                    .size(44.dp)
                    .testTag("all_apps_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Universal Search Box in the marked lower area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apps_search_input"),
                        decorationBox = { innerTextField ->
                            if (state.searchQuery.isEmpty()) {
                                Text(
                                    text = "Search apps, settings, contacts...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Category Filter Row below search bar
        if (!isSearching) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allSelected = selectedCategoryFilterId == null
                CategoryChip(
                    label = "ALL",
                    isSelected = allSelected,
                    onClick = {
                        LauncherHaptics.playClick(context)
                        selectedCategoryFilterId = null
                    },
                    testTag = "category_chip_all"
                )

                // Visible Categories (ordered according to user structure)
                val visibleCategories = state.categories.filter { !it.isHidden }
                visibleCategories.forEach { category ->
                    val isSelected = selectedCategoryFilterId == category.id
                    CategoryChip(
                        label = category.title,
                        isSelected = isSelected,
                        onClick = {
                            LauncherHaptics.playClick(context)
                            selectedCategoryFilterId = if (isSelected) null else category.id
                        },
                        testTag = "category_chip_${category.id.lowercase(Locale.ROOT)}"
                    )
                }

                // Quick Add Category Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            0.75.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            LauncherHaptics.playClick(context)
                            onOpenAddCategory()
                        }
                        .testTag("category_chip_add_new")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Category",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "ADD",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Restructure & Manage Categories Icon Chip
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            LauncherHaptics.playClick(context)
                            onOpenCategoryManager()
                        }
                        .testTag("manage_categories_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Manage and Restructure Categories",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Content List
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
                .testTag("all_apps_lazy_column"),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (isSearching) {
                // UNIVERSAL SEARCH RESULTS VIEW
                val hasAnyResults = matchingApps.isNotEmpty() ||
                        matchingSettings.isNotEmpty() ||
                        matchingContacts.isNotEmpty() ||
                        matchingMessages.isNotEmpty()

                // Section 1: Matching Apps
                if (matchingApps.isNotEmpty()) {
                    item(key = "search_header_apps") {
                        SearchSectionHeader(title = "APPS (${matchingApps.size})")
                    }
                    items(
                        items = matchingApps,
                        key = { "app_${it.packageName}_${it.label}" }
                    ) { app ->
                        AllAppsRowItem(
                            app = app,
                            showMonogram = state.showMonograms,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onLongPressApp(app) },
                            testTag = "search_app_item_${app.packageName}"
                        )
                    }
                }

                // Section 2: Phone Settings
                if (matchingSettings.isNotEmpty()) {
                    item(key = "search_header_settings") {
                        SearchSectionHeader(title = "PHONE SETTINGS (${matchingSettings.size})")
                    }
                    items(
                        items = matchingSettings,
                        key = { "setting_${it.intentAction}" }
                    ) { setting ->
                        SettingResultRowItem(
                            setting = setting,
                            onClick = {
                                try {
                                    val intent = Intent(setting.intentAction).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback to general settings
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    } catch (e2: Exception) {
                                        e2.printStackTrace()
                                    }
                                }
                            }
                        )
                    }
                }

                // Section 3: Contacts
                if (matchingContacts.isNotEmpty()) {
                    item(key = "search_header_contacts") {
                        SearchSectionHeader(title = "CONTACTS (${matchingContacts.size})")
                    }
                    items(
                        items = matchingContacts,
                        key = { "contact_${it.id}_${it.phoneNumber}" }
                    ) { contact ->
                        ContactResultRowItem(
                            contact = contact,
                            onCall = {
                                try {
                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${contact.phoneNumber}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(callIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            onMessage = {
                                try {
                                    val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("smsto:${contact.phoneNumber}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(smsIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                } else if (!hasContactsPerm) {
                    // Discreet affordance to enable contacts search
                    item(key = "enable_contacts_search") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .clickable {
                                    contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Enable Contacts in Universal Search",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "ALLOW",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Section 4: Messages & SMS Quick Actions
                item(key = "search_header_messages") {
                    SearchSectionHeader(
                        title = "MESSAGES",
                        subtitle = "PROCESSED LOCALLY"
                    )
                }

                // Quick Send SMS / Compose Message item
                item(key = "quick_compose_message") {
                    MessageActionRowItem(
                        title = "Send message to \"${state.searchQuery}\"",
                        subtitle = "Compose text message",
                        onClick = {
                            try {
                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${state.searchQuery}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(smsIntent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }

                if (matchingMessages.isNotEmpty()) {
                    items(
                        items = matchingMessages,
                        key = { "msg_${it.id}" }
                    ) { msg ->
                        MessageItemRow(
                            message = msg,
                            onClick = {
                                try {
                                    val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("smsto:${msg.address}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(smsIntent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                } else if (!hasSmsPerm) {
                    // Prominent affordance to enable message search
                    item(key = "enable_sms_search") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .clickable {
                                    smsPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Message,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Enable Message Search",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "ALLOW",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Section 5: Web Search Fallback
                item(key = "search_header_web") {
                    SearchSectionHeader(title = "WEB SEARCH")
                }
                item(key = "web_search_item") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                try {
                                    val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                                        putExtra(SearchManager.QUERY, state.searchQuery)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(webIntent)
                                } catch (e: Exception) {
                                    try {
                                        val browserIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/search?q=${Uri.encode(state.searchQuery)}")
                                        ).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (e2: Exception) {
                                        e2.printStackTrace()
                                    }
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Search Google for \"${state.searchQuery}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Open web browser search",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

            } else {
                // NORMAL ALL APPS VIEW
                if (matchingApps.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, bottom = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No apps found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                } else if (selectedCategoryFilterId == null) {
                    // WHEN NO CATEGORY IS SELECTED: Show all apps strictly alphabetically (A-Z) without category section headers
                    items(
                        items = matchingApps,
                        key = { it.id.ifBlank { "${it.packageName}/${it.activityName}#${it.label}" } }
                    ) { app ->
                        AllAppsRowItem(
                            app = app,
                            showMonogram = state.showMonograms,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onLongPressApp(app) },
                            testTag = "app_item_${app.packageName}"
                        )
                    }
                } else {
                    // WHEN A SPECIFIC CATEGORY IS SELECTED: Show header for that category followed by its apps
                    val currentCategoryTitle = state.categories.find { it.id == selectedCategoryFilterId }?.title ?: selectedCategoryFilterId!!
                    item(key = "header_${selectedCategoryFilterId}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp)
                        ) {
                            Text(
                                text = currentCategoryTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 2.5.sp,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            )
                        }
                    }

                    items(
                        items = matchingApps,
                        key = { it.id.ifBlank { "${it.packageName}/${it.activityName}#${it.label}" } }
                    ) { app ->
                        AllAppsRowItem(
                            app = app,
                            showMonogram = state.showMonograms,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onLongPressApp(app) },
                            testTag = "app_item_${app.packageName}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    }
}

@Composable
fun SettingResultRowItem(
    setting: SettingSearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(
                    0.75.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(6.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = setting.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = setting.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContactResultRowItem(
    contact: ContactSearchResult,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onCall)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        0.75.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        RoundedCornerShape(6.dp)
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(2).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Column {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onCall,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onMessage,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Message,
                    contentDescription = "Message",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MessageActionRowItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(
                    0.75.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    RoundedCornerShape(6.dp)
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Message,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun MessageItemRow(
    message: MessageSearchResult,
    onClick: () -> Unit
) {
    val dateStr = remember(message.date) {
        if (message.date > 0) {
            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(message.date)
        } else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Message,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message.senderOrRecipient,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (dateStr.isNotBlank()) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = message.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = 0.75.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.secondary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsRowItem(
    app: AppItem,
    showMonogram: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (showMonogram) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            0.75.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.monogram,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (app.isPinned) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
