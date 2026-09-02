package com.freelauncher.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
            val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
            val context = LocalContext.current

            // Memoized event handlers to prevent unnecessary recompositions of sheets/screens
            val onNavigate = remember { { screen: LauncherScreen ->
                if (screen == LauncherScreen.ALL_APPS) {
                    viewModel.requestAccessToAllApps()
                } else {
                    viewModel.navigateTo(screen)
                }
            } }
            val onOpenSettings = remember { { viewModel.setSettingsSheetVisible(true) } }
            val onClockStyleChanged = remember { { style: ClockStyle -> viewModel.setClockStyle(style) } }
            val onTimeCardOffsetChanged = remember { { x: Float, y: Float -> viewModel.setTimeCardOffset(x, y) } }
            val onResetTimeCardOffset = remember { { viewModel.resetTimeCardOffset() } }
            val onTimeCardScaleChanged = remember { { scale: Float -> viewModel.setTimeCardScale(scale) } }
            val onClockEditModeToggled = remember { { enabled: Boolean -> viewModel.setClockEditMode(enabled) } }
            
            val onLaunchApp = remember { { app: com.freelauncher.app.data.models.AppItem -> viewModel.launchApp(context, app) } }
            val onLongPressApp = remember { { app: com.freelauncher.app.data.models.AppItem -> viewModel.openPinDialog(app) } }
            val onToggleLock = remember { { 
                viewModel.togglePinnedOnlyLocked()
                Unit
            } }
            val onOpenMultiPin = remember { { viewModel.setMultiPinDialogVisible(true) } }
            val onClearLockFeedback = remember { { viewModel.clearPinnedLockFeedback() } }
            
            val onSearchQueryChange = remember { { query: String -> viewModel.setSearchQuery(query) } }
            val onOpenCategoryManager = remember { { viewModel.setCategoryManagerSheetVisible(true) } }
            val onOpenAddCategory = remember { { viewModel.setAddCategoryDialogVisible(true) } }
            
            val onOpenArticle = remember { { url: String -> viewModel.openWebUrl(context, url) } }
            val onManualRefresh = remember { { viewModel.syncFeeds() } }
            val onOpenRssManager = remember { { viewModel.setRssManagerVisible(true) } }

            val onSetAppCategory = remember { { pkg: String, catId: String -> viewModel.setAppCategory(pkg, catId) } }
            val onOpenWallpaperPicker = remember { { viewModel.setWallpaperPickerVisible(true) } }
            val onSelectWallpaper = remember { { id: String -> viewModel.setWallpaper(id) } }
            val saveCustomWallpaper = remember { { uri: android.net.Uri -> viewModel.saveCustomWallpaperFromUri(uri) } }
            val removeCustomWallpaper = remember { { viewModel.removeCustomWallpaper() } }
            val setWallpaperDim = remember { { dim: Float -> viewModel.setWallpaperDim(dim) } }
            val onOpenCreator = remember { { viewModel.setAtmosphericCreatorVisible(true) } }
            val onDeleteTheme = remember { { id: String -> viewModel.deleteAtmosphericTheme(id) } }

            val currentWallpaper = remember(state.wallpaperId, state.customWallpapers) {
                state.customWallpapers.find { it.id == state.wallpaperId }
                    ?: LauncherWallpaper.getById(state.wallpaperId)
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
                            val animSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 300, easing = FastOutSlowInEasing)
                            val fadeSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)

                            when {
                                // Horizontal: Navigating to TIME_AWAY (Left screen)
                                targetState == LauncherScreen.TIME_AWAY ->
                                    (slideInHorizontally(animSpec) { -it } + fadeIn(fadeSpec))
                                        .togetherWith(slideOutHorizontally(animSpec) { it / 3 } + fadeOut(fadeSpec))

                                // Horizontal: Returning from TIME_AWAY
                                initialState == LauncherScreen.TIME_AWAY ->
                                    (slideInHorizontally(animSpec) { -it / 3 } + fadeIn(fadeSpec))
                                        .togetherWith(slideOutHorizontally(animSpec) { it } + fadeOut(fadeSpec))

                                // Horizontal: Navigating to RSS_FEED (Right screen)
                                targetState == LauncherScreen.RSS_FEED ->
                                    (slideInHorizontally(animSpec) { it } + fadeIn(fadeSpec))
                                        .togetherWith(slideOutHorizontally(animSpec) { -it / 3 } + fadeOut(fadeSpec))

                                // Horizontal: Returning from RSS_FEED
                                initialState == LauncherScreen.RSS_FEED ->
                                    (slideInHorizontally(animSpec) { -it / 3 } + fadeIn(fadeSpec))
                                        .togetherWith(slideOutHorizontally(animSpec) { it } + fadeOut(fadeSpec))

                                // Vertical: Navigating to ALL_APPS
                                targetState == LauncherScreen.ALL_APPS ->
                                    (slideInVertically(animSpec) { it } + fadeIn(fadeSpec))
                                        .togetherWith(slideOutVertically(animSpec) { -it / 3 } + fadeOut(fadeSpec))

                                // Vertical: Returning from ALL_APPS
                                initialState == LauncherScreen.ALL_APPS ->
                                    (slideInVertically(animSpec) { -it / 3 } + fadeIn(fadeSpec))
                                        .togetherWith(slideOutVertically(animSpec) { it } + fadeOut(fadeSpec))

                                // Vertical: Navigating to SIX_APPS
                                targetState == LauncherScreen.SIX_APPS ->
                                    (slideInVertically(animSpec) { it } + fadeIn(fadeSpec))
                                        .togetherWith(slideOutVertically(animSpec) { -it / 3 } + fadeOut(fadeSpec))

                                // Vertical: Returning from SIX_APPS
                                initialState == LauncherScreen.SIX_APPS ->
                                    (slideInVertically(animSpec) { -it / 3 } + fadeIn(fadeSpec))
                                        .togetherWith(slideOutVertically(animSpec) { it } + fadeOut(fadeSpec))

                                else ->
                                    (fadeIn(fadeSpec)).togetherWith(fadeOut(fadeSpec))
                            }
                        },
                        label = "screen_navigation_animation",
                        modifier = Modifier.fillMaxSize(),
                    ) { targetScreen ->
                        when (targetScreen) {
                            LauncherScreen.HOME -> {
                                HomeScreen(
                                    state = state,
                                    currentTime = currentTime,
                                    onNavigate = onNavigate,
                                    onOpenSettings = onOpenSettings,
                                    onClockStyleChanged = onClockStyleChanged,
                                    onTimeCardOffsetChanged = onTimeCardOffsetChanged,
                                    onResetTimeCardOffset = onResetTimeCardOffset,
                                    onTimeCardScaleChanged = onTimeCardScaleChanged,
                                ) { onClockEditModeToggled(it) }
                            }
                            LauncherScreen.SIX_APPS -> {
                                SixAppsView(
                                    state = state,
                                    currentTime = currentTime,
                                    onLaunchApp = onLaunchApp,
                                    onLongPressApp = onLongPressApp,
                                    onNavigate = onNavigate,
                                    onToggleLock = onToggleLock,
                                    onOpenMultiPin = onOpenMultiPin,
                                    onClearLockFeedback = onClearLockFeedback,
                                )
                            }
                            LauncherScreen.ALL_APPS -> {
                                AllAppsScreen(
                                    state = state,
                                    onLaunchApp = onLaunchApp,
                                    onLongPressApp = onLongPressApp,
                                    onNavigate = onNavigate,
                                    onSearchQueryChange = onSearchQueryChange,
                                    onOpenCategoryManager = onOpenCategoryManager,
                                    onOpenAddCategory = onOpenAddCategory,
                                )
                            }
                            LauncherScreen.RSS_FEED -> {
                                RssFeedScreen(
                                    state = state,
                                    onNavigate = onNavigate,
                                    onOpenArticle = onOpenArticle,
                                    onManualRefresh = onManualRefresh,
                                    onOpenAddFeedDialog = onOpenRssManager,
                                )
                            }
                            LauncherScreen.TIME_AWAY -> {
                                TimeAwayScreen(
                                    state = state,
                                    onNavigate = onNavigate,
                                    onOpenUsageSettings = { viewModel.openUsageAccessSettings(context) },
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

                    // Dialog 1.6: Single App Pin & Category Customization Dialog
                    if (state.selectedAppForPinning != null) {
                        val selectedApp = state.selectedAppForPinning!!
                        PinAppDialog(
                            app = selectedApp,
                            isAlreadyPinned = selectedApp.isPinned,
                            pinnedCount = state.pinnedApps.size,
                            maxSlots = 6,
                            categories = state.categories,
                            onDismiss = { viewModel.closePinDialog() },
                            onConfirmPin = { viewModel.pinApp(it) },
                            onConfirmUnpin = { viewModel.unpinApp(it) },
                            onSetCategory = onSetAppCategory
                        )
                    }

                    // Dialog 2: RSS Manager Dialog
                    if (state.showRssManagerDialog) {
                        RssManagerDialog(
                            feeds = state.feeds,
                            onAddFeed = { title, url -> viewModel.addFeed(title, url) },
                            onToggleFeed = { viewModel.toggleFeed(it) },
                            onDeleteFeed = { viewModel.deleteFeed(it) },
                            onManualRefresh = onManualRefresh,
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
                            currentFont = state.fontFamily,
                            currentTheme = state.themeMode,
                            currentWallpaperId = state.wallpaperId,
                            customWallpaperUri = state.customWallpaperUri,
                            showMonograms = state.showMonograms,
                            showGestureHints = state.showGestureHints,
                            showNewsFeed = state.showNewsFeed,
                            currentGreeting = state.customGreeting,
                            isBiometricLockEnabled = state.isBiometricLockEnabled,
                            onClockStyleChanged = onClockStyleChanged,
                            onFontChanged = { viewModel.setFontFamily(it) },
                            onThemeChanged = { viewModel.setThemeMode(it) },
                            onOpenWallpaperPicker = onOpenWallpaperPicker,
                            onMonogramsToggled = { viewModel.setShowMonograms(it) },
                            onGestureHintsToggled = { viewModel.setShowGestureHints(it) },
                            onNewsFeedToggled = { viewModel.setShowNewsFeed(it) },
                            onGreetingChanged = { viewModel.setCustomGreeting(it) },
                            onBiometricLockToggled = { viewModel.setBiometricLockEnabled(it) },
                            onOpenCategoryManager = onOpenCategoryManager,
                            onOpenRssManager = onOpenRssManager,
                        ) { viewModel.setSettingsSheetVisible(visible = false) }
                    }

                    // Sheet 2: Transparent Atmosphere & Wallpaper Selector
                    if (state.showWallpaperPicker) {
                        TransparentWallpaperSheet(
                            currentWallpaperId = state.wallpaperId,
                            customWallpaperUri = state.customWallpaperUri,
                            userWallpapers = state.customWallpapers,
                            wallpaperDim = state.wallpaperDim,
                            onSelectWallpaper = onSelectWallpaper,
                            onSelectCustomWallpaperUri = saveCustomWallpaper,
                            onRemoveCustomWallpaper = removeCustomWallpaper,
                            onWallpaperDimChange = setWallpaperDim,
                            onOpenCreator = onOpenCreator,
                            onDeleteTheme = onDeleteTheme,
                        ) { viewModel.setWallpaperPickerVisible(visible = false) }
                    }

                    // Atmospheric Theme Creator Dialog
                    if (state.showAtmosphericCreator) {
                        AtmosphericThemeCreatorDialog(
                            onSave = { name, colors, isDark ->
                                viewModel.saveAtmosphericTheme(name, colors, isDark)
                            },
                            onDismiss = { viewModel.setAtmosphericCreatorVisible(false) }
                        )
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
