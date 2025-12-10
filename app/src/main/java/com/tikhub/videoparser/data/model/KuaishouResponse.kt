package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * 快手 API 响应数据模型
 * 对应 TikHub API 返回的完整结构
 */
data class KuaishouVideoData(
    @SerializedName("photo")
    val photo: KuaishouPhoto?  // 🎯 修复：photo 可能为 null
)

data class KuaishouPhoto(
    @SerializedName(value = "photoId", alternate = ["photo_id"])
    val photoId: String,

    @SerializedName("caption")
    val caption: String? = null,

    @SerializedName("timestamp")
    val timestamp: Long = 0,

    @SerializedName(value = "userInfo", alternate = ["user_info"])
    val userInfo: KuaishouUserInfo? = null,  // 🎯 修复：userInfo 可能为 null

    @SerializedName(value = "viewCount", alternate = ["view_count"])
    val viewCount: Long = 0,

    @SerializedName(value = "likeCount", alternate = ["like_count"])
    val likeCount: Long = 0,

    @SerializedName(value = "commentCount", alternate = ["comment_count"])
    val commentCount: Long = 0,

    @SerializedName(value = "shareCount", alternate = ["share_count"])
    val shareCount: Long = 0,

    @SerializedName(value = "mainMvUrls", alternate = ["main_mv_urls"])
    val mainMvUrls: List<KuaishouVideoUrl>? = null,

    @SerializedName(value = "coverUrls", alternate = ["cover_urls"])
    val coverUrls: List<KuaishouImageUrl>? = null,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName(value = "share_info", alternate = ["shareInfo"])
    val shareInfo: String? = null,  // API 返回字符串格式，如 "userId=xxx&photoId=xxx"

    // 🎯 新增：manifest 字段，包含多种清晰度的视频流
    @SerializedName("manifest")
    val manifest: KuaishouManifest? = null,

    // 🎯 新增：images 字段，用于图文内容
    @SerializedName("images")
    val images: List<KuaishouImage>? = null
)

data class KuaishouUserInfo(
    @SerializedName(value = "userId", alternate = ["user_id"])
    val userId: String,

    @SerializedName(value = "userName", alternate = ["user_name"])
    val userName: String,

    @SerializedName(value = "userText", alternate = ["user_text"])
    val userText: String? = null,

    @SerializedName(value = "headUrl", alternate = ["head_url"])
    val headUrl: String? = null
)

data class KuaishouVideoUrl(
    @SerializedName("url")
    val url: String? = null,

    @SerializedName(value = "qualityTag", alternate = ["quality_tag"])
    val qualityTag: String? = null,

    @SerializedName("cdn")
    val cdn: String? = null
)

data class KuaishouImageUrl(
    @SerializedName("url")
    val url: String? = null,

    @SerializedName("cdn")
    val cdn: String? = null
)

/**
 * 快手图片数据模型
 * 用于图文内容
 */
data class KuaishouImage(
    @SerializedName("url")
    val url: String? = null,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0
)

// KuaishouShareInfo 已移除，因为 API 返回的是字符串而不是对象

/**
 * 快手 Manifest 数据模型
 * 包含多种清晰度的视频流信息
 */
data class KuaishouManifest(
    @SerializedName("adaptationSet")
    val adaptationSet: List<KuaishouAdaptationSet>? = null,

    @SerializedName("videoId")
    val videoId: String? = null,

    @SerializedName("mediaType")
    val mediaType: Int = 0
)

/**
 * 快手 AdaptationSet 数据模型
 * 包含一组视频流
 */
data class KuaishouAdaptationSet(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("representation")
    val representation: List<KuaishouRepresentation>? = null
)

/**
 * 快手 Representation 数据模型
 * 代表一个具体的视频流（包含 URL、码率、分辨率等信息）
 */
data class KuaishouRepresentation(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("backupUrl")
    val backupUrl: List<String>? = null,

    @SerializedName("videoCodec")
    val videoCodec: String? = null,  // "avc" (H.264) 或 "hevc" (H.265)

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("maxBitrate")
    val maxBitrate: Int = 0,

    @SerializedName("avgBitrate")
    val avgBitrate: Int = 0,

    @SerializedName("fileSize")
    val fileSize: Long = 0,

    @SerializedName("qualityType")
    val qualityType: String? = null,  // "720p", "1080p" 等

    @SerializedName("qualityLabel")
    val qualityLabel: String? = null,  // "高清", "超清" 等

    @SerializedName("quality")
    val quality: Double = 0.0,

    @SerializedName("frameRate")
    val frameRate: Double = 0.0,  // 🎯 修复：TikHub API 返回浮点数（如 30.000269）

    @SerializedName("comment")
    val comment: String? = null,  // 包含编码信息，如 "AVC_VeryFast_720P_高码率_Basic"

    @SerializedName("defaultSelect")
    val defaultSelect: Boolean = false,

    @SerializedName("hidden")
    val hidden: Boolean = false
)

/**
 * 将快手响应转换为通用 ParseResult
 */
fun KuaishouVideoData.toParseResult(): ParseResult {
    val photo = this.photo
        ?: throw IllegalStateException("快手视频数据缺少 photo 字段")

    return ParseResult(
        type = "video",
        title = photo.caption,
        desc = photo.caption,
        author = photo.userInfo?.let {
            AuthorInfo(
                uid = it.userId,
                nickname = it.userName,
                avatar = it.headUrl,
                signature = it.userText
            )
        },
        statistics = Statistics(
            likeCount = photo.likeCount,
            commentCount = photo.commentCount,
            shareCount = photo.shareCount,
            collectCount = 0L,
            playCount = photo.viewCount
        ),
        video = VideoInfo(
            playUrl = photo.mainMvUrls?.firstOrNull()?.url,
            downloadUrl = photo.mainMvUrls?.firstOrNull()?.url,
            cover = photo.coverUrls?.firstOrNull()?.url,
            dynamicCover = null,
            duration = photo.duration / 1000, // 转换为秒
            width = photo.width,
            height = photo.height,
            ratio = null
        ),
        shareUrl = null,  // shareInfo 现在是字符串格式，不包含完整 URL
        createTime = photo.timestamp
    )
}
