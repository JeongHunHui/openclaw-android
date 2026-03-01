package com.hunhui.bot

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

object SlackMessenger {
    private val client = OkHttpClient()

    suspend fun sendMessage(context: Context, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        // 유저 토큰 우선, 없으면 봇 토큰
        val token = Prefs.getUserToken(context).ifBlank { Prefs.getBotToken(context) }
        val channel = Prefs.getChannel(context)

        if (token.isBlank() || channel.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Slack not configured"))
        }

        val body = FormBody.Builder()
            .add("channel", channel)
            .add("text", text)
            .build()

        val request = Request.Builder()
            .url("https://slack.com/api/chat.postMessage")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)

            if (json.optBoolean("ok", false)) {
                Result.success(Unit)
            } else {
                val error = json.optString("error", "unknown error")
                Result.failure(IOException("Slack API error: $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
