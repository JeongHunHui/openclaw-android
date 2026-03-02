package com.hunhui.bot

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SlackChannel(val id: String, val alias: String)

object Prefs {
    private const val NAME = "hunhui_bot_prefs"
    private const val KEY_BOT_TOKEN = "slack_bot_token"
    private const val KEY_USER_TOKEN = "slack_user_token"
    private const val KEY_CHANNEL = "slack_channel_id"   // legacy
    private const val KEY_CHANNELS = "slack_channels"
    private const val KEY_SELECTED_CHANNEL = "slack_selected_channel"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getBotToken(ctx: Context): String = prefs(ctx).getString(KEY_BOT_TOKEN, "") ?: ""
    fun getUserToken(ctx: Context): String = prefs(ctx).getString(KEY_USER_TOKEN, "") ?: ""
    fun getChannel(ctx: Context): String = prefs(ctx).getString(KEY_CHANNEL, "") ?: ""
    fun getToken(ctx: Context): String = getUserToken(ctx).ifBlank { getBotToken(ctx) }

    fun getChannels(ctx: Context): List<SlackChannel> {
        val json = prefs(ctx).getString(KEY_CHANNELS, "") ?: ""
        if (json.isNotBlank()) {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    SlackChannel(obj.optString("id", ""), obj.optString("alias", "채널${i + 1}"))
                }.filter { it.id.isNotBlank() }
            } catch (e: Exception) { emptyList() }
        }
        val legacy = getChannel(ctx)
        return if (legacy.isNotBlank()) listOf(SlackChannel(legacy, "채널1")) else emptyList()
    }

    fun saveChannels(ctx: Context, channels: List<SlackChannel>) {
        val arr = JSONArray()
        channels.forEach { ch ->
            arr.put(JSONObject().apply {
                put("id", ch.id)
                put("alias", ch.alias)
            })
        }
        prefs(ctx).edit().putString(KEY_CHANNELS, arr.toString()).apply()
    }

    fun getSelectedChannelIndex(ctx: Context): Int =
        prefs(ctx).getInt(KEY_SELECTED_CHANNEL, 0)

    fun saveSelectedChannelIndex(ctx: Context, index: Int) {
        prefs(ctx).edit().putInt(KEY_SELECTED_CHANNEL, index).apply()
    }

    fun save(ctx: Context, botToken: String, userToken: String, channel: String) {
        prefs(ctx).edit()
            .putString(KEY_BOT_TOKEN, botToken)
            .putString(KEY_USER_TOKEN, userToken)
            .putString(KEY_CHANNEL, channel)
            .apply()
    }

    fun isConfigured(ctx: Context): Boolean =
        getBotToken(ctx).isNotBlank() && getChannels(ctx).isNotEmpty()
}
