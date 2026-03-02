package com.hunhui.bot

import android.app.*
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BotService : Service() {

    companion object {
        const val CHANNEL_ID = "hunhui_bot_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_VOICE = "com.hunhui.bot.ACTION_VOICE"
        const val ACTION_STOP_VOICE = "com.hunhui.bot.ACTION_STOP_VOICE"
        const val ACTION_VOICE_STATE_CHANGED = "com.hunhui.bot.ACTION_VOICE_STATE_CHANGED"
        const val EXTRA_VOICE_STATE = "extra_voice_state"
        const val ACTION_TEXT_INPUT = "com.hunhui.bot.ACTION_TEXT_INPUT"
        const val ACTION_SEND_TEXT = "com.hunhui.bot.ACTION_SEND_TEXT"
        const val EXTRA_TEXT = "extra_text"
        const val KEY_TEXT_REPLY = "key_text_reply"
        private const val TAG = "BotService"
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val accumulatedText = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("대기 중..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_VOICE -> startVoiceInput()
            ACTION_STOP_VOICE -> stopVoiceInput()
            ACTION_TEXT_INPUT -> openTextInput()
            ACTION_SEND_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return START_STICKY
                sendToSlack(text)
            }
        }
        return START_STICKY
    }

    private fun startVoiceInput() {
        if (isListening) return
        isListening = true
        accumulatedText.clear() // 새 녹음 시작 시 이전 텍스트 초기화
        updateNotification("🎤 듣는 중...")

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    accumulatedText.append(if (accumulatedText.isEmpty()) text else " $text")
                    updateNotification("🎤 듣는 중... (${accumulatedText.length}자)")
                }
                // Keep listening until user presses stop
                if (isListening) {
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        kotlinx.coroutines.delay(200)
                        if (isListening) speechRecognizer?.startListening(buildRecognizerIntent())
                    }
                } else {
                    // User pressed stop — send accumulated text
                    val finalText = accumulatedText.toString().trim()
                    accumulatedText.clear()
                    broadcastVoiceState("idle")
                    if (finalText.isNotBlank()) {
                        sendToSlack(finalText)
                    } else {
                        updateNotification("대기 중...")
                    }
                }
            }

            override fun onError(error: Int) {
                val retriable = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                if (retriable && isListening) {
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        kotlinx.coroutines.delay(200)
                        if (isListening) speechRecognizer?.startListening(buildRecognizerIntent())
                    }
                    return
                }
                isListening = false
                broadcastVoiceState("idle")
                val msg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 오류"
                    SpeechRecognizer.ERROR_NO_MATCH -> "인식 실패"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "시간 초과"
                    else -> "오류 ($error)"
                }
                Log.e(TAG, "STT error: $msg")
                updateNotification("오류: $msg")
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(buildRecognizerIntent())
        broadcastVoiceState("listening")
    }

    private fun buildRecognizerIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
    }

    private fun stopVoiceInput() {
        if (!isListening) return
        speechRecognizer?.stopListening()
        isListening = false
        updateNotification("대기 중...")
        broadcastVoiceState("idle")
    }

    private fun broadcastVoiceState(state: String) {
        sendBroadcast(Intent(ACTION_VOICE_STATE_CHANGED).apply {
            putExtra(EXTRA_VOICE_STATE, state)
            setPackage(packageName)
        })
    }

    private fun openTextInput() {
        val intent = Intent(this, TextInputActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    private fun sendToSlack(text: String) {
        updateNotification("전송 중: $text")
        scope.launch {
            val result = SlackMessenger.sendMessage(applicationContext, text)
            if (result.isSuccess) {
                updateNotification("✅ 전송 완료")
                kotlinx.coroutines.delay(2000)
                updateNotification("대기 중...")
            } else {
                val err = result.exceptionOrNull()?.message ?: "알 수 없는 오류"
                Log.e(TAG, "Send failed: $err")
                updateNotification("❌ 오류: $err")
                kotlinx.coroutines.delay(3000)
                updateNotification("대기 중...")
            }
        }
    }

    private fun buildNotification(status: String, listening: Boolean = isListening): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // Toggle: if listening → STOP, else → START
        val voiceAction: String
        val voiceLabel: String
        val voiceRequestCode: Int
        if (listening) {
            voiceAction = ACTION_STOP_VOICE
            voiceLabel = "🔴 중지"
            voiceRequestCode = 11
        } else {
            voiceAction = ACTION_VOICE
            voiceLabel = "🎤 음성"
            voiceRequestCode = 1
        }

        val voiceIntent = PendingIntent.getService(
            this, voiceRequestCode,
            Intent(this, BotService::class.java).apply { action = voiceAction },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // RemoteInput: 알림에서 바로 텍스트 입력
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("메시지 입력...")
            .build()

        val replyIntent = PendingIntent.getBroadcast(
            this, 2,
            Intent(this, NotificationActionReceiver::class.java).apply {
                action = ACTION_SEND_TEXT
            },
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_edit, "✏️ 텍스트", replyIntent
        ).addRemoteInput(remoteInput).build()

        val channels = Prefs.getChannels(this)
        val selectedIdx = Prefs.getSelectedChannelIndex(this)
        val channelAlias = channels.getOrNull(selectedIdx)?.alias ?: "채널1"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("훈희봇 · $channelAlias")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_btn_speak_now, voiceLabel, voiceIntent)
            .addAction(replyAction)
            .build()
    }

    private fun updateNotification(status: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(status, isListening))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "훈희봇",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "훈희봇 상태 알림"
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        job.cancel()
    }
}
