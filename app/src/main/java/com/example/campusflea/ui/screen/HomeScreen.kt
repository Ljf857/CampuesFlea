package com.example.campusflea.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.data.model.BannerInfo
import com.example.campusflea.data.model.CategoryInfo
import com.example.campusflea.data.model.GoodsInfo
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.theme.CampusFleaTheme
import com.example.campusflea.ui.theme.LocalIsDarkTheme
import com.example.campusflea.ui.util.CategoryIcons
import com.example.campusflea.ui.util.categoryTileColors
import com.example.campusflea.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// 设计令牌
// 间距：8dp 网格（页面左右 16dp / 区块间距 24dp / 容器内边距 16dp / 元素间距 8dp）
//       全部间距均为 8 的倍数，无 4dp 半步
// 圆角：卡片 16dp、按钮 24dp、Chip 8dp
// 阴影：卡片阴影统一 1dp
// 颜色：全部取自 MaterialTheme.colorScheme，不出现任何字面量颜色
// ---------------------------------------------------------------------------
private object HomeTokens {
    val PageHorizontal: Dp = 16.dp
    val SectionGap: Dp = 24.dp
    val ItemGap: Dp = 8.dp
    val ContainerInset: Dp = 16.dp
    val CardRadius: Dp = 16.dp
    val ButtonRadius: Dp = 24.dp
    val ChipRadius: Dp = 8.dp
    val TileRadius: Dp = 16.dp
    val IconTile: Dp = 48.dp
    val TouchTarget: Dp = 48.dp
    val GridMinCell: Dp = 160.dp
    val BannerAspect: Float = 16f / 9f
}

/**
 * 首页 UI 状态。
 * 由 ViewModel 的若干 StateFlow 汇总而成，仅用于驱动界面，不含任何业务逻辑。
 *
 * 状态判定优先级（与 HomeViewModel 的赋值顺序一致）：
 * 1. [isLoading] 且列表为空        -> 加载中
 * 2. [error] 非空 且列表为空        -> 加载失败（注意 ViewModel 在失败时也会把 isEmpty 置为 true，
 *                                     因此必须优先判断 error）
 * 3. [isEmpty] 且列表为空           -> 空数据
 * 4. 列表非空                       -> 成功（[isRefreshing] 叠加下拉刷新指示器）
 */
data class HomeUiState(
    val goods: List<GoodsInfo> = emptyList(),
    val categories: List<CategoryInfo> = emptyList(),
    val banners: List<BannerInfo> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val hasUnread: Boolean = false,
    val isPersonalized: Boolean = false,
    val error: String? = null,
    val isEmpty: Boolean = false
) {
    val showLoading: Boolean get() = isLoading && goods.isEmpty() && error == null
    val showError: Boolean get() = error != null && goods.isEmpty()
    val showEmpty: Boolean get() = !showLoading && !showError && goods.isEmpty()
    val showContent: Boolean get() = goods.isNotEmpty()
    /** 首屏之外的翻页加载（列表已有内容时的 loading）。 */
    val showLoadMore: Boolean get() = isLoading && goods.isNotEmpty()
}

// ---------------------------------------------------------------------------
// 有状态入口：只负责订阅 ViewModel 与转发导航，不含业务逻辑
// ---------------------------------------------------------------------------
@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val goods by viewModel.goods.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val hasUnread by viewModel.hasUnread.collectAsStateWithLifecycle()
    val isPersonalized by viewModel.isPersonalized.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isEmpty by viewModel.isEmpty.collectAsStateWithLifecycle()

    // 回到前台时重新拉取未读数（repeatOnLifecycle 会在离开 RESUMED 时自动挂起）
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.checkUnread()
        }
    }

    HomeContent(
        state = HomeUiState(
            goods = goods,
            categories = categories,
            banners = banners,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            hasMore = hasMore,
            hasUnread = hasUnread,
            isPersonalized = isPersonalized,
            error = error,
            isEmpty = isEmpty
        ),
        onSearchClick = { navController.navigate(Screen.Search.route) },
        onMessageClick = {
            navController.navigate(Screen.Message.route) {
                popUpTo(Screen.Home.route)
                launchSingleTop = true
            }
        },
        onCategoryClick = { categoryId ->
            navController.navigate(Screen.SearchResult.createRoute(categoryId = categoryId))
        },
        onGoodsClick = { goodsId ->
            navController.navigate(Screen.GoodsDetail.createRoute(goodsId))
        },
        onMoreClick = { navController.navigate(Screen.SearchResult.createRoute("")) },
        onBrowseCategory = {
            navController.navigate(Screen.Category.route) {
                popUpTo(Screen.Home.route)
                launchSingleTop = true
            }
        },
        onRetry = viewModel::loadRecommend,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadMore
    )
}

