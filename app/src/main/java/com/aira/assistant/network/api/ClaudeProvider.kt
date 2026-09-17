package com.aira.assistant.network.api

import android.content.Context
import com.aira.assistant.data.ApiManager

class ClaudeProvider(private val context: Context) : ApiProvider {
    private val client = ClaudeClient()

    override suspend fun generateResponse(query: String, context: String?): String {
        val apiManager = ApiManager.getInstance(this.context)
        val key = apiManager.getKeyForProvider(ApiProviderType.CLAUDE)
        val model = apiManager.getSelectedModel(ApiProviderType.CLAUDE)

        val result = client.generateText(key, model, query, context)
        return result.getOrNull() ?: ""
    }
}
