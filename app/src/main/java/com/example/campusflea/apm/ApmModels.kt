package com.example.campusflea.apm

/**
 * 一次网络请求的观测记录。
 *
 * [url] 存的是归一化后的路径模板（数字段已替换为 `{id}`），不是原始 URL。
 * 原始 URL 会把 /goods/1001、/goods/1002 统计成两个接口，
 * 接口维度的 P99 会因此被稀释到毫无意义。
 */
data class ApmNetworkRecord(
    val url: String,
    val method: String,
    val code: Int,
    val durationMs: Long,
    val requestBytes: Long,
    val responseBytes: Long,
    /** 非空表示请求在传输层就失败了，此时 [code] 为 -1。 */
    val errorType: String?,
    /** 进程会话标识，用于把同一次进程运行期间产生的数据归到一条会话上。 */
    val traceId: String,
    val timestamp: Long
) {
    /**
     * 传输/HTTP 层面的成功。与后端 `SUCCESS_CONDITION` 保持一致：
     * 任何非 2xx/3xx 都算失败，含 401/403。它决定的是「失败数」这个原始事实。
     */
    val success: Boolean get() = errorType == null && code in 200..399

    /**
     * 鉴权失败：401/403。这类请求反映的是「当时没有登录态」而不是接口健康度，
     * 因此被排除在成功率的分母之外（见 [ApmNetworkSummary.successRate]）。
     * 注意传输层失败（code = -1）不属于此类。
     */
    val isAuthFailure: Boolean get() = code == 401 || code == 403
}

/** 时间窗口内的帧率与卡顿统计。 */
data class ApmFrameRecord(
    val fps: Float,
    val totalFrames: Int,
    /** 单帧耗时超过卡顿阈值的帧数。 */
    val jankyFrames: Int,
    /** 单帧耗时超过冻结帧阈值的帧数。 */
    val frozenFrames: Int,
    val worstFrameMs: Long,
    val windowMs: Long,
    val timestamp: Long
)

/** 冷启动耗时及其阶段拆分。 */
data class ApmStartupRecord(
    val totalMs: Long,
    val cold: Boolean,
    /** 阶段名 -> 耗时（ms）。 */
    val stages: Map<String, Long> = emptyMap(),
    val timestamp: Long
)

/** 崩溃或 ANR 记录。 */
data class ApmCrashRecord(
    /** crash | anr */
    val type: String,
    val message: String,
    val stack: String,
    val threadName: String,
    val foreground: Boolean,
    val timestamp: Long
) {
    val isAnr: Boolean get() = type == "anr"
}

/** 自定义区间埋点，例如页面停留时长、一次下单流程的总耗时。 */
data class ApmSceneRecord(
    val name: String,
    val durationMs: Long,
    val timestamp: Long
)

/** 网络指标的聚合结果。 */
data class ApmNetworkSummary(
    val total: Long,
    /** 失败数，含鉴权失败。这是原始事实，不做任何过滤。 */
    val failure: Long,
    /** 其中的鉴权失败数（401/403），已从 [successRate] 的分母中排除，单独展示。 */
    val authFailure: Long,
    /**
     * 成功率，0..1。分母是「可判定请求数」= [total] - [authFailure]。
     * 没有可判定请求时为 null，调用方应显示成「—」而不是 0%——
     * 「一次都没被判定过」和「判定过的全失败」是两回事。
     */
    val successRate: Float?,
    val p50: Long,
    val p90: Long,
    val p99: Long,
    val avg: Long,
    val max: Long
)

/**
 * 对外暴露的只读快照，供监控面板渲染。
 *
 * 面板拿到的是某一瞬间的拷贝而不是模块内部引用：
 * 采集线程仍在持续写入，直接暴露内部集合会同时带来并发问题和"边遍历边被改"的崩溃。
 */
data class ApmSnapshot(
    val enabled: Boolean,
    val startup: ApmStartupRecord?,
    val latestFrame: ApmFrameRecord?,
    val avgFps: Float,
    val jankRate: Float,
    val network: ApmNetworkSummary,
    val slowRequests: List<ApmNetworkRecord>,
    val crashes: List<ApmCrashRecord>,
    val scenes: List<ApmSceneRecord>,
    /** 尚未上报的条目数。长期不降说明上报链路有问题。 */
    val pendingUpload: Int,
    val uploadedTotal: Long,
    val lastUploadAt: Long,
    val lastUploadOk: Boolean
)
