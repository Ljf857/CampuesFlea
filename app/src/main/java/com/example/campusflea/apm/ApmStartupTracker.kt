package com.example.campusflea.apm

/**
 * 冷启动耗时采集。
 *
 * 起点取 `Application.attachBaseContext`，终点取首个 Activity 完成首次布局绘制之后。
 * 之所以不用 `onResume` 作为终点：onResume 时界面对用户还是不可见的，
 * 用户真正感知到的"启动完成"是屏幕上出现内容那一刻，用 onResume 会系统性低估。
 *
 * 只在进程内首个 Activity 上报一次，避免返回后台再切回来被记成新的启动。
 */
internal object ApmStartupTracker {

    private var attachAtNs = 0L
    private var appCreateAtNs = 0L
    private var reported = false

    /** 在 Application.attachBaseContext 中调用，是整个启动链路能拿到的最早时点。 */
    @Synchronized
    fun markAttach() {
        attachAtNs = System.nanoTime()
    }

    @Synchronized
    fun markAppCreate() {
        if (reported) return
        if (attachAtNs == 0L) attachAtNs = System.nanoTime()
        appCreateAtNs = System.nanoTime()
    }

    /**
     * 首屏绘制完成时调用。
     * 返回 null 表示本次不是有效启动（没有起点，或已经上报过）。
     */
    @Synchronized
    fun markFirstFrame(): ApmStartupRecord? {
        if (reported) return null
        val startNs = attachAtNs.takeIf { it != 0L } ?: return null
        reported = true

        val nowNs = System.nanoTime()
        val createNs = if (appCreateAtNs > startNs) appCreateAtNs else startNs

        val stages = linkedMapOf<String, Long>()
        stages["Application 初始化"] = (createNs - startNs) / 1_000_000L
        stages["首屏渲染"] = (nowNs - createNs) / 1_000_000L

        return ApmStartupRecord(
            totalMs = (nowNs - startNs) / 1_000_000L,
            // attachBaseContext 只在进程内执行一次，走到这里说明是新进程，即冷启动。
            cold = true,
            stages = stages,
            timestamp = System.currentTimeMillis()
        )
    }
}
