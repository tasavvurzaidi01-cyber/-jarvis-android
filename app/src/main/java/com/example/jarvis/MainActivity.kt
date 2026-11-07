package com.example.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
        text = "नमस्ते! मैं आपका Jarvis हूँ।"  
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
    val mic = Button(this).apply {  
        text = "🎙 बोलकर पूछें"  
        setOnClickListener { startListening() }  
    }  
    page.addView(heading, LinearLayout.LayoutParams(-1, -2))  
    page.addView(status, LinearLayout.LayoutParams(-1, -2))  
    page.addView(input, LinearLayout.LayoutParams(-1, -2))  
    page.addView(send, LinearLayout.LayoutParams(-1, -2))  
    page.addView(mic, LinearLayout.LayoutParams(-1, -2))  
    setContentView(page)  
}  

private fun handleText(raw: String) {  
    val message = raw.trim()  
    if (message.isEmpty()) {  
        Toast.makeText(this, "पहले कुछ लिखें या बोलें", Toast.LENGTH_SHORT).show()  
        return  
    }  
    // Starter version: local demo response. Connect a secure AI backend in the next step.  
    val reply = when {  
        message.contains("नमस्ते") || message.contains("hello", true) ->  
            "नमस्ते! बताइए, मैं आपकी क्या मदद करूँ?"  
        message.contains("समय") ->  
            "अभी आपके फोन में समय " + java.text.SimpleDateFormat("hh:mm a", Locale("hi", "IN")).format(java.util.Date()) + " है।"  
        else -> "आपने कहा: $message। मेरा AI दिमाग अगले चरण में जोड़ा जाएगा।"  
    }  
    status.text = reply  
    if (::tts.isInitialized) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")  
    input.text.clear()  
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
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Jarvis को अपना सवाल बताइए")  
    }  
    speechRecognizer?.startListening(intent)  
}  

override fun onInit(statusCode: Int) {  
    if (statusCode == TextToSpeech.SUCCESS) {  
        val result = tts.setLanguage(Locale("hi", "IN"))  
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {  
            Toast.makeText(this, "Hindi voice आपके फोन में उपलब्ध नहीं है। TTS settings में Hindi voice install करें।", Toast.LENGTH_LONG).show()  
        }  
    }  
}  

override fun onDestroy() {  
    speechRecognizer?.destroy()  
    if (::tts.isInitialized) tts.shutdown()  
    super.onDestroy()  
}

}
