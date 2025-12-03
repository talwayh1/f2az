package com.tikhub.videoparser.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.tikhub.videoparser.utils.ApiConstants
import com.tikhub.videoparser.utils.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载状态
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class Success(val filePath: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

/**
 * 下载管理器
 * 核心功能：
 * 1. 自动添加 Referer Header 绕过防盗链
 * 2. Android 10+ Scoped Storage 适配
 * 3. 下载完成后自动扫描相册
 */
@Singleton
class DownloadManager @Inject constructor(
    private val context: Context
) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .build()

    /**
     * 获取文件大小（通过 HEAD 请求读取 Content-Length）
     *
     * @param url 文件 URL
     * @param platform 平台（用于设置 Referer）
     * @return 文件大小（字节），失败返回 0
     */
    suspend fun getFileSize(url: String, platform: Platform): Long {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()  // 使用 HEAD 请求，只获取响应头
                .header("Referer", ApiConstants.Referers.getReferer(platform))
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.contentLength() ?: 0L
                } else {
                    0L
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * 下载视频
     *
     * @param url 视频 URL（无水印）
     * @param platform 平台（用于设置 Referer）
     * @param fileName 文件名（不含扩展名）
     * @return Flow<DownloadState> 下载状态流
     */
    fun downloadVideo(
        url: String,
        platform: Platform,
        fileName: String = "video_${System.currentTimeMillis()}"
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        try {
            // 构建请求（关键：添加 Referer Header）
            val request = Request.Builder()
                .url(url)
                .header("Referer", ApiConstants.Referers.getReferer(platform))
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .build()

            // 发起请求
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Failed("下载失败: HTTP ${response.code}"))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(DownloadState.Failed("响应体为空"))
                    return@flow
                }

                val contentLength = body.contentLength()
                val inputStream = body.byteStream()

                // Android 10+ 使用 MediaStore（Scoped Storage）
                val outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStoreQ(fileName, "video/mp4", Environment.DIRECTORY_MOVIES)
                } else {
                    saveToLegacyStorage(fileName, "mp4", ApiConstants.Download.SAVE_DIR_VIDEOS)
                }

                if (outputUri == null) {
                    emit(DownloadState.Failed("无法创建文件"))
                    return@flow
                }

                // 写入文件并报告进度
                context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // 计算进度
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            emit(DownloadState.Downloading(progress))
                        }
                    }

                    outputStream.flush()
                }

                // 下载完成，通知相册扫描
                val filePath = getFilePathFromUri(outputUri) ?: "未知路径"
                notifyMediaScanner(filePath)

                emit(DownloadState.Success(filePath))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(DownloadState.Failed(e.message ?: "未知错误"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 下载图片
     *
     * @param url 图片 URL
     * @param platform 平台
     * @param fileName 文件名
     */
    fun downloadImage(
        url: String,
        platform: Platform,
        fileName: String = "image_${System.currentTimeMillis()}"
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        try {
            val request = Request.Builder()
                .url(url)
                .header("Referer", ApiConstants.Referers.getReferer(platform))
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(DownloadState.Failed("下载失败: HTTP ${response.code}"))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(DownloadState.Failed("响应体为空"))
                    return@flow
                }

                val inputStream = body.byteStream()

                // 根据 Content-Type 判断图片格式
                val mimeType = response.header("Content-Type") ?: "image/jpeg"
                val extension = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("gif") -> "gif"
                    mimeType.contains("webp") -> "webp"
                    else -> "jpg"
                }

                val outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStoreQ(fileName, mimeType, Environment.DIRECTORY_PICTURES)
                } else {
                    saveToLegacyStorage(fileName, extension, ApiConstants.Download.SAVE_DIR_IMAGES)
                }

                if (outputUri == null) {
                    emit(DownloadState.Failed("无法创建文件"))
                    return@flow
                }

                context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }

                val filePath = getFilePathFromUri(outputUri) ?: "未知路径"
                notifyMediaScanner(filePath)

                emit(DownloadState.Success(filePath))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(DownloadState.Failed(e.message ?: "未知错误"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Android 10+ 保存到 MediaStore
     */
    private fun saveToMediaStoreQ(
        fileName: String,
        mimeType: String,
        directory: String
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/TikHub")
        }

        val collection = when {
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }

        return context.contentResolver.insert(collection, contentValues)
    }

    /**
     * Android 9 及以下保存到外部存储
     */
    private fun saveToLegacyStorage(
        fileName: String,
        extension: String,
        subDirectory: String
    ): Uri? {
        val directory = File(Environment.getExternalStorageDirectory(), subDirectory)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "$fileName.$extension")
        return Uri.fromFile(file)
    }

    /**
     * 从 Uri 获取文件路径
     */
    private fun getFilePathFromUri(uri: Uri): String? {
        return if (uri.scheme == "file") {
            uri.path
        } else {
            // MediaStore URI，返回 display name
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        }
    }

    /**
     * 通知系统相册扫描文件
     */
    private fun notifyMediaScanner(filePath: String) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(filePath),
            null
        ) { path, uri ->
            println("相册扫描完成: $path -> $uri")
        }
    }
}
