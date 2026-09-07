package com.voltpulse.app

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.util.Calendar
import kotlin.math.abs

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val powerWatts: Double,
    val voltageVolts: Double,
    val currentAmps: Double,
    val temperatureC: Double,
    val screenOnTimeMinutes: Long,
    val estimatedRemainingHours: Double
)

class BatteryMonitor(private val context: Context) {
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun getBatteryMetrics(): BatteryInfo {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, iFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = (level * 100 / scale.toFloat()).toInt()

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltageV = voltageMv / 1000.0

        val currentUa = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentA = abs(currentUa) / 1_000_000.0
        val powerW = voltageV * currentA

        val tempRaw = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempRaw / 10.0

        val sotMinutes = getTodayScreenOnMinutes()

        val estimatedHours = if (!isCharging && powerW > 0.5) {
            val remainingWh = (batteryPct / 100.0) * (4.5 * 3.85)
            remainingWh / powerW
        } else if (isCharging && powerW > 0.5) {
            val neededWh = ((100 - batteryPct) / 100.0) * (4.5 * 3.85)
            neededWh / powerW
        } else {
            0.0
        }

        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            powerWatts = (powerW * 100).toInt() / 100.0,
            voltageVolts = (voltageV * 100).toInt() / 100.0,
            currentAmps = (currentA * 100).toInt() / 100.0,
            temperatureC = tempC,
            screenOnTimeMinutes = sotMinutes,
            estimatedRemainingHours = (estimatedHours * 10).toInt() / 10.0
        )
    }

    private fun getTodayScreenOnMinutes(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return try {
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis()
            )
            val totalMillis = stats?.sumOf { it.totalTimeInForeground } ?: 0L
            totalMillis / (1000 * 60)
        } catch (e: Exception) {
            0L
        }
    }
}
