package com.example.di

import com.example.network.NetworkClient
import com.example.network.RetryInterceptor
import okhttp3.OkHttpClient

/**
 * Module providing network dependencies.
 */
object NetworkModule {

    fun provideOkHttpClient(): OkHttpClient {
        return NetworkClient.okHttpClient
    }

    fun provideRetryInterceptor(): RetryInterceptor {
        return RetryInterceptor()
    }
}
