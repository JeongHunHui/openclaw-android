package com.hunhui.bot

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hunhui.bot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var webView: WebView

    companion object {
        private const val REQ_PERMISSIONS = 100
        const val WEB_URL = "https://hunhui-bot.web.app"
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BotService.ACTION_VOICE_STATE_CHANGED -> {
                    val state = intent.getStringExtra(BotService.EXTRA_VOICE_STATE) ?: return
                    webView.post {
                        webView.evaluateJavascript(
                            "window.HunhuiNativeCallback && window.HunhuiNativeCallback.onVoiceStateChanged('$state');",
                            null
                        )
                    }
                }
                BotService.ACTION_STT_RESULT -> {
                    val text = intent.getStringExtra(BotService.EXTRA_STT_TEXT) ?: return
                    val escaped = text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
                    webView.post {
                        webView.evaluateJavascript(
                            "window.HunhuiNativeCallback && window.HunhuiNativeCallback.onVoiceResult('$escaped');",
                            null
                        )
                    }
                }
            }
        }
    }

    inner class HunhuiNativeInterface {
        @JavascriptInterface
        fun startVoice() {
            val i = Intent(this@MainActivity, BotService::class.java).apply {
                action = BotService.ACTION_VOICE
            }
            ContextCompat.startForegroundService(this@MainActivity, i)
        }

        @JavascriptInterface
        fun stopVoice() {
            val i = Intent(this@MainActivity, BotService::class.java).apply {
                action = BotService.ACTION_STOP_VOICE
            }
            ContextCompat.startForegroundService(this@MainActivity, i)
        }

        @JavascriptInterface
        fun sendMessage(text: String) {
            val i = Intent(this@MainActivity, BotService::class.java).apply {
                action = BotService.ACTION_SEND_TEXT
                putExtra(BotService.EXTRA_TEXT, text)
            }
            ContextCompat.startForegroundService(this@MainActivity, i)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
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

        webView = binding.webView
        setupWebView()

        requestPermissionsIfNeeded()
        startBotService()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            // 커스텀 User-Agent에 HunhuiBot-Android 추가
            userAgentString = userAgentString + " HunhuiBot-Android/1.0"
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.addJavascriptInterface(HunhuiNativeInterface(), "HunhuiNative")
        webView.loadUrl(WEB_URL)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(BotService.ACTION_VOICE_STATE_CHANGED)
            addAction(BotService.ACTION_STT_RESULT)
        }
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

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
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
