package com.tikhub.videoparser.utils

/**
 * 支持的视频平台
 */
enum class Platform(val displayName: String, val apiParam: String) {
    // 短视频三巨头
    DOUYIN("抖音", "douyin"),
    TIKTOK("TikTok", "tiktok"),
    KUAISHOU("快手", "kuaishou"),

    // 图文/社区平台
    XIAOHONGSHU("小红书", "xiaohongshu"),
    WEIBO("微博", "weibo"),
    INSTAGRAM("Instagram", "instagram"),

    // 长视频/横屏平台
    BILIBILI("哔哩哔哩", "bilibili"),
    XIGUA("西瓜视频", "xigua"),
    YOUTUBE("YouTube", "youtube"),

    // 其他平台
    WEISHI("微视", "weishi"),
    UNKNOWN("未知平台", "unknown");

    companion object {
        /**
         * 根据 URL 识别平台
         *
         * @param url 完整的视频链接（短链或长链）
         * @return 平台枚举
         */
        fun detect(url: String): Platform {
            val lowerUrl = url.lowercase()

            return when {
                // 抖音
                lowerUrl.contains("douyin.com") ||
                lowerUrl.contains("iesdouyin.com") -> DOUYIN

                // TikTok
                lowerUrl.contains("tiktok.com") -> TIKTOK

                // 小红书
                lowerUrl.contains("xiaohongshu.com") ||
                lowerUrl.contains("xhslink.com") -> XIAOHONGSHU

                // 快手（包含重定向域名 chenzhongtech.com）
                lowerUrl.contains("kuaishou.com") ||
                lowerUrl.contains("kw.ai") ||
                lowerUrl.contains("ksurl.cn") ||
                lowerUrl.contains("chenzhongtech.com") -> KUAISHOU

                // B站（哔哩哔哩）
                lowerUrl.contains("bilibili.com") ||
                lowerUrl.contains("b23.tv") -> BILIBILI

                // 微博（新增）
                lowerUrl.contains("weibo.com") ||
                lowerUrl.contains("weibo.cn") -> WEIBO

                // 西瓜视频（新增）
                lowerUrl.contains("ixigua.com") ||
                lowerUrl.contains("toutiao.com/video") -> XIGUA

                // Instagram（新增）
                lowerUrl.contains("instagram.com") ||
                lowerUrl.contains("instagr.am") -> INSTAGRAM

                // YouTube（新增）
                lowerUrl.contains("youtube.com") ||
                lowerUrl.contains("youtu.be") -> YOUTUBE

                // 微视
                lowerUrl.contains("weishi.qq.com") -> WEISHI

                // 未知平台
                else -> UNKNOWN
            }
        }
    }
}

/**
 * 平台检测工具（扩展功能）
 */
object PlatformDetector {

    /**
     * 检查 URL 是否为支持的平台
     */
    fun isSupported(url: String): Boolean {
        return Platform.detect(url) != Platform.UNKNOWN
    }

    /**
     * 获取平台的显示名称
     */
    fun getDisplayName(url: String): String {
        return Platform.detect(url).displayName
    }

    /**
     * 获取平台的 API 参数（用于调用 TikHub API）
     */
    fun getApiParam(url: String): String {
        return Platform.detect(url).apiParam
    }

    /**
     * 批量检测 URL 列表
     * @return Map<URL, Platform>
     */
    fun detectAll(urls: List<String>): Map<String, Platform> {
        return urls.associateWith { Platform.detect(it) }
    }

    /**
     * 从混合文本中提取并识别平台
     */
    fun extractAndDetect(text: String): List<Pair<String, Platform>> {
        val urls = UrlExtractor.extractUrls(text)
        return urls.map { it to Platform.detect(it) }
    }
}
