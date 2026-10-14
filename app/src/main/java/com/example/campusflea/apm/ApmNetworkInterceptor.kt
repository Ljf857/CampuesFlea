package com.example.campusflea.apm

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * 网络性能采集拦截器。
 *
 * 必须挂在 OkHttp 链路的最外层：只有最外层才能观察到 DNS 解析、TCP 连接、
 * 请求写入、响应读取这一整段耗时。挂在靠里的位置会把连接建立的时间漏掉，
 * 统计出来的耗时比用户真实感知的偏小，慢请求也就抓不出来了。
 *
 * 注意 `chain.proceed` 在网络失败时抛的 IOException 必须原样往上抛，
 * 采集不能把一次真实的网络失败变成一个"看起来成功但没数据"的响应。
 */
internal class ApmNetworkInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!Apm.isEnabled) return chain.proceed(chain.request())

        val request = chain.request()
        val path = Apm.normalizePath(request.url.encodedPath)
        val method = request.method
        val requestBytes = runCatching { request.body?.contentLength() ?: 0L }.getOrDefault(0L)
        val startNs = System.nanoTime()

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: IOException) {
            report(
                path = path,
                method = method,
                code = CODE_NO_RESPONSE,
                durationMs = elapsedMs(startNs),
                requestBytes = requestBytes,
                responseBytes = 0L,
                errorType = classify(e)
            )
            throw e
        }

        report(
            path = path,
            method = method,
            code = response.code,
            durationMs = elapsedMs(startNs),
            requestBytes = requestBytes,
            // body.contentLength() 只读头部声明，不会消费响应体，读它是安全的；
            // 无 Content-Length 的流式响应（如 WebSocket 升级）返回 -1，此时按未知处理。
            responseBytes = runCatching { response.body?.contentLength() ?: -1L }.getOrDefault(-1L),
            errorType = if (response.isSuccessful) null else "http"
        )
        return response
    }

    private fun report(
        path: String,
        method: String,
        code: Int,
        durationMs: Long,
        requestBytes: Long,
        responseBytes: Long,
        errorType: String?
    ) {
        Apm.recordNetwork(
            path = path,
            method = method,
            code = code,
            durationMs = durationMs,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            errorType = errorType,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun elapsedMs(startNs: Long): Long =
        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

    private fun classify(e: IOException): String = when (e) {
        is SocketTimeoutException -> "timeout"
        is UnknownHostException -> "dns"
        is ConnectException -> "connect"
        else -> "io"
    }

    private companion object {
        const val CODE_NO_RESPONSE = -1
    }
}
