package com.example.campusflea.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController) {
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("关于我们") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp)); Text("校园跳蚤", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("v1.0.0", color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp)); Text("让闲置物品流转起来，共建绿色校园", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        HorizontalDivider()
        listOf(Icons.Default.Description to "用户协议", Icons.Default.Lock to "隐私政策", Icons.Default.Star to "给我们评分", Icons.Default.SystemUpdate to "版本更新").forEach { (icon, title) ->
            ListItem(headlineContent = { Text(title) }, leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }, trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "更多") }, modifier = Modifier.clickable(role = Role.Button) { }); HorizontalDivider()
        }
        Spacer(Modifier.height(32.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text("联系我们", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text("客服电话：400-000-0000", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline); Text("官方邮箱：service@xxx.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
    }
}
