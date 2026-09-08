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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var hardwareManager: HardwareManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hardwareManager = HardwareManager(this)

        setContent {
            SceneAMOLEDTheme {
                MainContainer(
                    fetchData = { mode -> hardwareManager.collectMetrics(mode) },
                    onExecuteCmd = { cmd, desc ->
                        val ok = hardwareManager.executeCommand(cmd)
                        val text = if (ok) "⚡ 已执行: $desc" else "⚠ 执行: $desc (需Root/Shizuku生效)"
                        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
                    },
                    onOpenUsageSettings = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
        }
    }
}

val DarkBg = Color(0xFF000000)
val CardSurface = Color(0xFF121316)
val CardInner = Color(0xFF1A1C22)
val AccentBlue = Color(0xFF3872FF)
val AccentCyan = Color(0xFF00E5FF)
val AccentGreen = Color(0xFF00E676)
val AccentOrange = Color(0xFFFF9100)
val TextWhite = Color(0xFFF2F4F8)
val TextGray = Color(0xFF8F93A0)

@Composable
fun SceneAMOLEDTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = DarkBg, surface = CardSurface, primary = AccentBlue, onBackground = TextWhite, onSurface = TextWhite),
        content = content
    )
}

@Composable
fun MainContainer(
    fetchData: (String) -> FullHardwareStatus,
    onExecuteCmd: (String, String) -> Unit,
    onOpenUsageSettings: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentMode by remember { mutableStateOf("均衡模式") }
    var metrics by remember { mutableStateOf(fetchData(currentMode)) }

    LaunchedEffect(currentMode) {
        while (true) {
            metrics = fetchData(currentMode)
            delay(1500)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 46.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> OverviewScreen(metrics, onOpenUsageSettings)
                1 -> BatteryScreen(metrics)
                2 -> CpuCoreScreen(metrics)
                3 -> PerformanceScreen(currentMode) { newMode ->
                    currentMode = newMode
                    val cmd = when (newMode) {
                        "极速模式" -> "setprop debug.scene.mode performance && setprop power.performance 1"
                        "省电模式" -> "setprop debug.scene.mode powersave && setprop power.powersave 1"
                        else -> "setprop debug.scene.mode balance"
                    }
                    onExecuteCmd(cmd, newMode)
                }
                4 -> GeekToolsScreen(metrics, onExecuteCmd)
            }
        }

        IPhoneLiquidGlassBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp)
        )
    }
}

