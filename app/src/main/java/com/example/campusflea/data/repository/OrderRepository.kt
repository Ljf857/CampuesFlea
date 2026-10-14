package com.example.campusflea.data.repository

import com.example.campusflea.data.model.*
import com.example.campusflea.data.remote.PageResult
import com.example.campusflea.data.remote.RetrofitClient
import com.example.campusflea.data.remote.toFault

class OrderRepository {
    private val api = RetrofitClient.apiService

    suspend fun create(request: CreateOrderRequest): Result<Long> = try {
        val r = api.createOrder(request)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getList(page: Int = 1, size: Int = 10, type: String? = null, status: Int? = null): Result<PageResult<OrderInfo>> = try {
        val r = api.getOrders(page, size, type, status)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getDetail(id: Long): Result<OrderDetail> = try {
        val r = api.getOrderDetail(id)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun confirm(id: Long): Result<Unit> = try {
        val r = api.confirmOrder(id)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun cancel(id: Long, reason: String): Result<Unit> = try {
        val r = api.cancelOrder(id, CancelRequest(reason))
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun receive(id: Long): Result<Unit> = try {
        val r = api.receiveOrder(id)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun evaluate(id: Long, rating: Int, content: String, images: List<String> = emptyList()): Result<Unit> = try {
        val r = api.evaluateOrder(id, EvaluateRequest(rating, content, images))
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }
}
