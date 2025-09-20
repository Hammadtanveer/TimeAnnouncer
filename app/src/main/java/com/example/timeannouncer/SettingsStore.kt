package com.example.timeannouncer

import android.content.Context
import java.util.Locale

object SettingsStore {
    private const val PREF = "time_announcer_prefs"
    private const val KEY_LOCALE = "locale_code"
    private const val KEY_RATE = "speech_rate"
    private const val DEFAULT_LOCALE = "en-US"
    private const val DEFAULT_RATE = 1.0f

    fun setLocaleCode(context: Context, code: String) {
        context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCALE, code)
            .apply()
    }

    fun getLocaleCode(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE, DEFAULT_LOCALE) ?: DEFAULT_LOCALE
    }

    fun getLocale(context: Context): Locale {
        return when (getLocaleCode(context)) {
            "hi-IN" -> Locale("hi", "IN")
            else -> Locale("en", "US")
        }
    }

    fun setSpeechRate(context: Context, rate: Float) {
        val safe = rate.coerceIn(0.5f, 1.5f)
        context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_RATE, safe)
            .apply()
    }

    fun getSpeechRate(context: Context): Float {
        return context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getFloat(KEY_RATE, DEFAULT_RATE)
            .coerceIn(0.5f, 1.5f)
    }
}
