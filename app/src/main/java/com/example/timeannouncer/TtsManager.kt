package com.example.timeannouncer

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TtsManager : TextToSpeech.OnInitListener {

    private const val TAG = "TtsManager"
    private const val ENGINE_GOOGLE = "com.google.android.tts"
    private const val EARCON_ID = "chime_earcon"

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var lastSpokenText: Pair<String, Locale>? = null
    private var earconAdded = false

    fun init(context: Context) {
        if (tts == null) {
            Log.d(TAG, "Initializing TTS (pre-warm).")
            tts = TextToSpeech(context.applicationContext, this, ENGINE_GOOGLE)
        }
    }

    fun setLocaleCode(context: Context, code: String) {
        SettingsStore.setLocaleCode(context, code)
        applyLocaleAndVoice(context)
    }

    fun setSpeechRate(context: Context, rate: Float) {
        SettingsStore.setSpeechRate(context, rate.coerceIn(0.5f, 1.5f))
        applySpeechRate(context)
    }

    fun speakDateTime(context: Context) {
        val locale = SettingsStore.getLocale(context)
        val now = LocalDateTime.now()
        // Time only, no day
        val pattern = "h:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        val text = now.format(formatter)

        if (!isReady || tts == null) {
            lastSpokenText = text to locale
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext, this, ENGINE_GOOGLE)
            }
            return
        }

        applyLocaleAndVoice(context)
        applySpeechRate(context)
        addEarconIfNeeded(context)

        tts?.playEarcon(EARCON_ID, TextToSpeech.QUEUE_FLUSH, null, "earcon")
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utterance")
        Log.d(TAG, "Speaking: $text [$locale]")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS engine is ready.")
            isReady = true

            lastContext?.get()?.let { ctx ->
                applyLocaleAndVoice(ctx)
                applySpeechRate(ctx)
            }

            lastSpokenText?.let { (text, _) ->
                val ctx = lastContext?.get()
                if (ctx != null) {
                    addEarconIfNeeded(ctx)
                }
                tts?.playEarcon(EARCON_ID, TextToSpeech.QUEUE_FLUSH, null, "earcon")
                tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utterance")
                lastSpokenText = null
            }
        } else {
            Log.e(TAG, "TTS initialization failed! Status: $status")
            isReady = false
        }
    }

    private fun applyLocaleAndVoice(context: Context) {
        val locale = SettingsStore.getLocale(context)
        tts?.language = locale

        val voices: Set<Voice>? = tts?.voices
        val best = voices
            ?.filter { it.locale.language == locale.language && it.locale.country == locale.country }
            ?.sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.latency }
            )
            ?.firstOrNull()

        best?.let { voice ->
            runCatching { tts?.voice = voice }.onFailure {
                Log.w(TAG, "Failed to set voice ${voice.name}: ${it.message}")
            }
        }
        Log.d(TAG, "Locale applied: $locale, voice: ${tts?.voice?.name}")
    }

    private fun applySpeechRate(context: Context) {
        val rate = SettingsStore.getSpeechRate(context)
        tts?.setSpeechRate(rate)
        Log.d(TAG, "Speech rate applied: $rate x")
    }

    private fun addEarconIfNeeded(context: Context) {
        if (!earconAdded) {
            val resId = context.resources.getIdentifier("chime", "raw", context.packageName)
            if (resId != 0) {
                runCatching {
                    tts?.addEarcon(EARCON_ID, context.packageName, resId)
                    earconAdded = true
                    Log.d(TAG, "Earcon registered (resId=$resId).")
                }.onFailure { Log.w(TAG, "Earcon add failed: ${it.message}") }
            } else {
                Log.w(TAG, "Earcon resource 'raw/chime' not found. Skipping chime.")
            }
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        lastSpokenText = null
        earconAdded = false
        Log.d(TAG, "TTS engine shut down.")
    }

    private var lastContext: java.lang.ref.WeakReference<Context>? = null
    fun rememberContext(ctx: Context) {
        lastContext = java.lang.ref.WeakReference(ctx.applicationContext)
    }
}
