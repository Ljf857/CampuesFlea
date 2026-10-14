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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.viewmodel.AuthViewModel
import com.example.campusflea.ui.viewmodel.MineViewModel
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePhoneScreen(navController: NavHostController) {
    var newPhone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val authViewModel: AuthViewModel = viewModel()
    val mineViewModel: MineViewModel = viewModel()
    val countdown by authViewModel.smsCountdown.collectAsState()
    val smsSending by authViewModel.smsSending.collectAsState()
    val smsError by authViewModel.smsError.collectAsState()
    val smsTip by authViewModel.smsTip.collectAsState()
    val smsCodeFromVm by authViewModel.smsCode.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(smsError) {
        smsError?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); authViewModel.clearSmsError() }
    }

    LaunchedEffect(smsTip) {
        smsTip?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); authViewModel.clearSmsTip() }
    }

    // 仅模拟通道会回传验证码明文用于自动填充；真实短信通道下为空，由用户从短信读取后手动输入
    LaunchedEffect(smsCodeFromVm) {
        if (smsCodeFromVm.isNotEmpty()) code = smsCodeFromVm
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("修改绑定手机号") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        Column(Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 16.dp)) {
            OutlinedTextField(value = newPhone, onValueChange = { newPhone = it }, label = { Text("新手机号") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("验证码") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), singleLine = true, trailingIcon = { TextButton(onClick = { authViewModel.sendSms(newPhone, "phone") }, enabled = !smsSending && countdown == 0) { Text(when { smsSending -> "发送中"; countdown > 0 -> "${countdown}s"; else -> "获取" }) } })
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                mineViewModel.updatePhone(newPhone, code,
                    onSuccess = { Toast.makeText(context, "手机号修改成功", Toast.LENGTH_SHORT).show(); navController.popBackStack() },
                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                )
            }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("确认修改") }
        }
    }
}
