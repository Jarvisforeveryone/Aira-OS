package com.example.network.api

import android.content.Context
import com.example.data.ApiManager

class ClaudeProvider(private val context: Context) : ApiProvider {
    private val client = ClaudeClient()

    override suspend fun generateResponse(query: String, context: String?): String {
        val apiManager = ApiManager.getInstance(this.context)
        val key = apiManager.getKeyForProvider(ApiProviderType.CLAUDE)
        val model = apiManager.getSelectedModel(ApiProviderType.CLAUDE)

        val result = client.generateText(key, model, query, context)
        return if (result.isSuccess) {
            result.getOrDefault("")
        } else {
            apiManager.queryAi(query, context).getOrNull()?.second ?: ""
        }
    }
}
