package com.amperemonitor.pro

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

data class BatterySnapshot(
    val currentMa: Int?,
    val isCharging: Boolean,
    val isFull: Boolean,
    val level: Int,
    val voltage: Float?,
    val temperature: Float?,
    val health: String,
    val source: String
)

class BatteryRepository(private val context: Context) {
    fun read(): BatterySnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percentage = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val healthCode = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val rawCurrent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val current = rawCurrent.takeIf { it != Int.MIN_VALUE && it != 0 }?.let { abs(it) / 1000 }
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        return BatterySnapshot(
            currentMa = current,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING,
            isFull = status == BatteryManager.BATTERY_STATUS_FULL,
            level = percentage,
            voltage = voltageMv.takeIf { it > 0 }?.div(1000f),
            temperature = temp.takeIf { it >= 0 }?.div(10f),
            health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            },
            source = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB charger"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless charger"
                else -> "Battery"
            }
        )
    }
}
