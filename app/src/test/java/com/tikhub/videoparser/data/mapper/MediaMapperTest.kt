package com.tikhub.videoparser.data.mapper

import com.google.gson.Gson
import com.tikhub.videoparser.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * MediaMapper 单元测试
 *
 * 测试目标：
 * 1. 验证各平台数据转换的正确性
 * 2. 验证内容类型判断（Video vs ImageNote）
 * 3. 验证空值和异常情况的处理
 */
class MediaMapperTest {

    private lateinit var gson: Gson

    @Before
    fun setup() {
        gson = Gson()
    }

    // ========================================
    // 抖音测试
    // ========================================

    @Test
    fun `抖音视频转换成功`() {
        // Given: 构造抖音视频数据
        val douyinData = DouyinVideoData(
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
                    diggCount = 1000,
                    commentCount = 50,
                    shareCount = 20,
                    playCount = 50000
                ),
                video = DouyinVideo(
                    playAddr = DouyinUrlContainer(
                        urlList = listOf("https://example.com/video.mp4")
                    ),
                    cover = DouyinUrlContainer(
                        urlList = listOf("https://example.com/cover.jpg")
                    ),
                    duration = 15000, // 15秒
                    width = 1080,
                    height = 1920
                ),
                images = null,
                shareUrl = "https://v.douyin.com/test"
            )
        )

        // When: 转换数据
        val result = MediaMapper.mapDouyin(douyinData)

        // Then: 验证结果
        assertTrue("应该是 Video 类型", result is ParsedMedia.Video)
        val video = result as ParsedMedia.Video

        assertEquals("7123456789", video.id)
        assertEquals("douyin", video.platform)
        assertEquals("测试用户", video.authorName)
        assertEquals("测试视频标题", video.title)
        assertEquals(15, video.duration) // 毫秒转秒
        assertEquals(1080, video.width)
        assertEquals(1920, video.height)
        assertEquals(1000L, video.stats.likeCount)
        assertEquals(50L, video.stats.commentCount)
    }

    @Test
    fun `抖音图文转换成功`() {
        // Given: 构造抖音图文数据
        val douyinData = DouyinVideoData(
            awemeDetail = DouyinAwemeDetail(
                awemeId = "7987654321",
                desc = "测试图文笔记",
                createTime = 1234567890L,
                author = DouyinAuthor(
                    uid = "user456",
                    nickname = "图文作者",
                    avatarThumb = DouyinUrlContainer(
                        urlList = listOf("https://example.com/avatar2.jpg")
                    )
                ),
                statistics = DouyinStatistics(
                    diggCount = 500,
                    commentCount = 30
                ),
                video = null,
                images = listOf(
                    DouyinImage(
                        urlList = listOf("https://example.com/img1.jpg"),
                        width = 1080,
                        height = 1080
                    ),
                    DouyinImage(
                        urlList = listOf("https://example.com/img2.jpg"),
                        width = 1080,
                        height = 1080
                    )
                ),
                shareUrl = "https://v.douyin.com/test2"
            )
        )

        // When: 转换数据
        val result = MediaMapper.mapDouyin(douyinData)

        // Then: 验证结果
        assertTrue("应该是 ImageNote 类型", result is ParsedMedia.ImageNote)
        val imageNote = result as ParsedMedia.ImageNote

        assertEquals("7987654321", imageNote.id)
        assertEquals("douyin", imageNote.platform)
        assertEquals("图文作者", imageNote.authorName)
        assertEquals(2, imageNote.imageUrls.size)
        assertEquals("https://example.com/img1.jpg", imageNote.imageUrls[0])
    }

    @Test
    fun `抖音去水印逻辑测试`() {
        // Given: 包含水印的视频URL
        val douyinData = DouyinVideoData(
            awemeDetail = DouyinAwemeDetail(
                awemeId = "test",
                desc = "test",
                createTime = 0,
                author = DouyinAuthor(
                    uid = "test",
                    nickname = "test",
                    avatarThumb = null
                ),
                statistics = DouyinStatistics(),
                video = DouyinVideo(
                    playAddr = DouyinUrlContainer(
                        urlList = listOf("https://example.com/playwm/video.mp4")
                    ),
                    cover = DouyinUrlContainer(
                        urlList = listOf("https://example.com/cover.jpg")
                    ),
                    duration = 10000
                ),
                images = null,
                shareUrl = null
            )
        )

        // When: 转换数据
        val result = MediaMapper.mapDouyin(douyinData) as ParsedMedia.Video

        // Then: 验证去水印
        assertEquals(
            "应该将 playwm 替换为 play",
            "https://example.com/play/video.mp4",
            result.videoUrl
        )
    }

    // ========================================
    // 小红书测试
    // ========================================

    @Test
    fun `小红书图文转换成功`() {
        // Given: 构造小红书图文数据
        val xhsData = XiaohongshuNoteData(
            data = listOf(
                XiaohongshuDataItem(
                    noteList = listOf(
                        XiaohongshuNote(
                            noteId = "xhs123",
                            type = "normal",
                            title = "小红书笔记标题",
                            desc = "笔记描述",
                            time = 1234567890L,
                            user = XiaohongshuUser(
                                userId = "user123",
                                nickname = "小红书用户",
                                avatar = "https://example.com/avatar.jpg"
                            ),
                            likedCount = "1000",
                            collectedCount = "500",
                            shareCount = "100",
                            imagesList = listOf(
                                XiaohongshuImage(
                                    url = "https://example.com/img1.jpg",
                                    width = 1080,
                                    height = 1440
                                ),
                                XiaohongshuImage(
                                    url = "https://example.com/img2.jpg",
                                    width = 1080,
                                    height = 1440
                                )
                            ),
                            video = null,
                            shareUrl = "http://xhslink.com/test"
                        )
                    )
                )
            )
        )

        // When: 转换数据
        val result = MediaMapper.mapXiaohongshu(xhsData)

        // Then: 验证结果
        assertTrue("应该是 ImageNote 类型", result is ParsedMedia.ImageNote)
        val imageNote = result as ParsedMedia.ImageNote

        assertEquals("xhs123", imageNote.id)
        assertEquals("xiaohongshu", imageNote.platform)
        assertEquals("小红书用户", imageNote.authorName)
        assertEquals("小红书笔记标题", imageNote.title)
        assertEquals(2, imageNote.imageUrls.size)
        assertEquals(1000L, imageNote.stats.likeCount)
        assertEquals(500L, imageNote.stats.collectCount)
    }

    // ========================================
    // 微博测试
    // ========================================

    @Test
    fun `微博视频转换成功`() {
        // Given: 构造微博视频JSON
        val jsonString = """
        {
            "status": {
                "id": "4567890123",
                "text_raw": "这是一条微博视频",
                "user": {
                    "screen_name": "微博用户",
                    "avatar_large": "https://example.com/avatar.jpg"
                },
                "attitudes_count": 1000,
                "comments_count": 50,
                "reposts_count": 20,
                "page_info": {
                    "type": "video",
                    "page_pic": {
                        "url": "https://example.com/cover.jpg"
                    },
                    "media_info": {
                        "stream_url": "https://example.com/video.mp4",
                        "duration": 60
                    }
                }
            }
        }
        """.trimIndent()

        val jsonObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)

        // When: 转换数据
        val result = MediaMapper.mapWeibo(jsonObject)

        // Then: 验证结果
        assertTrue("应该是 Video 类型", result is ParsedMedia.Video)
        val video = result as ParsedMedia.Video

        assertEquals("4567890123", video.id)
        assertEquals("weibo", video.platform)
        assertEquals("微博用户", video.authorName)
        assertEquals("这是一条微博视频", video.title)
        assertEquals(1000L, video.stats.likeCount)
        assertEquals(50L, video.stats.commentCount)
    }

    @Test
    fun `微博图文转换成功`() {
        // Given: 构造微博图文JSON（九宫格）
        val jsonString = """
        {
            "status": {
                "id": "9876543210",
                "text_raw": "这是一条微博图文，包含九宫格图片",
                "user": {
                    "screen_name": "图文博主",
                    "avatar_large": "https://example.com/avatar2.jpg"
                },
                "attitudes_count": 5000,
                "comments_count": 200,
                "reposts_count": 100,
                "pics": [
                    {
                        "large": {
                            "url": "https://example.com/pic1.jpg"
                        }
                    },
                    {
                        "large": {
                            "url": "https://example.com/pic2.jpg"
                        }
                    },
                    {
                        "large": {
                            "url": "https://example.com/pic3.jpg"
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val jsonObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)

        // When: 转换数据
        val result = MediaMapper.mapWeibo(jsonObject)

        // Then: 验证结果
        assertTrue("应该是 ImageNote 类型", result is ParsedMedia.ImageNote)
        val imageNote = result as ParsedMedia.ImageNote

        assertEquals("9876543210", imageNote.id)
        assertEquals("weibo", imageNote.platform)
        assertEquals("图文博主", imageNote.authorName)
        assertEquals(3, imageNote.imageUrls.size)
        assertEquals(5000L, imageNote.stats.likeCount)
    }

    // ========================================
    // 异常处理测试
    // ========================================

    @Test(expected = IllegalStateException::class)
    fun `抖音数据既没有图片也没有视频应抛出异常`() {
        val douyinData = DouyinVideoData(
            awemeDetail = DouyinAwemeDetail(
                awemeId = "test",
                desc = "test",
                createTime = 0,
                author = DouyinAuthor(
                    uid = "test",
                    nickname = "test",
                    avatarThumb = null
                ),
                statistics = DouyinStatistics(),
                video = null,
                images = null,
                shareUrl = null
            )
        )

        // Should throw IllegalStateException
        MediaMapper.mapDouyin(douyinData)
    }

    @Test(expected = IllegalStateException::class)
    fun `小红书数据结构异常应抛出异常`() {
        val xhsData = XiaohongshuNoteData(
            data = emptyList() // 空数据
        )

        // Should throw IllegalStateException
        MediaMapper.mapXiaohongshu(xhsData)
    }

    // ========================================
    // StatsInfo 格式化测试
    // ========================================

    @Test
    fun `统计信息格式化测试`() {
        val stats = StatsInfo(
            likeCount = 12345,
            commentCount = 678,
            playCount = 987654
        )

        val formatted = stats.getFormattedStats()

        assertTrue("应包含点赞数", formatted.contains("❤"))
        assertTrue("应包含评论数", formatted.contains("💬"))
        assertTrue("应包含播放数", formatted.contains("▶"))
    }

    @Test
    fun `大数字格式化测试`() {
        val stats = StatsInfo(
            likeCount = 123456,  // 12.3w
            commentCount = 5678,  // 5678
            playCount = 9876543   // 987.7w
        )

        val formatted = stats.getFormattedStats()

        assertTrue("应该包含'w'单位", formatted.contains("w"))
    }

    // ========================================
    // ParsedMedia 扩展函数测试
    // ========================================

    @Test
    fun `Video 时长格式化测试`() {
        val video = ParsedMedia.Video(
            id = "test",
            platform = "test",
            authorName = "test",
            authorAvatar = "",
            title = "test",
            coverUrl = "",
            stats = StatsInfo(),
            videoUrl = "",
            duration = 125 // 2分5秒
        )

        assertEquals("02:05", video.getFormattedDuration())
    }

    @Test
    fun `Video 文件大小格式化测试`() {
        val video = ParsedMedia.Video(
            id = "test",
            platform = "test",
            authorName = "test",
            authorAvatar = "",
            title = "test",
            coverUrl = "",
            stats = StatsInfo(),
            videoUrl = "",
            fileSize = 1024 * 1024 * 5 // 5MB
        )

        val size = video.getReadableFileSize()
        assertTrue("应该包含 MB", size.contains("MB"))
    }

    @Test
    fun `ImageNote 图片数量描述测试`() {
        val singleImage = ParsedMedia.ImageNote(
            id = "test",
            platform = "test",
            authorName = "test",
            authorAvatar = "",
            title = "test",
            coverUrl = "",
            stats = StatsInfo(),
            imageUrls = listOf("url1")
        )

        assertEquals("单图", singleImage.getImageCountDescription())

        val nineImages = ParsedMedia.ImageNote(
            id = "test",
            platform = "test",
            authorName = "test",
            authorAvatar = "",
            title = "test",
            coverUrl = "",
            stats = StatsInfo(),
            imageUrls = List(9) { "url$it" }
        )

        assertEquals("九宫格 9图", nineImages.getImageCountDescription())
    }
}
