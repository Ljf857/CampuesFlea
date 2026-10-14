package com.example.campusflea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.campusflea.apm.Apm
import com.example.campusflea.ui.navigation.AppNavGraph
import com.example.campusflea.ui.theme.CampusFleaTheme
import com.example.campusflea.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themePrefs = ThemePreferences(applicationContext)

        // 冷启动终点：decorView.post 的回调排在首次布局绘制之后执行，
        // 此时屏幕上已经出现内容，对应的是用户真正感知到的"启动完成"。
        // 用 onResume 当终点会偏早，界面对用户尚不可见。
        window.decorView.post { Apm.markFirstFrame() }

        setContent {
            val themeMode by themePrefs.themeMode.collectAsState(initial = 0)
            val isDark = when (themeMode) {
                ThemePreferences.MODE_LIGHT -> false
                ThemePreferences.MODE_DARK -> true
                else -> isSystemInDarkTheme()
            }
            CampusFleaTheme(darkTheme = isDark) {
                AppNavGraph()
            }
        }
    }
}
