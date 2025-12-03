package com.tikhub.videoparser.utils

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * 下载助手
 *
 * 功能：
 * 1. 使用 Android DownloadManager 下载视频和图片
 * 2. 自动添加平台对应的 Referer 头（绕过防盗链）
 * 3. 下载完成后通知相册扫描
 * 4. 支持批量下载图片
 */
object DownloadHelper {

    /**
     * 平台 Referer 映射
     * 某些平台需要特定的 Referer 才能下载资源
     */
    private val PLATFORM_REFERERS = mapOf(
        "douyin" to "https://www.douyin.com/",
        "tiktok" to "https://www.tiktok.com/",
        "xiaohongshu" to "https://www.xiaohongshu.com/",
        "kuaishou" to "https://www.kuaishou.com/",
        "bilibili" to "https://www.bilibili.com/",
        "weibo" to "https://weibo.com/",
        "xigua" to "https://www.ixigua.com/",
        "instagram" to "https://www.instagram.com/",
        "youtube" to "https://www.youtube.com/"
    )

    /**
     * 下载视频
     *
     * @param context Context
     * @param videoUrl 视频 URL
     * @param fileName 保存的文件名
     * @param platform 平台标识（用于添加 Referer）
     */
    fun downloadVideo(
        context: Context,
        videoUrl: String,
        fileName: String,
        platform: String
    ): Long {
        Timber.d("开始下载视频: $fileName")
        Timber.d("URL: $videoUrl")
        Timber.d("平台: $platform")

        val downloadManager = context.getSystemService<DownloadManager>()
            ?: throw IllegalStateException("DownloadManager 不可用")

        val request = DownloadManager.Request(Uri.parse(videoUrl)).apply {
            // 设置标题和描述
            setTitle(fileName)
            setDescription("正在下载视频...")

            // 添加 Referer（关键：绕过防盗链）
            PLATFORM_REFERERS[platform]?.let { referer ->
                addRequestHeader("Referer", referer)
                Timber.d("添加 Referer: $referer")
            }

            // 添加 User-Agent
            addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")

            // 设置通知栏可见
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // 设置保存路径（Movies 目录）
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES,
                "TikHub/$fileName"
            )

            // 允许使用移动网络
            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )

            // 允许漫游
            setAllowedOverRoaming(true)
        }

        val downloadId = downloadManager.enqueue(request)

        Timber.i("✅ 视频下载任务已加入队列，ID: $downloadId")

        // 监听下载进度（可选）
        monitorDownload(context, downloadId, fileName)

        return downloadId
    }

    /**
     * 批量下载图片
     *
     * @param context Context
     * @param imageUrls 图片 URL 列表
     * @param filePrefix 文件名前缀（如 "xiaohongshu_12345"）
     * @param platform 平台标识
     */
    fun downloadImages(
        context: Context,
        imageUrls: List<String>,
        filePrefix: String,
        platform: String
    ): List<Long> {
        Timber.d("开始批量下载图片，共 ${imageUrls.size} 张")

        val downloadIds = mutableListOf<Long>()

        imageUrls.forEachIndexed { index, imageUrl ->
            val fileName = "${filePrefix}_${index + 1}.jpg"
            val downloadId = downloadImage(context, imageUrl, fileName, platform)
            downloadIds.add(downloadId)

            // 防止同时发起太多请求，稍微延迟
            Thread.sleep(100)
        }

        Timber.i("✅ 已加入 ${downloadIds.size} 个图片下载任务")

        return downloadIds
    }

    /**
     * 下载单张图片
     */
    private fun downloadImage(
        context: Context,
        imageUrl: String,
        fileName: String,
        platform: String
    ): Long {
        Timber.d("下载图片: $fileName")

        val downloadManager = context.getSystemService<DownloadManager>()
            ?: throw IllegalStateException("DownloadManager 不可用")

        val request = DownloadManager.Request(Uri.parse(imageUrl)).apply {
            setTitle(fileName)
            setDescription("正在下载图片...")

            // 添加 Referer
            PLATFORM_REFERERS[platform]?.let { referer ->
                addRequestHeader("Referer", referer)
            }

            // 添加 User-Agent
            addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")

            // 设置通知栏可见
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // 设置保存路径（Pictures 目录）
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_PICTURES,
                "TikHub/$fileName"
            )

            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )

            setAllowedOverRoaming(true)
        }

        val downloadId = downloadManager.enqueue(request)

        Timber.d("图片下载任务 ID: $downloadId")

        return downloadId
    }

    /**
     * 监听下载进度
     */
    private fun monitorDownload(context: Context, downloadId: Long, fileName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val downloadManager = context.getSystemService<DownloadManager>() ?: return@launch

            var isDownloading = true

            while (isDownloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor? = downloadManager.query(query)

                cursor?.use {
                    if (it.moveToFirst()) {
                        val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val bytesDownloadedIndex = it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        if (statusIndex != -1) {
                            val status = it.getInt(statusIndex)

                            when (status) {
                                DownloadManager.STATUS_RUNNING -> {
                                    if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                                        val bytesDownloaded = it.getLong(bytesDownloadedIndex)
                                        val bytesTotal = it.getLong(bytesTotalIndex)

                                        if (bytesTotal > 0) {
                                            val progress = (bytesDownloaded * 100 / bytesTotal).toInt()
                                            Timber.d("下载进度 [$fileName]: $progress% ($bytesDownloaded / $bytesTotal)")
                                        }
                                    }
                                }
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    Timber.i("✅ 下载完成: $fileName")
                                    isDownloading = false

                                    // 通知相册扫描
                                    notifyMediaScanner(context, downloadId)
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    val reasonIndex = it.getColumnIndex(DownloadManager.COLUMN_REASON)
                                    val reason = if (reasonIndex != -1) it.getInt(reasonIndex) else -1
                                    Timber.e("❌ 下载失败: $fileName, 原因代码: $reason")
                                    isDownloading = false
                                }
                                DownloadManager.STATUS_PAUSED -> {
                                    Timber.w("⏸️ 下载已暂停: $fileName")
                                }
                            }
                        }
                    }
                }

                delay(500) // 每 0.5 秒检查一次
            }
        }
    }

    /**
     * 通知相册扫描
     * 确保下载的文件在相册中可见
     */
    private fun notifyMediaScanner(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService<DownloadManager>() ?: return

        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)

        cursor?.use {
            if (it.moveToFirst()) {
                val uriIndex = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (uriIndex != -1) {
                    val localUri = it.getString(uriIndex)
                    if (localUri != null) {
                        val file = File(Uri.parse(localUri).path ?: return@use)

                        // 使用 MediaScannerConnection 扫描文件
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(file.absolutePath),
                            null
                        ) { path, uri ->
                            Timber.d("媒体扫描完成: $path")
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取 MIME 类型
     */
    private fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }

    /**
     * 查询下载状态
     */
    fun queryDownloadStatus(context: Context, downloadId: Long): Int {
        val downloadManager = context.getSystemService<DownloadManager>() ?: return -1

        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)

        return cursor?.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                if (statusIndex != -1) it.getInt(statusIndex) else -1
            } else {
                -1
            }
        } ?: -1
    }
}
