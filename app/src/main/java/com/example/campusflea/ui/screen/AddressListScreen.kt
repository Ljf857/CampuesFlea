package com.example.campusflea.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.ProfileViewModel
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddressListScreen(navController: NavHostController, viewModel: ProfileViewModel = viewModel()) {
    val addresses by viewModel.addresses.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAddresses() }
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("收货地址") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = { TextButton(onClick = { navController.navigate(Screen.EditAddress.createRoute()) }) { Text("新增") } })
        LazyColumn(Modifier.padding(horizontal = 16.dp)) {
            items(addresses, key = { it.id }) { addr ->
                Card(Modifier.animateItem(fadeInSpec = tween(200)).fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row { Text("${addr.receiver}  ${addr.phone}", fontWeight = FontWeight.Medium); if (addr.isDefault) Surface(Modifier.padding(start = 8.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) { Text("默认", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) } }
                        Text(addr.getFullAddress(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { navController.navigate(Screen.EditAddress.createRoute(addr.id)) }) { Text("编辑") }
                            TextButton(onClick = { viewModel.deleteAddress(addr.id) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}
