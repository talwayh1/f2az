package com.tikhub.videoparser.utils

import android.os.Build
import com.tikhub.videoparser.data.model.DouyinBitRate
import com.tikhub.videoparser.data.model.DouyinUrlContainer
import timber.log.Timber

/**
 * 视频画质智能选择器
 *
 * 功能：
 * 1. 基于 bit_rate 列表进行数据驱动的画质选择
 * 2. 支持 H.265 编码优先（画质更好体积更小）
 * 3. 考虑设备兼容性
 * 4. 提供多级降级策略
 *
 * 设计原则：
 * - 从"字符串替换"转向"数据驱动决策"
 * - 100% 命中最高画质（4K/60fps）
 * - 智能适配设备能力
 */
object VideoQualitySelector {

    /**
     * 视频候选项（用于排序和选择）
     */
    data class VideoCandidate(
        val url: String,
        val bitRate: Long,
        val isH265: Boolean,
        val fps: Int,
        val gearName: String?,
        val source: String,  // "bit_rate_list", "download_addr", "play_addr"
        val dataSize: Long,
        val codecType: String? = null,  // 视频编码类型（用于过滤不兼容的编码）
        val isBytevc1Value: Int = 0  // 原始编码标识 (0=H.264, 1=H.265, 2=ByteVC2)
    )

    /**
     * 设备能力检测
     */
    object DeviceCapability {
        /**
         * 检测设备是否支持 H.265 (HEVC) 硬件解码
         */
        fun supportsH265(): Boolean {
            // Android 5.0+ 开始支持 H.265
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        }

        /**
         * 检测设备是否支持高帧率视频（60fps+）
         */
        fun supportsHighFrameRate(): Boolean {
            // Android 6.0+ 对高帧率支持更好
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        }
    }

