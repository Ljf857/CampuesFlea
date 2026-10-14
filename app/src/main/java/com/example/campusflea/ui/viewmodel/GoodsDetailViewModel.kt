package com.example.campusflea.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.local.HistoryLocalRepository
import com.example.campusflea.data.model.GoodsDetail
import com.example.campusflea.data.model.ReportRequest
import com.example.campusflea.data.repository.GoodsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GoodsDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = GoodsRepository()
    private val historyRepo = HistoryLocalRepository.getInstance(application)
    private val _detail = MutableStateFlow<GoodsDetail?>(null)
    val detail: StateFlow<GoodsDetail?> = _detail
    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    fun clearError() { _error.value = null }

    fun loadDetail(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            repo.getDetail(id).onSuccess {
                _detail.value = it; _isFavorited.value = it.isFavorited
                historyRepo.insert(it.id, it.title, it.price, it.images.firstOrNull() ?: "")
            }
            _isLoading.value = false
        }
    }

    // 乐观更新：先切换状态，失败时回滚
    fun toggleFavorite() {
        val d = _detail.value ?: return
        val target = !_isFavorited.value
        viewModelScope.launch {
            _isFavorited.value = target
            val result = if (target) repo.favorite(d.id) else repo.unfavorite(d.id)
            result.onFailure {
                _isFavorited.value = !target
                _error.value = it.message ?: "操作失败，请重试"
            }
        }
    }

    fun report(goodsId: Long, reason: String, desc: String, images: List<String> = emptyList(), onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            repo.report(goodsId, ReportRequest(reason, desc, images))
                .fold(onSuccess = { onResult(true, null) }, onFailure = { onResult(false, it.message ?: "举报提交失败") })
        }
    }
}
