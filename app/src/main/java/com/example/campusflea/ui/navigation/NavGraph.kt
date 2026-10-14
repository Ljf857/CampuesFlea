package com.example.campusflea.ui.navigation

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.campusflea.CampusFleaApp
import com.example.campusflea.data.local.TokenManager
import com.example.campusflea.ui.components.glassShadowBrushUp
import com.example.campusflea.ui.components.pressScale
import com.example.campusflea.ui.screen.*
import kotlinx.coroutines.launch

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "首页", Icons.Default.Home),
    BottomNavItem(Screen.Category.route, "分类", Icons.Default.Apps),
    BottomNavItem(Screen.Message.route, "消息", Icons.Default.Chat),
    BottomNavItem(Screen.Mine.route, "我的", Icons.Default.Person)
)

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    // 进入主界面时请求通知权限（Android 13+），用于后台新消息提醒
    val ctx = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomNavItems.map { it.route }) {
                // 玻璃底栏：顶部渐变投影 + 半透明表面
                Column(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(8.dp).background(glassShadowBrushUp()))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)))
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        tonalElevation = 0.dp
                    ) {
                        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                            label = { Text("首页") },
                            selected = currentRoute == Screen.Home.route,
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentRoute != Screen.Home.route) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Home.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Apps, contentDescription = "分类") },
                            label = { Text("分类") },
                            selected = currentRoute == Screen.Category.route,
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentRoute != Screen.Category.route) {
                                    navController.navigate(Screen.Category.route) {
                                        popUpTo(Screen.Home.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).pressScale(
                                    onClick = {
                                        scope.launch {
                                            val isLoggedIn = TokenManager.getInstance(CampusFleaApp.instance).isLoggedIn()
                                            if (isLoggedIn) navController.navigate(Screen.PublishGoods.route)
                                            else rootNavController.navigate(Screen.Login.route)
                                        }
                                    }
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, "发布", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Chat, contentDescription = "消息") },
                            label = { Text("消息") },
                            selected = currentRoute == Screen.Message.route,
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentRoute != Screen.Message.route) {
                                    navController.navigate(Screen.Message.route) {
                                        popUpTo(Screen.Home.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, contentDescription = "我的") },
                            label = { Text("我的") },
                            selected = currentRoute == Screen.Mine.route,
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (currentRoute != Screen.Mine.route) {
                                    navController.navigate(Screen.Mine.route) {
                                        popUpTo(Screen.Home.route)
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
                }
            }
        },
        floatingActionButton = {}
    ) { innerPadding ->
        // 顶部保持 edge-to-edge：子界面自行 statusBarsPadding（玻璃顶栏延伸到状态栏之后），
        // 底部仍由 innerPadding 清空底部栏/导航栏
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            // 页面切换：统一淡入淡出（tab 切换与子页面进入共用，避免横向滑动错位）
            enterTransition = { fadeIn(animationSpec = tween(durationMillis = 220)) },
            exitTransition = { fadeOut(animationSpec = tween(durationMillis = 180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 220)) },
            popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 180)) }
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Category.route) { CategoryScreen(navController) }
            composable(Screen.Message.route) { MessageScreen(navController) }
            composable(Screen.Mine.route) { MineScreen(navController, rootNavController) }
            composable(Screen.Search.route) { SearchScreen(navController) }
            composable(Screen.SearchResult.route, arguments = listOf(navArgument("keyword") { type = NavType.StringType; defaultValue = "" }, navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null })) {
                SearchResultScreen(navController, it.arguments?.getString("keyword")?.ifBlank { null }, it.arguments?.getString("categoryId")?.toLongOrNull())
            }
            composable(Screen.GoodsDetail.route, arguments = listOf(navArgument("goodsId") { type = NavType.LongType })) {
                GoodsDetailScreen(navController, it.arguments?.getLong("goodsId") ?: 0L)
            }
            composable(Screen.PublishGoods.route) { PublishGoodsScreen(navController) }
            composable(Screen.EditGoods.route, arguments = listOf(navArgument("goodsId") { type = NavType.LongType })) {
                PublishGoodsScreen(navController, goodsId = it.arguments?.getLong("goodsId"))
            }
            composable(Screen.MyGoods.route) { MyGoodsScreen(navController) }
            composable(Screen.EditProfile.route) { EditProfileScreen(navController) }
            composable(Screen.MyOrders.route) { MyOrdersScreen(navController) }
            composable(Screen.OrderDetail.route, arguments = listOf(navArgument("orderId") { type = NavType.LongType })) {
                OrderDetailScreen(navController, it.arguments?.getLong("orderId") ?: 0L)
            }
            composable(Screen.CreateOrder.route, arguments = listOf(navArgument("goodsId") { type = NavType.LongType })) {
                CreateOrderScreen(navController, it.arguments?.getLong("goodsId") ?: 0L)
            }
            composable(Screen.Chat.route, arguments = listOf(
                navArgument("userId") { type = NavType.LongType },
                navArgument("userName") { type = NavType.StringType },
                navArgument("goodsId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("conversationId") { type = NavType.LongType; defaultValue = -1L }
            )) {
                ChatScreen(
                    navController = navController,
                    userId = it.arguments?.getLong("userId") ?: 0L,
                    userName = it.arguments?.getString("userName") ?: "",
                    goodsId = it.arguments?.getLong("goodsId")?.takeIf { id -> id > 0 },
                    conversationId = it.arguments?.getLong("conversationId")?.takeIf { id -> id > 0 }
                )
            }
            composable(Screen.Notification.route) { NotificationScreen(navController) }
            composable(Screen.Favorite.route) { FavoriteScreen(navController) }
            composable(Screen.History.route) { HistoryScreen(navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController) }
            composable(Screen.ThemeSettings.route) { ThemeSettingsScreen(navController) }
            composable(Screen.Verify.route) { VerifyScreen(navController) }
            composable(Screen.AddressList.route) { AddressListScreen(navController) }
            composable(Screen.EditAddress.route, arguments = listOf(navArgument("addressId") { type = NavType.LongType })) {
                EditAddressScreen(navController, it.arguments?.getLong("addressId") ?: -1L)
            }
            composable(Screen.Evaluate.route, arguments = listOf(navArgument("orderId") { type = NavType.LongType })) {
                EvaluateScreen(navController, it.arguments?.getLong("orderId") ?: 0L)
            }
            composable(Screen.Report.route, arguments = listOf(navArgument("goodsId") { type = NavType.LongType })) {
                ReportScreen(navController, it.arguments?.getLong("goodsId") ?: 0L)
            }
            composable(Screen.ChangePassword.route) { ChangePasswordScreen(navController) }
            composable(Screen.ChangePhone.route) { ChangePhoneScreen(navController) }
            composable(Screen.Feedback.route) { FeedbackScreen(navController) }
            composable(Screen.About.route) { AboutScreen(navController) }
            composable(Screen.ApmDashboard.route) { ApmDashboardScreen(navController) }
        }
    }
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    // 开屏页全屏沉浸：仅在 Splash 路由时隐藏系统状态栏/导航栏，进入主界面或登录流时恢复
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val view = LocalView.current
    val activity = view.context.findActivity()
    LaunchedEffect(currentRoute) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        if (controller != null) {
            if (currentRoute == Screen.Splash.route) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                // 全屏后通过边缘滑动可临时唤出系统栏
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    // 登录态失效：无论当前停在哪个页面，都清空回退栈并回到登录页。
    // 不处理的话用户会停留在一个所有请求都静默 401 的界面上，无从判断发生了什么。
    LaunchedEffect(Unit) {
        CampusFleaApp.sessionExpired.collect {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        // 登录流/启动页：轻微上移 + 淡入
        enterTransition = { fadeIn(animationSpec = tween(durationMillis = 250)) + slideInVertically(initialOffsetY = { it / 12 }) },
        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 250)) },
        popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 200)) }
    ) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.SmsLogin.route) { SmsLoginScreen(navController) }
        composable(Screen.ResetPassword.route) { ResetPasswordScreen(navController) }
        composable(Screen.Main.route) { MainScreen(rootNavController = navController) }
    }
}

// 从任意 Context/ContextWrapper 递归回溯到 Activity，用于获取 Window 控制全屏
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
