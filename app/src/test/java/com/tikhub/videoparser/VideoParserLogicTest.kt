package com.tikhub.videoparser

import com.google.common.truth.Truth.assertThat
import com.tikhub.videoparser.data.mapper.MediaMapper
import com.tikhub.videoparser.data.model.*
import com.tikhub.videoparser.utils.Platform
import com.tikhub.videoparser.utils.UrlExtractor
import org.junit.Test

/**
 * 视频解析器核心逻辑单元测试
 *
 * 测试范围：
 * 1. URL 提取逻辑
 * 2. 平台识别逻辑
 * 3. 数据映射逻辑
 * 4. 错误处理逻辑
 */
class VideoParserLogicTest {

    // ========================================
    // 测试 1: URL 提取逻辑
    // ========================================

    @Test
    fun `test URL extraction from mixed text`() {
        val input = "Check this video https://v.douyin.com/iFRAdjfF/ it's awesome!"
        val urls = UrlExtractor.extractUrls(input)

        assertThat(urls).isNotEmpty()
        assertThat(urls.first()).contains("douyin.com")
    }

    @Test
    fun `test URL extraction with multiple URLs`() {
        val input = """
            Video 1: https://v.douyin.com/abc123/
            Video 2: https://www.tiktok.com/@user/video/123456789
        """.trimIndent()
        val urls = UrlExtractor.extractUrls(input)

        assertThat(urls).hasSize(2)
    }

    @Test
    fun `test URL extraction returns empty for no URLs`() {
        val input = "Just some random text without any URLs"
        val urls = UrlExtractor.extractUrls(input)

        assertThat(urls).isEmpty()
    }

    // ========================================
    // 测试 2: 平台识别逻辑
    // ========================================

    @Test
    fun `test platform detection for Douyin`() {
        val url = "https://www.douyin.com/video/1234567890"
        val platform = Platform.detect(url)

        assertThat(platform).isEqualTo(Platform.DOUYIN)
    }

    @Test
    fun `test platform detection for TikTok`() {
        val url = "https://www.tiktok.com/@user/video/1234567890"
        val platform = Platform.detect(url)

        assertThat(platform).isEqualTo(Platform.TIKTOK)
    }

    @Test
    fun `test platform detection for Xiaohongshu`() {
        val url = "https://www.xiaohongshu.com/discovery/item/64a1b2c3d4e5f6789"
        val platform = Platform.detect(url)

        assertThat(platform).isEqualTo(Platform.XIAOHONGSHU)
    }

    @Test
    fun `test platform detection for unknown URL`() {
        val url = "https://example.com/video/123"
        val platform = Platform.detect(url)

        assertThat(platform).isEqualTo(Platform.UNKNOWN)
    }

    // ========================================
    // 测试 3: 数据映射逻辑 - 抖音
    // ========================================

    @Test
    fun `test Douyin video mapping with complete data`() {
        // 创建模拟的抖音视频数据
        val mockData = DouyinVideoData(
            awemeDetail = DouyinAwemeDetail(
                awemeId = "7123456789",
                desc = "测试视频标题",
                createTime = 1234567890L,
                author = DouyinAuthor(
                    uid = "user123",
                    nickname = "测试用户",
                    avatarThumb = DouyinUrlContainer(
                        urlList = listOf("https://example.com/avatar.jpg")
                    )
                ),
                statistics = DouyinStatistics(
                    diggCount = 1000L,
                    commentCount = 50L,
                    shareCount = 20L,
                    collectCount = 30L,
                    playCount = 10000L
                ),
                video = DouyinVideo(
                    playAddr = DouyinUrlContainer(
                        urlList = listOf("https://example.com/video.mp4")
                    ),
                    cover = DouyinUrlContainer(
                        urlList = listOf("https://example.com/cover.jpg")
                    ),
                    duration = 15000,
                    width = 1080,
                    height = 1920
                ),
                images = null,
                shareUrl = "https://v.douyin.com/test/",
                music = null
            )
        )

        // 执行映射
        val result = MediaMapper.mapDouyin(mockData)

        // 验证结果
        assertThat(result).isInstanceOf(ParsedMedia.Video::class.java)
        val video = result as ParsedMedia.Video

        assertThat(video.id).isEqualTo("7123456789")
        assertThat(video.title).isEqualTo("测试视频标题")
        assertThat(video.authorName).isEqualTo("测试用户")
        assertThat(video.videoUrl).contains("video.mp4")
        assertThat(video.duration).isEqualTo(15) // 转换为秒
        assertThat(video.stats.likeCount).isEqualTo(1000L)
    }

    @Test
    fun `test Douyin image note mapping`() {
        // 创建模拟的抖音图文数据
        val mockData = DouyinVideoData(
            awemeDetail = DouyinAwemeDetail(
                awemeId = "7123456789",
                desc = "测试图文",
                createTime = 1234567890L,
                author = DouyinAuthor(
                    uid = "user123",
                    nickname = "测试用户",
                    avatarThumb = DouyinUrlContainer(
                        urlList = listOf("https://example.com/avatar.jpg")
                    )
                ),
                statistics = DouyinStatistics(
                    diggCount = 1000L,
                    commentCount = 50L,
                    shareCount = 20L,
                    collectCount = 30L,
                    playCount = 10000L
                ),
                video = null,
                images = listOf(
                    DouyinImage(
                        urlList = listOf("https://example.com/img1.jpg"),
                        width = 1080,
                        height = 1440
                    ),
                    DouyinImage(
                        urlList = listOf("https://example.com/img2.jpg"),
                        width = 1080,
                        height = 1440
                    )
                ),
                shareUrl = "https://v.douyin.com/test/"
            )
        )

        // 执行映射
        val result = MediaMapper.mapDouyin(mockData)

        // 验证结果
        assertThat(result).isInstanceOf(ParsedMedia.ImageNote::class.java)
        val imageNote = result as ParsedMedia.ImageNote

        assertThat(imageNote.imageUrls).hasSize(2)
        assertThat(imageNote.imageSizes).hasSize(2)
    }

