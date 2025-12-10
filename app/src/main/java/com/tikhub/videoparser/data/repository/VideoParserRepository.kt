package com.tikhub.videoparser.data.repository

import com.google.gson.Gson
import com.tikhub.videoparser.data.api.TikHubApiService
import com.tikhub.videoparser.data.mapper.MediaMapper
import com.tikhub.videoparser.data.model.InstagramPostData
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.data.model.ParseResultWrapper
import com.tikhub.videoparser.data.model.XiguaVideoData
import com.tikhub.videoparser.data.model.YouTubeVideoData
import com.tikhub.videoparser.utils.ApiConstants
import com.tikhub.videoparser.utils.CostCalculator
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
     * 6. 计算耗时和费用
     *
     * @param input 用户输入的文本（可能包含多个链接、描述等）
     * @return Result<ParseResultWrapper> 成功返回包含解析结果、耗时和费用的包装对象
     */
    suspend fun parse(input: String): Result<ParseResultWrapper> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var apiCallCount = 1  // API 调用次数

        try {
            Timber.d("========== Repository 解析流程开始 ==========")

            // Step 1: 提取 URL
            val step1Start = System.currentTimeMillis()
            val urls = UrlExtractor.extractUrls(input)
            val step1Time = System.currentTimeMillis() - step1Start
            Timber.d("⏱️ Step 1: 提取 URL 耗时 ${step1Time}ms，提取到 ${urls.size} 个 URL")

            if (urls.isEmpty()) {
                Timber.w("❌ 未找到有效链接")
                return@withContext Result.failure(Exception("未找到有效链接"))
            }

            val shortUrl = urls.first()
            Timber.d("📎 使用第一个链接: $shortUrl")

            // Step 2: 短链追踪
            val step2Start = System.currentTimeMillis()
            val realUrl = ShortLinkResolver.resolve(shortUrl)
            val step2Time = System.currentTimeMillis() - step2Start
            Timber.i("⏱️ Step 2: 短链解析耗时 ${step2Time}ms")
            Timber.i("🔗 还原后的真实 URL: $realUrl")

            // Step 3: 平台识别
            val step3Start = System.currentTimeMillis()
            val platform = Platform.detect(realUrl)
            val step3Time = System.currentTimeMillis() - step3Start
            Timber.i("⏱️ Step 3: 平台识别耗时 ${step3Time}ms")
            Timber.i("🎯 识别平台: ${platform.displayName} (${platform.apiParam})")

            if (platform == Platform.UNKNOWN) {
                Timber.w("⚠️ 不支持的平台: $realUrl")
                return@withContext Result.failure(Exception("不支持的平台"))
            }

            // Step 4: 调用对应平台的解析方法
            val step4Start = System.currentTimeMillis()

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

            val step4Time = System.currentTimeMillis() - step4Start
            Timber.i("⏱️ Step 4: API 请求+数据映射耗时 ${step4Time}ms")

            // Step 5: 计算费用
            val step5Start = System.currentTimeMillis()
            val estimatedCost = CostCalculator.calculateCost(platform, apiCallCount)
            val step5Time = System.currentTimeMillis() - step5Start

            val totalTime = System.currentTimeMillis() - startTime

            Timber.d("========== Repository 解析流程结束 ==========")
            Timber.i("⏱️ 【性能统计】总耗时=${totalTime}ms")
            Timber.i("  ├─ URL提取: ${step1Time}ms (${step1Time * 100 / totalTime}%)")
            Timber.i("  ├─ 短链解析: ${step2Time}ms (${step2Time * 100 / totalTime}%)")
            Timber.i("  ├─ 平台识别: ${step3Time}ms (${step3Time * 100 / totalTime}%)")
            Timber.i("  ├─ API+映射: ${step4Time}ms (${step4Time * 100 / totalTime}%)")
            Timber.i("  └─ 费用计算: ${step5Time}ms (${step5Time * 100 / totalTime}%)")
            Timber.d("💰 费用统计: ${CostCalculator.formatCost(estimatedCost)}")

            // Step 6: 包装结果
            result.map { media ->
                ParseResultWrapper(
                    media = media,
                    parseTimeMs = totalTime,
                    networkTimeMs = step4Time,  // API+映射时间作为网络时间
                    estimatedCostCNY = estimatedCost
                )
            }

        } catch (e: Exception) {
            Timber.e(e, "💥 Repository 解析过程发生异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 抖音解析（优化版 - 使用通用轮询器）
    // ========================================

    private suspend fun parseDouyin(url: String): Result<ParsedMedia> {
        Timber.d("📱 准备解析抖音")

        return try {
            val awemeId = extractVideoId(url, "douyin")
            if (awemeId.isEmpty()) {
                Timber.w("❌ 无法从 URL 中提取抖音 ID: $url")
                return Result.failure(Exception("无法提取抖音 ID，请检查链接格式"))
            }

            Timber.d("🔑 抖音 ID: $awemeId")

            // 使用通用轮询器（代码量减少70%）
            EndpointPoller.poll(
                endpoints = listOf(
                    "抖音主接口(V3)" to suspend {
                        apiService.fetchDouyinVideo(awemeId, "Bearer ${ApiConstants.API_KEY}")
                    },
                    "抖音备用接口(V3_V2)" to suspend {
                        apiService.fetchDouyinVideoV2(awemeId, "Bearer ${ApiConstants.API_KEY}")
                    }
                ),
                mapper = { data -> MediaMapper.mapDouyin(data) },
                timeoutMs = 15000  // 15秒超时
            )

        } catch (e: Exception) {
            Timber.e(e, "💥 抖音解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // TikTok 解析（优化版）
    // ========================================

    private suspend fun parseTikTok(url: String): Result<ParsedMedia> {
        Timber.d("🎵 准备解析 TikTok")

        return try {
            val awemeId = extractVideoId(url, "tiktok")
            if (awemeId.isEmpty()) {
                return Result.failure(Exception("无法提取 TikTok ID"))
            }

            Timber.d("🔑 TikTok ID: $awemeId")

            EndpointPoller.poll(
                endpoints = listOf(
                    "TikTok主接口(V3)" to suspend {
                        apiService.fetchTikTokVideo(awemeId, "Bearer ${ApiConstants.API_KEY}")
                    },
                    "TikTok备用接口(V3_V2)" to suspend {
                        apiService.fetchTikTokVideoV2(awemeId, "Bearer ${ApiConstants.API_KEY}")
                    }
                ),
                mapper = { data -> MediaMapper.mapTikTok(data) }
            )

        } catch (e: Exception) {
            Timber.e(e, "💥 TikTok 解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 小红书解析（优化版）
    // ========================================

    private suspend fun parseXiaohongshu(url: String): Result<ParsedMedia> {
        Timber.d("📔 准备解析小红书")

        return try {
            // 提取笔记 ID
            val noteIdRegex = "/item/([a-f0-9]+)".toRegex()
            val matchResult = noteIdRegex.find(url)
            val noteId = matchResult?.groupValues?.get(1)

            if (noteId.isNullOrEmpty() || noteId.length < 10) {
                Timber.w("❌ 无法从 URL 中提取小红书笔记 ID: $url")

                val errorMessage = if (url.contains("xhslink.com")) {
                    "小红书短链接解析失败\n可能原因：链接已过期或失效\n建议：请使用完整链接"
                } else {
                    "无法提取小红书笔记 ID，请检查链接格式"
                }

                return Result.failure(Exception(errorMessage))
            }

            Timber.d("🔑 小红书笔记 ID: $noteId")

            EndpointPoller.poll(
                endpoints = listOf(
                    "小红书主接口(App)" to suspend {
                        apiService.fetchXiaohongshuNote(noteId, "Bearer ${ApiConstants.API_KEY}")
                    },
                    "小红书备用接口(Web)" to suspend {
                        apiService.fetchXiaohongshuNoteWeb(noteId, "Bearer ${ApiConstants.API_KEY}")
                    }
                ),
                mapper = { data -> MediaMapper.mapXiaohongshu(data) }
            )

        } catch (e: retrofit2.HttpException) {
            // HTTP 错误特殊处理
            Timber.e(e, "💥 小红书 HTTP 异常: ${e.code()}")
            val friendlyMessage = when (e.code()) {
                400 -> "该笔记无法获取，可能已删除或设置为私密"
                404 -> "该笔记不存在或已被删除"
                403 -> "访问被拒绝，该笔记可能设置了隐私保护"
                500 -> "服务器错误，请稍后重试"
                else -> "HTTP ${e.code()}: ${e.message()}"
            }
            Result.failure(Exception(friendlyMessage))
        } catch (e: Exception) {
            Timber.e(e, "💥 小红书解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // 快手解析（优化版）
    // ========================================

    private suspend fun parseKuaishou(url: String): Result<ParsedMedia> {
        Timber.d("⚡ 准备解析快手")

        return try {
            val photoId = extractVideoId(url, "kuaishou")
            if (photoId.isEmpty()) {
                return Result.failure(Exception("无法提取快手视频 ID"))
            }

            Timber.d("🔑 快手视频 ID: $photoId")

            EndpointPoller.poll(
                endpoints = listOf(
                    "快手主接口(App)" to suspend {
                        apiService.fetchKuaishouVideo(photoId, "Bearer ${ApiConstants.API_KEY}")
                    },
                    "快手备用接口(Web V2)" to suspend {
                        apiService.fetchKuaishouVideoV2(photoId, "Bearer ${ApiConstants.API_KEY}")
                    }
                ),
                mapper = { data -> MediaMapper.mapKuaishou(data) }
            )

        } catch (e: Exception) {
            Timber.e(e, "💥 快手解析异常")
            Result.failure(e)
        }
    }

    // ========================================
    // B站解析（优化版）
    // ========================================

    private suspend fun parseBilibili(url: String): Result<ParsedMedia> {
        Timber.d("📺 准备解析B站")

        return try {
            val bvId = extractVideoId(url, "bilibili")
            if (bvId.isEmpty()) {
                return Result.failure(Exception("无法提取 BV号"))
            }

            Timber.d("🔑 B站 BV号: $bvId")

            EndpointPoller.poll(
                endpoints = listOf(
                    "B站主接口(Web)" to suspend {
                        apiService.fetchBilibiliVideo(bvId, "Bearer ${ApiConstants.API_KEY}")
                    },
                    "B站备用接口(App)" to suspend {
                        apiService.fetchBilibiliVideoV2(bvId, "Bearer ${ApiConstants.API_KEY}")
                    }
                ),
                mapper = { data -> MediaMapper.mapBilibili(data) }
            )

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
                Result.failure(Exception(response.message))
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
                // 将 JsonObject 转换为 XiguaVideoData
                val xiguaData = gson.fromJson(response.data, XiguaVideoData::class.java)
                val media = MediaMapper.mapXigua(xiguaData)
                Timber.i("✅ 西瓜视频解析成功: ${media::class.simpleName}")
                Result.success(media)
            } else {
                Result.failure(Exception(response.message))
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
                // 将 JsonObject 转换为 InstagramPostData
                val instagramData = gson.fromJson(response.data, InstagramPostData::class.java)
                val media = MediaMapper.mapInstagram(instagramData)
                Timber.i("✅ Instagram 解析成功: ${media::class.simpleName}")
                Result.success(media)
            } else {
                Result.failure(Exception(response.message))
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
                // 将 JsonObject 转换为 YouTubeVideoData
                val youtubeData = gson.fromJson(response.data, YouTubeVideoData::class.java)
                val media = MediaMapper.mapYouTube(youtubeData)
                Timber.i("✅ YouTube 解析成功: ${media::class.simpleName}")
                Result.success(media)
            } else {
                Result.failure(Exception(response.message))
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
                // 支持抖音视频和图文笔记两种格式
                // video格式: https://www.douyin.com/video/7123456789
                // note格式: https://www.douyin.com/note/7123456789
                val videoRegex = "(?:video|note)/([0-9]+)".toRegex()
                videoRegex.find(url)?.groupValues?.get(1) ?: ""
            }
            "tiktok" -> {
                val regex = "video/([0-9]+)".toRegex()
                regex.find(url)?.groupValues?.get(1) ?: ""
            }
            "kuaishou" -> {
                // 支持多种快手URL格式
                // 1. 短链接: https://v.kuaishou.com/J8J2I2lL
                // 2. 完整链接: https://www.kuaishou.com/photo/xxxxx
                // 3. 短视频: https://www.kuaishou.com/short-video/xxxxx
                // 4. 参数形式: ?photoId=xxxxx

                // 尝试短链接格式 (v.kuaishou.com/xxxxx)
                val shortLinkRegex = "v\\.kuaishou\\.com/([a-zA-Z0-9_-]+)".toRegex()
                shortLinkRegex.find(url)?.groupValues?.get(1)?.let { return it }

                Timber.d("🔍 开始提取快手视频ID，原始URL: $url")

                // 🎯 修复1：尝试短视频格式 (/short-video/xxxxx)
                val shortVideoRegex = "/short-video/([a-zA-Z0-9_-]+)".toRegex()
                shortVideoRegex.find(url)?.groupValues?.get(1)?.let {
                    Timber.d("✅ 匹配短视频格式: $it")
                    return it
                }

                // 🎯 修复2：尝试photo格式 (/photo/xxxxx)
                val photoRegex = "/photo/([a-zA-Z0-9_-]+)".toRegex()
                photoRegex.find(url)?.groupValues?.get(1)?.let {
                    Timber.d("✅ 匹配photo格式: $it")
                    return it
                }

                // 🎯 修复3：尝试参数格式 (?photoId=xxxxx 或 &photoId=xxxxx)
                val photoIdRegex = "[?&]photoId=([a-zA-Z0-9_-]+)".toRegex()
                photoIdRegex.find(url)?.groupValues?.get(1)?.let {
                    Timber.d("✅ 匹配参数格式: $it")
                    return it
                }

                // 🎯 修复4：尝试 chenzhongtech.com 域名的格式 (/fw/photo/xxxxx)
                val chenzhongtechRegex = "/fw/photo/([a-zA-Z0-9_-]+)".toRegex()
                chenzhongtechRegex.find(url)?.groupValues?.get(1)?.let {
                    Timber.d("✅ 匹配chenzhongtech格式: $it")
                    return it
                }

                // 🎯 修复5：尝试从 URL 路径中提取最后一段（通用兜底方案）
                // 例如：https://www.kuaishou.com/f/X8kQz9w8Abc -> X8kQz9w8Abc
                val pathSegmentRegex = "/([a-zA-Z0-9_-]{8,})(?:[?#]|$)".toRegex()
                pathSegmentRegex.find(url)?.groupValues?.get(1)?.let {
                    Timber.d("✅ 匹配路径段格式: $it")
                    return it
                }

                // 如果都不匹配，记录详细日志用于调试
                Timber.e("❌ 无法从快手URL提取视频ID")
                Timber.e("原始URL: $url")
                Timber.e("尝试的格式: 短视频(/short-video/), photo(/photo/), 参数(?photoId=), chenzhongtech(/fw/photo/), 路径段")

                return ""
            }
            "bilibili" -> {
                // B站BV号格式：BV + 10位字符（大小写字母和数字）
                // 支持的URL格式：
                // 1. https://www.bilibili.com/video/BV1xx411c7mD
                // 2. https://m.bilibili.com/video/BV1xx411c7mD
                // 3. https://b23.tv/BV1xx411c7mD (短链接展开后)
                // 4. https://www.bilibili.com/video/BV1xx411c7mD?p=1 (带参数)

                val bvRegex = "(BV[1-9A-HJ-NP-Za-km-z]{10})".toRegex()
                val bvId = bvRegex.find(url)?.groupValues?.get(1) ?: ""

                if (bvId.isNotEmpty()) {
                    Timber.d("✅ 成功提取B站BV号: $bvId")
                } else {
                    Timber.w("⚠️ 无法从URL提取BV号: $url")
                }

                bvId
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
