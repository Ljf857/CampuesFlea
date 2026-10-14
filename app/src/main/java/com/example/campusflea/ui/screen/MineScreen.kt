package com.example.campusflea.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.components.GlassCard
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.components.pressScale
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.MineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(navController: NavHostController, rootNavController: NavHostController, viewModel: MineViewModel = viewModel()) {
    val user by viewModel.userInfo.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var showLoginDialog by remember { mutableStateOf(false) }

    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("提示") },
            text = { Text("请先登录") },
            confirmButton = {
                TextButton(onClick = { showLoginDialog = false; rootNavController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GlassTopBar(title = { Text("我的", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.padding(start = 16.dp)) }, actions = { IconButton(onClick = { navController.navigate(Screen.Settings.route) }) { Icon(Icons.Default.Settings, "设置") } })
        LaunchedEffect(Unit) { if (isLoggedIn) viewModel.loadUserInfo() }

        GlassCard(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = 1.dp,
            onClick = { if (isLoggedIn) navController.navigate(Screen.EditProfile.route) else showLoginDialog = true }
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape)) {
                if (user?.avatar?.isNotBlank() == true) {
                    NetworkImage(model = user!!.avatar, contentDescription = "头像", modifier = Modifier.fillMaxSize())
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text((user?.nickname ?: "U").take(1), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isLoggedIn) (user?.nickname ?: "未登录") else "未登录", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isLoggedIn) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                    if (user?.verifyStatus == 1) { Spacer(Modifier.width(8.dp)); Surface(shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text("已认证", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) } }
                }
                Text(user?.phone?.takeIf { it.length >= 7 }?.replaceRange(3, 7, "****") ?: (if (isLoggedIn) "" else "点击登录"), fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                // 这里以前写死了一句「热爱生活，喜欢读书」，跟账号里真实的个性签名毫无关系：
                // 「我的」页显示着一句签名，点进「编辑资料」却是空的，两个页面必然对不上。
                Text(
                    if (isLoggedIn) {
                        user?.signature?.takeIf { it.isNotBlank() } ?: "还没有个性签名"
                    } else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
        }
        if (isLoggedIn) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                listOf("发布" to (user?.stats?.goodsCount ?: 0), "已售" to (user?.stats?.soldCount ?: 0), "收藏" to (user?.stats?.favoriteCount ?: 0)).forEach { (label, count) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text("$count", fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) }
                }
            }
        }
        HorizontalDivider()
        listOf(
            Icons.Default.ShoppingCart to "我的订单" to { if (isLoggedIn) navController.navigate(Screen.MyOrders.route) else showLoginDialog = true },
            Icons.Default.Star to "我的收藏" to { if (isLoggedIn) navController.navigate(Screen.Favorite.route) else showLoginDialog = true },
            Icons.Default.History to "浏览历史" to { if (isLoggedIn) navController.navigate(Screen.History.route) else showLoginDialog = true },
            Icons.Default.LocationOn to "收货地址" to { if (isLoggedIn) navController.navigate(Screen.AddressList.route) else showLoginDialog = true },
            Icons.Default.VerifiedUser to "实名认证" to { if (isLoggedIn) navController.navigate(Screen.Verify.route) else showLoginDialog = true },
            Icons.Default.Feedback to "意见反馈" to { navController.navigate(Screen.Feedback.route) },
            Icons.Default.Info to "关于我们" to { navController.navigate(Screen.About.route) }
        ).forEach { (pair, action) ->
            val (icon, title) = pair
            ListItem(headlineContent = { Text(title) }, leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline) }, modifier = Modifier.pressScale(role = Role.Button, onClick = action))
            HorizontalDivider()
        }
        if (isLoggedIn) {
            Box(Modifier.fillMaxWidth().padding(16.dp)) {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth().pressScale(role = Role.Button, onClick = { viewModel.logout { rootNavController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } } })) {
                    Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                        Text("退出登录", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
