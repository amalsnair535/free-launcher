package com.freelauncher.app.data.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.util.Locale

data class SettingSearchResult(
    val title: String,
    val description: String,
    val intentAction: String,
    val keywords: List<String>
)

data class ContactSearchResult(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

data class MessageSearchResult(
    val id: String,
    val senderOrRecipient: String,
    val snippet: String,
    val date: Long,
    val address: String
)

object UniversalSearchManager {

    private val SYSTEM_SETTINGS = listOf(
        SettingSearchResult(
            title = "Wi-Fi & Internet",
            description = "Manage Wi-Fi networks and connection",
            intentAction = Settings.ACTION_WIFI_SETTINGS,
            keywords = listOf("wifi", "wi-fi", "internet", "wireless", "network", "hotspot", "wlan", "broadband", "online")
        ),
        SettingSearchResult(
            title = "Bluetooth",
            description = "Pair devices, headphones, and accessories",
            intentAction = Settings.ACTION_BLUETOOTH_SETTINGS,
            keywords = listOf("bluetooth", "bt", "pair", "wireless", "audio", "headphones", "earbuds", "speaker", "device")
        ),
        SettingSearchResult(
            title = "Display & Brightness",
            description = "Dark theme, screen timeout, font size",
            intentAction = Settings.ACTION_DISPLAY_SETTINGS,
            keywords = listOf("display", "brightness", "screen", "dark", "theme", "light", "font", "timeout", "sleep", "night", "wallpaper")
        ),
        SettingSearchResult(
            title = "Sound & Vibration",
            description = "Volume, ringtones, silent mode, haptics",
            intentAction = Settings.ACTION_SOUND_SETTINGS,
            keywords = listOf("sound", "volume", "vibrate", "vibration", "ringtone", "silent", "mute", "audio", "media", "alarm", "notification", "haptic")
        ),
        SettingSearchResult(
            title = "Battery & Power",
            description = "Battery saver, battery usage and health",
            intentAction = Settings.ACTION_BATTERY_SAVER_SETTINGS,
            keywords = listOf("battery", "power", "charge", "charging", "saver", "percentage", "energy", "drain", "optimize")
        ),
        SettingSearchResult(
            title = "Apps & Notifications",
            description = "Permissions, default apps, recent apps",
            intentAction = Settings.ACTION_APPLICATION_SETTINGS,
            keywords = listOf("apps", "applications", "permissions", "default", "uninstall", "installed", "notification", "alerts")
        ),
        SettingSearchResult(
            title = "Storage & Memory",
            description = "Internal storage, free space, cached files",
            intentAction = Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            keywords = listOf("storage", "memory", "space", "ram", "disk", "clean", "files", "sd card", "internal")
        ),
        SettingSearchResult(
            title = "Security & Biometrics",
            description = "Screen lock, fingerprint, face recognition",
            intentAction = Settings.ACTION_SECURITY_SETTINGS,
            keywords = listOf("security", "lock", "screen lock", "fingerprint", "biometric", "face", "pin", "password", "pattern", "privacy")
        ),
        SettingSearchResult(
            title = "Location & GPS",
            description = "Location access, accuracy, GPS",
            intentAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            keywords = listOf("location", "gps", "maps", "tracking", "position", "geo")
        ),
        SettingSearchResult(
            title = "Accessibility",
            description = "Screen reader, magnification, interaction controls",
            intentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            keywords = listOf("accessibility", "talkback", "caption", "contrast", "vision", "hearing", "dexterity")
        ),
        SettingSearchResult(
            title = "Date & Time",
            description = "Time zone, 24-hour format, clock sync",
            intentAction = Settings.ACTION_DATE_SETTINGS,
            keywords = listOf("date", "time", "clock", "timezone", "zone", "24 hour", "format")
        ),
        SettingSearchResult(
            title = "Airplane Mode & Cellular",
            description = "Mobile networks, SIM cards, flight mode",
            intentAction = Settings.ACTION_AIRPLANE_MODE_SETTINGS,
            keywords = listOf("airplane", "flight", "cellular", "sim", "mobile data", "roaming", "carrier")
        ),
        SettingSearchResult(
            title = "Language & Input",
            description = "Keyboards, languages, voice typing",
            intentAction = Settings.ACTION_LOCALE_SETTINGS,
            keywords = listOf("language", "keyboard", "input", "typing", "speech", "dictionary", "spell")
        ),
        SettingSearchResult(
            title = "Privacy & Permissions",
            description = "Camera, microphone, and permission manager",
            intentAction = Settings.ACTION_PRIVACY_SETTINGS,
            keywords = listOf("privacy", "permission", "camera", "microphone", "mic", "access")
        ),
        SettingSearchResult(
            title = "About Phone & System",
            description = "Android version, device status, software updates",
            intentAction = Settings.ACTION_DEVICE_INFO_SETTINGS,
            keywords = listOf("about", "phone", "system", "android", "version", "update", "model", "build", "info", "device")
        ),
        SettingSearchResult(
            title = "Developer Options",
            description = "USB debugging, animation scales, dev tools",
            intentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            keywords = listOf("developer", "debug", "usb", "options", "dev", "adb", "animation")
        )
    )

    fun searchSettings(query: String): List<SettingSearchResult> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        if (cleanQuery.isBlank()) return emptyList()

        return SYSTEM_SETTINGS.filter { setting ->
            setting.title.lowercase(Locale.ROOT).contains(cleanQuery) ||
            setting.description.lowercase(Locale.ROOT).contains(cleanQuery) ||
            setting.keywords.any { it.contains(cleanQuery) || cleanQuery.contains(it) }
        }.take(5)
    }

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun searchContacts(context: Context, query: String): List<ContactSearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || !hasContactsPermission(context)) return emptyList()

        val results = mutableListOf<ContactSearchResult>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%$cleanQuery%", "%$cleanQuery%")

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenNumbers = mutableSetOf<String>()
                while (cursor.moveToNext() && results.size < 6) {
                    val id = if (idIdx != -1) cursor.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "Contact" else "Contact"
                    val number = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                    val photo = if (photoIdx != -1) cursor.getString(photoIdx) else null

                    val normalized = number.replace(Regex("[^0-9+]"), "")
                    if (normalized.isNotBlank() && !seenNumbers.contains(normalized)) {
                        seenNumbers.add(normalized)
                        results.add(ContactSearchResult(id, name, number, photo))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Searches for text messages (SMS) matching the query.
     * CRITICAL: This operation is strictly local. Content is read from the device's 
     * SMS provider and returned to the UI. No message data is transmitted off the device.
     */
    fun searchMessages(context: Context, query: String): List<MessageSearchResult> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || !hasSmsPermission(context)) return emptyList()

        val results = mutableListOf<MessageSearchResult>()
        val uri = Uri.parse("content://sms")
        val projection = arrayOf("_id", "address", "body", "date")
        val selection = "body LIKE ? OR address LIKE ?"
        val selectionArgs = arrayOf("%$cleanQuery%", "%$cleanQuery%")

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val addressIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")

                while (cursor.moveToNext() && results.size < 5) {
                    val id = if (idIdx != -1) cursor.getString(idIdx) ?: "" else ""
                    val address = if (addressIdx != -1) cursor.getString(addressIdx) ?: "SMS" else "SMS"
                    val body = if (bodyIdx != -1) cursor.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L

                    if (body.isNotBlank()) {
                        results.add(MessageSearchResult(id, address, body, date, address))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return results
    }

    fun getYouTubeSearchIntent(query: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("vnd.youtube:results?search_query=${Uri.encode(query)}")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    fun getYouTubeWebSearchIntent(query: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }
}
