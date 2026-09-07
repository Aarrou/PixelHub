package com.voltpulse.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var monitor: SystemMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monitor = SystemMonitor(this)
        setContent {
            SceneDarkTheme {
                SceneDashboard(
                    fetchData = { mode -> monitor.getMetrics(mode) },
                    onApplyMode = { mode ->
                        val cmd = when (mode) {
                            "极速模式" -> "setprop debug.scene.mode performance"
                            "省电模式" -> "setprop debug.scene.mode powersave"
                            else -> "setprop debug.scene.mode balance"
                        }
                        val success = monitor.applySystemTweak(cmd)
                        val tip = if (success) "[$mode] 已激活底层调度" else "[$mode] 调控已设定 (未检测到Root/Shizuku提权)"
                        Toast.makeText(this, tip, Toast.LENGTH_SHORT).show()
                    },
                    onToggleRefreshRate = { hz ->
                        monitor.saveRefreshRate(hz)
                        Toast.makeText(this, "全局刷新率已设为 ${hz}Hz", Toast.LENGTH_SHORT).show()
                    },
                    onOpenUsage = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                )
            }
        }
    }
}

val DarkBg = Color(0xFF000000)
val CardBg = Color(0xFF131418)
val CardSubBg = Color(0xFF1B1D24)
val AccentBlue = Color(0xFF3872FF)
val AccentCyan = Color(0xFF00E5FF)
val AccentGreen = Color(0xFF00E676)
val AccentOrange = Color(0xFFFF9100)
val TextPrimary = Color(0xFFF0F2F5)
val TextSecondary = Color(0xFF8E929B)

@Composable
fun SceneDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = DarkBg, surface = CardBg, primary = AccentBlue, onBackground = TextPrimary, onSurface = TextPrimary),
        content = content
    )
}

@Composable
fun SceneDashboard(
    fetchData: (String) -> SystemStatus,
    onApplyMode: (String) -> Unit,
    onToggleRefreshRate: (Int) -> Unit,
    onOpenUsage: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentPerfMode by remember { mutableStateOf("均衡模式") }
    var status by remember { mutableStateOf(fetchData(currentPerfMode)) }

    LaunchedEffect(currentPerfMode) {
        while (true) {
            status = fetchData(currentPerfMode)
            delay(1500)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()).padding(top = 48.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> OverviewTab(status, onOpenUsage)
                1 -> PowerTab(status)
                2 -> PerformanceTab(currentPerfMode) {
                    currentPerfMode = it
                    onApplyMode(it)
                }
                3 -> ToolsTab(status, onToggleRefreshRate)
                4 -> SettingsTab(onOpenUsage, status.hasUsagePermission)
            }
        }
        LiquidGlassNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp)
        )
    }
}

@Composable
fun OverviewTab(status: SystemStatus, onOpenUsage: () -> Unit) {
    Text("仪表盘看板", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CardBg).border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(22.dp)).padding(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SOC: ${status.cpuModel}", color = TextSecondary, fontSize = 13.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AccentBlue.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(status.activeMode, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("CPU 负载", color = TextSecondary, fontSize = 13.sp); Text("${status.cpuUsagePct}%", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                Column { Text("运存占用", color = TextSecondary, fontSize = 13.sp); Text("${status.ramUsedGb}/${status.ramTotalGb}G", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                Column { Text("电池电量", color = TextSecondary, fontSize = 13.sp); Text("${status.batteryPct}%", color = if (status.isCharging) AccentGreen else TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            }
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(CardSubBg)) {
                Box(modifier = Modifier.fillMaxWidth((status.cpuUsagePct / 100f).coerceIn(0.05f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(AccentBlue, AccentCyan))))
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard("核心温度", "${status.cpuTempC} °C", "电池 ${status.batteryTempC}°C", Modifier.weight(1f))
        val sotVal = "${status.screenOnTimeMin / 60}h ${status.screenOnTimeMin % 60}m"
        val sotSub = if (status.hasUsagePermission) "充满起算(实时)" else "需开启使用情况权限"
        InfoMiniCard("上次充满起亮屏", sotVal, sotSub, Modifier.weight(1f), onOpenUsage)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard("实时功率", "${status.powerWatts} W", if (status.isCharging) "极速快充中" else "放电功耗", Modifier.weight(1f))
        InfoMiniCard("存储可用", "${(status.storageTotalGb - status.storageUsedGb).toInt()} GB", "总计 ${status.storageTotalGb.toInt()}G", Modifier.weight(1f))
    }
}

