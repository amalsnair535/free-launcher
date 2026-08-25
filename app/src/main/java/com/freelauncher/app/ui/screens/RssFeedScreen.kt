package com.freelauncher.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freelauncher.app.data.db.RssArticleEntity
import com.freelauncher.app.ui.util.LauncherHaptics
import com.freelauncher.app.ui.util.TrackScrollHaptics
import com.freelauncher.app.ui.viewmodel.LauncherScreen
import com.freelauncher.app.ui.viewmodel.LauncherUiState
import java.util.concurrent.TimeUnit

@Composable
fun RssFeedScreen(
    state: LauncherUiState,
    onNavigate: (LauncherScreen) -> Unit,
    onOpenArticle: (String) -> Unit,
    onManualRefresh: () -> Unit,
    onOpenAddFeedDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var totalDragX by remember { mutableFloatStateOf(0f) }
    val lazyListState = rememberLazyListState()
    TrackScrollHaptics(lazyListState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        // Swiping right (dragging left-to-right) returns to Home
                        if (totalDragX > 70f) {
                            onNavigate(LauncherScreen.HOME)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                    }
                )
            }
            .testTag("rss_feed_screen_root")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onNavigate(LauncherScreen.HOME) },
                modifier = Modifier.testTag("rss_back_to_home")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "NEWS",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        LauncherHaptics.playClick(context)
                        onManualRefresh()
                    },
                    modifier = Modifier.testTag("rss_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Feed",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = {
                        LauncherHaptics.playClick(context)
                        onOpenAddFeedDialog()
                    },
                    modifier = Modifier.testTag("rss_add_feed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Manage Feeds",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Sync Status Message
        if (state.rssSyncMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.rssSyncMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Unique Articles with strict deduplication check
        val uniqueArticles = remember(state.articles) {
            val seen = mutableSetOf<String>()
            val list = mutableListOf<RssArticleEntity>()
            for (article in state.articles) {
                val cleanLink = article.link.trim().lowercase().removeSuffix("/")
                val cleanTitle = article.title.lowercase().replace("[^a-z0-9]".toRegex(), "").take(35)
                val key = if (cleanLink.isNotBlank()) cleanLink else cleanTitle
                if (key.isNotBlank() && !seen.contains(key) && !seen.contains(cleanTitle)) {
                    seen.add(key)
                    seen.add(cleanTitle)
                    list.add(article)
                }
            }
            list
        }

        // Article List
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uniqueArticles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (state.isSyncingRss) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Fetching latest news...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Text(
                                    text = "No articles yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onManualRefresh) {
                                    Text("Tap to Refresh")
                                }
                            }
                        }
                    }
                }
            } else {
                items(uniqueArticles, key = { it.link.ifBlank { "${it.id}#${it.title}" } }) { article ->
                    RssArticleItem(
                        article = article,
                        onClick = {
                            if (article.link.isNotBlank()) {
                                onOpenArticle(article.link)
                            }
                        }
                    )
                }

                // Add News Feed Button at bottom of feed
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onOpenAddFeedDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_feed_footer_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add News Feed", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun RssArticleItem(
    article: RssArticleEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relativeTime = remember(article.pubTimeMillis) {
        formatRelativeTime(article.pubTimeMillis)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag("article_item_${article.id}")
    ) {
        // Article Title
        Text(
            text = article.title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                lineHeight = 23.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Source & Time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = article.sourceName.ifBlank { "News" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = " • ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = relativeTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Optional short clean description
        if (article.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = article.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}

private fun formatRelativeTime(millis: Long): String {
    val diff = System.currentTimeMillis() - millis
    if (diff < 0) return "Just now"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}
