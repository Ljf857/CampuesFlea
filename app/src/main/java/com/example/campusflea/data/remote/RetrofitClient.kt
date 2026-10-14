package com.example.campusflea.data.remote

import com.example.campusflea.BuildConfig
import com.example.campusflea.CampusFleaApp
import com.example.campusflea.apm.Apm
import com.example.campusflea.data.local.TokenManager
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /**
     * 后端基地址。由 Gradle 从 gradle.properties 的 api.baseUrl 注入
     * （见 app/build.gradle.kts 的 buildConfigField），切换环境只需改配置，不用动代码。
     */
    private val BASE_URL: String = BuildConfig.API_BASE_URL

    /** 当前后端基地址，UI 层拼资源 URL 用 */
    val baseUrl: String = BASE_URL
    /** 后端主机（IP/域名），用于把回环地址替换为实际部署主机 */
    val baseHost: String = BASE_URL.substringAfter("://").substringBefore(":")
    /**
     * MinIO 资源服务基地址：后端返回的相对路径（如 /campus-flea/goods/xx.png）
     * 由它拼接成完整 URL。MinIO 默认与后端同主机、端口 9000。
     */
    val minioBaseUrl: String = "http://$baseHost:9000"

    private val gson = GsonBuilder().setLenient().serializeNulls().create()

    // 仅 debug 输出请求体，release 关闭，避免 token/敏感信息进日志
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        if (original.url.encodedPath.contains("/api/auth/")) {
            return@Interceptor chain.proceed(original)
        }
        val token = runBlocking {
            TokenManager.getInstance(CampusFleaApp.instance).getAccessToken()
        }
        val request = if (!token.isNullOrEmpty()) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else original
        chain.proceed(request)
    }

    /**
     * 刷新 token 专用的 Retrofit：不挂本拦截器，避免刷新请求自身再走 401 刷新逻辑造成递归
     */
    private val refreshApiService: ApiService by lazy {
        Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    /**
     * 并发安全地刷新 access token：Mutex 保证同一时间只有一个请求真正调用 /api/auth/refresh，
     * 其余请求排队，进入后复用刚刷新好的有效 token；刷新失败返回 null。
     */
    private val refreshMutex = Mutex()

    private suspend fun refreshAccessToken(): String? {
        val tm = TokenManager.getInstance(CampusFleaApp.instance)
        refreshMutex.withLock {
            // 可能刚有其他请求刷新完成，先复用现成的有效 token
            val cached = tm.getAccessToken()
            if (!cached.isNullOrEmpty() && !tm.isAccessTokenExpired()) {
                return cached
            }
            val rt = tm.getRefreshToken()
            if (rt.isNullOrEmpty()) return null

            return try {
                val rr = refreshApiService.refreshToken("Bearer $rt")
                if (rr.isSuccess() && rr.data != null) {
                    tm.saveTokens(rr.data.accessToken, rr.data.refreshToken, rr.data.expiresIn)
                    rr.data.accessToken
                } else {
                    // refresh token 已失效/过期，会话不可用，清空登录态
                    tm.clearTokens()
                    null
                }
            } catch (e: Exception) {
                // 网络异常等：保留 token，本次刷新按失败处理，下次再试
                null
            }
        }
    }

    /**
     * 获取一个未过期的 access token：有效则直接返回，过期则自动刷新；刷新失败返回 null。
     * 供 WebSocket 等需要保证 token 有效的场景使用。
     */
    suspend fun getValidAccessToken(): String? {
        val tm = TokenManager.getInstance(CampusFleaApp.instance)
        val access = tm.getAccessToken()
        if (!access.isNullOrEmpty() && !tm.isAccessTokenExpired()) return access
        return refreshAccessToken()
    }

    private val tokenRefreshInterceptor = Interceptor { chain ->
        val request = chain.request()
        // 已经重试过一次就不再重试，避免 refresh 成功但接口仍 401 时无限循环
        if (request.header("X-Auth-Retried") != null) {
            return@Interceptor chain.proceed(request)
        }
        val response = chain.proceed(request)
        if (response.code != 401) return@Interceptor response

        val newToken = runBlocking { getValidAccessToken() }
        if (newToken.isNullOrEmpty()) {
            // 刷新失败（登录态失效或网络异常）：原样返回 401，不 close，避免读取已关闭的响应
            return@Interceptor response
        }
        response.close() // 将用新 token 重试，释放原 401 响应体
        chain.proceed(
            request.newBuilder()
                .header("X-Auth-Retried", "true")
                .header("Authorization", "Bearer $newToken")
                .build()
        )
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(tokenRefreshInterceptor)
        .addInterceptor(loggingInterceptor)
        // APM 网络采集放在最内层：OkHttp 的拦截器按添加顺序由外向内执行，
        // 放最后意味着它只包住真正的网络往返，测到的耗时不会被 Token 读取、
        // 日志打印这些应用层动作污染，慢请求的判断才准。
        .addInterceptor(Apm.networkInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
