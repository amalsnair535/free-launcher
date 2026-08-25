package com.freelauncher.app.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext

object LauncherHaptics {

    /**
     * Plays a crisp, light tactile tick for scrolling past list items or adjusting sliders.
     */
    fun playTick(context: Context) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, 40))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Plays a slightly more pronounced click for button presses, switches, or chip selections.
     */
    fun playClick(context: Context) {
        try {
            val vibrator = getVibrator(context)
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(18, 80))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(18)
                }
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

/**
 * Attaches rich, subtle haptic feedback as the user scrolls through any LazyList.
 * Vibrates softly on every item boundary scrolled past.
 */
@Composable
fun TrackScrollHaptics(lazyListState: LazyListState) {
    val context = LocalContext.current
    LaunchedEffect(lazyListState) {
        var lastItemIndex = lazyListState.firstVisibleItemIndex
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { newIndex ->
                if (newIndex != lastItemIndex && lazyListState.isScrollInProgress) {
                    LauncherHaptics.playTick(context)
                    lastItemIndex = newIndex
                }
            }
    }
}
