package com.example.campusflea.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.local.HistoryLocalRepository
import com.example.campusflea.data.model.GoodsInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = HistoryLocalRepository.getInstance(application)

    val history: StateFlow<List<GoodsInfo>> = repo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteHistory(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repo.clearAll() }
    }
}
