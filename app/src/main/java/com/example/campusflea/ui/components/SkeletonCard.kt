package com.example.campusflea.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0)),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

@Composable
private fun ShimmerPlaceholder(modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(4.dp)) {
    Box(modifier = modifier.background(shimmerBrush(), shape))
}

@Composable
fun SkeletonCard() {
    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            ShimmerPlaceholder(Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            Column(Modifier.padding(8.dp)) {
                ShimmerPlaceholder(Modifier.fillMaxWidth().height(14.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerPlaceholder(Modifier.width(80.dp).height(16.dp))
                Spacer(Modifier.height(4.dp))
                ShimmerPlaceholder(Modifier.width(100.dp).height(12.dp))
            }
        }
    }
}

@Composable
fun SkeletonBanner() {
    ShimmerPlaceholder(
        Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(horizontal = 16.dp),
        RoundedCornerShape(8.dp)
    )
}

@Composable
fun SkeletonCategory() {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(4) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShimmerPlaceholder(Modifier.size(44.dp), RoundedCornerShape(8.dp))
                    Spacer(Modifier.height(4.dp))
                    ShimmerPlaceholder(Modifier.width(40.dp).height(12.dp))
                }
            }
        }
    }
}
