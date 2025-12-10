package com.tikhub.videoparser.utils

/**
 * API 费用计算器
 *
 * 根据不同平台和 API 调用次数估算费用
 */
object CostCalculator {

    // API 调用单价（人民币/次）
    private const val PRICE_PER_CALL_DOUYIN = 0.002       // 抖音：¥0.002/次
    private const val PRICE_PER_CALL_TIKTOK = 0.003       // TikTok：¥0.003/次
    private const val PRICE_PER_CALL_XIAOHONGSHU = 0.0025 // 小红书：¥0.0025/次
    private const val PRICE_PER_CALL_KUAISHOU = 0.002     // 快手：¥0.002/次
    private const val PRICE_PER_CALL_BILIBILI = 0.0015    // B站：¥0.0015/次
    private const val PRICE_PER_CALL_WEIBO = 0.002        // 微博：¥0.002/次
    private const val PRICE_PER_CALL_XIGUA = 0.0018       // 西瓜：¥0.0018/次
    private const val PRICE_PER_CALL_INSTAGRAM = 0.004    // Instagram：¥0.004/次
    private const val PRICE_PER_CALL_YOUTUBE = 0.003      // YouTube：¥0.003/次
    private const val PRICE_PER_CALL_OTHER = 0.002        // 其他：¥0.002/次

    /**
     * 计算单次解析费用
     *
     * @param platform 平台
     * @param apiCallCount API 调用次数（通常为1，失败重试时可能更多）
     * @return 费用（人民币）
     */
    fun calculateCost(platform: Platform, apiCallCount: Int = 1): Double {
        val pricePerCall = when (platform) {
            Platform.DOUYIN -> PRICE_PER_CALL_DOUYIN
            Platform.TIKTOK -> PRICE_PER_CALL_TIKTOK
            Platform.XIAOHONGSHU -> PRICE_PER_CALL_XIAOHONGSHU
            Platform.KUAISHOU -> PRICE_PER_CALL_KUAISHOU
            Platform.BILIBILI -> PRICE_PER_CALL_BILIBILI
            Platform.WEIBO -> PRICE_PER_CALL_WEIBO
            Platform.XIGUA -> PRICE_PER_CALL_XIGUA
            Platform.INSTAGRAM -> PRICE_PER_CALL_INSTAGRAM
            Platform.YOUTUBE -> PRICE_PER_CALL_YOUTUBE
            else -> PRICE_PER_CALL_OTHER
        }

        return pricePerCall * apiCallCount
    }

    /**
     * 格式化费用显示
     */
    fun formatCost(cost: Double): String {
        return when {
            cost < 0.01 -> "¥${String.format("%.4f", cost)}"
            cost < 1.0 -> "¥${String.format("%.3f", cost)}"
            else -> "¥${String.format("%.2f", cost)}"
        }
    }

    /**
     * 估算每日费用
     *
     * @param platform 平台
     * @param dailyParseCount 每日解析次数
     * @return 每日费用（人民币）
     */
    fun estimateDailyCost(platform: Platform, dailyParseCount: Int): Double {
        return calculateCost(platform) * dailyParseCount
    }

    /**
     * 估算每月费用
     */
    fun estimateMonthlyCost(platform: Platform, dailyParseCount: Int): Double {
        return estimateDailyCost(platform, dailyParseCount) * 30
    }
}
