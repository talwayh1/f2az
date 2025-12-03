package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * 西瓜视频 API 响应数据模型
 * 对应 TikHub API: /api/v1/xigua/app/v2/fetch_one_video
 */
data class XiguaVideoData(
    @SerializedName("item_info")
    val itemInfo: XiguaItemInfo
)

data class XiguaItemInfo(
    @SerializedName("item_id")
    val itemId: String,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("desc")
    val desc: String? = null,

    @SerializedName("create_time")
    val createTime: Long = 0,

    @SerializedName("author")
    val author: XiguaAuthor,

    @SerializedName("video")
    val video: XiguaVideo,

    @SerializedName("stats")
    val stats: XiguaStats,

    @SerializedName("share_url")
    val shareUrl: String? = null
)

data class XiguaAuthor(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @SerializedName("signature")
    val signature: String? = null
)

data class XiguaVideo(
    @SerializedName("play_addr")
    val playAddr: XiguaVideoUrl? = null,

    @SerializedName("download_addr")
    val downloadAddr: XiguaVideoUrl? = null,

    @SerializedName("cover")
    val cover: XiguaImageUrl? = null,

    @SerializedName("dynamic_cover")
    val dynamicCover: XiguaImageUrl? = null,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("ratio")
    val ratio: String? = null
)

data class XiguaVideoUrl(
    @SerializedName("url_list")
    val urlList: List<String>? = null,

    @SerializedName("uri")
    val uri: String? = null
)

data class XiguaImageUrl(
    @SerializedName("url_list")
    val urlList: List<String>? = null
)

data class XiguaStats(
    @SerializedName("digg_count")
    val diggCount: Long = 0,

    @SerializedName("comment_count")
    val commentCount: Long = 0,

    @SerializedName("share_count")
    val shareCount: Long = 0,

    @SerializedName("play_count")
    val playCount: Long = 0
)
