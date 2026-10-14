package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusflea.data.repository.GoodsRepository
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.viewmodel.OrderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluateScreen(navController: NavHostController, orderId: Long, viewModel: OrderViewModel = viewModel()) {
    var rating by remember { mutableIntStateOf(5) }
    var content by remember { mutableStateOf("") }
    var images by remember { mutableStateOf(listOf<String>()) }
    var uploading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { GoodsRepository() }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                uploading = true
                repo.uploadGoodsImage(ctx, uri)
                    .onSuccess { url -> if (images.size < 3) images = images + url else Toast.makeText(ctx, "最多上传3张图片", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(ctx, it.message ?: "上传失败", Toast.LENGTH_SHORT).show() }
                uploading = false
            }
        }
    )

    fun submit() {
        if (submitting) return
        submitting = true
        viewModel.evaluate(orderId, rating, content, images) { success, msg ->
            submitting = false
            if (success) {
                Toast.makeText(ctx, "评价成功", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } else Toast.makeText(ctx, msg ?: "评价失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("评价订单") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = { TextButton(onClick = { submit() }, enabled = !submitting) { Text("提交") } })
        Column(Modifier.padding(16.dp)) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.ShoppingBag, null, tint = MaterialTheme.colorScheme.outline) }
                        Spacer(Modifier.width(12.dp))
                        Column { Text("订单 #$orderId", fontWeight = FontWeight.Medium); Text("请对本次交易进行评价", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("商品评分", fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp))
            Row { (1..5).forEach { i -> IconButton(onClick = { rating = i }) {                     Icon(if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder, "评分", tint = if (i <= rating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline) } } }
            Spacer(Modifier.height(16.dp))
            Text("评价内容", fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = content, onValueChange = { content = it }, modifier = Modifier.fillMaxWidth().height(120.dp), label = { Text("说说您的交易感受...") })
            Spacer(Modifier.height(16.dp))
            Text("上传图片（最多3张）", fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images.size) { i ->
                    Box(Modifier.size(64.dp)) {
                        NetworkImage(model = images[i], contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.TopEnd).size(48.dp), contentAlignment = Alignment.TopEnd) {
                            Icon(Icons.Default.Close, "关闭", tint = Color.White, modifier = Modifier.size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error).clickable(role = Role.Button) { images = images.toMutableList().also { it.removeAt(i) } })
                        }
                    }
                }
                if (uploading) item {
                    Box(Modifier.size(64.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
                }
                if (images.size < 3 && !uploading) item {
                    Box(Modifier.size(64.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).clickable(role = Role.Button) { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Add, "添加", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)); Text("添加", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline) }
                    }
                }
            }
        }
    }
}
