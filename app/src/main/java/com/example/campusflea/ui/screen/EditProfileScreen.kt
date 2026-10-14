package com.example.campusflea.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.data.model.ProfileUpdateRequest
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.viewmodel.ProfileViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavHostController, viewModel: ProfileViewModel = viewModel()) {
    val user by viewModel.userInfo.collectAsState()
    var nickname by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var gender by remember { mutableIntStateOf(0) }
    var avatarUrl by remember { mutableStateOf("") }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var initialized by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    fun createTempImageUri(): android.net.Uri {
        val file = File.createTempFile("avatar_", ".jpg", ctx.cacheDir)
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    fun uploadAvatar(uri: android.net.Uri) {
        viewModel.uploadAvatar(ctx, uri,
            onSuccess = { url ->
                avatarUrl = url
                viewModel.updateProfile(ProfileUpdateRequest(nickname, avatar = url, gender = gender, signature = signature)) {
                    if (it) viewModel.loadProfile()
                }
            },
            onError = { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show() }
        )
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { uploadAvatar(it) } }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success -> if (success) cameraImageUri?.let { uploadAvatar(it) } }
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

    LaunchedEffect(Unit) { viewModel.loadProfile() }
    // 只在首次加载时预填一次，避免后续 user 变化（如保存/上传头像后刷新）覆盖用户正在编辑的内容
    LaunchedEffect(user) {
        if (!initialized) {
            user?.let {
                nickname = it.nickname; signature = it.signature; gender = it.gender; avatarUrl = it.avatar
                initialized = true
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = { Text("编辑资料") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = {
                TextButton(onClick = {
                    viewModel.updateProfile(ProfileUpdateRequest(nickname, avatar = avatarUrl, gender = gender, signature = signature)) {
                        if (it) { Toast.makeText(ctx, "保存成功", Toast.LENGTH_SHORT).show(); navController.popBackStack() }
                    }
                }) { Text("保存") }
            }
        )
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                NetworkImage(
                    model = avatarUrl,
                    contentDescription = "头像",
                    modifier = Modifier.size(80.dp).clip(CircleShape).clickable { showAvatarPicker = true }
                )
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { showAvatarPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, "修改头像", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("昵称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(12.dp))
            Text("性别", modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(0 to "保密", 1 to "男", 2 to "女").forEach { (v, l) ->
                    Row(Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = gender == v, onClick = { gender = v })
                        Text(l)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = signature, onValueChange = { signature = it }, label = { Text("个性签名") }, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showAvatarPicker) {
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            title = { Text("修改头像") },
            text = {
                Column {
                    TextButton(onClick = { showAvatarPicker = false; launchCamera() }, modifier = Modifier.fillMaxWidth()) { Text("拍照") }
                    TextButton(onClick = { showAvatarPicker = false; launchGallery() }, modifier = Modifier.fillMaxWidth()) { Text("从相册选择") }
                    TextButton(onClick = { showAvatarPicker = false }, modifier = Modifier.fillMaxWidth()) { Text("取消") }
                }
            },
            confirmButton = {}
        )
    }
}
