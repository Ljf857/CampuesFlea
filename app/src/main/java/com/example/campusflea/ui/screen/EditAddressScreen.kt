package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.data.model.AddressRequest
import com.example.campusflea.ui.viewmodel.ProfileViewModel
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAddressScreen(navController: NavHostController, addressId: Long, viewModel: ProfileViewModel = viewModel()) {
    var receiver by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text(if (addressId > 0) "编辑地址" else "新增地址") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = { TextButton(onClick = { val req = AddressRequest(receiver, phone, province, city, district, detail, isDefault); if (addressId > 0) viewModel.updateAddress(addressId, req) { if (it) { Toast.makeText(ctx, "保存成功", Toast.LENGTH_SHORT).show(); navController.popBackStack() } } else viewModel.addAddress(req) { if (it) { Toast.makeText(ctx, "添加成功", Toast.LENGTH_SHORT).show(); navController.popBackStack() } } }) { Text("保存") } })
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(value = receiver, onValueChange = { receiver = it }, label = { Text("收货人") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("手机号") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = province, onValueChange = { province = it }, label = { Text("省份") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("城市") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("区县") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = detail, onValueChange = { detail = it }, label = { Text("详细地址") }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = isDefault, onCheckedChange = { isDefault = it }); Text("设为默认地址") }
        }
    }
}
