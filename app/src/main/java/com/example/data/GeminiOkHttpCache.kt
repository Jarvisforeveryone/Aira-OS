package com.example.data

import android.content.Context
import android.util.Log
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class GeminiCacheInterceptor(private val context: Context) : Interceptor {

    private val cacheDir = File(context.cacheDir, "gemini_okhttp_responses").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        // Only intercept Gemini and Groq API calls for generateContent / chat completions
        val isGeminiOrGroq = url.contains("generativelanguage.googleapis.com") || url.contains("api.groq.com")
        if (!isGeminiOrGroq || request.method.uppercase() != "POST") {
            return chain.proceed(request)
        }

        // Generate cache key based on URL endpoint path + request body hash
        val requestBodyString = try {
            val copy = request.newBuilder().build()
            val buffer = okio.Buffer()
            copy.body?.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            ""
        }

        if (requestBodyString.isEmpty()) {
            return chain.proceed(request)
        }

        val cacheKey = hashString("${request.url.encodedPath}_$requestBodyString")
        val cacheFile = File(cacheDir, "$cacheKey.json")
        val maxAgeMs = 24 * 60 * 60 * 1000L // 24 hours cache TTL

        // Check local OkHttp cache file
        if (cacheFile.exists()) {
            val age = System.currentTimeMillis() - cacheFile.lastModified()
            if (age < maxAgeMs) {
                try {
                    val cachedJson = cacheFile.readText(Charsets.UTF_8)
                    if (cachedJson.isNotEmpty()) {
                        Log.d("GeminiOkHttpCache", "OkHttp Cache HIT for Gemini request [$cacheKey]")
                        val mediaType = "application/json; charset=utf-8".toMediaType()
                        return Response.Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK (OkHttp Cache Hit)")
                            .header("X-OkHttp-Cache", "HIT")
                            .header("Cache-Control", "max-age=86400")
                            .body(cachedJson.toResponseBody(mediaType))
                            .build()
                    }
                } catch (e: Exception) {
                    Log.e("GeminiOkHttpCache", "Failed reading OkHttp cache file: ", e)
                }
            } else {
                cacheFile.delete()
            }
        }

        // Cache miss: execute network request
        val response = chain.proceed(request)

        if (response.isSuccessful && response.body != null) {
            try {
                val responseBody = response.body!!
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer.clone()
                val bodyString = buffer.readString(Charsets.UTF_8)

                if (bodyString.isNotEmpty()) {
                    cacheFile.writeText(bodyString, Charsets.UTF_8)
                    Log.d("GeminiOkHttpCache", "OkHttp Cache STORED Gemini response [$cacheKey]")
                }

                val contentType = responseBody.contentType() ?: "application/json; charset=utf-8".toMediaType()
                return response.newBuilder()
                    .header("X-OkHttp-Cache", "MISS")
                    .body(bodyString.toResponseBody(contentType))
                    .build()
            } catch (e: Exception) {
                Log.e("GeminiOkHttpCache", "Failed writing to OkHttp cache file: ", e)
            }
        }

        return response
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

object GeminiOkHttpCache {

    private var clientInstance: OkHttpClient? = null

    @Synchronized
    fun getClient(context: Context): OkHttpClient {
        if (clientInstance == null) {
            val cacheDirectory = File(context.cacheDir, "gemini_okhttp_cache")
            val cacheSize = 15L * 1024L * 1024L // 15 MiB

            clientInstance = OkHttpClient.Builder()
                .cache(Cache(cacheDirectory, cacheSize))
                .addInterceptor(GeminiCacheInterceptor(context.applicationContext))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }
        return clientInstance!!
    }
}
