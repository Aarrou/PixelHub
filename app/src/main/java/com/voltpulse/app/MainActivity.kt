package com.voltpulse.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var monitor: BatteryMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monitor = BatteryMonitor(this)

        setContent {
            MaterialTheme(colorScheme = dynamicLightColorScheme(this)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VoltPulseDashboard(
                        onOpenUsageSettings = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        fetchData = { monitor.getBatteryMetrics() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoltPulseDashboard(onOpenUsageSettings: () -> Unit, fetchData: () -> BatteryInfo) {
    var info by remember { mutableStateOf(fetchData()) }

    LaunchedEffect(Unit) {
        while (true) {
            info = fetchData()
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoltPulse 电池监控", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${info.level}%",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (info.isCharging) "⚡ 正在充电" else "🔋 放电中",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("实时功率", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${info.powerWatts} W",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "电压: ${info.voltageVolts} V | 电流: ${info.currentAmps} A | 温度: ${info.temperatureC} °C",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("预计还剩", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${info.estimatedRemainingHours} 小时", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (info.isCharging) "充至充满" else "基于当前功耗",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("今日亮屏", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        val h = info.screenOnTimeMinutes / 60
                        val m = info.screenOnTimeMinutes % 60
                        Text(text = "${h}h ${m}m", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("零点起统计", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            FilledTonalButton(onClick = onOpenUsageSettings, modifier = Modifier.fillMaxWidth()) {
                Text("若亮屏时间为0，点击授予使用情况访问权限")
            }
        }
    }
}
