package com.example.campusflea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.LoginResponse
import com.example.campusflea.data.model.UserInfo
import com.example.campusflea.data.remote.WebSocketManager
import com.example.campusflea.data.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val data: LoginResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    companion object {
        /**
         * 可重发倒计时（秒）。
         *
         * <p>与后端 {@code campus.sms.resend-seconds} 的默认值保持一致：服务端对同一手机号有
         * 60 秒发送冷却（本地 Redis 冷却 + 阿里云 Interval 频控），倒计时早于它结束只会换来
         * 一次「发送过于频繁」的报错，故此处不宜取小。
         */
        private const val SMS_RESEND_INTERVAL_SECONDS = 60

        /** 中国大陆手机号：1 开头、第二位 3-9、共 11 位 */
        private val PHONE_REGEX = Regex("^1[3-9]\\d{9}$")
    }

    private val repo = AuthRepository()
    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState
    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState
    private val _resetPasswordState = MutableStateFlow<AuthState>(AuthState.Idle)
    val resetPasswordState: StateFlow<AuthState> = _resetPasswordState
    private val _smsCountdown = MutableStateFlow(0)
    val smsCountdown: StateFlow<Int> = _smsCountdown
    private val _smsSending = MutableStateFlow(false)

    /** 验证码请求进行中：用于禁点「获取」按钮，避免真实通道下连点触发服务端频控 */
    val smsSending: StateFlow<Boolean> = _smsSending
    private val _smsError = MutableStateFlow<String?>(null)
    val smsError: StateFlow<String?> = _smsError
    private val _smsTip = MutableStateFlow<String?>(null)

    /** 发送成功后的提示语（区分真实通道与模拟通道），由页面以 Toast 呈现后清除 */
    val smsTip: StateFlow<String?> = _smsTip
    private val _smsCode = MutableStateFlow("")

    /** 模拟通道回传的验证码明文，用于自动填充；真实通道恒为空字符串 */
    val smsCode: StateFlow<String> = _smsCode
    private var countdownJob: Job? = null

    fun smsLogin(phone: String, smsCode: String) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            repo.smsLogin(phone, smsCode).fold(
                onSuccess = { _loginState.value = AuthState.Success(it) },
                onFailure = { _loginState.value = AuthState.Error(it.message ?: "验证码登录失败") }
            )
        }
    }

    fun login(phone: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            repo.login(phone, password).fold(
                onSuccess = { _loginState.value = AuthState.Success(it) },
                onFailure = { _loginState.value = AuthState.Error(it.message ?: "登录失败") }
            )
        }
    }

    fun register(phone: String, password: String, smsCode: String, nickname: String) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            repo.register(phone, password, smsCode, nickname).fold(
                onSuccess = { _registerState.value = AuthState.Success(LoginResponse("", "", 0, UserInfo())) },
                onFailure = { _registerState.value = AuthState.Error(it.message ?: "注册失败") }
            )
        }
    }

    /**
     * 发送短信验证码。
     *
     * <p>服务端 {@code POST /api/auth/sms} 的返回分两种形态：
     * <ul>
     *   <li><b>真实通道</b>（阿里云号码认证服务，{@code campus.sms.enabled=true}）：
     *       验证码由云端生成并保管，响应中不含 {@code code}，只能由用户从短信读取后手动输入；</li>
     *   <li><b>模拟通道</b>（{@code campus.sms.enabled=false}）：响应回传 {@code code} 明文，
     *       本端直接自动填充，便于本地联调与答辩演示。</li>
     * </ul>
     *
     * @param phone 手机号
     * @param type  业务场景，需与后端白名单一致：register-注册、login-登录、reset-重置密码、phone-换绑手机
     */
    fun sendSms(phone: String, type: String) {
        // 请求进行中或冷却未结束时忽略重复点击：真实短信通道计费且有 60 秒频控，不能白发
        if (_smsSending.value || _smsCountdown.value > 0) return

        val trimmed = phone.trim()
        if (!PHONE_REGEX.matches(trimmed)) {
            _smsError.value = "请输入正确的 11 位手机号"
            return
        }

        viewModelScope.launch {
            _smsSending.value = true
            repo.sendSms(trimmed, type).fold(
                onSuccess = { data ->
                    _smsSending.value = false
                    // 无论哪种通道都无条件覆盖：真实通道不回传明文，正好借此清掉上一次可能残留的旧验证码
                    val code = data.code.orEmpty()
                    _smsCode.value = code
                    val validMinutes = (data.expireIn / 60).coerceAtLeast(1)
                    _smsTip.value = if (code.isNotBlank()) {
                        "验证码已发送（模拟通道）：$code，已自动填充"
                    } else {
                        "验证码已发送，${validMinutes} 分钟内有效，请注意查收短信"
                    }
                    startCountdown()
                },
                onFailure = { e ->
                    _smsSending.value = false
                    _smsError.value = e.message ?: "发送验证码失败"
                }
            )
        }
    }

    /** 启动可重发倒计时；独立协程，不阻塞发送流程本身 */
    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _smsCountdown.value = SMS_RESEND_INTERVAL_SECONDS
            while (_smsCountdown.value > 0) {
                delay(1000)
                _smsCountdown.value--
            }
        }
    }

    fun clearSmsError() { _smsError.value = null }

    fun clearSmsTip() { _smsTip.value = null }

    fun resetPassword(phone: String, smsCode: String, newPassword: String) {
        viewModelScope.launch {
            _resetPasswordState.value = AuthState.Loading
            repo.resetPassword(phone, smsCode, newPassword).fold(
                onSuccess = { _resetPasswordState.value = AuthState.Success(LoginResponse("", "", 0, UserInfo())) },
                onFailure = { _resetPasswordState.value = AuthState.Error(it.message ?: "重置失败") }
            )
        }
    }

    fun logout() {
        WebSocketManager.disconnect()
        viewModelScope.launch { repo.logout() }
    }
    suspend fun isLoggedIn(): Boolean = repo.isLoggedIn()
}
