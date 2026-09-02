package com.freelauncher.app.data.repository

import android.content.Context
import com.freelauncher.app.data.db.*
import com.freelauncher.app.data.models.AppCategory
import com.freelauncher.app.data.models.AppCategoryInfo
import com.freelauncher.app.data.models.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LauncherRepository(
    private val context: Context,
    private val database: LauncherDatabase = LauncherDatabase.getDatabase(context),
    private val appManager: AppManager = AppManager(context),
    private val rssParser: RssParser = RssParser()
) {
    private val noteDao = database.noteDao()
    private val calendarDao = database.calendarDao()
    private val rssDao = database.rssDao()
    private val focusDao = database.focusDao()
    private val settingDao = database.settingDao()
    private val customWallpaperDao = database.customWallpaperDao()

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val allEvents: Flow<List<CalendarEventEntity>> = calendarDao.getAllEvents()
    val allFeeds: Flow<List<RssFeedEntity>> = rssDao.getAllFeeds()
    val allArticles: Flow<List<RssArticleEntity>> = rssDao.getAllArticles().map { list ->
        deduplicateArticles(list)
    }
    val allSettings: Flow<Map<String, String>> = settingDao.getAllSettings().map { list ->
        list.associate { it.key to it.value }
    }
    val allFocusSessions: Flow<List<FocusSessionEntity>> = focusDao.getAllSessions()
    val allCustomWallpapers: Flow<List<CustomWallpaperEntity>> = customWallpaperDao.getAllCustomWallpapers()

    fun getDefaultCategories(): List<AppCategoryInfo> = listOf(
        AppCategoryInfo("COMMUNICATION", "COMMUNICATION", isCustom = false, isHidden = false, order = 0),
        AppCategoryInfo("SOCIAL", "SOCIAL", isCustom = false, isHidden = false, order = 1),
        AppCategoryInfo("PRODUCTIVITY", "PRODUCTIVITY", isCustom = false, isHidden = false, order = 2),
        AppCategoryInfo("WORK", "WORK", isCustom = false, isHidden = false, order = 3),
        AppCategoryInfo("FINANCE", "FINANCE", isCustom = false, isHidden = false, order = 4),
        AppCategoryInfo("SHOPPING", "SHOPPING", isCustom = false, isHidden = false, order = 5),
        AppCategoryInfo("MEDIA", "MEDIA", isCustom = false, isHidden = false, order = 6),
        AppCategoryInfo("PHOTOGRAPHY", "PHOTOGRAPHY", isCustom = false, isHidden = false, order = 7),
        AppCategoryInfo("TRAVEL", "TRAVEL", isCustom = false, isHidden = false, order = 8),
        AppCategoryInfo("FOOD", "FOOD", isCustom = false, isHidden = false, order = 9),
        AppCategoryInfo("UTILITIES", "UTILITIES", isCustom = false, isHidden = false, order = 10),
        AppCategoryInfo("TOOLS", "TOOLS", isCustom = false, isHidden = false, order = 11),
        AppCategoryInfo("NEWS", "NEWS", isCustom = false, isHidden = false, order = 12),
        AppCategoryInfo("GAMES", "GAMES", isCustom = false, isHidden = false, order = 13),
        AppCategoryInfo("HEALTH_FITNESS", "HEALTH & FITNESS", isCustom = false, isHidden = false, order = 14),
        AppCategoryInfo("EDUCATION", "EDUCATION", isCustom = false, isHidden = false, order = 15),
        AppCategoryInfo("ENTERTAINMENT", "ENTERTAINMENT", isCustom = false, isHidden = false, order = 16),
        AppCategoryInfo("SYSTEM", "SYSTEM", isCustom = false, isHidden = false, order = 17)
    )

    suspend fun getUserCategories(): List<AppCategoryInfo> = withContext(Dispatchers.IO) {
        val raw = settingDao.getSettingValue("user_categories_v2")
        if (raw.isNullOrBlank()) {
            val defaults = getDefaultCategories()
            saveUserCategories(defaults)
            defaults
        } else {
            try {
                val array = JSONArray(raw)
                val list = mutableListOf<AppCategoryInfo>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AppCategoryInfo(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            isCustom = obj.optBoolean("isCustom", false),
                            isHidden = obj.optBoolean("isHidden", false),
                            order = obj.optInt("order", i)
                        )
                    )
                }
                
                // Migration check: if user only has 4 default categories, update to new defaults
                if (list.size == 4 && list.all { !it.isCustom && it.id in listOf("COMMUNICATION", "MEDIA", "WORK", "TOOLS") }) {
                    val defaults = getDefaultCategories()
                    saveUserCategories(defaults)
                    return@withContext defaults
                }
                
                if (list.isEmpty()) getDefaultCategories() else list.sortedBy { it.order }
            } catch (e: Exception) {
                getDefaultCategories()
            }
        }
    }

    suspend fun saveUserCategories(categories: List<AppCategoryInfo>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        categories.forEachIndexed { index, cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("title", cat.title)
            obj.put("isCustom", cat.isCustom)
            obj.put("isHidden", cat.isHidden)
            obj.put("order", index)
            array.put(obj)
        }
        settingDao.setSetting(LauncherSettingEntity("user_categories_v2", array.toString()))
    }

    suspend fun addCustomCategory(title: String): AppCategoryInfo = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim().uppercase(Locale.ROOT)
        val current = getUserCategories().toMutableList()
        val newId = "CAT_" + UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)
        val newCat = AppCategoryInfo(
            id = newId,
            title = cleanTitle,
            isCustom = true,
            isHidden = false,
            order = current.size
        )
        current.add(newCat)
        saveUserCategories(current)
        newCat
    }

    suspend fun renameCategory(categoryId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val cleanTitle = newTitle.trim().uppercase(Locale.ROOT)
        if (cleanTitle.isBlank()) return@withContext
        val current = getUserCategories().map {
            if (it.id == categoryId) it.copy(title = cleanTitle) else it
        }
        saveUserCategories(current)
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        val current = getUserCategories().filter { it.id != categoryId }
        val reIndexed = current.mapIndexed { idx, item -> item.copy(order = idx) }
        saveUserCategories(reIndexed)

        // Re-route any app overrides that pointed to this deleted category to TOOLS
        val overrides = getAppCategoryOverrides().toMutableMap()
        var changed = false
        overrides.forEach { (pkg, catId) ->
            if (catId == categoryId) {
                overrides[pkg] = "TOOLS"
                changed = true
            }
        }
        if (changed) {
            saveAppCategoryOverrides(overrides)
        }
    }

    suspend fun toggleCategoryVisibility(categoryId: String) = withContext(Dispatchers.IO) {
        val current = getUserCategories().map {
            if (it.id == categoryId) it.copy(isHidden = !it.isHidden) else it
        }
        saveUserCategories(current)
    }

    suspend fun reorderCategories(reorderedIds: List<String>) = withContext(Dispatchers.IO) {
        val currentMap = getUserCategories().associateBy { it.id }
        val reordered = mutableListOf<AppCategoryInfo>()
        reorderedIds.forEachIndexed { index, id ->
            currentMap[id]?.let {
                reordered.add(it.copy(order = index))
            }
        }
        // Append any categories that weren't in reorderedIds
        currentMap.values.filter { it.id !in reorderedIds }.forEach {
            reordered.add(it.copy(order = reordered.size))
        }
        saveUserCategories(reordered)
    }

    suspend fun resetCategoriesToDefault() = withContext(Dispatchers.IO) {
        val defaults = getDefaultCategories()
        saveUserCategories(defaults)
        settingDao.setSetting(LauncherSettingEntity("app_category_overrides", "{}"))
    }

    suspend fun getAppCategoryOverrides(): Map<String, String> = withContext(Dispatchers.IO) {
        val raw = settingDao.getSettingValue("app_category_overrides")
        if (raw.isNullOrBlank()) return@withContext emptyMap()
        try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun saveAppCategoryOverrides(overrides: Map<String, String>) = withContext(Dispatchers.IO) {
        val obj = JSONObject()
        overrides.forEach { (pkg, catId) ->
            obj.put(pkg, catId)
        }
        settingDao.setSetting(LauncherSettingEntity("app_category_overrides", obj.toString()))
    }

    suspend fun setAppCategory(packageName: String, categoryId: String) = withContext(Dispatchers.IO) {
        val current = getAppCategoryOverrides().toMutableMap()
        current[packageName] = categoryId
        saveAppCategoryOverrides(current)
    }

    suspend fun getInstalledApps(): List<AppItem> {
        val pinned = getPinnedIds()
        val rawApps = appManager.getInstalledApps(pinned)
        val categories = getUserCategories()
        val categoryMap = categories.associateBy { it.id }
        val overrides = getAppCategoryOverrides()

        return rawApps.map { app ->
            val overriddenCatId = overrides[app.packageName]
            if (overriddenCatId != null && categoryMap.containsKey(overriddenCatId)) {
                val catInfo = categoryMap[overriddenCatId]!!
                app.copy(
                    categoryId = catInfo.id,
                    categoryTitle = catInfo.title,
                    category = AppCategory.fromId(catInfo.id)
                )
            } else {
                // If default category was renamed or exists
                val matchedInfo = categoryMap[app.categoryId] ?: categoryMap.values.firstOrNull { it.id == app.category.name }
                if (matchedInfo != null) {
                    app.copy(
                        categoryId = matchedInfo.id,
                        categoryTitle = matchedInfo.title
                    )
                } else {
                    app
                }
            }
        }
    }

    suspend fun getPinnedIds(): List<String> = withContext(Dispatchers.IO) {
        val raw = settingDao.getSettingValue("pinned_apps")
        if (raw == null) {
            // First-time: Start with 0 pinned apps as requested
            val defaults = emptyList<String>()
            settingDao.setSetting(LauncherSettingEntity("pinned_apps", ""))
            defaults
        } else if (raw.isBlank()) {
            // User explicitly unpinned all apps -> empty list
            emptyList()
        } else {
            raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(6)
        }
    }

    suspend fun pinApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        val installedApps = appManager.getInstalledApps(emptyList())
        val installedIds = installedApps.map { it.id }.toSet()

        // Filter out ghost or uninstalled IDs so they never lock out real slots
        val current = getPinnedIds()
            .filter { id -> installedIds.isEmpty() || installedIds.contains(id) }
            .toMutableList()

        if (current.contains(appId)) return@withContext false

        if (current.size >= 6) {
            // Replace the 6th slot when all 6 slots are actively filled
            current[5] = appId
        } else {
            current.add(appId)
        }
        val limited = current.take(6)
        settingDao.setSetting(LauncherSettingEntity("pinned_apps", limited.joinToString(",")))
        true
    }

    suspend fun unpinApp(appId: String): Boolean = withContext(Dispatchers.IO) {
        val current = getPinnedIds().toMutableList()
        if (!current.contains(appId)) return@withContext false
        current.remove(appId)
        // Store the exact remaining pinned list without auto-filling or adding any other apps
        settingDao.setSetting(LauncherSettingEntity("pinned_apps", current.joinToString(",")))
        true
    }

    suspend fun setPinnedIds(ids: List<String>) = withContext(Dispatchers.IO) {
        val limited = ids.take(6)
        settingDao.setSetting(LauncherSettingEntity("pinned_apps", limited.joinToString(",")))
    }

    // Notes
    suspend fun addNote(content: String) = withContext(Dispatchers.IO) {
        if (content.isNotBlank()) {
            noteDao.insertNote(NoteEntity(content = content.trim()))
        }
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    // Calendar
    suspend fun addCalendarEvent(title: String, eventDate: Long, priority: String = "Normal") = withContext(Dispatchers.IO) {
        if (title.isNotBlank()) {
            calendarDao.insertEvent(CalendarEventEntity(title = title.trim(), eventDate = eventDate, priority = priority))
        }
    }

    suspend fun toggleEventCompletion(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        calendarDao.updateEvent(event.copy(isCompleted = !event.isCompleted))
    }

    suspend fun deleteCalendarEvent(id: Long) = withContext(Dispatchers.IO) {
        calendarDao.deleteEventById(id)
    }

    // RSS / News
    suspend fun syncRssFeeds(): Int = withContext(Dispatchers.IO) {
        val feeds = rssDao.getEnabledFeedsList()
        var newArticlesCount = 0
        val allFetched = mutableListOf<RssArticleEntity>()
        for (feed in feeds) {
            val articles = rssParser.fetchFeedArticles(feed)
            allFetched.addAll(articles)
        }
        if (allFetched.isNotEmpty()) {
            // Deduplicate fetched news articles by normalized link and normalized title
            val distinctArticles = deduplicateArticles(allFetched)
            rssDao.clearAllArticles()
            rssDao.insertArticles(distinctArticles)
            newArticlesCount = distinctArticles.size
        }
        newArticlesCount
    }

    private fun deduplicateArticles(articles: List<RssArticleEntity>): List<RssArticleEntity> {
        val seenKeys = mutableSetOf<String>()
        val result = mutableListOf<RssArticleEntity>()
        for (article in articles) {
            val cleanLink = article.link.trim().lowercase().removeSuffix("/")
            val cleanTitle = article.title.lowercase()
                .replace("[^a-z0-9]".toRegex(), "")
                .take(40)
            val key = if (cleanLink.isNotBlank()) cleanLink else cleanTitle
            if (key.isNotBlank() && !seenKeys.contains(key) && !seenKeys.contains(cleanTitle)) {
                seenKeys.add(key)
                seenKeys.add(cleanTitle)
                result.add(article)
            }
        }
        return result
    }

    suspend fun addFeed(title: String, url: String) = withContext(Dispatchers.IO) {
        if (url.isNotBlank()) {
            val feedId = rssDao.insertFeed(
                RssFeedEntity(
                    title = if (title.isNotBlank()) title.trim() else "News",
                    url = url.trim(),
                    isEnabled = true,
                    category = "News"
                )
            )
            // Fetch immediately
            val articles = rssParser.fetchFeedArticles(RssFeedEntity(id = feedId, title = title, url = url, category = "News"))
            if (articles.isNotEmpty()) {
                val deduped = deduplicateArticles(articles)
                rssDao.insertArticles(deduped)
            }
        }
    }

    suspend fun toggleFeed(feed: RssFeedEntity) = withContext(Dispatchers.IO) {
        rssDao.updateFeed(feed.copy(isEnabled = !feed.isEnabled))
    }

    suspend fun deleteFeed(feedId: Long) = withContext(Dispatchers.IO) {
        rssDao.deleteArticlesForFeed(feedId)
        rssDao.deleteFeed(feedId)
    }

    // Focus
    suspend fun recordFocusSession(minutes: Int) = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        focusDao.insertSession(
            FocusSessionEntity(
                dateString = today,
                sessionMinutes = minutes
            )
        )
    }

    // Settings
    suspend fun updateSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        settingDao.setSetting(LauncherSettingEntity(key, value))
    }

    // Custom Wallpapers
    suspend fun saveCustomWallpaper(name: String, colors: List<Long>, isDark: Boolean) = withContext(Dispatchers.IO) {
        customWallpaperDao.insertCustomWallpaper(
            CustomWallpaperEntity(
                name = name,
                colors = colors,
                isDark = isDark
            )
        )
    }

    suspend fun deleteCustomWallpaper(id: Long) = withContext(Dispatchers.IO) {
        customWallpaperDao.deleteCustomWallpaperById(id)
    }
}
