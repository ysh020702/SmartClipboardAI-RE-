package com.samsung.smartclipboard.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GeminiManager @Inject constructor(
    @Named("gemini_api_key") private val apiKey: String
) {

    companion object {
        private const val MODEL = "gemini-3.1-flash-lite"
    }

    private val client = OkHttpClient()

    suspend fun run(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
            }
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val res = client.newCall(request).execute()
            val json = JSONObject(res.body?.string() ?: "")
            json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } catch (e: Exception) {
            ""
        }
    }
}