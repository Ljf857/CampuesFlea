package com.example.campusflea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.GoodsInfo
import com.example.campusflea.data.repository.GoodsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FavoriteViewModel : ViewModel() {
    private val repo = GoodsRepository()
    private val _favorites = MutableStateFlow<List<GoodsInfo>>(emptyList())
    val favorites: StateFlow<List<GoodsInfo>> = _favorites
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init { loadFavorites() }
    fun loadFavorites() { viewModelScope.launch { _isLoading.value = true; repo.getFavorites().onSuccess { _favorites.value = it.list }; _isLoading.value = false } }
    fun unfavorite(goodsId: Long) { viewModelScope.launch { repo.unfavorite(goodsId); loadFavorites() } }
}
