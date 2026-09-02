package com.freelauncher.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ColorListConverter {
    @TypeConverter
    fun fromList(list: List<Long>): String = list.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<Long> = if (data.isEmpty()) emptyList() else data.split(",").map { it.toLong() }
}

@Database(
    entities = [
        NoteEntity::class,
        CalendarEventEntity::class,
        RssFeedEntity::class,
        RssArticleEntity::class,
        FocusSessionEntity::class,
        LauncherSettingEntity::class,
        CustomWallpaperEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(ColorListConverter::class)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun calendarDao(): CalendarDao
    abstract fun rssDao(): RssDao
    abstract fun focusDao(): FocusDao
    abstract fun settingDao(): SettingDao
    abstract fun customWallpaperDao(): CustomWallpaperDao

    companion object {
        @Volatile
        private var INSTANCE: LauncherDatabase? = null

        fun getDatabase(context: Context): LauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LauncherDatabase::class.java,
                    "free_launcher_database"
                )
                    .fallbackToDestructiveMigration() // Simple for dev, resets DB on version bump
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate default RSS feeds & initial notes
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.rssDao().insertFeed(
                                    RssFeedEntity(
                                        title = "Android Authority",
                                        url = "https://www.androidauthority.com/feed/",
                                        isEnabled = true,
                                        category = "News"
                                    )
                                )
                                database.rssDao().insertFeed(
                                    RssFeedEntity(
                                        title = "Ars Technica",
                                        url = "https://feeds.arstechnica.com/arstechnica/index",
                                        isEnabled = true,
                                        category = "News"
                                    )
                                )
                                database.rssDao().insertFeed(
                                    RssFeedEntity(
                                        title = "Hacker News",
                                        url = "https://news.ycombinator.com/rss",
                                        isEnabled = true,
                                        category = "News"
                                    )
                                )
                                database.rssDao().insertFeed(
                                    RssFeedEntity(
                                        title = "The Verge",
                                        url = "https://www.theverge.com/rss/index.xml",
                                        isEnabled = true,
                                        category = "News"
                                    )
                                )
                                database.rssDao().insertFeed(
                                    RssFeedEntity(
                                        title = "9to5Google",
                                        url = "https://9to5google.com/feed/",
                                        isEnabled = true,
                                        category = "News"
                                    )
                                )

                                // Pre-populate sample mindful note
                                database.noteDao().insertNote(
                                    NoteEntity(
                                        content = "Focus on what matters most today. Less screen time, more presence.",
                                        isPinned = true
                                    )
                                )

                                // Initial settings
                                database.settingDao().setSetting(
                                    LauncherSettingEntity("clock_style", "large_digital")
                                )
                                database.settingDao().setSetting(
                                    LauncherSettingEntity("time_card_v_align", "center")
                                )
                                database.settingDao().setSetting(
                                    LauncherSettingEntity("time_card_h_align", "center")
                                )
                                database.settingDao().setSetting(
                                    LauncherSettingEntity("time_card_scale", "1.0")
                                )
                                database.settingDao().setSetting(
                                    LauncherSettingEntity("font_family", "minimal_sans")
                                )
                                database.settingDao().setSetting(
                                    LauncherSettingEntity("theme_style", "oled_black")
                                )
                                database.settingDao().setSetting(
                                    LauncherSettingEntity("wallpaper_id", "cyber_noir")
                                )
                                database.settingDao().setSetting(
                                    LauncherSettingEntity("show_monograms", "true")
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
