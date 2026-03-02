package com.hunhui.bot

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.hunhui.bot.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private val channelRows = mutableListOf<Pair<EditText, EditText>>() // (idField, aliasField)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etBotToken.setText(Prefs.getBotToken(this))
        binding.etUserToken.setText(Prefs.getUserToken(this))

        // Load existing channels
        val existing = Prefs.getChannels(this)
        if (existing.isEmpty()) {
            addChannelRow("", "")
        } else {
            existing.forEach { ch -> addChannelRow(ch.id, ch.alias) }
        }

        binding.btnAddChannel.setOnClickListener {
            addChannelRow("", "")
        }

        binding.btnNext.setOnClickListener {
            val botToken = binding.etBotToken.text.toString().trim()
            val userToken = binding.etUserToken.text.toString().trim()

            if (botToken.isBlank()) {
                Toast.makeText(this, "Bot Token은 필수야", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val channels = channelRows.mapIndexedNotNull { idx, (idField, aliasField) ->
                val id = idField.text.toString().trim()
                val alias = aliasField.text.toString().trim().ifBlank { "채널${idx + 1}" }
                if (id.isNotBlank()) SlackChannel(id, alias) else null
            }

            if (channels.isEmpty()) {
                Toast.makeText(this, "채널을 최소 1개 입력해줘", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Prefs.save(this, botToken, userToken, channels[0].id) // legacy compat
            Prefs.saveChannels(this, channels)
            Prefs.saveSelectedChannelIndex(this, 0)
            Toast.makeText(this, "✅ 저장됐어!", Toast.LENGTH_SHORT).show()

            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
        }
    }

    private fun addChannelRow(channelId: String, alias: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        val rowIdx = channelRows.size + 1
        val etId = EditText(this).apply {
            hint = "C0XXXXXXXXX"
            setText(channelId)
            textSize = 13f
            setPadding(12, 10, 12, 10)
            setTextColor(0xFFE2E8F0.toInt())
            setHintTextColor(0xFF475569.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            setBackgroundResource(R.drawable.input_bg)
        }

        val etAlias = EditText(this).apply {
            hint = "채널$rowIdx"
            setText(alias)
            textSize = 13f
            setPadding(12, 10, 12, 10)
            setTextColor(0xFFE2E8F0.toInt())
            setHintTextColor(0xFF475569.toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 8
            }
            setBackgroundResource(R.drawable.input_bg)
        }

        val btnRemove = Button(this).apply {
            text = "❌"
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 8 }
            setOnClickListener {
                binding.channelContainer.removeView(row)
                channelRows.removeAll { it.first == etId }
            }
        }

        row.addView(etId)
        row.addView(etAlias)
        row.addView(btnRemove)
        binding.channelContainer.addView(row)
        channelRows.add(Pair(etId, etAlias))
    }
}
