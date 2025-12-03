package com.tikhub.videoparser.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

/**
 * 格式化工具
 * 用于格式化视频大小、��长、统计数据等
 */
object FormatUtils {

    /**
     * 格式化文件大小
     * @param bytes 字节数
     * @return 格式化后的字符串（例如：12.5 MB）
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes < 0) return "未知"
        if (bytes == 0L) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val k = 1024.0
        val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt().coerceAtMost(units.size - 1)
        val size = bytes / k.pow(i)

        return String.format(Locale.US, "%.2f %s", size, units[i])
    }

    /**
     * 格式化视频时长
     * @param seconds 秒数
     * @return 格式化后的字符串（例如：02:35）
     */
    fun formatDuration(seconds: Int): String {
        if (seconds < 0) return "00:00"

        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
    }

    /**
     * 格式化毫秒时长
     */
    fun formatDurationFromMillis(millis: Long): String {
        return formatDuration((millis / 1000).toInt())
    }

    /**
     * 格式化统计数字（点赞、评论等）
     * @param count 数量
     * @return 格式化后的字符串（例如：1.2万、10.5万）
     */
    fun formatCount(count: Long): String {
        return when {
            count < 10000 -> count.toString()
            count < 100000000 -> String.format(Locale.US, "%.1f万", count / 10000.0)
            else -> String.format(Locale.US, "%.1f亿", count / 100000000.0)
        }
    }

    /**
     * 格式化分辨率
     * @param width 宽度
     * @param height 高度
     * @return 格式化后的字符串（例如：1080x1920）
     */
    fun formatResolution(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "未知"
        return "${width}x${height}"
    }

    /**
     * 格式化码率
     * @param bitrate 比特率（bps）
     * @return 格式化后的字符串（例如：2.5 Mbps）
     */
    fun formatBitrate(bitrate: Long): String {
        if (bitrate <= 0) return "未知"

        return when {
            bitrate < 1000 -> "${bitrate} bps"
            bitrate < 1000000 -> String.format(Locale.US, "%.1f Kbps", bitrate / 1000.0)
            else -> String.format(Locale.US, "%.1f Mbps", bitrate / 1000000.0)
        }
    }

    /**
     * 截断长文本
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    fun truncate(text: String, maxLength: Int = 100): String {
        if (text.length <= maxLength) return text
        return text.substring(0, maxLength) + "..."
    }

    /**
     * 格式化时间戳
     * @param timestamp 时间戳（秒）
     * @return 格式化后的时间字符串
     */
    fun formatTimestamp(timestamp: Long): String {
        return if (timestamp > 0) {
            val date = Date(timestamp * 1000)
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
        } else {
            "未知时间"
        }
    }

    /**
     * 格式化数字（别名方法）
     */
    fun formatNumber(count: Long): String = formatCount(count)
}
