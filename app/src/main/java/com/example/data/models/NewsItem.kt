package com.example.data.models

data class NewsItem(
    val title: String = "",
    val link: String = "",
    val pubDate: String = "",
    val description: String = "",
    val source: String = "",
    val imageUrl: String = "",
    val category: String = "All"
)
