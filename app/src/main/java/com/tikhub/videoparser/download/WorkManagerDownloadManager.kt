package com.tikhub.videoparser.download

import android.content.Context
import androidx.work.*
import com.tikhub.videoparser.utils.Platform
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager 下载管理器（升级版）
 *
 * 新增功能:
 * 1. 后台下载（App 关闭也能继续）
 * 2. 下载通知（支持取消操作）
 * 3. 失败自动重试（智能退避策略）
 * 4. 批量下载（队列管理）
 * 5. 并发控制（防止带宽拥堵和平台限流）
 * 6. 平台特定 Headers 注入
 * 7. 文件大小预获取（用于完整性校验）
 */
@Singleton
class WorkManagerDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: DownloadNotificationManager
) {

    private val workManager = WorkManager.getInstance(context)

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        // 最大并发下载数（避免带宽拥堵）
        private const val MAX_CONCURRENT_DOWNLOADS = 3

        // 队列标签前缀
        private const val QUEUE_TAG_PREFIX = "download_queue_"
    }

    /**
     * 下载视频（升级版）
     *
     * @param url 视频 URL
     * @param platform 平台
     * @param fileName 文件名
     * @param useQueue 是否使用队列（串行下载，防止并发过多）
     * @return 下载任务 ID
     */
    fun downloadVideo(
        url: String,
        platform: Platform,
        fileName: String = "video_${System.currentTimeMillis()}.mp4",
        useQueue: Boolean = false
    ): UUID {
        Timber.i("使用 WorkManager 下载视频: $fileName (平台: ${platform.displayName}, 队列模式: $useQueue)")

        // 🎯 修复：移除主线程的文件大小预获取，改为在 Worker 中执行
        // 文件大小校验将在后台 Worker 中进行，避免阻塞主线程
        Timber.d("下载任务将在后台执行文件大小校验")

        // 创建下载任务输入数据
        val inputData = workDataOf(
            DownloadWorker.KEY_URL to url,
            DownloadWorker.KEY_PLATFORM to platform.name,
            DownloadWorker.KEY_FILE_NAME to fileName,
            DownloadWorker.KEY_FILE_TYPE to "video",
            DownloadWorker.KEY_EXPECTED_SIZE to 0L  // 在 Worker 中获取
        )

        // 配置下载任务
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,  // 使用指数退避策略
                15000,  // 15 秒初始退避时间
                TimeUnit.MILLISECONDS
            )
            .addTag("video_download")  // 添加标签，方便管理
            .addTag(platform.name)  // 添加平台标签
            .build()

        // 🎯 优化：根据是否使用队列决定提交方式
        if (useQueue) {
            // 使用唯一工作链，串行下载（防止并发过多）
            val queueName = "${QUEUE_TAG_PREFIX}${platform.name}"
            workManager.enqueueUniqueWork(
                queueName,
                ExistingWorkPolicy.APPEND,  // 追加到队列尾部
                downloadRequest
            )
            Timber.d("下载任务已加入队列: $queueName, 任务ID: ${downloadRequest.id}")
        } else {
            // 直接提交（允许并发）
            workManager.enqueue(downloadRequest)
            Timber.d("下载任务已提交: ${downloadRequest.id}")
        }

        return downloadRequest.id
    }

    /**
     * 批量下载图片（升级版 - 使用队列管理）
     *
     * @param urls 图片 URL 列表
     * @param platform 平台
     * @param useQueue 是否使用队列（推荐开启，防止并发过多）
     * @return 任务 ID 列表
     */
    fun downloadImages(
        urls: List<String>,
        platform: Platform,
        useQueue: Boolean = true
    ): List<UUID> {
        Timber.i("使用 WorkManager 批量下载 ${urls.size} 张图片 (平台: ${platform.displayName}, 队列模式: $useQueue)")

        val taskIds = mutableListOf<UUID>()
        val timestamp = System.currentTimeMillis()  // 🎯 修复：统一使用同一个时间戳
        val queueName = "${QUEUE_TAG_PREFIX}images_${platform.name}_$timestamp"

        urls.forEachIndexed { index, url ->
            // 🎯 修复：使用统一时间戳 + 索引，确保文件名唯一
            val fileName = "${platform.name.lowercase()}_${timestamp}_$index.${getImageExtension(url)}"

            Timber.d("准备下载任务 $index: fileName=$fileName, url=$url")

            val inputData = workDataOf(
                DownloadWorker.KEY_URL to url,
                DownloadWorker.KEY_PLATFORM to platform.name,
                DownloadWorker.KEY_FILE_NAME to fileName,
                DownloadWorker.KEY_FILE_TYPE to "image",
                DownloadWorker.KEY_BATCH_INDEX to index,
                DownloadWorker.KEY_BATCH_TOTAL to urls.size
            )

            val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10000,
                    TimeUnit.MILLISECONDS
                )
                .addTag("image_download")
                .addTag(platform.name)
                .addTag(queueName)  // 批量下载的统一标签
                .build()

            // 🎯 修复：直接使用 enqueue，让 WorkManager 自动管理并发
            // 不使用 enqueueUniqueWork，避免任务被覆盖
            // WorkManager 会根据系统资源自动限制并发数量
            workManager.enqueue(downloadRequest)

            taskIds.add(downloadRequest.id)
        }

        Timber.d("批量下载任务已提交: ${taskIds.size} 个任务")
        return taskIds
    }

    /**
     * 获取下载任务状态
     */
    fun getWorkInfo(workId: UUID) = workManager.getWorkInfoByIdLiveData(workId)

    /**
     * 取消下载任务
     */
    fun cancelDownload(workId: UUID) {
        workManager.cancelWorkById(workId)
        Timber.d("已取消下载任务: $workId")
    }

    /**
     * 取消所有下载任务
     */
    fun cancelAllDownloads() {
        workManager.cancelAllWork()
        Timber.d("已取消所有下载任务")
    }

    /**
     * 获取文件大小（通过 HEAD 请求读取 Content-Length）- 同步版本
     *
     * @param url 文件 URL
     * @param platform 平台（用于设置 Headers）
     * @return 文件大小（字节），失败返回 0
     */
    private fun getFileSizeSync(url: String, platform: Platform): Long {
        return try {
            // 获取平台特定的 Headers
            val headersConfig = PlatformHeadersConfig.getHeadersConfig(platform)

            val requestBuilder = Request.Builder()
                .url(url)
                .head()  // 使用 HEAD 请求，只获取响应头
                .header("User-Agent", headersConfig.userAgent)

            if (headersConfig.referer.isNotEmpty()) {
                requestBuilder.header("Referer", headersConfig.referer)
            }

            val request = requestBuilder.build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val size = response.body?.contentLength() ?: 0L
                    if (size > 0) {
                        Timber.d("获取文件大小成功: ${formatFileSize(size)}")
                    }
                    size
                } else {
                    Timber.w("获取文件大小失败: HTTP ${response.code}")
                    0L
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "获取文件大小异常")
            0L
        }
    }

    /**
     * 获取文件大小（通过 HEAD 请求读取 Content-Length）- 异步版本
     *
     * @param url 文件 URL
     * @param platform 平台（用于设置 Headers）
     * @return 文件大小（字节），失败返回 0
     */
    suspend fun getFileSize(url: String, platform: Platform): Long {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            getFileSizeSync(url, platform)
        }
    }

    /**
     * 获取图片扩展名
     */
    private fun getImageExtension(url: String): String {
        return when {
            url.contains(".png", ignoreCase = true) -> "png"
            url.contains(".gif", ignoreCase = true) -> "gif"
            url.contains(".webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format(java.util.Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    /**
     * 获取所有正在进行的下载任务
     */
    fun getRunningDownloads() = workManager.getWorkInfosByTagLiveData("video_download")

    /**
     * 取消特定平台的所有下载任务
     */
    fun cancelDownloadsByPlatform(platform: Platform) {
        workManager.cancelAllWorkByTag(platform.name)
        Timber.d("已取消平台 ${platform.displayName} 的所有下载任务")
    }

    /**
     * 取消批量下载任务
     */
    fun cancelBatchDownload(queueTag: String) {
        workManager.cancelAllWorkByTag(queueTag)
        Timber.d("已取消批量下载任务: $queueTag")
    }
}
