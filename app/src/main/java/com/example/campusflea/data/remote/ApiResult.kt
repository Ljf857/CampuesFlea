package com.example.campusflea.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException
import java.io.IOException

data class ApiResult<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?,
    @SerializedName("timestamp") val timestamp: Long = 0
) {
    fun isSuccess(): Boolean = code == 200
}

data class PageResult<T>(
    @SerializedName("list") val list: List<T>,
    @SerializedName("total") val total: Long,
    @SerializedName("page") val page: Long,
    @SerializedName("size") val size: Long,
    @SerializedName("totalPages") val totalPages: Long
)

/**
 * 后端统一错误响应体（仅用于从 HttpException 的 errorBody 里提取用户可读的 message）
 */
private data class ApiErrorBody(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null
)

/**
 * 把任意异常包装成一个 message 友好的 Exception，供仓库层 catch 后放入 Result.failure。
 * - HttpException：优先解析错误响应体里的 message（如“成色不能为空”），取不到再退回 “HTTP 400 ...”
 * - 其他 IO/网络类异常：统一提示“网络异常，请检查网络连接”
 * - 其余：使用原始 message（避免空串）
 */
fun Throwable.toFault(): Exception {
    val friendly: String = when (this) {
        is HttpException -> errorBodyMessage() ?: "HTTP ${code()} ${message()}".trim()
        is IOException -> "网络异常，请检查网络连接"
        else -> message ?: "请求失败"
    }
    return Exception(friendly)
}

private fun HttpException.errorBodyMessage(): String? = try {
    response()?.errorBody()?.string()
        ?.takeIf { it.isNotBlank() }
        ?.let { Gson().fromJson(it, ApiErrorBody::class.java) }
        ?.message
        ?.takeIf { it.isNotBlank() }
} catch (_: Exception) {
    null
}
