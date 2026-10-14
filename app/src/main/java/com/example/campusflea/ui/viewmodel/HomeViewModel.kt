package com.example.campusflea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.BannerInfo
import com.example.campusflea.data.model.CategoryInfo
import com.example.campusflea.data.model.GoodsInfo
import com.example.campusflea.data.repository.GoodsRepository
import com.example.campusflea.data.repository.MessageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repo = GoodsRepository()
    private val msgRepo = MessageRepository()
    private val _goods = MutableStateFlow<List<GoodsInfo>>(emptyList())
    val goods: StateFlow<List<GoodsInfo>> = _goods
    private val _categories = MutableStateFlow<List<CategoryInfo>>(emptyList())
    val categories: StateFlow<List<CategoryInfo>> = _categories
    private val _banners = MutableStateFlow<List<BannerInfo>>(emptyList())
    val banners: StateFlow<List<BannerInfo>> = _banners
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing
    private var currentPage = 1
    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore
    private var listJob: Job? = null
    private val _hasUnread = MutableStateFlow(false)
    val hasUnread: StateFlow<Boolean> = _hasUnread
    private val _isPersonalized = MutableStateFlow(false)
    val isPersonalized: StateFlow<Boolean> = _isPersonalized
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty

    init { loadData() }
    private fun loadData() { loadBanners(); loadCategories(); loadRecommend(); checkUnread() }
    fun loadBanners() { viewModelScope.launch { repo.getBanners().onSuccess { _banners.value = it } } }

    fun loadCategories() {
        viewModelScope.launch { repo.getCategories().onSuccess { _categories.value = it.filter { c -> c.status == 1 } } }
    }

    fun loadRecommend() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _isEmpty.value = false
            currentPage = 1
            repo.getPersonalRecommend(currentPage).fold(
                onSuccess = {
                    _goods.value = it.list
                    _hasMore.value = currentPage < it.totalPages
                    _isPersonalized.value = true
                    _isEmpty.value = it.list.isEmpty()
                },
                onFailure = {
                    repo.getRecommend(currentPage).fold(
                        onSuccess = {
                            _goods.value = it.list
                            _hasMore.value = currentPage < it.totalPages
                            _isPersonalized.value = false
                            _isEmpty.value = it.list.isEmpty()
                        },
                        onFailure = { e ->
                            _error.value = e.message ?: "获取推荐失败，请检查网络后重试"
                            _isEmpty.value = true
                        }
                    )
                }
            )
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (!_hasMore.value || _isLoading.value || _isRefreshing.value) return
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _isLoading.value = true
            val page = currentPage + 1
            currentPage = page
            val call = if (_isPersonalized.value) repo.getPersonalRecommend(page) else repo.getRecommend(page)
            call.fold(
                onSuccess = {
                    _goods.value = _goods.value + it.list
                    _hasMore.value = page < it.totalPages
                    if (it.list.isEmpty()) _hasMore.value = false
                },
                onFailure = {
                    currentPage = page - 1
                    _error.value = "加载更多失败"
                }
            )
            _isLoading.value = false
        }
    }

    fun refresh() {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null
            _isEmpty.value = false
            currentPage = 1
            loadBanners(); checkUnread()
            repo.getPersonalRecommend(currentPage).fold(
                onSuccess = {
                    _goods.value = it.list
                    _hasMore.value = currentPage < it.totalPages
                    _isPersonalized.value = true
                    _isEmpty.value = it.list.isEmpty()
                },
                onFailure = {
                    repo.getRecommend(currentPage).fold(
                        onSuccess = {
                            _goods.value = it.list
                            _hasMore.value = currentPage < it.totalPages
                            _isPersonalized.value = false
                            _isEmpty.value = it.list.isEmpty()
                        },
                        onFailure = { e ->
                            _error.value = e.message ?: "刷新失败，请检查网络后重试"
                            _isEmpty.value = true
                        }
                    )
                }
            )
            _isRefreshing.value = false
        }
    }

    fun checkUnread() {
        viewModelScope.launch {
            msgRepo.getConversations().onSuccess { _hasUnread.value = it.any { conv -> conv.unreadCount > 0 } }
        }
    }
}
