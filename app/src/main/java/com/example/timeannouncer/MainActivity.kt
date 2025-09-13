package com.example.timeannouncer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.CircleShape

class MainActivity : ComponentActivity() {

    private var isServiceRunning by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startAnnouncerService() else Unit
        }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TimeAnnouncerService.ACTION_STATE) {
                isServiceRunning = intent.getBooleanExtra(TimeAnnouncerService.EXTRA_RUNNING, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize UI with persisted service state
        isServiceRunning = ServiceStateStore.isRunning(applicationContext)

        setContent {
            MainScreen(
                isRunning = isServiceRunning,
                onStartClick = { checkPermissionsAndStartService() },
                onStopClick = { stopAnnouncerService() }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-sync from persisted state in case no broadcast arrives
        isServiceRunning = ServiceStateStore.isRunning(applicationContext)

        val filter = IntentFilter(TimeAnnouncerService.ACTION_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(stateReceiver) }
    }

    private fun checkPermissionsAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (PackageManager.PERMISSION_GRANTED) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) -> {
                    startAnnouncerService()
                }
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startAnnouncerService()
        }
    }

    private fun startAnnouncerService() {
        val serviceIntent = Intent(this, TimeAnnouncerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopAnnouncerService() {
        val serviceIntent = Intent(this, TimeAnnouncerService::class.java)
        stopService(serviceIntent)
    }
}

@Composable
fun MainScreen(isRunning: Boolean, onStartClick: () -> Unit, onStopClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ServiceStatusIndicator(isRunning = isRunning)
                Text(
                    text = if (isRunning) "Service running" else "Service stopped",
                    fontSize = 16.sp
                )
            }
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Start Service", fontSize = 18.sp)
            }
            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Stop Service", fontSize = 18.sp)
            }
        }
        Text(
            text = "Powered by Hammad Tanveer",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .alpha(0.5f),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ServiceStatusIndicator(isRunning: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by if (isRunning) {
        transition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        androidx.compose.runtime.remember { mutableStateOf(1f) }
    }
    val color = if (isRunning) Color(0xFF2ECC71) else Color(0xFFE74C3C)
    Box(
        modifier = Modifier
            .size((14f * scale).dp)
            .background(color = color, shape = CircleShape)
    )
}

