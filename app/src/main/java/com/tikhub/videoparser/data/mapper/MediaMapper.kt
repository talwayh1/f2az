package com.tikhub.videoparser.data.mapper

import com.google.gson.JsonObject
import com.tikhub.videoparser.data.model.*
import timber.log.Timber

/**
 * 数据映射器（Mapper）
 *
 * 职责：将各平台的原始 API 响应转换为统一的 ParsedMedia 模型
 *
 * 设计原则：
 * 1. 每个平台一个独立的转换方法
 * 2. 统一的错误处理和日志记录
 * 3. 防御性编程：所有字段都考虑 null 情况
 * 4. 智能判断内容类型（Video vs ImageNote）
 */
object MediaMapper {

    // ========================================
    // 抖音 Mapper
    // ========================================

    /**
     * 转换抖音数据
     *
     * 特殊逻辑：
     * - 抖音有"图文笔记"模式（images 字段不为空）
     * - 优先判断 images，其次才是 video
     * - 去水印逻辑：将 playwm 替换为 play
     */
    fun mapDouyin(data: DouyinVideoData): ParsedMedia {
        val detail = data.awemeDetail

        // 判断内容类型
        val hasImages = !detail.images.isNullOrEmpty()
        val hasVideo = detail.video != null && detail.video.playAddr != null

        // 构建统计信息
        val stats = StatsInfo(
            likeCount = detail.statistics.diggCount,
            commentCount = detail.statistics.commentCount,
            shareCount = detail.statistics.shareCount,
            collectCount = detail.statistics.collectCount,
            playCount = detail.statistics.playCount
        )

        // 获取作者头像
        val authorAvatar = detail.author.avatarThumb?.getFirstUrl() ?: ""

        return if (hasImages) {
            // 图文笔记模式
            ParsedMedia.ImageNote(
                id = detail.awemeId,
                platform = "douyin",
                authorName = detail.author.nickname,
                authorAvatar = authorAvatar,
                title = detail.desc ?: "抖音图文",
                coverUrl = detail.images?.firstOrNull()?.getFirstUrl() ?: "",
                stats = stats,
                createTime = detail.createTime,
                shareUrl = detail.shareUrl,
                imageUrls = detail.images?.mapNotNull { it.getFirstUrl() } ?: emptyList(),
                imageSizes = detail.images?.map {
                    ImageSize(it.width, it.height, 0)
                } ?: emptyList()
            )
        } else if (hasVideo) {
            // 视频模式
            val video = detail.video!!
            val playUrl = video.playAddr?.getFirstUrl() ?: ""

            // 抖音去水印逻辑：playwm -> play
            val noWatermarkUrl = if (playUrl.contains("playwm")) {
                playUrl.replace("playwm", "play")
            } else {
                playUrl
            }

            ParsedMedia.Video(
                id = detail.awemeId,
                platform = "douyin",
                authorName = detail.author.nickname,
                authorAvatar = authorAvatar,
                title = detail.desc ?: "抖音视频",
                coverUrl = video.cover?.getFirstUrl() ?: "",
                stats = stats,
                createTime = detail.createTime,
                shareUrl = detail.shareUrl,
                videoUrl = noWatermarkUrl,
                duration = video.duration / 1000, // 毫秒转秒
                width = video.width,
                height = video.height,
                fileSize = 0, // API 未提供
                bitrate = video.bitRate?.firstOrNull()?.bitRate ?: 0,
                musicUrl = detail.music?.playUrl?.getFirstUrl(),
                musicTitle = detail.music?.title
            )
        } else {
            throw IllegalStateException("抖音数据既没有图片也没有视频")
        }
    }

    // ========================================
    // TikTok Mapper
    // ========================================

