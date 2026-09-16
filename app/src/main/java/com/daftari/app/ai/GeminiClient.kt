package com.daftari.app.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun ask(
        apiKey: String,
        model: String,
        instructions: String,
        input: String
    ): Result<String> = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Add your Gemini API key in Settings first.")
            )
        }

        try {
            val systemInstruction = JSONObject()
                .put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put("text", instructions)
                    )
                )

            val userContent = JSONObject()
                .put("role", "user")
                .put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put("text", input)
                    )
                )

            val body = JSONObject()
                .put("systemInstruction", systemInstruction)
                .put(
                    "contents",
                    JSONArray().put(userContent)
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", 0.3)
                        .put("maxOutputTokens", 700)
                )
                .toString()
                .toRequestBody("application/json".toMediaType())

            val selectedModel = model.ifBlank { "gemini-3.6-flash" }

            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                "$selectedModel:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            var lastError = "Gemini request failed."

            for (attempt in 0..1) {
                try {
                    client.newCall(request).execute().use { response ->

                        val raw = response.body?.string().orEmpty()

                        if (response.isSuccessful) {

                            val json = JSONObject(raw)

                            val candidates =
                                json.optJSONArray("candidates")

                            if (candidates == null || candidates.length() == 0) {
                                return@withContext Result.failure(
                                    IllegalStateException("Gemini returned no answer.")
                                )
                            }

                            val content =
                                candidates.optJSONObject(0)
                                    ?.optJSONObject("content")

                            val parts =
                                content?.optJSONArray("parts")

                            if (parts == null || parts.length() == 0) {
                                return@withContext Result.failure(
                                    IllegalStateException(
                                        "Gemini returned no readable text."
                                    )
                                )
                            }

                            val answer = buildString {
                                for (i in 0 until parts.length()) {
                                    val text =
                                        parts.optJSONObject(i)
                                            ?.optString("text")
                                            .orEmpty()

                                    if (text.isNotBlank()) {
                                        if (isNotEmpty()) append("\n")
                                        append(text)
                                    }
                                }
                            }

                            if (answer.isBlank()) {
                                return@withContext Result.failure(
                                    IllegalStateException(
                                        "Gemini returned an empty answer."
                                    )
                                )
                            }

                            return@withContext Result.success(answer)
                        }

                        lastError = runCatching {
                            JSONObject(raw)
                                .optJSONObject("error")
                                ?.optString("message")
                        }.getOrNull()
                            ?.takeIf { it.isNotBlank() }
                            ?: "Gemini request failed (${response.code})."

                        val temporary =
                            response.code == 429 ||
                            response.code == 500 ||
                            response.code == 502 ||
                            response.code == 503 ||
                            response.code == 504

                        if (!temporary || attempt == 1) {
                            return@withContext Result.failure(
                                IllegalStateException(lastError)
                            )
                        }
                    }

                } catch (e: Exception) {
                    lastError = e.message ?: "Gemini connection failed."

                    if (attempt == 1) {
                        return@withContext Result.failure(e)
                    }
                }

                delay(1500L)
            }

            Result.failure(IllegalStateException(lastError))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}