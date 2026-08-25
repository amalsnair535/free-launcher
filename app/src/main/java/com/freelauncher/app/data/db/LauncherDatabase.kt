package com.freelauncher.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        NoteEntity::class,
        CalendarEventEntity::class,
        RssFeedEntity::class,
        RssArticleEntity::class,
        FocusSessionEntity::class,
        LauncherSettingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun calendarDao(): CalendarDao
    abstract fun rssDao(): RssDao
    abstract fun focusDao(): FocusDao
    abstract fun settingDao(): SettingDao

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
