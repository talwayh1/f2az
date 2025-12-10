package com.tikhub.videoparser.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager 转码管理器
 * 负责启动、监控和管理视频转码任务
 */
@Singleton
class WorkManagerTranscodeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * 启动转码任务
     * @param inputFilePath 输入文件路径（ByteVC2 视频）
     * @param videoTitle 视频标题
     * @param codecType 原始编码类型
     * @return 转码任务的 UUID
     */
    fun startTranscode(
        inputFilePath: String,
        videoTitle: String,
        codecType: String = "ByteVC2"
    ): UUID {
        val inputFile = File(inputFilePath)

        // 生成输出文件路径（在同一目录下，添加 _h264 后缀）
        val outputFilePath = generateOutputFilePath(inputFilePath)

        Timber.i("🎬 准备启动转码任务")
        Timber.i("输入文件: $inputFilePath")
        Timber.i("输出文件: $outputFilePath")
        Timber.i("视频标题: $videoTitle")
        Timber.i("原始编码: $codecType")

        // 构建转码任务的输入数据
        val inputData = workDataOf(
            TranscodeWorker.KEY_INPUT_FILE_PATH to inputFilePath,
            TranscodeWorker.KEY_OUTPUT_FILE_PATH to outputFilePath,
            TranscodeWorker.KEY_VIDEO_TITLE to videoTitle,
            TranscodeWorker.KEY_CODEC_TYPE to codecType
        )

        // 设置任务约束（不需要网络）
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)  // 允许低电量时转码
            .setRequiresStorageNotLow(true)   // 需要足够的存储空间
            .build()

        // 创建一次性转码任务
        val transcodeRequest = OneTimeWorkRequestBuilder<TranscodeWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("transcode")
            .addTag("transcode_${inputFile.name}")
            .build()

        // 使用唯一工作名称，避免重复转码同一文件
        val uniqueWorkName = "transcode_${inputFile.absolutePath}"

        // 启动转码任务（如果已存在相同任务，则保留现有任务）
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.KEEP,
            transcodeRequest
        )

        Timber.i("✅ 转码任务已启动: ${transcodeRequest.id}")

        return transcodeRequest.id
    }

    /**
     * 生成输出文件路径
     * 在原文件名后添加 _h264 后缀
     */
    private fun generateOutputFilePath(inputFilePath: String): String {
        val inputFile = File(inputFilePath)
        val parentDir = inputFile.parentFile
        val fileName = inputFile.nameWithoutExtension
        val extension = inputFile.extension

        return File(parentDir, "${fileName}_h264.$extension").absolutePath
    }

    /**
     * 获取转码任务状态
     */
    fun getTranscodeStatus(workId: UUID): Flow<WorkInfo?> {
        return workManager.getWorkInfoByIdFlow(workId)
    }

    /**
     * 获取转码进度
     */
    fun getTranscodeProgress(workId: UUID): Flow<Int> {
        return workManager.getWorkInfoByIdFlow(workId).map { workInfo ->
            workInfo?.progress?.getInt(TranscodeWorker.KEY_PROGRESS, 0) ?: 0
        }
    }

    /**
     * 取消转码任务
     */
    fun cancelTranscode(workId: UUID) {
        Timber.i("🚫 取消转码任务: $workId")
        workManager.cancelWorkById(workId)
    }

    /**
     * 取消所有转码任务
     */
    fun cancelAllTranscodes() {
        Timber.i("🚫 取消所有转码任务")
        workManager.cancelAllWorkByTag("transcode")
    }

    /**
     * 检查文件是否正在转码
     */
    fun isFileTranscoding(filePath: String): Boolean {
        val uniqueWorkName = "transcode_$filePath"
        val workInfos = workManager.getWorkInfosForUniqueWork(uniqueWorkName).get()
        return workInfos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
    }

    /**
     * 获取所有转码任务
     */
    fun getAllTranscodeTasks(): Flow<List<WorkInfo>> {
        return workManager.getWorkInfosByTagFlow("transcode")
    }
}
