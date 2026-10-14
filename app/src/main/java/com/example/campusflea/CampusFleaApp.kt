package com.example.campusflea

import android.app.Application
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.campusflea.apm.Apm
import com.example.campusflea.data.local.TokenManager
import com.example.campusflea.data.remote.WebSocketManager
import com.example.campusflea.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class CampusFleaApp : Application() {
    companion object {
        lateinit var instance: CampusFleaApp
            private set

        /**
         * 登录态失效事件。
         *
         * 单独开一条通道而不是复用页面级的错误提示：会话失效必须由全局层接管并引导回登录页，
         * 否则用户会停留在一个所有请求都静默 401 的界面上，既没有报错也不知道要重新登录。
         * 在 CampusFleaApp 内发出，由 AppNavGraph 消费并导航。
         */
        private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val sessionExpired: SharedFlow<Unit> = _sessionExpired

        fun notifySessionExpired() {
            _sessionExpired.tryEmit(Unit)
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wsJob: Job? = null

    // 应用是否处于前台：仅后台时才弹通知，避免自己发送消息也收到提醒
    @Volatile
    private var isForeground = false

    override fun attachBaseContext(newBase: Context) {
        // 冷启动耗时起点。attachBaseContext 是进程起来后我们能拿到的最早时点，
        // 放在这里才能把 Application 自身的初始化耗时也算进启动时间里。
        Apm.markAttach()
        super.attachBaseContext(newBase)
    }

    override fun onCreate() {
        // 启动打点放在一切初始化之前，否则会把本方法里的耗时漏算掉
        Apm.markAppCreate()
        super.onCreate()
        instance = this

        // APM 性能监控：网络耗时、卡顿、冷启动、崩溃/ANR 四路采集。
        // 开关与上报地址在 gradle.properties 中配置，关闭时全部采集入口短路，无额外开销。
        Apm.init(this)

        NotificationHelper.createChannel(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        isForeground = true
                        // 回前台：重置帧率统计窗口，并把后台期间积压的数据送出去
                        Apm.onForeground()
                        // 回到前台时确认 WebSocket 仍然连接：后台可能被系统回收/掉线导致重连计数耗尽
                        appScope.launch {
                            if (TokenManager.getInstance(this@CampusFleaApp).isLoggedIn()) WebSocketManager.connect()
                        }
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        isForeground = true
                    }
                    Lifecycle.Event.ON_STOP -> {
                        isForeground = false
                        Apm.onBackground()
                    }
                    else -> {}
                }
            }
        })

        // 登录状态下保持 WebSocket 连接，后台收到新消息时推送本地通知
        wsJob = appScope.launch {
            val loggedIn = TokenManager.getInstance(this@CampusFleaApp).isLoggedIn()
            if (loggedIn) WebSocketManager.connect()

            WebSocketManager.events.collect { event ->
                when (event) {
                    is WebSocketManager.WsEvent.NewMessage -> {
                        // 判据是「不是我发的」，不是「应用是否在前台」。
                        // 服务端会把消息回推给收发双方，只靠前台状态判断的话，
                        // 应用在后台时你自己发出的消息也会给自己弹一条新消息通知。
                        if (!event.message.isMine && !isForeground) {
                            NotificationHelper.showNewMessage(this@CampusFleaApp, event.message)
                        }
                    }
                    is WebSocketManager.WsEvent.SessionExpired -> {
                        appScope.launch {
                            TokenManager.getInstance(this@CampusFleaApp).clearTokens()
                        }
                        notifySessionExpired()
                    }
                    else -> {}
                }
            }
        }
    }
}
