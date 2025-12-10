package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * B站（哔哩哔哩）API 响应数据模型
 *
 * 响应结构：
 * {
 *   "code": 200,
 *   "data": {
 *     "code": 0,
 *     "data": { ... }
 *   }
 * }
 */
data class BilibiliVideoData(
    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("ttl")
    val ttl: Int = 0,

    @SerializedName("data")
    val data: BilibiliVideo? = null
)

data class BilibiliVideo(
    @SerializedName("bvid")
    val bvid: String? = null,

    @SerializedName("aid")
    val aid: Long? = null,

    @SerializedName("videos")
    val videos: Int = 0,

    @SerializedName("tid")
    val tid: Int = 0,

    @SerializedName("tname")
    val tname: String? = null,

    @SerializedName("copyright")
    val copyright: Int = 0,

    @SerializedName("pic")
    val pic: String? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("pubdate")
    val pubdate: Long = 0,

    @SerializedName("ctime")
    val ctime: Long = 0,

    @SerializedName("desc")
    val desc: String? = null,

    @SerializedName("state")
    val state: Int = 0,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("owner")
    val owner: BilibiliOwner? = null,

    @SerializedName("stat")
    val stat: BilibiliStat? = null,

    @SerializedName("dynamic")
    val dynamic: String? = null,

    @SerializedName("cid")
    val cid: Long? = null,

    @SerializedName("dimension")
    val dimension: BilibiliDimension? = null,

    @SerializedName("short_link_v2")
    val shortLinkV2: String? = null,

    @SerializedName("first_frame")
    val firstFrame: String? = null,

    @SerializedName("pub_location")
    val pubLocation: String? = null,

    // 🎯 新增：TikHub API 返回的视频流数据（已完成服务端签名）
    @SerializedName("durl")
    val durl: List<BilibiliDurl>? = null,

    // 🎯 新增：视频质量标识（16=流畅, 32=清晰, 64=高清, 80=超清, 112=高码率, 116=4K, 120=8K）
    @SerializedName("quality")
    val quality: Int = 0,

    // 🎯 新增：支持的清晰度列表
    @SerializedName("accept_quality")
    val acceptQuality: List<Int>? = null,

    // 🎯 新增：清晰度描述列表
    @SerializedName("accept_description")
    val acceptDescription: List<String>? = null,

    // 🎯 新增：DASH 格式视频流（音视频分离，现代格式）
    @SerializedName("dash")
    val dash: BilibiliDash? = null
)

/**
 * B站视频流信息（TikHub API 已完成签名处理）
 */
data class BilibiliDurl(
    @SerializedName("order")
    val order: Int = 0,

    @SerializedName("length")
    val length: Long = 0,  // 时长（毫秒）

    @SerializedName("size")
    val size: Long = 0,  // 文件大小（字节）

    @SerializedName("url")
    val url: String? = null,  // 直链（已签名，可直接下载）

    @SerializedName("backup_url")
    val backupUrl: List<String>? = null  // 备用链接（CDN 容错）
)

/**
 * B站 DASH 格式视频流（音视频分离）
 */
data class BilibiliDash(
    @SerializedName("duration")
    val duration: Int = 0,  // 时长（秒）

    @SerializedName("video")
    val video: List<BilibiliDashStream>? = null,  // 视频流列表

    @SerializedName("audio")
    val audio: List<BilibiliDashStream>? = null,  // 音频流列表

    @SerializedName("dolby")
    val dolby: BilibiliDashDolby? = null,  // 杜比音效（可选）

    @SerializedName("flac")
    val flac: BilibiliDashFlac? = null  // 无损音质（可选）
)

/**
 * DASH 流信息（视频或音频）
 */
