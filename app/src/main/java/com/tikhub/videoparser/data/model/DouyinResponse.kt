package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * 抖音 API 响应数据模型
 * 对应 TikHub API 返回的完整结构
 */
data class DouyinVideoData(
    @SerializedName("aweme_detail")
    val awemeDetail: DouyinAwemeDetail
)

data class DouyinAwemeDetail(
    @SerializedName("aweme_id")
    val awemeId: String,

    @SerializedName("desc")
    val desc: String? = null,

    @SerializedName("create_time")
    val createTime: Long = 0,

    @SerializedName("author")
    val author: DouyinAuthor,

    @SerializedName("statistics")
    val statistics: DouyinStatistics,

    @SerializedName("video")
    val video: DouyinVideo? = null,

    @SerializedName("images")
    val images: List<DouyinImage>? = null,

    @SerializedName("share_url")
    val shareUrl: String? = null,

    @SerializedName("music")
    val music: DouyinMusic? = null
)

data class DouyinAuthor(
    @SerializedName("uid")
    val uid: String,

    @SerializedName("nickname")
    val nickname: String,

    @SerializedName("avatar_thumb")
    val avatarThumb: DouyinUrlContainer? = null,

    @SerializedName("signature")
    val signature: String? = null
)

data class DouyinStatistics(
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

data class DouyinVideo(
    @SerializedName("play_addr")
    val playAddr: DouyinUrlContainer? = null,

    @SerializedName("download_addr")
    val downloadAddr: DouyinUrlContainer? = null,

    @SerializedName("cover")
    val cover: DouyinUrlContainer? = null,

    @SerializedName("dynamic_cover")
    val dynamicCover: DouyinUrlContainer? = null,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("width")
    val width: Int = 0,

    @SerializedName("height")
    val height: Int = 0,

    @SerializedName("ratio")
    val ratio: String? = null,

    @SerializedName("bit_rate")
    val bitRate: List<DouyinBitRate>? = null
)

data class DouyinImage(
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

data class DouyinUrlContainer(
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

data class DouyinBitRate(
    @SerializedName("bit_rate")
    val bitRate: Long = 0,

    @SerializedName("gear_name")
    val gearName: String? = null,

    @SerializedName("quality_type")
    val qualityType: Int = 0,

    @SerializedName("play_addr")
    val playAddr: DouyinUrlContainer? = null
)

data class DouyinMusic(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("author")
    val author: String? = null,

    @SerializedName("play_url")
    val playUrl: DouyinUrlContainer? = null
)

/**
 * 将抖音响应转换为通用 ParseResult
 */
fun DouyinVideoData.toParseResult(): ParseResult {
    val detail = awemeDetail

    // 判断内容类型：优先检查 images（图文笔记也可能有背景音乐，所以优先判断图片）
    val hasImages = !detail.images.isNullOrEmpty()
    val hasVideo = detail.video != null && detail.video.playAddr != null && detail.video.duration > 0
    val isVideo = !hasImages && hasVideo  // 只有没有图片且有视频时才是视频

    return ParseResult(
        type = if (isVideo) "video" else "image",
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
        video = if (isVideo && detail.video != null) {
            VideoInfo(
                playUrl = detail.video.playAddr?.getFirstUrl(),
                downloadUrl = detail.video.downloadAddr?.getFirstUrl(),
                cover = detail.video.cover?.getFirstUrl(),
                dynamicCover = detail.video.dynamicCover?.getFirstUrl(),
                duration = detail.video.duration / 1000, // 转换为秒
                width = detail.video.width,
                height = detail.video.height,
                ratio = detail.video.ratio
            )
        } else null,
        images = if (!isVideo && !detail.images.isNullOrEmpty()) {
            detail.images.mapNotNull { img ->
                val url = img.getFirstUrl() ?: return@mapNotNull null
                ImageInfo(
                    url = url,
                    width = img.width,
                    height = img.height,
                    size = 0  // API 未提供，UI 层可异步获取
                )
            }
        } else null,
        shareUrl = detail.shareUrl,
        createTime = detail.createTime
    )
}
