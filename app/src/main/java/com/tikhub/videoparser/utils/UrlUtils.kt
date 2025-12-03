package com.tikhub.videoparser.utils

import java.util.regex.Pattern

/**
 * URL工具类
 * 移植自PHP的findAllUrlsInText逻辑
 */
object UrlUtils {

    private val URL_PATTERN = Pattern.compile(
        """https?://[^\s<>"']+""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * 从文本中提取所有URL
     * @param text 输入文本
     * @return 提取的URL列表
     */
    fun findAllUrlsInText(text: String): List<String> {
        val urls = mutableListOf<String>()
        val matcher = URL_PATTERN.matcher(text)

        while (matcher.find()) {
            val url = matcher.group().trim()
            if (url.isNotBlank()) {
                urls.add(url)
            }
        }

        return urls.distinct() // 去重
    }

    /**
     * 检查是否为有效的URL
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            android.net.Uri.parse(url).scheme?.let { scheme ->
                scheme == "http" || scheme == "https"
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 清理URL（移除末尾的标点符号等）
     */
    fun cleanUrl(url: String): String {
        return url.trim()
            .removeSuffix(".")
            .removeSuffix(",")
            .removeSuffix("!")
            .removeSuffix("?")
            .removeSuffix(";")
    }
}