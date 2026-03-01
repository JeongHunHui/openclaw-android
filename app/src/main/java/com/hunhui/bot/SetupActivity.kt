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

        binding.btnNext.setOnClickListener {
            val token = binding.etToken.text.toString().trim()
            val channel = binding.etChannel.text.toString().trim()

            if (token.isBlank() || channel.isBlank()) {
                Toast.makeText(this, "토큰과 채널 ID를 모두 입력해줘", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Prefs.save(this, token, channel)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
