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
    val pubLocation: String? = null
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
