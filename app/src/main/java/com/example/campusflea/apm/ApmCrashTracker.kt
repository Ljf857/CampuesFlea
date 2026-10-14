package com.example.campusflea.apm

import android.os.Handler
import android.os.Looper
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 崩溃与 ANR 采集。
 *
 * 两条独立链路：
 * - 崩溃：接管 `Thread.setDefaultUncaughtExceptionHandler`，记录后**必须**把异常交还给原处理器。
 *   采集模块最忌讳改变原有语义——如果这里把异常吞掉，线上的崩溃就会静默消失，
 *   比采集不到数据严重得多。
 * - ANR：Android 不允许应用层直接监听 ANR，这里用看门狗近似判定——
 *   向主线程投递一个空任务，若超过阈值仍未被消费，说明主线程确实卡住了，
 *   此时立刻抓取主线程堆栈，这正是排查 ANR 最需要的那份现场。
 */
internal class ApmCrashTracker(
    private val anrBlockMs: Long,
    private val isForeground: () -> Boolean,
    private val onCrash: (ApmCrashRecord) -> Unit,
    /** 崩溃后进程即将结束，需要立刻把数据抢送出去。 */
    private val flushBeforeExit: () -> Unit
) {

    private val previousHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var watchdog: ScheduledExecutorService? = null

    @Volatile
    private var lastAnrAt = 0L

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                onCrash(
                    ApmCrashRecord(
                        type = TYPE_CRASH,
                        message = throwable.message ?: throwable.javaClass.name,
                        stack = stackToString(throwable),
                        threadName = thread.name,
                        foreground = isForeground(),
                        timestamp = System.currentTimeMillis()
                    )
                )
                // 单独开线程并限时等待：崩溃线程本身状态不可信，
                // 直接在当前线程做网络请求可能永远回不来，把崩溃处理也一起挂死。
                val flusher = Thread({ flushBeforeExit() }, "apm-crash-flush")
                flusher.start()
                flusher.join(CRASH_FLUSH_TIMEOUT_MS)
            } catch (t: Throwable) {
                ApmLog.e("崩溃信息采集失败", t)
            }

            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }

        startWatchdog()
    }

    fun shutdown() {
        watchdog?.shutdownNow()
        watchdog = null
    }

    private fun startWatchdog() {
        if (watchdog != null) return
        val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "apm-anr-watchdog").apply { isDaemon = true }
        }
        executor.scheduleWithFixedDelay(
            {
                try {
                    checkMainThreadBlocked()
                } catch (t: Throwable) {
                    ApmLog.e("ANR 看门狗异常", t)
                }
            },
            WATCH_INTERVAL_MS,
            WATCH_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
        watchdog = executor
    }

    private fun checkMainThreadBlocked() {
        val latch = CountDownLatch(1)
        if (!mainHandler.post { latch.countDown() }) return

        // await 返回 true 说明主线程在阈值内消费了这个空任务，一切正常
        if (latch.await(anrBlockMs, TimeUnit.MILLISECONDS)) return

        val now = System.currentTimeMillis()
        // 主线程被长时间卡住时会连续命中，加冷却避免同一段阻塞刷出一堆记录
        if (now - lastAnrAt < ANR_COOLDOWN_MS) return
        lastAnrAt = now

        onCrash(
            ApmCrashRecord(
                type = TYPE_ANR,
                message = "主线程阻塞超过 ${anrBlockMs}ms",
                stack = mainThreadStack(),
                threadName = "main",
                foreground = isForeground(),
                timestamp = now
            )
        )
    }

    private fun mainThreadStack(): String =
        runCatching {
            Looper.getMainLooper().thread.stackTrace.joinToString("\n") { "    at $it" }
        }.getOrDefault("主线程堆栈获取失败")

    private fun stackToString(throwable: Throwable): String {
        val text = runCatching {
            val writer = StringWriter()
            throwable.printStackTrace(PrintWriter(writer))
            writer.toString()
        }.getOrDefault("${throwable.javaClass.name}: ${throwable.message}")
        return if (text.length > MAX_STACK_LENGTH) text.take(MAX_STACK_LENGTH) else text
    }

    private companion object {
        const val TYPE_CRASH = "crash"
        const val TYPE_ANR = "anr"

        const val WATCH_INTERVAL_MS = 5_000L
        const val ANR_COOLDOWN_MS = 30_000L
        const val CRASH_FLUSH_TIMEOUT_MS = 2_500L
        const val MAX_STACK_LENGTH = 8_000
    }
}