    /**
     * 转换 TikTok 数据
     *
     * TikTok 的结构与抖音非常相似
     */
    fun mapTikTok(data: TikTokVideoData): ParsedMedia {
        val detail = data.awemeDetail

        val stats = StatsInfo(
            likeCount = detail.statistics.diggCount,
            commentCount = detail.statistics.commentCount,
            shareCount = detail.statistics.shareCount,
            collectCount = detail.statistics.collectCount,
            playCount = detail.statistics.playCount
        )

        val video = detail.video
            ?: throw IllegalStateException("TikTok 数据缺少 video 字段")

        val playUrl = video.playAddr?.getFirstUrl() ?: ""

        // TikTok 去水印逻辑（与抖音相同）
        val noWatermarkUrl = if (playUrl.contains("playwm")) {
            playUrl.replace("playwm", "play")
        } else {
            playUrl
        }

        return ParsedMedia.Video(
            id = detail.awemeId,
            platform = "tiktok",
            authorName = detail.author.nickname,
            authorAvatar = detail.author.avatarThumb?.getFirstUrl() ?: "",
            title = detail.desc ?: "TikTok video",
            coverUrl = video.cover?.getFirstUrl() ?: "",
            stats = stats,
            createTime = detail.createTime,
            shareUrl = detail.shareUrl,
            videoUrl = noWatermarkUrl,
            duration = video.duration / 1000,
            width = video.width,
            height = video.height,
            fileSize = 0,
            bitrate = video.bitRate?.firstOrNull()?.bitRate ?: 0,
            musicUrl = detail.music?.playUrl?.getFirstUrl(),
            musicTitle = detail.music?.title
        )
    }

    // ========================================
    // 小红书 Mapper
    // ========================================

    /**
     * 转换小红书数据
     *
     * 特殊逻辑：
     * - 小红书以图文为主，也支持视频笔记
     * - 需要从复杂的嵌套结构中提取数据
     * - 统计数据字段名称与抖音不同
     */
    fun mapXiaohongshu(data: XiaohongshuNoteData): ParsedMedia {
        // 小红书的数据结构：data.data[0].note_list[0]
        val noteDetail = data.data?.firstOrNull()?.noteList?.firstOrNull()
            ?: throw IllegalStateException("小红书数据结构异常：缺少 note_list")

        val isVideo = noteDetail.type == "video" && noteDetail.video != null

        val stats = StatsInfo(
            likeCount = noteDetail.likedCount.toLong(),
            commentCount = noteDetail.commentsCount.toLong(),
            shareCount = noteDetail.sharedCount.toLong(),
            collectCount = noteDetail.collectedCount.toLong(),
            playCount = noteDetail.viewCount.toLong()
        )

        val authorAvatar = noteDetail.user?.image ?: ""
        val authorName = noteDetail.user?.nickname ?: "小红书用户"

        return if (isVideo) {
            // 视频笔记
            val video = noteDetail.video!!

            // 从 urlInfoList 中选择最佳质量（优先 H264）
            val h264Url = video.urlInfoList
                ?.firstOrNull { it.desc?.contains("h264", ignoreCase = true) == true }
                ?.url
            val h265Url = video.urlInfoList
                ?.firstOrNull { it.desc?.contains("h265", ignoreCase = true) == true }
                ?.url
            val videoUrl = h264Url ?: h265Url ?: video.url
                ?: throw IllegalStateException("小红书视频 URL 为空")

            // 获取视频尺寸信息（从 H264 或默认）
            val bestQuality = video.urlInfoList
                ?.firstOrNull { it.desc?.contains("h264", ignoreCase = true) == true }
                ?: video.urlInfoList?.firstOrNull()

            ParsedMedia.Video(
                id = noteDetail.id ?: "",
                platform = "xiaohongshu",
                authorName = authorName,
                authorAvatar = authorAvatar,
                title = noteDetail.title ?: noteDetail.desc ?: "小红书视频笔记",
                coverUrl = noteDetail.imagesList?.firstOrNull()?.url ?: "",
                stats = stats,
                createTime = noteDetail.time,
                shareUrl = noteDetail.shareInfo?.link,
                videoUrl = videoUrl,
                duration = video.duration,
                width = bestQuality?.width ?: video.width,
                height = bestQuality?.height ?: video.height,
                fileSize = 0,
                bitrate = bestQuality?.avgBitrate?.toLong() ?: video.avgBitrate.toLong()
            )
        } else {
            // 图文笔记
            val imagesList = noteDetail.imagesList ?: emptyList()
            if (imagesList.isEmpty()) {
                throw IllegalStateException("小红书图文笔记缺少图片")
            }

            ParsedMedia.ImageNote(
                id = noteDetail.id ?: "",
                platform = "xiaohongshu",
                authorName = authorName,
                authorAvatar = authorAvatar,
                title = noteDetail.title ?: noteDetail.desc ?: "小红书图文笔记",
                coverUrl = imagesList.firstOrNull()?.url ?: "",
                stats = stats,
                createTime = noteDetail.time ?: 0,
                shareUrl = noteDetail.shareInfo?.link,
                imageUrls = imagesList.mapNotNull { it.url },
                imageSizes = imagesList.map {
                    ImageSize(it.width ?: 0, it.height ?: 0, 0)
                }
            )
        }
    }

