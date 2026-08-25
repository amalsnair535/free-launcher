package com.freelauncher.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.freelauncher.app.ui.components.*
import com.freelauncher.app.ui.screens.*
import com.freelauncher.app.ui.theme.FreeLauncherTheme
import com.freelauncher.app.ui.theme.LauncherWallpaper
import com.freelauncher.app.ui.util.BiometricHelper
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherViewModel
import java.io.File

class MainActivity : FragmentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configure for high refresh rate performance on supported devices
        window.attributes.preferredRefreshRate = 0f // Let the system choose the best rate, typically highest for foreground apps

        // Hide Status Bar for immersive minimal launcher experience
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            val currentWallpaper = remember(state.wallpaperId) {
                LauncherWallpaper.getById(state.wallpaperId)
            }
            val isCustomActive = (state.wallpaperId == "custom_gallery") && !state.customWallpaperUri.isNullOrBlank()

            FreeLauncherTheme(
                themeMode = state.themeMode,
                launcherFont = state.fontFamily,
            ) {
                // Outer Fullscreen Canvas with Dynamic Background Wallpaper & Dim Scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // Wallpaper Layer
                    if (isCustomActive) {
                        AsyncImage(
                            model = File(state.customWallpaperUri!!),
                            contentDescription = "Device Wallpaper",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(currentWallpaper.brush),
                        )
                    }

                    // Scrim Dim overlay for ultra-crisp readable typography
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = state.wallpaperDim)),
                    )

                    // Back Press Navigation Handler
                    BackHandler(enabled = (state.currentScreen != LauncherScreen.HOME) || state.isPinnedOnlyLocked) {
                        if (state.isPinnedOnlyLocked) {
                            // Locked in focus mode: cannot exit until triple tapped
                        } else if (state.currentScreen != LauncherScreen.HOME) {
                            viewModel.navigateTo(LauncherScreen.HOME)
                        }
                    }

                    // Navigation and Screen switcher
                    AnimatedContent(
                        targetState = state.currentScreen,
                        transitionSpec = {
                            when {
                                ((initialState == LauncherScreen.HOME) && (targetState == LauncherScreen.ALL_APPS)) ->
                                    (slideInVertically { height -> height } + fadeIn())
                                        .togetherWith(slideOutVertically { height -> -height / 3 } + fadeOut())
                                ((initialState == LauncherScreen.ALL_APPS) && (targetState == LauncherScreen.HOME)) ->
                                    (slideInVertically { height -> -height / 3 } + fadeIn())
                                        .togetherWith(slideOutVertically { height -> height } + fadeOut())
                                ((initialState == LauncherScreen.HOME) && (targetState == LauncherScreen.SIX_APPS)) ->
                                    (slideInVertically { height -> -height } + fadeIn())
                                        .togetherWith(slideOutVertically { height -> height / 3 } + fadeOut())
                                ((initialState == LauncherScreen.SIX_APPS) && (targetState == LauncherScreen.HOME)) ->
                                    (slideInVertically { height -> height / 3 } + fadeIn())
                                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                                targetState == LauncherScreen.PRODUCTIVITY ->
                                    (slideInHorizontally { width -> -width } + fadeIn())
                                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                                targetState == LauncherScreen.RSS_FEED ->
                                    (slideInHorizontally { width -> width } + fadeIn())
                                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                                else ->
                                    fadeIn() togetherWith fadeOut()
                            }
                        },
                        label = "screen_navigation_animation",
                        modifier = Modifier.fillMaxSize(),
                    ) { targetScreen ->
                        when (targetScreen) {
                            LauncherScreen.HOME -> {
                                HomeScreen(
                                    state = state,
                                    onNavigate = { screen ->
                                        if (screen == LauncherScreen.ALL_APPS) {
                                            viewModel.requestAccessToAllApps()
                                        } else {
                                            viewModel.navigateTo(screen)
                                        }
                                    },
                                    onOpenSettings = { viewModel.setSettingsSheetVisible(visible = true) },
                                    onClockStyleChanged = { viewModel.setClockStyle(it) },
                                    onTimeCardOffsetChanged = { x, y -> viewModel.setTimeCardOffset(x, y) },
                                    onResetTimeCardOffset = { viewModel.resetTimeCardOffset() },
                                    onTimeCardScaleChanged = { viewModel.setTimeCardScale(it) },
                                ) { viewModel.setClockEditMode(it) }
                            }
                            LauncherScreen.SIX_APPS -> {
                                SixAppsView(
                                    state = state,
                                    onLaunchApp = { viewModel.launchApp(context, it) },
                                    onLongPressApp = { viewModel.openPinDialog(it) },
                                    onNavigate = { screen ->
                                        if (screen == LauncherScreen.ALL_APPS) {
                                            viewModel.requestAccessToAllApps()
                                        } else {
                                            viewModel.navigateTo(screen)
                                        }
                                    },
                                    onToggleLock = { viewModel.togglePinnedOnlyLocked() },
                                    onOpenMultiPin = { viewModel.setMultiPinDialogVisible(visible = true) },
                                    onClearLockFeedback = { viewModel.clearPinnedLockFeedback() },
                                )
                            }
                            LauncherScreen.ALL_APPS -> {
                                AllAppsScreen(
                                    state = state,
                                    onLaunchApp = { viewModel.launchApp(context, it) },
                                    onLongPressApp = { viewModel.openPinDialog(it) },
                                    onNavigate = { viewModel.navigateTo(it) },
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onOpenCategoryManager = { viewModel.setCategoryManagerSheetVisible(visible = true) },
                                    onOpenAddCategory = { viewModel.setAddCategoryDialogVisible(visible = true) },
                                )
                            }
                            LauncherScreen.PRODUCTIVITY -> {
                                ProductivityScreen(
                                    state = state,
                                    onNavigate = { viewModel.navigateTo(it) },
                                    onAddNote = { viewModel.addNote(it) },
                                    onDeleteNote = { viewModel.deleteNote(it) },
                                    onAddEvent = { title, date -> viewModel.addCalendarEvent(title, date) },
                                    onToggleEvent = { viewModel.toggleEventCompletion(it) },
                                    onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                                    onStartFocusTimer = { viewModel.startFocusTimer(it) },
                                    onStopFocusTimer = { viewModel.stopFocusTimer() },
                                    onRequestUsagePermission = { viewModel.openUsageAccessSettings(context) },
                                    onRefreshUsageStats = { viewModel.refreshDigitalWellbeingStats() },
                                )
                            }
                            LauncherScreen.RSS_FEED -> {
                                RssFeedScreen(
                                    state = state,
                                    onNavigate = { viewModel.navigateTo(it) },
                                    onOpenArticle = { viewModel.openWebUrl(context, it) },
                                    onManualRefresh = { viewModel.syncFeeds() },
                                    onOpenAddFeedDialog = { viewModel.setRssManagerVisible(visible = true) },
                                )
                            }
                        }
                    }

                    // Biometric Authentication Dialog
                    if (state.showBiometricAuthDialog) {
                        BiometricAuthDialog(
                            errorMessage = state.biometricAuthError,
                            onTriggerBiometric = {
                                if (BiometricHelper.isBiometricAvailable(this@MainActivity)) {
                                    BiometricHelper.authenticate(
                                        activity = this@MainActivity,
                                        title = "Unlock All Apps",
                                        subtitle = "Use fingerprint or screen lock to open app drawer",
                                        onSuccess = {
                                            viewModel.unlockAllApps()
                                        },
                                    ) { error ->
                                        viewModel.setBiometricAuthDialogVisible(visible = true, error = error)
                                    }
                                }
                            },
                            onUnlockSuccess = {
                                viewModel.unlockAllApps()
                            },
                            onDismiss = {
                                viewModel.setBiometricAuthDialogVisible(visible = false)
                            },
                        )
                    }

                    // Dialog 1.5: Multi-App Selection Dialog
                    if (state.showMultiPinDialog) {
                        MultiAppPinDialog(
                            installedApps = state.installedApps,
                            initiallyPinnedIds = state.pinnedApps.map { it.id },
                            onDismiss = { viewModel.setMultiPinDialogVisible(false) },
                            onConfirm = { ids ->
                                viewModel.updatePinnedApps(ids)
                                viewModel.setMultiPinDialogVisible(false)
                            }
                        )
                    }

                    // Dialog 2: RSS Manager Dialog
                    if (state.showRssManagerDialog) {
                        RssManagerDialog(
                            feeds = state.feeds,
                            onAddFeed = { title, url -> viewModel.addFeed(title, url) },
                            onToggleFeed = { viewModel.toggleFeed(it) },
                            onDeleteFeed = { viewModel.deleteFeed(it) },
                            onManualRefresh = { viewModel.syncFeeds() },
                        ) { viewModel.setRssManagerVisible(visible = false) }
                    }

                    // Dialog 3: Add Custom Category Dialog
                    if (state.showAddCategoryDialog) {
                        AddCategoryDialog(
                            existingCategories = state.categories,
                            onDismiss = { viewModel.setAddCategoryDialogVisible(visible = false) },
                        ) { title ->
                            viewModel.addCategory(title)
                        }
                    }

                    // Sheet 1: Comprehensive Launcher Settings Sheet
                    if (state.showSettingsSheet) {
                        SettingsSheet(
                            currentClockStyle = state.clockStyle,
                            currentTimeCardVAlign = state.timeCardVAlign,
                            currentTimeCardHAlign = state.timeCardHAlign,
                            currentTimeCardScale = state.timeCardScale,
                            currentFont = state.fontFamily,
                            currentTheme = state.themeMode,
                            currentWallpaperId = state.wallpaperId,
                            customWallpaperUri = state.customWallpaperUri,
                            showMonograms = state.showMonograms,
                            showGestureHints = state.showGestureHints,
                            currentGreeting = state.customGreeting,
                            isBiometricLockEnabled = state.isBiometricLockEnabled,
                            onClockStyleChanged = { viewModel.setClockStyle(it) },
                            onTimeCardVAlignChanged = { viewModel.setTimeCardVAlign(it) },
                            onTimeCardHAlignChanged = { viewModel.setTimeCardHAlign(it) },
                            onTimeCardScaleChanged = { viewModel.setTimeCardScale(it) },
                            onFontChanged = { viewModel.setFontFamily(it) },
                            onThemeChanged = { viewModel.setThemeMode(it) },
                            onOpenWallpaperPicker = { viewModel.setWallpaperPickerVisible(visible = true) },
                            onMonogramsToggled = { viewModel.setShowMonograms(it) },
                            onGestureHintsToggled = { viewModel.setShowGestureHints(it) },
                            onGreetingChanged = { viewModel.setCustomGreeting(it) },
                            onBiometricLockToggled = { viewModel.setBiometricLockEnabled(it) },
                            onOpenCategoryManager = { viewModel.setCategoryManagerSheetVisible(visible = true) },
                            onOpenRssManager = { viewModel.setRssManagerVisible(visible = true) },
                        ) { viewModel.setSettingsSheetVisible(visible = false) }
                    }

                    // Sheet 2: Transparent Atmosphere & Wallpaper Selector
                    if (state.showWallpaperPicker) {
                        TransparentWallpaperSheet(
                            currentWallpaperId = state.wallpaperId,
                            customWallpaperUri = state.customWallpaperUri,
                            wallpaperDim = state.wallpaperDim,
                            onSelectWallpaper = { viewModel.setWallpaper(it) },
                            onSelectCustomWallpaperUri = { viewModel.saveCustomWallpaperFromUri(it) },
                            onRemoveCustomWallpaper = { viewModel.removeCustomWallpaper() },
                            onWallpaperDimChange = { viewModel.setWallpaperDim(it) },
                        ) { viewModel.setWallpaperPickerVisible(visible = false) }
                    }

                    // Sheet 3: Full Category Manager & Restructuring Sheet
                    if (state.showCategoryManagerSheet) {
                        CategoryManagerSheet(
                            categories = state.categories,
                            installedApps = state.installedApps,
                            onAddCategory = { title ->
                                viewModel.addCategory(title)
                            },
                            onRenameCategory = { id, title ->
                                viewModel.renameCategory(id, title)
                            },
                            onDeleteCategory = { id ->
                                viewModel.deleteCategory(id)
                            },
                            onToggleCategoryVisibility = { id ->
                                viewModel.toggleCategoryVisibility(id)
                            },
                            onMoveCategoryUp = { id ->
                                viewModel.moveCategoryUp(id)
                            },
                            onMoveCategoryDown = { id ->
                                viewModel.moveCategoryDown(id)
                            },
                            onSetAppCategory = { pkg, catId ->
                                viewModel.setAppCategory(pkg, catId)
                            },
                            onResetToDefaults = {
                                viewModel.resetCategoriesToDefault()
                            },
                        ) { viewModel.setCategoryManagerSheetVisible(visible = false) }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.statusBars())
        viewModel.refreshApps()
        viewModel.refreshDigitalWellbeingStats()
    }
}
