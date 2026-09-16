package com.daftari.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun ask(apiKey: String, model: String, instructions: String, input: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Add your OpenAI API key in Settings first."))
        try {
            val body = JSONObject()
                .put("model", model.ifBlank { "gpt-5.6" })
                .put("store", false)
                .put("instructions", instructions)
                .put("input", input)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://api.openai.com/v1/responses")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()
                    return@withContext Result.failure(IllegalStateException(message ?: "OpenAI request failed (${response.code})."))
                }
                val json = JSONObject(raw)
                val outputText = json.optString("output_text")
                if (outputText.isNotBlank()) return@withContext Result.success(outputText)

                val output = json.optJSONArray("output") ?: return@withContext Result.failure(IllegalStateException("No text returned by the model."))
                val chunks = mutableListOf<String>()
                for (i in 0 until output.length()) {
                    val item = output.optJSONObject(i) ?: continue
                    val content = item.optJSONArray("content") ?: continue
                    for (j in 0 until content.length()) {
                        val c = content.optJSONObject(j) ?: continue
                        val text = c.optString("text")
                        if (text.isNotBlank()) chunks += text
                    }
                }
                if (chunks.isEmpty()) Result.failure(IllegalStateException("The model returned no readable text.")) else Result.success(chunks.joinToString("\n"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
