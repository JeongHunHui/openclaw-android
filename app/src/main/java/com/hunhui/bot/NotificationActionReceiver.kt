package com.hunhui.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == BotService.ACTION_SEND_TEXT) {
            // RemoteInput에서 텍스트 꺼내기
            val remoteInputResults = RemoteInput.getResultsFromIntent(intent)
            val text = remoteInputResults?.getCharSequence(BotService.KEY_TEXT_REPLY)?.toString()
            if (!text.isNullOrBlank()) {
                val serviceIntent = Intent(context, BotService::class.java).apply {
                    this.action = BotService.ACTION_SEND_TEXT
                    putExtra(BotService.EXTRA_TEXT, text)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        } else {
            val serviceIntent = Intent(context, BotService::class.java).apply {
                this.action = action
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
