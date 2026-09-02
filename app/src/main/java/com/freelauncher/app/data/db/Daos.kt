package com.freelauncher.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_events ORDER BY eventDate ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
}

@Dao
interface RssDao {
    @Query("SELECT * FROM rss_feeds ORDER BY id ASC")
    fun getAllFeeds(): Flow<List<RssFeedEntity>>

    @Query("SELECT * FROM rss_feeds WHERE isEnabled = 1")
    suspend fun getEnabledFeedsList(): List<RssFeedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: RssFeedEntity): Long

    @Update
    suspend fun updateFeed(feed: RssFeedEntity)

    @Query("DELETE FROM rss_feeds WHERE id = :id")
    suspend fun deleteFeed(id: Long)

    @Query("SELECT * FROM rss_articles ORDER BY pubTimeMillis DESC LIMIT 100")
    fun getAllArticles(): Flow<List<RssArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<RssArticleEntity>)

    @Query("DELETE FROM rss_articles WHERE feedId = :feedId")
    suspend fun deleteArticlesForFeed(feedId: Long)

    @Query("DELETE FROM rss_articles")
    suspend fun clearAllArticles()
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    @Query("SELECT SUM(sessionMinutes) FROM focus_sessions WHERE dateString = :dateString")
    suspend fun getTotalMinutesForDate(dateString: String): Int?
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM launcher_settings")
    fun getAllSettings(): Flow<List<LauncherSettingEntity>>

    @Query("SELECT value FROM launcher_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: LauncherSettingEntity)
}

@Dao
interface CustomWallpaperDao {
    @Query("SELECT * FROM custom_wallpapers ORDER BY createdAt DESC")
    fun getAllCustomWallpapers(): Flow<List<CustomWallpaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomWallpaper(wallpaper: CustomWallpaperEntity): Long

    @Query("DELETE FROM custom_wallpapers WHERE id = :id")
    suspend fun deleteCustomWallpaperById(id: Long)
}
