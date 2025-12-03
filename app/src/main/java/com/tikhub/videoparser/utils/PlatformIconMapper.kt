package com.tikhub.videoparser.utils

import com.tikhub.videoparser.R

/**
 * 平台图标资源映射工具
 *
 * 用途：根据平台类型返回对应的 icon 资源 ID
 * UI 层可以直接使用这个工具来显示平台 Logo
 */
object PlatformIconMapper {

    /**
     * 获取平台图标资源 ID
     *
     * @param platform Platform 枚举
     * @return Drawable 资源 ID
     */
    fun getIconRes(platform: Platform): Int {
        return when (platform) {
            Platform.DOUYIN -> R.drawable.ic_douyin
            Platform.TIKTOK -> R.drawable.ic_tiktok
            Platform.XIAOHONGSHU -> R.drawable.ic_xiaohongshu
            Platform.KUAISHOU -> R.drawable.ic_kuaishou
            Platform.BILIBILI -> R.drawable.ic_bilibili
            Platform.WEIBO -> R.drawable.ic_weibo
            Platform.XIGUA -> R.drawable.ic_xigua
            Platform.INSTAGRAM -> R.drawable.ic_instagram
            Platform.YOUTUBE -> R.drawable.ic_youtube
            Platform.WEISHI -> R.drawable.ic_weishi
            Platform.UNKNOWN -> R.drawable.ic_web  // 默认全球图标
        }
    }

    /**
     * 获取平台主题色（用于 UI 装饰）
     *
     * 例如：可以用于卡片边框颜色、按钮背景色等
     */
    fun getPlatformColor(platform: Platform): Int {
        return when (platform) {
            Platform.DOUYIN -> 0xFF000000.toInt()      // 黑色
            Platform.TIKTOK -> 0xFFFF0050.toInt()      // 粉红色
            Platform.XIAOHONGSHU -> 0xFFFF2442.toInt() // 小红书红
            Platform.KUAISHOU -> 0xFFFF6600.toInt()    // 橙色
            Platform.BILIBILI -> 0xFFFF8EB3.toInt()    // 粉色
            Platform.WEIBO -> 0xFFE6162D.toInt()       // 微博红
            Platform.XIGUA -> 0xFFFF6633.toInt()       // 橙红色
            Platform.INSTAGRAM -> 0xFFC13584.toInt()   // Instagram渐变主色
            Platform.YOUTUBE -> 0xFFFF0000.toInt()     // YouTube红
            Platform.WEISHI -> 0xFF07C160.toInt()      // 微信绿
            Platform.UNKNOWN -> 0xFF888888.toInt()     // 灰色
        }
    }

    /**
     * 获取平台简称（用于 UI 标签）
     *
     * 例如：在列表中显示 "DY" 而不是完整的 "抖音"
     */
    fun getShortName(platform: Platform): String {
        return when (platform) {
            Platform.DOUYIN -> "DY"
            Platform.TIKTOK -> "TT"
            Platform.XIAOHONGSHU -> "XHS"
            Platform.KUAISHOU -> "KS"
            Platform.BILIBILI -> "BL"
            Platform.WEIBO -> "WB"
            Platform.XIGUA -> "XG"
            Platform.INSTAGRAM -> "INS"
            Platform.YOUTUBE -> "YT"
            Platform.WEISHI -> "WS"
            Platform.UNKNOWN -> "?"
        }
    }

    /**
     * 判断平台是否支持图文内容
     */
    fun supportsImageNote(platform: Platform): Boolean {
        return when (platform) {
            Platform.XIAOHONGSHU,
            Platform.WEIBO,
            Platform.INSTAGRAM,
            Platform.DOUYIN -> true  // 抖音也有图文模式
            else -> false
        }
    }

    /**
     * 判断平台视频是否为横屏主导
     *
     * 用于 UI 布局优化：横屏视频需要不同的展示方式
     */
    fun isLandscapeOriented(platform: Platform): Boolean {
        return when (platform) {
            Platform.BILIBILI,
            Platform.XIGUA,
            Platform.YOUTUBE -> true
            else -> false  // 抖音、快手、TikTok 都是竖屏主导
        }
    }
}

/**
 * Platform 扩展函数
 */

/**
 * 获取平台图标资源 ID（扩展属性）
 */
val Platform.iconRes: Int
    get() = PlatformIconMapper.getIconRes(this)

/**
 * 获取平台主题色（扩展属性）
 */
val Platform.themeColor: Int
    get() = PlatformIconMapper.getPlatformColor(this)

/**
 * 获取平台简称（扩展属性）
 */
val Platform.shortName: String
    get() = PlatformIconMapper.getShortName(this)

/**
 * 是否支持图文内容（扩展属性）
 */
val Platform.supportsImageNote: Boolean
    get() = PlatformIconMapper.supportsImageNote(this)

/**
 * 是否为横屏主导平台（扩展属性）
 */
val Platform.isLandscapeOriented: Boolean
    get() = PlatformIconMapper.isLandscapeOriented(this)
