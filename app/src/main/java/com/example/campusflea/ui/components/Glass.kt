package com.example.campusflea.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 毛玻璃底色：由主题 surface 派生，深色模式自动适配。 */
@Composable
fun glassTint(alpha: Float = 0.72f): Color = MaterialTheme.colorScheme.surface.copy(alpha = alpha)

/** 毛玻璃描边色：由主题 outlineVariant 派生。 */
@Composable
fun glassBorder(alpha: Float = 0.45f): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)

/** 栏底/栏顶渐变投影：从 scrim 渐变到透明，营造立体悬浮感。深色模式下提升透明度保证可见。 */
@Composable
fun glassShadowBrush(fromAlpha: Float = 0.10f): Brush {
    val a = if (isSystemInDarkTheme()) fromAlpha + 0.12f else fromAlpha
    return Brush.verticalGradient(
        colors = listOf(MaterialTheme.colorScheme.scrim.copy(alpha = a), Color.Transparent)
    )
}

/** 底栏上方的渐变投影（自上而下：透明 → scrim）。 */
@Composable
fun glassShadowBrushUp(fromAlpha: Float = 0.10f): Brush {
    val a = if (isSystemInDarkTheme()) fromAlpha + 0.12f else fromAlpha
    return Brush.verticalGradient(
        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.scrim.copy(alpha = a))
    )
}

/** 玻璃表面：半透明底色 + 细描边。 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    tint: Color = glassTint(),
    borderColor: Color = glassBorder(),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = tint,
        border = BorderStroke(1.dp, borderColor)
    ) { content() }
}

/**
 * 毛玻璃顶栏：背景延伸至状态栏之后（edge-to-edge），内容经 statusBarsPadding 落在状态栏下方，
 * 栏底带 1dp 分隔线与渐变投影。API 镜像 Material3 TopAppBar。
 */
@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    tint: Color = glassTint(),
    borderColor: Color = glassBorder()
) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().background(tint)) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (navigationIcon != null) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { navigationIcon() }
                }
                Box(Modifier.weight(1f)) { title() }
                Row(verticalAlignment = Alignment.CenterVertically) { actions() }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(borderColor))
        Box(Modifier.fillMaxWidth().height(10.dp).background(glassShadowBrush()))
    }
}

/**
 * 玻璃卡片：半透明容器 + 描边 + 柔和双层投影，适用于商品卡、信息卡等需要立体感的位置。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    tint: Color = glassTint(0.68f),
    borderColor: Color = glassBorder(),
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // 按压反馈：按下轻微缩小、松开弹性回弹，保留涟漪指示
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glassCardScale"
    )
    val base = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = true,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.12f)
        )
        .background(tint, shape)
        .border(1.dp, borderColor, shape)
    if (onClick != null) {
        Box(base.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onClick
        )) { content() }
    } else {
        Box(base) { content() }
    }
}
