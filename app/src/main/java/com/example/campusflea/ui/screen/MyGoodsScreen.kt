package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.data.model.GoodsInfo
import com.example.campusflea.data.repository.GoodsRepository
import com.example.campusflea.ui.navigation.Screen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyGoodsViewModel : ViewModel() {
    private val repo = GoodsRepository()
    private val _goods = MutableStateFlow<List<GoodsInfo>>(emptyList())
    val goods: StateFlow<List<GoodsInfo>> = _goods
    fun loadGoods(status: Int? = null) { viewModelScope.launch { repo.getMyGoods(status = status).onSuccess { _goods.value = it.list } } }
    fun updateStatus(id: Long, status: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.updateStatus(id, status).fold(
                onSuccess = { onDone(true, null) },
                onFailure = { onDone(false, it.message ?: "操作失败") }
            )
        }
    }
    fun delete(id: Long, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.delete(id).fold(
                onSuccess = { onDone(true, null) },
                onFailure = { onDone(false, it.message ?: "删除失败") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyGoodsScreen(navController: NavHostController, viewModel: MyGoodsViewModel = viewModel()) {
    val goods by viewModel.goods.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("全部" to null, "在售" to 1, "下架" to 2, "审核中" to 3, "已售" to 5)
    val ctx = LocalContext.current
    var deletingId by remember { mutableStateOf<Long?>(null) }
    val currentStatus = tabs[tabIndex].second
    LaunchedEffect(tabIndex) { viewModel.loadGoods(currentStatus) }
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("我的发布") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        TabRow(selectedTabIndex = tabIndex) { tabs.forEachIndexed { i, (t, _) -> Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(t) }) } }
        LazyColumn {
            items(goods, key = { it.id }) { g ->
                Card(Modifier.animateItem(fadeInSpec = tween(200)).fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NetworkImage(model = g.coverImage, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(g.title, fontWeight = FontWeight.Medium, maxLines = 2)
                                Spacer(Modifier.height(4.dp))
                                Text("¥${g.price}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("浏览${g.viewCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Spacer(Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(2.dp), color = when (g.condition) {
                                        1 -> MaterialTheme.colorScheme.primaryContainer
                                        2 -> MaterialTheme.colorScheme.tertiaryContainer
                                        3 -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }) {
                                        Text(g.conditionDesc.ifEmpty { "在售" }, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { navController.navigate(Screen.EditGoods.createRoute(g.id)) }) { Text("编辑") }
                            Spacer(Modifier.width(4.dp))
                            OutlinedButton(onClick = {
                                val target = if (currentStatus == 2) 1 else 2
                                viewModel.updateStatus(g.id, target) { success, msg ->
                                    if (success) {
                                        Toast.makeText(ctx, if (target == 1) "已上架" else "已下架", Toast.LENGTH_SHORT).show()
                                        viewModel.loadGoods(currentStatus)
                                    } else Toast.makeText(ctx, msg ?: "操作失败", Toast.LENGTH_SHORT).show()
                                }
                            }) { Text(if (currentStatus == 2) "上架" else "下架") }
                            Spacer(Modifier.width(4.dp))
                            Button(onClick = { deletingId = g.id }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("删除") }
                        }
                    }
                }
            }
        }
    }

    deletingId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("删除商品") },
            text = { Text("确定要删除该商品吗？删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    deletingId = null
                    viewModel.delete(id) { success, msg ->
                        if (success) {
                            Toast.makeText(ctx, "删除成功", Toast.LENGTH_SHORT).show()
                            viewModel.loadGoods(currentStatus)
                        } else Toast.makeText(ctx, msg ?: "删除失败", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("取消") } }
        )
    }
}
