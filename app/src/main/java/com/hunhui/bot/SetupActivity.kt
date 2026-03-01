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

            setLoading(true)
            val ctx = applicationContext  // Activity 참조 안전하게 분리

            lifecycleScope.launch {
                try {
                    val tokenToTest = userToken.ifBlank { botToken }
                    val result = SlackMessenger.validateToken(tokenToTest)

                    if (isFinishing || isDestroyed) return@launch

                    if (result.isSuccess) {
                        Prefs.save(ctx, botToken, userToken, channel)
                        val intent = Intent(ctx, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    } else {
                        setLoading(false)
                        val err = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
                        Toast.makeText(ctx, "❌ $err", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    if (!isFinishing && !isDestroyed) {
                        setLoading(false)
                        Toast.makeText(ctx, "❌ 오류: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        if (isFinishing || isDestroyed) return
        binding.btnNext.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnNext.text = if (loading) "확인 중..." else "저장하고 시작하기 🚀"
    }
}
