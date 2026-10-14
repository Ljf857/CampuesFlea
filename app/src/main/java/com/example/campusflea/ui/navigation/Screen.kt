package com.example.campusflea.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object SmsLogin : Screen("sms_login")
    object ResetPassword : Screen("reset_password")
    object Main : Screen("main")
    object Home : Screen("home")
    object Category : Screen("category")
    object Message : Screen("message")
    object Mine : Screen("mine")
    object Search : Screen("search")
    object SearchResult : Screen("search_result?keyword={keyword}&categoryId={categoryId}") {
        fun createRoute(keyword: String = "", categoryId: Long? = null) =
            buildString {
                append("search_result?keyword=${java.net.URLEncoder.encode(keyword, "UTF-8").replace("+", "%20")}")
                if (categoryId != null) append("&categoryId=$categoryId")
            }
    }
    object GoodsDetail : Screen("goods_detail/{goodsId}") {
        fun createRoute(goodsId: Long) = "goods_detail/$goodsId"
    }
    object PublishGoods : Screen("publish_goods")
    object EditGoods : Screen("edit_goods/{goodsId}") {
        fun createRoute(goodsId: Long) = "edit_goods/$goodsId"
    }
    object MyGoods : Screen("my_goods")
    object EditProfile : Screen("edit_profile")
    object MyOrders : Screen("my_orders")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: Long) = "order_detail/$orderId"
    }
    object CreateOrder : Screen("create_order/{goodsId}") {
        fun createRoute(goodsId: Long) = "create_order/$goodsId"
    }
    /**
     * 聊天页。
     *
     * goodsId 让「从商品详情发起咨询」带上商品上下文，卖家才知道问的是哪件商品；
     * conversationId 是会话的权威标识，从消息列表进入时携带，服务端据此把消息
     * 落到确定的会话，不会在同一对用户有多个商品会话时投递错。
     * 两者都缺省为 -1，表示「未知」。
     */
    object Chat : Screen("chat/{userId}/{userName}?goodsId={goodsId}&conversationId={conversationId}") {
        fun createRoute(
            userId: Long,
            userName: String,
            goodsId: Long? = null,
            conversationId: Long? = null
        ) = buildString {
            append("chat/$userId/${java.net.URLEncoder.encode(userName, "UTF-8").replace("+", "%20")}")
            append("?goodsId=${goodsId ?: -1L}")
            append("&conversationId=${conversationId ?: -1L}")
        }
    }
    object Notification : Screen("notification")
    object Favorite : Screen("favorite")
    object History : Screen("history")
    object Settings : Screen("settings")
    object ThemeSettings : Screen("theme_settings")
    object Verify : Screen("verify")
    object AddressList : Screen("address_list")
    object EditAddress : Screen("edit_address/{addressId}") {
        fun createRoute(addressId: Long = -1L) = "edit_address/$addressId"
    }
    object Evaluate : Screen("evaluate/{orderId}") {
        fun createRoute(orderId: Long) = "evaluate/$orderId"
    }
    object Report : Screen("report/{goodsId}") {
        fun createRoute(goodsId: Long) = "report/$goodsId"
    }
    object ChangePassword : Screen("change_password")
    object ChangePhone : Screen("change_phone")
    object Feedback : Screen("feedback")
    object About : Screen("about")

    /** 性能监控面板（开发者工具），入口在设置页。 */
    object ApmDashboard : Screen("apm_dashboard")
}
