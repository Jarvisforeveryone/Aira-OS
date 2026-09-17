package com.aira.assistant.network

import com.aira.assistant.network.api.commonOkHttpClient
import okhttp3.OkHttpClient

/**
 * Centralized singleton providing standard OkHttpClient instance.
 */
object NetworkClient {
    val okHttpClient: OkHttpClient get() = commonOkHttpClient
}
