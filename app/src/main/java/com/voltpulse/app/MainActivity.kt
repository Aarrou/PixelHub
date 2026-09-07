package com.voltpulse.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
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
                    onOpenUsage = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                )
            }
        }
    }
}

val DarkBg = Color(0xFF000000)
val CardBg = Color(0xFF141518)
val CardSubBg = Color(0xFF1C1E23)
val AccentBlue = Color(0xFF2F80ED)
val AccentGreen = Color(0xFF00C853)
val AccentOrange = Color(0xFFFF9100)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFF9E9E9E)

@Composable
fun SceneDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBg,
            surface = CardBg,
            primary = AccentBlue,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

@Composable
fun SceneDashboard(
    fetchData: (String) -> SystemStatus,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> OverviewTab(status, onOpenUsage)
                1 -> PowerTab(status)
                2 -> PerformanceTab(currentPerfMode) { currentPerfMode = it }
                3 -> ToolsTab(status)
                4 -> SettingsTab()
            }
        }

        LiquidFloatingNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}

@Composable
fun OverviewTab(status: SystemStatus, onOpenUsage: () -> Unit) {
    Text(text = "仪表盘看板", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOC 平台: ${status.cpuModel}", color = TextSecondary, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(status.activeMode, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CPU 负载", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.cpuUsagePct}%", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("运存占用", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.ramUsedGb} / ${status.ramTotalGb}G", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("电池电量", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.batteryPct}%", color = if (status.isCharging) AccentGreen else TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 自定义平滑进度条，避开 LinearProgressIndicator 的版本兼容冲突
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CardSubBg)
            ) {
                val fraction = (status.cpuUsagePct / 100f).coerceIn(0.05f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(AccentBlue)
                )
            }
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard(title = "核心温度", value = "${status.cpuTempC} °C", sub = "电池 ${status.batteryTempC}°C", modifier = Modifier.weight(1f))
        InfoMiniCard(title = "今日亮屏", value = "${status.screenOnTimeMin / 60}h ${status.screenOnTimeMin % 60}m", sub = "点击授权统计", modifier = Modifier.weight(1f), onClick = onOpenUsage)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard(title = "实时功率", value = "${status.powerWatts} W", sub = if (status.isCharging) "正在极速快充" else "放电功耗", modifier = Modifier.weight(1f))
        InfoMiniCard(title = "存储可用", value = "${(status.storageTotalGb - status.storageUsedGb).toInt()} GB", sub = "总计 ${status.storageTotalGb.toInt()}GB", modifier = Modifier.weight(1f))
    }
}

