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
    fun getCategoryRssUrl(category: String): String {
        return when (category.lowercase()) {
            "education" -> "https://news.google.com/rss/search?q=education&hl=en-US&gl=US&ceid=US:en"
            "finance" -> "https://news.google.com/rss/headlines/section/topic/BUSINESS?hl=en-US&gl=US&ceid=US:en"
            "technology" -> "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en"
            "sports" -> "https://news.google.com/rss/headlines/section/topic/SPORTS?hl=en-US&gl=US&ceid=US:en"
            "health" -> "https://news.google.com/rss/headlines/section/topic/HEALTH?hl=en-US&gl=US&ceid=US:en"
            "entertainment" -> "https://news.google.com/rss/headlines/section/topic/ENTERTAINMENT?hl=en-US&gl=US&ceid=US:en"
            else -> "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"
        }
    }

    suspend fun getGoogleNews(category: String = "All"): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        try {
            val url = getCategoryRssUrl(category)
            val response = newsRssService.getRssFeed(url)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val inputStream = body.byteStream()
                val items = parseRssXml(inputStream, category)
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

    private fun parseRssXml(inputStream: InputStream, category: String): List<NewsItem> {
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
                            currentItem = NewsItemBuilder(category)
                        } else if (currentItem != null) {
                            when (tagName.lowercase()) {
                                "title" -> currentItem.title = parser.nextText()
                                "link" -> currentItem.link = parser.nextText()
                                "pubdate" -> currentItem.pubDate = parser.nextText()
                                "description" -> {
                                    val descText = parser.nextText()
                                    currentItem.description = descText
                                    if (currentItem.imageUrl.isBlank()) {
                                        val imgMatch = Regex("""<img[^>]+src=["']([^"']+)["']""").find(descText)
                                        if (imgMatch != null) {
                                            currentItem.imageUrl = imgMatch.groupValues[1]
                                        }
                                    }
                                }
                                "source" -> currentItem.source = parser.nextText()
                                "media:content", "media:thumbnail", "enclosure" -> {
                                    val urlAttr = parser.getAttributeValue(null, "url")
                                    if (!urlAttr.isNullOrBlank()) {
                                        currentItem.imageUrl = urlAttr
                                    }
                                }
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

    private class NewsItemBuilder(private val category: String) {
        var title: String = ""
        var link: String = ""
        var pubDate: String = ""
        var description: String = ""
        var source: String = ""
        var imageUrl: String = ""

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

            // Fallback placeholder image URL based on category or index if RSS didn't specify one
            val finalImageUrl = if (imageUrl.isNotBlank()) imageUrl else getCategoryFallbackImage(category, cleanTitle)

            return NewsItem(
                title = cleanTitle,
                link = link.trim(),
                pubDate = pubDate.trim(),
                description = cleanDesc,
                source = finalSource,
                imageUrl = finalImageUrl,
                category = category
            )
        }

        private fun getCategoryFallbackImage(cat: String, titleText: String): String {
            val hash = Math.abs(titleText.hashCode()) % 5
            return when (cat.lowercase()) {
                "technology" -> "https://images.unsplash.com/photo-1518770660439-4636190af475?w=200&auto=format&fit=crop"
                "finance" -> "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=200&auto=format&fit=crop"
                "education" -> "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?w=200&auto=format&fit=crop"
                "sports" -> "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=200&auto=format&fit=crop"
                "health" -> "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=200&auto=format&fit=crop"
                "entertainment" -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=200&auto=format&fit=crop"
                else -> when (hash) {
                    0 -> "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=200&auto=format&fit=crop"
                    1 -> "https://images.unsplash.com/photo-1495020689067-958852a7765e?w=200&auto=format&fit=crop"
                    2 -> "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=200&auto=format&fit=crop"
                    3 -> "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=200&auto=format&fit=crop"
                    else -> "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=200&auto=format&fit=crop"
                }
            }
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
