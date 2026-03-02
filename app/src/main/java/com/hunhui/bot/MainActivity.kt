package com.hunhui.bot

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hunhui.bot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isListening = false

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
        setupChannelSpinner()

        binding.btnVoice.setOnClickListener {
            if (!isListening) {
                val i = Intent(this, BotService::class.java).apply { action = BotService.ACTION_VOICE }
                ContextCompat.startForegroundService(this, i)
                setListeningState(true)
            } else {
                val i = Intent(this, BotService::class.java).apply { action = BotService.ACTION_STOP_VOICE }
                ContextCompat.startForegroundService(this, i)
                setListeningState(false)
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }
    }

    private fun setupChannelSpinner() {
        val channels = Prefs.getChannels(this)
        if (channels.isEmpty()) return

        val aliases = channels.map { it.alias }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, aliases).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerChannel.adapter = adapter
        binding.spinnerChannel.setSelection(Prefs.getSelectedChannelIndex(this))
        binding.spinnerChannel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                Prefs.saveSelectedChannelIndex(this@MainActivity, pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        setupChannelSpinner()
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
