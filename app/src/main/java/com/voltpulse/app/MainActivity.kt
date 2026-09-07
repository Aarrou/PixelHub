package com.voltpulse.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

// 纯黑 AMOLED 高对比配色
val DarkBg = Color(0xFF000000)
val CardBg = Color(0xFF131418)
val CardSubBg = Color(0xFF1B1D24)
val AccentBlue = Color(0xFF3872FF)
val AccentCyan = Color(0xFF00E5FF)
val AccentGreen = Color(0xFF00E676)
val AccentOrange = Color(0xFFFF9100)
val TextPrimary = Color(0xFFF0F2F5)
val TextSecondary = Color(0xFF8E929B)

// 液态毛玻璃调色盘
val GlassBgStart = Color(0xD9161822)
val GlassBgEnd = Color(0xCC0E1017)
val GlassBorderTop = Color(0x55FFFFFF)
val GlassBorderBottom = Color(0x0DFFFFFF)
val LiquidPillStart = Color(0x403872FF)
val LiquidPillEnd = Color(0x2B00E5FF)
val LiquidPillBorder = Color(0x803872FF)

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

        // 底部液态玻璃悬浮药丸底栏（支持滑动触感震动反馈）
        LiquidGlassNavigationBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 26.dp)
        )
    }
}

// ==================== 1. 仪表盘看板 ====================
@Composable
fun OverviewTab(status: SystemStatus, onOpenUsage: () -> Unit) {
    Text(text = "仪表盘看板", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOC 平台: ${status.cpuModel}", color = TextSecondary, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
                        .background(Brush.horizontalGradient(listOf(AccentBlue, AccentCyan)))
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

// ==================== 2. 电池与功耗 ====================
@Composable
fun PowerTab(status: SystemStatus) {
    Text(text = "电池与充放电", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(22.dp))
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

// ==================== 3. 性能模式调控 ====================
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
                .border(
                    BorderStroke(1.dp, if (isSelected) color.copy(alpha = 0.5f) else Color(0x1AFFFFFF)),
                    RoundedCornerShape(18.dp)
                )
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

// ==================== 4. 极客工具箱 ====================
@Composable
fun ToolsTab(status: SystemStatus) {
    Text(text = "系统极客工具箱", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

    CardItem(title = "屏幕刷新率控制", subtitle = "强制锁定 120Hz 全局高刷或 60Hz 节能档位", tag = "${status.refreshRateHz}Hz")
    CardItem(title = "进程墓碑与冻结", subtitle = "模仿 iOS 墓碑挂起机制，防止第三方应用后台耗电偷跑", tag = "运行中")
    CardItem(title = "虚拟内存与 ZRAM 扩展", subtitle = "当前 ZRAM 动态压缩已启用 4.0 GB，大幅减少后台杀后台", tag = "已激活")
    CardItem(title = "屏幕 DPI 物理缩放", subtitle = "修改系统显示密度数值，获取更大视野桌面布局", tag = "默认")
    CardItem(title = "Keybox 密钥与环境校验", subtitle = "查看系统 TEE 硬件密钥与 Play 完整性验证等级", tag = "通过")
}

// ==================== 5. 设置 ====================
@Composable
fun SettingsTab() {
    Text(text = "偏好与设置", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    CardItem(title = "无障碍与免 Root 守护进程", subtitle = "通过无线调试配对自动常驻，免 Root 享受性能调控", tag = "配置")
    CardItem(title = "悬浮窗实时监控指示器", subtitle = "在屏幕左上角显示极小尺寸的 CPU、帧率与功率悬浮胶囊", tag = "未开启")
    CardItem(title = "关于 VoltPulse 极客版", subtitle = "版本: 2.0.0 (Liquid Glass Edition)", tag = "检查更新")
}

// ==================== 震动反馈驱动器 ====================
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

// ==================== 核心：液态毛玻璃悬浮底栏 ====================
@Composable
fun LiquidGlassNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("主页", "电源", "性能", "工具", "设置")
    val tabCount = items.size
    val pillTotalWidth = 336.dp
    val itemWidthDp = pillTotalWidth / tabCount

    val view = LocalView.current

    // 液态弹簧弹性位移动画
    val animatedPosition by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "liquidPill"
    )

    // 外层容器：液态毛玻璃外壳
    Box(
        modifier = modifier
            .width(pillTotalWidth)
            .height(66.dp)
            .clip(RoundedCornerShape(33.dp))
            .background(Brush.verticalGradient(listOf(GlassBgStart, GlassBgEnd)))
            .border(
                BorderStroke(1.2.dp, Brush.verticalGradient(listOf(GlassBorderTop, GlassBorderBottom))),
                RoundedCornerShape(33.dp)
            )
            // 手势监听：按住滑动持续触发细腻 tick，松手/点击即刻响应
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
                                    triggerSegmentTick(view) // 滑动跨越选项卡触发细腻微震
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
        // 顶部玻璃菲涅尔高光反射线
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(pillTotalWidth * 0.7f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )

        // 液态发光活动药丸底块
        Box(
            modifier = Modifier
                .offset {
                    IntOffset((animatedPosition * itemWidthDp.toPx()).roundToInt(), 0)
                }
                .width(itemWidthDp - 4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(LiquidPillStart, LiquidPillEnd)))
                .border(
                    BorderStroke(1.dp, Brush.verticalGradient(listOf(LiquidPillBorder, Color(0x3300E5FF)))),
                    RoundedCornerShape(28.dp)
                )
        )

        // 标签文字与图标指示点
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedTab == index

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AccentCyan else Color(0xFF6B7280))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = label,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// 辅助组件
@Composable
fun InfoMiniCard(title: String, value: String, sub: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(18.dp))
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
    }
}

@Composable
fun CardItem(title: String, subtitle: String, tag: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(18.dp))
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
                    .border(BorderStroke(1.dp, Color(0x263872FF)), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(tag, color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
