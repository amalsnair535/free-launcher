package com.freelauncher.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val eventDate: Long, // timestamp in millis
    val isCompleted: Boolean = false,
    val priority: String = "Normal" // Low, Normal, High
)

@Entity(tableName = "rss_feeds")
data class RssFeedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val isEnabled: Boolean = true,
    val category: String = "News"
)

@Entity(tableName = "rss_articles")
data class RssArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val feedId: Long = 0,
    val title: String,
    val link: String,
    val pubDateString: String = "",
    val pubTimeMillis: Long = System.currentTimeMillis(),
    val sourceName: String = "News",
    val description: String = "",
    val isRead: Boolean = false
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val sessionMinutes: Int,
    val mindfulUnlocks: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "launcher_settings")
data class LauncherSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(tableName = "custom_wallpapers")
data class CustomWallpaperEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colors: List<Long>, // List of color values (ULong/Long)
    val isDark: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
