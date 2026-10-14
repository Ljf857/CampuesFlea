package com.example.campusflea.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColorScheme = lightColorScheme(
    primary = Primary, onPrimary = OnPrimary, primaryContainer = PrimaryContainer, onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary, secondaryContainer = SecondaryContainer, onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary, onTertiary = OnTertiary, tertiaryContainer = TertiaryContainer, onTertiaryContainer = OnTertiaryContainer,
    background = Background, onBackground = OnBackground,
    surface = Surface, onSurface = OnSurface, surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant,
    outline = Outline, outlineVariant = OutlineVariant,
    error = Error, onError = OnError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary, onPrimary = DarkOnPrimary, primaryContainer = DarkPrimaryContainer, onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary, secondaryContainer = DarkSecondaryContainer, onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary, onTertiary = DarkOnTertiary, tertiaryContainer = DarkTertiaryContainer, onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface, surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline, outlineVariant = DarkOutlineVariant,
    error = DarkError, onError = DarkOnError
)

/**
 * 当前是否处于深色主题。
 *
 * 不能改用 isSystemInDarkTheme()：本应用支持在设置里强制主题，与系统偏好可能不一致。
 * 需要按明暗切换「数据驱动颜色」的场景（如后台下发的分类主题色）应读这个值。
 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun CampusFleaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = Shapes, content = content)
    }
}
