package com.example.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url

interface NewsRssService {
    @GET
    suspend fun getRssFeed(
        @Url url: String = "https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en"
    ): Response<ResponseBody>
}
