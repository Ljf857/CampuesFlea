package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.navigation.Screen

import com.example.campusflea.ui.viewmodel.MineViewModel
import com.example.campusflea.ui.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MyOrdersScreen(navController: NavHostController, viewModel: OrderViewModel = viewModel(), mineVm: MineViewModel = viewModel()) {
    val orders by viewModel.orders.collectAsState()
    val userInfo by mineVm.userInfo.collectAsState()
    val ctx = LocalContext.current
    var tabIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(tabIndex) { viewModel.loadOrders(if (tabIndex == 1) "buy" else if (tabIndex == 2) "sell" else null) }
    LaunchedEffect(Unit) { mineVm.loadUserInfo() }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("我的订单") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        TabRow(selectedTabIndex = tabIndex) { listOf("全部", "我买的", "我卖的").forEachIndexed { i, t -> Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(t) }) } }
        LazyColumn {
            items(orders, key = { it.id }) { order ->
                Card(Modifier.animateItem(fadeInSpec = tween(200)).fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { navController.navigate(Screen.OrderDetail.createRoute(order.id)) }, shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(order.orderNo, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline); Surface(shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text(order.statusDesc, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        NetworkImage(model = order.goodsCover, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(order.goodsTitle, fontWeight = FontWeight.Medium, maxLines = 2); Text("¥${order.goodsPrice}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
                    }
                    // 只有买家才展示 取消/确认收货/评价 等操作
                    val isBuyer = when {
                        tabIndex == 1 -> true
                        tabIndex == 2 -> false
                        else -> userInfo?.id?.let { order.buyerId == it } ?: false
                    }
                    if (isBuyer) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
                            when (order.status) {
                                1 -> OutlinedButton(onClick = { viewModel.cancel(order.id, "不想买了") { ok, msg -> Toast.makeText(ctx, if (ok) "已取消" else (msg ?: "取消失败"), Toast.LENGTH_SHORT).show(); viewModel.loadOrders(if (tabIndex == 1) "buy" else null) } }, Modifier.height(36.dp)) { Text("取消", fontSize = 13.sp) }
                                2 -> Button(onClick = { viewModel.receive(order.id) { ok, msg -> Toast.makeText(ctx, if (ok) "确认收货成功" else (msg ?: "操作失败"), Toast.LENGTH_SHORT).show(); viewModel.loadOrders(if (tabIndex == 1) "buy" else null) } }, Modifier.height(36.dp)) { Text("确认收货", fontSize = 13.sp) }
                                3 -> OutlinedButton(onClick = { navController.navigate(Screen.Evaluate.createRoute(order.id)) }, Modifier.height(36.dp)) { Text("评价", fontSize = 13.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
