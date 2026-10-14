package com.example.campusflea.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.ui.components.GlassCard
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.glassShadowBrushUp
import com.example.campusflea.ui.components.pressScale
import com.example.campusflea.ui.navigation.Screen
import com.example.campusflea.ui.viewmodel.GoodsDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodsDetailScreen(navController: NavHostController, goodsId: Long, viewModel: GoodsDetailViewModel = viewModel()) {
    val detail by viewModel.detail.collectAsState()
    val isFavorited by viewModel.isFavorited.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    LaunchedEffect(goodsId) { viewModel.loadDetail(goodsId) }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    if (isLoading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    else detail?.let { d ->
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                GlassTopBar(title = { Text("商品详情") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) { Icon(Icons.Default.MoreVert, "更多") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("分享") }, onClick = { showMenu = false; Toast.makeText(ctx, "已复制分享链接", Toast.LENGTH_SHORT).show() }, leadingIcon = { Icon(Icons.Default.Share, null) })
                            DropdownMenuItem(text = { Text("收藏") }, onClick = { showMenu = false; viewModel.toggleFavorite() }, leadingIcon = { Icon(if (isFavorited) Icons.Default.Star else Icons.Default.StarBorder, null) })
                            DropdownMenuItem(text = { Text("举报") }, onClick = { showMenu = false; navController.navigate(Screen.Report.createRoute(d.id)) }, leadingIcon = { Icon(Icons.Default.Report, null) })
                        }
                    }
                })
                val images = d.images
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    if (images.isEmpty()) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Text("暂无图片", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        val ps = rememberPagerState(pageCount = { images.size })
                        HorizontalPager(state = ps, modifier = Modifier.fillMaxSize()) { p -> NetworkImage(model = images[p], contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("${ps.currentPage + 1}/${images.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("¥${d.price}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error); if (d.originalPrice > 0) Text("  ¥${d.originalPrice}", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline, style = LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) }
                    Text(d.title, fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
                    Row { if (d.conditionDesc.isNotEmpty()) Surface(shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.secondaryContainer) { Text(d.conditionDesc, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) }; Spacer(Modifier.width(8.dp)); Text("浏览${d.viewCount} 收藏${d.favoriteCount}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline) }
                }
                HorizontalDivider()
                d.seller?.let { s ->
                    GlassCard(Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(8.dp), elevation = 1.dp) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape), contentAlignment = Alignment.Center) { Text(s.nickname.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Text(s.nickname, fontWeight = FontWeight.SemiBold, fontSize = 15.sp); if (s.verifyStatus == 1) { Spacer(Modifier.width(4.dp)); Surface(shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.primaryContainer) { Text("已认证", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) } } }
                                Text("${s.campus}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                Text("已售${s.soldCount}件 · 好评率${String.format("%.0f", s.avgRating * 100)}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            OutlinedButton(onClick = { Toast.makeText(ctx, "进入店铺", Toast.LENGTH_SHORT).show() }) { Text("进店") }
                        }
                    }
                }
                HorizontalDivider()
                Column(Modifier.padding(16.dp)) { Text("商品描述", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text(d.description, fontSize = 14.sp) }
                HorizontalDivider()
                Column(Modifier.padding(16.dp)) { Text("交易信息", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text("交易方式：${d.deliveryTypeDesc}"); Text("交易位置：${d.location}"); Text("发布时间：${d.publishTime}") }
                if (d.relatedGoods.isNotEmpty()) {
                    HorizontalDivider()
                    Column(Modifier.padding(16.dp)) { Text("相关推荐", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { d.relatedGoods.take(4).forEach { r -> Column(Modifier.width(140.dp).pressScale(role = Role.Button, onClick = { navController.navigate(Screen.GoodsDetail.createRoute(r.id)) })) { NetworkImage(model = r.coverImage, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop); Text(r.title, fontSize = 12.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis); Text("¥${r.price}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) } } } }
                }
            }
            // 玻璃购买栏：顶部渐变投影 + 半透明表面
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.80f))) {
                Box(Modifier.fillMaxWidth().height(10.dp).background(glassShadowBrushUp()))
                Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)))
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        AnimatedContent(
                            targetState = isFavorited,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))) togetherWith
                                    (fadeOut(animationSpec = tween(100)) + scaleOut(targetScale = 0.6f, animationSpec = tween(100)))
                            },
                            label = "favIcon"
                        ) { fav ->
                            Icon(if (fav) Icons.Default.Star else Icons.Default.StarBorder, "收藏", tint = if (fav) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    // 带上 goodsId：卖家需要知道买家问的是哪件商品，服务端也据此归入对应的商品会话
                    OutlinedButton(onClick = {
                        d.seller?.let {
                            navController.navigate(Screen.Chat.createRoute(it.id, it.nickname, goodsId = d.id))
                        }
                    }) { Text("聊聊") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { navController.navigate(Screen.CreateOrder.createRoute(d.id)) }, modifier = Modifier.weight(1f).height(48.dp)) { Text("立即购买") }
                }
            }
        }
    }
}
