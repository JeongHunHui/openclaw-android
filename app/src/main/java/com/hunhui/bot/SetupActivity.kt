package com.hunhui.bot

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hunhui.bot.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 기존 저장값 로드
        binding.etBotToken.setText(Prefs.getBotToken(this))
        binding.etUserToken.setText(Prefs.getUserToken(this))
        binding.etChannel.setText(Prefs.getChannel(this))

        binding.btnNext.setOnClickListener {
            val botToken = binding.etBotToken.text.toString().trim()
            val userToken = binding.etUserToken.text.toString().trim()
            val channel = binding.etChannel.text.toString().trim()

            if (botToken.isBlank() || channel.isBlank()) {
                Toast.makeText(this, "Bot Token과 Channel ID는 필수야", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 저장 후 바로 메인화면 이동 (토큰 검증은 전송 시점에 자연스럽게 됨)
            Prefs.save(this, botToken, userToken, channel)
            Toast.makeText(this, "✅ 저장됐어!", Toast.LENGTH_SHORT).show()

            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }
}
