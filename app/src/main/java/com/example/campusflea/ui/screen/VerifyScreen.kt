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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(navController: NavHostController, viewModel: ProfileViewModel = viewModel()) {
    val verifyStatus by viewModel.verifyStatus.collectAsState()
    var realName by remember { mutableStateOf("") }
    var studentNo by remember { mutableStateOf("") }
    var frontUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var backUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showFrontPicker by remember { mutableStateOf(false) }
    var showBackPicker by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var currentImageSlot by remember { mutableStateOf<Int?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    fun createTempUri(): android.net.Uri {
        val file = File.createTempFile("verify_", ".jpg", ctx.cacheDir)
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let {
            when (currentImageSlot) { 0 -> frontUri = it; 1 -> backUri = it }
        } }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) cameraImageUri?.let {
            when (currentImageSlot) { 0 -> frontUri = it; 1 -> backUri = it }
        } }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val uri = createTempUri()
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(ctx, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }
    )

    fun launchCamera(slot: Int) {
        currentImageSlot = slot
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = createTempUri()
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery(slot: Int) {
        currentImageSlot = slot
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(Unit) { viewModel.loadVerifyStatus() }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = { Text("实名认证") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            verifyStatus?.let { vs ->
                when (vs.verifyStatus) {
                    1 -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("已通过实名认证", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("您已可以发布商品和下单交易", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        }
                    }
                    2 -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(Modifier.size(48.dp), strokeWidth = 4.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("审核中，请耐心等待", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("我们正在核实您的信息，通常需要1-2个工作日", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        }
                    }
                    else -> {
                        Text("完成实名认证后可发布商品、下单", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                        Spacer(Modifier.height(16.dp))
                        if (vs.verifyStatus == 3 && vs.rejectReason != null) {
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(8.dp))
                                    Text("驳回原因：${vs.rejectReason}", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        OutlinedTextField(value = realName, onValueChange = { realName = it }, label = { Text("真实姓名") }, placeholder = { Text("请输入真实姓名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = studentNo, onValueChange = { studentNo = it }, label = { Text("学号") }, placeholder = { Text("请输入学号") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(16.dp))
                        Text("学生证正面", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        ImageUploadBox(
                            uri = frontUri,
                            onClick = { showFrontPicker = true },
                            onClear = { frontUri = null }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("学生证反面", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        ImageUploadBox(
                            uri = backUri,
                            onClick = { showBackPicker = true },
                            onClear = { backUri = null }
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("信息仅用于身份验证，不会公开", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (realName.isBlank()) { Toast.makeText(ctx, "请输入真实姓名", Toast.LENGTH_SHORT).show(); return@Button }
                                if (studentNo.isBlank()) { Toast.makeText(ctx, "请输入学号", Toast.LENGTH_SHORT).show(); return@Button }
                                val fUri = frontUri ?: return@Button.also { Toast.makeText(ctx, "请上传学生证正面", Toast.LENGTH_SHORT).show() }
                                val bUri = backUri ?: return@Button.also { Toast.makeText(ctx, "请上传学生证反面", Toast.LENGTH_SHORT).show() }
                                scope.launch {
                                    isSubmitting = true
                                    viewModel.submitVerify(realName, studentNo, fUri, bUri, ctx,
                                        onDone = { success, msg ->
                                            isSubmitting = false
                                            if (success) {
                                                Toast.makeText(ctx, "认证已提交", Toast.LENGTH_SHORT).show()
                                                viewModel.loadVerifyStatus()
                                            } else {
                                                Toast.makeText(ctx, msg ?: "提交失败", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            },
                            Modifier.fillMaxWidth().height(48.dp),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            else Text("提交认证")
                        }
                    }
                }
            }
        }
    }

    if (showFrontPicker) {
        ImageSourceDialog(
            onDismiss = { showFrontPicker = false },
            onCamera = { showFrontPicker = false; launchCamera(0) },
            onGallery = { showFrontPicker = false; launchGallery(0) }
        )
    }
    if (showBackPicker) {
        ImageSourceDialog(
            onDismiss = { showBackPicker = false },
            onCamera = { showBackPicker = false; launchCamera(1) },
            onGallery = { showBackPicker = false; launchGallery(1) }
        )
    }
}

@Composable
private fun ImageUploadBox(uri: android.net.Uri?, onClick: () -> Unit, onClear: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            NetworkImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error).clickable { onClear() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, "删除", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CameraAlt, "上传", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(4.dp))
                Text("点击上传", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ImageSourceDialog(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加图片") },
        text = {
            Column {
                TextButton(onClick = onCamera, modifier = Modifier.fillMaxWidth()) { Text("拍照") }
                TextButton(onClick = onGallery, modifier = Modifier.fillMaxWidth()) { Text("从相册选择") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            }
        },
        confirmButton = {}
    )
}
