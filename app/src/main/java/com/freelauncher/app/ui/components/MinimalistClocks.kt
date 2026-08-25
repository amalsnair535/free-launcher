package com.freelauncher.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

enum class ClockStyle(val id: String, val displayName: String) {
    LARGE_DIGITAL("large_digital", "Large Digital"),
    THIN_DIGITAL("thin_digital", "Thin Digital"),
    MONOSPACED("monospaced", "Monospaced [10:16]"),
    MINIMAL_STACKED("minimal_stacked", "Minimal Stacked"),
    WORD_BASED("word_based", "Word-Based Clock"),
    COMPACT("compact", "Compact Digital"),
    ELEGANT_SERIF("serif", "Elegant Serif"),
    DOT_BASED("dot_based", "Minimal Dot-Based")
}

enum class TimeCardVerticalAlign(val id: String, val displayName: String) {
    TOP("top", "Top"),
    CENTER("center", "Center"),
    BOTTOM("bottom", "Bottom")
}

enum class TimeCardHorizontalAlign(val id: String, val displayName: String) {
    LEFT("left", "Left"),
    CENTER("center", "Center"),
    RIGHT("right", "Right")
}

@Composable
fun MinimalistClock(
    currentTime: Date,
    clockStyle: ClockStyle,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = clockStyle,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "clock_style_anim"
        ) { targetStyle ->
            when (targetStyle) {
                ClockStyle.LARGE_DIGITAL -> LargeDigitalClock(currentTime)
                ClockStyle.THIN_DIGITAL -> ThinDigitalClock(currentTime)
                ClockStyle.MONOSPACED -> MonospacedClock(currentTime)
                ClockStyle.MINIMAL_STACKED -> MinimalStackedClock(currentTime)
                ClockStyle.WORD_BASED -> WordBasedClock(currentTime)
                ClockStyle.COMPACT -> CompactClock(currentTime)
                ClockStyle.ELEGANT_SERIF -> ElegantSerifClock(currentTime)
                ClockStyle.DOT_BASED -> MinimalDotBasedClock(currentTime)
            }
        }
    }
}

@Composable
fun LargeDigitalClock(time: Date) {
    val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    Text(
        text = timeFormat.format(time),
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun ThinDigitalClock(time: Date) {
    val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
    Text(
        text = timeFormat.format(time),
        style = MaterialTheme.typography.displayLarge.copy(
            fontSize = 76.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 2.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MonospacedClock(time: Date) {
    val timeFormat = SimpleDateFormat("h:mm:ss", Locale.getDefault())
    Text(
        text = "[ ${timeFormat.format(time)} ]",
        style = MaterialTheme.typography.headlineLarge.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 3.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun MinimalStackedClock(time: Date) {
    val hourFormat = SimpleDateFormat("h", Locale.getDefault())
    val minFormat = SimpleDateFormat("mm", Locale.getDefault())

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = hourFormat.format(time),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 68.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 64.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = minFormat.format(time),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 68.sp,
                fontWeight = FontWeight.Light,
                lineHeight = 64.sp
            ),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WordBasedClock(time: Date) {
    val cal = Calendar.getInstance().apply { timeZone = TimeZone.getDefault(); this.time = time }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val wordText = formatTimeToWords(hour, minute)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Text(
            text = wordText.first,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Light,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        if (wordText.second.isNotEmpty()) {
            Text(
                text = wordText.second,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactClock(time: Date) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = timeFormat.format(time).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = " • ",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = dateFormat.format(time),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ElegantSerifClock(time: Date) {
    val timeFormat = SimpleDateFormat("hh : mm", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeFormat.format(time),
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 64.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = amPmFormat.format(time).lowercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Serif,
                letterSpacing = 3.sp
            ),
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun MinimalDotBasedClock(time: Date) {
    val hourFormat = SimpleDateFormat("h", Locale.getDefault())
    val minFormat = SimpleDateFormat("mm", Locale.getDefault())
    val h = hourFormat.format(time)
    val m = minFormat.format(time)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$h $m",
                style = MaterialTheme.typography.displayMedium.copy(
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Light
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Dot indicator row representing quarters of the hour
        val cal = Calendar.getInstance().apply { this.time = time }
        val currentMin = cal.get(Calendar.MINUTE)
        val activeQuarter = currentMin / 15

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..3) {
                Box(
                    modifier = Modifier
                        .size(if (i == activeQuarter) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= activeQuarter) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

private fun formatTimeToWords(hour: Int, minute: Int): Pair<String, String> {
    val hoursArray = arrayOf(
        "Twelve", "One", "Two", "Three", "Four", "Five",
        "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve"
    )
    val displayHour = hoursArray[hour % 12]
    val nextHour = hoursArray[(hour + 1) % 12]

    return when (minute) {
        0 -> Pair(displayHour, "O'CLOCK")
        15 -> Pair("Quarter Past", displayHour.uppercase(Locale.ROOT))
        30 -> Pair("Half Past", displayHour.uppercase(Locale.ROOT))
        45 -> Pair("Quarter To", nextHour.uppercase(Locale.ROOT))
        in 1..9 -> Pair(displayHour, "OH " + getNumberWord(minute).uppercase(Locale.ROOT))
        in 10..19 -> Pair(displayHour, getNumberWord(minute).uppercase(Locale.ROOT))
        in 20..29 -> Pair(displayHour, ("TWENTY " + getNumberWord(minute % 10)).trim().uppercase(Locale.ROOT))
        in 31..39 -> Pair(displayHour, ("THIRTY " + getNumberWord(minute % 10)).trim().uppercase(Locale.ROOT))
        in 40..49 -> Pair(displayHour, ("FORTY " + getNumberWord(minute % 10)).trim().uppercase(Locale.ROOT))
        in 50..59 -> Pair(displayHour, ("FIFTY " + getNumberWord(minute % 10)).trim().uppercase(Locale.ROOT))
        else -> Pair(displayHour, minute.toString())
    }
}

private fun getNumberWord(num: Int): String {
    return when (num) {
        0 -> ""
        1 -> "One"
        2 -> "Two"
        3 -> "Three"
        4 -> "Four"
        5 -> "Five"
        6 -> "Six"
        7 -> "Seven"
        8 -> "Eight"
        9 -> "Nine"
        10 -> "Ten"
        11 -> "Eleven"
        12 -> "Twelve"
        13 -> "Thirteen"
        14 -> "Fourteen"
        15 -> "Fifteen"
        16 -> "Sixteen"
        17 -> "Seventeen"
        18 -> "Eighteen"
        19 -> "Nineteen"
        else -> ""
    }
}

fun getContextualGreeting(cal: Calendar = Calendar.getInstance()): String {
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Stay present"
    }
}
