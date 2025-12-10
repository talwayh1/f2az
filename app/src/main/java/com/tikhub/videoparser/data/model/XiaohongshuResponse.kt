package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * 小红书 API 响应数据模型（根据实际 API 测试结果设计）
 *
 * 完整结构：
 * {
 *   "code": 200,
 *   "message": "Request successful...",
 *   "data": {
 *     "code": 0,
 *     "success": true,
 *     "message": "成功",
 *     "data": [
 *       {
 *         "model_type": "note",
 *         "note_list": [
 *           {
 *             "id": "692e52fc000000001f00dabf",
 *             "type": "video",
 *             "title": "电信广东卡",
 *             "desc": "...",
 *             "user": {...},
 *             "video": {...},
 *             "images_list": [...]
 *           }
 *         ]
 *       }
 *     ]
 *   }
 * }
 */
data class XiaohongshuNoteData(
    @SerializedName("code")
    val code: Int,

    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<XiaohongshuDataItem>? = null
)

data class XiaohongshuDataItem(
    @SerializedName("model_type")
    val modelType: String? = null,

    @SerializedName("note_list")
    val noteList: List<XiaohongshuNoteDetail>? = null
)

data class XiaohongshuNoteDetail(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("desc")
    val desc: String? = null,

    @SerializedName("type")
    val type: String? = null, // "video" 或 "normal"（图文）

    @SerializedName("time")
    val time: Long = 0,

    @SerializedName("ip_location")
    val ipLocation: String? = null,

    @SerializedName("user")
    val user: XiaohongshuUser? = null,

    @SerializedName("liked_count")
    val likedCount: Int = 0,

    @SerializedName("collected_count")
    val collectedCount: Int = 0,

    @SerializedName("comments_count")
    val commentsCount: Int = 0,

    @SerializedName("shared_count")
    val sharedCount: Int = 0,

    @SerializedName("view_count")
    val viewCount: Int = 0,

    @SerializedName("video")
    val video: XiaohongshuVideo? = null,

    @SerializedName("images_list")
    val imagesList: List<XiaohongshuImage>? = null,

    @SerializedName("share_info")
    val shareInfo: XiaohongshuShareInfo? = null
)

data class XiaohongshuUser(
    @SerializedName("userid")
    val userId: String? = null,

    @SerializedName("nickname")
    val nickname: String? = null,

    @SerializedName("image")
    val image: String? = null,

    @SerializedName("name")
    val name: String? = null
)

/**
 * 小红书视频信息（支持多质量）
 */
data class XiaohongshuVideo(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("avg_bitrate")
    val avgBitrate: Int = 0,

    /**
     * 多质量视频列表（H264/H265）
     * 根据实际测试，小红书返回 url_info_list 包含多个质量版本
     */
    @SerializedName("url_info_list")
    val urlInfoList: List<XiaohongshuVideoUrl>? = null
)

/**
 * 小红书视频质量选项
 */
data class XiaohongshuVideoUrl(
    @SerializedName("desc")
    val desc: String? = null, // "h264-RedH264" 或 "h265-RedH265"

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("avg_bitrate")
    val avgBitrate: Int = 0,

    @SerializedName("vmaf")
    val vmaf: Int = -1
)

data class XiaohongshuImage(
    @SerializedName("fileid")
    val fileId: String? = null,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("original")
    val original: String? = null,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("index")
    val index: Int = 0,

    // 🎯 新增：Live Photo 实况视频 URL（小红书特有功能）
    @SerializedName("live_photo")
    val livePhoto: XiaohongshuLivePhoto? = null
)

/**
 * 小红书 Live Photo（实况照片）数据结构
 * Live Photo 是静态图片 + 短视频的组合
 */
data class XiaohongshuLivePhoto(
    @SerializedName("url")
    val url: String? = null,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0
)

data class XiaohongshuShareInfo(
    @SerializedName("link")
    val link: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("content")
    val content: String? = null
)

/**
 * 将小红书响应转换为通用 ParseResult
 */
fun XiaohongshuNoteData.toParseResult(): ParseResult {
    // 安全地获取笔记详情：data[0].note_list[0]
    val noteDetail = data?.firstOrNull()?.noteList?.firstOrNull()
        ?: throw IllegalStateException("小红书响应数据为空或格式不正确")

    val isVideo = noteDetail.type == "video" && noteDetail.video != null

    // 视频处理：优先使用 H264 高质量版本
    val videoUrl = if (isVideo && noteDetail.video != null) {
        // 优先级：url_info_list 中的 H264 > 默认 url
        val h264Url = noteDetail.video.urlInfoList
            ?.firstOrNull { it.desc?.contains("h264", ignoreCase = true) == true }
            ?.url

        val h265Url = noteDetail.video.urlInfoList
            ?.firstOrNull { it.desc?.contains("h265", ignoreCase = true) == true }
            ?.url

        // 选择顺序：H264（兼容性好） > H265（省流量） > 默认 URL
        h264Url ?: h265Url ?: noteDetail.video.url
    } else null

    return ParseResult(
        type = if (isVideo) "video" else "image",
        title = noteDetail.title ?: noteDetail.desc ?: "无标题",
        desc = noteDetail.desc,
        author = noteDetail.user?.let {
            AuthorInfo(
                uid = it.userId ?: "",
                nickname = it.nickname ?: it.name ?: "未知用户",
                avatar = it.image,
                signature = noteDetail.ipLocation // 使用地理位置作为签名
            )
        },
        statistics = Statistics(
            likeCount = noteDetail.likedCount.toLong(),
            commentCount = noteDetail.commentsCount.toLong(),
            shareCount = noteDetail.sharedCount.toLong(),
            collectCount = noteDetail.collectedCount.toLong(),
            playCount = noteDetail.viewCount.toLong()
        ),
        video = if (isVideo && noteDetail.video != null) {
            VideoInfo(
                playUrl = videoUrl,
                downloadUrl = videoUrl,
                cover = noteDetail.imagesList?.firstOrNull()?.url,
                dynamicCover = null,
                duration = noteDetail.video.duration,
                width = noteDetail.video.width,
                height = noteDetail.video.height,
                ratio = if (noteDetail.video.width > 0 && noteDetail.video.height > 0) {
                    "${noteDetail.video.width}:${noteDetail.video.height}"
                } else null,
                bitrate = noteDetail.video.avgBitrate.toLong()
            )
        } else null,
        images = if (!isVideo && !noteDetail.imagesList.isNullOrEmpty()) {
            noteDetail.imagesList.mapNotNull { img ->
                val url = img.original ?: img.url ?: return@mapNotNull null
                ImageInfo(
                    url = url,
                    width = img.width,
                    height = img.height,
                    size = 0  // API 未提供，UI 层可异步获取
                )
            }
        } else null,
        shareUrl = noteDetail.shareInfo?.link,
        createTime = noteDetail.time
    )
}
