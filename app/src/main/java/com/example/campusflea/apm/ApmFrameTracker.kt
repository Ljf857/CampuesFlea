package com.example.campusflea.apm

import android.os.Handler
import android.os.Looper
import android.view.Choreographer

/**
 * 帧率与卡顿采集。
 *
 * 用 Choreographer 而不是自己起定时器：Choreographer 的回调由 vsync 驱动，
 * 拿到的是渲染管线真实产出一帧的节奏，能反映掉帧；定时器只能测出主线程是不是空闲。
 *
 * 每 [windowMs] 汇总一次并清空窗口，避免帧数无限累加导致 FPS 被平均成一条直线，
 * 那样"卡了几秒"这种突发问题会被长时间的正常帧稀释掉。
 */
internal class ApmFrameTracker(
    private val slowFrameMs: Long,
    private val frozenFrameMs: Long,
    private val windowMs: Long = 5_000L,
    private val onReport: (ApmFrameRecord) -> Unit
) {

    private val choreographer: Choreographer = Choreographer.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var running = false
    private var lastFrameNs = 0L
    private var windowStartNs = 0L
    private var frames = 0
    private var janky = 0
    private var frozen = 0
    private var worstMs = 0L

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return

            if (lastFrameNs != 0L) {
                val deltaMs = (frameTimeNanos - lastFrameNs) / 1_000_000L
                frames++
                if (deltaMs >= frozenFrameMs) frozen++
                if (deltaMs >= slowFrameMs) janky++
                if (deltaMs > worstMs) worstMs = deltaMs
            }
            lastFrameNs = frameTimeNanos

            val windowElapsedMs = (frameTimeNanos - windowStartNs) / 1_000_000L
            if (windowElapsedMs >= windowMs) {
                report(windowElapsedMs)
                windowStartNs = frameTimeNanos
            }
            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { start() }
            return
        }
        running = true
        lastFrameNs = 0L
        windowStartNs = System.nanoTime()
        resetWindow()
        choreographer.postFrameCallback(callback)
    }

    fun stop() {
        if (!running) return
        running = false
        choreographer.removeFrameCallback(callback)
    }

    /**
     * 回到前台时调用。
     *
     * 必须清掉上一帧的时间戳：应用在后台时 Choreographer 收不到 vsync，
     * 回调会停住，等切回前台第一帧的时间差会是"整个后台时长"，
     * 不清掉就会凭空多出一帧几十万毫秒的假卡顿。
     */
    fun onForeground() {
        lastFrameNs = 0L
        windowStartNs = System.nanoTime()
        resetWindow()
    }

    private fun resetWindow() {
        frames = 0
        janky = 0
        frozen = 0
        worstMs = 0L
    }

    private fun report(windowElapsedMs: Long) {
        if (frames <= 0) {
            resetWindow()
            return
        }
        val fps = frames * 1000f / windowElapsedMs.coerceAtLeast(1L)
        onReport(
            ApmFrameRecord(
                fps = fps,
                totalFrames = frames,
                jankyFrames = janky,
                frozenFrames = frozen,
                worstFrameMs = worstMs,
                windowMs = windowElapsedMs,
                timestamp = System.currentTimeMillis()
            )
        )
        resetWindow()
    }
}
