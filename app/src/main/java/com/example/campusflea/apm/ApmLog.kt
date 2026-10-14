package com.example.campusflea.apm

import android.util.Log

/**
 * APM 内部日志。
 *
 * 单独走一个 Tag 而不是复用业务日志：排查性能数据是否上报成功时，
 * 一条 `adb logcat -s Apm` 就能过滤出模块自身的行为，
 * 不至于和业务日志混在一起翻。
 */
internal object ApmLog {

    private const val TAG = "Apm"

    @Volatile
    var enabled: Boolean = false

    fun d(message: String) {
        if (enabled) Log.d(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        if (enabled) Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        // 错误日志不受开关约束：采集模块自身出问题必须能被发现，
        // 否则会表现成"监控没数据"，而没人知道是监控自己坏了。
        Log.e(TAG, message, throwable)
    }
}
