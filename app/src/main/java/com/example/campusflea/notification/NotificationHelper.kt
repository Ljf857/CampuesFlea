package com.example.campusflea.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.campusflea.MainActivity
import com.example.campusflea.R
import com.example.campusflea.data.model.MessageInfo

/**
 * 本地通知工具：创建通知渠道、判断通知权限、展示新消息通知。
 */
object NotificationHelper {

    const val CHANNEL_MESSAGE_ID = "channel_new_message"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_MESSAGE_ID,
                "新消息",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "收到新的聊天消息时提醒"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /** Android 13+ 需要 POST_NOTIFICATIONS 运行时权限；13 以下默认允许。 */
    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun showNewMessage(context: Context, message: MessageInfo) {
        if (!canPostNotifications(context)) return

        val contentText = when (message.type) {
            2 -> "[图片]"
            3 -> "[文件]"
            else -> message.content.ifBlank { message.typeDesc.ifBlank { "收到一条新消息" } }
        }
        val title = message.senderName.ifBlank { "新消息" }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 通知权限被拒绝，静默忽略
        }
    }
}