data class BilibiliDashStream(
    @SerializedName("id")
    val id: Int = 0,  // 清晰度标识

    @SerializedName("base_url")
    val baseUrl: String? = null,  // 主 URL

    @SerializedName("backup_url")
    val backupUrl: List<String>? = null,  // 备用 URL（多 CDN 容灾）

    @SerializedName("bandwidth")
    val bandwidth: Long = 0,  // 带宽/码率（重要：用于排序选择最高质量）

    @SerializedName("mime_type")
    val mimeType: String? = null,  // MIME 类型（如 video/mp4, audio/mp4）

    @SerializedName("codecs")
    val codecs: String? = null,  // 编码格式（如 avc1.640032, mp4a.40.2）

    @SerializedName("width")
    val width: Int = 0,  // 宽度（仅视频流）

    @SerializedName("height")
    val height: Int = 0,  // 高度（仅视频流）

    @SerializedName("frame_rate")
    val frameRate: String? = null,  // 帧率（如 "30"）

    @SerializedName("sar")
    val sar: String? = null,  // 采样宽高比

    @SerializedName("start_with_sap")
    val startWithSap: Int = 0,

    @SerializedName("segment_base")
    val segmentBase: BilibiliSegmentBase? = null,

    @SerializedName("codecid")
    val codecId: Int = 0  // 编码 ID（7=AVC/H.264, 12=HEVC/H.265）
)

/**
 * DASH 分段基础信息
 */
data class BilibiliSegmentBase(
    @SerializedName("initialization")
    val initialization: String? = null,

    @SerializedName("index_range")
    val indexRange: String? = null
)

/**
 * 杜比音效信息
 */
data class BilibiliDashDolby(
    @SerializedName("type")
    val type: Int = 0,

    @SerializedName("audio")
    val audio: List<BilibiliDashStream>? = null
)

/**
 * 无损音质信息
 */
data class BilibiliDashFlac(
    @SerializedName("display")
    val display: Boolean = false,

    @SerializedName("audio")
    val audio: BilibiliDashStream? = null
)

data class BilibiliOwner(
    @SerializedName("mid")
    val mid: Long? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("face")
    val face: String? = null
)

data class BilibiliStat(
    @SerializedName("aid")
    val aid: Long? = null,

    @SerializedName("view")
    val view: Int = 0,

    @SerializedName("danmaku")
    val danmaku: Int = 0,

    @SerializedName("reply")
    val reply: Int = 0,

    @SerializedName("favorite")
    val favorite: Int = 0,

    @SerializedName("coin")
    val coin: Int = 0,

    @SerializedName("share")
    val share: Int = 0,

    @SerializedName("now_rank")
    val nowRank: Int = 0,

    @SerializedName("his_rank")
    val hisRank: Int = 0,

    @SerializedName("like")
    val like: Int = 0,

    @SerializedName("dislike")
    val dislike: Int = 0
)

data class BilibiliDimension(
    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("rotate")
    val rotate: Int = 0
)

/**
 * 将B站响应转换为通用 ParseResult
 */
fun BilibiliVideoData.toParseResult(): ParseResult {
    val video = data ?: throw IllegalStateException("B站响应数据为空")

    return ParseResult(
        type = "video",
        title = video.title ?: "无标题",
        desc = video.desc,
        author = video.owner?.let {
            AuthorInfo(
                uid = it.mid?.toString() ?: "",
                nickname = it.name ?: "未知用户",
                avatar = it.face,
                signature = null
            )
        },
        statistics = video.stat?.let {
            Statistics(
                likeCount = it.like.toLong(),
                commentCount = it.reply.toLong(),
                shareCount = it.share.toLong(),
                collectCount = it.favorite.toLong(),
                playCount = it.view.toLong()
            )
        },
        video = VideoInfo(
            playUrl = null,  // B站不直接返回播放链接，需要额外API
            downloadUrl = null,
            cover = video.pic,
            dynamicCover = video.firstFrame,
            duration = video.duration,
            width = video.dimension?.width ?: 0,
            height = video.dimension?.height ?: 0,
            ratio = if (video.dimension != null && video.dimension.width > 0 && video.dimension.height > 0) {
                "${video.dimension.width}:${video.dimension.height}"
            } else null,
            bitrate = 0
        ),
        images = null,
        shareUrl = video.shortLinkV2,
        createTime = video.pubdate
    )
}
