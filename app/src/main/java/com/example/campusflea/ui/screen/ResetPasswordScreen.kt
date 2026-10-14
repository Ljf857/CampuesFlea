package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.viewmodel.AuthState
import com.example.campusflea.ui.viewmodel.AuthViewModel

@Composable
fun ResetPasswordScreen(navController: NavHostController, viewModel: AuthViewModel = viewModel()) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val countdown by viewModel.smsCountdown.collectAsState()
    val smsSending by viewModel.smsSending.collectAsState()
    val smsError by viewModel.smsError.collectAsState()
    val smsTip by viewModel.smsTip.collectAsState()
    val smsCodeFromVm by viewModel.smsCode.collectAsState()
    val state by viewModel.resetPasswordState.collectAsState()
    val context = LocalContext.current

    val passwordMismatch = password != confirm && confirm.isNotEmpty()
    val passwordTooShort = password.isNotEmpty() && password.length < 8

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
            is AuthState.Success -> { Toast.makeText(context, "密码修改成功", Toast.LENGTH_SHORT).show(); navController.popBackStack() }
            is AuthState.Error -> Toast.makeText(context, (state as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(24.dp)) {
        IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Default.ArrowBack, "返回") }
        Text("找回密码", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 24.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("注册手机号") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("验证码") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { TextButton(onClick = { viewModel.sendSms(phone, "reset") }, enabled = !smsSending && countdown == 0) { Text(when { smsSending -> "发送中"; countdown > 0 -> "${countdown}s"; else -> "获取" }) } })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("新密码") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), isError = passwordTooShort, supportingText = { if (passwordTooShort) Text("密码长度不能少于8位", color = MaterialTheme.colorScheme.error) })
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = confirm, onValueChange = { confirm = it }, label = { Text("确认新密码") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), isError = passwordMismatch, supportingText = { if (passwordMismatch) Text("两次输入的密码不一致", color = MaterialTheme.colorScheme.error) })
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            when {
                password.length < 8 -> Toast.makeText(context, "密码长度不能少于8位", Toast.LENGTH_SHORT).show()
                password != confirm -> Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                else -> viewModel.resetPassword(phone, code, password)
            }
        }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = state !is AuthState.Loading) { Text("确认修改") }
    }
}
