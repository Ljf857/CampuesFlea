package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.AuthState
import com.example.campusflea.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(navController: NavHostController, viewModel: AuthViewModel = viewModel()) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var agree by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    val countdown by viewModel.smsCountdown.collectAsState()
    val smsSending by viewModel.smsSending.collectAsState()
    val smsError by viewModel.smsError.collectAsState()
    val smsTip by viewModel.smsTip.collectAsState()
    val smsCodeFromVm by viewModel.smsCode.collectAsState()
    val state by viewModel.registerState.collectAsState()
    val context = LocalContext.current

    val passwordTooShort = password.isNotEmpty() && password.length < 8
    val passwordMismatch = confirm.isNotEmpty() && password != confirm

    LaunchedEffect(smsError) {
        smsError?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearSmsError() }
    }

    LaunchedEffect(smsTip) {
        smsTip?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearSmsTip() }
    }

    // 仅模拟通道会回传验证码明文用于自动填充；真实短信通道下为空，由用户从短信读取后手动输入
    LaunchedEffect(smsCodeFromVm) {
        if (smsCodeFromVm.isNotEmpty()) code = smsCodeFromVm
    }

    LaunchedEffect(state) {
        when (state) {
            is AuthState.Success -> { Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show(); navController.navigate(Screen.Login.route) }
            is AuthState.Error -> Toast.makeText(context, (state as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(24.dp)) {
        IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Default.ArrowBack, "返回") }
        Text("注册账号", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 24.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("手机号") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("验证码") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { TextButton(onClick = { viewModel.sendSms(phone, "register") }, enabled = !smsSending && countdown == 0) { Text(when { smsSending -> "发送中"; countdown > 0 -> "${countdown}s"; else -> "获取" }) } })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("设置密码(8-20位)") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "切换密码可见") } }, isError = passwordTooShort, supportingText = { if (passwordTooShort) Text("密码长度不能少于8位", color = MaterialTheme.colorScheme.error) })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("确认密码") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { confirmVisible = !confirmVisible }) { Icon(if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "切换密码可见") } }, isError = passwordMismatch, supportingText = { if (passwordMismatch) Text("两次输入的密码不一致", color = MaterialTheme.colorScheme.error) })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("昵称") }, leadingIcon = { Icon(Icons.Default.Person, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = agree, onCheckedChange = { agree = it })
            Text("我已阅读并同意《用户协议》和《隐私政策》", style = MaterialTheme.typography.bodySmall, modifier = Modifier.clickable { agree = !agree })
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            when {
                !agree -> Toast.makeText(context, "请先同意协议", Toast.LENGTH_SHORT).show()
                password.length < 8 -> Toast.makeText(context, "密码长度不能少于8位", Toast.LENGTH_SHORT).show()
                password != confirm -> Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                else -> viewModel.register(phone, password, code, nickname)
            }
        }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = state !is AuthState.Loading) { Text("注 册") }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("已有账号？", color = MaterialTheme.colorScheme.outline); TextButton(onClick = { navController.navigate(Screen.Login.route) }) { Text("去登录") } }
    }
}
