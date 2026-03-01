package com.hunhui.bot

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hunhui.bot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isListening = false

    // BotService 상태 수신 리시버
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getStringExtra(BotService.EXTRA_VOICE_STATE)) {
                "listening" -> setListeningState(true)
                "idle" -> setListeningState(false)
            }
        }
    }

    companion object {
        private const val REQ_PERMISSIONS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Prefs.isConfigured(this)) {
            startActivity(Intent(this, SetupActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionsIfNeeded()
        startBotService()

        binding.btnVoice.setOnClickListener {
            if (!isListening) {
                // 녹음 시작
                val i = Intent(this, BotService::class.java).apply { action = BotService.ACTION_VOICE }
                ContextCompat.startForegroundService(this, i)
                setListeningState(true)
            } else {
                // 녹음 중지 (stopListening 요청)
                val i = Intent(this, BotService::class.java).apply { action = BotService.ACTION_STOP_VOICE }
                ContextCompat.startForegroundService(this, i)
                setListeningState(false)
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    private fun setListeningState(listening: Boolean) {
        isListening = listening
        if (listening) {
            binding.btnVoice.text = "👂 듣는 중... (탭하면 중지)"
            binding.btnVoice.alpha = 0.7f
        } else {
            binding.btnVoice.text = "🎤 지금 바로 음성 전송"
            binding.btnVoice.alpha = 1.0f
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BotService.ACTION_VOICE_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(statusReceiver) } catch (_: Exception) {}
    }

    private fun startBotService() {
        ContextCompat.startForegroundService(this, Intent(this, BotService::class.java))
    }

    private fun requestPermissionsIfNeeded() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQ_PERMISSIONS)
        }
    }
}
