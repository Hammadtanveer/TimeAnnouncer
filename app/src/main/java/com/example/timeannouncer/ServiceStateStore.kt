package com.example.timeannouncer

import android.content.Context

object ServiceStateStore {
    private const val PREF = "time_announcer_prefs"
    private const val KEY_RUNNING = "service_running"

    fun setRunning(context: Context, running: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RUNNING, running)
            .apply()
    }

    fun isRunning(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_RUNNING, false)
    }
}
