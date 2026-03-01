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
            val botToken = binding.etBotToken.text.toString().trim()
            val userToken = binding.etUserToken.text.toString().trim()
            val channel = binding.etChannel.text.toString().trim()

            if (botToken.isBlank() || channel.isBlank()) {
                Toast.makeText(this, "Bot Token과 Channel ID는 필수야", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Prefs.save(this, botToken, userToken, channel)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
