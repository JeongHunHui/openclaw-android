package com.hunhui.bot

import android.Manifest
import android.content.Intent
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

    companion object {
        private const val REQ_PERMISSIONS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 첫 실행이면 SetupActivity로
        if (!Prefs.isConfigured(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissionsIfNeeded()
        startBotService()

        // 메뉴 버튼들
        binding.btnVoice.setOnClickListener {
            val i = Intent(this, BotService::class.java).apply { action = BotService.ACTION_VOICE }
            ContextCompat.startForegroundService(this, i)
            Toast.makeText(this, "🎤 말해봐!", Toast.LENGTH_SHORT).show()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // SetupActivity에서 돌아왔을 때 재초기화
        if (Prefs.isConfigured(this) && ::binding.isInitialized) {
            startBotService()
        }
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
