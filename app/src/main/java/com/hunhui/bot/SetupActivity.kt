package com.hunhui.bot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hunhui.bot.databinding.ActivitySetupBinding
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 기존 저장값 로드
        val savedBot = Prefs.getBotToken(this)
        val savedUser = Prefs.getUserToken(this)
        val savedChannel = Prefs.getChannel(this)
        if (savedBot.isNotBlank()) binding.etBotToken.setText(savedBot)
        if (savedUser.isNotBlank()) binding.etUserToken.setText(savedUser)
        if (savedChannel.isNotBlank()) binding.etChannel.setText(savedChannel)

        binding.btnNext.setOnClickListener {
            val botToken = binding.etBotToken.text.toString().trim()
            val userToken = binding.etUserToken.text.toString().trim()
            val channel = binding.etChannel.text.toString().trim()

            if (botToken.isBlank() || channel.isBlank()) {
                Toast.makeText(this, "Bot Token과 Channel ID는 필수야", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 토큰 검증 후 저장
            setLoading(true)
            lifecycleScope.launch {
                val tokenToTest = userToken.ifBlank { botToken }
                val result = SlackMessenger.validateToken(tokenToTest)

                if (result.isSuccess) {
                    Prefs.save(this@SetupActivity, botToken, userToken, channel)
                    Toast.makeText(this@SetupActivity, "✅ 연결됐어!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@SetupActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                } else {
                    setLoading(false)
                    val err = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
                    Toast.makeText(this@SetupActivity, "❌ $err", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnNext.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnNext.text = if (loading) "확인 중..." else "저장하고 시작하기 🚀"
    }
}
