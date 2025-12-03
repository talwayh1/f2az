package com.tikhub.videoparser.utils

/**
 * URL 提取工具
 * 移植 PHP 的 preg_match_all 逻辑，从混合文本中提取所有 http/https 链接
 */
object UrlExtractor {

    /**
     * 从文本中提取所有 URL
     *
     * @param text 输入的混合文本（可能包含多个链接、描述文字等）
     * @return 提取到的 URL 列表
     *
     * 示例：
     * "快来看这个视频 https://v.douyin.com/aBcDeFg/ 超级好笑"
     * -> ["https://v.douyin.com/aBcDeFg/"]
     */
    fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        // 正则表达式：匹配 http:// 或 https:// 开头的 URL
        // 支持路径、参数、锚点等完整 URL 格式
        val urlPattern = Regex(
            pattern = "(https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w.,@?^=%&:/~+#\\-]*[\\w@?^=%&/~+#\\-])?)",
            options = setOf(RegexOption.IGNORE_CASE)
        )

        return urlPattern.findAll(text)
            .map { it.value }
            .distinct() // 去重
            .toList()
    }

    /**
     * 检查文本中是否包含 URL
     */
    fun containsUrl(text: String): Boolean {
        return extractUrls(text).isNotEmpty()
    }

    /**
     * 获取第一个 URL（常用于剪贴板单链接场景）
     */
    fun getFirstUrl(text: String): String? {
        return extractUrls(text).firstOrNull()
    }
}
