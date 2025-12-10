package com.tikhub.videoparser.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tikhub.videoparser.utils.Platform
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * 下载 Worker（升级版）
 *
 * 新增功能：
 * 1. 前台服务支持 - 防止长时间下载被系统杀掉
 * 2. 临时文件策略 - 下载到 .tmp 文件，完成后重命名，防止文件损坏
 * 3. 平台特定 Headers - 支持每个平台的专属 User-Agent 和 Referer
 * 4. 取消检测 - 支持用户取消下载
 * 5. 文件完整性校验 - 验证文件大小
 */
@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val workerParams: WorkerParameters,
    private val notificationManager: DownloadNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_URL_LIST = "url_list"  // 🎯 新增：多 URL 列表（CDN 容错）
        const val KEY_PLATFORM = "platform"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_FILE_TYPE = "file_type"  // "video" or "image"
        const val KEY_BATCH_INDEX = "batch_index"
        const val KEY_BATCH_TOTAL = "batch_total"
        const val KEY_EXPECTED_SIZE = "expected_size"  // 预期文件大小（用于校验）

        const val KEY_PROGRESS = "progress"
        const val KEY_DOWNLOADED_BYTES = "downloaded_bytes"
        const val KEY_TOTAL_BYTES = "total_bytes"
    }

    // 🎯 优化：配置 OkHttp 客户端以提升下载性能
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)  // 连接超时 30 秒
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)    // 读取超时 120 秒（大文件需要更长时间）
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)    // 写入超时 60 秒
        .retryOnConnectionFailure(true)  // 连接失败时自动重试
        .build()

    /**
     * 提供前台服务信息（防止长时间下载被系统杀掉）
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "下载中..."

        val notification = notificationManager.createForegroundNotification(
            workId = id,
            fileName = fileName,
            progress = 0,
            indeterminate = true
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                id.hashCode(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(id.hashCode(), notification)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 🎯 步骤1：立即晋升为前台服务，防止被系统杀掉
            setForeground(getForegroundInfo())

            // 🎯 步骤2：获取下载参数
            val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
            val platformName = inputData.getString(KEY_PLATFORM) ?: "UNKNOWN"
            val fileName = inputData.getString(KEY_FILE_NAME) ?: "download_${System.currentTimeMillis()}"
            val fileType = inputData.getString(KEY_FILE_TYPE) ?: "video"
            @Suppress("UNUSED_VARIABLE")
            val expectedSize = inputData.getLong(KEY_EXPECTED_SIZE, 0L)
            val platform = Platform.valueOf(platformName)

            // 🎯 新增：支持多 URL 轮询（CDN 容错）
            val urlsArray = inputData.getStringArray(KEY_URL_LIST)
            val urls = if (!urlsArray.isNullOrEmpty()) {
                urlsArray.toList()
            } else {
                listOf(url)  // 降级到单 URL
            }

            Timber.i("开始下载: $fileName (平台: ${platform.displayName}, URL数量: ${urls.size})")

            // 🎯 步骤3：获取平台特定的 Headers 配置
            val headersConfig = PlatformHeadersConfig.getHeadersConfig(platform)

            // 🎯 步骤4：多 URL 轮询下载（CDN 容错）
            var lastError: Exception? = null

            for ((index, currentUrl) in urls.withIndex()) {
                try {
                    Timber.d("尝试 URL ${index + 1}/${urls.size}: $currentUrl")

                    // 构建请求（使用平台专属 Headers）
                    val requestBuilder = Request.Builder().url(currentUrl)

                    // 添加 User-Agent
                    requestBuilder.header("User-Agent", headersConfig.userAgent)

                    // 添加 Referer（如果有）
                    if (headersConfig.referer.isNotEmpty()) {
                        requestBuilder.header("Referer", headersConfig.referer)
                    }

                    // 添加其他额外的 Headers
                    headersConfig.additionalHeaders.forEach { (key, value) ->
                        requestBuilder.header(key, value)
                    }

                    val request = requestBuilder.build()

                    Timber.d("请求 Headers: User-Agent=${headersConfig.userAgent}, Referer=${headersConfig.referer}")

                    // 🎯 步骤5：执行下载
                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        val errorMsg = "HTTP ${response.code}"
                        Timber.w("URL ${index + 1} 下载失败: $errorMsg")

                        // 如果是 403 或 404，尝试下一个 URL
                        if (response.code in listOf(403, 404) && index < urls.size - 1) {
                            Timber.i("尝试下一个 CDN 节点...")
                            continue
                        }

                        // 最后一个 URL 也失败了
                        if (index == urls.size - 1) {
                            notificationManager.showDownloadFailed(fileName, errorMsg)
                            return@withContext Result.failure()
                        }
                        continue
                    }

                    // 下载成功，跳出循环
                    Timber.i("URL ${index + 1} 下载成功")

                    // 继续处理响应体...
                    return@withContext processDownloadResponse(response, fileName, fileType)

                } catch (e: Exception) {
                    lastError = e
                    Timber.e(e, "URL ${index + 1} 下载异常")

                    // 如果不是最后一个 URL，尝试下一个
                    if (index < urls.size - 1) {
                        Timber.i("尝试下一个 CDN 节点...")
                        continue
                    }
                }
            }

            // 所有 URL 都失败了
            Timber.e("所有 URL 下载失败")
            notificationManager.showDownloadFailed(fileName, lastError?.message ?: "所有 CDN 节点均失败")
            Result.failure()
        } catch (e: Exception) {
            Timber.e(e, "下载过程发生异常")
            val fileName = inputData.getString(KEY_FILE_NAME) ?: "未知文件"
            notificationManager.showDownloadFailed(fileName, e.message ?: "未知错误")
            Result.failure()
        }
    }

    /**
     * 获取下载目录
     */
    private fun getDownloadDirectory(fileType: String): File {
        val type = when (fileType) {
            "video" -> Environment.DIRECTORY_MOVIES
            "image" -> Environment.DIRECTORY_PICTURES
            else -> Environment.DIRECTORY_DOWNLOADS
        }

        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(Environment.getExternalStoragePublicDirectory(type), "TikHub")
        } else {
            File(Environment.getExternalStorageDirectory(), "Download/TikHub/$type")
        }

        if (!dir.exists()) {
            dir.mkdirs()
        }

        return dir
    }

    /**
     * 扫描媒体文件，刷新相册
     */
    private fun scanMediaFile(filePath: String, fileType: String) {
        val mimeType = when (fileType) {
            "video" -> "video/mp4"
            "image" -> when {
                filePath.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
                filePath.endsWith(".png", ignoreCase = true) -> "image/png"
                filePath.endsWith(".gif", ignoreCase = true) -> "image/gif"
                filePath.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }
            else -> "*/*"
        }

        Timber.i("扫描媒体文件: $filePath, mimeType=$mimeType")

        MediaScannerConnection.scanFile(
            appContext,
            arrayOf(filePath),
            arrayOf(mimeType)
        ) { path, uri ->
            Timber.i("媒体扫描完成: path=$path, uri=$uri")
        }
    }

    /**
     * 处理下载响应（提取为独立函数以支持多 URL 轮询）
     */
    private suspend fun processDownloadResponse(
        response: okhttp3.Response,
        fileName: String,
        fileType: String
    ): Result {
        val expectedSize = inputData.getLong(KEY_EXPECTED_SIZE, 0L)

        val body = response.body ?: run {
            Timber.e("响应体为空")
            notificationManager.showDownloadFailed(fileName, "响应体为空")
            return Result.failure()
        }

        val contentLength = body.contentLength()
        val inputStream = body.byteStream()

        // 创建临时文件（.tmp 策略，防止下载中断导致文件损坏）
        val downloadDir = getDownloadDirectory(fileType)
        val tempFile = File(appContext.cacheDir, "$fileName.tmp")
        val finalFile = File(downloadDir, fileName)

        Timber.d("临时文件: ${tempFile.absolutePath}")
        Timber.d("最终文件: ${finalFile.absolutePath}")

        // 写入临时文件并报告进度
        @Suppress("RedundantExplicitType", "VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var downloadSuccess: Boolean = false
        try {
            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(128 * 1024)  // 128KB 缓冲区
                var bytesRead: Int
                var totalBytesRead = 0L
                @Suppress("UNUSED_VARIABLE", "UNUSED_VALUE")
                var lastProgressUpdate = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        Timber.w("下载任务被用户取消: $fileName")
                        outputStream.close()
                        tempFile.delete()
                        return Result.failure()
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    if (contentLength > 0) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()

                        if (progress - lastProgressUpdate >= 1 || progress == 100) {
                            @Suppress("UNUSED_VALUE")
                            lastProgressUpdate = progress

                            setProgressAsync(
                                workDataOf(
                                    KEY_PROGRESS to progress,
                                    KEY_DOWNLOADED_BYTES to totalBytesRead,
                                    KEY_TOTAL_BYTES to contentLength
                                )
                            )

                            if (fileType == "video" && (progress % 5 == 0 || progress == 100)) {
                                notificationManager.showVideoDownloadProgress(
                                    fileName,
                                    progress,
                                    totalBytesRead,
                                    contentLength
                                )
                            }

                            Timber.d("下载进度: $progress%, $totalBytesRead/$contentLength bytes")
                        }
                    }
                }

                outputStream.flush()
            }

            // 文件完整性校验
            val downloadedSize = tempFile.length()
            Timber.i("下载完成，文件大小: $downloadedSize bytes")

            if (contentLength > 0 && downloadedSize != contentLength) {
                Timber.e("文件大小不匹配: 预期 $contentLength bytes, 实际 $downloadedSize bytes")
                notificationManager.showDownloadFailed(fileName, "文件大小不匹配，可能下载不完整")
                tempFile.delete()
                return Result.failure()
            }

            if (expectedSize > 0 && downloadedSize != expectedSize) {
                Timber.w("文件大小与预期不符: 预期 $expectedSize bytes, 实际 $downloadedSize bytes")
            }

            // 原子写入 - 将临时文件移动到最终位置
            if (tempFile.renameTo(finalFile)) {
                Timber.i("文件移动成功: ${finalFile.absolutePath}")
                downloadSuccess = true
            } else {
                Timber.w("renameTo 失败，使用复制方式")
                try {
                    // 使用缓冲流复制，提高性能和可靠性
                    tempFile.inputStream().use { input ->
                        finalFile.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 128 * 1024)
                            output.flush()
                        }
                    }

                    // 验证复制后的文件大小
                    val copiedSize = finalFile.length()
                    val originalSize = tempFile.length()

                    if (copiedSize != originalSize) {
                        Timber.e("文件复制后大小不匹配: 原始 $originalSize bytes, 复制后 $copiedSize bytes")
                        finalFile.delete()
                        notificationManager.showDownloadFailed(fileName, "文件保存失败（大小不匹配）")
                        tempFile.delete()
                        return Result.failure()
                    }

                    Timber.i("文件复制成功: ${finalFile.absolutePath}, 大小: $copiedSize bytes")
                    tempFile.delete()
                    downloadSuccess = true
                } catch (e: Exception) {
                    Timber.e(e, "文件复制失败")
                    finalFile.delete()
                    tempFile.delete()
                    notificationManager.showDownloadFailed(fileName, "文件保存失败: ${e.message}")
                    return Result.failure()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "下载过程发生异常")
            tempFile.delete()
            throw e
        }

        if (!downloadSuccess) {
            notificationManager.showDownloadFailed(fileName, "文件保存失败")
            return Result.failure()
        }

        // 下载完成
        val filePath = finalFile.absolutePath
        Timber.i("下载完成: $filePath")

        // 刷新媒体库
        scanMediaFile(filePath, fileType)

        // 显示完成通知
        if (fileType == "video") {
            notificationManager.showVideoDownloadComplete(fileName, filePath)
        } else {
            val batchIndex = inputData.getInt(KEY_BATCH_INDEX, -1)
            val batchTotal = inputData.getInt(KEY_BATCH_TOTAL, 0)

            if (batchTotal > 0) {
                notificationManager.showImagesDownloadProgress(
                    batchIndex + 1,
                    batchTotal,
                    fileName
                )

                if (batchIndex == batchTotal - 1) {
                    notificationManager.showImagesDownloadComplete(
                        batchTotal,
                        batchTotal,
                        0
                    )
                }
            }
        }

        return Result.success(
            workDataOf(
                "file_path" to filePath,
                "file_name" to fileName
            )
        )
    }
}
