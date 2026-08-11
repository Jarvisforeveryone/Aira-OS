package com.example.di

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.ChatKeyManager
import com.example.data.ThemeRepository
import com.example.network.api.commonOkHttpClient
import okhttp3.OkHttpClient

/**
 * Clean Service Locator / Dependency Injection Container for Aira.
 * Provides decoupled, thread-safe access to Repositories, Databases, and Services.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val chatKeyManager: ChatKeyManager by lazy {
        ChatKeyManager.getInstance(context)
    }

    val okHttpClient: OkHttpClient by lazy {
        commonOkHttpClient
    }

    val themeRepository: ThemeRepository = ThemeRepository
}
