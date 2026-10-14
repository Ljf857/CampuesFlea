package com.example.campusflea.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.campusflea.data.remote.RetrofitClient

@Composable
fun NetworkImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val fixedModel = if (model is String) {
        val t = model.trimStart('/')
        when {
            t.isEmpty() -> t
            // 绝对地址：把回环地址（localhost/127.0.0.1）替换为当前后端主机，
            // MinIO 等资源服务一般与后端部署在同一台机器上
            t.startsWith("http://") || t.startsWith("https://") ->
                t.replace("://localhost:", "://${RetrofitClient.baseHost}:")
                    .replace("://127.0.0.1:", "://${RetrofitClient.baseHost}:")
            // 相对路径（如 /campus-flea/goods/xx.png，已含 bucket）：直接拼 MinIO 基地址
            else -> "${RetrofitClient.minioBaseUrl}/$t"
        }
    } else model
    SubcomposeAsyncImage(
        model = fixedModel,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize().background(placeholderColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        },
        error = {
            Box(
                modifier = Modifier.fillMaxSize().background(placeholderColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        },
        success = { SubcomposeAsyncImageContent() }
    )
}
