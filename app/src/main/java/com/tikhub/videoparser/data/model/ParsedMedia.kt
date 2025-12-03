package com.tikhub.videoparser.data.model

import java.util.Locale

/**
 * 统一的内容数据模型（密封类）
 *
 * 优势：
 * 1. 类型安全：编译时就能确保处理所有情况
 * 2. 清晰的分类：Video 和 ImageNote 是完全不同的类型
 * 3. 易于扩展：未来可以添加新类型如 Music、Article 等
 * 4. UI 友好：when 表达式可以智能分发不同的布局
 *
 * 设计思路来自技术指导文档的"统一数据模型"章节
 */
sealed class ParsedMedia {
    // 所有内容类型的共同属性
    abstract val id: String
    abstract val platform: String          // 平台标识：douyin, tiktok, xiaohongshu等
    abstract val authorName: String        // 作者昵称
    abstract val authorAvatar: String      // 作者头像 URL
    abstract val title: String             // 标题/描述文案
    abstract val coverUrl: String          // 封面图 URL（用于列表展示）
    abstract val stats: StatsInfo          // 统计信息（点赞/播放等）
    abstract val createTime: Long?         // 创建时间戳
    abstract val shareUrl: String?         // 原始分享链接

    /**
     * 视频类型
     * 适用平台：抖音、TikTok、快手、B站、西瓜视频、YouTube等
     */
    data class Video(
        override val id: String,
        override val platform: String,
        override val authorName: String,
        override val authorAvatar: String,
        override val title: String,
        override val coverUrl: String,
        override val stats: StatsInfo,
        override val createTime: Long? = null,
        override val shareUrl: String? = null,

        // 视频特有属性
        val videoUrl: String,              // 无水印视频直链
        val duration: Int = 0,             // 时长（秒）
        val width: Int = 0,                // 宽度（px）
        val height: Int = 0,               // 高度（px）
        val fileSize: Long = 0,            // 文件大小（字节）
        val bitrate: Long = 0,             // 码率（bps）
        val musicUrl: String? = null,      // 背景音乐链接（可选）
        val musicTitle: String? = null     // 音乐标题（可选）
    ) : ParsedMedia() {

        /**
         * 获取视频宽高比描述
         */
        fun getAspectRatioDescription(): String {
            return when {
                height == 0 -> "未知"
                width.toFloat() / height > 1.5 -> "横屏 (16:9)"
                width.toFloat() / height < 0.75 -> "竖屏 (9:16)"
                else -> "方形 (1:1)"
            }
        }

        /**
         * 获取文件大小的可读格式
         */
        fun getReadableFileSize(): String {
            return when {
                fileSize < 1024 -> "$fileSize B"
                fileSize < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", fileSize / 1024.0)
                fileSize < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", fileSize / (1024.0 * 1024))
                else -> String.format(Locale.US, "%.1f GB", fileSize / (1024.0 * 1024 * 1024))
            }
        }

        /**
         * 获取时长的可读格式 (MM:SS)
         */
        fun getFormattedDuration(): String {
            val minutes = duration / 60
            val seconds = duration % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 图文类型
     * 适用平台：小红书、Instagram、微博（九宫格）、抖音图文等
     */
    data class ImageNote(
        override val id: String,
        override val platform: String,
        override val authorName: String,
        override val authorAvatar: String,
        override val title: String,
        override val coverUrl: String,
        override val stats: StatsInfo,
        override val createTime: Long? = null,
        override val shareUrl: String? = null,

        // 图文特有属性
        val imageUrls: List<String>,       // 无水印图片列表（原图）
        val imageSizes: List<ImageSize>? = null  // 图片尺寸信息（可选）
    ) : ParsedMedia() {

        /**
         * 获取图片数量描述
         */
        fun getImageCountDescription(): String {
            return when (imageUrls.size) {
                1 -> "单图"
                in 2..3 -> "${imageUrls.size}图"
                in 4..9 -> "九宫格 ${imageUrls.size}图"
                else -> "${imageUrls.size}图"
            }
        }

        /**
         * 是否是多图笔记
         */
        fun isMultipleImages(): Boolean = imageUrls.size > 1
    }
}

/**
 * 统计信息（所有平台通用）
 */
data class StatsInfo(
    val likeCount: Long = 0,      // 点赞数
    val commentCount: Long = 0,    // 评论数
    val shareCount: Long = 0,      // 分享数
    val collectCount: Long = 0,    // 收藏数
    val playCount: Long = 0        // 播放数（视频专用）
) {
    /**
     * 获取格式化的统计文本（用于UI展示）
     * 例如："❤ 1.2w · 💬 523 · ▶ 12.5w"
     */
    fun getFormattedStats(): String {
        val parts = mutableListOf<String>()

        if (likeCount > 0) {
            parts.add("❤ ${formatCount(likeCount)}")
        }
        if (commentCount > 0) {
            parts.add("💬 ${formatCount(commentCount)}")
        }
        if (playCount > 0) {
            parts.add("▶ ${formatCount(playCount)}")
        }

        return parts.joinToString(" · ")
    }

    private fun formatCount(count: Long): String {
        return when {
            count < 1000 -> count.toString()
            count < 10000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
            count < 100000000 -> String.format(Locale.US, "%.1fw", count / 10000.0)
            else -> String.format(Locale.US, "%.1f亿", count / 100000000.0)
        }
    }
}

/**
 * 图片尺寸信息
 */
data class ImageSize(
    val width: Int,
    val height: Int,
    val fileSize: Long = 0  // 字节
)

/**
 * 扩展函数：从旧的 ParseResult 转换为 ParsedMedia
 * 用于兼容性迁移
 */
fun ParseResult.toNewModel(platform: String): ParsedMedia {
    val stats = StatsInfo(
        likeCount = statistics?.likeCount ?: 0,
        commentCount = statistics?.commentCount ?: 0,
        shareCount = statistics?.shareCount ?: 0,
        collectCount = statistics?.collectCount ?: 0,
        playCount = statistics?.playCount ?: 0
    )

    return if (isVideo()) {
        ParsedMedia.Video(
            id = shareUrl ?: System.currentTimeMillis().toString(),
            platform = platform,
            authorName = author?.nickname ?: "未知作者",
            authorAvatar = author?.avatar ?: "",
            title = getDisplayTitle(),
            coverUrl = video?.cover ?: "",
            stats = stats,
            createTime = createTime,
            shareUrl = shareUrl,
            videoUrl = video?.getNoWatermarkUrl() ?: "",
            duration = video?.duration ?: 0,
            width = video?.width ?: 0,
            height = video?.height ?: 0,
            fileSize = video?.size ?: 0,
            bitrate = video?.bitrate ?: 0
        )
    } else {
        ParsedMedia.ImageNote(
            id = shareUrl ?: System.currentTimeMillis().toString(),
            platform = platform,
            authorName = author?.nickname ?: "未知作者",
            authorAvatar = author?.avatar ?: "",
            title = getDisplayTitle(),
            coverUrl = images?.firstOrNull()?.url ?: "",
            stats = stats,
            createTime = createTime,
            shareUrl = shareUrl,
            imageUrls = images?.map { it.url } ?: emptyList(),
            imageSizes = images?.map { ImageSize(it.width, it.height, it.size) }
        )
    }
}
