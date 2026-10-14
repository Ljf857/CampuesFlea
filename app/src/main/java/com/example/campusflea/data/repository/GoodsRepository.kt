package com.example.campusflea.data.repository

import android.content.Context
import android.net.Uri
import com.example.campusflea.data.model.*
import com.example.campusflea.data.remote.PageResult
import com.example.campusflea.data.remote.RetrofitClient
import com.example.campusflea.data.remote.toFault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class GoodsRepository {
    private val api = RetrofitClient.apiService

    suspend fun getRecommend(page: Int = 1, size: Int = 10): Result<PageResult<GoodsInfo>> = try {
        val r = api.getRecommend(page, size)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getPersonalRecommend(page: Int = 1, size: Int = 10): Result<PageResult<GoodsInfo>> = try {
        val r = api.getPersonalRecommend(page, size)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getCategoryGoods(categoryId: Long, page: Int = 1, size: Int = 10, sort: String? = null): Result<PageResult<GoodsInfo>> = try {
        val r = api.getCategoryGoods(categoryId, page, size, sort)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun search(keyword: String? = null, page: Int = 1, size: Int = 10, categoryId: Long? = null, minPrice: Double? = null, maxPrice: Double? = null, sort: String? = null): Result<PageResult<GoodsInfo>> = try {
        val r = api.search(keyword, page, size, categoryId, minPrice, maxPrice, sort)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getHotKeywords(): Result<List<String>> = try {
        val r = api.getHotKeywords()
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getDetail(id: Long): Result<GoodsDetail> = try {
        val r = api.getGoodsDetail(id)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun publish(request: PublishRequest): Result<Unit> = try {
        val r = api.publishGoods(request)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun update(id: Long, request: PublishRequest): Result<Unit> = try {
        val r = api.updateGoods(id, request)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun updateStatus(id: Long, status: Int): Result<Unit> = try {
        val r = api.updateGoodsStatus(id, mapOf("status" to status))
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun delete(id: Long): Result<Unit> = try {
        val r = api.deleteGoods(id)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getMyGoods(page: Int = 1, size: Int = 10, status: Int? = null): Result<PageResult<GoodsInfo>> = try {
        val r = api.getMyGoods(page, size, status)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun favorite(id: Long): Result<Unit> = try {
        val r = api.favorite(id)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun unfavorite(id: Long): Result<Unit> = try {
        val r = api.unfavorite(id)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getFavorites(page: Int = 1, size: Int = 10): Result<PageResult<GoodsInfo>> = try {
        val r = api.getFavorites(page, size)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun getHistory(page: Int = 1, size: Int = 20): Result<PageResult<GoodsInfo>> = try {
        val r = api.getHistory(page, size)
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun deleteHistory(id: Long): Result<Unit> = try {
        val r = api.deleteHistory(id)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun clearHistory(): Result<Unit> = try {
        val r = api.clearHistory()
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun report(id: Long, request: ReportRequest): Result<Unit> = try {
        val r = api.report(id, request)
        if (r.isSuccess()) Result.success(Unit) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    /**
     * 分类树。
     *
     * 走 [CategoryCache]：首页与分类页共用一份，切 Tab 不再重复请求。
     * 网络失败时退回上次的数据（哪怕已过期），避免分类页整页空白。
     *
     * @param forceRefresh 为 true 时跳过缓存（下拉刷新用）
     */
    suspend fun getCategories(forceRefresh: Boolean = false): Result<List<CategoryInfo>> {
        val cached = CategoryCache.get()
        if (!forceRefresh && cached != null) return Result.success(cached)
        return try {
            val r = api.getCategories()
            if (r.isSuccess() && r.data != null) {
                CategoryCache.put(r.data)
                Result.success(r.data)
            } else {
                CategoryCache.getStale()?.let { return Result.success(it) }
                Result.failure(Exception(r.message))
            }
        } catch (e: Exception) {
            CategoryCache.getStale()?.let { return Result.success(it) }
            Result.failure(e.toFault())
        }
    }

    suspend fun getBanners(): Result<List<BannerInfo>> = try {
        val r = api.getBanners()
        if (r.isSuccess() && r.data != null) Result.success(r.data) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }

    suspend fun uploadGoodsImage(context: Context, uri: Uri): Result<String> = try {
        val (bytes, mimeType) = withContext(Dispatchers.IO) {
            val b = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (b == null) null else b to (context.contentResolver.getType(uri) ?: "image/jpeg")
        } ?: return Result.failure(Exception("无法读取图片"))
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "goods.jpg", requestBody)
        val r = api.uploadImage(part)
        if (r.isSuccess() && r.data != null) Result.success(r.data.url) else Result.failure(Exception(r.message))
    } catch (e: Exception) { Result.failure(e.toFault()) }
}
