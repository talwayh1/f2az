package com.tikhub.videoparser.utils

/**
 * TikHub API 统一错误码处理器
 *
 * 功能：
 * 1. 将 TikHub API 返回的错误码转换为用户友好的错误消息
 * 2. 支持中文错误提示
 * 3. 提供详细的错误分类
 *
 * 参考：TikHub API 文档 - 错误码说明
 */
object TikHubErrorHandler {

    /**
     * 错误码映射表
     * 基于 TikHub API 文档的常见错误码
     */
    private val ERROR_CODE_MAP = mapOf(
        // 成功
        200 to "请求成功",

        // 客户端错误 (4xx)
        400 to "请求参数错误，请检查链接格式",
        401 to "API 密钥无效或已过期，请联系开发者",
        403 to "访问被拒绝，可能是 API 配额不足",
        404 to "内容不存在或已被删除",
        429 to "请求过于频繁，请稍后再试",

        // 服务端错误 (5xx)
        500 to "服务器内部错误，请稍后重试",
        502 to "网关错误，服务暂时不可用",
        503 to "服务暂时不可用，请稍后重试",
        504 to "请求超时，请检查网络连接",

        // TikHub 特定错误码
        1001 to "视频解析失败，可能是链接格式不正确",
        1002 to "视频已被删除或设置为私密",
        1003 to "视频地区限制，无法访问",
        1004 to "视频需要登录才能查看",
        1005 to "视频解析超时，请重试",

        2001 to "API 密钥无效",
        2002 to "API 配额已用完",
        2003 to "API 密钥已过期",
        2004 to "API 访问频率超限",

        3001 to "平台接口异常，请稍后重试",
        3002 to "平台返回数据格式错误",
        3003 to "平台限流，请稍后重试"
    )

    /**
     * 根据错误码获取用户友好的错误消息
     *
     * @param code TikHub API 返回的错误码
     * @param defaultMessage 默认错误消息（如果错误码未映射）
     * @return 用户友好的错误消息
     */
    fun getErrorMessage(code: Int, defaultMessage: String? = null): String {
        return ERROR_CODE_MAP[code] ?: defaultMessage ?: "未知错误 (错误码: $code)"
    }

    /**
     * 判断错误是否可重试
     *
     * @param code 错误码
     * @return true 表示可以重试，false 表示不应重试
     */
    fun isRetryable(code: Int): Boolean {
        return when (code) {
            // 网络相关错误，可重试
            429, 500, 502, 503, 504, 1005, 3001, 3003 -> true
            // 客户端错误或权限问题，不应重试
            400, 401, 403, 404, 1002, 1003, 1004, 2001, 2002, 2003, 2004 -> false
            // 其他错误，默认可重试一次
            else -> true
        }
    }

    /**
     * 获取错误类型分类
     *
     * @param code 错误码
     * @return 错误类型描述
     */
    fun getErrorType(code: Int): String {
        return when (code) {
            in 200..299 -> "成功"
            in 400..499 -> "客户端错误"
            in 500..599 -> "服务器错误"
            in 1000..1999 -> "解析错误"
            in 2000..2999 -> "认证错误"
            in 3000..3999 -> "平台错误"
            else -> "未知错误"
        }
    }

    /**
     * 格式化完整的错误信息（用于日志记录）
     *
     * @param code 错误码
     * @param message API 返回的原始消息
     * @return 格式化的错误信息
     */
    fun formatErrorLog(code: Int, message: String?): String {
        val userMessage = getErrorMessage(code, message)
        val errorType = getErrorType(code)
        val retryable = if (isRetryable(code)) "可重试" else "不可重试"

        return "[$errorType] 错误码: $code, 消息: $userMessage ($retryable)"
    }
}
