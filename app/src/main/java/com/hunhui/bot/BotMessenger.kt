package com.hunhui.bot

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object BotMessenger {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val SERVER_URL = "http://100.73.43.27:3000/chat"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    /** hunhui-bot-server로 메시지 전송, 클로드 응답 반환 */
    suspend fun sendMessage(context: Context, text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply { put("text", text) }.toString()
            val body = bodyJson.toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(SERVER_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)

            if (json.optBoolean("ok", false)) {
                val reply = json.optString("reply", "")
                Result.success(reply)
            } else {
                val err = json.optString("error", "unknown_error")
                Result.failure(IOException("서버 오류: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
