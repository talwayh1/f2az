package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * Instagram API 响应数据模型
 * 对应 TikHub API: /api/v1/instagram/web/fetch_post_detail
 */
data class InstagramPostData(
    @SerializedName("items")
    val items: List<InstagramItem>? = null
)

data class InstagramItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("code")
    val code: String? = null,

    @SerializedName("caption")
    val caption: InstagramCaption? = null,

    @SerializedName("taken_at")
    val takenAt: Long = 0,

    @SerializedName("user")
    val user: InstagramUser,

    @SerializedName("media_type")
    val mediaType: Int = 0, // 1=图片, 2=视频, 8=轮播

    @SerializedName("image_versions2")
    val imageVersions: InstagramImageVersions? = null,

    @SerializedName("video_versions")
    val videoVersions: List<InstagramVideoVersion>? = null,

    @SerializedName("carousel_media")
    val carouselMedia: List<InstagramCarouselItem>? = null,

    @SerializedName("like_count")
    val likeCount: Long = 0,

    @SerializedName("comment_count")
    val commentCount: Long = 0,

    @SerializedName("play_count")
    val playCount: Long = 0,

    @SerializedName("view_count")
    val viewCount: Long = 0
)

data class InstagramCaption(
    @SerializedName("text")
    val text: String? = null
)

data class InstagramUser(
    @SerializedName("pk")
    val pk: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("full_name")
    val fullName: String? = null,

    @SerializedName("profile_pic_url")
    val profilePicUrl: String? = null,

    @SerializedName("is_verified")
    val isVerified: Boolean = false
)

data class InstagramImageVersions(
    @SerializedName("candidates")
    val candidates: List<InstagramImageCandidate>? = null
)

data class InstagramImageCandidate(
    @SerializedName("url")
    val url: String,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0
)

data class InstagramVideoVersion(
    @SerializedName("url")
    val url: String,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("type")
    val type: Int = 0
)

data class InstagramCarouselItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("media_type")
    val mediaType: Int = 0,

    @SerializedName("image_versions2")
    val imageVersions: InstagramImageVersions? = null,

    @SerializedName("video_versions")
    val videoVersions: List<InstagramVideoVersion>? = null
)
