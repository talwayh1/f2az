package com.tikhub.videoparser.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
        .connectTimeout(5, TimeUnit.SECONDS)  // 降低连接超时（10秒→5秒）
        .readTimeout(5, TimeUnit.SECONDS)     // 降低读取超时（10秒→5秒）
        .build()

    /**
     * 短链缓存（LRU缓存，最多缓存500个）
     * 避免重复解析相同的短链
     * 优化：增加缓存大小 100 → 500
     */
    private val cache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > 500  // 100 → 500
        }
    }

    // 缓存统计
    private var cacheHits = 0
    private var cacheMisses = 0
    private var totalRedirects = 0
    private var lastStatsLogTime = System.currentTimeMillis()

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
     * 执行异步 HTTP 请求（避免主线程阻塞）
     */
    private suspend fun executeAsync(request: Request): Response = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)

        continuation.invokeOnCancellation {
            call.cancel()
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) {
                    continuation.resume(response)
                }
            }
        })
    }

    /**
     * 解析短链为长链接
     *
     * @param shortUrl 短链接（例如：https://v.douyin.com/aBcDeFg/）
     * @param maxRedirects 最大重定向次数（防止死循环）
     * @return 真实的长链接（如果解析失败，返回原始 URL）
     */
    suspend fun resolve(shortUrl: String, maxRedirects: Int = 10): String = withContext(Dispatchers.IO) {
        // 检查缓存
        cache[shortUrl]?.let {
            cacheHits++
            logCacheStats()
            Timber.d("✅ 缓存命中: $it")
            return@withContext it
        }

        cacheMisses++
        var currentUrl = shortUrl
        var redirectCount = 0

        while (redirectCount < maxRedirects) {
            try {
                val userAgent = selectUserAgent(currentUrl)

                // 优化：使用 HEAD 请求代替 GET，只获取响应头，不下载响应体
                val requestBuilder = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")

                // 【特殊处理】小红书短链接需要添加 Referer 并使用 GET 请求
                val useGetRequest = currentUrl.contains("xhslink.com")
                if (useGetRequest) {
                    Timber.d("🔗 检测到小红书短链接，使用 GET 请求并添加特殊请求头")
                    requestBuilder
                        .header("Referer", "https://www.xiaohongshu.com/")
                        .header("Accept-Encoding", "gzip, deflate")
                        .header("Connection", "keep-alive")
                }

                val request = if (useGetRequest) {
                    requestBuilder.get().build()
                } else {
                    requestBuilder.head().build()  // 其他平台使用 HEAD
                }

                // 使用异步请求，避免阻塞主线程
                val response = executeAsync(request)
                response.use {
                    val statusCode = response.code

                    when {
                        // 301/302/303/307/308 重定向
                        statusCode in 300..399 -> {
                            val location = response.header("Location")

                            if (location.isNullOrBlank()) {
                                Timber.w("未找到 Location Header，返回当前 URL")
                                cache[shortUrl] = currentUrl
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
                            totalRedirects++
                            Timber.d("重定向 #$redirectCount: $currentUrl")

                            // 【特殊处理】小红书需要包含 xsec_token 的链接
                            if (currentUrl.contains("xiaohongshu.com") && currentUrl.contains("xsec_token")) {
                                cache[shortUrl] = currentUrl
                                return@withContext currentUrl
                            }

                            // 【特殊处理】微博长链接检测
                            if (currentUrl.contains("weibo.com") && currentUrl.contains("/status/")) {
                                cache[shortUrl] = currentUrl
                                return@withContext currentUrl
                            }
                        }

                        // 200 成功，但可能还不是最终 URL（某些平台会用 JS 跳转）
                        statusCode == 200 -> {
                            // 如果已经是长链接，直接返回
                            if (!isShortUrl(currentUrl)) {
                                cache[shortUrl] = currentUrl
                                return@withContext currentUrl
                            }
                            // 否则返回当前 URL
                            cache[shortUrl] = currentUrl
                            return@withContext currentUrl
                        }

                        // 其他状态码（4xx, 5xx）
                        else -> {
                            if (currentUrl.contains("xhslink.com")) {
                                Timber.w("⚠️ 小红书短链接返回状态码: $statusCode (可能已过期或需要特定环境)")
                            } else {
                                Timber.w("收到非重定向状态码: $statusCode，返回当前 URL")
                            }
                            cache[shortUrl] = currentUrl
                            return@withContext currentUrl
                        }
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                Timber.e(e, "网络不可达，无法解析短链: $currentUrl")
                cache[shortUrl] = currentUrl
                return@withContext currentUrl
            } catch (e: java.net.SocketTimeoutException) {
                Timber.e(e, "短链解析超时: $currentUrl")
                cache[shortUrl] = currentUrl
                return@withContext currentUrl
            } catch (e: IOException) {
                Timber.e(e, "网络请求失败: $currentUrl")
                cache[shortUrl] = currentUrl
                return@withContext currentUrl
            } catch (e: Exception) {
                Timber.e(e, "短链解析异常: $currentUrl")
                cache[shortUrl] = currentUrl
                return@withContext currentUrl
            }
        }

        // 达到最大重定向次数
        Timber.w("达到最大重定向次数($maxRedirects): $currentUrl")
        cache[shortUrl] = currentUrl
        currentUrl
    }

    /**
     * 批量解析短链接
     */
    suspend fun resolveAll(urls: List<String>): List<String> = withContext(Dispatchers.IO) {
        urls.map { resolve(it) }
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

    /**
     * 输出缓存统计信息（每30秒输出一次）
     */
    private fun logCacheStats() {
        val now = System.currentTimeMillis()
        if (now - lastStatsLogTime > 30000) {  // 30秒
            val totalRequests = cacheHits + cacheMisses
            val hitRate = if (totalRequests > 0) {
                (cacheHits * 100.0 / totalRequests).toInt()
            } else {
                0
            }
            val avgRedirects = if (cacheMisses > 0) {
                (totalRedirects.toDouble() / cacheMisses).let { "%.1f".format(java.util.Locale.US, it) }
            } else {
                "0.0"
            }

            Timber.i("📊 【短链缓存统计】")
            Timber.i("  ├─ 缓存命中: $cacheHits 次")
            Timber.i("  ├─ 缓存未命中: $cacheMisses 次")
            Timber.i("  ├─ 命中率: $hitRate%")
            Timber.i("  ├─ 缓存大小: ${cache.size}/500")
            Timber.i("  └─ 平均重定向次数: $avgRedirects")

            lastStatsLogTime = now
        }
    }
}
