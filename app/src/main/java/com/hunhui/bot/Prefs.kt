package com.hunhui.bot

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "hunhui_bot_prefs"
    private const val KEY_BOT_TOKEN = "slack_bot_token"
    private const val KEY_USER_TOKEN = "slack_user_token"
    private const val KEY_CHANNEL = "slack_channel_id"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getBotToken(ctx: Context): String = prefs(ctx).getString(KEY_BOT_TOKEN, "") ?: ""
    fun getUserToken(ctx: Context): String = prefs(ctx).getString(KEY_USER_TOKEN, "") ?: ""
    fun getChannel(ctx: Context): String = prefs(ctx).getString(KEY_CHANNEL, "") ?: ""

    // 하위 호환
    fun getToken(ctx: Context): String = getUserToken(ctx).ifBlank { getBotToken(ctx) }

    fun save(ctx: Context, botToken: String, userToken: String, channel: String) {
        prefs(ctx).edit()
            .putString(KEY_BOT_TOKEN, botToken)
            .putString(KEY_USER_TOKEN, userToken)
            .putString(KEY_CHANNEL, channel)
            .apply()
    }

    fun isConfigured(ctx: Context): Boolean =
        getBotToken(ctx).isNotBlank() && getChannel(ctx).isNotBlank()
}
