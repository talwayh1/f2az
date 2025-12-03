package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * 快手 API 响应数据模型
 * 对应 TikHub API 返回的完整结构
 */
data class KuaishouVideoData(
    @SerializedName("photo")
    val photo: KuaishouPhoto
)

data class KuaishouPhoto(
    @SerializedName("photo_id")
    val photoId: String,

    @SerializedName("caption")
    val caption: String? = null,

    @SerializedName("timestamp")
    val timestamp: Long = 0,

    @SerializedName("user_info")
    val userInfo: KuaishouUserInfo,

    @SerializedName("view_count")
    val viewCount: Long = 0,

    @SerializedName("like_count")
    val likeCount: Long = 0,

    @SerializedName("comment_count")
    val commentCount: Long = 0,

    @SerializedName("share_count")
    val shareCount: Long = 0,

    @SerializedName("main_mv_urls")
    val mainMvUrls: List<KuaishouVideoUrl>? = null,

    @SerializedName("cover_urls")
    val coverUrls: List<KuaishouImageUrl>? = null,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("share_info")
    val shareInfo: KuaishouShareInfo? = null
)

data class KuaishouUserInfo(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("user_name")
    val userName: String,

    @SerializedName("user_text")
    val userText: String? = null,

    @SerializedName("head_url")
    val headUrl: String? = null
)

data class KuaishouVideoUrl(
    @SerializedName("url")
    val url: String? = null,

    @SerializedName("quality_tag")
    val qualityTag: String? = null
)

data class KuaishouImageUrl(
    @SerializedName("url")
    val url: String? = null
)

data class KuaishouShareInfo(
    @SerializedName("share_url")
    val shareUrl: String? = null
)

/**
 * 将快手响应转换为通用 ParseResult
 */
fun KuaishouVideoData.toParseResult(): ParseResult {
    val photo = this.photo

    return ParseResult(
        type = "video",
        title = photo.caption,
        desc = photo.caption,
        author = AuthorInfo(
            uid = photo.userInfo.userId,
            nickname = photo.userInfo.userName,
            avatar = photo.userInfo.headUrl,
            signature = photo.userInfo.userText
        ),
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
        shareUrl = photo.shareInfo?.shareUrl,
        createTime = photo.timestamp
    )
}
