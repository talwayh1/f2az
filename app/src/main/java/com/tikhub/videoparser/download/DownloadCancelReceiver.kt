package com.tikhub.videoparser.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import timber.log.Timber
import java.util.UUID

/**
 * 下载取消广播接收器
 *
 * 功能：
 * 1. 接收通知栏的取消下载操作
 * 2. 调用 WorkManager 取消对应的下载任务
 */
class DownloadCancelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadNotificationManager.ACTION_CANCEL_DOWNLOAD) {
            val workIdString = intent.getStringExtra(DownloadNotificationManager.EXTRA_WORK_ID)

            if (workIdString != null) {
                try {
                    val workId = UUID.fromString(workIdString)
                    Timber.i("用户取消下载任务: $workId")

                    // 取消 WorkManager 任务
                    WorkManager.getInstance(context).cancelWorkById(workId)

                    Timber.d("下载任务已取消: $workId")
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "无效的 Work ID: $workIdString")
                }
            } else {
                Timber.w("取消下载请求缺少 Work ID")
            }
        }
    }
}
