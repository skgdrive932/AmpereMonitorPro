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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AppBackground = Color(0xFF0E0F11)
private val CardBackground = Color(0xFF1A1C20)
private val AccentGreen = Color(0xFF42C964)
private val AccentAmber = Color(0xFFFFB74D)
private val AccentRed = Color(0xFFFF6B6B)
private val SecondaryText = Color(0xFFABB0B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmpereMonitorApp() {
    val context = LocalContext.current
    val repository = remember {
        BatteryRepository(context.applicationContext)
    }

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    var showAbout by remember {
        mutableStateOf(false)
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()

    val snapshot by produceState(
        initialValue = repository.read(),
        key1 = refreshKey
    ) {
        while (true) {
            value = repository.read()
            delay(3_000)
        }
    }

    if (showAbout) {
        AboutDialog(
            onDismiss = {
                showAbout = false
            }
        )
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = {
                showSettings = false
            }
        )
    }

    MaterialTheme {
        Scaffold(
            containerColor = AppBackground,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState
                )
            },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Ampere Monitor",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                showAbout = true
                            }
                        ) {
                            Text(
                                text = "ⓘ",
                                color = Color.White,
                                fontSize = 25.sp
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                refreshKey++

                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Battery data refreshed"
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = "↻",
                                color = Color.White,
                                fontSize = 28.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                showSettings = true
                            }
                        ) {
                            Text(
                                text = "⚙",
                                color = Color.White,
                                fontSize = 24.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = AppBackground
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MainBatteryCard(snapshot)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "Level",
                        value = "${snapshot.level}%",
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        label = "Health",
                        value = snapshot.health,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "Voltage",
                        value = snapshot.voltage?.let {
                            String.format(Locale.US, "%.3f V", it)
                        } ?: "--",
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        label = "Temperature",
                        value = snapshot.temperature?.let {
                            String.format(Locale.US, "%.1f °C", it)
                        } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                }

                StatusCard(snapshot)

                Text(
                    text = "Last updated: ${formatTime(snapshot.updatedAt)}",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Auto-refreshes every 3 seconds",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        titleContentColor = Color.White,
        textContentColor = SecondaryText,
        title = {
            Text("About Ampere Monitor")
        },
        text = {
            Text(
                "Version 1.2\
\
" +
                    "This app reads battery level, voltage, temperature, " +
                    "charging state, health, and current from Android system APIs.\
\
" +
                    "Actual current reporting depends on your phone manufacturer."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        titleContentColor = Color.White,
        textContentColor = SecondaryText,
        title = {
            Text("Settings")
        },
        text = {
            Text(
                "Current unit detection is automatic.\
\
" +
                    "The dashboard refreshes every 3 seconds. " +
                    "Use the refresh button for an immediate update."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun MainBatteryCard(snapshot: BatterySnapshot) {
    val currentText = snapshot.currentMa?.let { current ->
        if (current > 0) {
            "+$current mA"
        } else {
            "$current mA"
        }
    } ?: "-- mA"

    val currentColor = when {
        snapshot.currentMa == null -> SecondaryText
        snapshot.currentMa > 0 -> AccentGreen
        snapshot.currentMa < 0 -> AccentAmber
        else -> SecondaryText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            BatteryGauge(level = snapshot.level)

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentText,
                    color = currentColor,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when {
                        snapshot.currentMa == null -> "Current unavailable"
                        snapshot.isFull -> "Battery full"
                        snapshot.currentMa > 0 -> "Charging"
                        snapshot.currentMa < 0 -> "Discharging"
                        else -> "Current unavailable"
                    },
                    color = SecondaryText,
                    fontSize = 16.sp
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Text(
                    text = snapshot.source,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BatteryGauge(level: Int) {
    Box(
        modifier = Modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawArc(
                color = Color(0xFF30343B),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = AccentGreen,
                startAngle = -90f,
                sweepAngle = level.coerceIn(0, 100) * 3.6f,
                useCenter = false,
                style = Stroke(
                    width = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        Text(
            text = "$level%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = label,
                color = SecondaryText,
                fontSize = 13.sp
            )

            Text(
                text = value,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatusCard(snapshot: BatterySnapshot) {
    val statusText = when {
        snapshot.isFull -> "Full"
        snapshot.isCharging -> "Charging"
        else -> "Not charging"
    }

    val statusColor = if (snapshot.isCharging || snapshot.isFull) {
        AccentGreen
    } else {
        AccentAmber
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(statusColor)
            )

            Spacer(
                modifier = Modifier.size(10.dp)
            )

            Text(
                text = "Status",
                color = SecondaryText,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = statusText,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatTime(timeInMillis: Long): String {
    return SimpleDateFormat(
        "hh:mm:ss a",
        Locale.getDefault()
    ).format(Date(timeInMillis))
}