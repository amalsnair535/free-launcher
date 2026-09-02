package com.freelauncher.app.data.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.freelauncher.app.data.db.FocusSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class FocusDayUsageData(
    val dayLetter: String,
    val dateKey: String,
    val focusScore: Int,
    val unlocks: Int,
    val screenTimeMinutes: Int,
    val isToday: Boolean,
    val focusSessionMinutes: Int = 0
)

data class LongestBreakData(
    val dayLabel: String,
    val minutes: Int
)

data class TimeAwayStats(
    val todayPhoneFreeMinutes: Int,
    val currentBreakMinutes: Int,
    val todayLongestBreakMinutes: Int,
    val weeklyPhoneFreePercentage: Int,
    val totalWeeklyReclaimedMinutes: Int,
    val weeklyHistory: List<FocusDayUsageData>,
    val longestBreaksHistory: List<LongestBreakData>
)

object DigitalWellbeingService {

    /**
     * Checks if the app has been granted PACKAGE_USAGE_STATS permission to query Digital Wellbeing stats.
     */
    fun hasUsagePermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Launches the Android system Settings screen for Usage Access so the user can grant permission.
     */
    fun openUsageSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * System packages to ignore when calculating user active screen time.
     */
    private val IGNORED_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.android.keyguard"
    )

    /**
     * Obtains real digital wellbeing usage statistics from Android's UsageStatsManager for the past 7 days.
     * Accurately computes:
     * 1. Screen Time (minutes user actually spent in foreground applications, never exceeding daily elapsed time)
     * 2. Device Unlocks (number of times keyguard was unlocked or screen turned interactive)
     * 3. Focus Score (0 - 100%) based on digital wellbeing metrics and intentional focus sessions.
     */
    fun getWeeklyStats(
        context: Context,
        focusSessions: List<FocusSessionEntity>
    ): List<FocusDayUsageData> {
        val hasPermission = hasUsagePermission(context)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayLetterFormat = SimpleDateFormat("EEEEE", Locale.getDefault())

        val results = mutableListOf<FocusDayUsageData>()
        val now = System.currentTimeMillis()

        // Query the last 7 days ending today (6 days ago -> today)
        for (i in 6 downTo 0) {
            val startCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val startTime = startCal.timeInMillis
            val maxDayEndTime = if (i == 0) now else endCal.timeInMillis
            val dateKey = dateFormat.format(startCal.time)
            val dayLetter = dayLetterFormat.format(startCal.time).take(1).uppercase(Locale.ROOT)
            val isToday = (i == 0)

            val sessionsOnDay = focusSessions.filter { it.dateString == dateKey }
            val focusSessionMins = sessionsOnDay.sumOf { it.sessionMinutes }

            var screenTimeMinutes = 0
            var unlockCount = 0

            if (hasPermission && usageStatsManager != null && maxDayEndTime > startTime) {
                try {
                    val events = usageStatsManager.queryEvents(startTime, maxDayEndTime)
                    val event = UsageEvents.Event()

                    var activePackage: String? = null
                    var activeStartTime: Long = 0L
                    var totalForegroundMillis = 0L
                    var keyguardUnlockCount = 0
                    var screenInteractiveCount = 0

                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        val eventTime = event.timeStamp.coerceIn(startTime, maxDayEndTime)

                        when (event.eventType) {
                            UsageEvents.Event.KEYGUARD_HIDDEN -> {
                                keyguardUnlockCount++
                            }
                            UsageEvents.Event.SCREEN_INTERACTIVE -> {
                                screenInteractiveCount++
                            }
                            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                            UsageEvents.Event.KEYGUARD_SHOWN -> {
                                // Screen turned off: commit any ongoing foreground duration
                                if (activePackage != null && eventTime > activeStartTime) {
                                    val duration = eventTime - activeStartTime
                                    totalForegroundMillis += duration
                                }
                                activePackage = null
                                activeStartTime = 0L
                            }
                            UsageEvents.Event.ACTIVITY_RESUMED -> {
                                val pkg = event.packageName
                                if (pkg != null && !IGNORED_PACKAGES.contains(pkg)) {
                                    // If another app was foregrounded, close its session first
                                    if (activePackage != null && eventTime > activeStartTime) {
                                        totalForegroundMillis += (eventTime - activeStartTime)
                                    }
                                    activePackage = pkg
                                    activeStartTime = eventTime
                                }
                            }
                            UsageEvents.Event.ACTIVITY_PAUSED,
                            UsageEvents.Event.ACTIVITY_STOPPED -> {
                                if (activePackage != null && activePackage == event.packageName) {
                                    if (eventTime > activeStartTime) {
                                        totalForegroundMillis += (eventTime - activeStartTime)
                                    }
                                    activePackage = null
                                    activeStartTime = 0L
                                }
                            }
                        }
                    }

                    // Account for currently active app up to maxDayEndTime
                    if (activePackage != null && activeStartTime > 0L && maxDayEndTime > activeStartTime) {
                        totalForegroundMillis += (maxDayEndTime - activeStartTime)
                    }

                    // Unlocks preference: keyguard hidden, fallback to screen interactive
                    unlockCount = if (keyguardUnlockCount > 0) {
                        keyguardUnlockCount
                    } else {
                        screenInteractiveCount
                    }

                    // Fallback to queryUsageStats if event stream was empty
                    val dayMaxMillis = (maxDayEndTime - startTime).coerceAtLeast(0L)
                    if (totalForegroundMillis == 0L) {
                        val statsList = usageStatsManager.queryUsageStats(
                            UsageStatsManager.INTERVAL_DAILY,
                            startTime,
                            maxDayEndTime
                        )
                        if (!statsList.isNullOrEmpty()) {
                            val filteredSum = statsList
                                .filter { !IGNORED_PACKAGES.contains(it.packageName) }
                                .sumOf { it.totalTimeInForeground }
                            totalForegroundMillis = filteredSum.coerceIn(0L, dayMaxMillis)
                        }
                    }

                    // Hard constraint: daily screen time can NEVER exceed the day's duration
                    totalForegroundMillis = totalForegroundMillis.coerceIn(0L, dayMaxMillis)
                    screenTimeMinutes = (totalForegroundMillis / (1000 * 60)).toInt()

                } catch (_: Exception) {}
            }

            val calculatedScore = calculateDigitalWellbeingFocusScore(
                screenTimeMinutes = screenTimeMinutes,
                unlocks = unlockCount,
                focusSessionMinutes = focusSessionMins,
                hasPermission = hasPermission,
                isToday = isToday
            )

            results.add(
                FocusDayUsageData(
                    dayLetter = dayLetter,
                    dateKey = dateKey,
                    focusScore = calculatedScore,
                    unlocks = unlockCount,
                    screenTimeMinutes = screenTimeMinutes,
                    isToday = isToday,
                    focusSessionMinutes = focusSessionMins
                )
            )
        }

        return results
    }

    /**
     * Digital Wellbeing Focus Score calculation (0 - 100%):
     * - Screen Time Health (50% max):
     *     <= 2h: 50 pts
     *     3h: ~42 pts
     *     4h: ~33 pts
     *     5h: ~25 pts
     *     6h: ~15 pts
     *     8h+: 0 pts
     * - Unlock Hygiene (30% max):
     *     <= 30 unlocks: 30 pts
     *     50 unlocks: ~24 pts
     *     80 unlocks: ~15 pts
     *     120+ unlocks: 0 pts
     * - Intentional Focus Sessions (20% bonus):
     *     +5 pts per 15 min session (up to 20 pts)
     * - Base Mindful Bonus:
     *     +10 pts baseline for mindful daily balance
     */
    fun calculateDigitalWellbeingFocusScore(
        screenTimeMinutes: Int,
        unlocks: Int,
        focusSessionMinutes: Int,
        hasPermission: Boolean,
        isToday: Boolean
    ): Int {
        if (!hasPermission && screenTimeMinutes == 0 && unlocks == 0) {
            return if (focusSessionMinutes > 0) (70 + focusSessionMinutes).coerceAtMost(100) else 0
        }

        if (screenTimeMinutes == 0 && unlocks == 0) {
            return if (isToday) 100 else 0
        }

        // 1. Screen Time Health (0 to 50 pts)
        val screenTimeScore = when {
            screenTimeMinutes <= 120 -> 50.0 // Under 2 hours is exceptional
            screenTimeMinutes >= 480 -> 0.0  // 8+ hours
            else -> 50.0 * (1.0 - (screenTimeMinutes - 120).toDouble() / 360.0)
        }

        // 2. Unlock Hygiene (0 to 30 pts)
        val unlockScore = when {
            unlocks <= 30 -> 30.0
            unlocks >= 120 -> 0.0
            else -> 30.0 * (1.0 - (unlocks - 30).toDouble() / 90.0)
        }

        // 3. Intentional Focus Sessions (0 to 20 pts)
        val sessionBonus = ((focusSessionMinutes / 15.0) * 6.0).coerceIn(0.0, 20.0)

        // 4. Mindful Balance Baseline (+10 pts)
        val mindfulBaseline = if (screenTimeMinutes in 1..240 && unlocks in 1..70) 10.0 else 0.0

        val total = (screenTimeScore + unlockScore + sessionBonus + mindfulBaseline).roundToInt()
        return total.coerceIn(1, 100)
    }

    const val DAILY_WAKING_MINUTES = 16 * 60 // 960 minutes (16 waking hours/day, assuming 8 hours sleep)
    private const val WAKE_UP_HOUR = 7       // 7:00 AM
    private const val SLEEP_HOUR = 23        // 11:00 PM

    /**
     * Calculates active waking minutes elapsed so far today (ignoring 8-hour sleep window 11 PM - 7 AM).
     */
    fun getElapsedWakingMinutesToday(): Int {
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        return when {
            currentHour < WAKE_UP_HOUR -> 0
            currentHour >= SLEEP_HOUR -> DAILY_WAKING_MINUTES
            else -> {
                val hoursSinceWake = currentHour - WAKE_UP_HOUR
                (hoursSinceWake * 60 + currentMinute).coerceIn(0, DAILY_WAKING_MINUTES)
            }
        }
    }

    /**
     * Aggregates real-time statistics for the new "Time Away" dashboard.
     */
    fun getTimeAwayStats(
        context: Context,
        focusSessions: List<FocusSessionEntity>
    ): TimeAwayStats {
        val hasPermission = hasUsagePermission(context)
        if (!hasPermission) {
            return TimeAwayStats(
                todayPhoneFreeMinutes = 0,
                currentBreakMinutes = 0,
                todayLongestBreakMinutes = 0,
                weeklyPhoneFreePercentage = 0,
                totalWeeklyReclaimedMinutes = 0,
                weeklyHistory = emptyList(),
                longestBreaksHistory = emptyList()
            )
        }

        val weeklyHistory = getWeeklyStats(context, focusSessions)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

        var todayPhoneFreeMinutes = 0
        var currentBreakMinutes = 0
        var todayLongestBreakMinutes = 0
        var totalWeeklyReclaimedMinutes = 0
        val longestBreaksHistory = mutableListOf<LongestBreakData>()

        val now = System.currentTimeMillis()

        // 1. Calculate today's reclaimed waking time (Elapsed waking time today - active screen time)
        val today = weeklyHistory.lastOrNull()
        if (today != null) {
            val wakingMinutesSoFar = getElapsedWakingMinutesToday()
            todayPhoneFreeMinutes = (wakingMinutesSoFar - today.screenTimeMinutes).coerceAtLeast(0)
        }

        // 2. Weekly total reclaimed waking time (Sum of waking phone-free time for each day)
        totalWeeklyReclaimedMinutes = weeklyHistory.sumOf { day ->
            if (day.isToday) todayPhoneFreeMinutes
            else (DAILY_WAKING_MINUTES - day.screenTimeMinutes).coerceAtLeast(0)
        }

        // 3. Process Gaps for longest breaks and current break status during waking hours (7 AM - 11 PM)
        if (usageStatsManager != null) {
            for (i in 0..3) {
                val wakingStart = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, WAKE_UP_HOUR)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val wakingEnd = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, SLEEP_HOUR)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val queryEnd = if (i == 0) now.coerceAtMost(wakingEnd) else wakingEnd

                var longestGapOnDay = 0L
                var lastScreenOffTime = -1L

                if (queryEnd > wakingStart) {
                    lastScreenOffTime = wakingStart

                    val events = usageStatsManager.queryEvents(wakingStart, queryEnd)
                    val event = UsageEvents.Event()

                    while (events.hasNextEvent()) {
                        events.getNextEvent(event)
                        val eventTime = event.timeStamp.coerceIn(wakingStart, queryEnd)
                        when (event.eventType) {
                            UsageEvents.Event.SCREEN_INTERACTIVE,
                            UsageEvents.Event.KEYGUARD_HIDDEN -> {
                                if (lastScreenOffTime > 0L) {
                                    val gap = eventTime - lastScreenOffTime
                                    if (gap > longestGapOnDay) longestGapOnDay = gap
                                    lastScreenOffTime = -1L
                                }
                            }
                            UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                            UsageEvents.Event.KEYGUARD_SHOWN -> {
                                lastScreenOffTime = eventTime
                            }
                            UsageEvents.Event.ACTIVITY_RESUMED -> {
                                if (lastScreenOffTime == wakingStart) {
                                    lastScreenOffTime = -1L
                                }
                            }
                        }
                    }

                    if (lastScreenOffTime > 0L) {
                        val finalGap = queryEnd - lastScreenOffTime
                        if (finalGap > longestGapOnDay) longestGapOnDay = finalGap
                    }
                }

                val dayLabel = when (i) {
                    0 -> "Today"
                    1 -> "Yesterday"
                    else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(wakingStart))
                }

                val gapMins = (longestGapOnDay / (1000 * 60)).toInt()
                if (gapMins > 0) {
                    longestBreaksHistory.add(LongestBreakData(dayLabel, gapMins))
                }

                if (i == 0) {
                    todayLongestBreakMinutes = gapMins
                    currentBreakMinutes = if (lastScreenOffTime > 0L) {
                        ((queryEnd - lastScreenOffTime) / (1000 * 60)).toInt().coerceAtLeast(0)
                    } else {
                        0
                    }
                }
            }
        }

        val wakingPhoneFreePct = if (today != null) {
            val elapsedWaking = getElapsedWakingMinutesToday()
            if (elapsedWaking > 0) {
                ((todayPhoneFreeMinutes.toDouble() / elapsedWaking.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            } else {
                100
            }
        } else {
            0
        }

        return TimeAwayStats(
            todayPhoneFreeMinutes = todayPhoneFreeMinutes,
            currentBreakMinutes = currentBreakMinutes,
            todayLongestBreakMinutes = todayLongestBreakMinutes,
            weeklyPhoneFreePercentage = wakingPhoneFreePct,
            totalWeeklyReclaimedMinutes = totalWeeklyReclaimedMinutes,
            weeklyHistory = weeklyHistory,
            longestBreaksHistory = longestBreaksHistory
        )
    }
}