@Composable
fun PowerTab(status: SystemStatus) {
    Text(text = "电池与充放电", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${status.powerWatts} W",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (status.isCharging) AccentGreen else AccentOrange
            )
            Text(
                text = if (status.isCharging) "⚡ 正在充电中" else "🔋 电池放电功率",
                color = TextSecondary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("实时电压", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.voltageVolts} V", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("实时电流", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.currentAmps} A", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("电池温度", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.batteryTempC} °C", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    Text("充放电策略配置", color = TextSecondary, fontSize = 14.sp)
    CardItem(title = "旁路供电模式", subtitle = "插电玩游戏时直接为主板供电，防止电池发热", tag = "需Root")
    CardItem(title = "智能充电停止", subtitle = "达到 80% 电量时自动断电以延缓电池衰减", tag = "推荐")
    CardItem(title = "充电曲线监控", subtitle = "记录每次充电全程电压与功率波动曲线", tag = "开启")
}

@Composable
fun PerformanceTab(currentMode: String, onSelectMode: (String) -> Unit) {
    Text(text = "性能调节 (Scene方案)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    val modes = listOf(
        Triple("极速模式", "解除温控约束，激进调度 CPU/GPU 超大核满频运行，适合高画质大型游戏", AccentOrange),
        Triple("均衡模式", "系统日常智能调度，动态按需升降频，兼顾滑动流畅度与续航稳定性", AccentBlue),
        Triple("省电模式", "限制大核高频负载，限制后台进程唤醒，大幅度延长亮屏使用时间", AccentGreen)
    )

    modes.forEach { (title, desc, color) ->
        val isSelected = currentMode == title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) CardSubBg else CardBg)
                .clickable { onSelectMode(title) }
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) color else Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = if (isSelected) color else TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
fun ToolsTab(status: SystemStatus) {
    Text(text = "系统极客工具箱", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    CardItem(title = "屏幕刷新率控制", subtitle = "强制锁定 120Hz 全局高刷或 60Hz 节能档位", tag = "${status.refreshRateHz}Hz")
    CardItem(title = "进程墓碑与冻结", subtitle = "模仿 iOS 墓碑挂起机制，防止第三方应用后台耗电偷跑", tag = "运行中")
    CardItem(title = "虚拟内存与 ZRAM 扩展", subtitle = "当前 ZRAM 动态压缩已启用 4.0 GB，大幅减少后台杀后台", tag = "已激活")
    CardItem(title = "屏幕 DPI 物理缩放", subtitle = "修改系统显示密度数值，获取更大视野桌面布局", tag = "默认")
    CardItem(title = "Keybox 密钥与环境校验", subtitle = "查看系统 TEE 硬件密钥与 Play 完整性验证等级", tag = "通过")
}

@Composable
fun SettingsTab() {
    Text(text = "偏好与设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    CardItem(title = "无障碍与免 Root 守护进程", subtitle = "通过无线调试配对自动常驻，免 Root 享受性能调控", tag = "配置")
    CardItem(title = "悬浮窗实时监控指示器", subtitle = "在屏幕左上角显示极小尺寸的 CPU、帧率与功率悬浮胶囊", tag = "未开启")
    CardItem(title = "关于 VoltPulse 极客版", subtitle = "版本: 2.0.0 (MD3 AMOLED Edition)", tag = "检查更新")
}

// 液态药丸底栏组件：使用标准 Compose Draggable 彻底消除手势底层异常
@Composable
fun LiquidFloatingNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("主页", "电源", "性能", "工具", "设置")
    val tabCount = items.size
    val pillTotalWidth = 330.dp
    val itemWidth = pillTotalWidth / tabCount

    val animatedOffset by animateDpAsState(
        targetValue = itemWidth * selectedTab,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillOffset"
    )

    var dragTotal by remember { mutableFloatStateOf(0f) }
    val dragCurrentTab by rememberUpdatedState(selectedTab)

    Box(
        modifier = modifier
            .width(pillTotalWidth)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF16171B))
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    dragTotal += delta
                    if (dragTotal > 60f && dragCurrentTab < tabCount - 1) {
                        onTabSelected(dragCurrentTab + 1)
                        dragTotal = 0f
                    } else if (dragTotal < -60f && dragCurrentTab > 0) {
                        onTabSelected(dragCurrentTab - 1)
                        dragTotal = 0f
                    }
                },
                onDragStopped = { dragTotal = 0f }
            )
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToPx(), 0) }
                .width(itemWidth - 6.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.28f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) AccentBlue else Color(0xFF757575),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(textColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoMiniCard(title: String, value: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
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
fun CardItem(title: String, subtitle: String, tag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CardSubBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tag, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monitor = SystemMonitor(this)

        setContent {
            SceneDarkTheme {
                SceneDashboard(
                    fetchData = { mode -> monitor.getMetrics(mode) },
                    onOpenUsage = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                )
            }
        }
    }
}

val DarkBg = Color(0xFF000000)
val CardBg = Color(0xFF141518)
val CardSubBg = Color(0xFF1C1E23)
val AccentBlue = Color(0xFF2F80ED)
val AccentGreen = Color(0xFF00C853)
val AccentOrange = Color(0xFFFF9100)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFF9E9E9E)

@Composable
fun SceneDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBg,
            surface = CardBg,
            primary = AccentBlue,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

@Composable
fun SceneDashboard(
    fetchData: (String) -> SystemStatus,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> OverviewTab(status, onOpenUsage)
                1 -> PowerTab(status)
                2 -> PerformanceTab(currentPerfMode) { currentPerfMode = it }
                3 -> ToolsTab(status)
                4 -> SettingsTab()
            }
        }

        LiquidFloatingNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}

