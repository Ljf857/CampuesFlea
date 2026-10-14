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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.campusflea.data.repository.GoodsRepository
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.viewmodel.GoodsDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(navController: NavHostController, goodsId: Long, viewModel: GoodsDetailViewModel = viewModel()) {
    var selectedReason by remember { mutableIntStateOf(0) }
    var description by remember { mutableStateOf("") }
    var images by remember { mutableStateOf(listOf<String>()) }
    var uploading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val reasons = listOf("违规商品", "虚假信息", "欺诈行为", "侵权盗版", "其他")
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
        viewModel.report(goodsId, reasons[selectedReason], description, images) { success, msg ->
            submitting = false
            if (success) {
                Toast.makeText(ctx, "举报已提交", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } else Toast.makeText(ctx, msg ?: "举报提交失败，请重试", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("举报商品") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = { TextButton(onClick = { submit() }, enabled = !submitting) { Text("提交") } })
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("请选择举报原因", fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp))
            reasons.forEachIndexed { i, reason -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedReason = i }, verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = selectedReason == i, onClick = { selectedReason = i }); Text(reason, modifier = Modifier.padding(start = 4.dp)) } }
            Spacer(Modifier.height(16.dp))
            Text("补充说明", fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth().height(120.dp), label = { Text("详细描述举报原因...") })
            Spacer(Modifier.height(16.dp))
            Text("上传证据图片（最多3张）", fontWeight = FontWeight.Medium); Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images.size) { i ->
                    Box(Modifier.size(64.dp)) {
                        NetworkImage(model = images[i], contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Icon(Icons.Default.Close, "关闭", tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error).clickable(role = Role.Button) { images = images.toMutableList().also { it.removeAt(i) } })
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
