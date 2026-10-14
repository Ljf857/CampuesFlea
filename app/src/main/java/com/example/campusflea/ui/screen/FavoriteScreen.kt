package com.example.campusflea.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.FavoriteViewModel
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoriteScreen(navController: NavHostController, viewModel: FavoriteViewModel = viewModel()) {
    val favorites by viewModel.favorites.collectAsState()
    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text("我的收藏") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 160.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(favorites, key = { it.id }) { GoodsCard(Modifier.animateItem(fadeInSpec = tween(200)), it) { navController.navigate(Screen.GoodsDetail.createRoute(it.id)) } } }
    }
}
