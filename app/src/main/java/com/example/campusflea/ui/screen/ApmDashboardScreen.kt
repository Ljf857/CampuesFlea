package com.example.campusflea.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.campusflea.apm.Apm
import com.example.campusflea.apm.ApmCrashRecord
import com.example.campusflea.apm.ApmNetworkRecord
import com.example.campusflea.apm.ApmSnapshot
import com.example.campusflea.ui.components.GlassTopBar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 性能监控面板（开发者工具）。
 *
 * 只读 [Apm.snapshot] 的拷贝并以 1 秒节奏刷新，不订阅任何持续流：
 * 面板是"打开时看一眼"的调试工具，常驻的响应式更新会持续触发重组，
 * 反而会把自己的存在变成被观测对象的一部分，污染 FPS 数据。
 */
@Composable
fun ApmDashboardScreen(navController: NavHostController) {
    var snapshot by remember { mutableStateOf(Apm.snapshot()) }
    var autoRefresh by remember { mutableStateOf(true) }

    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            snapshot = Apm.snapshot()
            delay(1_000L)
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        GlassTopBar(
            title = { Text("性能监控") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            }
        )

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            OverviewCard(
                snapshot = snapshot,
                autoRefresh = autoRefresh,
                onToggleRefresh = { autoRefresh = !autoRefresh },
                onFlush = { Apm.flushNow() },
                onReset = {
                    Apm.reset()
                    snapshot = Apm.snapshot()
                }
            )

            StartupSection(snapshot)
            FrameSection(snapshot)
            NetworkSection(snapshot)
            SlowRequestSection(snapshot)
            SceneSection(snapshot)
            CrashSection(snapshot)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OverviewCard(
    snapshot: ApmSnapshot,
    autoRefresh: Boolean,
    onToggleRefresh: () -> Unit,
    onFlush: () -> Unit,
    onReset: () -> Unit
) {
    SectionCard("采集概览") {
        KeyValueRow("采集状态", if (snapshot.enabled) "运行中" else "已关闭")

        val target = Apm.uploadUrl
        KeyValueRow(
            label = "上报地址",
            value = if (target.isBlank()) "未配置（仅端上采集）" else target,
            valueColor = if (target.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
        )
        KeyValueRow("待上报条目", snapshot.pendingUpload.toString())
        KeyValueRow("累计上报", snapshot.uploadedTotal.toString())
        KeyValueRow(
            label = "最近上报",
            value = when {
                snapshot.lastUploadAt == 0L -> "尚未上报"
                snapshot.lastUploadOk -> "${formatTime(snapshot.lastUploadAt)} 成功"
                else -> "${formatTime(snapshot.lastUploadAt)} 失败"
            },
            valueColor = when {
                snapshot.lastUploadAt == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                snapshot.lastUploadOk -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionChip(
                text = if (autoRefresh) "暂停刷新" else "继续刷新",
                modifier = Modifier.weight(1f),
                onClick = onToggleRefresh
            )
            ActionChip(text = "立即上报", modifier = Modifier.weight(1f), onClick = onFlush)
            ActionChip(text = "重置数据", modifier = Modifier.weight(1f), onClick = onReset)
        }
    }
}

@Composable
private fun StartupSection(snapshot: ApmSnapshot) {
    val startup = snapshot.startup ?: return
    SectionCard("冷启动") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = startup.totalMs.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (startup.totalMs <= 1_000) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(" ms", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text(
            text = if (startup.cold) "冷启动 · ${formatTime(startup.timestamp)}" else "热启动 · ${formatTime(startup.timestamp)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        startup.stages.forEach { (stage, cost) ->
            KeyValueRow(stage, "${cost} ms")
        }
    }
}

@Composable
private fun FrameSection(snapshot: ApmSnapshot) {
    val frame = snapshot.latestFrame
    SectionCard("流畅度") {
        if (frame == null) {
            EmptyHint("暂未采集到帧数据，前台运行几秒后回来查看")
            return@SectionCard
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = String.format(Locale.US, "%.1f", snapshot.avgFps),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (snapshot.avgFps >= 50f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Text(" FPS", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text("最近 ${frame.windowMs / 1000} 秒窗口", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))

        RateBar(label = "卡顿帧占比", rate = snapshot.jankRate, good = snapshot.jankRate < 0.05f)
        Spacer(Modifier.height(6.dp))
        KeyValueRow("窗口内帧数", frame.totalFrames.toString())
        KeyValueRow("卡顿帧", frame.jankyFrames.toString())
        KeyValueRow(
            label = "冻结帧（>700ms）",
            value = frame.frozenFrames.toString(),
            valueColor = if (frame.frozenFrames > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        KeyValueRow("最差单帧", "${frame.worstFrameMs} ms")
    }
}

@Composable
private fun NetworkSection(snapshot: ApmSnapshot) {
    val net = snapshot.network
    SectionCard("网络请求") {
        if (net.total == 0L) {
            EmptyHint("暂无网络请求记录")
            return@SectionCard
        }
        RateBar(
            label = "请求成功率",
            rate = net.successRate,
            good = (net.successRate ?: 0f) >= 0.99f
        )
        Spacer(Modifier.height(6.dp))
        // 分母排除鉴权失败，这里把两个数都摊开：多少是真实失败、多少只是当时没登录
        KeyValueRow("总请求 / 失败", "${net.total} / ${net.failure}")
        KeyValueRow("其中鉴权失败（401/403）", net.authFailure.toString())
        KeyValueRow("平均耗时", "${net.avg} ms")
        KeyValueRow("P50", "${net.p50} ms")
        KeyValueRow(
            label = "P90",
            value = "${net.p90} ms",
            valueColor = if (net.p90 > 500) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        KeyValueRow(
            label = "P99",
            value = "${net.p99} ms",
            valueColor = if (net.p99 > 1_000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        KeyValueRow("最大值", "${net.max} ms")
    }
}

@Composable
private fun SlowRequestSection(snapshot: ApmSnapshot) {
    SectionCard("慢请求 / 失败请求 Top") {
        if (snapshot.slowRequests.isEmpty()) {
            EmptyHint("暂未发现慢请求或失败请求")
            return@SectionCard
        }
        snapshot.slowRequests.forEach { record ->
            RequestRow(record)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RequestRow(record: ApmNetworkRecord) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${record.method} ${record.url}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${record.durationMs} ms",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (record.success) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }
        Text(
            text = buildString {
                append("状态 ").append(if (record.code > 0) record.code.toString() else record.errorType ?: "无响应")
                append(" · ").append(formatTime(record.timestamp))
                if (record.responseBytes > 0) append(" · ").append(formatBytes(record.responseBytes))
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SceneSection(snapshot: ApmSnapshot) {
    SectionCard("自定义埋点") {
        if (snapshot.scenes.isEmpty()) {
            EmptyHint("暂无埋点数据。业务侧可通过 Apm.beginScene / endScene 记录任意区间耗时")
            return@SectionCard
        }
        snapshot.scenes.forEach { scene ->
            KeyValueRow(scene.name, "${scene.durationMs} ms")
        }
    }
}

@Composable
private fun CrashSection(snapshot: ApmSnapshot) {
    SectionCard("崩溃 / ANR") {
        if (snapshot.crashes.isEmpty()) {
            EmptyHint("未捕获到崩溃或 ANR")
            return@SectionCard
        }
        snapshot.crashes.forEach { record ->
            CrashRow(record)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun CrashRow(record: ApmCrashRecord) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (record.isAnr) "ANR" else "CRASH",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "${if (record.foreground) "前台" else "后台"} · ${formatTime(record.timestamp)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Text(record.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "线程 ${record.threadName}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = record.stack.lineSequence().take(MAX_STACK_LINES).joinToString("\n"),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (record.stack.lineSequence().count() > MAX_STACK_LINES) {
            Text("… 完整堆栈见上报数据", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

// ---------------- 通用小组件 ----------------

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
        }
    }
}

@Composable
private fun KeyValueRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/**
 * 比率条。[rate] 为 null 表示「没有可判定的样本」，此时显示「—」并收起填充条，
 * 而不是画成 0%——0% 会被读成「全都失败了」，与事实相反。
 */
@Composable
private fun RateBar(label: String, rate: Float?, good: Boolean) {
    val color = if (good) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val safe = rate?.coerceIn(0f, 1f) ?: 0f
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = if (rate == null) "—" else "${(safe * 100).roundToInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (rate == null) MaterialTheme.colorScheme.outline else color
            )
        }
        Spacer(Modifier.height(5.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(safe)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ActionChip(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private const val MAX_STACK_LINES = 8
