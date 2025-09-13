package com.example.timeannouncer

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TtsManager : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var lastSpokenText: String? = null

    // Pre-initialize TTS to reduce first-utterance latency
    fun init(context: Context) {
        if (tts == null) {
            Log.d("TtsManager", "Initializing TTS (pre-warm).")
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    fun speakDateTime(context: Context) {
        Log.d("TtsManager", "speakDateTime called.")
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("h:mm a 'on' EEEE")
        val text = now.format(formatter)

        if (!isReady || tts == null) {
            lastSpokenText = text
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext, this)
            }
        } else {
            speakText(text)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TtsManager", "TTS engine is ready.")
            isReady = true
            tts?.language = Locale.getDefault()
            lastSpokenText?.let {
                speakText(it)
                lastSpokenText = null
            }
        } else {
            Log.e("TtsManager", "TTS initialization failed! Status: $status")
            isReady = false
        }
    }

    private fun speakText(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TimeAnnouncer_utteranceId")
            Log.d("TtsManager", "Speaking: $text")
        } else {
            Log.e("TtsManager", "Tried to speak but TTS is not ready.")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        lastSpokenText = null
        Log.d("TtsManager", "TTS engine shut down.")
    }
}