// ---------------------------------------------------------------------------
// 无状态内容：可预览、可测试
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onSearchClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onCategoryClick: (Long) -> Unit = {},
    onGoodsClick: (Long) -> Unit = {},
    onMoreClick: () -> Unit = {},
    onBrowseCategory: () -> Unit = {},
    onRetry: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶栏固定：仅顶部消费状态栏 inset，底部由 MainScreen 的 innerPadding 统一处理
        HomeTopBar(
            hasUnread = state.hasUnread,
            onSearchClick = onSearchClick,
            onMessageClick = onMessageClick
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.showLoading -> HomeLoadingSkeleton()
                state.showError -> HomeMessageState(
                    icon = { tint -> Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp)) },
                    title = "加载失败",
                    description = state.error.orEmpty(),
                    primaryLabel = "重新加载",
                    onPrimary = onRetry
                )
                state.showEmpty -> HomeMessageState(
                    icon = { tint -> Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = tint, modifier = Modifier.size(48.dp)) },
                    title = "暂无推荐商品",
                    description = "下拉刷新试试，或去分类里逛逛",
                    primaryLabel = "浏览分类",
                    onPrimary = onBrowseCategory,
                    secondaryLabel = "刷新",
                    onSecondary = onRefresh
                )
                else -> HomeGoodsGrid(
                    state = state,
                    onGoodsClick = onGoodsClick,
                    onCategoryClick = onCategoryClick,
                    onMoreClick = onMoreClick,
                    onLoadMore = onLoadMore
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 顶栏
// ---------------------------------------------------------------------------
@Composable
private fun HomeTopBar(
    hasUnread: Boolean,
    onSearchClick: () -> Unit,
    onMessageClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = HomeTokens.PageHorizontal, vertical = HomeTokens.ItemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "校园跳蚤",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(HomeTokens.ItemGap))
        SearchEntry(
            onClick = onSearchClick,
            modifier = Modifier.weight(1f)
        )
        // IconButton 自身已保证 48dp 触控区域
        Box(modifier = Modifier.size(HomeTokens.TouchTarget)) {
            IconButton(
                onClick = onMessageClick,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MailOutline,
                    contentDescription = if (hasUnread) "消息，有未读消息" else "消息",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = HomeTokens.ItemGap, end = HomeTokens.ItemGap)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(HomeTokens.TouchTarget),
        shape = RoundedCornerShape(HomeTokens.ButtonRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HomeTokens.ContainerInset),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(HomeTokens.ItemGap))
            Text(
                text = "搜索闲置物品",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 成功态：网格内容
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun HomeGoodsGrid(
    state: HomeUiState,
    onGoodsClick: (Long) -> Unit,
    onCategoryClick: (Long) -> Unit,
    onMoreClick: () -> Unit,
    onLoadMore: () -> Unit
) {
    val gridState = rememberLazyGridState()

    // 触底加载：接近末尾时请求下一页，请求去重由 ViewModel 内部的守卫负责
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= total - 2
        }
    }
    LaunchedEffect(shouldLoadMore, state.hasMore, state.goods.size) {
        if (shouldLoadMore && state.hasMore) onLoadMore()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = HomeTokens.GridMinCell),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HomeTokens.PageHorizontal,
            end = HomeTokens.PageHorizontal,
            bottom = HomeTokens.SectionGap
        ),
        verticalArrangement = Arrangement.spacedBy(HomeTokens.ItemGap),
        horizontalArrangement = Arrangement.spacedBy(HomeTokens.ItemGap)
    ) {
        // 区块间距 24dp = 网格行距 8dp + 区块自身 16dp 下边距
        item(key = "banner", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.padding(bottom = 16.dp)) {
                HomeBannerCarousel(banners = state.banners)
            }
        }

        if (state.categories.isNotEmpty()) {
            item(key = "categories", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(bottom = 16.dp)) {
                    CategorySection(
                        categories = state.categories,
                        onCategoryClick = onCategoryClick
                    )
                }
            }
        }

        item(key = "section_header", span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = if (state.isPersonalized) "为你推荐" else "热门推荐",
                onMoreClick = onMoreClick,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(items = state.goods, key = { it.id }) { goods ->
            GoodsCard(
                modifier = Modifier.animateItem(fadeInSpec = tween(200)),
                goods = goods,
                onClick = { onGoodsClick(goods.id) }
            )
        }

        if (state.showLoadMore) {
            item(key = "load_more", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HomeTokens.TouchTarget),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        if (!state.hasMore) {
            item(key = "end", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = HomeTokens.SectionGap),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "— 已经到底了 —",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Banner 轮播
// ---------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeBannerCarousel(banners: List<BannerInfo>) {
    val images = remember(banners) { banners.filter { it.image.isNotBlank() } }
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { images.size })
    val lifecycleOwner = LocalLifecycleOwner.current

    // 仅在 RESUMED 时自动轮播，避免应用退到后台后仍在唤醒主线程做动画
    LaunchedEffect(pagerState, images.size, lifecycleOwner) {
        if (images.size <= 1) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(3500)
                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % images.size)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HomeTokens.BannerAspect)
                .clip(RoundedCornerShape(HomeTokens.CardRadius))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                NetworkImage(
                    model = images[page].image,
                    contentDescription = images[page].title.ifBlank { null },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // 指示器放在 Banner 之外，避免被图片内容淹没
        Spacer(Modifier.height(HomeTokens.ItemGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(images.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = HomeTokens.ItemGap)
                        .size(width = if (selected) 16.dp else 8.dp, height = 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            // outlineVariant 叠在 background 上仅 1.6:1，未选中态几乎不可见；
                            // outline 为 4.3:1，满足非文字 UI 元素 3:1 的要求
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 分类
// ---------------------------------------------------------------------------
@Composable
private fun CategorySection(
    categories: List<CategoryInfo>,
    onCategoryClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(HomeTokens.CardRadius))
            .padding(vertical = HomeTokens.ContainerInset),
        verticalArrangement = Arrangement.spacedBy(HomeTokens.ItemGap)
    ) {
        categories.chunked(4).forEach { rowCategories ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowCategories.forEach { category ->
                    CategoryItem(
                        category = category,
                        onClick = { onCategoryClick(category.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 最后一行不足 4 个时用等宽占位，保证图标纵向对齐
                repeat(4 - rowCategories.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryItem(
    category: CategoryInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 主题色由后台下发，深浅模式各取一套；前景色按底色亮度自动选，保证任何下发色都可读
    val tile = categoryTileColors(category, LocalIsDarkTheme.current)

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(HomeTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = HomeTokens.ItemGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(HomeTokens.IconTile)
                    .clip(RoundedCornerShape(HomeTokens.TileRadius))
                    .background(tile.background),
                contentAlignment = Alignment.Center
            ) {
                if (category.icon.startsWith("http")) {
                    NetworkImage(
                        model = category.icon,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = CategoryIcons.resolve(category.icon),
                        contentDescription = null,
                        tint = tile.foreground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.height(HomeTokens.ItemGap))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 区块标题
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionHeader(
    title: String,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Surface(
            onClick = onMoreClick,
            shape = RoundedCornerShape(HomeTokens.ButtonRadius),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = HomeTokens.TouchTarget)
                    .padding(horizontal = HomeTokens.ItemGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "更多",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 商品卡（对外公开，SearchResultScreen / FavoriteScreen 复用，签名保持不变）
// ---------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodsCard(
    modifier: Modifier = Modifier,
    goods: GoodsInfo,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(HomeTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column {
            NetworkImage(
                model = goods.coverImage,
                contentDescription = null, // 标题已承载语义，图片作为装饰
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(HomeTokens.ContainerInset)) {
                Text(
                    text = goods.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(HomeTokens.ItemGap))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HomeTokens.ItemGap)
                ) {
                    Text(
                        text = "¥${goods.price}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                    if (goods.originalPrice > 0) {
                        Text(
                            text = "¥${goods.originalPrice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(HomeTokens.ItemGap))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HomeTokens.ItemGap)
                ) {
                    if (goods.conditionDesc.isNotEmpty()) {
                        ConditionChip(goods.conditionDesc)
                    }
                    if (goods.campus.isNotEmpty()) {
                        Text(
                            text = goods.campus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionChip(text: String) {
    Surface(
        shape = RoundedCornerShape(HomeTokens.ChipRadius),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = HomeTokens.ItemGap, vertical = HomeTokens.ItemGap)
        )
    }
}

// ---------------------------------------------------------------------------
// 空 / 错误态
// ---------------------------------------------------------------------------
@Composable
private fun HomeMessageState(
    icon: @Composable (Color) -> Unit,
    title: String,
    description: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    // 用 LazyColumn 承载，保证内容不足一屏时下拉刷新手势依然可用
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillParentMaxSize()
                    .padding(horizontal = HomeTokens.PageHorizontal),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    icon(MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(HomeTokens.SectionGap))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    if (description.isNotBlank()) {
                        Spacer(Modifier.height(HomeTokens.ItemGap))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(HomeTokens.SectionGap))
                    Button(
                        onClick = onPrimary,
                        shape = RoundedCornerShape(HomeTokens.ButtonRadius),
                        modifier = Modifier.heightIn(min = HomeTokens.TouchTarget)
                    ) {
                        Text(primaryLabel, style = MaterialTheme.typography.labelLarge)
                    }
                    if (secondaryLabel != null && onSecondary != null) {
                        Spacer(Modifier.height(HomeTokens.ItemGap))
                        OutlinedButton(
                            onClick = onSecondary,
                            shape = RoundedCornerShape(HomeTokens.ButtonRadius),
                            modifier = Modifier.heightIn(min = HomeTokens.TouchTarget)
                        ) {
                            Text(secondaryLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 加载态：骨架屏
// ---------------------------------------------------------------------------
@Composable
private fun HomeLoadingSkeleton() {
    val brush = rememberShimmerBrush()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = HomeTokens.GridMinCell),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = HomeTokens.PageHorizontal,
            end = HomeTokens.PageHorizontal,
            bottom = HomeTokens.SectionGap
        ),
        verticalArrangement = Arrangement.spacedBy(HomeTokens.ItemGap),
        horizontalArrangement = Arrangement.spacedBy(HomeTokens.ItemGap),
        userScrollEnabled = false
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 16.dp)) {
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(HomeTokens.BannerAspect),
                    shape = RoundedCornerShape(HomeTokens.CardRadius)
                )
                Spacer(Modifier.height(HomeTokens.ItemGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) {
                        SkeletonBlock(
                            brush = brush,
                            modifier = Modifier
                                .padding(horizontal = HomeTokens.ItemGap)
                                .size(width = 8.dp, height = 8.dp),
                            shape = CircleShape
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(HomeTokens.CardRadius))
                    .padding(vertical = HomeTokens.ContainerInset)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SkeletonBlock(
                            brush = brush,
                            modifier = Modifier.size(HomeTokens.IconTile),
                            shape = RoundedCornerShape(HomeTokens.TileRadius)
                        )
                        Spacer(Modifier.height(HomeTokens.ItemGap))
                        SkeletonBlock(
                            brush = brush,
                            modifier = Modifier.size(width = 40.dp, height = 12.dp),
                            shape = RoundedCornerShape(HomeTokens.ChipRadius)
                        )
                    }
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SkeletonBlock(
                brush = brush,
                modifier = Modifier
                    .size(width = 96.dp, height = 20.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(HomeTokens.ChipRadius)
            )
        }

        items(6) { GoodsCardSkeleton(brush) }
    }
}

@Composable
private fun GoodsCardSkeleton(brush: Brush) {
    Surface(
        shape = RoundedCornerShape(HomeTokens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column {
            SkeletonBlock(
                brush = brush,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(
                    topStart = HomeTokens.CardRadius,
                    topEnd = HomeTokens.CardRadius
                )
            )
            Column(Modifier.padding(HomeTokens.ContainerInset)) {
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    shape = RoundedCornerShape(HomeTokens.ChipRadius)
                )
                Spacer(Modifier.height(HomeTokens.ItemGap))
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier.size(width = 72.dp, height = 16.dp),
                    shape = RoundedCornerShape(HomeTokens.ChipRadius)
                )
                Spacer(Modifier.height(HomeTokens.ItemGap))
                SkeletonBlock(
                    brush = brush,
                    modifier = Modifier.size(width = 96.dp, height = 12.dp),
                    shape = RoundedCornerShape(HomeTokens.ChipRadius)
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(brush: Brush, modifier: Modifier, shape: Shape) {
    Box(modifier = modifier.clip(shape).background(brush))
}

/**
 * 骨架屏微光。渐变两端均取自主题，深浅色模式自动适配。
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "homeShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homeShimmerProgress"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x = progress * 900f - 300f, y = 0f),
        end = Offset(x = progress * 900f, y = 0f)
    )
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------
private val previewGoods = listOf(
    GoodsInfo(
        id = 1L, title = "罗技 G304 无线游戏鼠标",
        price = 80.0, originalPrice = 199.0,
        conditionDesc = "9成新", campus = "主校区"
    ),
    GoodsInfo(
        id = 2L, title = "iPad Air 5 64G WiFi版",
        price = 3200.0, originalPrice = 4799.0,
        conditionDesc = "9成新", campus = "主校区"
    ),
    GoodsInfo(
        id = 3L, title = "考研政治全套资料 2026版",
        price = 80.0, originalPrice = 298.0,
        conditionDesc = "8成新", campus = "南校区"
    ),
    GoodsInfo(
        id = 4L, title = "捷安特山地车 ATX660 26寸",
        price = 600.0, originalPrice = 1598.0,
        conditionDesc = "8成新", campus = "南校区"
    )
)

private val previewCategories = listOf(
    CategoryInfo(id = 1L, name = "教材书籍", icon = "MenuBook"),
    CategoryInfo(id = 2L, name = "数码产品", icon = "Devices"),
    CategoryInfo(id = 3L, name = "服饰鞋包", icon = "Checkroom"),
    CategoryInfo(id = 4L, name = "运动器材", icon = "SportsBasketball"),
    CategoryInfo(id = 5L, name = "生活用品", icon = "Coffee"),
    CategoryInfo(id = 6L, name = "交通工具", icon = "PedalBike"),
    CategoryInfo(id = 7L, name = "美妆护肤", icon = "Face"),
    CategoryInfo(id = 8L, name = "其他", icon = "Sell")
)

@Preview(name = "首页 · 成功", showBackground = true, device = "id:pixel_5")
@Composable
private fun HomeContentSuccessPreview() {
    CampusFleaTheme {
        HomeContent(
            state = HomeUiState(
                goods = previewGoods,
                categories = previewCategories,
                isPersonalized = true,
                hasMore = false
            )
        )
    }
}

@Preview(name = "首页 · 成功 · 深色", showBackground = true, device = "id:pixel_5", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeContentSuccessDarkPreview() {
    CampusFleaTheme(darkTheme = true) {
        HomeContent(
            state = HomeUiState(
                goods = previewGoods,
                categories = previewCategories,
                isPersonalized = true,
                hasMore = false
            )
        )
    }
}

@Preview(name = "首页 · 加载中", showBackground = true, device = "id:pixel_5")
@Composable
private fun HomeContentLoadingPreview() {
    CampusFleaTheme {
        HomeContent(state = HomeUiState(isLoading = true))
    }
}

@Preview(name = "首页 · 加载中 · 深色", showBackground = true, device = "id:pixel_5", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeContentLoadingDarkPreview() {
    CampusFleaTheme(darkTheme = true) {
        HomeContent(state = HomeUiState(isLoading = true))
    }
}

@Preview(name = "首页 · 空数据", showBackground = true, device = "id:pixel_5")
@Composable
private fun HomeContentEmptyPreview() {
    CampusFleaTheme {
        HomeContent(state = HomeUiState(isEmpty = true))
    }
}

@Preview(name = "首页 · 加载失败", showBackground = true, device = "id:pixel_5")
@Composable
private fun HomeContentErrorPreview() {
    CampusFleaTheme {
        HomeContent(
            state = HomeUiState(
                error = "获取推荐失败，请检查网络后重试",
                isLoading = false,
                isEmpty = true
            )
        )
    }
}

@Preview(name = "首页 · 加载失败 · 深色", showBackground = true, device = "id:pixel_5", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeContentErrorDarkPreview() {
    CampusFleaTheme(darkTheme = true) {
        HomeContent(
            state = HomeUiState(
                error = "获取推荐失败，请检查网络后重试",
                isLoading = false,
                isEmpty = true
            )
        )
    }
}

@Preview(name = "商品卡", showBackground = true)
@Composable
private fun GoodsCardPreview() {
    CampusFleaTheme {
        Box(Modifier.padding(16.dp)) {
            GoodsCard(goods = previewGoods[1], onClick = {})
        }
    }
}
