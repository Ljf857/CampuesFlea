package com.example.campusflea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.CategoryInfo
import com.example.campusflea.data.repository.GoodsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoryViewModel : ViewModel() {

    private val repo = GoodsRepository()

    private val _categories = MutableStateFlow<List<CategoryInfo>>(emptyList())
    val categories: StateFlow<List<CategoryInfo>> = _categories

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadCategories()
    }

    /**
     * 加载分类树。
     *
     * 数据来自 [GoodsRepository.getCategories]，带进程内缓存：
     * 首页已经拉过一次时这里会立即返回，不会再有可见的等待。
     * 因此 [isLoading] 只在真正需要请求时才为 true，骨架屏不会在每次进入时闪一下。
     *
     * @param forceRefresh 跳过缓存重新拉取（错误重试时用）
     */
    fun loadCategories(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repo.getCategories(forceRefresh)
                .onSuccess { list ->
                    val visible = list.filter { it.status == 1 }
                    _categories.value = visible
                    // 分类增删后把选中项收进有效范围。
                    // 不处理的话会停在越界索引上，表现为「左侧没有高亮、右侧一片空白」。
                    if (_selectedIndex.value !in visible.indices) {
                        _selectedIndex.value = 0
                    }
                }
                .onFailure { _error.value = it.message ?: "分类加载失败" }
            _isLoading.value = false
        }
    }

    fun selectIndex(index: Int) {
        if (index in _categories.value.indices) {
            _selectedIndex.value = index
        }
    }
}
