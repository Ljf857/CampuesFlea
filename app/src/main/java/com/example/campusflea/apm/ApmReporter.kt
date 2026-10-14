package com.example.campusflea.apm

import com.example.campusflea.BuildConfig
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 数据上报。
 *
 * 刻意使用独立的 OkHttpClient，不复用业务客户端的 RetrofitClient：
 * 1. 业务客户端挂着认证拦截器和 APM 网络拦截器，上报请求走进去会被加上 token，
 *    还会被自己采集一次，形成"上报产生数据、数据触发上报"的自我放大；
 * 2. 上报属于可丢弃流量，超时必须比业务请求短得多，
 *    否则弱网下一次上报会长时间占住调度线程。
 *
 * 失败一律返回 false 交给上层回填重试，这里不自行重试——
 * 失败往往意味着网络整体不可用，立即重试只会连续失败并徒增耗电。
 */
internal class ApmReporter(private val config: ApmConfig) {

    private val gson = Gson()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(UPLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(UPLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(UPLOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(false)
        .build()

    fun upload(batch: ApmPending, deviceId: String, sessionId: String): Boolean {
        if (config.uploadUrl.isBlank()) {
            ApmLog.d("未配置上报地址，${batch.size} 条数据仅保留在端上")
            return false
        }
        if (batch.isEmpty) return true

        return try {
            val request = Request.Builder()
                .url(config.uploadUrl)
                .header("X-Apm-Client", CLIENT_TAG)
                .post(buildPayload(batch, deviceId, sessionId).toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                ApmLog.d("上报 ${batch.size} 条数据，服务端返回 ${response.code}")
                response.isSuccessful
            }
        } catch (t: Throwable) {
            ApmLog.w("上报失败（${batch.size} 条），已回填待下次重试", t)
            false
        }
    }

    private fun buildPayload(batch: ApmPending, deviceId: String, sessionId: String): String {
        val root = linkedMapOf<String, Any?>(
            "appId" to config.appId,
            "platform" to "android",
            "appVersion" to BuildConfig.VERSION_NAME,
            "osVersion" to android.os.Build.VERSION.RELEASE,
            "apiLevel" to android.os.Build.VERSION.SDK_INT,
            // 仅采集厂商+型号用于机型维度聚合，不采集任何可关联到个人的标识
            "device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            "deviceId" to deviceId,
            "sessionId" to sessionId,
            "reportedAt" to System.currentTimeMillis(),
            "network" to batch.network,
            "crashes" to batch.crashes,
            "startups" to batch.startups,
            "frames" to batch.frames,
            "scenes" to batch.scenes
        )
        return gson.toJson(root)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val CLIENT_TAG = "campus-flea-android"
        const val UPLOAD_TIMEOUT_MS = 8_000L
    }
}
