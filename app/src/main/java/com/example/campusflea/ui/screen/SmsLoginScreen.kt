package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.AuthState
import com.example.campusflea.ui.viewmodel.AuthViewModel

@Composable
fun SmsLoginScreen(navController: NavHostController, viewModel: AuthViewModel = viewModel()) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val countdown by viewModel.smsCountdown.collectAsState()
    val smsSending by viewModel.smsSending.collectAsState()
    val smsError by viewModel.smsError.collectAsState()
    val smsTip by viewModel.smsTip.collectAsState()
    val smsCodeFromVm by viewModel.smsCode.collectAsState()
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current

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

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthState.Success -> navController.navigate(Screen.Main.route) { popUpTo(Screen.Login.route) { inclusive = true } }
            is AuthState.Error -> Toast.makeText(context, (loginState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(24.dp)) {
        IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.Default.ArrowBack, "返回") }
        Spacer(Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("验证码登录", style = MaterialTheme.typography.headlineMedium)
            Text("输入手机号获取验证码", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("手机号") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("验证码") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { TextButton(onClick = { viewModel.sendSms(phone, "login") }, enabled = !smsSending && countdown == 0) { Text(when { smsSending -> "发送中"; countdown > 0 -> "${countdown}s"; else -> "获取" }) } })
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.smsLogin(phone, code) }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = loginState !is AuthState.Loading) {
            if (loginState is AuthState.Loading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("登 录")
        }
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("还没有账号？", color = MaterialTheme.colorScheme.outline); TextButton(onClick = { navController.navigate(Screen.Register.route) }) { Text("立即注册") } }
    }
}
