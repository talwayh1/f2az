package com.tikhub.videoparser.download

import com.tikhub.videoparser.utils.Platform

/**
 * 平台特定的 HTTP Headers 配置管理器
 *
 * 功能：
 * 1. 为每个平台提供专属的 User-Agent、Referer 等 HTTP 头
 * 2. 解决各平台 CDN 的防盗链和反爬虫机制
 * 3. 提高下载成功率，避免 403/429 错误
 *
 * 设计原则：
 * - 每个平台使用真实的浏览器/App User-Agent
 * - Referer 设置为平台官网，防止防盗链
 * - 支持动态扩展和自定义 Headers
 */
object PlatformHeadersConfig {

    /**
     * 平台 Headers 配置数据类
     */
    data class HeadersConfig(
        val userAgent: String,
        val referer: String,
        val additionalHeaders: Map<String, String> = emptyMap()
    )

    /**
     * 默认 User-Agent（通用移动端浏览器）
     */
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * 抖音专用 User-Agent（模拟抖音 App）
     */
    private const val DOUYIN_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 " +
        "Aweme/28.0.0"

    /**
     * TikTok 专用 User-Agent（模拟 TikTok App）
     */
    private const val TIKTOK_USER_AGENT =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Mobile/15E148 " +
        "TikTok/32.0.0"

    /**
     * 快手专用 User-Agent（模拟快手 App）
     */
    private const val KUAISHOU_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 " +
        "Kwai/11.0.0"

    /**
     * 小红书专用 User-Agent（模拟小红书 App）
     */
    private const val XIAOHONGSHU_USER_AGENT =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Mobile/15E148 " +
        "discover/7.50.0 XiaoHongShu/7.50.0"

    /**
     * B站专用 User-Agent（模拟 B站 App）
     */
    private const val BILIBILI_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 " +
        "BiliApp/7.50.0"

    /**
     * Instagram 专用 User-Agent（模拟 Instagram App）
     */
    private const val INSTAGRAM_USER_AGENT =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
        "(KHTML, like Gecko) Mobile/15E148 " +
        "Instagram 310.0.0.0.0"

    /**
     * YouTube 专用 User-Agent（模拟 YouTube App）
     */
    private const val YOUTUBE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 " +
        "YouTube/18.50.0"

    /**
     * 西瓜视频专用 User-Agent（模拟西瓜视频 App）
     */
    private const val XIGUA_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 " +
        "Xigua/5.0.0"

    /**
     * 获取平台专属的 Headers 配置
     *
     * @param platform 平台枚举
     * @return Headers 配置对象
     */
    fun getHeadersConfig(platform: Platform): HeadersConfig {
        return when (platform) {
            Platform.DOUYIN -> HeadersConfig(
                userAgent = DOUYIN_USER_AGENT,
                referer = "https://www.douyin.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "zh-CN,zh;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.TIKTOK -> HeadersConfig(
                userAgent = TIKTOK_USER_AGENT,
                referer = "https://www.tiktok.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.KUAISHOU -> HeadersConfig(
                userAgent = KUAISHOU_USER_AGENT,
                referer = "https://www.kuaishou.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "zh-CN,zh;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.XIAOHONGSHU -> HeadersConfig(
                userAgent = XIAOHONGSHU_USER_AGENT,
                referer = "https://www.xiaohongshu.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "zh-CN,zh;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.BILIBILI -> HeadersConfig(
                userAgent = BILIBILI_USER_AGENT,
                referer = "https://www.bilibili.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "zh-CN,zh;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.INSTAGRAM -> HeadersConfig(
                userAgent = INSTAGRAM_USER_AGENT,
                referer = "https://www.instagram.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive",
                    "X-Requested-With" to "XMLHttpRequest"
                )
            )

            Platform.YOUTUBE -> HeadersConfig(
                userAgent = YOUTUBE_USER_AGENT,
                referer = "https://www.youtube.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.XIGUA -> HeadersConfig(
                userAgent = XIGUA_USER_AGENT,
                referer = "https://www.ixigua.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "zh-CN,zh;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.WEIBO -> HeadersConfig(
                userAgent = DEFAULT_USER_AGENT,
                referer = "https://weibo.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "zh-CN,zh;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.WEISHI -> HeadersConfig(
                userAgent = DEFAULT_USER_AGENT,
                referer = "https://weishi.qq.com/",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "zh-CN,zh;q=0.9",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )

            Platform.UNKNOWN -> HeadersConfig(
                userAgent = DEFAULT_USER_AGENT,
                referer = "",
                additionalHeaders = mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "gzip, deflate, br",
                    "Connection" to "keep-alive"
                )
            )
        }
    }

    /**
     * 将 Headers 配置转换为 Map（用于传递给 Worker）
     */
    fun toMap(config: HeadersConfig): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["User-Agent"] = config.userAgent
        if (config.referer.isNotEmpty()) {
            headers["Referer"] = config.referer
        }
        headers.putAll(config.additionalHeaders)
        return headers
    }

    /**
     * 直接获取平台的 Headers Map
     */
    fun getHeadersMap(platform: Platform): Map<String, String> {
        return toMap(getHeadersConfig(platform))
    }
}
