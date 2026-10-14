package com.example.campusflea.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campusflea.data.model.*
import com.example.campusflea.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OrderViewModel : ViewModel() {
    private val repo = OrderRepository()
    private val _orders = MutableStateFlow<List<OrderInfo>>(emptyList())
    val orders: StateFlow<List<OrderInfo>> = _orders
    private val _orderDetail = MutableStateFlow<OrderDetail?>(null)
    val orderDetail: StateFlow<OrderDetail?> = _orderDetail
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadOrders(type: String? = null, status: Int? = null) {
        viewModelScope.launch { _isLoading.value = true; repo.getList(type = type, status = status).onSuccess { _orders.value = it.list }; _isLoading.value = false }
    }
    fun loadDetail(id: Long) {
        viewModelScope.launch { _isLoading.value = true; repo.getDetail(id).onSuccess { _orderDetail.value = it }; _isLoading.value = false }
    }
    fun createOrder(goodsId: Long, deliveryType: Int, addressId: Long? = null, remark: String? = null, onResult: (Long?) -> Unit) {
        viewModelScope.launch { repo.create(CreateOrderRequest(goodsId, addressId, deliveryType, remark)).fold(onSuccess = { onResult(it) }, onFailure = { onResult(null) }) }
    }
    fun confirm(id: Long, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { repo.confirm(id).fold(onSuccess = { onResult(true, null); loadDetail(id) }, onFailure = { onResult(false, it.message ?: "确认失败") }) }
    }
    fun cancel(id: Long, reason: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { repo.cancel(id, reason).fold(onSuccess = { onResult(true, null); loadDetail(id) }, onFailure = { onResult(false, it.message ?: "取消失败") }) }
    }
    fun receive(id: Long, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { repo.receive(id).fold(onSuccess = { onResult(true, null); loadDetail(id) }, onFailure = { onResult(false, it.message ?: "确认收货失败") }) }
    }
    fun evaluate(id: Long, rating: Int, content: String, images: List<String> = emptyList(), onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch { repo.evaluate(id, rating, content, images).fold(onSuccess = { onResult(true, null) }, onFailure = { onResult(false, it.message ?: "评价失败") }) }
    }
}
