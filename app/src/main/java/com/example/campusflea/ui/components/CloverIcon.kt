package com.example.campusflea.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CloverIcon(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "CampusFlea", modifier = modifier, tint = tint)
}

@Preview(showBackground = true, widthDp = 175, heightDp = 175)
@Composable
private fun CloverIconPreview() { MaterialTheme { CloverIcon(modifier = Modifier.size(175.dp)) } }
