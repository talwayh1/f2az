package com.tikhub.videoparser.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.tikhub.videoparser.R
import com.tikhub.videoparser.ui.activity.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 转码通知管理器
 * 负责显示转码进度、成功、失败通知
 */
@Singleton
class TranscodeNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "transcode_channel"
        private const val CHANNEL_NAME = "视频转码"
        private const val CHANNEL_DESCRIPTION = "显示视频转码进度和结果"
        private const val NOTIFICATION_ID_BASE = 3000
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var notificationIdCounter = NOTIFICATION_ID_BASE

    init {
        createNotificationChannel()
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(true)
                enableVibration(false)
                enableLights(false)
            }
            notificationManager.createNotificationChannel(channel)
            Timber.d("转码通知渠道已创建")
        }
    }

    /**
     * 获取新的通知 ID
     */
    fun getNotificationId(): Int {
        return notificationIdCounter++
    }

    /**
     * 创建转码通知（用于前台服务）
     */
    fun createTranscodeNotification(
        videoTitle: String,
        progress: Int,
        workId: UUID
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 创建取消转码的 PendingIntent
        val cancelIntent = Intent(context, TranscodeCancelReceiver::class.java).apply {
            action = TranscodeCancelReceiver.ACTION_CANCEL_TRANSCODE
            putExtra(TranscodeCancelReceiver.EXTRA_WORK_ID, workId.toString())
        }

        val cancelPendingIntent = PendingIntent.getBroadcast(
            context,
            workId.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("正在转码: $videoTitle")
            .setContentText("转码进度: $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_delete,
                "取消",
                cancelPendingIntent
            )
            .build()
    }

    /**
     * 显示转码进度通知
     */
    fun showTranscodeProgress(
        videoTitle: String,
        progress: Int,
        workId: UUID
    ) {
        val notification = createTranscodeNotification(videoTitle, progress, workId)
        notificationManager.notify(workId.hashCode(), notification)
    }

    /**
     * 显示转码成功通知
     */
    fun showTranscodeSuccess(
        videoTitle: String,
        outputPath: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("✅ 转码完成")
            .setContentText("$videoTitle 已转码为 H.264 格式")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$videoTitle\n已转码为 H.264 格式\n保存位置: $outputPath")
            )
            .build()

        notificationManager.notify(getNotificationId(), notification)
        Timber.i("✅ 转码成功通知已显示: $videoTitle")
    }

    /**
     * 显示转码失败通知
     */
    fun showTranscodeFailed(
        videoTitle: String,
        errorMessage: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("❌ 转码失败")
            .setContentText("$videoTitle: $errorMessage")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$videoTitle\n错误: $errorMessage")
            )
            .build()

        notificationManager.notify(getNotificationId(), notification)
        Timber.e("❌ 转码失败通知已显示: $videoTitle - $errorMessage")
    }

    /**
     * 取消转码通知
     */
    fun cancelTranscodeNotification(workId: UUID) {
        notificationManager.cancel(workId.hashCode())
        Timber.d("转码通知已取消: $workId")
    }
}
