package com.tikhub.videoparser.data.repository

import com.google.gson.Gson
import com.tikhub.videoparser.data.api.TikHubApiService
import com.tikhub.videoparser.data.mapper.MediaMapper
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.utils.ApiConstants
import com.tikhub.videoparser.utils.Platform
import com.tikhub.videoparser.utils.ShortLinkResolver
import com.tikhub.videoparser.utils.UrlExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 视频解析仓库层（升级版）
 *
 * 重大改进：
 * 1. 使用类型安全的 ParsedMedia 数据模型
 * 2. 通过 MediaMapper 统一数据转换
 * 3. 支持 11 个平台（8 → 11）
 * 4. 完善的错误处理和日志记录
 *
 * 支持的平台：
 * - 短视频：抖音、TikTok、快手
 * - 图文社区：小红书、微博、Instagram
 * - 长视频：B站、西瓜视频、YouTube
 * - 其他：微视
 */
@Singleton
class VideoParserRepository @Inject constructor(
    private val apiService: TikHubApiService,
    private val gson: Gson
) {

    /**
     * 解析链接（完整流程）
     *
     * 流程：
     * 1. 提取 URL
     * 2. 短链追踪（还原真实 URL）
     * 3. 平台识别
     * 4. 调用对应平台的 API
     * 5. 数据转换（通过 MediaMapper）
     *
     * @param input 用户输入的文本（可能包含多个链接、描述等）
     * @return Result<ParsedMedia> 成功返回统一的 ParsedMedia，失败返回异常
     */
    suspend fun parse(input: String): Result<ParsedMedia> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            Timber.d("========== Repository 解析流程开始 ==========")

            // Step 1: 提取 URL
            val urls = UrlExtractor.extractUrls(input)
            Timber.d("Step 1: 提取到 ${urls.size} 个 URL")

            if (urls.isEmpty()) {
                Timber.w("❌ 未找到有效链接")
                return@withContext Result.failure(Exception("未找到有效链接"))
            }

            val shortUrl = urls.first()
            Timber.d("📎 使用第一个链接: $shortUrl")

            // Step 2: 短链追踪
            val realUrl = ShortLinkResolver.resolve(shortUrl)
            Timber.i("🔗 还原后的真实 URL: $realUrl")

            // Step 3: 平台识别
            val platform = Platform.detect(realUrl)
            Timber.i("🎯 识别平台: ${platform.displayName} (${platform.apiParam})")

            if (platform == Platform.UNKNOWN) {
                Timber.w("⚠️ 不支持的平台: $realUrl")
                return@withContext Result.failure(Exception("不支持的平台"))
            }

            // Step 4: 调用对应平台的解析方法
            val networkStartTime = System.currentTimeMillis()

            val result = when (platform) {
                Platform.DOUYIN -> parseDouyin(realUrl)
                Platform.TIKTOK -> parseTikTok(realUrl)
                Platform.XIAOHONGSHU -> parseXiaohongshu(realUrl)
                Platform.KUAISHOU -> parseKuaishou(realUrl)
                Platform.BILIBILI -> parseBilibili(realUrl)
                Platform.WEIBO -> parseWeibo(realUrl)
                Platform.XIGUA -> parseXigua(realUrl)
                Platform.INSTAGRAM -> parseInstagram(realUrl)
                Platform.YOUTUBE -> parseYouTube(realUrl)
                else -> {
                    Timber.w("⚠️ 暂不支持该平台: $platform")
                    Result.failure(Exception("暂不支持该平台"))
                }
            }

            val networkEndTime = System.currentTimeMillis()
            val networkTime = networkEndTime - networkStartTime
            val totalTime = System.currentTimeMillis() - startTime

            Timber.d("========== Repository 解析流程结束 ==========")
            Timber.d("⏱️ 性能统计: 总耗时=${totalTime}ms, 网络=${networkTime}ms")

            result

        } catch (e: Exception) {
            Timber.e(e, "💥 Repository 解析过程发生异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 抖音解析
    // ========================================

    private suspend fun parseDouyin(url: String): Result<ParsedMedia> {
        Timber.d("📱 准备解析抖音")

        return try {
            val awemeId = extractVideoId(url, "douyin")
            if (awemeId.isEmpty()) {
                return Result.failure(Exception("无法提取抖音 ID"))
            }

            Timber.d("🔑 抖音 ID: $awemeId")

            // 接口轮询
            val endpoints = listOf(
                "主接口(V3)" to { apiService.fetchDouyinVideo(awemeId, "Bearer ${ApiConstants.API_KEY}") },
                "备用接口(V3_V2)" to { apiService.fetchDouyinVideoV2(awemeId, "Bearer ${ApiConstants.API_KEY}") }
            )

            for ((index, pair) in endpoints.withIndex()) {
                val (name, fetch) = pair
                try {
                    Timber.d("🔄 尝试 $name (${index + 1}/${endpoints.size})")
                    val response = fetch()

                    if (response.code == 200 && response.data != null) {
                        val media = MediaMapper.mapDouyin(response.data)
                        Timber.i("✅ 抖音解析成功: ${media::class.simpleName}")
                        return Result.success(media)
                    }

                    if (index == endpoints.lastIndex) {
                        return Result.failure(Exception(response.message ?: "所有接口均失败"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ $name 异常")
                    if (index == endpoints.lastIndex) return Result.failure(e)
                }
            }

            Result.failure(Exception("所有接口均失败"))

        } catch (e: Exception) {
            Timber.e(e, "💥 抖音解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // TikTok 解析
    // ========================================

    private suspend fun parseTikTok(url: String): Result<ParsedMedia> {
        Timber.d("🎵 准备解析 TikTok")

        return try {
            val awemeId = extractVideoId(url, "tiktok")
            if (awemeId.isEmpty()) {
                return Result.failure(Exception("无法提取 TikTok ID"))
            }

            Timber.d("🔑 TikTok ID: $awemeId")

            val endpoints = listOf(
                "主接口(V3)" to { apiService.fetchTikTokVideo(awemeId, "Bearer ${ApiConstants.API_KEY}") },
                "备用接口(V3_V2)" to { apiService.fetchTikTokVideoV2(awemeId, "Bearer ${ApiConstants.API_KEY}") }
            )

            for ((index, pair) in endpoints.withIndex()) {
                val (name, fetch) = pair
                try {
                    Timber.d("🔄 尝试 $name (${index + 1}/${endpoints.size})")
                    val response = fetch()

                    if (response.code == 200 && response.data != null) {
                        val media = MediaMapper.mapTikTok(response.data)
                        Timber.i("✅ TikTok 解析成功: ${media::class.simpleName}")
                        return Result.success(media)
                    }

                    if (index == endpoints.lastIndex) {
                        return Result.failure(Exception(response.message ?: "所有接口均失败"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ $name 异常")
                    if (index == endpoints.lastIndex) return Result.failure(e)
                }
            }

            Result.failure(Exception("所有接口均失败"))

        } catch (e: Exception) {
            Timber.e(e, "💥 TikTok 解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 小红书解析
    // ========================================

    private suspend fun parseXiaohongshu(url: String): Result<ParsedMedia> {
        Timber.d("📔 准备解析小红书")

        return try {
            val noteIdRegex = "/item/([a-f0-9]+)".toRegex()
            val noteId = noteIdRegex.find(url)?.groupValues?.get(1)

            if (noteId.isNullOrEmpty()) {
                return Result.failure(Exception("无法提取小红书笔记 ID"))
            }

            Timber.d("🔑 小红书笔记 ID: $noteId")

            val response = apiService.fetchXiaohongshuNote(
                noteId = noteId,
                authorization = "Bearer ${ApiConstants.API_KEY}"
            )

            if (response.code == 200 && response.data != null) {
                val media = MediaMapper.mapXiaohongshu(response.data)
                Timber.i("✅ 小红书解析成功: ${media::class.simpleName}")
                Result.success(media)
            } else {
                Result.failure(Exception(response.message ?: "解析失败"))
            }

        } catch (e: Exception) {
            Timber.e(e, "💥 小红书解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 快手解析
    // ========================================

    private suspend fun parseKuaishou(url: String): Result<ParsedMedia> {
        Timber.d("⚡ 准备解析快手")

        return try {
            val photoId = extractVideoId(url, "kuaishou")
            if (photoId.isEmpty()) {
                return Result.failure(Exception("无法提取快手视频 ID"))
            }

            Timber.d("🔑 快手视频 ID: $photoId")

            val endpoints = listOf(
                "主接口(App)" to { apiService.fetchKuaishouVideo(photoId, "Bearer ${ApiConstants.API_KEY}") },
                "备用接口(Web V2)" to { apiService.fetchKuaishouVideoV2(photoId, "Bearer ${ApiConstants.API_KEY}") }
            )

            for ((index, pair) in endpoints.withIndex()) {
                val (name, fetch) = pair
                try {
                    Timber.d("🔄 尝试 $name (${index + 1}/${endpoints.size})")
                    val response = fetch()

                    if (response.code == 200 && response.data != null) {
                        val media = MediaMapper.mapKuaishou(response.data)
                        Timber.i("✅ 快手解析成功: ${media::class.simpleName}")
                        return Result.success(media)
                    }

                    if (index == endpoints.lastIndex) {
                        return Result.failure(Exception(response.message ?: "所有接口均失败"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ $name 异常")
                    if (index == endpoints.lastIndex) return Result.failure(e)
                }
            }

            Result.failure(Exception("所有接口均失败"))

        } catch (e: Exception) {
            Timber.e(e, "💥 快手解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // B站解析
    // ========================================

    private suspend fun parseBilibili(url: String): Result<ParsedMedia> {
        Timber.d("📺 准备解析B站")

        return try {
            val bvId = extractVideoId(url, "bilibili")
            if (bvId.isEmpty()) {
                return Result.failure(Exception("无法提取 BV号"))
            }

            Timber.d("🔑 B站 BV号: $bvId")

            val endpoints = listOf(
                "主接口(Web)" to { apiService.fetchBilibiliVideo(bvId, "Bearer ${ApiConstants.API_KEY}") },
                "备用接口(App)" to { apiService.fetchBilibiliVideoV2(bvId, "Bearer ${ApiConstants.API_KEY}") }
            )

            for ((index, pair) in endpoints.withIndex()) {
                val (name, fetch) = pair
                try {
                    Timber.d("🔄 尝试 $name (${index + 1}/${endpoints.size})")
                    val response = fetch()

                    if (response.code == 200 && response.data != null) {
                        val media = MediaMapper.mapBilibili(response.data)
                        Timber.i("✅ B站解析成功: ${media::class.simpleName}")
                        return Result.success(media)
                    }

                    if (index == endpoints.lastIndex) {
                        return Result.failure(Exception(response.message ?: "所有接口均失败"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ $name 异常")
                    if (index == endpoints.lastIndex) return Result.failure(e)
                }
            }

            Result.failure(Exception("所有接口均失败"))

        } catch (e: Exception) {
            Timber.e(e, "💥 B站解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 微博解析（新增）
    // ========================================

    private suspend fun parseWeibo(url: String): Result<ParsedMedia> {
        Timber.d("📰 准备解析微博")

        return try {
            Timber.d("🔗 微博 URL: $url")

            val response = apiService.fetchWeiboPost(url, "Bearer ${ApiConstants.API_KEY}")

            if (response.code == 200 && response.data != null) {
                val media = MediaMapper.mapWeibo(response.data)
                Timber.i("✅ 微博解析成功: ${media::class.simpleName}")
                Result.success(media)
            } else {
                Result.failure(Exception(response.message ?: "解析失败"))
            }

        } catch (e: Exception) {
            Timber.e(e, "💥 微博解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 西瓜视频解析（新增）
    // ========================================

    private suspend fun parseXigua(url: String): Result<ParsedMedia> {
        Timber.d("🍉 准备解析西瓜视频")

        return try {
            Timber.d("🔗 西瓜视频 URL: $url")

            val response = apiService.fetchXiguaVideo(url, "Bearer ${ApiConstants.API_KEY}")

            if (response.code == 200 && response.data != null) {
                val media = MediaMapper.mapXigua(response.data)
                Timber.i("✅ 西瓜视频解析成功: ${media::class.simpleName}")
                Result.success(media)
            } else {
                Result.failure(Exception(response.message ?: "解析失败"))
            }

        } catch (e: Exception) {
            Timber.e(e, "💥 西瓜视频解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // Instagram 解析（新增）
    // ========================================

    private suspend fun parseInstagram(url: String): Result<ParsedMedia> {
        Timber.d("📷 准备解析 Instagram")

        return try {
            Timber.d("🔗 Instagram URL: $url")

            val response = apiService.fetchInstagramPost(url, "Bearer ${ApiConstants.API_KEY}")

            if (response.code == 200 && response.data != null) {
                val media = MediaMapper.mapInstagram(response.data)
                Timber.i("✅ Instagram 解析成功: ${media::class.simpleName}")
                Result.success(media)
            } else {
                Result.failure(Exception(response.message ?: "解析失败"))
            }

        } catch (e: Exception) {
            Timber.e(e, "💥 Instagram 解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // YouTube 解析（新增）
    // ========================================

    private suspend fun parseYouTube(url: String): Result<ParsedMedia> {
        Timber.d("▶️ 准备解析 YouTube")

        return try {
            val videoId = extractYouTubeVideoId(url)
            if (videoId.isEmpty()) {
                return Result.failure(Exception("无法提取 YouTube 视频 ID"))
            }

            Timber.d("🔑 YouTube 视频 ID: $videoId")

            val response = apiService.fetchYouTubeVideo(videoId, "Bearer ${ApiConstants.API_KEY}")

            if (response.code == 200 && response.data != null) {
                val media = MediaMapper.mapYouTube(response.data)
                Timber.i("✅ YouTube 解析成功: ${media::class.simpleName}")
                Result.success(media)
            } else {
                Result.failure(Exception(response.message ?: "解析失败"))
            }

        } catch (e: Exception) {
            Timber.e(e, "💥 YouTube 解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 辅助方法
    // ========================================

    /**
     * 从 URL 中提取视频 ID
     */
    private fun extractVideoId(url: String, platform: String): String {
        return when (platform.lowercase()) {
            "douyin" -> {
                val regex = "video/([0-9]+)".toRegex()
                regex.find(url)?.groupValues?.get(1) ?: ""
            }
            "tiktok" -> {
                val regex = "video/([0-9]+)".toRegex()
                regex.find(url)?.groupValues?.get(1) ?: ""
            }
            "kuaishou" -> {
                val photoRegex = "/photo/([a-zA-Z0-9_-]+)".toRegex()
                photoRegex.find(url)?.groupValues?.get(1)
                    ?: run {
                        val photoIdRegex = "[?&]photoId=([a-zA-Z0-9_-]+)".toRegex()
                        photoIdRegex.find(url)?.groupValues?.get(1) ?: ""
                    }
            }
            "bilibili" -> {
                val bvRegex = "(BV[a-zA-Z0-9]+)".toRegex()
                bvRegex.find(url)?.groupValues?.get(1) ?: ""
            }
            else -> ""
        }
    }

    /**
     * 提取 YouTube 视频 ID
     *
     * 支持格式：
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://www.youtube.com/embed/VIDEO_ID
     */
    private fun extractYouTubeVideoId(url: String): String {
        // 格式 1: youtube.com/watch?v=VIDEO_ID
        val watchRegex = "[?&]v=([a-zA-Z0-9_-]{11})".toRegex()
        watchRegex.find(url)?.let {
            return it.groupValues[1]
        }

        // 格式 2: youtu.be/VIDEO_ID
        val shortRegex = "youtu\\.be/([a-zA-Z0-9_-]{11})".toRegex()
        shortRegex.find(url)?.let {
            return it.groupValues[1]
        }

        // 格式 3: youtube.com/embed/VIDEO_ID
        val embedRegex = "embed/([a-zA-Z0-9_-]{11})".toRegex()
        embedRegex.find(url)?.let {
            return it.groupValues[1]
        }

        return ""
    }
}