    // ========================================
    // 快手 Mapper
    // ========================================

    /**
     * 转换快手数据
     */
    fun mapKuaishou(data: KuaishouVideoData): ParsedMedia {
        val photo = data.photo

        val stats = StatsInfo(
            likeCount = photo.likeCount,
            commentCount = photo.commentCount,
            shareCount = photo.shareCount,
            collectCount = 0,
            playCount = photo.viewCount
        )

        val playUrl = photo.mainMvUrls?.firstOrNull()?.url
            ?: throw IllegalStateException("快手视频 URL 为空")

        return ParsedMedia.Video(
            id = photo.photoId,
            platform = "kuaishou",
            authorName = photo.userInfo.userName,
            authorAvatar = photo.userInfo.headUrl ?: "",
            title = photo.caption ?: "快手视频",
            coverUrl = photo.coverUrls?.firstOrNull()?.url ?: "",
            stats = stats,
            createTime = photo.timestamp,
            shareUrl = photo.shareInfo?.shareUrl,
            videoUrl = playUrl,
            duration = photo.duration / 1000,
            width = photo.width,
            height = photo.height,
            fileSize = 0,
            bitrate = 0
        )
    }

    // ========================================
    // B站 Mapper
    // ========================================

    /**
     * 转换 B站 数据
     */
    fun mapBilibili(data: BilibiliVideoData): ParsedMedia {
        val videoData = data.data
            ?: throw IllegalStateException("B站数据缺少 data 字段")

        val stats = StatsInfo(
            likeCount = videoData.stat?.like?.toLong() ?: 0,
            commentCount = videoData.stat?.reply?.toLong() ?: 0,
            shareCount = videoData.stat?.share?.toLong() ?: 0,
            collectCount = videoData.stat?.favorite?.toLong() ?: 0,
            playCount = videoData.stat?.view?.toLong() ?: 0
        )

        // 注意：B站的视频播放地址需要额外的 API 调用获取
        // 这里仅保存基础信息，实际播放地址需要通过 cid 和 bvid 获取
        return ParsedMedia.Video(
            id = videoData.bvid ?: videoData.aid?.toString() ?: "",
            platform = "bilibili",
            authorName = videoData.owner?.name ?: "B站用户",
            authorAvatar = videoData.owner?.face ?: "",
            title = videoData.title ?: "B站视频",
            coverUrl = videoData.pic ?: "",
            stats = stats,
            createTime = videoData.ctime,
            shareUrl = "https://www.bilibili.com/video/${videoData.bvid}",
            videoUrl = "", // B站需要额外 API 获取真实播放地址
            duration = videoData.duration,
            width = videoData.dimension?.width ?: 0,
            height = videoData.dimension?.height ?: 0,
            fileSize = 0,
            bitrate = 0
        )
    }

