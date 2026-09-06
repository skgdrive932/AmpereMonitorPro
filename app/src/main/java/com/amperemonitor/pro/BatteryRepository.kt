package com.amperemonitor.pro

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

data class BatterySnapshot(
    val currentMa: Int?,
    val rawCurrent: Int?,
    val isCharging: Boolean,
    val isFull: Boolean,
    val level: Int,
    val voltage: Float?,
    val temperature: Float?,
    val health: String,
    val source: String,
    val updatedAt: Long = System.currentTimeMillis()
)

class BatteryRepository(private val context: Context) {

    fun read(): BatterySnapshot {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val batteryManager = context.getSystemService(
            Context.BATTERY_SERVICE
        ) as BatteryManager

        val rawLevel = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_LEVEL,
            -1
        ) ?: -1

        val scale = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_SCALE,
            -1
        ) ?: -1

        val level = if (rawLevel >= 0 && scale > 0) {
            rawLevel * 100 / scale
        } else {
            0
        }

        val status = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            -1
        ) ?: -1

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL

        val healthCode = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            -1
        ) ?: -1

        val plugged = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_PLUGGED,
            0
        ) ?: 0

        val rawCurrent = batteryManager.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CURRENT_NOW
        )

        val currentMa = rawCurrent
            .takeIf { it != Int.MIN_VALUE && it != 0 }
            ?.let { currentValue ->
                /*
                 * Android normally returns microamperes (µA).
                 * Some manufacturers return mA instead.
                 *
                 * Values above 10,000 are safely treated as µA.
                 * Smaller values are kept as mA.
                 */
                if (abs(currentValue) >= 10_000) {
                    currentValue / 1_000
                } else {
                    currentValue
                }
            }

        val voltageMv = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_VOLTAGE,
            -1
        ) ?: -1

        val temperatureTenths = batteryIntent?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE,
            -1
        ) ?: -1

        return BatterySnapshot(
            currentMa = currentMa,
            rawCurrent = rawCurrent.takeIf { it != Int.MIN_VALUE },
            isCharging = isCharging,
            isFull = isFull,
            level = level,
            voltage = voltageMv.takeIf { it > 0 }?.div(1000f),
            temperature = temperatureTenths.takeIf { it >= 0 }?.div(10f),
            health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
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