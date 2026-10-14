package com.example.campusflea.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.campusflea.ui.theme.ThemePreferences
import kotlinx.coroutines.launch
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(navController: NavHostController) {
    val ctx = LocalContext.current
    val themePrefs = remember { ThemePreferences(ctx) }
    val themeMode by themePrefs.themeMode.collectAsState(initial = 0)
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        GlassTopBar(title = { Text("主题模式") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        listOf(
            Triple(Icons.Default.PhoneAndroid, "跟随系统", "根据系统设置自动切换") to ThemePreferences.MODE_SYSTEM,
            Triple(Icons.Default.LightMode, "明亮模式", "始终使用浅色主题") to ThemePreferences.MODE_LIGHT,
            Triple(Icons.Default.DarkMode, "深色模式", "始终使用深色主题") to ThemePreferences.MODE_DARK
        ).forEach { (triple, value) ->
            val (icon, title, desc) = triple
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { scope.launch { themePrefs.setThemeMode(value) } }, shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                    RadioButton(selected = themeMode == value, onClick = { scope.launch { themePrefs.setThemeMode(value) } })
                }
            }
        }
    }
}
