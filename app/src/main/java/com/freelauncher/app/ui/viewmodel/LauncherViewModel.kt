package com.freelauncher.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freelauncher.app.data.db.*
import com.freelauncher.app.data.models.AppCategory
import com.freelauncher.app.data.models.AppCategoryInfo
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.repository.LauncherRepository
import com.freelauncher.app.data.service.DigitalWellbeingService
import com.freelauncher.app.data.service.FocusDayUsageData
import com.freelauncher.app.ui.components.ClockStyle
import com.freelauncher.app.ui.components.TimeCardVerticalAlign
import com.freelauncher.app.ui.components.TimeCardHorizontalAlign
import com.freelauncher.app.ui.theme.LauncherFont
import com.freelauncher.app.ui.theme.LauncherThemeMode
import com.freelauncher.app.ui.theme.LauncherWallpaper
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

enum class LauncherScreen {
    HOME,
    SIX_APPS,
    ALL_APPS,
    RSS_FEED,
    TIME_AWAY
}

data class LauncherUiState(
    val currentScreen: LauncherScreen = LauncherScreen.HOME,
    val clockStyle: ClockStyle = ClockStyle.LARGE_DIGITAL,
    val timeCardVAlign: TimeCardVerticalAlign = TimeCardVerticalAlign.CENTER,
    val timeCardHAlign: TimeCardHorizontalAlign = TimeCardHorizontalAlign.CENTER,
    val timeCardOffsetX: Float = 0f,
    val timeCardOffsetY: Float = 0f,
    val timeCardScale: Float = 1.0f,
    val fontFamily: LauncherFont = LauncherFont.MINIMAL_SANS,
    val themeMode: LauncherThemeMode = LauncherThemeMode.OLED_BLACK,
    val wallpaperId: String = "cyber_noir",
    val customWallpaperUri: String? = null,
    val wallpaperDim: Float = 0.25f,
    val showMonograms: Boolean = true,
    val customGreeting: String = "auto",
    val installedApps: List<AppItem> = emptyList(),
    val pinnedApps: List<AppItem> = emptyList(),
    val categories: List<AppCategoryInfo> = emptyList(),
    val categorizedApps: Map<AppCategory, List<AppItem>> = emptyMap(),
    val notes: List<NoteEntity> = emptyList(),
    val events: List<CalendarEventEntity> = emptyList(),
    val feeds: List<RssFeedEntity> = emptyList(),
    val articles: List<RssArticleEntity> = emptyList(),
    val isSyncingRss: Boolean = false,
    val rssSyncMessage: String? = null,
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val focusTimerSecondsLeft: Int = 0,
    val isFocusTimerRunning: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val weeklyFocusHistory: List<FocusDayUsageData> = emptyList(),
    val timeAwayStats: com.freelauncher.app.data.service.TimeAwayStats? = null,
    val searchQuery: String = "",
    val selectedAppForPinning: AppItem? = null,
    val showMultiPinDialog: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showCategoryManagerSheet: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val showRssManagerDialog: Boolean = false,
    val showWallpaperPicker: Boolean = false,
    val showAtmosphericCreator: Boolean = false,
    val customWallpapers: List<LauncherWallpaper> = emptyList(),
    val isClockEditMode: Boolean = false,
    val showGestureHints: Boolean = false,
    val showNewsFeed: Boolean = true,
    val isPinnedOnlyLocked: Boolean = false,
    val pinnedLockFeedbackMessage: String? = null,
    val isBiometricLockEnabled: Boolean = false,
    val showBiometricAuthDialog: Boolean = false,
    val biometricAuthError: String? = null,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LauncherRepository(application)

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val _currentTime = MutableStateFlow(Date())
    val currentTime: StateFlow<Date> = _currentTime.asStateFlow()

    init {
        startTimeTicker()
        loadSettings()
        observeDatabase()
        refreshApps()
        syncFeeds()
        refreshDigitalWellbeingStats()
    }

    private fun startTimeTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                _currentTime.value = Date()
                delay(1000.milliseconds)
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            repository.allSettings.collect { settingsMap ->
                val clockStyleId = settingsMap["clock_style"] ?: "large_digital"
                val vAlignId = settingsMap["time_card_v_align"] ?: "center"
                val hAlignId = settingsMap["time_card_h_align"] ?: "center"
                val offsetX = settingsMap["time_card_offset_x"]?.toFloatOrNull() ?: 0f
                val offsetY = settingsMap["time_card_offset_y"]?.toFloatOrNull() ?: 0f
                val cardScale = settingsMap["time_card_scale"]?.toFloatOrNull() ?: 1.0f
                val fontId = settingsMap["font_family"] ?: "minimal_sans"
                val themeId = settingsMap["theme_style"] ?: "oled_black"
                val wallpaperId = settingsMap["wallpaper_id"] ?: "cyber_noir"
                val customWallpaperUri = settingsMap["custom_wallpaper_uri"]?.takeIf { it.isNotBlank() }
                val wallpaperDim = settingsMap["wallpaper_dim"]?.toFloatOrNull() ?: 0.25f
                val showMonograms = settingsMap["show_monograms"] != "false"
                val greeting = settingsMap["custom_greeting"] ?: "auto"
                val biometricLock = settingsMap["biometric_lock_enabled"] == "true"
                val gestureHints = settingsMap["show_gesture_hints"] == "true"
                val showNewsFeed = settingsMap["show_news_feed"] != "false"

                _uiState.update { state ->
                    state.copy(
                        clockStyle = ClockStyle.entries.find { it.id == clockStyleId } ?: ClockStyle.LARGE_DIGITAL,
                        timeCardVAlign = TimeCardVerticalAlign.entries.find { it.id == vAlignId } ?: TimeCardVerticalAlign.CENTER,
                        timeCardHAlign = TimeCardHorizontalAlign.entries.find { it.id == hAlignId } ?: TimeCardHorizontalAlign.CENTER,
                        timeCardOffsetX = offsetX,
                        timeCardOffsetY = offsetY,
                        timeCardScale = cardScale.coerceIn(0.5f, 2.0f),
                        fontFamily = LauncherFont.entries.find { it.id == fontId } ?: LauncherFont.MINIMAL_SANS,
                        themeMode = LauncherThemeMode.entries.find { it.id == themeId } ?: LauncherThemeMode.OLED_BLACK,
                        wallpaperId = wallpaperId,
                        customWallpaperUri = customWallpaperUri,
                        wallpaperDim = wallpaperDim,
                        showMonograms = showMonograms,
                        customGreeting = greeting,
                        isBiometricLockEnabled = biometricLock,
                        showGestureHints = gestureHints,
                        showNewsFeed = showNewsFeed,
                    )
                }
            }
        }
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            repository.allNotes.collect { notes ->
                _uiState.update { it.copy(notes = notes) }
            }
        }

        viewModelScope.launch {
            repository.allEvents.collect { events ->
                _uiState.update { it.copy(events = events) }
            }
        }

        viewModelScope.launch {
            repository.allFeeds.collect { feeds ->
                _uiState.update { it.copy(feeds = feeds) }
            }
        }

        viewModelScope.launch {
            repository.allArticles.collect { articles ->
                val seenKeys = mutableSetOf<String>()
                val distinctArticles = mutableListOf<RssArticleEntity>()
                for (article in articles) {
                    val cleanLink = article.link.trim().lowercase().removeSuffix("/")
                    val cleanTitle = article.title.lowercase().replace("[^a-z0-9]".toRegex(), "").take(35)
                    val key = cleanLink.ifBlank { cleanTitle }
                    if (key.isNotBlank() && !seenKeys.contains(key) && !seenKeys.contains(cleanTitle)) {
                        seenKeys.add(key)
                        seenKeys.add(cleanTitle)
                        distinctArticles.add(article)
                    }
                }
                _uiState.update { it.copy(articles = distinctArticles) }
            }
        }

        viewModelScope.launch {
            repository.allFocusSessions.collect { sessions ->
                _uiState.update { it.copy(focusSessions = sessions) }
                refreshDigitalWellbeingStats()
            }
        }

        viewModelScope.launch {
            repository.allCustomWallpapers.collect { entities ->
                val wallpapers = entities.map { LauncherWallpaper.fromEntity(it) }
                _uiState.update { it.copy(customWallpapers = wallpapers) }
            }
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            val pinnedIds = repository.getPinnedIds()
            val installedIds = apps.asSequence().map { it.id }.toSet()

            // Build the pinned apps matching the stored list (can be 0 to 6 apps freely)
            val pinnedList = mutableListOf<AppItem>()
            val validPinnedIds = mutableListOf<String>()

            for (id in pinnedIds) {
                if (pinnedList.size >= 6) break
                val found = apps.find { it.id == id }
                if ((found != null) && (!pinnedList.any { it.id == id })) {
                    pinnedList.add(found.copy(isPinned = true, pinIndex = pinnedList.size))
                    validPinnedIds.add(id)
                }
            }

            // Sync sanitized valid ID list back to repository if there was any ghost app
            if (pinnedIds.isNotEmpty() && (pinnedIds.size != validPinnedIds.size) && installedIds.isNotEmpty()) {
                repository.setPinnedIds(validPinnedIds)
            }

            val updatedApps = apps.map { app ->
                val pinIdx = validPinnedIds.indexOf(app.id)
                if (pinIdx != -1) app.copy(isPinned = true, pinIndex = pinIdx)
                else app.copy(isPinned = false, pinIndex = -1)
            }

            val userCategories = repository.getUserCategories()
            val categorized = updatedApps.groupBy { it.category }
            _uiState.update {
                it.copy(
                    installedApps = updatedApps,
                    pinnedApps = pinnedList,
                    categories = userCategories,
                    categorizedApps = categorized,
                )
            }
        }
    }

    fun setCategoryManagerSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showCategoryManagerSheet = visible) }
    }

    fun setAddCategoryDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAddCategoryDialog = visible) }
    }

    fun addCategory(title: String) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                repository.addCustomCategory(title)
                refreshApps()
            }
        }
    }

    fun renameCategory(categoryId: String, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.renameCategory(categoryId, newTitle)
                refreshApps()
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            refreshApps()
        }
    }

    fun toggleCategoryVisibility(categoryId: String) {
        viewModelScope.launch {
            repository.toggleCategoryVisibility(categoryId)
            refreshApps()
        }
    }

    fun reorderCategories(reorderedIds: List<String>) {
        viewModelScope.launch {
            repository.reorderCategories(reorderedIds)
            refreshApps()
        }
    }

    fun moveCategoryUp(categoryId: String) {
        val current = _uiState.value.categories
        val idx = current.indexOfFirst { it.id == categoryId }
        if (idx > 0) {
            val list = current.toMutableList()
            val item = list.removeAt(idx)
            list.add(idx - 1, item)
            reorderCategories(list.map { it.id })
        }
    }

    fun moveCategoryDown(categoryId: String) {
        val current = _uiState.value.categories
        val idx = current.indexOfFirst { it.id == categoryId }
        if (idx in (0 until (current.size - 1))) {
            val list = current.toMutableList()
            val item = list.removeAt(idx)
            list.add(idx + 1, item)
            reorderCategories(list.map { it.id })
        }
    }

    fun setAppCategory(packageName: String, categoryId: String) {
        viewModelScope.launch {
            repository.setAppCategory(packageName, categoryId)
            refreshApps()
        }
    }

    fun resetCategoriesToDefault() {
        viewModelScope.launch {
            repository.resetCategoriesToDefault()
            refreshApps()
        }
    }

    fun navigateTo(screen: LauncherScreen) {
        if (_uiState.value.isPinnedOnlyLocked && (screen != LauncherScreen.SIX_APPS)) {
            _uiState.update { it.copy(pinnedLockFeedbackMessage = "Focus Mode Locked • Triple-tap to exit") }
            return
        }
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openPinDialog(app: AppItem) {
        _uiState.update { it.copy(selectedAppForPinning = app) }
    }

    fun closePinDialog() {
        _uiState.update { it.copy(selectedAppForPinning = null) }
    }

    fun setMultiPinDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showMultiPinDialog = visible) }
    }

    fun updatePinnedApps(appIds: List<String>) {
        viewModelScope.launch {
            repository.setPinnedIds(appIds)
            refreshApps()
        }
    }

    fun pinApp(app: AppItem) {
        viewModelScope.launch {
            repository.pinApp(app.id)
            refreshApps()
        }
    }

    fun unpinApp(app: AppItem) {
        viewModelScope.launch {
            repository.unpinApp(app.id)
            refreshApps()
        }
    }

    fun launchApp(context: Context, app: AppItem) {
        try {
            val pm = context.packageManager
            var intent: Intent? = null

            // Special handling for Phone/Dialer to ensure dialpad opens directly
            val lowerPkg = app.packageName.lowercase(java.util.Locale.ROOT)
            val lowerLabel = app.label.lowercase(java.util.Locale.ROOT)
            if (lowerPkg.contains("dialer") || lowerPkg.contains("phone") || 
                lowerLabel.contains("phone") || lowerLabel.contains("dialer")) {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (dialIntent.resolveActivity(pm) != null) {
                    intent = dialIntent
                }
            }

            // Try specific launcher activity if available if not already handled
            if (intent == null && app.activityName.isNotBlank()) {
                val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(app.packageName, app.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (explicitIntent.resolveActivity(pm) != null) {
                    intent = explicitIntent
                }
            }

            // Fallback to default package launch intent
            if (intent == null) {
                intent = pm.getLaunchIntentForPackage(app.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
            }

            if (intent != null) {
                context.startActivity(intent)
            } else {
                android.widget.Toast.makeText(context, "${app.label} is not installed", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Could not open ${app.label}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun openWebUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Notes
    fun addNote(content: String) {
        viewModelScope.launch {
            repository.addNote(content)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    // Calendar
    fun addCalendarEvent(title: String, eventDate: Long, priority: String = "Normal") {
        viewModelScope.launch {
            repository.addCalendarEvent(title, eventDate, priority)
        }
    }

    fun toggleEventCompletion(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.toggleEventCompletion(event)
        }
    }

    fun deleteCalendarEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
        }
    }

    // RSS / News
    fun syncFeeds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingRss = true, rssSyncMessage = "Fetching latest articles...") }
            val count = repository.syncRssFeeds()
            _uiState.update {
                it.copy(
                    isSyncingRss = false,
                    rssSyncMessage = if (count > 0) "Synced $count articles" else "Up to date",
                )
            }
            delay(3000.milliseconds)
            _uiState.update { it.copy(rssSyncMessage = null) }
        }
    }

    fun addFeed(title: String, url: String) {
        viewModelScope.launch {
            repository.addFeed(title, url)
        }
    }

    fun toggleFeed(feed: RssFeedEntity) {
        viewModelScope.launch {
            repository.toggleFeed(feed)
            syncFeeds()
        }
    }

    fun deleteFeed(feedId: Long) {
        viewModelScope.launch {
            repository.deleteFeed(feedId)
        }
    }

    private var focusTimerJob: kotlinx.coroutines.Job? = null

    // Focus session
    fun startFocusTimer(minutes: Int) {
        focusTimerJob?.cancel()
        focusTimerJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    focusTimerSecondsLeft = minutes * 60,
                    isFocusTimerRunning = true,
                    isPinnedOnlyLocked = true,
                    currentScreen = LauncherScreen.SIX_APPS,
                    pinnedLockFeedbackMessage = "Focus Mode Locked • Triple-tap to exit",
                )
            }
            while (_uiState.value.isFocusTimerRunning && (_uiState.value.focusTimerSecondsLeft > 0)) {
                delay(1000.milliseconds)
                _uiState.update { it.copy(focusTimerSecondsLeft = it.focusTimerSecondsLeft - 1) }
            }
            if ((_uiState.value.focusTimerSecondsLeft <= 0) && _uiState.value.isFocusTimerRunning) {
                repository.recordFocusSession(minutes)
                _uiState.update {
                    it.copy(
                        isFocusTimerRunning = false,
                        isPinnedOnlyLocked = false,
                        pinnedLockFeedbackMessage = "Focus Session Completed • Unlocked",
                    )
                }
            }
        }
    }

    fun stopFocusTimer() {
        focusTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isFocusTimerRunning = false,
                focusTimerSecondsLeft = 0,
                isPinnedOnlyLocked = false,
                pinnedLockFeedbackMessage = "Focus Session Ended • Unlocked",
            )
        }
        refreshDigitalWellbeingStats()
    }

    // Digital Wellbeing & Focus Stats
    fun refreshDigitalWellbeingStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val hasPerm = DigitalWellbeingService.hasUsagePermission(app)
            val stats = DigitalWellbeingService.getWeeklyStats(app, _uiState.value.focusSessions)
            val timeAway = DigitalWellbeingService.getTimeAwayStats(app, _uiState.value.focusSessions)
            _uiState.update {
                it.copy(
                    hasUsagePermission = hasPerm,
                    weeklyFocusHistory = stats,
                    timeAwayStats = timeAway,
                )
            }
        }
    }

    fun openUsageAccessSettings(context: Context) {
        DigitalWellbeingService.openUsageSettings(context)
    }

    // Settings & Time Card Controls
    fun setClockStyle(style: ClockStyle) {
        viewModelScope.launch {
            _uiState.update { it.copy(clockStyle = style) }
            repository.updateSetting("clock_style", style.id)
        }
    }

    fun setTimeCardVAlign(vAlign: TimeCardVerticalAlign) {
        viewModelScope.launch {
            _uiState.update { it.copy(timeCardVAlign = vAlign) }
            repository.updateSetting("time_card_v_align", vAlign.id)
        }
    }

    fun setTimeCardHAlign(hAlign: TimeCardHorizontalAlign) {
        viewModelScope.launch {
            _uiState.update { it.copy(timeCardHAlign = hAlign) }
            repository.updateSetting("time_card_h_align", hAlign.id)
        }
    }

    fun setTimeCardScale(scale: Float) {
        val clamped = scale.coerceIn(0.5f, 2.0f)
        viewModelScope.launch {
            _uiState.update { it.copy(timeCardScale = clamped) }
            repository.updateSetting("time_card_scale", clamped.toString())
        }
    }

    fun setClockEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isClockEditMode = enabled) }
    }

    fun setFontFamily(font: LauncherFont) {
        viewModelScope.launch {
            _uiState.update { it.copy(fontFamily = font) }
            repository.updateSetting("font_family", font.id)
        }
    }

    fun setThemeMode(mode: LauncherThemeMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(themeMode = mode) }
            repository.updateSetting("theme_style", mode.id)
        }
    }

    fun setShowMonograms(show: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(showMonograms = show) }
            repository.updateSetting("show_monograms", show.toString())
        }
    }

    fun setCustomGreeting(greeting: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(customGreeting = greeting) }
            repository.updateSetting("custom_greeting", greeting)
        }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettingsSheet = visible) }
    }

    fun setRssManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(showRssManagerDialog = visible) }
    }

    fun setWallpaper(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(wallpaperId = id) }
            repository.updateSetting("wallpaper_id", id)
        }
    }

    fun setWallpaperDim(dim: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(wallpaperDim = dim) }
            repository.updateSetting("wallpaper_dim", dim.toString())
        }
    }

    fun setWallpaperPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(showWallpaperPicker = visible) }
    }

    fun setAtmosphericCreatorVisible(visible: Boolean) {
        _uiState.update { it.copy(showAtmosphericCreator = visible) }
    }

    fun saveAtmosphericTheme(name: String, colors: List<Long>, isDark: Boolean) {
        viewModelScope.launch {
            repository.saveCustomWallpaper(name, colors, isDark)
            setAtmosphericCreatorVisible(false)
        }
    }

    fun deleteAtmosphericTheme(id: String) {
        val longId = id.removePrefix("custom_theme_").toLongOrNull()
        if (longId != null) {
            viewModelScope.launch {
                repository.deleteCustomWallpaper(longId)
                if (_uiState.value.wallpaperId == id) {
                    setWallpaper("cyber_noir")
                }
            }
        }
    }

    fun saveCustomWallpaperFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val destinationFile = java.io.File(context.filesDir, "custom_wallpaper_image.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val savedPath = destinationFile.absolutePath
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            wallpaperId = "custom_gallery",
                            customWallpaperUri = savedPath,
                        )
                    }
                }
                repository.updateSetting("wallpaper_id", "custom_gallery")
                repository.updateSetting("custom_wallpaper_uri", savedPath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeCustomWallpaper() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    wallpaperId = "cyber_noir",
                    customWallpaperUri = null,
                )
            }
            repository.updateSetting("wallpaper_id", "cyber_noir")
            repository.updateSetting("custom_wallpaper_uri", "")
        }
    }

    // Biometric / Fingerprint Lock
    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBiometricLockEnabled = enabled) }
            repository.updateSetting("biometric_lock_enabled", enabled.toString())
        }
    }

    fun setBiometricAuthDialogVisible(visible: Boolean, error: String? = null) {
        _uiState.update {
            it.copy(
                showBiometricAuthDialog = visible,
                biometricAuthError = error,
            )
        }
    }

    fun requestAccessToAllApps() {
        if (_uiState.value.isBiometricLockEnabled) {
            _uiState.update {
                it.copy(
                    showBiometricAuthDialog = true,
                    biometricAuthError = null,
                )
            }
        } else {
            _uiState.update { it.copy(currentScreen = LauncherScreen.ALL_APPS) }
        }
    }

    fun unlockAllApps() {
        _uiState.update {
            it.copy(
                showBiometricAuthDialog = false,
                biometricAuthError = null,
                currentScreen = LauncherScreen.ALL_APPS,
            )
        }
    }

    fun setTimeCardOffset(x: Float, y: Float) {
        _uiState.update { it.copy(timeCardOffsetX = x, timeCardOffsetY = y) }
        viewModelScope.launch {
            repository.updateSetting("time_card_offset_x", x.toString())
            repository.updateSetting("time_card_offset_y", y.toString())
        }
    }

    fun resetTimeCardOffset() {
        setTimeCardOffset(0f, 0f)
    }

    fun setShowGestureHints(show: Boolean) {
        _uiState.update { it.copy(showGestureHints = show) }
        viewModelScope.launch {
            repository.updateSetting("show_gesture_hints", show.toString())
        }
    }

    fun setShowNewsFeed(show: Boolean) {
        _uiState.update { it.copy(showNewsFeed = show) }
        viewModelScope.launch {
            repository.updateSetting("show_news_feed", show.toString())
        }
    }

    fun togglePinnedOnlyLocked(): Boolean {
        val newStatus = !_uiState.value.isPinnedOnlyLocked
        _uiState.update {
            it.copy(
                isPinnedOnlyLocked = newStatus,
                currentScreen = if (newStatus) LauncherScreen.SIX_APPS else it.currentScreen,
                pinnedLockFeedbackMessage = if (newStatus) "Focus Mode Locked • Triple-tap to exit" else "Focus Mode Unlocked",
            )
        }
        return newStatus
    }

    fun clearPinnedLockFeedback() {
        _uiState.update { it.copy(pinnedLockFeedbackMessage = null) }
    }
}
