package com.example.kursovaya.ai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Прокси Qwen (Docker), OpenAI-совместимый POST /api/chat/completions.
 * Эмулятор: http://10.0.2.2:3264 — тот же хост, что и бэкенд ПК.
 */
class QwenChatRepository(
    private val baseUrl: String = DEFAULT_BASE,
) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    suspend fun chatCompletion(
        model: String = "qwen-max-latest",
        messages: List<Pair<String, String>>,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val arr = JsonArray()
                messages.forEach { (role, content) ->
                    val o = JsonObject()
                    o.addProperty("role", role)
                    o.addProperty("content", content)
                    arr.add(o)
                }
                val body = JsonObject()
                body.addProperty("model", model)
                body.add("messages", arr)
                val json = body.toString()
                val url = baseUrl.trimEnd('/') + "/api/chat/completions"
                val req =
                    Request.Builder()
                        .url(url)
                        .post(json.toRequestBody(JSON))
                        .build()
                val resp = client.newCall(req).execute()
                val respBody = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Qwen HTTP ${resp.code}: $respBody"))
                }
                val root = JsonParser.parseString(respBody).asJsonObject
                val choices = root.getAsJsonArray("choices")
                if (choices == null || choices.size() == 0) {
                    return@withContext Result.failure(Exception("Пустой ответ модели"))
                }
                val message = choices[0].asJsonObject.getAsJsonObject("message")
                val content =
                    message.get("content")?.asString
                        ?: return@withContext Result.failure(Exception("Нет content в ответе"))
                Result.success(content)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        const val DEFAULT_BASE = "http://10.0.2.2:3264"
    }
}
