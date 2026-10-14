package com.example.campusflea.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * 按压反馈：按下轻微缩小、松开弹性回弹，并保留 Material 涟漪指示。
 * 用于替换 Modifier.clickable(role = Role.Button, onClick = ...) 的调用点。
 */
@Composable
fun Modifier.pressScale(
    onClick: () -> Unit,
    role: Role? = null,
    pressedScale: Float = 0.96f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScale"
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            role = role,
            onClick = onClick
        )
}
