package com.example.campusflea.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.theme.LocalIsDarkTheme
import com.example.campusflea.ui.util.CategoryIcons
import com.example.campusflea.ui.util.categoryTileColors
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(navController: NavHostController, viewModel: CategoryViewModel = viewModel()) {
    val categories by viewModel.categories.collectAsState()
    val selectedIndex by viewModel.selectedIndex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selected = categories.getOrNull(selectedIndex)
    val isDark = LocalIsDarkTheme.current

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("分类", fontWeight = FontWeight.SemiBold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { navController.navigate(Screen.Search.route) }) { Icon(Icons.Default.Search, "搜索") }
        }

        when {
            // 数据未到位时给出占位，而不是一片空白——空白会让人以为页面卡住了
            isLoading && categories.isEmpty() -> CategorySkeleton()

            error != null && categories.isEmpty() -> CategoryMessage(
                icon = { tint -> Icon(Icons.Default.WifiOff, null, tint = tint, modifier = Modifier.size(40.dp)) },
                title = "分类加载失败",
                description = error.orEmpty(),
                actionLabel = "重新加载",
                // 重试必须绕过缓存，否则拿到的是同一个失败结果
                onAction = { viewModel.loadCategories(forceRefresh = true) }
            )

            categories.isEmpty() -> CategoryMessage(
                icon = { tint -> Icon(Icons.Default.Refresh, null, tint = tint, modifier = Modifier.size(40.dp)) },
                title = "还没有商品分类",
                description = "稍后再试，或下拉刷新",
                actionLabel = "刷新",
                onAction = { viewModel.loadCategories(forceRefresh = true) }
            )

            else -> Row(Modifier.fillMaxSize()) {
                // 左侧一级分类
                LazyColumn(Modifier.width(100.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant)) {
                    itemsIndexed(categories) { index, cat ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectIndex(index) }
                                .background(
                                    if (index == selectedIndex) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(
                                        if (index == selectedIndex) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                            )
                            Box(Modifier.weight(1f).padding(vertical = 14.dp)) {
                                Text(
                                    cat.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 右侧二级分类
                LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    item {
                        Text(
                            selected?.name ?: "",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    val children = selected?.children ?: emptyList()
                    if (children.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "该分类下暂无子分类",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(children.chunked(2), key = { it.first().id }) { row ->
                            Row(
                                Modifier.animateItem(fadeInSpec = tween(200)).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                row.forEach { sub ->
                                    val tile = categoryTileColors(sub, isDark)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { navController.navigate(Screen.SearchResult.createRoute(categoryId = sub.id)) }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Box(
                                            Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(tile.background),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (sub.icon.startsWith("http")) {
                                                NetworkImage(model = sub.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                                            } else {
                                                Icon(
                                                    CategoryIcons.resolve(sub.icon),
                                                    contentDescription = null,
                                                    tint = tile.foreground,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            sub.name,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

/** 加载骨架：与真实布局同构，避免数据到位时内容跳动 */
@Composable
private fun CategorySkeleton() {
    Row(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.width(100.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant)) {
            items(7) {
                Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(width = 56.dp, height = 12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
        }
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
            Box(
                Modifier
                    .size(width = 96.dp, height = 16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.height(24.dp))
            repeat(3) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                Modifier
                                    .size(width = 48.dp, height = 10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun CategoryMessage(
    icon: @Composable (Color) -> Unit,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon(MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(actionLabel) }
        }
    }
}