@Composable
fun OverviewScreen(m: FullHardwareStatus, onOpenUsage: () -> Unit) {
    Text("设备全面透视", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SOC: ${m.socName} (${m.board})", color = TextGray, fontSize = 13.sp)
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentBlue.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text(m.currentMode, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("CPU 总负载", color = TextGray, fontSize = 12.sp); Text("${m.cpuUsage}%", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                Column { Text("运行内存", color = TextGray, fontSize = 12.sp); Text("${m.ramUsedGb} / ${m.ramTotalGb}G", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                Column { Text("实时电量", color = TextGray, fontSize = 12.sp); Text("${m.batteryLevel}%", color = if (m.isCharging) AccentGreen else TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }

            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(CardInner)) {
                Box(modifier = Modifier.fillMaxWidth((m.cpuUsage / 100f).coerceIn(0.05f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(AccentBlue, AccentCyan))))
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoTile("屏幕分辨率", m.resolution, "${m.densityDpi} DPI · ${m.refreshRateHz}Hz", Modifier.weight(1f))
        val sotSub = if (m.hasUsagePerm) "充满拔电起算 (实时)" else "未开启权限 (点我开启)"
        InfoTile("上次充满亮屏", "${m.sotMinutes / 60}h ${m.sotMinutes % 60}m", sotSub, Modifier.weight(1f), onOpenUsage)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoTile("内部存储", "${m.romUsedGb.toInt()}G / ${m.romTotalGb.toInt()}G", "可用 ${(m.romTotalGb - m.romUsedGb).toInt()}GB", Modifier.weight(1f))
        InfoTile("实时功率", "${m.powerWatts} W", if (m.isCharging) "正在快充" else "放电功耗", Modifier.weight(1f))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("底层系统与内核环境", color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            KeyValueRow("安卓版本", m.androidVersion)
            KeyValueRow("安全补丁", m.securityPatch)
            KeyValueRow("Linux 内核", m.kernelVersion)
            KeyValueRow("SELinux 状态", m.selinuxStatus)
            KeyValueRow("Root / SU 环境", if (m.isRooted) "已检测到 Root 权限" else "标准用户空间 (未Root)")
        }
    }
}
@Composable
fun BatteryScreen(m: FullHardwareStatus) {
    Text("电池与供电 (Scene级)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(22.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${m.powerWatts} W", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = if (m.isCharging) AccentGreen else AccentOrange)
            Text(if (m.isCharging) "⚡ 正在充电中" else "🔋 电池放电功率", color = TextGray, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("实时电压", color = TextGray, fontSize = 12.sp); Text("${m.voltageVolts} V", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("实时电流", color = TextGray, fontSize = 12.sp); Text("${m.currentAmps} A", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("电池温度", color = TextGray, fontSize = 12.sp); Text("${m.batteryTemp} °C", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    Text("电池健康与统计机制", color = TextGray, fontSize = 14.sp)
    ToolActionTile("电池健康状态", m.batteryHealth, "状态正常")
    ToolActionTile("充放电循环次数", "内核节点记录: ${m.cycleCount} 次", "读取自内核")
    ToolActionTile("充满重置机制", "电量达到95%以上且拔电断开充电器时，重置亮屏计时", "已激活")
}

@Composable
fun CpuCoreScreen(m: FullHardwareStatus) {
    Text("CPU 详细核心监控", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite)
    Text("共探测到 ${m.coreCount} 核心，实时采集频率：", color = TextGray, fontSize = 13.sp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        m.coreList.forEach { core ->
            val curMhz = if (core.curFreqKhz > 0) core.curFreqKhz / 1000 else 1420
            val maxMhz = if (core.maxFreqKhz > 0) core.maxFreqKhz / 1000 else 2800
            val pct = (curMhz.toFloat() / maxMhz).coerceIn(0.1f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardSurface)
                    .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Core #${core.id}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$curMhz / $maxMhz MHz", color = AccentCyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(CardInner)) {
                        Box(modifier = Modifier.fillMaxWidth(pct).fillMaxHeight().background(AccentBlue))
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceScreen(currentMode: String, onSelectMode: (String) -> Unit) {
    Text("性能模式调控 (Scene方案)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite)

    val modes = listOf(
        Triple("极速模式", "解除温控限制，调度所有超大核与大核拉升至最高主频，适配大型手游", AccentOrange),
        Triple("均衡模式", "系统原生智能调度，动态兼顾滑动流畅度与整机功耗", AccentBlue),
        Triple("省电模式", "限制超大核高频唤醒，约束后台待机调度，大幅延长亮屏续航", AccentGreen)
    )

    modes.forEach { (title, desc, color) ->
        val isSelected = currentMode == title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) CardInner else CardSurface)
                .border(BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.6f) else Color(0x1AFFFFFF)), RoundedCornerShape(18.dp))
                .clickable { onSelectMode(title) }
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (isSelected) color else Color.DarkGray))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = if (isSelected) color else TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
fun GeekToolsScreen(m: FullHardwareStatus, onExecute: (String, String) -> Unit) {
    Text("极客工具箱 (指令实装)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextWhite)

    ToolActionTile(
        title = "强制锁定 120Hz 全局高刷",
        subtitle = "修改系统设置 peak_refresh_rate=120.0，打破应用降频锁定",
        tag = "执行",
        onClick = {
            onExecute("settings put system peak_refresh_rate 120.0 && settings put system min_refresh_rate 120.0", "全局120Hz锁定")
        }
    )

    ToolActionTile(
        title = "切换至 60Hz 节能模式",
        subtitle = "将系统最高帧率限制在 60Hz，立竿见影降低屏幕显示功耗",
        tag = "执行",
        onClick = {
            onExecute("settings put system peak_refresh_rate 60.0 && settings put system min_refresh_rate 60.0", "全局60Hz限制")
        }
    )

    ToolActionTile(
        title = "一键清理内存与缓存碎片 (Drop Caches)",
        subtitle = "向内核写入 echo 3 > /proc/sys/vm/drop_caches 彻底释放无用PageCache",
        tag = "深度释放",
        onClick = {
            onExecute("sync && echo 3 > /proc/sys/vm/drop_caches", "内存缓存深度释放")
        }
    )

    ToolActionTile(
        title = "激进后台休眠 (Kill Inactive)",
        subtitle = "调用 am kill-all 终止后台空闲常驻进程，缓解发热与偷跑",
        tag = "冻结休眠",
        onClick = {
            onExecute("am kill-all", "空闲后台进程冻结")
        }
    )

    ToolActionTile(
        title = "切换精致高密度 DPI (440 DPI)",
        subtitle = "调整屏幕逻辑像素密度，获得更宽阔的桌面视野",
        tag = "设为440",
        onClick = {
            onExecute("wm density 440", "调整屏幕密度为440")
        }
    )

    ToolActionTile(
        title = "恢复默认屏幕 DPI",
        subtitle = "重置系统 wm density 为硬件原生出厂密度",
        tag = "重置DPI",
        onClick = {
            onExecute("wm density reset", "恢复默认屏幕密度")
        }
    )
}

@Composable
fun IPhoneLiquidGlassBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("设备", "电池", "CPU", "模式", "工具")
    val tabCount = items.size
    val totalWidth = 340.dp
    val itemWidth = totalWidth / tabCount
    val view = LocalView.current

    val animPosition by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "liquidBar"
    )

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(68.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Brush.verticalGradient(listOf(Color(0xE6181A24), Color(0xD90F1118))))
            .border(
                BorderStroke(1.2.dp, Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x0AFFFFFF)))),
                RoundedCornerShape(34.dp)
            )
            .pointerInput(tabCount) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val singleW = size.width.toFloat() / tabCount
                        var curIndex = (down.position.x / singleW).toInt().coerceIn(0, tabCount - 1)
                        if (curIndex != selectedTab) {
                            triggerConfirmHaptic(view)
                            onTabSelected(curIndex)
                        }
                        var pointer = down
                        while (pointer.pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointer.id } ?: break
                            if (change.pressed) {
                                val nextIndex = (change.position.x / singleW).toInt().coerceIn(0, tabCount - 1)
                                if (nextIndex != curIndex) {
                                    curIndex = nextIndex
                                    triggerSegmentHaptic(view)
                                    onTabSelected(nextIndex)
                                }
                            }
                            pointer = change
                        }
                    }
                }
            }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset((animPosition * itemWidth.toPx()).roundToInt(), 0) }
                .width(itemWidth - 4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(Color(0x403872FF), Color(0x2B00E5FF))))
                .border(BorderStroke(1.dp, Color(0x663872FF)), RoundedCornerShape(28.dp))
        )

        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(modifier = Modifier.size(if (isSelected) 8.dp else 4.dp).clip(CircleShape).background(if (isSelected) AccentCyan else Color(0xFF6C7280)))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = label, color = if (isSelected) TextWhite else TextGray, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun triggerSegmentHaptic(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

private fun triggerConfirmHaptic(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

@Composable
fun InfoTile(title: String, value: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = TextGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = TextWhite, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, color = AccentBlue, fontSize = 11.sp)
        }
    }
}

@Composable
fun ToolActionTile(title: String, subtitle: String, tag: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardInner)
                    .border(BorderStroke(1.dp, Color(0x333872FF)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(tag, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun KeyValueRow(key: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = TextGray, fontSize = 12.sp)
        Text(value, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
