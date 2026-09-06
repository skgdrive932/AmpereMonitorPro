package com.amperemonitor.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Info
import androidx.compose.material3.icons.filled.Refresh
import androidx.compose.material3.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.util.Locale

private val Bg = Color(0xFF0E0F11)
private val CardBg = Color(0xFF1A1C20)
private val Green = Color(0xFF42C964)
private val Amber = Color(0xFFFFB74D)
private val TextSecondary = Color(0xFFABB0B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmpereMonitorApp() {
    val context = LocalContext.current
    val repository = remember { BatteryRepository(context.applicationContext) }
    var manualRefresh by remember { mutableStateOf(0) }
    val snapshot by produceState(initialValue = repository.read(), manualRefresh) {
        while (true) { value = repository.read(); delay(3000) }
    }

    MaterialTheme {
        Scaffold(
            containerColor = Bg,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Ampere Monitor", color = Color.White, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = { IconButton(onClick = {}) { Icon(Icons.Default.Info, "About", tint = Color.White) } },
                    actions = {
                        IconButton(onClick = { manualRefresh++ }) { Icon(Icons.Default.Refresh, "Refresh readings", tint = Color.White) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Bg)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MainCard(snapshot)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Level", "${snapshot.level}%", Modifier.weight(1f))
                    MetricCard("Health", snapshot.health, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricCard("Voltage", snapshot.voltage?.let { String.format(Locale.US, "%.3f V", it) } ?: "--", Modifier.weight(1f))
                    MetricCard("Temperature", snapshot.temperature?.let { String.format(Locale.US, "%.1f °C", it) } ?: "--", Modifier.weight(1f))
                }
                StatusCard(snapshot)
                Text("Auto-refreshes every 3 seconds", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun MainCard(snapshot: BatterySnapshot) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            BatteryGauge(snapshot.level)
            Column(modifier = Modifier.weight(1f)) {
                Text(snapshot.currentMa?.let { "$it mA" } ?: "-- mA", color = Green, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text(when { snapshot.currentMa == null -> "Current unavailable"; snapshot.isFull -> "Battery full"; snapshot.isCharging -> "Charging"; else -> "Discharging" }, color = TextSecondary, fontSize = 16.sp)
                Spacer(Modifier.height(7.dp))
                Text(snapshot.source, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun BatteryGauge(level: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(Color(0xFF30343B), -90f, 360f, false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
            drawArc(Green, -90f, (level.coerceIn(0,100) * 3.6f), false, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
        }
        Text("$level%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(label, color = TextSecondary, fontSize = 13.sp)
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun StatusCard(snapshot: BatterySnapshot) {
    val status = when { snapshot.isFull -> "Full"; snapshot.isCharging -> "Charging"; else -> "Not charging" }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(99.dp)).background(if (snapshot.isCharging) Green else Amber))
            Spacer(Modifier.size(10.dp))
            Text("Status", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(status, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
