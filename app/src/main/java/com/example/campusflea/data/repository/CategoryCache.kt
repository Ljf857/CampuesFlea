package com.example.campusflea.data.repository

import com.example.campusflea.data.model.CategoryInfo

/**
 * 分类树的进程内缓存。
 *
 * ### 为什么需要它
 *
 * 分类树在一次会话里几乎不变（后台改了才会变），但之前每次进入页面都会重新请求：
 *   - 首页与分类页各持有自己的 ViewModel，两者都会拉一次**完全相同**的分类树；
 *   - Tab 切换用的是 `popUpTo`，离开即销毁 ViewModel，切回来又是一次完整请求。
 * 结果是每次点「分类」都要等一次网络往返，而这段时间页面是空白的——用户感知就是「慢」。
 *
 * 有了这层缓存后：
 *   - 首页拉过一次，分类页立刻命中；
 *   - 切走再切回来也是瞬时；
 *   - 网络抖动时还能退回上次的数据，不至于整页空白。
 *
 * ### 为什么是 5 分钟
 *
 * 服务端 `category:tree` 本身的缓存是 10 分钟，客户端再短一些即可。
 * 更长会让后台改完分类长时间看不到效果，更短则起不到作用。
 * 需要立即生效时调用 [invalidate]（例如下拉刷新）。
 */
object CategoryCache {

    private const val TTL_MS = 5 * 60 * 1000L

    @Volatile
    private var snapshot: List<CategoryInfo>? = null

    @Volatile
    private var cachedAt: Long = 0L

    /** 取缓存；已过期返回 null */
    fun get(): List<CategoryInfo>? {
        val value = snapshot ?: return null
        if (System.currentTimeMillis() - cachedAt > TTL_MS) return null
        return value
    }

    /** 取缓存，忽略过期时间。用于网络失败时兜底 */
    fun getStale(): List<CategoryInfo>? = snapshot

    fun put(list: List<CategoryInfo>) {
        snapshot = list
        cachedAt = System.currentTimeMillis()
    }

    fun invalidate() {
        snapshot = null
        cachedAt = 0L
    }
}
