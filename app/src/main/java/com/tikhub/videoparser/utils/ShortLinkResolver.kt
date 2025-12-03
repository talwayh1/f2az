package com.tikhub.videoparser.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * 短链追踪工具（核心功能）
 *
 * 作用：将平台的短链接还原为真实长链接
 * - 抖音：v.douyin.com -> www.douyin.com/video/xxxxx
 * - 小红书：xhslink.com -> www.xiaohongshu.com/discovery/item/xxxxx?xsec_token=xxxxx
 * - 快手：kw.ai -> www.kuaishou.com/short-video/xxxxx
 *
 * 原理：模拟浏览器发送 GET 请求，手动处理 HTTP 301/302 重定向，获取 Location Header
 */
object ShortLinkResolver {

    private val client = OkHttpClient.Builder()
        .followRedirects(false) // 【关键】禁用自动重定向，手动处理
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * User-Agent 列表（模拟真实设备）
     * 不同平台可能需要不同的 UA，这里提供多个备选
     */
    private val userAgents = listOf(
        // iPhone
        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
        // Android
        "Mozilla/5.0 (Linux; Android 13; SM-S908B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36",
        // 抖音 App WebView
        "com.ss.android.ugc.aweme/180101 (Linux; U; Android 13; zh_CN; SM-G9980; Build/TP1A.220624.014; Cronet/TTNetVersion:2c7c9f61 2022-11-28 QuicVersion:0144d358 2022-03-24)",
        // 小红书 App WebView
        "Mozilla/5.0 (Linux; Android 13; 22081212C Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/116.0.0.0 Mobile Safari/537.36 xhsShareeNative/1.0.0"
    )

    /**
     * 解析短链为长链接
     *
     * @param shortUrl 短链接（例如：https://v.douyin.com/aBcDeFg/）
     * @param maxRedirects 最大重定向次数（防止死循环）
     * @return 真实的长链接（如果解析失败，返回原始 URL）
     */
    suspend fun resolve(shortUrl: String, maxRedirects: Int = 10): String = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Timber.d("========== 开始解析短链 ==========")
        Timber.d("原始 URL: $shortUrl")
        Timber.d("最大重定向次数: $maxRedirects")

        var currentUrl = shortUrl
        var redirectCount = 0
        var totalNetworkTime = 0L

