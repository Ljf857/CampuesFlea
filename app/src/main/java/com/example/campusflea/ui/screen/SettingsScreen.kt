package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.BuildConfig
import com.example.campusflea.data.local.CacheManager
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.theme.ThemePreferences
import com.example.campusflea.ui.viewmodel.MineViewModel
import kotlinx.coroutines.launch
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController, viewModel: MineViewModel = viewModel()) {
    val ctx = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userInfo by viewModel.userInfo.collectAsState()
    val themeMode by remember { ThemePreferences(ctx) }.themeMode.collectAsState(initial = 0)
    var cacheSize by remember { mutableStateOf(0L) }
    var isCalculating by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        cacheSize = CacheManager.getTotalCacheSize(ctx)
        isCalculating = false
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        GlassTopBar(title = { Text("设置") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("账号", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column {
                    ListItem(headlineContent = { Text("修改密码") }, leadingContent = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline) }, modifier = Modifier.clickable(role = Role.Button) { navController.navigate(Screen.ChangePassword.route) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(headlineContent = { Text("绑定手机") }, supportingContent = { Text(userInfo?.phone?.takeIf { it.length >= 7 }?.replaceRange(3, 7, "****") ?: "未绑定") }, leadingContent = { Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary) }, modifier = Modifier.clickable(role = Role.Button) { navController.navigate(Screen.ChangePhone.route) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("通用", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column {
                    ListItem(headlineContent = { Text("主题模式") }, supportingContent = { Text(when (themeMode) { ThemePreferences.MODE_DARK -> "深色模式"; ThemePreferences.MODE_LIGHT -> "明亮模式"; else -> "跟随系统" }) }, leadingContent = { Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline) }, modifier = Modifier.clickable(role = Role.Button) { navController.navigate(Screen.ThemeSettings.route) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(headlineContent = { Text("清除缓存") }, supportingContent = { Text(if (isCalculating) "计算中..." else CacheManager.formatSize(cacheSize)) }, leadingContent = { Icon(Icons.Default.DeleteSweep, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline) }, modifier = Modifier.clickable(role = Role.Button) {
                        scope.launch {
                            CacheManager.clearAll(ctx)
                            cacheSize = CacheManager.getTotalCacheSize(ctx)
                            Toast.makeText(ctx, "缓存已清理", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("其他", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column {
                    ListItem(headlineContent = { Text("意见反馈") }, leadingContent = { Icon(Icons.Default.Feedback, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline) }, modifier = Modifier.clickable(role = Role.Button) { navController.navigate(Screen.Feedback.route) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    // 性能监控面板：开发者自查入口，仅 debug 包可见，避免把调试工具暴露给正式用户
                    if (BuildConfig.DEBUG) {
                        ListItem(headlineContent = { Text("性能监控") }, supportingContent = { Text("网络耗时 · 卡顿 · 启动 · 崩溃") }, leadingContent = { Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline) }, modifier = Modifier.clickable(role = Role.Button) { navController.navigate(Screen.ApmDashboard.route) })
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    ListItem(headlineContent = { Text("关于我们") }, leadingContent = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline) }, modifier = Modifier.clickable(role = Role.Button) { navController.navigate(Screen.About.route) })
                }
            }
            Spacer(Modifier.height(24.dp))
            if (isLoggedIn) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { viewModel.logout { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }; Toast.makeText(ctx, "已退出登录", Toast.LENGTH_SHORT).show() } }) {
                        Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                            Text("退出登录", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("v1.0.0", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
