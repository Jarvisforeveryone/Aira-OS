package com.example.repository

import android.util.Xml
import com.example.data.models.NewsItem
import com.example.network.NewsRssService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import retrofit2.Retrofit

class NewsRepository(
    private val newsRssService: NewsRssService = createDefaultService()
) {
    suspend fun getGoogleNews(): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        try {
            val response = newsRssService.getRssFeed("https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en")
            if (response.isSuccessful && response.body() != null) {
                val inputStream = response.body()!!.byteStream()
                val items = parseRssXml(inputStream)
                if (items.isNotEmpty()) {
                    Result.success(items)
                } else {
                    Result.failure(Exception("No news items parsed from RSS feed"))
                }
            } else {
                Result.failure(Exception("HTTP Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRssXml(inputStream: InputStream): List<NewsItem> {
        val newsItems = mutableListOf<NewsItem>()
        try {
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentItem: NewsItemBuilder? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", ignoreCase = true)) {
                            currentItem = NewsItemBuilder()
                        } else if (currentItem != null) {
                            when (tagName.lowercase()) {
                                "title" -> currentItem.title = parser.nextText()
                                "link" -> currentItem.link = parser.nextText()
                                "pubdate" -> currentItem.pubDate = parser.nextText()
                                "description" -> currentItem.description = parser.nextText()
                                "source" -> currentItem.source = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", ignoreCase = true) && currentItem != null) {
                            newsItems.add(currentItem.build())
                            currentItem = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { inputStream.close() } catch (_: Exception) {}
        }
        return newsItems
    }

    private class NewsItemBuilder {
        var title: String = ""
        var link: String = ""
        var pubDate: String = ""
        var description: String = ""
        var source: String = ""

        fun build(): NewsItem {
            val cleanTitle = cleanHtml(title)
            val cleanDesc = cleanHtml(description)
            val finalSource = if (source.isNotBlank()) {
                cleanHtml(source)
            } else if (cleanTitle.contains(" - ")) {
                cleanTitle.substringAfterLast(" - ").trim()
            } else {
                "Google News"
            }
            return NewsItem(
                title = cleanTitle,
                link = link.trim(),
                pubDate = pubDate.trim(),
                description = cleanDesc,
                source = finalSource
            )
        }

        private fun cleanHtml(raw: String): String {
            if (raw.isBlank()) return ""
            return raw.replace(Regex("<.*?>"), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim()
        }
    }

    companion object {
        fun createDefaultService(): NewsRssService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://news.google.com/")
                .build()
            return retrofit.create(NewsRssService::class.java)
        }
    }
}
