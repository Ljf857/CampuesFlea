package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.viewmodel.MineViewModel
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavHostController, viewModel: MineViewModel = viewModel()) {
    var oldPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var oldVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val newTooShort = newPwd.isNotEmpty() && newPwd.length < 8
    val mismatch = confirmPwd.isNotEmpty() && newPwd != confirmPwd

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        GlassTopBar(title = { Text("修改密码") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        Column(Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 16.dp)) {
            OutlinedTextField(value = oldPwd, onValueChange = { oldPwd = it }, label = { Text("原密码") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (oldVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { oldVisible = !oldVisible }) { Icon(if (oldVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "切换密码可见") } })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = newPwd, onValueChange = { newPwd = it }, label = { Text("新密码(8-20位)") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { newVisible = !newVisible }) { Icon(if (newVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "切换密码可见") } }, isError = newTooShort, supportingText = { if (newTooShort) Text("密码长度不能少于8位", color = MaterialTheme.colorScheme.error) })
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = confirmPwd, onValueChange = { confirmPwd = it }, label = { Text("确认新密码") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { confirmVisible = !confirmVisible }) { Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "切换密码可见") } }, isError = mismatch, supportingText = { if (mismatch) Text("两次输入的密码不一致", color = MaterialTheme.colorScheme.error) })
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                when {
                    oldPwd.isEmpty() -> Toast.makeText(context, "请输入原密码", Toast.LENGTH_SHORT).show()
                    newPwd.length < 8 -> Toast.makeText(context, "新密码长度不能少于8位", Toast.LENGTH_SHORT).show()
                    newPwd != confirmPwd -> Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                    else -> viewModel.changePassword(oldPwd, newPwd,
                        onSuccess = { Toast.makeText(context, "密码修改成功", Toast.LENGTH_SHORT).show(); navController.popBackStack() },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                    )
                }
            }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("确认修改") }
        }
    }
}
