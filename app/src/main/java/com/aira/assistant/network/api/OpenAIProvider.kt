package com.aira.assistant.network.api

import android.content.Context
import com.aira.assistant.data.ApiManager

class OpenAIProvider(private val context: Context) : ApiProvider {
    private val client = OpenAIClient()

    override suspend fun generateResponse(query: String, context: String?): String {
        val apiManager = ApiManager.getInstance(this.context)
        val key = apiManager.getKeyForProvider(ApiProviderType.OPENAI)
        val model = apiManager.getSelectedModel(ApiProviderType.OPENAI)

        val result = client.generateText(key, model, query, context)
        return result.getOrNull() ?: ""
    }
}
