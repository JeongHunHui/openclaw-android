package com.hunhui.bot

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hunhui.bot.databinding.ActivityTextInputBinding

class TextInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTextInputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isBlank()) {
                Toast.makeText(this, "내용을 입력해줘", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val serviceIntent = Intent(this, BotService::class.java).apply {
                action = BotService.ACTION_SEND_TEXT
                putExtra(BotService.EXTRA_TEXT, text)
            }
            startService(serviceIntent)
            finish()
        }

        binding.btnCancel.setOnClickListener { finish() }
    }
}
