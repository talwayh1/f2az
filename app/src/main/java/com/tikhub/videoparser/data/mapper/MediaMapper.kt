package com.tikhub.videoparser.data.mapper

import com.google.gson.JsonObject
import com.tikhub.videoparser.data.model.*
import com.tikhub.videoparser.utils.VideoQualitySelector
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

        // 判断内容类型 - 更严格的验证
        val imageUrls = detail.images?.mapNotNull { it.getFirstUrl() }?.filter { it.isNotBlank() } ?: emptyList()
        val hasImages = imageUrls.isNotEmpty()

        val hasVideo = detail.video != null &&
                      detail.video.playAddr != null &&
                      detail.video.playAddr.getFirstUrl()?.isNotBlank() == true

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
            // 图文笔记模式 - imageUrls 已经过滤验证，确保非空
            Timber.d("抖音图文笔记：共 ${imageUrls.size} 张图片")
            val imageResult = ParsedMedia.ImageNote(
                id = detail.awemeId,
                platform = "douyin",
                authorName = detail.author.nickname,
                authorAvatar = authorAvatar,
                title = detail.desc ?: "抖音图文",
                coverUrl = imageUrls.firstOrNull() ?: "",
                stats = stats,
                createTime = detail.createTime,
                shareUrl = detail.shareUrl,
                imageUrls = imageUrls,
                imageSizes = detail.images?.map {
                    ImageSize(it.width, it.height, 0)
                } ?: emptyList()
            )
            Timber.i("✅ 抖音图文解析成功: ${imageResult.title} (aweme_id=${detail.awemeId})")
            imageResult
        } else if (hasVideo) {
            // 视频模式
            val video = detail.video!!

            // 🎯 使用智能画质选择算法（基于 bit_rate 数据驱动）
            val bestVideo = VideoQualitySelector.selectBestDouyinVideo(
                bitRateList = video.bitRate,
                downloadAddr = video.downloadAddr,
                playAddr = video.playAddr
            ) ?: throw IllegalStateException("抖音视频：无法找到任何可用的视频 URL")

            // 去除水印（如果需要）
            val noWatermarkUrl = VideoQualitySelector.removeDouyinWatermark(bestVideo.url)

            // 格式化画质信息
            val qualityInfo = VideoQualitySelector.parseQualityFromGearName(bestVideo.gearName)
            val bitrateInfo = VideoQualitySelector.formatBitRate(bestVideo.bitRate)
            // 🎯 正确识别编码格式 (包括 ByteVC2)
            val codecInfo = when (bestVideo.isBytevc1Value) {
                2 -> "ByteVC2"
                1 -> "H.265"
                else -> "H.264"
            }

            val codecWarning = if (bestVideo.isBytevc1Value == 2) " [⚠️ 可能不兼容]" else ""
            Timber.i("抖音视频：画质=$qualityInfo, 编码=$codecInfo$codecWarning, 码率=$bitrateInfo, " +
                    "FPS=${bestVideo.fps}, 文件大小=${bestVideo.dataSize} 字节, 来源=${bestVideo.source}")

            val videoResult = ParsedMedia.Video(
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
                fileSize = bestVideo.dataSize,
                bitrate = bestVideo.bitRate,
                musicUrl = detail.music?.playUrl?.getFirstUrl(),
                musicTitle = detail.music?.title,
                // 🎯 新增：编码技术信息
                codecType = codecInfo,
                fps = bestVideo.fps,
                qualityTag = qualityInfo,
                videoSource = bestVideo.source
            )
            Timber.i("✅ 抖音视频解析成功: ${videoResult.title} (aweme_id=${detail.awemeId})")
            videoResult
        } else {
            throw IllegalStateException("抖音数据解析失败：内容既没有有效图片也没有有效视频 (aweme_id=${detail.awemeId})")
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

        // 🎯 将 TikTok 数据转换为抖音格式（数据结构相同）
        val douyinBitRateList = video.bitRate?.map { tikTokBitRate ->
            DouyinBitRate(
                bitRate = tikTokBitRate.bitRate,
                gearName = tikTokBitRate.gearName,
                qualityType = tikTokBitRate.qualityType,
                playAddr = tikTokBitRate.playAddr?.let { tikTokUrl ->
                    DouyinUrlContainer(
                        urlList = tikTokUrl.urlList,
                        uri = tikTokUrl.uri,
                        width = tikTokUrl.width,
                        height = tikTokUrl.height,
                        dataSize = tikTokUrl.dataSize
                    )
                },
                isBytevc1 = tikTokBitRate.isBytevc1,
                videoCodecType = tikTokBitRate.videoCodecType,
                fps = tikTokBitRate.fps
            )
        }

        val douyinDownloadAddr = video.downloadAddr?.let {
            DouyinUrlContainer(
                urlList = it.urlList,
                uri = it.uri,
                width = it.width,
                height = it.height,
                dataSize = it.dataSize
            )
        }

        val douyinPlayAddr = video.playAddr?.let {
            DouyinUrlContainer(
                urlList = it.urlList,
                uri = it.uri,
                width = it.width,
                height = it.height,
                dataSize = it.dataSize
            )
        }

        // 🎯 使用智能画质选择算法（与抖音相同的逻辑）
        val bestVideo = VideoQualitySelector.selectBestDouyinVideo(
            bitRateList = douyinBitRateList,
            downloadAddr = douyinDownloadAddr,
            playAddr = douyinPlayAddr
        ) ?: throw IllegalStateException("TikTok视频：无法找到任何可用的视频 URL")

        // 去除水印（如果需要）
        val noWatermarkUrl = VideoQualitySelector.removeDouyinWatermark(bestVideo.url)

        // 格式化画质信息
        val qualityInfo = VideoQualitySelector.parseQualityFromGearName(bestVideo.gearName)
        val bitrateInfo = VideoQualitySelector.formatBitRate(bestVideo.bitRate)
        val codecInfo = if (bestVideo.isH265) "H.265" else "H.264"

        Timber.i("TikTok视频：画质=$qualityInfo, 编码=$codecInfo, 码率=$bitrateInfo, " +
                "FPS=${bestVideo.fps}, 文件大小=${bestVideo.dataSize} 字节, 来源=${bestVideo.source}")

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
            fileSize = bestVideo.dataSize,
            bitrate = bestVideo.bitRate,
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
        // 更安全的多层级 null 检查
        val noteDetail = data.data?.firstOrNull()?.noteList?.firstOrNull()
            ?: throw IllegalStateException("小红书数据结构异常：缺少 note_list (code=${data.code}, success=${data.success}, message=${data.message})")

        // 验证笔记 ID
        if (noteDetail.id.isNullOrBlank()) {
            throw IllegalStateException("小红书笔记 ID 为空")
        }

        // 判断是否为视频笔记，且视频数据有效
        val isVideo = noteDetail.type == "video" &&
                      noteDetail.video != null &&
                      noteDetail.video.url?.isNotBlank() == true

        val stats = StatsInfo(
            likeCount = noteDetail.likedCount.toLong(),
            commentCount = noteDetail.commentsCount.toLong(),
            shareCount = noteDetail.sharedCount.toLong(),
            collectCount = noteDetail.collectedCount.toLong(),
            playCount = noteDetail.viewCount.toLong()
        )

        val authorAvatar = noteDetail.user?.image ?: ""
        val authorName = noteDetail.user?.nickname
            ?: noteDetail.user?.name
            ?: "小红书用户"

        return if (isVideo) {
            // 视频笔记
            val video = noteDetail.video!!

            // 🎯 从 urlInfoList 中选择最佳质量（优先 H264 高码率，兼容性更好）
            val h264Videos = video.urlInfoList
                ?.filter { it.desc?.contains("h264", ignoreCase = true) == true }
                ?: emptyList()

            val h265Videos = video.urlInfoList
                ?.filter { it.desc?.contains("h265", ignoreCase = true) == true }
                ?: emptyList()

            // 选择最高码率的 H264 视频，如果没有则选择最高码率的 H265
            val bestH264 = h264Videos.maxByOrNull { it.avgBitrate }
            val bestH265 = h265Videos.maxByOrNull { it.avgBitrate }

            val bestQuality = bestH264 ?: bestH265 ?: video.urlInfoList?.firstOrNull()
            val videoUrl = bestQuality?.url
                ?.takeIf { it.isNotBlank() }
                ?: video.url
                ?: throw IllegalStateException("小红书视频 URL 为空 (note_id=${noteDetail.id})")

            if (videoUrl.isBlank()) {
                throw IllegalStateException("小红书视频 URL 为空字符串 (note_id=${noteDetail.id})")
            }

            val codecInfo = when {
                bestH264 != null -> "H264"
                bestH265 != null -> "H265"
                else -> "默认"
            }
            val bitrateInfo = bestQuality?.avgBitrate ?: video.avgBitrate
            Timber.d("小红书视频笔记：${noteDetail.id}, 编码=$codecInfo, 码率=$bitrateInfo, 时长=${video.duration}s, 分辨率=${video.width}x${video.height}")

            // 合并标题和描述作为完整文案（更健壮的逻辑）
            val fullContent = buildString {
                val titleText = noteDetail.title?.trim()
                val descText = noteDetail.desc?.trim()

                if (!titleText.isNullOrBlank()) {
                    append(titleText)
                }

                if (!descText.isNullOrBlank()) {
                    if (isNotEmpty() && descText != titleText) {
                        append("\n\n")
                    }
                    if (descText != titleText) {
                        append(descText)
                    }
                }

                if (isEmpty()) {
                    append("小红书视频笔记")
                }
            }

            val videoResult = ParsedMedia.Video(
                id = noteDetail.id,
                platform = "xiaohongshu",
                authorName = authorName,
                authorAvatar = authorAvatar,
                title = fullContent,
                coverUrl = noteDetail.imagesList?.firstOrNull()?.url ?: "",
                stats = stats,
                createTime = noteDetail.time,
                shareUrl = noteDetail.shareInfo?.link,
                videoUrl = videoUrl,
                duration = video.duration,
                width = bestQuality?.width ?: video.width,
                height = bestQuality?.height ?: video.height,
                fileSize = 0,
                bitrate = bitrateInfo.toLong()
            )
            Timber.i("✅ 小红书视频解析成功: ${videoResult.title} (note_id=${noteDetail.id})")
            videoResult
        } else {
            // 图文笔记 - 验证图片列表
            val imagesList = noteDetail.imagesList ?: emptyList()

            // 提取有效的图片 URL（优先使用 original 原始图片，过滤空白和 null）
            val validImageUrls = imagesList
                .mapNotNull { it.original ?: it.url }  // 🎯 优先使用 original（原始图片）
                .filter { it.isNotBlank() }

            if (validImageUrls.isEmpty()) {
                throw IllegalStateException("小红书图文笔记缺少有效图片 (note_id=${noteDetail.id}, type=${noteDetail.type})")
            }

            // 🎯 新增：检测并提取 Live Photo（实况照片）
            val livePhotos = imagesList.mapIndexedNotNull { index, image ->
                val livePhoto = image.livePhoto
                val livePhotoUrl = livePhoto?.url

                if (!livePhotoUrl.isNullOrBlank()) {
                    Timber.d("小红书 Live Photo 检测：图片索引=$index, URL=$livePhotoUrl, 时长=${livePhoto.duration}ms")
                    LivePhotoInfo(
                        imageIndex = index,
                        videoUrl = livePhotoUrl,
                        duration = livePhoto.duration,
                        width = livePhoto.width,
                        height = livePhoto.height
                    )
                } else {
                    null
                }
            }

            if (livePhotos.isNotEmpty()) {
                Timber.i("小红书图文笔记包含 ${livePhotos.size} 个 Live Photo")
            }

            // 合并标题和描述作为完整文案
            val fullContent = buildString {
                val titleText = noteDetail.title?.trim()
                val descText = noteDetail.desc?.trim()

                if (!titleText.isNullOrBlank()) {
                    append(titleText)
                }

                if (!descText.isNullOrBlank()) {
                    if (isNotEmpty() && descText != titleText) {
                        append("\n\n")
                    }
                    if (descText != titleText) {
                        append(descText)
                    }
                }

                if (isEmpty()) {
                    append("小红书图文笔记")
                }
            }

            Timber.d("小红书图文笔记：${noteDetail.id}, 共 ${validImageUrls.size} 张图片")

            val imageResult = ParsedMedia.ImageNote(
                id = noteDetail.id,
                platform = "xiaohongshu",
                authorName = authorName,
                authorAvatar = authorAvatar,
                title = fullContent,
                coverUrl = validImageUrls.firstOrNull() ?: "",
                stats = stats,
                createTime = noteDetail.time,
                shareUrl = noteDetail.shareInfo?.link,
                imageUrls = validImageUrls,
                imageSizes = imagesList.map {
                    ImageSize(it.width, it.height, 0)
                },
                livePhotos = livePhotos.takeIf { it.isNotEmpty() }  // 🎯 只有存在 Live Photo 时才传递
            )
            Timber.i("✅ 小红书图文解析成功: ${imageResult.title} (note_id=${noteDetail.id})")
            imageResult
        }
    }

    // ========================================
    // 快手 Mapper
    // ========================================

    /**
     * 转换快手数据
     */
    fun mapKuaishou(data: KuaishouVideoData): ParsedMedia {
        Timber.d("开始解析快手数据")

        try {
            val photo = data.photo
                ?: throw IllegalStateException("快手数据缺少 photo 字段，TikHub API 可能未返回完整数据")

            Timber.d("快手基础信息 - ID: ${photo.photoId}, 标题: ${photo.caption}")

            // 🎯 新增：判断内容类型（图文 vs 视频）
            // 注意：快手图文的图片存储在 coverUrls 中，而不是 images 字段
            val imageUrls = photo.images?.mapNotNull { it.url }?.filter { it.isNotBlank() }
                ?: photo.coverUrls?.mapNotNull { it.url }?.filter { it.isNotBlank() }
                ?: emptyList()
            val hasImages = imageUrls.isNotEmpty()
            val hasVideo = photo.mainMvUrls?.isNotEmpty() == true

            Timber.d("快手内容类型检测: hasImages=$hasImages (${imageUrls.size}张), hasVideo=$hasVideo")
            Timber.d("数据来源: images=${photo.images?.size}, coverUrls=${photo.coverUrls?.size}, mainMvUrls=${photo.mainMvUrls?.size}")

            val stats = StatsInfo(
                likeCount = photo.likeCount,
                commentCount = photo.commentCount,
                shareCount = photo.shareCount,
                collectCount = 0,
                playCount = photo.viewCount
            )

            // 🎯 修复：userInfo 可能为 null，提供默认值
            val authorName = photo.userInfo?.userName ?: "快手用户"
            val authorAvatar = photo.userInfo?.headUrl ?: ""

            if (photo.userInfo == null) {
                Timber.w("快手作者信息缺失 (photoId=${photo.photoId})")
            }

            // 🎯 核心修复：根据内容类型返回不同的 ParsedMedia
            return if (hasImages && !hasVideo) {
                // 图文内容
                Timber.i("快手图文解析: ${imageUrls.size}张图片")

                ParsedMedia.ImageNote(
                    id = photo.photoId,
                    platform = "kuaishou",
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    title = photo.caption ?: "快手图文",
                    coverUrl = imageUrls.firstOrNull() ?: "",
                    stats = stats,
                    createTime = photo.timestamp,
                    shareUrl = null,
                    imageUrls = imageUrls
                ).also {
                    Timber.d("快手图文解析成功: ${it.title}")
                }

            } else if (hasVideo) {
                // 视频内容
                val videoUrls = photo.mainMvUrls!!

                Timber.d("快手视频可用 URL 数量: ${videoUrls.size}")
                videoUrls.forEachIndexed { index, url ->
                    Timber.d("视频 URL $index: quality=${url.qualityTag}, url=${url.url}")
                }

                // 选择第一个可用 URL（TikHub API 已按质量排序，第一个通常是最高质量）
                val firstUrl = videoUrls.first()
                val videoUrl = firstUrl.url ?: run {
                    Timber.e("快手视频 URL 为空，qualityTag=${firstUrl.qualityTag}")
                    throw IllegalStateException("快手视频 URL 为空")
                }

                val selectedBitrate = 0L  // mainMvUrls 不提供码率信息
                val selectedWidth = photo.width
                val selectedHeight = photo.height
                val selectedFileSize = 0L  // mainMvUrls 不提供文件大小

                Timber.i("快手视频：画质=${firstUrl.qualityTag}, 分辨率=${photo.width}x${photo.height}")

                ParsedMedia.Video(
                    id = photo.photoId,
                    platform = "kuaishou",
                    authorName = authorName,
                    authorAvatar = authorAvatar,
                    title = photo.caption ?: "快手视频",
                    coverUrl = photo.coverUrls?.firstOrNull()?.url ?: "",
                    stats = stats,
                    createTime = photo.timestamp,
                    shareUrl = null,  // shareInfo 现在是字符串格式，不包含完整 URL
                    videoUrl = videoUrl,
                    duration = photo.duration / 1000,
                    width = selectedWidth,
                    height = selectedHeight,
                    fileSize = selectedFileSize,
                    bitrate = selectedBitrate
                ).also {
                    Timber.d("快手视频解析成功: ${it.title}")
                }

            } else {
                // 既没有图片也没有视频
                Timber.e("快手内容解析失败: 既没有图片也没有视频")
                Timber.e("photoId=${photo.photoId}, caption=${photo.caption}")
                Timber.e("images=${photo.images?.size}, mainMvUrls=${photo.mainMvUrls?.size}")
                throw IllegalStateException("快手内容既没有图片也没有视频，无法解析")
            }

        } catch (e: Exception) {
            Timber.e(e, "快手数据映射失败")
            // 记录更详细的错误信息用于调试
            Timber.e("错误详情: ${e.message}")
            Timber.e("数据结构: { photoId: ${data.photo?.photoId}, caption: ${data.photo?.caption} }")
            throw e
        }
    }

    // ========================================
    // B站 Mapper
    // ========================================

    /**
     * 转换 B站 数据
     *
     * 🎯 优化：直接使用 TikHub API 返回的 durl 直链（已完成服务端签名）
     * 不再需要客户端计算 w_rid 签名
     */
    fun mapBilibili(data: BilibiliVideoData): ParsedMedia {
        Timber.d("开始解析 B站 视频数据")

        val videoData = data.data
            ?: throw IllegalStateException("B站数据缺少 data 字段")

        val stats = StatsInfo(
            likeCount = videoData.stat?.like?.toLong() ?: 0,
            commentCount = videoData.stat?.reply?.toLong() ?: 0,
            shareCount = videoData.stat?.share?.toLong() ?: 0,
            collectCount = videoData.stat?.favorite?.toLong() ?: 0,
            playCount = videoData.stat?.view?.toLong() ?: 0
        )

        // 🎯 优先检查 DASH 格式（现代格式，音视频分离）
        val dash = videoData.dash
        val durls = videoData.durl

        Timber.d("📊 B站数据格式检测: dash=${if (dash != null) "存在" else "null"}, durl=${durls?.size ?: 0}个")

        // 🔍 详细调试信息
        Timber.d("🔍 B站视频详细信息:")
        Timber.d("  • BV号: ${videoData.bvid}")
        Timber.d("  • 标题: ${videoData.title}")
        Timber.d("  • UP主: ${videoData.owner?.name}")
        Timber.d("  • 画质: ${videoData.quality}")
        Timber.d("  • CID: ${videoData.cid}")

        // 🔍 检查 durl 详细信息
        if (durls != null) {
            Timber.d("  • durl 列表大小: ${durls.size}")
            durls.forEachIndexed { index, durl ->
                Timber.d("    - durl[$index]: order=${durl.order}, url=${if (durl.url.isNullOrBlank()) "空" else "存在(${durl.url.length}字符)"}, size=${durl.size}, length=${durl.length}ms")
            }
        }

        // 🔍 检查 dash 详细信息
        if (dash != null) {
            Timber.d("  • dash.video 列表大小: ${dash.video?.size ?: 0}")
            Timber.d("  • dash.audio 列表大小: ${dash.audio?.size ?: 0}")
            dash.video?.forEachIndexed { index, video ->
                Timber.d("    - video[$index]: id=${video.id}, bandwidth=${video.bandwidth}, baseUrl=${if (video.baseUrl.isNullOrBlank()) "空" else "存在(${video.baseUrl.length}字符)"}")
            }
        }

        // 🎯 优先使用 DASH 格式（现代格式）
        if (dash != null && !dash.video.isNullOrEmpty()) {
            Timber.d("✅ 使用 DASH 格式解析")
            return parseBilibiliDash(videoData, dash, stats)
        }

        // 🎯 回退到 durl 格式（传统格式）
        if (!durls.isNullOrEmpty()) {
            Timber.d("✅ 使用 durl 格式解析")
            return parseBilibiliDurl(videoData, durls, stats)
        }

        // 🎯 两种格式都没有，构建详细错误信息
        val errorMsg = buildString {
            appendLine("❌ B站视频解析失败：无法获取视频流")
            appendLine()
            appendLine("📺 视频信息：")
            appendLine("  • BV号: ${videoData.bvid}")
            appendLine("  • 标题: ${videoData.title ?: "未知"}")
            appendLine("  • UP主: ${videoData.owner?.name ?: "未知"}")
            appendLine()
            appendLine("🔍 可能的原因：")
            appendLine("  1️⃣ 视频需要登录B站账号才能观看")
            appendLine("  2️⃣ 视频有地区限制（仅限特定地区）")
            appendLine("  3️⃣ 视频已被UP主删除或下架")
            appendLine("  4️⃣ 大会员专享内容（需要B站大会员）")
            appendLine("  5️⃣ 番剧/影视等版权内容")
            appendLine()
            appendLine("💡 建议：")
            appendLine("  • 尝试其他公开的B站视频")
            appendLine("  • 确认视频链接是否正确")
            appendLine("  • 检查视频是否需要特殊权限")
        }

        Timber.e("B站视频解析失败: dash 和 durl 都为空 (bvid=${videoData.bvid})")
        Timber.e("标题: ${videoData.title}, UP主: ${videoData.owner?.name}")

        throw IllegalStateException(errorMsg)
    }

    /**
     * 解析 B站 DASH 格式视频（现代格式，音视频分离）
     */
    private fun parseBilibiliDash(
        videoData: BilibiliVideo,
        dash: BilibiliDash,
        stats: StatsInfo
    ): ParsedMedia.Video {
        Timber.d("🎬 开始解析 DASH 格式")

        // 🎯 从视频流列表中选择最高码率（参考 TikTokWeb 的逻辑）
        val videoStreams = dash.video ?: emptyList()
        Timber.d("📹 DASH 视频流数量: ${videoStreams.size}")

        // 按 bandwidth（码率）降序排序，选择最高质量
        val bestVideo = videoStreams
            .filter { !it.baseUrl.isNullOrBlank() }
            .maxByOrNull { it.bandwidth }
            ?: throw IllegalStateException("DASH 视频流列表为空或无有效 URL")

        val videoUrl = bestVideo.baseUrl!!

        // 🎯 记录详细信息
        val codecInfo = when (bestVideo.codecId) {
            7 -> "AVC/H.264"
            12 -> "HEVC/H.265"
            else -> "未知编码(${bestVideo.codecId})"
        }

        val qualityDesc = when (bestVideo.id) {
            120 -> "8K超高清"
            116 -> "4K超清"
            112 -> "高码率1080P+"
            80 -> "超清1080P"
            64 -> "高清720P"
            32 -> "清晰480P"
            16 -> "流畅360P"
            else -> "未知画质(${bestVideo.id})"
        }

        Timber.i("B站视频(DASH)：画质=$qualityDesc, 编码=$codecInfo, 码率=${bestVideo.bandwidth}, 分辨率=${bestVideo.width}x${bestVideo.height}")

        // 🎯 记录备用 URL（多 CDN 容灾）
        if (!bestVideo.backupUrl.isNullOrEmpty()) {
            Timber.d("📡 备用 URL 数量: ${bestVideo.backupUrl.size}")
        }

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
            videoUrl = videoUrl,
            duration = dash.duration,
            width = bestVideo.width,
            height = bestVideo.height,
            fileSize = 0,  // DASH 格式不提供文件大小
            bitrate = bestVideo.bandwidth
        )
    }

    /**
     * 解析 B站 durl 格式视频（传统格式，音视频混合）
     */
    private fun parseBilibiliDurl(
        videoData: BilibiliVideo,
        durls: List<BilibiliDurl>,
        stats: StatsInfo
    ): ParsedMedia.Video {
        Timber.d("🎬 开始解析 durl 格式")

        // 选择第一个视频流（通常是最高质量）
        val bestDurl = durls.firstOrNull()
            ?: throw IllegalStateException("B站视频 durl 列表为空")

        val videoUrl = bestDurl.url
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("B站视频 URL 为空 (bvid=${videoData.bvid})")

        // 获取清晰度描述
        val qualityDesc = when (videoData.quality) {
            120 -> "8K超高清"
            116 -> "4K超清"
            112 -> "高码率1080P+"
            80 -> "超清1080P"
            64 -> "高清720P"
            32 -> "清晰480P"
            16 -> "流畅360P"
            else -> "未知画质(${videoData.quality})"
        }

        Timber.i("B站视频(durl)：画质=$qualityDesc, 文件大小=${bestDurl.size} 字节, 时长=${bestDurl.length}ms")

        // 记录支持的清晰度列表
        if (!videoData.acceptDescription.isNullOrEmpty()) {
            Timber.d("B站视频支持的清晰度: ${videoData.acceptDescription.joinToString(", ")}")
        }

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
            videoUrl = videoUrl,
            duration = videoData.duration,
            width = videoData.dimension?.width ?: 0,
            height = videoData.dimension?.height ?: 0,
            fileSize = bestDurl.size,
            bitrate = 0  // durl 格式不直接提供码率信息
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

        // 🎯 优先选择 download_addr（通常质量更高），然后是 play_addr
        val downloadUrls = itemInfo.video.downloadAddr?.urlList ?: emptyList()
        val playUrls = itemInfo.video.playAddr?.urlList ?: emptyList()

        val videoUrl = when {
            downloadUrls.isNotEmpty() -> {
                Timber.d("西瓜视频：使用 download_addr (高质量)")
                downloadUrls.first()
            }
            playUrls.isNotEmpty() -> {
                Timber.d("西瓜视频：使用 play_addr (标准质量)")
                playUrls.first()
            }
            else -> throw IllegalStateException("西瓜视频 URL 为空")
        }

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
                // 单图 - 🎯 选择最高分辨率
                val bestImage = item.imageVersions?.candidates?.maxByOrNull { it.width * it.height }
                val imageUrl = bestImage?.url
                    ?: throw IllegalStateException("Instagram 图片 URL 为空")

                Timber.d("Instagram图片：选择最高分辨率 ${bestImage.width}x${bestImage.height}")

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
                    imageSizes = item.imageVersions.candidates.map {
                        ImageSize(it.width, it.height, 0)
                    }
                )
            }
            2 -> {
                // 单视频 - 🎯 优化：考虑 CDN 节点类型优先级
                // type 字段表示 CDN 节点类型，优先选择特定类型以获得更好的下载速度
                val videoVersions = item.videoVersions
                    ?: throw IllegalStateException("Instagram 视频版本列表为空")

                // 选择策略：
                // 1. 优先选择 type=101 或 type=102（主要 CDN 节点）
                // 2. 如果没有，选择最高分辨率
                val preferredTypes = listOf(101, 102)
                val bestVideo = videoVersions
                    .filter { it.type in preferredTypes }
                    .maxByOrNull { it.width * it.height }
                    ?: videoVersions.maxByOrNull { it.width * it.height }
                    ?: throw IllegalStateException("Instagram 视频 URL 为空")

                val videoUrl = bestVideo.url

                val coverImage = item.imageVersions?.candidates?.maxByOrNull { it.width * it.height }
                val coverUrl = coverImage?.url ?: ""

                Timber.i("Instagram视频：选择分辨率 ${bestVideo.width}x${bestVideo.height}, CDN类型=${bestVideo.type}")

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
                    width = bestVideo.width,
                    height = bestVideo.height,
                    fileSize = 0,
                    bitrate = 0
                )
            }
            8 -> {
                // 轮播（Carousel）- 🎯 优化：支持图片和视频混合
                val carouselMedia = item.carouselMedia
                    ?: throw IllegalStateException("Instagram 轮播数据为空")

                val imageUrls = mutableListOf<String>()
                val imageSizes = mutableListOf<ImageSize>()
                var videoCount = 0

                for (media in carouselMedia) {
                    when (media.mediaType) {
                        1, 8 -> {
                            // 图片 - 选择最高分辨率
                            val bestImage = media.imageVersions?.candidates?.maxByOrNull { it.width * it.height }
                            if (bestImage != null) {
                                imageUrls.add(bestImage.url)
                                imageSizes.add(ImageSize(bestImage.width, bestImage.height, 0))
                                Timber.d("Instagram轮播图片：${bestImage.width}x${bestImage.height}")
                            }
                        }
                        2 -> {
                            // 视频 - 🎯 优化：考虑 CDN 节点类型
                            val videoVersions = media.videoVersions
                            if (!videoVersions.isNullOrEmpty()) {
                                val preferredTypes = listOf(101, 102)
                                val bestVideo = videoVersions
                                    .filter { it.type in preferredTypes }
                                    .maxByOrNull { it.width * it.height }
                                    ?: videoVersions.maxByOrNull { it.width * it.height }

                                if (bestVideo != null) {
                                    // 将视频 URL 也添加到 imageUrls（作为特殊标记）
                                    // 注意：这里简化处理，实际应该返回混合媒体列表
                                    videoCount++
                                    Timber.d("Instagram轮播视频：${bestVideo.width}x${bestVideo.height}, CDN类型=${bestVideo.type}")
                                }
                            }
                        }
                    }
                }

                if (imageUrls.isEmpty()) {
                    throw IllegalStateException("Instagram 轮播中没有可用图片（视频数量：$videoCount）")
                }

                Timber.i("Instagram轮播：共 ${imageUrls.size} 张图片, $videoCount 个视频")

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