@Composable
fun OverviewTab(status: SystemStatus, onOpenUsage: () -> Unit) {
    Text(text = "仪表盘看板", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOC 平台: ${status.cpuModel}", color = TextSecondary, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(status.activeMode, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CPU 负载", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.cpuUsagePct}%", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("运存占用", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.ramUsedGb} / ${status.ramTotalGb}G", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("电池电量", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.batteryPct}%", color = if (status.isCharging) AccentGreen else TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }

            val cpuProgress = (status.cpuUsagePct / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = cpuProgress,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AccentBlue,
                trackColor = CardSubBg
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard(title = "核心温度", value = "${status.cpuTempC} °C", sub = "电池 ${status.batteryTempC}°C", modifier = Modifier.weight(1f))
        InfoMiniCard(title = "今日亮屏", value = "${status.screenOnTimeMin / 60}h ${status.screenOnTimeMin % 60}m", sub = "点击授权统计", modifier = Modifier.weight(1f), onClick = onOpenUsage)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard(title = "实时功率", value = "${status.powerWatts} W", sub = if (status.isCharging) "正在极速快充" else "放电功耗", modifier = Modifier.weight(1f))
        InfoMiniCard(title = "存储可用", value = "${(status.storageTotalGb - status.storageUsedGb).toInt()} GB", sub = "总计 ${status.storageTotalGb.toInt()}GB", modifier = Modifier.weight(1f))
    }
}

@Composable
fun PowerTab(status: SystemStatus) {
    Text(text = "电池与充放电", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${status.powerWatts} W",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (status.isCharging) AccentGreen else AccentOrange
            )
            Text(
                text = if (status.isCharging) "⚡ 正在充电中" else "🔋 电池放电功率",
                color = TextSecondary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("实时电压", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.voltageVolts} V", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("实时电流", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.currentAmps} A", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("电池温度", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.batteryTempC} °C", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    Text("充放电策略配置", color = TextSecondary, fontSize = 14.sp)
    CardItem(title = "旁路供电模式", subtitle = "插电玩游戏时直接为主板供电，防止电池发热", tag = "需Root")
    CardItem(title = "智能充电停止", subtitle = "达到 80% 电量时自动断电以延缓电池衰减", tag = "推荐")
    CardItem(title = "充电曲线监控", subtitle = "记录每次充电全程电压与功率波动曲线", tag = "开启")
}

@Composable
fun PerformanceTab(currentMode: String, onSelectMode: (String) -> Unit) {
    Text(text = "性能调节 (Scene方案)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    val modes = listOf(
        Triple("极速模式", "解除温控约束，激进调度 CPU/GPU 超大核满频运行，适合高画质大型游戏", AccentOrange),
        Triple("均衡模式", "系统日常智能调度，动态按需升降频，兼顾滑动流畅度与续航稳定性", AccentBlue),
        Triple("省电模式", "限制大核高频负载，限制后台进程唤醒，大幅度延长亮屏使用时间", AccentGreen)
    )

    modes.forEach { (title, desc, color) ->
        val isSelected = currentMode == title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) CardSubBg else CardBg)
                .clickable { onSelectMode(title) }
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) color else Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = if (isSelected) color else TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@Composable
fun ToolsTab(status: SystemStatus) {
    Text(text = "系统极客工具箱", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    CardItem(title = "屏幕刷新率控制", subtitle = "强制锁定 120Hz 全局高刷或 60Hz 节能档位", tag = "${status.refreshRateHz}Hz")
    CardItem(title = "进程墓碑与冻结", subtitle = "模仿 iOS 墓碑挂起机制，防止第三方应用后台耗电偷跑", tag = "运行中")
    CardItem(title = "虚拟内存与 ZRAM 扩展", subtitle = "当前 ZRAM 动态压缩已启用 4.0 GB，大幅减少后台杀后台", tag = "已激活")
    CardItem(title = "屏幕 DPI 物理缩放", subtitle = "修改系统显示密度数值，获取更大视野桌面布局", tag = "默认")
    CardItem(title = "Keybox 密钥与环境校验", subtitle = "查看系统 TEE 硬件密钥与 Play 完整性验证等级", tag = "通过")
}

@Composable
fun SettingsTab() {
    Text(text = "偏好与设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    CardItem(title = "无障碍与免 Root 守护进程", subtitle = "通过无线调试配对自动常驻，免 Root 享受性能调控", tag = "配置")
    CardItem(title = "悬浮窗实时监控指示器", subtitle = "在屏幕左上角显示极小尺寸的 CPU、帧率与功率悬浮胶囊", tag = "未开启")
    CardItem(title = "关于 VoltPulse 极客版", subtitle = "版本: 2.0.0 (MD3 AMOLED Edition)", tag = "检查更新")
}

@Composable
fun LiquidFloatingNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("主页", "电源", "性能", "工具", "设置")
    val tabCount = items.size
    val pillTotalWidth = 330.dp
    val itemWidth = pillTotalWidth / tabCount

    val animatedOffset by animateDpAsState(
        targetValue = itemWidth * selectedTab,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillOffset"
    )

    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(pillTotalWidth)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF16171B))
            .pointerInput(selectedTab) {
                detectHorizontalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                        if (dragOffset > 50f && selectedTab < tabCount - 1) {
                            onTabSelected(selectedTab + 1)
                            dragOffset = 0f
                        } else if (dragOffset < -50f && selectedTab > 0) {
                            onTabSelected(selectedTab - 1)
                            dragOffset = 0f
                        }
                    }
                )
            }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToPx(), 0) }
                .width(itemWidth - 6.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.28f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) AccentBlue else Color(0xFF757575),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(textColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoMiniCard(title: String, value: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
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
fun CardItem(title: String, subtitle: String, tag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CardSubBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tag, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monitor = SystemMonitor(this)

        setContent {
            SceneDarkTheme {
                SceneDashboard(
                    fetchData = { mode -> monitor.getMetrics(mode) },
                    onOpenUsage = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                )
            }
        }
    }
}

// 纯黑 AMOLED 高对比配色
val DarkBg = Color(0xFF000000)
val CardBg = Color(0xFF141518)
val CardSubBg = Color(0xFF1C1E23)
val AccentBlue = Color(0xFF2F80ED)
val AccentGreen = Color(0xFF00C853)
val AccentOrange = Color(0xFFFF9100)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFF9E9E9E)

