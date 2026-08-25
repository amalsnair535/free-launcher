package com.freelauncher.app.data.repository

import android.text.Html
import android.util.Xml
import com.freelauncher.app.data.db.RssArticleEntity
import com.freelauncher.app.data.db.RssFeedEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RssParser {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val dateFormats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    )

    suspend fun fetchFeedArticles(feed: RssFeedEntity): List<RssArticleEntity> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(feed.url)
                .header("User-Agent", "Mozilla/5.0 (Android; FREE-Launcher/1.0)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val xmlContent = response.body?.string() ?: return@withContext emptyList()

            return@withContext parseXml(xmlContent, feed)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun parseXml(xmlContent: String, feed: RssFeedEntity): List<RssArticleEntity> {
        val articles = mutableListOf<RssArticleEntity>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentTitle = ""
            var currentLink = ""
            var currentPubDate = ""
            var currentDescription = ""
            var insideItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase(Locale.ROOT) ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item" || tagName == "entry") {
                            insideItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentPubDate = ""
                            currentDescription = ""
                        } else if (insideItem) {
                            when (tagName) {
                                "title" -> currentTitle = parser.nextText().trim()
                                "link" -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrBlank()) {
                                        currentLink = href.trim()
                                    } else {
                                        val text = parser.nextText().trim()
                                        if (text.isNotBlank()) currentLink = text
                                    }
                                }
                                "pubdate", "published", "updated", "dc:date" -> currentPubDate = parser.nextText().trim()
                                "description", "summary", "content" -> {
                                    if (currentDescription.isBlank()) {
                                        currentDescription = cleanHtml(parser.nextText().trim())
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ((tagName == "item" || tagName == "entry") && insideItem) {
                            val cleanedTitle = cleanHtml(currentTitle)
                            val cleanedLink = cleanUrl(currentLink)
                            if (cleanedTitle.isNotBlank()) {
                                val pubMillis = parseDateToMillis(currentPubDate)
                                articles.add(
                                    RssArticleEntity(
                                        feedId = feed.id,
                                        title = cleanedTitle,
                                        link = cleanedLink,
                                        pubDateString = currentPubDate,
                                        pubTimeMillis = if (pubMillis > 0) pubMillis else System.currentTimeMillis(),
                                        sourceName = feed.title.ifBlank { "News" },
                                        description = currentDescription.take(200)
                                    )
                                )
                            }
                            insideItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Deduplicate within the same feed by normalized title and link
        return articles.distinctBy { article ->
            val normalizedLink = cleanUrl(article.link)
            val normalizedTitle = normalizeTitle(article.title)
            if (normalizedLink.isNotBlank()) normalizedLink else normalizedTitle
        }
    }

    private fun cleanHtml(html: String): String {
        return try {
            val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            spanned.toString().replace("\n+", " ").trim()
        } catch (e: Exception) {
            html.replace("<[^>]*>".toRegex(), "").trim()
        }
    }

    private fun cleanUrl(url: String): String {
        if (url.isBlank()) return ""
        var cleaned = url.trim()
        // Strip query tracking parameters like utm_source, utm_medium, etc.
        val questionMarkIndex = cleaned.indexOf('?')
        if (questionMarkIndex != -1) {
            val base = cleaned.substring(0, questionMarkIndex)
            val query = cleaned.substring(questionMarkIndex + 1)
            val keptParams = query.split("&").filterNot { param ->
                val lower = param.lowercase(Locale.ROOT)
                lower.startsWith("utm_") || lower.startsWith("feed=") || lower.startsWith("source=") || lower.startsWith("ref=")
            }
            cleaned = if (keptParams.isEmpty()) base else "$base?${keptParams.joinToString("&")}"
        }
        // Remove trailing slash
        return cleaned.removeSuffix("/")
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]".toRegex(), "")
            .trim()
    }

    private fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        for (format in dateFormats) {
            try {
                val date: Date? = format.parse(dateStr)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