    // ========================================
    // 微博 Mapper（新增）
    // ========================================

    /**
     * 转换微博数据
     *
     * 微博特殊性：
     * - 支持视频微博和九宫格图文微博
     * - 通过 page_info.type 判断是否为视频
     * - 图文微博通过 pics 字段获取图片列表
     */
    fun mapWeibo(json: JsonObject): ParsedMedia {
        Timber.d("开始解析微博数据")

        // 微博 API 返回的 status 对象
        val status = json.getAsJsonObject("status")
            ?: throw IllegalStateException("微博数据缺少 status 字段")

        val user = status.getAsJsonObject("user")
        val pageInfo = status.getAsJsonObject("page_info")

        // 判断是否为视频微博
        val isVideo = pageInfo != null &&
                pageInfo.get("type")?.asString == "video" &&
                pageInfo.getAsJsonObject("media_info") != null

        // 构建统计信息
        val stats = StatsInfo(
            likeCount = status.get("attitudes_count")?.asLong ?: 0,
            commentCount = status.get("comments_count")?.asLong ?: 0,
            shareCount = status.get("reposts_count")?.asLong ?: 0,
            collectCount = 0,
            playCount = 0
        )

        val authorName = user?.get("screen_name")?.asString ?: "微博用户"
        val authorAvatar = user?.get("avatar_large")?.asString ?: ""
        val title = status.get("text_raw")?.asString ?: "微博内容"
        val id = status.get("id")?.asString ?: ""

        return if (isVideo) {
            // 视频微博
            val mediaInfo = pageInfo.getAsJsonObject("media_info")
            val videoUrl = mediaInfo.get("stream_url_hd")?.asString
                ?: mediaInfo.get("stream_url")?.asString
                ?: throw IllegalStateException("微博视频 URL 为空")

            val coverUrl = pageInfo.getAsJsonObject("page_pic")?.get("url")?.asString ?: ""

            ParsedMedia.Video(
                id = id,
                platform = "weibo",
                authorName = authorName,
                authorAvatar = authorAvatar,
                title = title,
                coverUrl = coverUrl,
                stats = stats,
                createTime = status.get("created_at")?.asLong ?: 0,
                shareUrl = null,
                videoUrl = videoUrl,
                duration = mediaInfo.get("duration")?.asInt ?: 0,
                width = 0,
                height = 0,
                fileSize = 0,
                bitrate = 0
            )
        } else {
            // 图文微博（九宫格）
            val pics = status.getAsJsonArray("pics")
            if (pics == null || pics.size() == 0) {
                throw IllegalStateException("微博图文内容缺少图片")
            }

            val imageUrls = mutableListOf<String>()
            for (i in 0 until pics.size()) {
                val pic = pics[i].asJsonObject
                val large = pic.getAsJsonObject("large")
                val url = large?.get("url")?.asString
                if (url != null) {
                    imageUrls.add(url)
                }
            }

            ParsedMedia.ImageNote(
                id = id,
                platform = "weibo",
                authorName = authorName,
                authorAvatar = authorAvatar,
                title = title,
                coverUrl = imageUrls.firstOrNull() ?: "",
                stats = stats,
                createTime = status.get("created_at")?.asLong ?: 0,
                shareUrl = null,
                imageUrls = imageUrls,
                imageSizes = null
            )
        }
    }

    // ========================================
    // 西瓜视频 Mapper（新增）
    // ========================================