@Composable
fun SceneDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBg,
            surface = CardBg,
            primary = AccentBlue,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

@Composable
fun SceneDashboard(
    fetchData: (String) -> SystemStatus,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> OverviewTab(status, onOpenUsage)
                1 -> PowerTab(status)
                2 -> PerformanceTab(currentPerfMode) { currentPerfMode = it }
                3 -> ToolsTab(status)
                4 -> SettingsTab()
            }
        }

        // 仿截图底栏：液态药丸悬浮胶囊 (支持按住左右拖动滑动吸附 + 点击)
        LiquidFloatingNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}

// 1. 仪表盘
@Composable
fun OverviewTab(status: SystemStatus, onOpenUsage: () -> Unit) {
    Text(text = "仪表盘看板", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOC 平台: ${status.cpuModel}", color = TextSecondary, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(status.activeMode, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CPU 负载", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.cpuUsagePct}%", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("运存占用", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.ramUsedGb} / ${status.ramTotalGb}G", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("电池电量", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.batteryPct}%", color = if (status.isCharging) AccentGreen else TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }

            LinearProgressIndicator(
                progress = { (status.cpuUsagePct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AccentBlue,
                trackColor = CardSubBg
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard(title = "核心温度", value = "${status.cpuTempC} °C", sub = "电池 ${status.batteryTempC}°C", modifier = Modifier.weight(1f))
        InfoMiniCard(title = "今日亮屏", value = "${status.screenOnTimeMin / 60}h ${status.screenOnTimeMin % 60}m", sub = "点击授权统计", modifier = Modifier.weight(1f), onClick = onOpenUsage)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard(title = "实时功率", value = "${status.powerWatts} W", sub = if (status.isCharging) "正在极速快充" else "放电功耗", modifier = Modifier.weight(1f))
        InfoMiniCard(title = "存储可用", value = "${(status.storageTotalGb - status.storageUsedGb).toInt()} GB", sub = "总计 ${status.storageTotalGb.toInt()}GB", modifier = Modifier.weight(1f))
    }
}

// 2. 电池功耗
@Composable
fun PowerTab(status: SystemStatus) {
    Text(text = "电池与充放电", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${status.powerWatts} W",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (status.isCharging) AccentGreen else AccentOrange
            )
            Text(
                text = if (status.isCharging) "⚡ 正在充电中" else "🔋 电池放电功率",
                color = TextSecondary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("实时电压", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.voltageVolts} V", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("实时电流", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.currentAmps} A", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("电池温度", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.batteryTempC} °C", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    Text("充放电策略配置", color = TextSecondary, fontSize = 14.sp)
    CardItem(title = "旁路供电模式", subtitle = "插电玩游戏时直接为主板供电，防止电池发热", tag = "需Root")
    CardItem(title = "智能充电停止", subtitle = "达到 80% 电量时自动断电以延缓电池衰减", tag = "推荐")
    CardItem(title = "充电曲线监控", subtitle = "记录每次充电全程电压与功率波动曲线", tag = "开启")
}

// 3. 性能调节
@Composable
fun PerformanceTab(currentMode: String, onSelectMode: (String) -> Unit) {
    Text(text = "性能调节 (Scene方案)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    val modes = listOf(
        Triple("极速模式", "解除温控约束，激进调度 CPU/GPU 超大核满频运行，适合高画质大型游戏", AccentOrange),
        Triple("均衡模式", "系统日常智能调度，动态按需升降频，兼顾滑动流畅度与续航稳定性", AccentBlue),
        Triple("省电模式", "限制大核高频负载，限制后台进程唤醒，大幅度延长亮屏使用时间", AccentGreen)
    )

    modes.forEach { (title, desc, color) ->
        val isSelected = currentMode == title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) CardSubBg else CardBg)
                .clickable { onSelectMode(title) }
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) color else Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = if (isSelected) color else TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

// 4. 极客工具箱
@Composable
fun ToolsTab(status: SystemStatus) {
    Text(text = "系统极客工具箱", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    CardItem(title = "屏幕刷新率控制", subtitle = "强制锁定 120Hz 全局高刷或 60Hz 节能档位", tag = "${status.refreshRateHz}Hz")
    CardItem(title = "进程墓碑与冻结", subtitle = "模仿 iOS 墓碑挂起机制，防止第三方应用后台耗电偷跑", tag = "运行中")
    CardItem(title = "虚拟内存与 ZRAM 扩展", subtitle = "当前 ZRAM 动态压缩已启用 4.0 GB，大幅减少后台杀后台", tag = "已激活")
    CardItem(title = "屏幕 DPI 物理缩放", subtitle = "修改系统显示密度数值，获取更大视野桌面布局", tag = "默认")
    CardItem(title = "Keybox 密钥与环境校验", subtitle = "查看系统 TEE 硬件密钥与 Play 完整性验证等级", tag = "通过")
}

// 5. 设置
@Composable
fun SettingsTab() {
    Text(text = "偏好与设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    CardItem(title = "无障碍与免 Root 守护进程", subtitle = "通过无线调试配对自动常驻，免 Root 享受性能调控", tag = "配置")
    CardItem(title = "悬浮窗实时监控指示器", subtitle = "在屏幕左上角显示极小尺寸的 CPU、帧率与功率悬浮胶囊", tag = "未开启")
    CardItem(title = "关于 VoltPulse 极客版", subtitle = "版本: 2.0.0 (MD3 AMOLED Edition)", tag = "检查更新")
}

// 液态药丸底栏组件
@Composable
fun LiquidFloatingNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("主页", "电源", "性能", "工具", "设置")
    val tabCount = items.size
    val pillTotalWidth = 330.dp
    val itemWidth = pillTotalWidth / tabCount

    val animatedOffset by animateDpAsState(
        targetValue = itemWidth * selectedTab,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillOffset"
    )

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(pillTotalWidth)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF16171B))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount.x
                        if (dragAccumulator > 70f && selectedTab < tabCount - 1) {
                            onTabSelected(selectedTab + 1)
                            dragAccumulator = 0f
                        } else if (dragAccumulator < -70f && selectedTab > 0) {
                            onTabSelected(selectedTab - 1)
                            dragAccumulator = 0f
                        }
                    }
                )
            }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToPx(), 0) }
                .width(itemWidth - 6.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.28f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) AccentBlue else Color(0xFF757575),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(textColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoMiniCard(title: String, value: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
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
fun CardItem(title: String, subtitle: String, tag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CardSubBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tag, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monitor = SystemMonitor(this)

        setContent {
            SceneDarkTheme {
                SceneDashboard(
                    fetchData = { mode -> monitor.getMetrics(mode) },
                    onOpenUsage = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                )
            }
        }
    }
}

// 纯黑 AMOLED 高对比极客主题配色
val DarkBg = Color(0xFF000000)
val CardBg = Color(0xFF141518)
val CardSubBg = Color(0xFF1C1E23)
val AccentBlue = Color(0xFF2F80ED)
val AccentGreen = Color(0xFF00C853)
val AccentOrange = Color(0xFFFF9100)
val TextPrimary = Color(0xFFEEEEEE)
val TextSecondary = Color(0xFF9E9E9E)

@Composable
fun SceneDarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBg,
            surface = CardBg,
            primary = AccentBlue,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

@Composable
fun SceneDashboard(
    fetchData: (String) -> SystemStatus,
    onOpenUsage: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentPerfMode by remember { mutableStateOf("均衡模式") }
    var status by remember { mutableStateOf(fetchData(currentPerfMode)) }

    // 动态 1.5 秒轮询刷新硬件各项性能数据
    LaunchedEffect(currentPerfMode) {
        while (true) {
            status = fetchData(currentPerfMode)
            delay(1500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // 主内容滑动区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> OverviewTab(status, onOpenUsage)
                1 -> PowerTab(status)
                2 -> PerformanceTab(currentPerfMode) { currentPerfMode = it }
                3 -> ToolsTab(status)
                4 -> SettingsTab()
            }
        }

        // 底部：仿截图高质感悬浮液态胶囊导航栏（支持手势拖动滑动 & 点击）
        LiquidFloatingNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}

// ==================== 1. 主控看板 Tab ====================
@Composable
fun OverviewTab(status: SystemStatus, onOpenUsage: () -> Unit) {
    Text(
        text = "仪表盘看板",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )

    // 核心平台状态大卡片
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOC 平台: ${status.cpuModel}", color = TextSecondary, fontSize = 14.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(status.activeMode, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CPU 负载", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.cpuUsagePct}%", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("运存占用", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.ramUsedGb} / ${status.ramTotalGb}G", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("电池电量", color = TextSecondary, fontSize = 13.sp)
                    Text("${status.batteryPct}%", color = if (status.isCharging) AccentGreen else TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 进度指示条
            LinearProgressIndicator(
                progress = { status.cpuUsagePct / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = AccentBlue,
                trackColor = CardSubBg
            )
        }
    }

    // 快捷状态卡片栅格
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard(title = "核心温度", value = "${status.cpuTempC} °C", sub = "电池 ${status.batteryTempC}°C", modifier = Modifier.weight(1f))
        InfoMiniCard(title = "今日亮屏", value = "${status.screenOnTimeMin / 60}h ${status.screenOnTimeMin % 60}m", sub = "点击授权统计", modifier = Modifier.weight(1f), onClick = onOpenUsage)
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoMiniCard(title = "实时功率", value = "${status.powerWatts} W", sub = if (status.isCharging) "正在极速快充" else "放电功耗", modifier = Modifier.weight(1f))
        InfoMiniCard(title = "存储可用", value = "${(status.storageTotalGb - status.storageUsedGb).toInt()} GB", sub = "总计 ${status.storageTotalGb.toInt()}GB", modifier = Modifier.weight(1f))
    }
}

