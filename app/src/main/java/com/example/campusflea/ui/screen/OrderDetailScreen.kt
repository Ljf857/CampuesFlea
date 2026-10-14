package com.example.campusflea.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen

import com.example.campusflea.ui.viewmodel.OrderViewModel
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(navController: NavHostController, orderId: Long, viewModel: OrderViewModel = viewModel()) {
    val detail by viewModel.orderDetail.collectAsState()
    LaunchedEffect(orderId) { viewModel.loadDetail(orderId) }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("订单详情") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        detail?.let { d ->
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) { Text(d.statusDesc, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp)) }
                Column(Modifier.padding(16.dp)) { d.timeline.forEachIndexed { i, item -> Row(Modifier.padding(bottom = 16.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)); if (i < d.timeline.size - 1) Box(Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant)) }; Spacer(Modifier.width(12.dp)); Column { Text(item.desc, fontWeight = FontWeight.Medium, fontSize = 14.sp); Text(item.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) } } } }
                HorizontalDivider()
                d.goods?.let { g -> Row(Modifier.padding(16.dp)) { Text(g.title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)); Text("¥${g.price}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } }
                HorizontalDivider()
                Column(Modifier.padding(16.dp)) { Text("交易方式：${d.deliveryTypeDesc}"); if (d.remark.isNotEmpty()) Text("买家备注：${d.remark}") }
                HorizontalDivider()
                d.buyer?.let { b -> Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null); Spacer(Modifier.width(8.dp)); Column { Text("买家：${b.nickname}"); Text(b.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) } } }
                d.seller?.let { s -> Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null); Spacer(Modifier.width(8.dp)); Column { Text("卖家：${s.nickname}"); Text(s.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) } } }
                Column(Modifier.padding(16.dp)) { Text("订单编号：${d.orderNo}", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline); Text("下单时间：${d.createTime}", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
            }
            when (d.status) { 2 -> Button(onClick = { viewModel.receive(d.id) }, Modifier.fillMaxWidth().padding(16.dp).height(48.dp)) { Text("确认收货") }; 3 -> OutlinedButton(onClick = { navController.navigate(Screen.Evaluate.createRoute(d.id)) }, Modifier.fillMaxWidth().padding(16.dp).height(48.dp)) { Text("评价订单") }; else -> {} }
        }
    }
}
