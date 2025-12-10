package com.tikhub.videoparser.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * 视频转码 Worker
 * 注意：FFmpeg 功能暂时禁用，需要时请取消注释相关代码并添加 FFmpeg 依赖
 */
@HiltWorker
class TranscodeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationManager: TranscodeNotificationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_INPUT_FILE_PATH = "input_file_path"
        const val KEY_OUTPUT_FILE_PATH = "output_file_path"
        const val KEY_VIDEO_TITLE = "video_title"
        const val KEY_CODEC_TYPE = "codec_type"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR_MESSAGE = "error_message"

        // 转码预设配置
        const val PRESET_FAST = "fast"           // 快速转码（质量较低）
        const val PRESET_MEDIUM = "medium"       // 平衡模式（推荐）
        const val PRESET_SLOW = "slow"           // 慢速转码（质量最高）
    }

    override suspend fun doWork(): Result {
        // FFmpeg 功能暂时禁用
        Timber.w("⚠️ 转码功能暂时禁用（FFmpeg 库未包含）")
        val videoTitle = inputData.getString(KEY_VIDEO_TITLE) ?: "视频"
        notificationManager.showTranscodeFailed(videoTitle, "转码功能暂时禁用")
        return Result.failure(
            workDataOf(KEY_ERROR_MESSAGE to "转码功能暂时禁用（FFmpeg 库未包含）")
        )
    }
}
