package com.tikhub.videoparser.data.model

/**
 * 解析结果包装类
 * 包含解析结果、耗时和费用信息
 */
data class ParseResultWrapper(
    val media: ParsedMedia,
    val parseTimeMs: Long,           // 总耗时（毫秒）
    val networkTimeMs: Long,         // 网络请求耗时（毫秒）
    val estimatedCostCNY: Double     // 预估费用（人民币）
) {
    /**
     * 获取耗时显示文本
     */
    fun getTimeDisplay(): String {
        return when {
            parseTimeMs < 1000 -> "${parseTimeMs}ms"
            parseTimeMs < 60000 -> String.format("%.2fs", parseTimeMs / 1000.0)
            else -> String.format("%.2fmin", parseTimeMs / 60000.0)
        }
    }

    /**
     * 获取费用显示文本
     */
    fun getCostDisplay(): String {
        return when {
            estimatedCostCNY < 0.01 -> "¥${String.format("%.4f", estimatedCostCNY)}"
            estimatedCostCNY < 1.0 -> "¥${String.format("%.3f", estimatedCostCNY)}"
            else -> "¥${String.format("%.2f", estimatedCostCNY)}"
        }
    }

    /**
     * 获取性能等级
     */
    fun getPerformanceLevel(): PerformanceLevel {
        return when {
            parseTimeMs < 500 -> PerformanceLevel.EXCELLENT
            parseTimeMs < 1000 -> PerformanceLevel.GOOD
            parseTimeMs < 2000 -> PerformanceLevel.NORMAL
            else -> PerformanceLevel.SLOW
        }
    }
}

/**
 * 性能等级
 */
enum class PerformanceLevel(val displayName: String, val emoji: String) {
    EXCELLENT("极速", "🚀"),
    GOOD("快速", "⚡"),
    NORMAL("正常", "✅"),
    SLOW("较慢", "🐌")
}
