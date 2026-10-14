package com.example.campusflea.ui.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        entered = true
        delay(2000)
        navController.navigate(Screen.Main.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }
    // Logo 弹性放大 + 标题/副标题淡入，与 2s 导航并行
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "splashLogo"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "splashText"
    )
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(80.dp).graphicsLayer { scaleX = logoScale; scaleY = logoScale }.background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), shape = MaterialTheme.shapes.large), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("校园跳蚤", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.graphicsLayer { alpha = textAlpha })
            Text("让闲置流转起来", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f), modifier = Modifier.graphicsLayer { alpha = textAlpha })
        }
        Text("v1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp))
    }
}
