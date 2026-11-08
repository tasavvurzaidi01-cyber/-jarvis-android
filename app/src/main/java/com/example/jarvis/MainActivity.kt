package com.example.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var input: EditText
    private var speechRecognizer: SpeechRecognizer? = null
    private val requestAudio = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildScreen()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestAudio)
        }
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 28)
            setBackgroundColor(0xFF07111F.toInt())
        }
        val heading = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 30f
            setTextColor(0xFF55C8FF.toInt())
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "नमस्ते! मैं JARVIS हूँ। बोलिए या लिखिए।"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        input = EditText(this).apply {
            hint = "यहाँ अपना संदेश लिखें…"
            setHintTextColor(0xFF9AAABD.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setSingleLine(false)
            setPadding(16, 12, 16, 12)
        }
        val send = Button(this).apply {
            text = "भेजें"
            setOnClickListener { handleText(input.text.toString()) }
        }
        val wake = Button(this).apply {
            text = "🟢 Hey Jarvis ON"
            setOnClickListener {
                val needed = mutableListOf<String>()
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.RECORD_AUDIO)
                }
                if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    needed.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (needed.isNotEmpty()) {
                    requestPermissions(needed.toTypedArray(), requestAudio)
                } else {
                    val service = Intent(this@MainActivity, WakeWordService::class.java)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
                    status.text = "Hey Jarvis सुनने की कोशिश चालू है। Notification दिखेगा।"
                }
            }
        }
        val stopWake = Button(this).apply {
            text = "🔴 Hey Jarvis OFF"
            setOnClickListener { stopService(Intent(this@MainActivity, WakeWordService::class.java)); status.text = "Wake word बंद है।" }
        }
        val mic = Button(this).apply {
            text = "🎙 बोलकर पूछें"
            setOnClickListener { startListening() }
        }
        page.addView(heading, LinearLayout.LayoutParams(-1, -2))
        page.addView(status, LinearLayout.LayoutParams(-1, -2))
        page.addView(input, LinearLayout.LayoutParams(-1, -2))
        page.addView(send, LinearLayout.LayoutParams(-1, -2))
        page.addView(mic, LinearLayout.LayoutParams(-1, -2))
        page.addView(wake, LinearLayout.LayoutParams(-1, -2))
        page.addView(stopWake, LinearLayout.LayoutParams(-1, -2))
        setContentView(page)
    }

    private fun speak(reply: String) {
        status.text = reply
        if (::tts.isInitialized) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    private fun handleText(raw: String) {
        val message = raw.trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "पहले कुछ लिखें या बोलें", Toast.LENGTH_SHORT).show()
            return
        }
        val q = message.lowercase(Locale.ROOT)
        val reply = when {
            q.contains("नमस्ते") || q.contains("hello") || q == "hi" ->
                "नमस्ते! बताइए, मैं आपकी क्या मदद करूँ?"
            q.contains("समय") || q.contains("समय हुआ") || q.contains("बजे") || q.contains("टाइम") || q.contains("time") || q.contains("kitne baje") || q.contains("time kya hua") || q.contains("kya hua hai") || q.contains("abhi time") ->
                "अभी समय " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) + " है।"
            q.contains("तारीख") || q.contains("date") ->
                "आज " + SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("hi", "IN")).format(Date()) + " है।"
            q.contains("यूट्यूब") || q.contains("youtube") -> {
                openAppOrWeb("com.google.android.youtube", "https://www.youtube.com")
                "YouTube खोल रहा हूँ।"
            }
            q.contains("व्हाट्सएप") || q.contains("whatsapp") -> {
                openAppOrWeb("com.whatsapp", "https://www.whatsapp.com")
                "WhatsApp खोल रहा हूँ।"
            }
            q.contains("कैमरा") || q.contains("camera") -> {
                runCatching { startActivity(Intent("android.media.action.IMAGE_CAPTURE")) }
                "कैमरा खोलने की कोशिश कर रहा हूँ।"
            }
            q.contains("सेटिंग") || q.contains("settings") -> {
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                "फोन की Settings खोल रहा हूँ।"
            }
            q.contains("कॉल") || q.contains("call") -> {
                val number = message.filter { it.isDigit() || it == '+' }
                if (number.count { it.isDigit() } >= 5) {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                    "डायलर खोल रहा हूँ। कॉल करने से पहले नंबर जाँच लें।"
                } else "किस नंबर पर कॉल करना है? नंबर लिखकर बोलें।"
            }
            q.contains("सर्च") || q.contains("search") || q.contains("खोजो") -> {
                val query = message.replace(Regex("(?i)search|सर्च|खोजो"), "").trim()
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query.ifBlank { message }))))
                "इंटरनेट पर खोज रहा हूँ।"
            }
            else -> "मैंने आपकी बात सुनी: $message। अभी इस संस्करण में इंटरनेट वाला AI जवाब और Hey Jarvis wake word सक्रिय नहीं हैं।"
        }
        speak(reply)
        input.text.clear()
    }

    private fun openAppOrWeb(packageName: String, url: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) startActivity(launch)
        else startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "इस फोन में Speech Recognition उपलब्ध नहीं है", Toast.LENGTH_LONG).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestAudio)
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { status.text = "सुन रहा हूँ…" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { status.text = "समझ रहा हूँ…" }
                override fun onError(error: Int) { status.text = "आवाज़ समझ नहीं आई। फिर कोशिश करें।" }
                override fun onResults(results: Bundle?) {
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    input.setText(spoken)
                    handleText(spoken)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS को अपना सवाल बताइए")
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Hindi voice के लिए फोन की TTS settings में Hindi voice install करें।", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}
