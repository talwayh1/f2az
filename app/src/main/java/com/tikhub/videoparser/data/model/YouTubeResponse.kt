package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * YouTube API 响应数据模型
 * 对应 TikHub API: /api/v1/youtube/web/fetch_video_detail
 */
data class YouTubeVideoData(
    @SerializedName("video_details")
    val videoDetails: YouTubeVideoDetails,

    @SerializedName("streaming_data")
    val streamingData: YouTubeStreamingData? = null
)

data class YouTubeVideoDetails(
    @SerializedName("video_id")
    val videoId: String,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("short_description")
    val shortDescription: String? = null,

    @SerializedName("length_seconds")
    val lengthSeconds: Int = 0,

    @SerializedName("channel_id")
    val channelId: String? = null,

    @SerializedName("author")
    val author: String? = null,

    @SerializedName("view_count")
    val viewCount: Long = 0,

    @SerializedName("thumbnail")
    val thumbnail: YouTubeThumbnail? = null,

    @SerializedName("is_live_content")
    val isLiveContent: Boolean = false
)

data class YouTubeThumbnail(
    @SerializedName("thumbnails")
    val thumbnails: List<YouTubeThumbnailItem>? = null
)

data class YouTubeThumbnailItem(
    @SerializedName("url")
    val url: String,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0
)

data class YouTubeStreamingData(
    @SerializedName("formats")
    val formats: List<YouTubeFormat>? = null,

    @SerializedName("adaptive_formats")
    val adaptiveFormats: List<YouTubeFormat>? = null
)

data class YouTubeFormat(
    @SerializedName("itag")
    val itag: Int,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("mime_type")
    val mimeType: String? = null,

    @SerializedName("bitrate")
    val bitrate: Int = 0,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("quality")
    val quality: String? = null,

    @SerializedName("fps")
    val fps: Int = 0,

    @SerializedName("quality_label")
    val qualityLabel: String? = null,

    @SerializedName("content_length")
    val contentLength: Long = 0
)

data class YouTubeChannelInfo(
    @SerializedName("channel_id")
    val channelId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("avatar")
    val avatar: String? = null
)
