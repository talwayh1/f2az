package com.tikhub.videoparser.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tikhub.videoparser.R
import com.tikhub.videoparser.ui.activity.MainActivity
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载通知管理器
 * 功能:
 * 1. 显示下载进度通知（支持前台服务）
 * 2. 下载完成通知
 * 3. 下载失败通知
 * 4. 批量下载进度汇总
 * 5. 支持取消下载操作
 */
@Singleton
class DownloadNotificationManager @Inject constructor(
    private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val CHANNEL_NAME = "下载管理"
        private const val CHANNEL_DESCRIPTION = "文件下载进度和状态通知"

        // 通知 ID
        private const val NOTIFICATION_ID_VIDEO = 1001
        private const val NOTIFICATION_ID_IMAGES = 1002
        private const val NOTIFICATION_ID_COMPLETE = 1003

        // 取消下载的 Action
        const val ACTION_CANCEL_DOWNLOAD = "com.tikhub.videoparser.ACTION_CANCEL_DOWNLOAD"
        const val EXTRA_WORK_ID = "work_id"
    }

    init {
        createNotificationChannel()
    }

    /**
     * 创建通知渠道 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW  // 低重要性，不打扰用户
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台服务通知（用于 Worker 的 getForegroundInfo）
     *
     * @param workId 下载任务 ID
     * @param fileName 文件名
     * @param progress 进度（0-100）
     * @param indeterminate 是否为不确定进度
     * @return Notification 对象
     */
    fun createForegroundNotification(
        workId: UUID,
        fileName: String,
        progress: Int = 0,
        indeterminate: Boolean = true
    ): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建取消下载的 PendingIntent
        val cancelIntent = Intent(ACTION_CANCEL_DOWNLOAD).apply {
            putExtra(EXTRA_WORK_ID, workId.toString())
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            workId.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("正在下载")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)  // 不可滑动删除
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "取消",
                cancelPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    /**
     * 显示视频下载进度通知
     */
    fun showVideoDownloadProgress(
        fileName: String,
        progress: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("正在下载视频")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)  // 不可滑动删除
            .setContentIntent(pendingIntent)
            .setSubText("${formatFileSize(downloadedBytes)} / ${formatFileSize(totalBytes)}")
            .build()

        notificationManager.notify(NOTIFICATION_ID_VIDEO, notification)
    }

    /**
     * 显示视频下载完成通知
     */
    fun showVideoDownloadComplete(fileName: String, @Suppress("UNUSED_PARAMETER") filePath: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("视频下载完成")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)  // 点击后自动消失
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // 取消进度通知，显示完成通知
        notificationManager.cancel(NOTIFICATION_ID_VIDEO)
        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    /**
     * 显示批量下载图片进度
     */
    fun showImagesDownloadProgress(
        completedCount: Int,
        totalCount: Int,
        currentFileName: String
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progress = if (totalCount > 0) (completedCount * 100 / totalCount) else 0

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("正在批量下载图片")
            .setContentText(currentFileName)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSubText("$completedCount / $totalCount")
            .build()

        notificationManager.notify(NOTIFICATION_ID_IMAGES, notification)
    }

    /**
     * 显示批量下载完成通知
     */
    fun showImagesDownloadComplete(totalCount: Int, successCount: Int, failedCount: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (failedCount > 0) {
            "成功: $successCount, 失败: $failedCount"
        } else {
            "全部下载完成"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("图片下载完成 ($successCount/$totalCount)")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // 取消进度通知，显示完成通知
        notificationManager.cancel(NOTIFICATION_ID_IMAGES)
        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    /**
     * 显示下载失败通知
     */
    fun showDownloadFailed(fileName: String, errorMessage: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("下载失败")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(errorMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID_COMPLETE, notification)
    }

    /**
     * 取消所有下载通知
     */
    fun cancelAllNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_VIDEO)
        notificationManager.cancel(NOTIFICATION_ID_IMAGES)
        notificationManager.cancel(NOTIFICATION_ID_COMPLETE)
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
