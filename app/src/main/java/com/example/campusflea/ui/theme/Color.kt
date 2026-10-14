package com.example.campusflea.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF0077FF)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFD8E4FF)
val OnPrimaryContainer = Color(0xFF002266)
val Secondary = Color(0xFF565F71)
val SecondaryContainer = Color(0xFFDAE2F9)
val OnSecondaryContainer = Color(0xFF131C2B)
val Tertiary = Color(0xFF7C5800)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFDEA1)
val OnTertiaryContainer = Color(0xFF271900)
val Background = Color(0xFFF8F9FC)
val OnBackground = Color(0xFF1A1C1E)
val Surface = Color(0xFFFDFBFF)
val OnSurface = Color(0xFF1A1C1E)
val SurfaceVariant = Color(0xFFE0E2EC)
val OnSurfaceVariant = Color(0xFF44474E)
val SurfaceContainerLow = Color(0xFFF0F1F5)
val SurfaceContainerHigh = Color(0xFFECEEF2)
val Outline = Color(0xFF74777F)
val OutlineVariant = Color(0xFFC4C6CF)
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val PriceColor = Color(0xFFBA1A1A)
val StarColor = Color(0xFFFF9800)
val SuccessColor = Color(0xFF2E7D32)
val WarningColor = Color(0xFFED6C02)
val DividerColor = Color(0xFFE0E2EC)

val DarkPrimary = Color(0xFFADC8FF)
val DarkOnPrimary = Color(0xFF003A9E)
val DarkPrimaryContainer = Color(0xFF0052C2)
val DarkOnPrimaryContainer = Color(0xFFD8E4FF)
val DarkSecondary = Color(0xFFBEC6DC)
val DarkSecondaryContainer = Color(0xFF3E4759)
val DarkOnSecondaryContainer = Color(0xFFDAE2F9)
val DarkTertiary = Color(0xFFEAC28C)
val DarkOnTertiary = Color(0xFF402D00)
val DarkTertiaryContainer = Color(0xFF5E4200)
val DarkOnTertiaryContainer = Color(0xFFFFDEA1)
val DarkBackground = Color(0xFF1A1C1E)
val DarkOnBackground = Color(0xFFE2E2E6)
val DarkSurface = Color(0xFF1A1C1E)
val DarkOnSurface = Color(0xFFE2E2E6)
val DarkSurfaceVariant = Color(0xFF44474E)
val DarkOnSurfaceVariant = Color(0xFFC4C6CF)
val DarkSurfaceContainerLow = Color(0xFF232528)
val DarkSurfaceContainerHigh = Color(0xFF2A2C2F)
val DarkOutline = Color(0xFF8E9099)
val DarkOutlineVariant = Color(0xFF44474E)
val DarkError = Color(0xFFF2B8B5)
val DarkOnError = Color(0xFF601410)
val DarkPriceColor = Color(0xFFF2B8B5)
val DarkStarColor = Color(0xFFFF9800)
val DarkDividerColor = Color(0xFF44474E)

/**
 * 数据驱动色块上的前景色。
 *
 * 分类主题色由后台下发、取值任意，图标颜色无法预先写死；只能按背景的相对亮度在
 * 深浅两极之间二选一，才能对任意下发色都保证可读。放在主题里而不是就地写
 * Color.White / Color.Black，是让这两个值受控、可统一调整。
 */
val OnColorLight = Color(0xFFFFFFFF)
val OnColorDark = Color(0xFF1A1C1E)