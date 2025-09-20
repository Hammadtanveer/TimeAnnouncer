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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private var isServiceRunning by mutableStateOf(false)
    private var selectedLocaleCode by mutableStateOf("en-US")
    private var speechRate by mutableFloatStateOf(1.0f)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startAnnouncerService()
        }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TimeAnnouncerService.ACTION_STATE) {
                isServiceRunning =
                    intent.getBooleanExtra(TimeAnnouncerService.EXTRA_RUNNING, false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isServiceRunning = ServiceStateStore.isRunning(applicationContext)
        selectedLocaleCode = SettingsStore.getLocaleCode(applicationContext)
        speechRate = SettingsStore.getSpeechRate(applicationContext)

        TtsManager.rememberContext(applicationContext)
        TtsManager.init(applicationContext)
        TtsManager.setLocaleCode(applicationContext, selectedLocaleCode)
        TtsManager.setSpeechRate(applicationContext, speechRate)

        setContent {
            MainScreen(
                isRunning = isServiceRunning,
                selectedLocaleCode = selectedLocaleCode,
                speechRate = speechRate,
                onLanguageChange = { code ->
                    selectedLocaleCode = code
                    TtsManager.setLocaleCode(applicationContext, code)
                },
                onRateChange = { rate ->
                    speechRate = rate
                    TtsManager.setSpeechRate(applicationContext, rate)
                },
                onStartClick = { checkPermissionsAndStartService() },
                onStopClick = { stopAnnouncerService() }
            )
        }
    }

    override fun onStart() {
        super.onStart()
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
fun MainScreen(
    isRunning: Boolean,
    selectedLocaleCode: String,
    speechRate: Float,
    onLanguageChange: (String) -> Unit,
    onRateChange: (Float) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
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

            LanguageSelector(
                selected = selectedLocaleCode,
                onSelected = onLanguageChange
            )

            SpeedControl(
                rate = speechRate,
                onChange = onRateChange
            )

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
private fun LanguageSelector(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = listOf(
        "English" to "en-US",
        "Hindi" to "hi-IN"
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Announcement language", fontSize = 14.sp)
        Box {
            Text(
                text = items.firstOrNull { it.second == selected }?.first ?: "Select language",
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { (label, code) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            expanded = false
                            onSelected(code)
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SpeedControl(rate: Float, onChange: (Float) -> Unit) {
    val display = (rate * 10f).roundToInt() / 10f
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speech speed", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${display}x", fontSize = 14.sp)
        }
        Slider(
            value = rate,
            onValueChange = { onChange(it.coerceIn(0.5f, 1.5f)) },
            valueRange = 0.5f..1.5f,
            steps = 10,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
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
