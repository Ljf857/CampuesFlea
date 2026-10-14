package com.example.campusflea.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(navController: NavHostController, viewModel: HistoryViewModel = viewModel()) {
    val history by viewModel.history.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = { Text("浏览历史") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = {
                if (history.isNotEmpty()) {
                    TextButton(onClick = { showClearDialog = true }) { Text("清空") }
                }
            }
        )
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无浏览记录", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(history, key = { it.id }) { g ->
                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    Row(
                        Modifier.animateItem(fadeInSpec = tween(200)).fillMaxWidth().padding(vertical = 8.dp).clickable { navController.navigate(Screen.GoodsDetail.createRoute(g.id)) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NetworkImage(model = g.coverImage, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(g.title, fontWeight = FontWeight.Medium, maxLines = 2, fontSize = 14.sp)
                            Text("¥${g.price}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    HorizontalDivider()
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            confirmButton = { TextButton(onClick = { viewModel.deleteHistory(g.id); showDeleteConfirm = false }) { Text("删除") } },
                            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } },
                            title = { Text("提示") },
                            text = { Text("确定删除该浏览记录？") }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            confirmButton = { TextButton(onClick = { viewModel.clearHistory(); showClearDialog = false }) { Text("清空") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } },
            title = { Text("提示") },
            text = { Text("确定清空所有浏览记录？") }
        )
    }
}