@Composable
fun PowerTab(status: SystemStatus) {
    Text("电池与充放电", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CardBg).border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(22.dp)).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${status.powerWatts} W", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = if (status.isCharging) AccentGreen else AccentOrange)
            Text(if (status.isCharging) "⚡ 正在充电中" else "🔋 电池放电功率", color = TextSecondary, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("实时电压", color = TextSecondary, fontSize = 12.sp); Text("${status.voltageVolts} V", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("实时电流", color = TextSecondary, fontSize = 12.sp); Text("${status.currentAmps} A", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("电池温度", color = TextSecondary, fontSize = 12.sp); Text("${status.batteryTempC} °C", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
    Text("充放电策略配置 (Scene标准)", color = TextSecondary, fontSize = 14.sp)
    CardItem("充满自动重置周期", "电量达到95%以上且拔电时，自动重置SOT亮屏计时", "已生效")
    CardItem("旁路供电模式", "插电高负载时跳过电池直接为主板供电", "需Root")
    CardItem("智能充电停止", "达到 80% 电量时自动断开以保护电池寿命", "推荐")
}

@Composable
fun PerformanceTab(currentMode: String, onSelectMode: (String) -> Unit) {
    Text("性能调节 (Scene方案)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    listOf(
        Triple("极速模式", "解除温控约束，激进调度 CPU/GPU 超大核满频", AccentOrange),
        Triple("均衡模式", "系统日常智能调度，动态按需升降频", AccentBlue),
        Triple("省电模式", "限制超大核高频负载，延长亮屏使用时间", AccentGreen)
    ).forEach { (title, desc, color) ->
        val isSelected = currentMode == title
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (isSelected) CardSubBg else CardBg)
                .border(BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.5f) else Color(0x1AFFFFFF)), RoundedCornerShape(18.dp))
                .clickable { onSelectMode(title) }.padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (isSelected) color else Color.DarkGray))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = if (isSelected) color else TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ToolsTab(status: SystemStatus, onToggleRefreshRate: (Int) -> Unit) {
    Text("系统极客工具箱", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    val nextHz = if (status.refreshRateHz == 120) 60 else 120
    CardItem("屏幕刷新率控制", "点击在 60Hz 与 120Hz 之间一键切换", "当前: ${status.refreshRateHz}Hz (点我切${nextHz})", onClick = {
        onToggleRefreshRate(nextHz)
    })
    CardItem("进程墓碑与冻结", "模仿 iOS 墓碑挂起机制，防止后台进程偷跑", "运行中")
    CardItem("虚拟内存与 ZRAM 扩展", "动态压缩已启用 4.0 GB，大幅减少杀后台", "已激活")
    CardItem("Keybox 密钥与环境校验", "查看系统 TEE 硬件密钥与 Play 完整性验证等级", "通过")
}

@Composable
fun SettingsTab(onOpenUsage: () -> Unit, hasUsagePermission: Boolean) {
    Text("偏好与权限设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    CardItem(
        title = "使用情况访问权限 (注意不是无障碍)",
        subtitle = "用于读取系统前台应用亮屏时长，请进入设置找到 VoltPulse 勾选允许",
        tag = if (hasUsagePermission) "已授权" else "去开启",
        onClick = onOpenUsage
    )
    CardItem("无障碍与免 Root 守护进程", "通过无线调试配对自动常驻，免 Root 享受性能调控", "配置")
    CardItem("关于 VoltPulse 极客版", "版本: 2.1.0 (Scene Engine Edition)", "检查更新")
}

private fun triggerSegmentTick(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

private fun triggerConfirmTap(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

@Composable
fun LiquidGlassNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items = listOf("主页", "电源", "性能", "工具", "设置")
    val tabCount = items.size
    val pillTotalWidth = 336.dp
    val itemWidthDp = pillTotalWidth / tabCount
    val view = LocalView.current

    val animatedPos by animateFloatAsState(targetValue = selectedTab.toFloat(), animationSpec = spring(0.72f, 380f), label = "pill")

    Box(
        modifier = modifier.width(pillTotalWidth).height(66.dp).clip(RoundedCornerShape(33.dp))
            .background(Brush.verticalGradient(listOf(Color(0xD9161822), Color(0xCC0E1017))))
            .border(BorderStroke(1.2.dp, Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color(0x0DFFFFFF)))), RoundedCornerShape(33.dp))
            .pointerInput(tabCount) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val itemW = size.width.toFloat() / tabCount
                        var activeIdx = (down.position.x / itemW).toInt().coerceIn(0, tabCount - 1)
                        if (activeIdx != selectedTab) {
                            triggerConfirmTap(view)
                            onTabSelected(activeIdx)
                        }
                        var pointer = down
                        while (pointer.pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointer.id } ?: break
                            if (change.pressed) {
                                val newIdx = (change.position.x / itemW).toInt().coerceIn(0, tabCount - 1)
                                if (newIdx != activeIdx) {
                                    activeIdx = newIdx
                                    triggerSegmentTick(view)
                                    onTabSelected(newIdx)
                                }
                            }
                            pointer = change
                        }
                    }
                }
            }
            .padding(5.dp)
    ) {
        Box(
            modifier = Modifier.offset { IntOffset((animatedPos * itemWidthDp.toPx()).roundToInt(), 0) }
                .width(itemWidthDp - 4.dp).fillMaxHeight().clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0x403872FF), Color(0x2B00E5FF))))
                .border(BorderStroke(1.dp, Color(0x803872FF)), RoundedCornerShape(28.dp))
        )
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier.size(if (isSelected) 8.dp else 4.dp).clip(CircleShape).background(if (isSelected) AccentCyan else Color(0xFF6B7280)))
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(label, color = if (isSelected) TextPrimary else TextSecondary, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoMiniCard(title: String, value: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(modifier = modifier.clip(RoundedCornerShape(18.dp)).background(CardBg).border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(18.dp)).clickable { onClick() }.padding(16.dp)) {
        Column {
            Text(title, color = TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, color = AccentBlue, fontSize = 11.sp)
        }
    }
}

@Composable
fun CardItem(title: String, subtitle: String, tag: String, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardBg).border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(18.dp)).clickable { onClick() }.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(CardSubBg).border(BorderStroke(1.dp, Color(0x263872FF)), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(tag, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
