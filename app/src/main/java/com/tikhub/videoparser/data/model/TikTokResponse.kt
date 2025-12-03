package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * TikTok API 响应数据模型
 * 对应 TikHub API 返回的完整结构
 */
data class TikTokVideoData(
    @SerializedName("aweme_detail")
    val awemeDetail: TikTokAwemeDetail
)

data class TikTokAwemeDetail(
    @SerializedName("aweme_id")
    val awemeId: String,

    @SerializedName("desc")
    val desc: String? = null,

    @SerializedName("create_time")
    val createTime: Long = 0,

    @SerializedName("author")
    val author: TikTokAuthor,

    @SerializedName("statistics")
    val statistics: TikTokStatistics,

    @SerializedName("video")
    val video: TikTokVideo,

    @SerializedName("share_url")
    val shareUrl: String? = null,

    @SerializedName("music")
    val music: TikTokMusic? = null
)

data class TikTokAuthor(
    @SerializedName("uid")
    val uid: String,

    @SerializedName("unique_id")
    val uniqueId: String? = null,

    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("avatar_thumb")
    val avatarThumb: TikTokUrlContainer? = null,

    @SerializedName("signature")
    val signature: String? = null
)

data class TikTokStatistics(
    @SerializedName("digg_count")
    val diggCount: Long = 0,

    @SerializedName("comment_count")
    val commentCount: Long = 0,

    @SerializedName("share_count")
    val shareCount: Long = 0,

    @SerializedName("collect_count")
    val collectCount: Long = 0,

    @SerializedName("play_count")
    val playCount: Long = 0
)

data class TikTokVideo(
    @SerializedName("play_addr")
    val playAddr: TikTokUrlContainer? = null,

    @SerializedName("download_addr")
    val downloadAddr: TikTokUrlContainer? = null,

    @SerializedName("cover")
    val cover: TikTokUrlContainer? = null,

    @SerializedName("dynamic_cover")
    val dynamicCover: TikTokUrlContainer? = null,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("ratio")
    val ratio: String? = null,

    @SerializedName("bit_rate")
    val bitRate: List<TikTokBitRate>? = null
)

data class TikTokUrlContainer(
    @SerializedName("url_list")
    val urlList: List<String>? = null,

    @SerializedName("uri")
    val uri: String? = null,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0
) {
    /**
     * 获取第一个可用 URL
     */
    fun getFirstUrl(): String? = urlList?.firstOrNull()
}

data class TikTokBitRate(
    @SerializedName("bit_rate")
    val bitRate: Long = 0,

    @SerializedName("gear_name")
    val gearName: String? = null,

    @SerializedName("quality_type")
    val qualityType: Int = 0,

    @SerializedName("play_addr")
    val playAddr: TikTokUrlContainer? = null
)

data class TikTokMusic(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("author")
    val author: String? = null,

    @SerializedName("play_url")
    val playUrl: TikTokUrlContainer? = null
)

/**
 * 将 TikTok 响应转换为通用 ParseResult
 */
fun TikTokVideoData.toParseResult(): ParseResult {
    val detail = awemeDetail

    return ParseResult(
        type = "video",
        title = detail.desc,
        desc = detail.desc,
        author = AuthorInfo(
            uid = detail.author.uid,
            nickname = detail.author.nickname,
            avatar = detail.author.avatarThumb?.getFirstUrl(),
            signature = detail.author.signature
        ),
        statistics = Statistics(
            likeCount = detail.statistics.diggCount,
            commentCount = detail.statistics.commentCount,
            shareCount = detail.statistics.shareCount,
            collectCount = detail.statistics.collectCount,
            playCount = detail.statistics.playCount
        ),
        video = VideoInfo(
            playUrl = detail.video.playAddr?.getFirstUrl(),
            downloadUrl = detail.video.downloadAddr?.getFirstUrl(),
            cover = detail.video.cover?.getFirstUrl(),
            dynamicCover = detail.video.dynamicCover?.getFirstUrl(),
            duration = detail.video.duration / 1000, // 转换为秒
            width = detail.video.width,
            height = detail.video.height,
            ratio = detail.video.ratio
        ),
        shareUrl = detail.shareUrl,
        createTime = detail.createTime
    )
}