    /**
     * 转换西瓜视频数据
     *
     * 西瓜视频特点：
     * - 以横屏视频为主
     * - 数据结构与抖音相似（同属字节跳动）
     */
    fun mapXigua(data: XiguaVideoData): ParsedMedia {
        Timber.d("开始解析西瓜视频数据")

        val itemInfo = data.itemInfo

        val stats = StatsInfo(
            likeCount = itemInfo.stats.diggCount,
            commentCount = itemInfo.stats.commentCount,
            shareCount = itemInfo.stats.shareCount,
            collectCount = 0,
            playCount = itemInfo.stats.playCount
        )

        val videoUrl = itemInfo.video.playAddr?.urlList?.firstOrNull()
            ?: itemInfo.video.downloadAddr?.urlList?.firstOrNull()
            ?: throw IllegalStateException("西瓜视频 URL 为空")

        val coverUrl = itemInfo.video.cover?.urlList?.firstOrNull() ?: ""

        return ParsedMedia.Video(
            id = itemInfo.itemId,
            platform = "xigua",
            authorName = itemInfo.author.name,
            authorAvatar = itemInfo.author.avatarUrl ?: "",
            title = itemInfo.title ?: itemInfo.desc ?: "西瓜视频",
            coverUrl = coverUrl,
            stats = stats,
            createTime = itemInfo.createTime,
            shareUrl = itemInfo.shareUrl,
            videoUrl = videoUrl,
            duration = itemInfo.video.duration / 1000,
            width = itemInfo.video.width,
            height = itemInfo.video.height,
            fileSize = 0,
            bitrate = 0
        )
    }

    // ========================================
    // Instagram Mapper（新增）
    // ========================================

    /**
     * 转换 Instagram 数据
     *
     * Instagram 特点：
     * - 支持单图、单视频、轮播（Carousel）
     * - media_type: 1=图片, 2=视频, 8=轮播
     * - 轮播可能包含图片和视频的混合
     */
    fun mapInstagram(data: InstagramPostData): ParsedMedia {
        Timber.d("开始解析 Instagram 数据")

        val item = data.items?.firstOrNull()
            ?: throw IllegalStateException("Instagram 数据缺少 items")

        val stats = StatsInfo(
            likeCount = item.likeCount,
            commentCount = item.commentCount,
            shareCount = 0,
            collectCount = 0,
            playCount = item.playCount
        )

        val authorName = item.user.username
        val authorAvatar = item.user.profilePicUrl ?: ""
        val title = item.caption?.text ?: "Instagram post"
        val id = item.id

        return when (item.mediaType) {
            1 -> {
                // 单图
                val imageUrl = item.imageVersions?.candidates?.firstOrNull()?.url
                    ?: throw IllegalStateException("Instagram 图片 URL 为空")

                ParsedMedia.ImageNote(
                    id = id,
                    platform = "instagram",
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    title = title,
                    coverUrl = imageUrl,
                    stats = stats,
                    createTime = item.takenAt,
                    shareUrl = "https://www.instagram.com/p/${item.code}/",
                    imageUrls = listOf(imageUrl),
                    imageSizes = item.imageVersions?.candidates?.map {
                        ImageSize(it.width, it.height, 0)
                    }
                )
            }
            2 -> {
                // 单视频
                val videoUrl = item.videoVersions?.firstOrNull()?.url
                    ?: throw IllegalStateException("Instagram 视频 URL 为空")

                val coverUrl = item.imageVersions?.candidates?.firstOrNull()?.url ?: ""

                ParsedMedia.Video(
                    id = id,
                    platform = "instagram",
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    title = title,
                    coverUrl = coverUrl,
                    stats = stats,
                    createTime = item.takenAt,
                    shareUrl = "https://www.instagram.com/p/${item.code}/",
                    videoUrl = videoUrl,
                    duration = 0, // Instagram API 可能不提供
                    width = item.videoVersions?.firstOrNull()?.width ?: 0,
                    height = item.videoVersions?.firstOrNull()?.height ?: 0,
                    fileSize = 0,
                    bitrate = 0
                )
            }
            8 -> {
                // 轮播（Carousel）- 提取所有图片
                val carouselMedia = item.carouselMedia
                    ?: throw IllegalStateException("Instagram 轮播数据为空")

                val imageUrls = mutableListOf<String>()
                val imageSizes = mutableListOf<ImageSize>()

                for (media in carouselMedia) {
                    if (media.mediaType == 1 || media.mediaType == 8) {
                        // 图片
                        val url = media.imageVersions?.candidates?.firstOrNull()?.url
                        if (url != null) {
                            imageUrls.add(url)
                            val candidate = media.imageVersions?.candidates?.firstOrNull()
                            imageSizes.add(ImageSize(candidate?.width ?: 0, candidate?.height ?: 0, 0))
                        }
                    }
                    // 注意：如果轮播中包含视频，这里暂时忽略
                }

                if (imageUrls.isEmpty()) {
                    throw IllegalStateException("Instagram 轮播中没有可用图片")
                }

                ParsedMedia.ImageNote(
                    id = id,
                    platform = "instagram",
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    title = title,
                    coverUrl = imageUrls.firstOrNull() ?: "",
                    stats = stats,
                    createTime = item.takenAt,
                    shareUrl = "https://www.instagram.com/p/${item.code}/",
                    imageUrls = imageUrls,
                    imageSizes = imageSizes
                )
            }
            else -> {
                throw IllegalStateException("不支持的 Instagram 媒体类型: ${item.mediaType}")
            }
        }
    }

