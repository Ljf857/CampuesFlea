package com.example.campusflea.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.campusflea.ui.components.GlassTopBar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchResultScreen(navController: NavHostController, keyword: String? = null, categoryId: Long? = null, viewModel: SearchViewModel = viewModel()) {
    val results by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val gridState = rememberLazyGridState()
    LaunchedEffect(keyword, categoryId) { viewModel.search(keyword, categoryId) }

    // 滚动到底部附近时加载下一页
    LaunchedEffect(gridState, hasMore, isLoading) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastIndex ->
                val total = results.size
                if (lastIndex != null && hasMore && !isLoading && total > 0 && lastIndex >= total - 3) {
                    viewModel.loadMore()
                }
            }
    }

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = { Text(keyword ?: "分类筛选") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } })
        Text("为您找到 ${results.size} 件相关商品", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall)
        when {
            isLoading && results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error ?: "没有找到相关商品", color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { GoodsCard(Modifier.animateItem(fadeInSpec = tween(200)), it) { navController.navigate(Screen.GoodsDetail.createRoute(it.id)) } }
                if (hasMore) item {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
                }
            }
        }
    }
}
