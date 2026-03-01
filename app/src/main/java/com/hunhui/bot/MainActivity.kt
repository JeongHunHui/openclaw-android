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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load saved values (masked)
        val token = Prefs.getToken(this)
        val channel = Prefs.getChannel(this)
        if (token.isNotBlank()) binding.etToken.setText(token)
        if (channel.isNotBlank()) binding.etChannel.setText(channel)
        updateStatus()

        binding.btnSave.setOnClickListener {
            val newToken = binding.etToken.text.toString().trim()
            val newChannel = binding.etChannel.text.toString().trim()

            if (newToken.isBlank() || newChannel.isBlank()) {
                Toast.makeText(this, "토큰과 채널 ID를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Prefs.save(this, newToken, newChannel)
            Toast.makeText(this, "저장됐어!", Toast.LENGTH_SHORT).show()
            updateStatus()
            startService()
        }

        binding.btnStartStop.setOnClickListener {
            if (Prefs.isConfigured(this)) {
                startService()
            } else {
                Toast.makeText(this, "먼저 설정을 저장해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionsIfNeeded()
    }

    private fun startService() {
        val intent = Intent(this, BotService::class.java)
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "훈희봇 실행 중! 알림바를 확인해봐", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        if (Prefs.isConfigured(this)) {
            binding.tvStatus.text = "✅ 연결 설정 완료"
            binding.btnStartStop.text = "서비스 시작"
        } else {
            binding.tvStatus.text = "⚠️ 설정이 필요해요"
            binding.btnStartStop.text = "설정 후 시작"
        }
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
