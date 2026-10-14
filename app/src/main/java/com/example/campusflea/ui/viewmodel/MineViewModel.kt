package com.example.campusflea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.UserInfo
import com.example.campusflea.data.remote.WebSocketManager
import com.example.campusflea.data.repository.AuthRepository
import com.example.campusflea.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MineViewModel : ViewModel() {
    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init { checkLoginState() }
    private fun checkLoginState() { viewModelScope.launch { _isLoggedIn.value = authRepo.isLoggedIn(); if (_isLoggedIn.value) loadUserInfo() } }
    fun loadUserInfo() { viewModelScope.launch { _isLoading.value = true; userRepo.getUserInfo().onSuccess { _userInfo.value = it }; _isLoading.value = false } }
    fun refreshLoginState() { checkLoginState() }
    fun logout(onDone: () -> Unit) {
        WebSocketManager.disconnect()
        viewModelScope.launch { authRepo.logout(); _isLoggedIn.value = false; _userInfo.value = null; onDone() }
    }
    fun changePassword(oldPwd: String, newPwd: String, onSuccess: () -> Unit, onError: (String) -> Unit) { viewModelScope.launch { userRepo.changePassword(oldPwd, newPwd).fold(onSuccess = { onSuccess() }, onFailure = { onError(it.message ?: "修改失败") }) } }
    fun updatePhone(newPhone: String, smsCode: String, onSuccess: () -> Unit, onError: (String) -> Unit) { viewModelScope.launch { userRepo.updatePhone(newPhone, smsCode).fold(onSuccess = { onSuccess() }, onFailure = { onError(it.message ?: "换绑失败") }) } }
}
