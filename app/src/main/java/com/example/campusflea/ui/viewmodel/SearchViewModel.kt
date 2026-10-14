package com.example.campusflea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.GoodsInfo
import com.example.campusflea.data.repository.GoodsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val repo = GoodsRepository()
    private val _results = MutableStateFlow<List<GoodsInfo>>(emptyList())
    val results: StateFlow<List<GoodsInfo>> = _results
    private val _hotKeywords = MutableStateFlow<List<String>>(emptyList())
    val hotKeywords: StateFlow<List<String>> = _hotKeywords
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private var currentPage = 1
    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore
    private var currentKeyword: String? = null
    private var currentCategoryId: Long? = null
    private var searchJob: Job? = null

    init { loadHotKeywords() }
    fun loadHotKeywords() { viewModelScope.launch { repo.getHotKeywords().onSuccess { _hotKeywords.value = it } } }
    fun search(keyword: String? = null, categoryId: Long? = null) {
        searchJob?.cancel()
        currentKeyword = keyword; currentCategoryId = categoryId; currentPage = 1
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _results.value = emptyList()
            repo.search(keyword, currentPage, categoryId = categoryId).fold(
                onSuccess = { _results.value = it.list; _hasMore.value = currentPage < it.totalPages },
                onFailure = { _error.value = it.message ?: "搜索失败，请检查网络后重试" }
            )
            _isLoading.value = false
        }
    }
    fun loadMore() {
        if (!_hasMore.value || _isLoading.value) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            val page = currentPage + 1
            currentPage = page
            repo.search(currentKeyword, page, categoryId = currentCategoryId).fold(
                onSuccess = { _results.value = _results.value + it.list; _hasMore.value = page < it.totalPages },
                onFailure = { currentPage = page - 1; _error.value = "加载更多失败" }
            )
            _isLoading.value = false
        }
    }
}
