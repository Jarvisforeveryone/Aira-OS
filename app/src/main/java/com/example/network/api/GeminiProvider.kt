package com.example.network.api

import android.content.Context
import com.example.data.ApiManager

class GeminiProvider(private val context: Context) : ApiProvider {
    private val client = GeminiClient()

    override suspend fun generateResponse(query: String, context: String?): String {
        val apiManager = ApiManager.getInstance(this.context)
        val key = apiManager.getKeyForProvider(ApiProviderType.GEMINI)
        val model = apiManager.getSelectedModel(ApiProviderType.GEMINI)
        
        val result = client.generateText(key, model, query, context)
        return if (result.isSuccess) {
            result.getOrDefault("")
        } else {
            apiManager.queryAi(query, context).getOrNull()?.second ?: ""
        }
    }
}
