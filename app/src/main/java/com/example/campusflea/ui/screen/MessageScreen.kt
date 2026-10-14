package com.example.campusflea.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.components.GlassCard
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.MessageViewModel
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.components.pressScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessageScreen(navController: NavHostController, viewModel: MessageViewModel = viewModel()) {
    val conversations by viewModel.conversations.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadConversations()
        }
    }
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("消息", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(start = 16.dp)) })
        GlassCard(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp),
            elevation = 1.dp,
            onClick = { navController.navigate(Screen.Notification.route) }
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, "通知", tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) { Text("系统通知", fontWeight = FontWeight.Medium); Text("查看通知", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
                Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline)
            }
        }
        LazyColumn {
            items(conversations, key = { it.id }) { conv ->
                ListItem(
                    headlineContent = { Row { Text(conv.targetUser?.nickname ?: "", fontWeight = FontWeight.Medium); Spacer(Modifier.weight(1f)); Text(conv.lastTime, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) } },
                    supportingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 同一对用户可能因不同商品存在多个会话：只显示昵称会出现两个一模一样的条目，
                            // 用户根本分不清点的是哪一个。带上商品标题才可区分。
                            if (conv.goodsTitle.isNotBlank()) {
                                Text(
                                    "【${conv.goodsTitle}】",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.widthIn(max = 110.dp)
                                )
                            }
                            Text(conv.lastMessage, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            if (conv.unreadCount > 0) Badge { Text("${conv.unreadCount}") }
                        }
                    },
                    leadingContent = { Box(Modifier.size(48.dp).clip(CircleShape), contentAlignment = Alignment.Center) { Text((conv.targetUser?.nickname ?: "U").take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } },
                    // 带上 conversationId：会话列表是唯一确知「用户点的是哪个会话」的地方，
                    // 只靠 userId 会让服务端在同一对用户有多个商品会话时选错
                    modifier = Modifier.animateItem(fadeInSpec = tween(200)).pressScale(onClick = {
                        conv.targetUser?.let {
                            navController.navigate(
                                Screen.Chat.createRoute(
                                    userId = it.id,
                                    userName = it.nickname,
                                    // 双保险：conversationId 是权威标识；goodsId 让服务端在会话 ID 失效
                                    // （例如该会话已被删除）时也能定位到同一件商品的会话，而不是凭空新建一个
                                    goodsId = conv.goodsId.takeIf { id -> id > 0 },
                                    conversationId = conv.id
                                )
                            )
                        }
                    })
                )
                HorizontalDivider()
            }
        }
    }
}