    // ========================================
    // 测试 4: 数据映射逻辑 - 快手
    // ========================================

    @Test
    fun `test Kuaishou video mapping`() {
        val mockData = KuaishouVideoData(
            photo = KuaishouPhoto(
                photoId = "ks123456",
                caption = "快手测试视频",
                timestamp = 1234567890L,
                userInfo = KuaishouUserInfo(
                    userId = "user123",
                    userName = "快手用户",
                    headUrl = "https://example.com/avatar.jpg"
                ),
                viewCount = 10000L,
                likeCount = 1000L,
                commentCount = 50L,
                shareCount = 20L,
                mainMvUrls = listOf(
                    KuaishouVideoUrl(url = "https://example.com/video.mp4")
                ),
                coverUrls = listOf(
                    KuaishouImageUrl(url = "https://example.com/cover.jpg")
                ),
                duration = 15000,
                width = 1080,
                height = 1920
            )
        )

        val result = MediaMapper.mapKuaishou(mockData)

        assertThat(result).isInstanceOf(ParsedMedia.Video::class.java)
        val video = result as ParsedMedia.Video

        assertThat(video.id).isEqualTo("ks123456")
        assertThat(video.title).isEqualTo("快手测试视频")
        assertThat(video.authorName).isEqualTo("快手用户")
        assertThat(video.stats.playCount).isEqualTo(10000L)
    }

    // ========================================
    // 测试 5: 错误处理逻辑
    // ========================================

    @Test(expected = IllegalStateException::class)
    fun `test Douyin mapping fails when both video and images are null`() {
        val mockData = DouyinVideoData(
            awemeDetail = DouyinAwemeDetail(
                awemeId = "7123456789",
                desc = "测试",
                createTime = 1234567890L,
                author = DouyinAuthor(
                    uid = "user123",
                    nickname = "测试用户"
                ),
                statistics = DouyinStatistics(),
                video = null,
                images = null
            )
        )

        // 应该抛出 IllegalStateException
        MediaMapper.mapDouyin(mockData)
    }

    @Test(expected = IllegalStateException::class)
    fun `test Kuaishou mapping fails when video URL is null`() {
        val mockData = KuaishouVideoData(
            photo = KuaishouPhoto(
                photoId = "ks123456",
                caption = "测试",
                timestamp = 1234567890L,
                userInfo = KuaishouUserInfo(
                    userId = "user123",
                    userName = "用户"
                ),
                mainMvUrls = null, // 缺少视频URL
                duration = 15000,
                width = 1080,
                height = 1920
            )
        )

        // 应该抛出 IllegalStateException
        MediaMapper.mapKuaishou(mockData)
    }

    // ========================================
    // 测试 6: 空安全性
    // ========================================

    @Test
    fun `test Douyin mapping handles null optional fields`() {
        val mockData = DouyinVideoData(
            awemeDetail = DouyinAwemeDetail(
                awemeId = "7123456789",
                desc = null, // null 描述
                createTime = 1234567890L,
                author = DouyinAuthor(
                    uid = "user123",
                    nickname = "测试用户",
                    avatarThumb = null // null 头像
                ),
                statistics = DouyinStatistics(),
                video = DouyinVideo(
                    playAddr = DouyinUrlContainer(
                        urlList = listOf("https://example.com/video.mp4")
                    ),
                    duration = 15000,
                    width = 1080,
                    height = 1920
                ),
                images = null,
                shareUrl = null, // null 分享链接
                music = null // null 音乐
            )
        )

        // 应该不会崩溃
        val result = MediaMapper.mapDouyin(mockData)

        assertThat(result).isNotNull()
        val video = result as ParsedMedia.Video
        assertThat(video.title).isEqualTo("抖音视频") // 默认标题
        assertThat(video.authorAvatar).isEmpty() // 默认空字符串
    }

    // ========================================
    // 测试 7: 类型转换
    // ========================================

    @Test
    fun `test duration conversion from milliseconds to seconds`() {
        val mockData = DouyinVideoData(
            awemeDetail = DouyinAwemeDetail(
                awemeId = "7123456789",
                desc = "测试",
                createTime = 1234567890L,
                author = DouyinAuthor(
                    uid = "user123",
                    nickname = "测试用户"
                ),
                statistics = DouyinStatistics(),
                video = DouyinVideo(
                    playAddr = DouyinUrlContainer(
                        urlList = listOf("https://example.com/video.mp4")
                    ),
                    duration = 30000, // 30秒 = 30000毫秒
                    width = 1080,
                    height = 1920
                )
            )
        )

        val result = MediaMapper.mapDouyin(mockData)
        val video = result as ParsedMedia.Video

        assertThat(video.duration).isEqualTo(30) // 应该转换为30秒
    }
}
