package com.tikhub.videoparser.utils

/**
 * API 常量配置
 * 包含 TikHub API 的基础配置和密钥
 */
object ApiConstants {

    /**
     * TikHub API 基础地址
     * 🇨🇳 中国大陆用户使用：https://api.tikhub.dev/
     * 🌍 国际用户使用：https://api.tikhub.io/
     */
    const val BASE_URL = "https://api.tikhub.dev/"  // 默认使用中国镜像

    /**
     * API 密钥
     * 注意：应该从 DataStore 读取，这里只是默认值
     */
    const val API_KEY = "vZkdLfQ64g+1g0KsBrWdxRKNNhKee6Vi7xfXghXfVPimmlvnFiWIWWnCbA=="

    /**
     * API 端点路径
     */
    object Endpoints {
        // 抖音解析
        const val DOUYIN_PARSE = "api/v1/douyin/web/fetch_one_video"
        const val DOUYIN_PARSE_V2 = "api/v2/douyin/web/fetch_one_video"

        // TikTok 解析
        const val TIKTOK_PARSE = "api/v1/tiktok/web/fetch_one_video"

        // 小红书解析
        const val XIAOHONGSHU_PARSE = "api/v1/xiaohongshu/web/fetch_one_note"
        const val XIAOHONGSHU_PARSE_V2 = "api/v2/xiaohongshu/web/fetch_one_note"

        // 快手解析
        const val KUAISHOU_PARSE = "api/v1/kuaishou/web/fetch_one_video"
    }

    /**
     * Referer 配置（用于下载时绕过防盗链）
     */
    object Referers {
        const val DOUYIN = "https://www.douyin.com/"
        const val TIKTOK = "https://www.tiktok.com/"
        const val XIAOHONGSHU = "https://www.xiaohongshu.com/"
        const val KUAISHOU = "https://www.kuaishou.com/"

        /**
         * 根据平台获取 Referer
         */
        fun getReferer(platform: Platform): String {
            return when (platform) {
                Platform.DOUYIN -> DOUYIN
                Platform.TIKTOK -> TIKTOK
                Platform.XIAOHONGSHU -> XIAOHONGSHU
                Platform.KUAISHOU -> KUAISHOU
                else -> ""
            }
        }
    }

    /**
     * 超时配置（秒）
     * 优化：增加超时时间以应对网络不稳定情况
     */
    object Timeout {
        const val CONNECT = 20L  // 连接超时：20秒（应对网络延迟）
        const val READ = 30L     // 读取超时：30秒（应对大数据传输）
        const val WRITE = 30L    // 写入超时：30秒
    }

    /**
     * 下载配置
     */
    object Download {
        const val SAVE_DIR_VIDEOS = "TikHub/Videos"
        const val SAVE_DIR_IMAGES = "TikHub/Images"
        const val MAX_CONCURRENT_DOWNLOADS = 3
    }
}