        while (redirectCount < maxRedirects) {
            try {
                val userAgent = selectUserAgent(currentUrl)
                Timber.d("--- 第 ${redirectCount + 1} 次请求 ---")
                Timber.d("当前 URL: $currentUrl")
                Timber.d("User-Agent: $userAgent")

                val request = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .get()
                    .build()

                val requestStartTime = System.currentTimeMillis()
                client.newCall(request).execute().use { response ->
                    val requestDuration = System.currentTimeMillis() - requestStartTime
                    totalNetworkTime += requestDuration
                    Timber.d("⏱️ 请求耗时: ${requestDuration}ms")
                    val statusCode = response.code
                    Timber.d("响应状态码: $statusCode")

                    when {
                        // 301/302/303/307/308 重定向
                        statusCode in 300..399 -> {
                            val location = response.header("Location")
                            Timber.d("Location Header: $location")

                            if (location.isNullOrBlank()) {
                                // 没有 Location Header，返回当前 URL
                                val totalDuration = System.currentTimeMillis() - startTime
                                Timber.w("未找到 Location Header，返回当前 URL")
                                Timber.d("========== 解析结束（无 Location） ==========")
                                Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                                return@withContext currentUrl
                            }

                            // 处理相对路径重定向
                            currentUrl = if (location.startsWith("http")) {
                                location
                            } else {
                                val baseUrl = request.url.toString()
                                resolveRelativeUrl(baseUrl, location)
                            }

                            redirectCount++
                            Timber.d("重定向到: $currentUrl (第 $redirectCount 次)")

                            // 【特殊处理】小红书需要包含 xsec_token 的链接
                            if (currentUrl.contains("xiaohongshu.com") && currentUrl.contains("xsec_token")) {
                                val totalDuration = System.currentTimeMillis() - startTime
                                Timber.i("✅ 检测到小红书长链接（包含 xsec_token），解析完成")
                                Timber.d("========== 解析结束（小红书）==========")
                                Timber.i("最终 URL: $currentUrl")
                                Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                                return@withContext currentUrl
                            }

                            // 【特殊处理】微博长链接检测
                            if (currentUrl.contains("weibo.com") && currentUrl.contains("/status/")) {
                                val totalDuration = System.currentTimeMillis() - startTime
                                Timber.i("✅ 检测到微博长链接，解析完成")
                                Timber.d("========== 解析结束（微博）==========")
                                Timber.i("最终 URL: $currentUrl")
                                Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                                return@withContext currentUrl
                            }
                        }

                        // 200 成功，但可能还不是最终 URL（某些平台会用 JS 跳转）
                        statusCode == 200 -> {
                            val totalDuration = System.currentTimeMillis() - startTime
                            Timber.d("收到 200 响应")
                            // 如果已经是长链接，直接返回
                            if (!isShortUrl(currentUrl)) {
                                Timber.i("检测到长链接，解析完成")
                                Timber.d("========== 解析结束（200 OK）==========")
                                Timber.i("最终 URL: $currentUrl")
                                Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                                return@withContext currentUrl
                            }
                            // 否则继续尝试获取真实 URL（可能需要解析 HTML）
                            Timber.w("收到 200 但仍是短链接，返回当前 URL")
                            Timber.d("========== 解析结束（200 但仍是短链）==========")
                            Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                            return@withContext currentUrl
                        }

                        // 其他状态码（4xx, 5xx）
                        else -> {
                            val totalDuration = System.currentTimeMillis() - startTime
                            // 返回当前URL，不抛出异常
                            Timber.w("收到非重定向状态码: $statusCode，返回当前 URL")
                            Timber.d("========== 解析结束（错误状态码）==========")
                            Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                            return@withContext currentUrl
                        }
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                // 网络不可达，返回原始URL
                val totalDuration = System.currentTimeMillis() - startTime
                Timber.w(e, "❌ 网络不可达，无法解析短链: $currentUrl")
                Timber.e("错误详情: ${e.message}")
                Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                return@withContext currentUrl
            } catch (e: java.net.SocketTimeoutException) {
                // 超时，返回当前URL
                val totalDuration = System.currentTimeMillis() - startTime
                Timber.w(e, "⏰ 短链解析超时: $currentUrl")
                Timber.e("超时详情: ${e.message}")
                Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                return@withContext currentUrl
            } catch (e: Exception) {
                // 其他网络异常，记录日志并返回当前 URL，不中断解析流程
                val totalDuration = System.currentTimeMillis() - startTime
                Timber.e(e, "💥 短链解析异常: $currentUrl")
                Timber.e("异常类型: ${e.javaClass.simpleName}")
                Timber.e("异常消息: ${e.message}")
                Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
                return@withContext currentUrl
            }
        }

        // 达到最大重定向次数
        val totalDuration = System.currentTimeMillis() - startTime
        Timber.w("达到最大重定向次数($maxRedirects): $currentUrl")
        Timber.d("========== 解析结束（达到最大重定向）==========")
        Timber.i("最终 URL: $currentUrl")
        Timber.i("📊 性能统计 - 总耗时: ${totalDuration}ms | 网络耗时: ${totalNetworkTime}ms | 重定向次数: $redirectCount")
        currentUrl
    }

    /**
     * 批量解析短链接
     */
    suspend fun resolveAll(urls: List<String>): List<String> = withContext(Dispatchers.IO) {
        Timber.i("开始批量解析 ${urls.size} 个短链接")
        val results = urls.map { resolve(it) }
        Timber.i("批量解析完成")
        results
    }

    /**
     * 根据 URL 选择合适的 User-Agent
     */
    private fun selectUserAgent(url: String): String {
        return when {
            url.contains("douyin.com") -> userAgents[2] // 抖音 App UA
            url.contains("xiaohongshu.com") || url.contains("xhslink.com") -> userAgents[3] // 小红书 UA
            url.contains("kuaishou.com") -> userAgents[1] // Android UA
            url.contains("weibo.com") || url.contains("t.cn") -> userAgents[0] // 微博使用 iPhone UA
            url.contains("bilibili.com") || url.contains("b23.tv") -> userAgents[1] // B站使用 Android UA
            else -> userAgents[0] // 默认 iPhone UA
        }
    }

    /**
     * 判断是否为短链接
     */
    private fun isShortUrl(url: String): Boolean {
        val shortDomains = listOf(
            "v.douyin.com",
            "vt.tiktok.com",
            "vm.tiktok.com",
            "xhslink.com",
            "kw.ai",
            "t.cn",        // 微博短链
            "weibo.cn",    // 微博短链
            "b23.tv"       // B站短链
        )
        return shortDomains.any { url.contains(it, ignoreCase = true) }
    }

    /**
     * 解析相对路径 URL
     */
    private fun resolveRelativeUrl(baseUrl: String, relativePath: String): String {
        return try {
            val base = java.net.URL(baseUrl)
            java.net.URL(base, relativePath).toString()
        } catch (e: Exception) {
            relativePath
        }
    }
}