    /**
     * 从抖音 bit_rate 列表中选择最佳视频
     *
     * 策略：
     * 1. 优先选择 bit_rate 列表中的最高码率
     * 2. 如果设备支持 H.265，优先选择 H.265 编码（相同画质下体积更小）
     * 3. 如果设备不支持 H.265，过滤掉 H.265 编码
     * 4. 降级策略：bit_rate 列表 -> download_addr -> play_addr
     *
     * @param bitRateList TikHub API 返回的 bit_rate 列表
     * @param downloadAddr 下载地址（降级选项）
     * @param playAddr 播放地址（最后降级选项）
     * @param preferH265 是否优先选择 H.265（默认根据设备能力自动判断）
     * @return 最佳视频 URL 和元数据
     */
    fun selectBestDouyinVideo(
        bitRateList: List<DouyinBitRate>?,
        downloadAddr: DouyinUrlContainer?,
        playAddr: DouyinUrlContainer?,
        preferH265: Boolean = DeviceCapability.supportsH265()
    ): VideoCandidate? {

        Timber.d("开始智能画质选择 - bit_rate 列表大小: ${bitRateList?.size}, 设备支持 H.265: $preferH265")

        // 🎯 策略 A：优先使用 bit_rate 列表（数据源最准确）
        if (!bitRateList.isNullOrEmpty()) {
            val candidates = bitRateList.mapNotNull { bitRate ->
                val url = bitRate.playAddr?.getFirstUrl()
                if (url.isNullOrBlank()) {
                    Timber.w("bit_rate 项缺少 URL: gearName=${bitRate.gearName}")
                    return@mapNotNull null
                }

                VideoCandidate(
                    url = url,
                    bitRate = bitRate.bitRate,
                    isH265 = bitRate.isBytevc1 == 1,  // 1=H.265, 2=ByteVC2
                    fps = bitRate.fps,
                    gearName = bitRate.gearName,
                    source = "bit_rate_list",
                    dataSize = bitRate.playAddr.dataSize,
                    codecType = bitRate.videoCodecType,
                    isBytevc1Value = bitRate.isBytevc1  // 传递原始值用于过滤
                )
            }

            if (candidates.isNotEmpty()) {
                // 🎯 第一步：分离兼容和不兼容的编码
                val compatibleCodecs = listOf("h264", "avc", "h265", "hevc", "bytevc1")
                val compatibleCandidates = mutableListOf<VideoCandidate>()
                val bytevc2Candidates = mutableListOf<VideoCandidate>()

                candidates.forEach { candidate ->
                    when {
                        // ByteVC2 编码
                        candidate.isBytevc1Value == 2 -> {
                            Timber.d("🔍 发现 ByteVC2 编码: 码率=${candidate.bitRate}, 档位=${candidate.gearName}")
                            bytevc2Candidates.add(candidate)
                        }
                        // 其他不兼容编码
                        candidate.codecType?.lowercase()?.let { codec ->
                            !compatibleCodecs.any { it == codec }
                        } == true -> {
                            Timber.w("⚠️ 发现不兼容编码: ${candidate.codecType}, 码率=${candidate.bitRate}")
                        }
                        // 兼容编码 (H.264, H.265)
                        else -> {
                            compatibleCandidates.add(candidate)
                        }
                    }
                }

                Timber.i("📊 候选项统计: 兼容编码=${compatibleCandidates.size}, ByteVC2=${bytevc2Candidates.size}")

                // 🎯 第二步：优先选择兼容编码
                val filteredCandidates = if (compatibleCandidates.isNotEmpty()) {
                    Timber.i("✅ 使用兼容编码 (H.264/H.265)")
                    // 如果设备不支持 H.265,进一步过滤
                    if (preferH265) {
                        compatibleCandidates
                    } else {
                        val h264Only = compatibleCandidates.filter { !it.isH265 }
                        if (h264Only.isNotEmpty()) {
                            Timber.d("设备不支持 H.265，过滤后剩余 ${h264Only.size} 个候选项")
                            h264Only
                        } else {
                            Timber.w("没有 H.264 编码，保留 H.265 候选项")
                            compatibleCandidates
                        }
                    }
                } else if (bytevc2Candidates.isNotEmpty()) {
                    // 🎯 关键改动：如果只有 ByteVC2,选择 ByteVC2 (不降级到有水印视频)
                    Timber.w("⚠️ 只有 ByteVC2 编码可用，选择 ByteVC2 (无水印优先)")
                    Timber.w("⚠️ ByteVC2 可能在部分设备上无法播放，建议使用 VLC 等播放器")
                    bytevc2Candidates
                } else {
                    Timber.e("❌ 没有任何可用的候选项")
                    emptyList()
                }

                // 排序策略：
                // 1. 优先选择 H.265（如果设备支持）
                // 2. 按码率倒序
                // 3. 按 FPS 倒序
                val bestCandidate = filteredCandidates.sortedWith(
                    compareByDescending<VideoCandidate> { if (preferH265 && it.isH265) 1 else 0 }
                        .thenByDescending { it.bitRate }
                        .thenByDescending { it.fps }
                ).firstOrNull()

                if (bestCandidate != null) {
                    val codecWarning = if (bestCandidate.isBytevc1Value == 2) " [⚠️ ByteVC2]" else ""
                    Timber.i("✅ 选择最佳画质: 码率=${bestCandidate.bitRate}, H.265=${bestCandidate.isH265}, " +
                            "FPS=${bestCandidate.fps}, 档位=${bestCandidate.gearName}$codecWarning")
                    return bestCandidate
                }
            }
        }

        // 🎯 策略 B：降级到 download_addr（优先选择无水印）
        val downloadUrl = downloadAddr?.getFirstUrl()
        if (!downloadUrl.isNullOrBlank()) {
            // 检查是否有水印
            val hasWatermark = downloadUrl.contains("playwm")
            if (hasWatermark) {
                Timber.w("⚠️ download_addr 包含水印，尝试去除水印")
                val noWatermarkUrl = removeDouyinWatermark(downloadUrl)
                Timber.d("降级到 download_addr (已去水印)")
                return VideoCandidate(
                    url = noWatermarkUrl,
                    bitRate = 0,
                    isH265 = false,
                    fps = 0,
                    gearName = "download_addr_no_wm",
                    source = "download_addr",
                    dataSize = downloadAddr.dataSize
                )
            } else {
                Timber.d("降级到 download_addr (无水印)")
                return VideoCandidate(
                    url = downloadUrl,
                    bitRate = 0,
                    isH265 = false,
                    fps = 0,
                    gearName = "download_addr",
                    source = "download_addr",
                    dataSize = downloadAddr.dataSize
                )
            }
        }

        // 🎯 策略 C：最后降级到 play_addr（同样检查水印）
        val playUrl = playAddr?.getFirstUrl()
        if (!playUrl.isNullOrBlank()) {
            val hasWatermark = playUrl.contains("playwm")
            if (hasWatermark) {
                Timber.w("⚠️ play_addr 包含水印，尝试去除水印")
                val noWatermarkUrl = removeDouyinWatermark(playUrl)
                Timber.d("降级到 play_addr (已去水印)")
                return VideoCandidate(
                    url = noWatermarkUrl,
                    bitRate = 0,
                    isH265 = false,
                    fps = 0,
                    gearName = "play_addr_no_wm",
                    source = "play_addr",
                    dataSize = playAddr.dataSize
                )
            } else {
                Timber.d("降级到 play_addr (无水印)")
                return VideoCandidate(
                    url = playUrl,
                    bitRate = 0,
                    isH265 = false,
                    fps = 0,
                    gearName = "play_addr",
                    source = "play_addr",
                    dataSize = playAddr.dataSize
                )
            }
        }

        Timber.e("❌ 无法找到任何可用的视频 URL")
        return null
    }

    /**
     * 去除抖音水印（如果 URL 包含 playwm）
     *
     * 注意：这是降级策略，优先使用 bit_rate 列表中的无水印链接
     */
    fun removeDouyinWatermark(url: String): String {
        return if (url.contains("playwm")) {
            val newUrl = url.replace("playwm", "play")
            Timber.d("去除水印: playwm -> play")
            newUrl
        } else {
            url
        }
    }

    /**
     * 格式化码率显示
     */
    fun formatBitRate(bitRate: Long): String {
        return when {
            bitRate >= 1_000_000 -> String.format(java.util.Locale.US, "%.1f Mbps", bitRate / 1_000_000.0)
            bitRate >= 1_000 -> String.format(java.util.Locale.US, "%.0f Kbps", bitRate / 1_000.0)
            else -> "$bitRate bps"
        }
    }

    /**
     * 根据 gear_name 推测画质等级
     */
    fun parseQualityFromGearName(gearName: String?): String {
        return when {
            gearName == null -> "未知"
            gearName.contains("2160", ignoreCase = true) -> "4K"
            gearName.contains("1080", ignoreCase = true) -> "1080P"
            gearName.contains("720", ignoreCase = true) -> "720P"
            gearName.contains("540", ignoreCase = true) -> "540P"
            gearName.contains("480", ignoreCase = true) -> "480P"
            gearName.contains("360", ignoreCase = true) -> "360P"
            else -> gearName
        }
    }
}
