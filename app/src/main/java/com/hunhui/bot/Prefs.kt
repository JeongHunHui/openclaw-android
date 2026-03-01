package com.hunhui.bot

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "hunhui_bot_prefs"
    private const val KEY_TOKEN = "slack_bot_token"
    private const val KEY_CHANNEL = "slack_channel_id"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getToken(ctx: Context): String = prefs(ctx).getString(KEY_TOKEN, "") ?: ""
    fun getChannel(ctx: Context): String = prefs(ctx).getString(KEY_CHANNEL, "") ?: ""

    fun save(ctx: Context, token: String, channel: String) {
        prefs(ctx).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_CHANNEL, channel)
            .apply()
    }

    fun isConfigured(ctx: Context): Boolean =
        getToken(ctx).isNotBlank() && getChannel(ctx).isNotBlank()
}