// ==================== 2. 电源与功耗 Tab ====================
@Composable
fun PowerTab(status: SystemStatus) {
    Text(text = "电池与充放电", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${status.powerWatts} W",
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (status.isCharging) AccentGreen else AccentOrange
            )
            Text(
                text = if (status.isCharging) "⚡ 正在充电中" else "🔋 电池放电功率",
                color = TextSecondary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("实时电压", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.voltageVolts} V", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("实时电流", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.currentAmps} A", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("电池温度", color = TextSecondary, fontSize = 12.sp)
                    Text("${status.batteryTempC} °C", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // 充放电记录卡片
    Text("充放电策略配置", color = TextSecondary, fontSize = 14.sp)
    CardItem(title = "旁路供电模式", subtitle = "插电玩游戏时直接为主板供电，防止电池发热", tag = "需Root")
    CardItem(title = "智能充电停止", subtitle = "达到 80% 电量时自动断电以延缓电池衰减", tag = "推荐")
    CardItem(title = "充电曲线监控", subtitle = "记录每次充电全程电压与功率波动曲线", tag = "开启")
}

// ==================== 3. 性能模式调控 Tab (Scene风格) ====================
@Composable
fun PerformanceTab(currentMode: String, onSelectMode: (String) -> Unit) {
    Text(text = "性能调节 (Scene方案)", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    val modes = listOf(
        Triple("极速模式", "解除温控约束，激进调度 CPU/GPU 超大核满频运行，适合高画质大型游戏", AccentOrange),
        Triple("均衡模式", "系统日常智能调度，动态按需升降频，兼顾滑动流畅度与续航稳定性", AccentBlue),
        Triple("省电模式", "限制大核高频负载，限制后台进程唤醒，大幅度延长亮屏使用时间", AccentGreen)
    )

    modes.forEach { (title, desc, color) ->
        val isSelected = currentMode == title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (isSelected) CardSubBg else CardBg)
                .clickable { onSelectMode(title) }
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) color else Color.DarkGray)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = if (isSelected) color else TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(desc, color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

// ==================== 4. 极客工具箱 Tab ====================
@Composable
fun ToolsTab(status: SystemStatus) {
    Text(text = "系统极客工具箱", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    CardItem(title = "屏幕刷新率控制", subtitle = "强制锁定 120Hz 全局高刷或 60Hz 节能档位", tag = "${status.refreshRateHz}Hz")
    CardItem(title = "进程墓碑与冻结", subtitle = "模仿 iOS 墓碑挂起机制，防止第三方应用后台耗电偷跑", tag = "运行中")
    CardItem(title = "虚拟内存与 ZRAM 扩展", subtitle = "当前 ZRAM 动态压缩已启用 4.0 GB，大幅减少后台杀后台", tag = "已激活")
    CardItem(title = "屏幕 DPI 物理缩放", subtitle = "修改系统显示密度数值，获取更大视野桌面布局", tag = "默认")
    CardItem(title = "Keybox 密钥与环境校验", subtitle = "查看系统 TEE 硬件密钥与 Play 完整性验证等级", tag = "通过")
}

// ==================== 5. 设置 Tab ====================
@Composable
fun SettingsTab() {
    Text(text = "偏好与设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    CardItem(title = "无障碍与免 Root 守护进程", subtitle = "通过无线调试配对自动常驻，免 Root 享受性能调控", tag = "配置")
    CardItem(title = "悬浮窗实时监控指示器", subtitle = "在屏幕左上角显示极小尺寸的 CPU、帧率与功率悬浮胶囊", tag = "未开启")
    CardItem(title = "关于 VoltPulse 极客版", subtitle = "版本: 2.0.0 (MD3 AMOLED Edition)", tag = "检查更新")
}

// ==================== 仿截图底栏：液态药丸悬浮胶囊 (滑动 + 弹性动画) ====================
@Composable
fun LiquidFloatingNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("主页", "电源", "性能", "工具", "设置")
    val tabCount = items.size
    val pillTotalWidth = 330.dp
    val itemWidth = pillTotalWidth / tabCount

    // 药丸滑动弹性动画
    val animatedOffset by animateDpAsState(
        targetValue = itemWidth * selectedTab,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pillOffset"
    )

    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(pillTotalWidth)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF16171B)) // 截图中的胶囊深灰底色
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount.x
                        if (dragAccumulator > 70f && selectedTab < tabCount - 1) {
                            onTabSelected(selectedTab + 1)
                            dragAccumulator = 0f
                        } else if (dragAccumulator < -70f && selectedTab > 0) {
                            onTabSelected(selectedTab - 1)
                            dragAccumulator = 0f
                        }
                    }
                )
            }
            .padding(6.dp)
    ) {
        // 液态滑动指示球 (蓝色选中底块)
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToPx(), 0) }
                .width(itemWidth - 6.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.28f))
        )

        // 导航按钮
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedTab == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) AccentBlue else Color(0xFF757575),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // 模仿截图的图标与圆点标识
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(textColor)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// 辅助小型组件
@Composable
fun InfoMiniCard(title: String, value: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
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
fun CardItem(title: String, subtitle: String, tag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CardSubBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tag, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
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
