package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * 解析结果（视频/图文通用）
 */
data class ParseResult(
    // 内容类型
    @SerializedName("type")
    val type: String = "video", // video 或 image

    // 标题/描述
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("desc")
    val desc: String? = null,

    // 作者信息
    @SerializedName("author")
    val author: AuthorInfo? = null,

    // 统计数据
    @SerializedName("statistics")
    val statistics: Statistics? = null,

    // 视频信息（type=video 时有效）
    @SerializedName("video")
    val video: VideoInfo? = null,

    // 图片列表（type=image 时有效）
    @SerializedName("images")
    val images: List<ImageInfo>? = null,

    // 原始 URL
    @SerializedName("share_url")
    val shareUrl: String? = null,

    // 创建时间
    @SerializedName("create_time")
    val createTime: Long? = null,

    // 性能统计信息（新增）
    @SerializedName("performance")
    val performance: PerformanceInfo? = null,

    // API 调用信息（新增）
    @SerializedName("api_info")
    val apiInfo: ApiCallInfo? = null
) {
    /**
     * 判断是否为视频类型
     */
    fun isVideo(): Boolean = type == "video" && video != null

    /**
     * 判断是否为图文类型
     */
    fun isImageGallery(): Boolean = type == "image" && !images.isNullOrEmpty()

    /**
     * 获取显示标题
     */
    fun getDisplayTitle(): String {
        return when {
            !title.isNullOrBlank() -> title
            !desc.isNullOrBlank() -> desc
            else -> "无标题"
        }
    }
}

/**
 * 作者信息
 */
data class AuthorInfo(
    @SerializedName("uid")
    val uid: String? = null,

    @SerializedName("nickname")
    val nickname: String = "未知作者",

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("signature")
    val signature: String? = null
)

/**
 * 统计数据
 */
data class Statistics(
    @SerializedName("digg_count")
    val likeCount: Long = 0, // 点赞数

    @SerializedName("comment_count")
    val commentCount: Long = 0, // 评论数

    @SerializedName("share_count")
    val shareCount: Long = 0, // 分享数

    @SerializedName("download_count")
    val downloadCount: Long = 0, // 下载数

    @SerializedName("collect_count")
    val collectCount: Long = 0, // 收藏数

    @SerializedName("play_count")
    val playCount: Long = 0 // 播放数
)

/**
 * 视频信息
 */
data class VideoInfo(
    // 无水印视频 URL（核心）
    @SerializedName("play_addr")
    val playUrl: String? = null,

    // 带水印视频 URL（备用）
    @SerializedName("download_addr")
    val downloadUrl: String? = null,

    // 封面图
    @SerializedName("cover")
    val cover: String? = null,

    // 动态封面
    @SerializedName("dynamic_cover")
    val dynamicCover: String? = null,

    // 视频参数
    @SerializedName("duration")
    val duration: Int = 0, // 时长（秒）

    @SerializedName("width")
    val width: Int = 0, // 宽度

    @SerializedName("height")
    val height: Int = 0, // 高度

    @SerializedName("ratio")
    val ratio: String? = null, // 宽高比

    @SerializedName("size")
    val size: Long = 0, // 文件大小（字节）

    @SerializedName("bitrate")
    val bitrate: Long = 0 // 码率
) {
    /**
     * 获取无水印视频 URL
     * 核心逻辑：将 playwm 替换为 play（PHP 版本的逻辑）
     */
    fun getNoWatermarkUrl(): String? {
        val url = playUrl ?: downloadUrl ?: return null

        // 抖音/TikTok 去水印逻辑：将 playwm 替换为 play
        return if (url.contains("playwm")) {
            url.replace("playwm", "play")
        } else {
            url
        }
    }
}

/**
 * 性能统计信息
 */
data class PerformanceInfo(
    @SerializedName("total_time")
    val totalTime: Long = 0, // 总耗时（毫秒）

    @SerializedName("network_time")
    val networkTime: Long = 0, // 网络耗时（毫秒）

    @SerializedName("processing_time")
    val processingTime: Long = 0 // 处理耗时（毫秒）
)

/**
 * API 调用信息
 */
data class ApiCallInfo(
    @SerializedName("endpoint")
    val endpoint: String = "", // API 端点

    @SerializedName("platform")
    val platform: String = "", // 平台名称

    @SerializedName("cost")
    val cost: Double = 0.0, // 花费（如果API提供）

    @SerializedName("cached")
    val cached: Boolean = false // 是否使用缓存
)

/**
 * 图片信息（用于图文笔记）
 */
data class ImageInfo(
    @SerializedName("url")
    val url: String,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("size")
    val size: Long = 0 // 文件大小（字节）
)
