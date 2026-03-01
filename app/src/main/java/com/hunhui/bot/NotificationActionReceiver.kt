package com.hunhui.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

// Reserved for future notification quick-reply actions
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val serviceIntent = Intent(context, BotService::class.java).apply {
            this.action = action
            putExtra(BotService.EXTRA_TEXT, intent.getStringExtra(BotService.EXTRA_TEXT))
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
