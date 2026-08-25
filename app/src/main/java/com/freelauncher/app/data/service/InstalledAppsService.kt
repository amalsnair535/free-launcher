package com.freelauncher.app.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.freelauncher.app.data.models.AppCategory
import com.freelauncher.app.data.models.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Service to retrieve installed apps using PackageManager and organize them
 * into the required categories (Communication, Media, Work, Tools) for the All Apps screen.
 */
class InstalledAppsService(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Retrieves all launchable installed apps on the device, sorted by category and label.
     */
    suspend fun getInstalledApps(pinnedIds: List<String> = emptyList()): List<AppItem> =
        withContext(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }

            val myPackageName = context.packageName
            val apps = mutableListOf<AppItem>()
            val seenIds = mutableSetOf<String>()

            for (resolveInfo in resolveInfos) {
                val activityInfo = resolveInfo.activityInfo ?: continue
                val pkgName = activityInfo.packageName ?: continue
                // Exclude the launcher itself from the app list
                if (pkgName == myPackageName) continue

                val activityName = activityInfo.name ?: ""
                val label = try {
                    resolveInfo.loadLabel(packageManager).toString().trim()
                } catch (e: Exception) {
                    pkgName
                }

                val category = categorizeApp(resolveInfo, label, pkgName)
                val monogram = generateMonogram(label, pkgName)
                val uniqueId = if (activityName.isNotBlank()) "$pkgName/$activityName" else "$pkgName#$label"

                if (seenIds.contains(uniqueId)) continue
                seenIds.add(uniqueId)

                val pinIndex = pinnedIds.indexOf(uniqueId)
                val isPinned = pinIndex != -1

                apps.add(
                    AppItem(
                        id = uniqueId,
                        packageName = pkgName,
                        activityName = activityName,
                        label = label,
                        category = category,
                        categoryId = category.name,
                        categoryTitle = category.title,
                        monogram = monogram,
                        isPinned = isPinned,
                        pinIndex = pinIndex
                    )
                )
            }

            // Fallback for minimal/test environments where no launcher activities are returned
            if (apps.isEmpty()) {
                return@withContext getFallbackApps(pinnedIds)
            }

            // Sort by category order (Communication, Media, Work, Tools) then alphabetically by label
            apps.sortedWith(
                compareBy<AppItem> { it.category.ordinal }
                    .thenBy { it.label.lowercase(Locale.ROOT) }
            )
        }

    /**
     * Retrieves installed apps grouped by the 4 required categories (Communication, Media, Work, Tools).
     */
    suspend fun getCategorizedApps(
        pinnedIds: List<String> = emptyList()
    ): Map<AppCategory, List<AppItem>> = withContext(Dispatchers.IO) {
        val apps = getInstalledApps(pinnedIds)
        val map = linkedMapOf<AppCategory, MutableList<AppItem>>()

        // Ensure all 4 required categories are present in the map in fixed order
        for (category in AppCategory.values()) {
            map[category] = mutableListOf()
        }

        for (app in apps) {
            map[app.category]?.add(app)
        }
        map
    }

    /**
     * Determines which of the 18 required categories an app belongs to.
     */
    fun categorizeApp(resolveInfo: ResolveInfo?, label: String, packageName: String): AppCategory {
        val lowerPkg = packageName.lowercase(Locale.ROOT)
        val lowerLabel = label.lowercase(Locale.ROOT)

        // 1. Android ApplicationInfo system category flag inspection (API 26+)
        if (resolveInfo?.activityInfo?.applicationInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appInfoCategory = resolveInfo.activityInfo.applicationInfo.category
            when (appInfoCategory) {
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.MEDIA
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.PHOTOGRAPHY
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.NEWS
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.TRAVEL
            }
        }

        // 2. SOCIAL: Social media and networking
        if (lowerPkg.contains("instagram") || lowerPkg.contains("facebook") || lowerPkg.contains("twitter") ||
            lowerPkg.contains("x.android") || lowerPkg.contains("snapchat") || lowerPkg.contains("reddit") ||
            lowerPkg.contains("linkedin") || lowerPkg.contains("threads") || lowerPkg.contains("tiktok") ||
            lowerLabel.contains("instagram") || lowerLabel.contains("facebook") || lowerLabel.contains("twitter") ||
            lowerLabel.contains("snapchat") || lowerLabel.contains("reddit") || lowerLabel.contains("linkedin")
        ) {
            return AppCategory.SOCIAL
        }

        // 3. COMMUNICATION: Messengers, dialers, and email
        if (lowerPkg.contains("dialer") || lowerPkg.contains("phone") || lowerPkg.contains("telecom") ||
            lowerPkg.contains("contact") || lowerPkg.contains("message") || lowerPkg.contains("mms") ||
            lowerPkg.contains("sms") || lowerPkg.contains("whatsapp") || lowerPkg.contains("telegram") ||
            lowerPkg.contains("signal") || lowerPkg.contains("messenger") || lowerPkg.contains("discord") ||
            lowerPkg.contains("viber") || lowerPkg.contains("wechat") || lowerPkg.contains("line") ||
            lowerPkg.contains("skype") || lowerPkg.contains("duo") || lowerPkg.contains("chat") ||
            lowerLabel.contains("phone") || lowerLabel.contains("messages") || lowerLabel.contains("contacts") ||
            lowerLabel.contains("whatsapp") || lowerLabel.contains("telegram") || lowerLabel.contains("signal")
        ) {
            return AppCategory.COMMUNICATION
        }

        // 4. PRODUCTIVITY: Notes, Drive, Docs, etc.
        if (lowerPkg.contains("docs") || lowerPkg.contains("sheets") || lowerPkg.contains("slides") ||
            lowerPkg.contains("drive") || lowerPkg.contains("notion") || lowerPkg.contains("keep") ||
            lowerPkg.contains("notes") || lowerPkg.contains("office") || lowerPkg.contains("evernote") ||
            lowerPkg.contains("todo") || lowerPkg.contains("tasks") || lowerPkg.contains("pdf") ||
            lowerPkg.contains("scanner") || lowerPkg.contains("reader") ||
            lowerLabel.contains("docs") || lowerLabel.contains("drive") || lowerLabel.contains("notes") ||
            lowerLabel.contains("tasks") || lowerLabel.contains("notion")
        ) {
            return AppCategory.PRODUCTIVITY
        }

        // 5. WORK: Slack, Teams, Zoom, Jira, LinkedIn (already in Social, but prioritize Work if keyword matches)
        if (lowerPkg.contains("slack") || lowerPkg.contains("teams") || lowerPkg.contains("zoom") ||
            lowerPkg.contains("webex") || lowerPkg.contains("meet") || lowerPkg.contains("trello") ||
            lowerPkg.contains("asana") || lowerPkg.contains("jira") ||
            lowerLabel.contains("slack") || lowerLabel.contains("teams") || lowerLabel.contains("zoom") ||
            lowerLabel.contains("meeting")
        ) {
            return AppCategory.WORK
        }

        // 6. FINANCE: Banking, Payments, Wallets
        if (lowerPkg.contains("bank") || lowerPkg.contains("wallet") || lowerPkg.contains("finance") ||
            lowerPkg.contains("stock") || lowerPkg.contains("portfolio") || lowerPkg.contains("pay") ||
            lowerPkg.contains("bitcoin") || lowerPkg.contains("crypto") ||
            lowerLabel.contains("bank") || lowerLabel.contains("finance") || lowerLabel.contains("wallet") ||
            lowerLabel.contains("pay")
        ) {
            return AppCategory.FINANCE
        }

        // 7. SHOPPING: E-commerce
        if (lowerPkg.contains("amazon") || lowerPkg.contains("flipkart") || lowerPkg.contains("ebay") ||
            lowerPkg.contains("shop") || lowerPkg.contains("myntra") || lowerPkg.contains("meesho") ||
            lowerLabel.contains("amazon") || lowerLabel.contains("flipkart") || lowerLabel.contains("shopping")
        ) {
            return AppCategory.SHOPPING
        }

        // 8. MEDIA: Music, Video players, Streaming
        if (lowerPkg.contains("youtube") || lowerPkg.contains("spotify") || lowerPkg.contains("music") ||
            lowerPkg.contains("vlc") || lowerPkg.contains("player") || lowerPkg.contains("audio") ||
            lowerPkg.contains("sound") || lowerPkg.contains("twitch") ||
            lowerLabel.contains("spotify") || lowerLabel.contains("youtube") || lowerLabel.contains("music") ||
            lowerLabel.contains("player")
        ) {
            return AppCategory.MEDIA
        }

        // 9. PHOTOGRAPHY: Camera and Editors
        if (lowerPkg.contains("camera") || lowerPkg.contains("gallery") || lowerPkg.contains("photo") ||
            lowerPkg.contains("snapseed") || lowerPkg.contains("lightroom") || lowerPkg.contains("editor") ||
            lowerLabel.contains("camera") || lowerLabel.contains("photos") || lowerLabel.contains("gallery")
        ) {
            return AppCategory.PHOTOGRAPHY
        }

        // 10. TRAVEL: Maps, Uber, Travel apps
        if (lowerPkg.contains("maps") || lowerPkg.contains("uber") || lowerPkg.contains("ola") ||
            lowerPkg.contains("flight") || lowerPkg.contains("hotel") || lowerPkg.contains("airbnb") ||
            lowerLabel.contains("maps") || lowerLabel.contains("uber") || lowerLabel.contains("travel")
        ) {
            return AppCategory.TRAVEL
        }

        // 11. FOOD: Delivery and Cooking
        if (lowerPkg.contains("swiggy") || lowerPkg.contains("zomato") || lowerPkg.contains("food") ||
            lowerPkg.contains("restaurant") || lowerPkg.contains("cooking") ||
            lowerLabel.contains("swiggy") || lowerLabel.contains("zomato") || lowerLabel.contains("food")
        ) {
            return AppCategory.FOOD
        }

        // 12. UTILITIES: Clock, Calculator, Files
        if (lowerPkg.contains("calculator") || lowerPkg.contains("clock") || lowerPkg.contains("files") ||
            lowerPkg.contains("recorder") || lowerPkg.contains("alarm") || lowerPkg.contains("calendar") ||
            lowerLabel.contains("calculator") || lowerLabel.contains("clock") || lowerLabel.contains("files") ||
            lowerLabel.contains("calendar")
        ) {
            return AppCategory.UTILITIES
        }

        // 13. NEWS: News and RSS
        if (lowerPkg.contains("news") || lowerPkg.contains("rss") || lowerPkg.contains("medium") ||
            lowerPkg.contains("pocket") || lowerPkg.contains("magazin") ||
            lowerLabel.contains("news") || lowerLabel.contains("rss")
        ) {
            return AppCategory.NEWS
        }

        // 14. HEALTH_FITNESS
        if (lowerPkg.contains("health") || lowerPkg.contains("fitness") || lowerPkg.contains("fit") ||
            lowerPkg.contains("workout") || lowerPkg.contains("run") || lowerPkg.contains("strava") ||
            lowerLabel.contains("health") || lowerLabel.contains("fit") || lowerLabel.contains("workout")
        ) {
            return AppCategory.HEALTH_FITNESS
        }

        // 15. EDUCATION
        if (lowerPkg.contains("education") || lowerPkg.contains("learn") || lowerPkg.contains("course") ||
            lowerPkg.contains("duolingo") || lowerPkg.contains("udemy") ||
            lowerLabel.contains("learn") || lowerLabel.contains("education")
        ) {
            return AppCategory.EDUCATION
        }

        // 16. ENTERTAINMENT
        if (lowerPkg.contains("netflix") || lowerPkg.contains("disney") || lowerPkg.contains("primevideo") ||
            lowerPkg.contains("hulu") || lowerPkg.contains("podcast") || lowerPkg.contains("cinema") ||
            lowerLabel.contains("netflix") || lowerLabel.contains("prime video") || lowerLabel.contains("entertainment")
        ) {
            return AppCategory.ENTERTAINMENT
        }

        // 17. SYSTEM
        if (lowerPkg.contains("settings") || lowerPkg.contains("android.vending") || lowerPkg.contains("packageinstaller") ||
            lowerPkg.contains("system") || lowerPkg.contains("launcher") ||
            lowerLabel.contains("settings") || lowerLabel.contains("play store")
        ) {
            return AppCategory.SYSTEM
        }

        // 18. TOOLS & Others (Default)
        if (lowerPkg.contains("browser") || lowerPkg.contains("chrome") || lowerPkg.contains("firefox") ||
            lowerPkg.contains("opera") || lowerPkg.contains("vpn") || lowerPkg.contains("scanner") ||
            lowerPkg.contains("tool") || lowerPkg.contains("util") ||
            lowerLabel.contains("browser") || lowerLabel.contains("tool")
        ) {
            return AppCategory.TOOLS
        }

        return AppCategory.TOOLS
    }

    /**
     * Generates a 1- or 2-letter uppercase monogram from an app label.
     */
    fun generateMonogram(label: String, packageName: String = ""): String {
        val lowerPkg = packageName.lowercase(Locale.ROOT)
        val lowerLabel = label.lowercase(Locale.ROOT)
        
        // Force "PH" for dialers to avoid confusion with Contacts
        if (lowerLabel == "phone" || lowerLabel == "dialer" || 
            (lowerPkg.contains("dialer") && !lowerPkg.contains("contact"))) {
            return "PH"
        }
        // Force "CO" for contacts
        if (lowerLabel == "contacts" || lowerPkg.contains("contacts")) {
            return "CO"
        }

        val words = label.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> {
                val first = words[0].take(1).uppercase(Locale.ROOT)
                val second = words[1].take(1).uppercase(Locale.ROOT)
                "$first$second"
            }
            label.length >= 2 -> label.take(2).uppercase(Locale.ROOT)
            label.isNotEmpty() -> label.take(1).uppercase(Locale.ROOT)
            else -> "AP"
        }
    }

    /**
     * Default curated app set for test / clean simulator environments across the 18 required categories.
     */
    private fun getFallbackApps(pinnedIds: List<String>): List<AppItem> {
        val defaults = listOf(
            // Communication
            Triple("com.google.android.dialer", "Phone", AppCategory.COMMUNICATION),
            Triple("com.google.android.apps.messaging", "Messages", AppCategory.COMMUNICATION),
            Triple("com.whatsapp", "WhatsApp", AppCategory.COMMUNICATION),
            
            // Social
            Triple("com.instagram.android", "Instagram", AppCategory.SOCIAL),
            Triple("com.facebook.katana", "Facebook", AppCategory.SOCIAL),
            Triple("com.twitter.android", "X", AppCategory.SOCIAL),

            // Productivity
            Triple("com.google.android.apps.docs", "Drive", AppCategory.PRODUCTIVITY),
            Triple("com.google.android.keep", "Keep Notes", AppCategory.PRODUCTIVITY),

            // Work
            Triple("com.slack", "Slack", AppCategory.WORK),
            Triple("com.microsoft.teams", "Teams", AppCategory.WORK),

            // Finance
            Triple("com.google.android.apps.nbu.paisa.user", "Google Pay", AppCategory.FINANCE),
            Triple("com.phonepe.app", "PhonePe", AppCategory.FINANCE),

            // Shopping
            Triple("com.amazon.mShop.android.shopping", "Amazon", AppCategory.SHOPPING),
            Triple("com.flipkart.android", "Flipkart", AppCategory.SHOPPING),

            // Media
            Triple("com.google.android.youtube", "YouTube", AppCategory.MEDIA),
            Triple("com.spotify.music", "Spotify", AppCategory.MEDIA),

            // Photography
            Triple("com.google.android.GoogleCamera", "Camera", AppCategory.PHOTOGRAPHY),
            Triple("com.google.android.apps.photos", "Photos", AppCategory.PHOTOGRAPHY),

            // Travel
            Triple("com.google.android.apps.maps", "Maps", AppCategory.TRAVEL),
            Triple("com.ubercab", "Uber", AppCategory.TRAVEL),

            // Food
            Triple("com.swiggy.android", "Swiggy", AppCategory.FOOD),
            Triple("com.application.zomato", "Zomato", AppCategory.FOOD),

            // Utilities
            Triple("com.google.android.calculator", "Calculator", AppCategory.UTILITIES),
            Triple("com.google.android.deskclock", "Clock", AppCategory.UTILITIES),

            // Tools
            Triple("com.android.chrome", "Chrome", AppCategory.TOOLS),
            Triple("com.google.android.apps.nbu.files", "Files", AppCategory.TOOLS),

            // News
            Triple("com.google.android.apps.magazines", "Google News", AppCategory.NEWS),

            // Games
            Triple("com.king.candycrushsaga", "Candy Crush", AppCategory.GAMES),

            // Health & Fitness
            Triple("com.google.android.apps.fitness", "Google Fit", AppCategory.HEALTH_FITNESS),

            // Education
            Triple("com.duolingo", "Duolingo", AppCategory.EDUCATION),

            // Entertainment
            Triple("com.netflix.mediaclient", "Netflix", AppCategory.ENTERTAINMENT),

            // System
            Triple("com.android.settings", "Settings", AppCategory.SYSTEM),
            Triple("com.android.vending", "Play Store", AppCategory.SYSTEM)
        )

        return defaults.map { (pkg, label, category) ->
            val pinIndex = pinnedIds.indexOf(pkg)
            AppItem(
                id = pkg,
                packageName = pkg,
                activityName = "",
                label = label,
                category = category,
                categoryId = category.name,
                categoryTitle = category.title,
                monogram = generateMonogram(label, pkg),
                isPinned = pinIndex != -1,
                pinIndex = pinIndex
            )
        }
    }
}
