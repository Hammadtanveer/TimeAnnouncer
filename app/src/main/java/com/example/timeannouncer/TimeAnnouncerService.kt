package com.example.timeannouncer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimeAnnouncerService : Service() {

    companion object {
        const val ACTION_STOP = "com.example.timeannouncer.action.STOP"
        const val ACTION_STATE = "com.example.timeannouncer.action.STATE"
        const val EXTRA_RUNNING = "running"
    }

    private val unlockReceiver = UnlockReceiver()
    private val notificationChannelId = "TimeAnnouncerChannel"
    private val notificationId = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
        val notification = createNotification()
        startForeground(notificationId, notification)

        // Mark running and notify UI
        ServiceStateStore.setRunning(applicationContext, true)
        sendStateBroadcast(true)

        // Pre-warm TTS
        TtsManager.init(applicationContext)

        // Listen for device unlock
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(unlockReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            ServiceStateStore.setRunning(applicationContext, false)
            sendStateBroadcast(false)
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { unregisterReceiver(unlockReceiver) }
        TtsManager.shutdown()
        stopForeground(true)
        ServiceStateStore.setRunning(applicationContext, false)
        sendStateBroadcast(false)
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun sendStateBroadcast(isRunning: Boolean) {
        val stateIntent = Intent(ACTION_STATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_RUNNING, isRunning)
        }
        sendBroadcast(stateIntent)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Time Announcer Service"
            val descriptionText = "Notifies that the time announcer is active"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(notificationChannelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, TimeAnnouncerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Time Announcer")
            .setContentText("Service is running to announce the time on unlock.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(R.mipmap.ic_launcher, "Stop", stopPendingIntent)
            .build()
    }
}
