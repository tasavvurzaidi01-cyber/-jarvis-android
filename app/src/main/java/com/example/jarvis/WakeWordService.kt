package com.example.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import java.util.Locale

class WakeWordService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private val channelId = "jarvis_wake_word"

    override fun onCreate() { super.onCreate(); createChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIFICATION_ID, notification("Hey Jarvis बोलकर बुलाएँ"))
        startRecognition()
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "JARVIS Wake Word", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(message: String): Notification {
        val openIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("JARVIS सुन रहा है").setContentText(message)
            .setContentIntent(openIntent).setOngoing(true).build()
    }

    private fun startRecognition() {
        if (listening || !SpeechRecognizer.isRecognitionAvailable(this)) return
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listening = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) { listening = false; scheduleRestart() }
                override fun onResults(results: Bundle?) {
                    listening = false
                    val phrases = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (phrases.any { isWakePhrase(it) }) {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,
                            notification("Hey Jarvis सुना! JARVIS खोलने के लिए notification दबाएँ"))
                    }
                    scheduleRestart()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val phrases = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (phrases.any { isWakePhrase(it) }) {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,
                            notification("Hey Jarvis सुना! JARVIS खोलने के लिए notification दबाएँ"))
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer?.startListening(speechIntent)
        } catch (_: Exception) { listening = false; scheduleRestart() }
    }

    private fun isWakePhrase(text: String): Boolean {
        val s = text.lowercase(Locale.ROOT)
        return s.contains("hey jarvis") || s.contains("hey j a r v i s") || s.contains("हे जार्विस") || s.contains("ए जार्विस")
    }

    private fun scheduleRestart() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ startRecognition() }, 900)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel(); recognizer?.destroy(); recognizer = null
        listening = false
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.example.jarvis.STOP_WAKE"
        private const val NOTIFICATION_ID = 701
    }
}
