package com.example.campusflea.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.*
import com.example.campusflea.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repo = UserRepository()
    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo
    private val _verifyStatus = MutableStateFlow<VerifyStatusInfo?>(null)
    val verifyStatus: StateFlow<VerifyStatusInfo?> = _verifyStatus
    private val _addresses = MutableStateFlow<List<AddressInfo>>(emptyList())
    val addresses: StateFlow<List<AddressInfo>> = _addresses

    fun loadProfile() { viewModelScope.launch { repo.getUserInfo().onSuccess { _userInfo.value = it } } }
    fun updateProfile(request: ProfileUpdateRequest, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { repo.updateProfile(request).fold(onSuccess = { onDone(true) }, onFailure = { onDone(false) }) }
    }
    fun loadVerifyStatus() { viewModelScope.launch { repo.getVerifyStatus().onSuccess { _verifyStatus.value = it } } }
    fun loadAddresses() { viewModelScope.launch { repo.getAddressList().onSuccess { _addresses.value = it } } }
    fun addAddress(request: AddressRequest, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { repo.addAddress(request).fold(onSuccess = { onDone(true) }, onFailure = { onDone(false) }) }
    }
    fun updateAddress(id: Long, request: AddressRequest, onDone: (Boolean) -> Unit) {
        viewModelScope.launch { repo.updateAddress(id, request).fold(onSuccess = { onDone(true) }, onFailure = { onDone(false) }) }
    }
    fun deleteAddress(id: Long) { viewModelScope.launch { repo.deleteAddress(id); loadAddresses() } }

    fun uploadAvatar(context: Context, uri: Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repo.uploadAvatar(context, uri).fold(
                onSuccess = { url -> onSuccess(url) },
                onFailure = { onError(it.message ?: "上传失败") }
            )
        }
    }

    fun submitVerify(realName: String, studentNo: String, frontUri: Uri, backUri: Uri, context: Context, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repo.submitVerify(realName, studentNo, frontUri, backUri, context).fold(
                onSuccess = { onDone(true, null) },
                onFailure = { onDone(false, it.message ?: "提交失败") }
            )
        }
    }
}
