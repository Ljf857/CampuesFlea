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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.AuthState
import com.example.campusflea.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(navController: NavHostController, viewModel: AuthViewModel = viewModel()) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPwd by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(loginState) {
        when (loginState) {
            is AuthState.Success -> navController.navigate(Screen.Main.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            is AuthState.Error -> Toast.makeText(context, (loginState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(24.dp)) {
        IconButton(onClick = { if (!navController.popBackStack()) navController.navigate(Screen.Main.route) { popUpTo(0) { inclusive = true } } }) { Icon(Icons.Default.ArrowBack, "返回") }
        Spacer(Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("欢迎回来", style = MaterialTheme.typography.headlineMedium)
            Text("登录校园跳蚤账号", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("手机号") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = { IconButton(onClick = { showPwd = !showPwd }) { Icon(if (showPwd) Icons.Default.Visibility else Icons.Default.VisibilityOff, "切换密码可见") } })
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { navController.navigate(Screen.ResetPassword.route) }, modifier = Modifier.align(Alignment.End)) { Text("忘记密码？") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.login(phone, password) }, modifier = Modifier.fillMaxWidth().height(48.dp), enabled = loginState !is AuthState.Loading) {
            if (loginState is AuthState.Loading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("登 录")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { navController.navigate(Screen.SmsLogin.route) }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Default.Email, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("验证码登录") }
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Text("还没有账号？", color = MaterialTheme.colorScheme.outline); TextButton(onClick = { navController.navigate(Screen.Register.route) }) { Text("立即注册") } }
    }
}
