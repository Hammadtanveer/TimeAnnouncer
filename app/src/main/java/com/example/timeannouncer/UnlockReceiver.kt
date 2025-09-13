package com.example.timeannouncer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class UnlockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("UnlockReceiver", "Device unlocked! Triggering TTS.")
            TtsManager.speakDateTime(context.applicationContext)
        }
    }
}
