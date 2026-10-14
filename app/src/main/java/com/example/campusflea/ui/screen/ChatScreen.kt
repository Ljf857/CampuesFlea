package com.example.campusflea.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusflea.data.model.MessageInfo
import com.example.campusflea.data.model.MessageSendState
import com.example.campusflea.ui.components.GlassTopBar
import com.example.campusflea.ui.components.NetworkImage
import com.example.campusflea.ui.components.glassShadowBrushUp
import com.example.campusflea.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    navController: NavHostController,
    userId: Long,
    userName: String,
    goodsId: Long? = null,
    conversationId: Long? = null,
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val uploading by viewModel.uploading.collectAsState()
    val loadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val error by viewModel.error.collectAsState()
    var input by remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }
    var showClearMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val ctx = LocalContext.current

    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok: Boolean ->
        if (ok && cameraUri != null) {
            viewModel.uploadAndSendFile(ctx, userId, cameraUri!!, isImage = true)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = viewModel.createCameraTempUri(ctx)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(ctx, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val uri = viewModel.createCameraTempUri(ctx)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadAndSendFile(ctx, userId, it, isImage = true) } }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.uploadAndSendFile(ctx, userId, it, isImage = false) } }

    // goodsId 决定「这是哪件商品的会话」，conversationId 直接锁定会话，两者变化都要重新初始化。
    // conversationId 必须真正传下去：以前只传了 userId/goodsId，从消息列表进来时
    // 服务端只能按 (用户, 商品) 猜会话，同一对用户有多个会话时必然读错/写错。
    LaunchedEffect(userId, goodsId, conversationId) {
        viewModel.init(ctx, userId, goodsId, conversationId)
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val chatItems = remember(messages) { buildChatItems(messages) }

    // 贴近底部时才自动滚动。旧实现无条件滚到底，用户正在上翻历史时会被新消息强行拽回去。
    LaunchedEffect(chatItems.size) {
        if (chatItems.isEmpty()) return@LaunchedEffect
        val info = listState.layoutInfo
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
        if (lastVisible < 0 || lastVisible >= info.totalItemsCount - 3) {
            listState.animateScrollToItem(chatItems.size - 1)
        }
    }

    // 上滑到顶时加载更早的历史。此前聊天记录只能看到最近 20 条，再往前的永远打不开。
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.totalItemsCount == 0) return@derivedStateOf false
            (info.visibleItemsInfo.firstOrNull()?.index ?: 1) <= 1
        }
    }
    LaunchedEffect(shouldLoadMore, hasMore) {
        if (shouldLoadMore && hasMore) viewModel.loadMoreHistory()
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        GlassTopBar(
            title = { Text(userName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, "返回") } },
            actions = {
                Box {
                    IconButton(onClick = { showClearMenu = true }) { Icon(Icons.Default.DeleteSweep, "清除") }
                    DropdownMenu(expanded = showClearMenu, onDismissRequest = { showClearMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("清除聊天记录") },
                            onClick = { showClearMenu = false; viewModel.clearChat(ctx, userId) },
                            leadingIcon = { Icon(Icons.Default.DeleteForever, null) }
                        )
                    }
                }
            }
        )
        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            if (loadingMore) {
                item(key = "loading_more") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            } else if (hasMore) {
                item(key = "load_more_hint") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("上滑查看更早的消息", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else if (chatItems.isNotEmpty()) {
                item(key = "history_start") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("— 没有更早的消息了 —", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            items(chatItems, key = { item ->
                when (item) {
                    is ChatItem.DateHeader -> "date_${item.key}"
                    is ChatItem.TimeLabel -> "time_${item.key}"
                    is ChatItem.Message -> "msg_${item.key}"
                }
            }) { item ->
                Box(Modifier.animateItem(fadeInSpec = tween(200))) {
                    when (item) {
                        is ChatItem.DateHeader -> DateSeparator(item.label)
                        is ChatItem.TimeLabel -> TimeSeparator(item.time)
                        is ChatItem.Message -> MessageBubble(item.msg) { viewModel.retrySend(item.msg.localId) }
                    }
                }
            }
        }
        // 玻璃输入栏：顶部渐变投影 + 半透明表面
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.80f))) {
            Box(Modifier.fillMaxWidth().height(8.dp).background(glassShadowBrushUp()))
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                Box {
                    IconButton(onClick = { showAttachMenu = true }) {
                        if (uploading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Add, "添加")
                    }
                    DropdownMenu(expanded = showAttachMenu, onDismissRequest = { showAttachMenu = false }) {
                        DropdownMenuItem(text = { Text("拍照") }, onClick = {
                            showAttachMenu = false
                            launchCamera()
                        }, leadingIcon = { Icon(Icons.Default.CameraAlt, null) })
                        DropdownMenuItem(text = { Text("图片") }, onClick = { showAttachMenu = false; imageLauncher.launch("image/*") }, leadingIcon = { Icon(Icons.Default.Image, null) })
                        DropdownMenuItem(text = { Text("文件") }, onClick = { showAttachMenu = false; fileLauncher.launch("*/*") }, leadingIcon = { Icon(Icons.Default.AttachFile, null) })
                    }
                }
                // 上传附件不应阻断文字发送（以前两者共用一个 uploading 开关）
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    Modifier.weight(1f),
                    label = { Text("输入消息...") },
                    minLines = 1,
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val text = input.trim()
                        if (text.isNotEmpty()) {
                            viewModel.sendMessage(userId, text, goodsId)
                            input = ""
                        }
                    },
                    Modifier.height(48.dp),
                    enabled = input.isNotBlank()
                ) { Text("发送") }
            }
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
            Text(label, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimeSeparator(time: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun MessageBubble(msg: MessageInfo, onRetry: () -> Unit) {
    // 未确认的消息降低不透明度，与已送达的区分开
    val pendingAlpha = if (msg.sendState == MessageSendState.SENT) 1f else 0.65f

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!msg.isMine) {
                AvatarImage(avatar = msg.senderAvatar, name = msg.senderName.ifEmpty { "?" }, size = 36)
                Spacer(Modifier.width(6.dp))
            }
            Column(horizontalAlignment = if (msg.isMine) Alignment.End else Alignment.Start) {
                // 1v1 会话不重复渲染发送者昵称：每条消息上方都顶一个同样的名字只是噪音
                Box(Modifier.alpha(pendingAlpha)) {
                    when (msg.type) {
                        2 -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).widthIn(max = 200.dp)
                            ) {
                                NetworkImage(model = msg.content, contentDescription = null, modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Crop)
                            }
                        }
                        3 -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (msg.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = if (msg.isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.InsertDriveFile, null, tint = if (msg.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.width(6.dp))
                                    Text("文件", color = if (msg.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (msg.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = if (msg.isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    msg.content,
                                    Modifier.padding(10.dp).widthIn(max = 240.dp),
                                    color = if (msg.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                // 发送状态：仅自己发出的消息需要
                if (msg.isMine && msg.sendState != MessageSendState.SENT) {
                    Spacer(Modifier.height(2.dp))
                    SendStateIndicator(state = msg.sendState, onRetry = onRetry)
                }
            }
            if (msg.isMine) {
                Spacer(Modifier.width(6.dp))
                AvatarImage(avatar = msg.senderAvatar, name = msg.senderName.ifEmpty { "我" }, size = 36)
            }
        }
    }
}

@Composable
private fun SendStateIndicator(state: MessageSendState, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (state == MessageSendState.FAILED) Modifier.clickable(role = Role.Button, onClick = onRetry) else Modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            MessageSendState.SENDING -> {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
                Spacer(Modifier.width(4.dp))
                Text("发送中", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
            MessageSendState.FAILED -> {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("发送失败，点击重试", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
            MessageSendState.SENT -> Unit
        }
    }
}

@Composable
private fun AvatarImage(avatar: String, name: String, size: Int) {
    if (avatar.isNotEmpty()) {
        NetworkImage(
            model = avatar,
            contentDescription = null,
            modifier = Modifier.size(size.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (size / 2).sp)
        }
    }
}

private sealed class ChatItem {
    data class DateHeader(val key: String, val label: String) : ChatItem()
    data class TimeLabel(val key: String, val time: String) : ChatItem()
    data class Message(val msg: MessageInfo, val key: String) : ChatItem()
}

private fun buildChatItems(messages: List<MessageInfo>): List<ChatItem> {
    if (messages.isEmpty()) return emptyList()
    val items = mutableListOf<ChatItem>()
    val now = Calendar.getInstance()
    var prevMsg: MessageInfo? = null
    var msgIndex = 0

    for (msg in messages) {
        val cal = parseToCalendar(msg.createTime) ?: Calendar.getInstance()
        val prevCal = if (prevMsg != null) parseToCalendar(prevMsg.createTime) ?: Calendar.getInstance() else null
        if (prevCal == null || !isSameDay(cal, prevCal)) {
            val label = dateLabel(cal, now)
            if (label != null) {
                val key = dateKey(cal)
                items.add(ChatItem.DateHeader(key, label))
            }
        }
        val showTime = prevCal == null || prevMsg == null || timeDiffMinutes(prevMsg.createTime, msg.createTime) > 5
        if (showTime) {
            items.add(ChatItem.TimeLabel("${msg.id}_$msgIndex", timeLabel(cal)))
        }
        // 本地待确认消息 id 为 0，用 localId 保证 key 稳定且唯一
        val key = when {
            msg.localId != 0L -> "local_${msg.localId}"
            msg.id != 0L -> "remote_${msg.id}"
            else -> "idx_$msgIndex"
        }
        items.add(ChatItem.Message(msg, key))
        prevMsg = msg
        msgIndex++
    }
    return items
}

private fun dateKey(cal: Calendar): String =
    "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"

private fun dateLabel(cal: Calendar, now: Calendar): String? {
    if (isSameDay(cal, now)) return null
    val y = now.clone() as Calendar; y.add(Calendar.DAY_OF_YEAR, -1)
    if (isSameDay(cal, y)) return "昨天"
    if (isSameWeek(cal, now)) return chineseWeekday(cal)
    if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
        return "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    }
    return "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
}

private fun chineseWeekday(cal: Calendar): String = when (cal.get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> "周一"; Calendar.TUESDAY -> "周二"; Calendar.WEDNESDAY -> "周三"
    Calendar.THURSDAY -> "周四"; Calendar.FRIDAY -> "周五"; Calendar.SATURDAY -> "周六"
    Calendar.SUNDAY -> "周日"; else -> ""
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

private fun isSameWeek(c1: Calendar, c2: Calendar): Boolean {
    val a = c1.clone() as Calendar; a.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val b = c2.clone() as Calendar; b.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    a.set(Calendar.HOUR_OF_DAY, 0); a.set(Calendar.MINUTE, 0); a.set(Calendar.SECOND, 0); a.set(Calendar.MILLISECOND, 0)
    b.set(Calendar.HOUR_OF_DAY, 0); b.set(Calendar.MINUTE, 0); b.set(Calendar.SECOND, 0); b.set(Calendar.MILLISECOND, 0)
    return a.timeInMillis == b.timeInMillis
}

private fun chinesePeriod(cal: Calendar): String = when (cal.get(Calendar.HOUR_OF_DAY)) {
    in 0..5 -> "凌晨"; in 6..11 -> "上午"; in 12..17 -> "下午"; else -> "晚上"
}

private fun timeLabel(cal: Calendar): String {
    val p = chinesePeriod(cal)
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = String.format("%02d", cal.get(Calendar.MINUTE))
    return "$p $h:$m"
}

private fun timeDiffMinutes(t1: String, t2: String): Long {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val d1 = fmt.parse(t1) ?: return Long.MAX_VALUE
        val d2 = fmt.parse(t2) ?: return Long.MAX_VALUE
        (d2.time - d1.time) / 60000
    } catch (_: Exception) { Long.MAX_VALUE }
}

private fun parseToCalendar(timeStr: String): Calendar? {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = fmt.parse(timeStr) ?: return null
        Calendar.getInstance().apply { time = date }
    } catch (_: Exception) { null }
}
