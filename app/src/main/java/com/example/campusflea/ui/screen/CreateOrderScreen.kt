package com.example.campusflea.ui.screen

import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.data.model.AddressInfo
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.GoodsDetailViewModel
import com.example.campusflea.ui.viewmodel.OrderViewModel
import com.example.campusflea.ui.viewmodel.ProfileViewModel
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(navController: NavHostController, goodsId: Long, orderVm: OrderViewModel = viewModel(), goodsVm: GoodsDetailViewModel = viewModel(), profileVm: ProfileViewModel = viewModel()) {
    val detail by goodsVm.detail.collectAsState()
    val addresses by profileVm.addresses.collectAsState()
    var deliveryType by remember { mutableIntStateOf(1) }
    var remark by remember { mutableStateOf("") }
    var selectedAddressId by remember { mutableStateOf<Long?>(null) }
    var showAddressPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(goodsId) { goodsVm.loadDetail(goodsId) }
    LaunchedEffect(Unit) { profileVm.loadAddresses() }

    // 地址加载后自动选中默认地址（优先 isDefault，否则第一条）
    LaunchedEffect(addresses) {
        if (addresses.isNotEmpty() && selectedAddressId == null) {
            selectedAddressId = addresses.firstOrNull { it.isDefault }?.id ?: addresses.first().id
        }
    }

    val selectedAddress: AddressInfo? = addresses.firstOrNull { it.id == selectedAddressId }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("确认订单") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        detail?.let { d ->
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Row(Modifier.padding(16.dp).clickable { if (addresses.isEmpty()) navController.navigate(Screen.AddressList.route) else showAddressPicker = true }) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            if (selectedAddress != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${selectedAddress.receiver}  ${selectedAddress.phone}", fontWeight = FontWeight.Medium)
                                    if (selectedAddress.isDefault) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) { Text("默认", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(selectedAddress.getFullAddress(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (addresses.isEmpty()) {
                                Text("暂无收货地址，点击添加", color = MaterialTheme.colorScheme.outline)
                            } else {
                                Text("请选择收货地址", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Icon(Icons.Default.KeyboardArrowRight, "选择地址", tint = MaterialTheme.colorScheme.outline)
                    }
                }
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) { Row(Modifier.padding(12.dp)) { Column(Modifier.weight(1f)) { Text(d.title, fontWeight = FontWeight.Medium); Text("¥${d.price}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } } }
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("交易方式", fontWeight = FontWeight.Medium)
                        Row(Modifier.padding(vertical = 8.dp)) { listOf(1 to "自取", 2 to "邮寄", 3 to "自取/邮寄").forEach { (v, l) -> Row(Modifier.padding(end = 16.dp)) { RadioButton(selected = deliveryType == v, onClick = { deliveryType = v }); Text(l, modifier = Modifier.padding(start = 4.dp)) } } }
                        OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("给卖家留言（选填）") }, modifier = Modifier.fillMaxWidth())
                    }
                }
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("商品金额"); Text("¥${d.price}") }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("运费"); Text("¥0.00") }; HorizontalDivider(Modifier.padding(vertical = 8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("实付款", fontWeight = FontWeight.Bold); Text("¥${d.price}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) } } }
            }
            Button(onClick = {
                if (deliveryType == 2 && selectedAddress == null) {
                    Toast.makeText(context, if (addresses.isEmpty()) "请先添加收货地址" else "请选择收货地址", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                orderVm.createOrder(d.id, deliveryType, addressId = selectedAddress?.id, remark = remark.ifBlank { null }) { id -> if (id != null) { Toast.makeText(context, "下单成功", Toast.LENGTH_SHORT).show(); navController.navigate(Screen.OrderDetail.createRoute(id)) { popUpTo(Screen.CreateOrder.route) { inclusive = true } } } else Toast.makeText(context, "下单失败", Toast.LENGTH_SHORT).show() }
            }, Modifier.fillMaxWidth().padding(16.dp).height(48.dp)) { Text("提交订单") }
        }
    }

    if (showAddressPicker) {
        AlertDialog(
            onDismissRequest = { showAddressPicker = false },
            title = { Text("选择收货地址") },
            text = {
                Column {
                    addresses.forEach { addr ->
                        Row(Modifier.fillMaxWidth().clickable { selectedAddressId = addr.id; showAddressPicker = false }.padding(vertical = 8.dp)) {
                            RadioButton(selected = selectedAddressId == addr.id, onClick = { selectedAddressId = addr.id; showAddressPicker = false })
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${addr.receiver}  ${addr.phone}", fontWeight = FontWeight.Medium)
                                    if (addr.isDefault) { Spacer(Modifier.width(6.dp)); Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) { Text("默认", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }
                                }
                                Text(addr.getFullAddress(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddressPicker = false }) { Text("取消") } }
        )
    }
}
