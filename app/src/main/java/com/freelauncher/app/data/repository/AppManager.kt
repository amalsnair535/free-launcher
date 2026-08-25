package com.freelauncher.app.data.repository

import android.content.Context
import com.freelauncher.app.data.models.AppCategory
import com.freelauncher.app.data.models.AppItem
import com.freelauncher.app.data.service.InstalledAppsService

/**
 * AppManager acts as a repository-level facade over [InstalledAppsService]
 * to retrieve, categorize, and query installed applications on the device.
 */
class AppManager(
    private val context: Context,
    private val installedAppsService: InstalledAppsService = InstalledAppsService(context)
) {
    /**
     * Retrieves all installed applications, categorized into Communication, Media, Work, and Tools.
     */
    suspend fun getInstalledApps(pinnedIds: List<String>): List<AppItem> {
        return installedAppsService.getInstalledApps(pinnedIds)
    }

    /**
     * Retrieves all installed applications grouped into the 4 required categories.
     */
    suspend fun getCategorizedApps(pinnedIds: List<String>): Map<AppCategory, List<AppItem>> {
        return installedAppsService.getCategorizedApps(pinnedIds)
    }
}
