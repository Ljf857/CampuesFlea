package com.example.campusflea.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.data.model.CategoryInfo
import com.example.campusflea.data.model.PublishRequest
import com.example.campusflea.data.repository.GoodsRepository
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishGoodsScreen(navController: NavHostController, goodsId: Long? = null, catVm: CategoryViewModel = viewModel()) {
    val isEdit = goodsId != null
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var originalPrice by remember { mutableStateOf("") }
    var condition by remember { mutableFloatStateOf(9f) }
    var deliveryType by remember { mutableIntStateOf(3) }
    var location by remember { mutableStateOf("") }
    var images by remember { mutableStateOf(listOf<String>()) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<CategoryInfo?>(null) }
    val categories by catVm.categories.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImageSourcePicker by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var isLoadingEdit by remember { mutableStateOf(isEdit) }
    val repo = remember { GoodsRepository() }

    // 编辑模式：加载商品详情并预填
    var prefilled by remember { mutableStateOf(false) }
    LaunchedEffect(goodsId) {
        val id = goodsId ?: return@LaunchedEffect
        repo.getDetail(id).fold(
            onSuccess = { d ->
                title = d.title; description = d.description
                price = if (d.price > 0) d.price.toString() else ""
                originalPrice = if (d.originalPrice > 0) d.originalPrice.toString() else ""
                condition = d.condition.toFloat().coerceIn(1f, 10f)
                deliveryType = d.deliveryType
                location = d.location
                images = d.images
                selectedCategory = categories.flatMap { it.children }.firstOrNull { it.id == d.categoryId }
                    ?: categories.firstOrNull { it.id == d.categoryId }
                prefilled = true
                isLoadingEdit = false
            },
            onFailure = {
                isLoadingEdit = false
                Toast.makeText(ctx, "加载商品信息失败", Toast.LENGTH_SHORT).show()
            }
        )
    }

    fun createTempImageUri(): android.net.Uri {
        val file = File.createTempFile("goods_", ".jpg", ctx.cacheDir)
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    fun startUpload(uri: android.net.Uri) {
        scope.launch {
            isUploading = true
            repo.uploadGoodsImage(ctx, uri).fold(
                onSuccess = { url -> images = images + url },
                onFailure = { Toast.makeText(ctx, it.message ?: "上传失败", Toast.LENGTH_SHORT).show() }
            )
            isUploading = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { startUpload(it) } }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) cameraImageUri?.let { startUpload(it) } }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val uri = createTempImageUri()
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(ctx, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = createTempImageUri()
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun publishGoods() {
        val catId = selectedCategory?.id
        val p = price.toDoubleOrNull()
        when {
            title.isBlank() -> Toast.makeText(ctx, "请填写商品标题", Toast.LENGTH_SHORT).show()
            catId == null -> Toast.makeText(ctx, "请选择分类", Toast.LENGTH_SHORT).show()
            p == null || p <= 0 -> Toast.makeText(ctx, "请填写正确的价格（大于0）", Toast.LENGTH_SHORT).show()
            images.isEmpty() -> Toast.makeText(ctx, "请至少添加一张图片", Toast.LENGTH_SHORT).show()
            else -> scope.launch {
                val request = PublishRequest(
                    title = title,
                    price = p,
                    originalPrice = originalPrice.toDoubleOrNull()?.takeIf { it > 0 },
                    description = description,
                    images = images,
                    condition = condition.toInt(),
                    categoryId = catId,
                    location = location,
                    deliveryType = deliveryType
                )
                val job = if (isEdit && goodsId != null) repo.update(goodsId, request) else repo.publish(request)
                job.fold(
                    onSuccess = {
                        Toast.makeText(ctx, if (isEdit) "保存成功" else "发布成功", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    onFailure = { Toast.makeText(ctx, it.message ?: if (isEdit) "保存失败" else "发布失败", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = { Text(if (isEdit) "编辑商品" else "发布商品") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = { TextButton(onClick = { publishGoods() }, enabled = !isUploading && !isLoadingEdit) { Text(if (isEdit) "保存" else "发布") } }
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("商品图片（最多9张）", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(images.size) { i ->
                    Box(Modifier.size(80.dp)) {
                        NetworkImage(model = images[i], contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.TopEnd).size(48.dp), contentAlignment = Alignment.TopEnd) {
                            Icon(Icons.Default.Close, "关闭", tint = Color.White, modifier = Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error).clickable(role = Role.Button) { images = images.toMutableList().also { it.removeAt(i) } })
                        }
                    }
                }
                if (isUploading) item {
                    Box(Modifier.size(80.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                if (images.size < 9 && !isUploading) item {
                    Box(Modifier.size(80.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).clickable(role = Role.Button) { showImageSourcePicker = true }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, "添加", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                            Text("添加", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("商品标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("5-50字描述商品特点") })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("商品描述") }, modifier = Modifier.fillMaxWidth().height(120.dp), placeholder = { Text("描述商品成色、使用情况等") })
            Spacer(Modifier.height(12.dp))
            OutlinedCard(Modifier.fillMaxWidth().clickable(role = Role.Button) { showCategoryPicker = true }) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("分类", style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedCategory?.name ?: "请选择分类", color = if (selectedCategory != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                        Icon(Icons.Default.KeyboardArrowRight, "更多", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("成色：${condition.toInt()}成新", fontSize = 14.sp)
                Slider(value = condition, onValueChange = { condition = it }, valueRange = 1f..10f, steps = 8, modifier = Modifier.weight(1f).padding(start = 16.dp))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("价格") }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("¥") })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = originalPrice, onValueChange = { originalPrice = it }, label = { Text("原价（选填）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, prefix = { Text("¥") })
            Spacer(Modifier.height(12.dp))
            Text("交易方式", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row { listOf(1 to "自取", 2 to "邮寄", 3 to "自取/邮寄").forEach { (v, l) -> Row(Modifier.padding(end = 16.dp)) { RadioButton(selected = deliveryType == v, onClick = { deliveryType = v }); Text(l) } } }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("交易位置") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }

    val allSubCategories = remember(categories) {
        categories.flatMap { pc -> pc.children.map { it.copy(parentId = pc.id, name = "${pc.name} / ${it.name}") } }
    }

    if (showCategoryPicker) {
        AlertDialog(onDismissRequest = { showCategoryPicker = false }, title = { Text("选择分类") }, text = {
            LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                items(allSubCategories) { cat ->
                    Card(Modifier.padding(4.dp).fillMaxWidth().clickable(role = Role.Button) { selectedCategory = cat; showCategoryPicker = false }) {
                        Text(cat.name, modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center, fontSize = 13.sp)
                    }
                }
            }
        }, confirmButton = {})
    }

    if (showImageSourcePicker) {
        AlertDialog(
            onDismissRequest = { showImageSourcePicker = false },
            title = { Text("添加图片") },
            text = {
                Column {
                    TextButton(onClick = { showImageSourcePicker = false; launchCamera() }, modifier = Modifier.fillMaxWidth()) { Text("拍照") }
                    TextButton(onClick = { showImageSourcePicker = false; launchGallery() }, modifier = Modifier.fillMaxWidth()) { Text("从相册选择") }
                    TextButton(onClick = { showImageSourcePicker = false }, modifier = Modifier.fillMaxWidth()) { Text("取消") }
                }
            },
            confirmButton = {}
        )
    }
}