    // ========================================
    // YouTube Mapper（新增）
    // ========================================

    /**
     * 转换 YouTube 数据
     *
     * YouTube 特点：
     * - 长视频为主
     * - 有多种清晰度的视频流
     * - 需要从 formats 中选择最佳质量
     */
    fun mapYouTube(data: YouTubeVideoData): ParsedMedia {
        Timber.d("开始解析 YouTube 数据")

        val videoDetails = data.videoDetails

        val stats = StatsInfo(
            likeCount = 0, // YouTube API 可能不提供点赞数
            commentCount = 0,
            shareCount = 0,
            collectCount = 0,
            playCount = videoDetails.viewCount
        )

        // 从 formats 中选择最高质量的视频
        val formats = data.streamingData?.formats ?: emptyList()
        val adaptiveFormats = data.streamingData?.adaptiveFormats ?: emptyList()

        // 优先选择包含音视频的 formats
        val videoUrl = formats.maxByOrNull { it.bitrate }?.url
            ?: adaptiveFormats.filter { it.mimeType?.contains("video") == true }
                .maxByOrNull { it.bitrate }?.url
            ?: throw IllegalStateException("YouTube 视频 URL 为空")

        val coverUrl = videoDetails.thumbnail?.thumbnails?.maxByOrNull { it.width * it.height }?.url ?: ""

        // 获取视频分辨率
        val bestFormat = formats.maxByOrNull { it.width * it.height }
            ?: adaptiveFormats.filter { it.mimeType?.contains("video") == true }
                .maxByOrNull { it.width * it.height }

        return ParsedMedia.Video(
            id = videoDetails.videoId,
            platform = "youtube",
            authorName = videoDetails.author ?: "YouTube Creator",
            authorAvatar = "", // YouTube API 可能不直接提供频道头像
            title = videoDetails.title ?: "YouTube Video",
            coverUrl = coverUrl,
            stats = stats,
            createTime = 0, // YouTube API 可能不提供
            shareUrl = "https://www.youtube.com/watch?v=${videoDetails.videoId}",
            videoUrl = videoUrl,
            duration = videoDetails.lengthSeconds,
            width = bestFormat?.width ?: 0,
            height = bestFormat?.height ?: 0,
            fileSize = bestFormat?.contentLength ?: 0,
            bitrate = bestFormat?.bitrate?.toLong() ?: 0
        )
    }

    // ========================================
    // 通用错误处理
    // ========================================

    /**
     * 安全转换（带错误处理）
     */
    fun <T> safeMap(
        data: T,
        mapper: (T) -> ParsedMedia,
        platform: String
    ): Result<ParsedMedia> {
        return try {
            Timber.d("开始转换 $platform 数据")
            val result = mapper(data)
            Timber.i("✅ $platform 数据转换成功")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "❌ $platform 数据转换失败")
            Result.failure(e)
        }
    }
}
