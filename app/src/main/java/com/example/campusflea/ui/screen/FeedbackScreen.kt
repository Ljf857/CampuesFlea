package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.campusflea.data.remote.RetrofitClient
import kotlinx.coroutines.launch
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(navController: NavHostController) {
    val types = listOf("功能建议", "体验反馈", "商品投诉", "账号问题", "其他")
    var type by remember { mutableStateOf(types[0]) }
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    fun submit() {
        if (submitting) return
        if (content.isBlank()) {
            Toast.makeText(ctx, "请填写反馈内容", Toast.LENGTH_SHORT).show()
            return
        }
        // 后端类型：1-功能建议 2-问题反馈 3-其他
        val typeInt = when (type) {
            "功能建议" -> 1
            "其他" -> 3
            else -> 2
        }
        submitting = true
        scope.launch {
            val body = mapOf(
                "type" to typeInt,
                "content" to content.trim(),
                "contact" to contact.trim(),
                "images" to emptyList<String>()
            )
            try {
                val r = RetrofitClient.apiService.submitFeedback(body)
                submitting = false
                if (r.isSuccess()) {
                    Toast.makeText(ctx, "感谢您的反馈！", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(ctx, r.message ?: "提交失败，请重试", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                submitting = false
                Toast.makeText(ctx, e.message ?: "提交失败，请检查网络", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(
            title = { Text("意见反馈") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = { TextButton(onClick = { submit() }, enabled = !submitting) { Text("提交") } }
        )
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("反馈类型", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    types.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t) },
                            onClick = { type = t; expanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("反馈内容", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                label = { Text("请详细描述您的建议或遇到的问题...") }
            )
            Spacer(Modifier.height(16.dp))
            Text("联系方式（选填）", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("手机号或QQ，方便我们联系您") }
            )
        }
    }
}
