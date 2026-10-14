package com.example.campusflea.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.example.campusflea.data.model.CategoryInfo
import com.example.campusflea.ui.theme.OnColorLight

/**
 * 分类色块的一对颜色：底色与压在底色上的前景色。
 */
data class CategoryTileColors(
    val background: Color,
    val foreground: Color
)

/**
 * 色块底色的相对亮度上限。
 *
 * 白色图标要在色块上满足非文字图形 3:1 的最低对比度，底色亮度必须 ≤ 0.30
 * （1.05 / (0.30 + 0.05) = 3.0）。超过这个值的颜色会被自动压暗。
 */
private const val MAX_TILE_LUMINANCE = 0.30f

/**
 * 解析分类色块的前景 / 背景色。
 *
 * ### 为什么图标恒为白色
 *
 * 早先的实现是按底色亮度在白 / 黑之间二选一，以保证对比度。这在数学上没错，
 * 但视觉上出了问题：后台的预设色板里有 `#90CAF9`、`#A5D6A7` 这类浅色，
 * 它们的亮度超过 0.5，于是同一屏里大部分分类是白图标、少数几个是黑图标，
 * 看起来像渲染错误而不是有意为之。
 *
 * 现在改为「恒定白图标 + 保证底色足够深」，两个目标同时满足：
 * 观感一致，对比度也达标。后台选到过浅的颜色时会被压暗，
 * 管理端的预览显示的就是压暗之后的结果，不会出现「后台看到的和 App 不一样」。
 *
 * ### 深色模式
 *
 * 优先用后台配置的 `colorDark`；没配则按浅色值推导（降饱和、略压暗），
 * 这样存量分类不必回填数据，功能立刻可用。
 * 无论走哪条路径，底色同样受 [MAX_TILE_LUMINANCE] 约束。
 */
@Composable
@ReadOnlyComposable
fun categoryTileColors(category: CategoryInfo, isDark: Boolean): CategoryTileColors {
    val scheme = MaterialTheme.colorScheme
    val light = parseHex(category.color)
    // 后台未配置颜色：退回主题容器色。直接给灰色兜底会让整个分类页变成一片灰
    if (light == null) {
        return CategoryTileColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
    }

    val raw = if (isDark) parseHex(category.colorDark) ?: deriveDarkVariant(light) else light
    return CategoryTileColors(
        background = clampToMaxLuminance(raw, MAX_TILE_LUMINANCE),
        foreground = OnColorLight
    )
}

private fun parseHex(hex: String): Color? {
    val cleaned = hex.removePrefix("#")
    if (cleaned.length != 6) return null
    return try {
        Color(cleaned.toLong(16) or 0xFF000000L)
    } catch (_: Exception) {
        null
    }
}

/**
 * 深色模式下的默认推导：同色相，降饱和并略压暗。
 *
 * 目的不是换一个颜色，而是让色块在近黑背景上不刺眼——
 * 高饱和的玫红、橙红在深色背景上会显得格外跳。
 * 后台可以配置 `colorDark` 覆盖这个结果。
 */
private fun deriveDarkVariant(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = (hsl[1] * 0.8f).coerceIn(0f, 1f)
    hsl[2] = (hsl[2] * 0.9f).coerceIn(0f, 1f)
    return Color(ColorUtils.HSLToColor(hsl))
}

/**
 * 把颜色压暗到亮度不超过 [maxLuminance]，保持色相与饱和度。
 *
 * 在 HSL 明度上做二分查找而不是简单地乘一个系数：
 * 不同色相对明度的敏感度差别很大（蓝色和黄色同样的 HSL 明度，实际亮度差好几倍），
 * 固定系数会有的压不够、有的压过头。二分 12 次精度已足够（明度步进 < 0.0003）。
 */
private fun clampToMaxLuminance(color: Color, maxLuminance: Float): Color {
    if (color.luminance() <= maxLuminance) return color

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    val hue = hsl[0]
    val saturation = hsl[1]

    var low = 0f
    var high = hsl[2]
    repeat(12) {
        val mid = (low + high) / 2f
        val candidate = Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, mid)))
        if (candidate.luminance() > maxLuminance) high = mid else low = mid
    }
    return Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, low)))
}
