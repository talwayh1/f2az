package com.tikhub.videoparser.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * 转码取消广播接收器
 * 处理用户从通知栏取消转码任务的操作
 */
@AndroidEntryPoint
class TranscodeCancelReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CANCEL_TRANSCODE = "com.tikhub.videoparser.ACTION_CANCEL_TRANSCODE"
        const val EXTRA_WORK_ID = "work_id"
    }

    @Inject
    lateinit var notificationManager: TranscodeNotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL_TRANSCODE) {
            val workIdString = intent.getStringExtra(EXTRA_WORK_ID)
            if (workIdString != null) {
                try {
                    val workId = UUID.fromString(workIdString)
                    Timber.i("🚫 用户取消转码任务: $workId")

                    // 取消 WorkManager 任务
                    WorkManager.getInstance(context).cancelWorkById(workId)

                    // 取消通知
                    notificationManager.cancelTranscodeNotification(workId)

                    Timber.i("✅ 转码任务已取消")
                } catch (e: Exception) {
                    Timber.e(e, "取消转码任务失败")
                }
            }
        }
    }
}
