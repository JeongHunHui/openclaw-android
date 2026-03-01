package com.hunhui.bot

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object SlackMessenger {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** 토큰 유효성 검사 (auth.test) */
    suspend fun validateToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalArgumentException("토큰이 비어있어"))
        try {
            val request = Request.Builder()
                .url("https://slack.com/api/auth.test")
                .header("Authorization", "Bearer $token")
                .post(FormBody.Builder().build())
                .build()
            val body = client.newCall(request).execute().body?.string() ?: ""
            val json = JSONObject(body)
            if (json.optBoolean("ok", false)) {
                val userName = json.optString("user", "unknown")
                Result.success(userName)
            } else {
                val err = json.optString("error", "unknown_error")
                Result.failure(IOException("Slack 오류: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 메시지 전송 */
    suspend fun sendMessage(context: Context, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        // 유저 토큰 우선, 없으면 봇 토큰
        val token = Prefs.getUserToken(context).ifBlank { Prefs.getBotToken(context) }
        val channel = Prefs.getChannel(context)

        if (token.isBlank() || channel.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Slack 설정이 없어"))
        }

        try {
            val isUserToken = token.startsWith("xoxp-")
            val bodyBuilder = FormBody.Builder()
                .add("channel", channel)
                .add("text", text)
            // 유저 토큰이면 as_user=true → 내가 직접 친 것처럼 보임
            if (isUserToken) bodyBuilder.add("as_user", "true")
            val body = bodyBuilder.build()

            val request = Request.Builder()
                .url("https://slack.com/api/chat.postMessage")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()

            val responseBody = client.newCall(request).execute().body?.string() ?: ""
            val json = JSONObject(responseBody)

            if (json.optBoolean("ok", false)) {
                Result.success(Unit)
            } else {
                val err = json.optString("error", "unknown_error")
                Result.failure(IOException("Slack 오류: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
