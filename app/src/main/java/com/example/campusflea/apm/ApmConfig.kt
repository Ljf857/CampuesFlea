package com.example.campusflea.apm

import com.example.campusflea.BuildConfig

/**
 * APM 模块配置。
 *
 * 所有阈值都做成可调参数而不是写死在采集逻辑里：卡顿、慢请求这类判定标准高度依赖业务形态
 * （翻页流和实时音视频对"慢"的定义完全不是一个量级），写死了后面就调不动了。
 *
 * 开关与上报地址通过 Gradle 注入 BuildConfig，改配置不需要动代码：
 * 见 gradle.properties 中的 apm.* 字段。
 */
data class ApmConfig(
    /** 总开关。关闭后所有采集入口直接返回，不产生任何可测量的开销。 */
    val enabled: Boolean = BuildConfig.APM_ENABLED,
    /** 应用标识，随每条数据上报，服务端用于区分数据来源。 */
    val appId: String = BuildConfig.APM_APP_ID,
    /** 上报地址；为空表示只做端上采集，不产生任何外发请求。 */
    val uploadUrl: String = BuildConfig.APM_UPLOAD_URL,
    /** 单批最多携带的条目数。弱网下批量过大会直接超时失败，不如拆小。 */
    val batchSize: Int = 50,
    /** 定时上报间隔。 */
    val flushIntervalMs: Long = 30_000L,
    /** 慢请求阈值，超过单独计数并在面板中排名。 */
    val slowRequestMs: Long = 800L,
    /** 卡顿帧阈值。16.67ms 是 60Hz 的一帧，超过 2 帧用户已能感知。 */
    val slowFrameMs: Long = 32L,
    /** 严重卡顿（冻结帧）阈值，对齐 Android 官方对 frozen frame 的定义。 */
    val frozenFrameMs: Long = 700L,
    /** 主线程阻塞阈值，超过则记为一次 ANR 风险并抓取主线程堆栈。 */
    val anrBlockMs: Long = 3_000L,
    /** 依赖用户触摸的实时交互场景，允许放宽阈值。 */
    val maxBufferSize: Int = 300,
    /** 是否输出 APM 自身日志。 */
    val debugLog: Boolean = BuildConfig.DEBUG
)
