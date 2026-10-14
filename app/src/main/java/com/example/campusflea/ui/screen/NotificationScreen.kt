package com.example.campusflea.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.viewmodel.MessageViewModel
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationScreen(navController: NavHostController, viewModel: MessageViewModel = viewModel()) {
    val notifications by viewModel.notifications.collectAsState()
    val error by viewModel.notificationsError.collectAsState()
    val loading by viewModel.notificationsLoading.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadNotifications() }
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("系统通知") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })

        when {
            // 首次加载：还没有任何数据可展示时才占位，有旧数据就不打断阅读
            loading && notifications.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

            notifications.isEmpty() && error != null ->
                NotificationsMessageState(
                    icon = Icons.Default.WifiOff,
                    title = "通知加载失败",
                    description = error.orEmpty(),
                    actionLabel = "重新加载",
                    onAction = { viewModel.loadNotifications() }
                )

            notifications.isEmpty() ->
                NotificationsMessageState(
                    icon = Icons.Default.NotificationsNone,
                    title = "暂无通知",
                    description = "系统通知和交易提醒会出现在这里",
                    actionLabel = "刷新",
                    onAction = { viewModel.loadNotifications() }
                )

            else -> LazyColumn {
                // 已有旧数据时刷新失败：不整页替换成错误页，只在顶部提示并给重试入口
                if (error != null) {
                    item {
                        LoadFailedBanner(message = error.orEmpty(), onRetry = { viewModel.loadNotifications() })
                    }
                }
                items(notifications, key = { it.id }) { n ->
                    ListItem(modifier = Modifier.animateItem(fadeInSpec = tween(200)), headlineContent = { Text(n.title, fontWeight = if (n.isRead == 0) FontWeight.Bold else FontWeight.Normal) }, supportingContent = { Text(n.content, maxLines = 2) }, leadingContent = { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary) })
                    HorizontalDivider()
                }
            }
        }
    }
}

/** 加载失败但手里还有旧数据时的顶部提示条。 */
@Composable
private fun LoadFailedBanner(message: String, onRetry: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onErrorContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

/** 整页空态 / 错误态：与首页、分类页保持同一套观感。 */
@Composable
private fun NotificationsMessageState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(8.dp), modifier = Modifier.heightIn(min = 48.dp)) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
