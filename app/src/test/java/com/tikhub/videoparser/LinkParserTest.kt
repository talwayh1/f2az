package com.tikhub.videoparser

import com.tikhub.videoparser.utils.Platform
import com.tikhub.videoparser.utils.PlatformDetector
import com.tikhub.videoparser.utils.UrlExtractor

/**
 * 测试脚本：验证 URL 提取和平台识别
 * 使用用户提供的真实测试链接
 */
object LinkParserTest {

    // 测试数据：用户提供的真实链接
    private val testCases = listOf(
        TestCase(
            name = "抖音短链",
            input = "2.82 复制打开抖音，看看【cc..的作品】科技不是高高在上，而是服务于人民 # 大疆云台  https://v.douyin.com/8E5pI4WTIHA/ 12/10",
            expectedUrl = "https://v.douyin.com/8E5pI4WTIHA/",
            expectedPlatform = Platform.DOUYIN
        ),
        TestCase(
            name = "快手短链",
            input = "Jic:/ f@b.nQ ；https://v.kuaishou.com/J8J2I2lL 儿子又长身高，妈妈拒绝面对现实",
            expectedUrl = "https://v.kuaishou.com/J8J2I2lL",
            expectedPlatform = Platform.KUAISHOU
        ),
        TestCase(
            name = "小红书短链",
            input = "电信广东卡 线上线下的套餐为什么差距这么大？ http://xhslink.com/o/PoVSqnGsRh 复制后打开【小红书】查看笔记！",
            expectedUrl = "http://xhslink.com/o/PoVSqnGsRh",
            expectedPlatform = Platform.XIAOHONGSHU
        )
    )

    data class TestCase(
        val name: String,
        val input: String,
        val expectedUrl: String,
        val expectedPlatform: Platform
    )

    @JvmStatic
    fun main(args: Array<String>) {
        println("=" * 80)
        println("TikHub Video Parser - 链接解析测试")
        println("=" * 80)
        println()

        var passedTests = 0
        var totalTests = testCases.size * 2 // 每个测试有 2 个验证点

        testCases.forEach { testCase ->
            println("【测试】${testCase.name}")
            println("输入: ${testCase.input}")
            println()

            // 测试 1: URL 提取
            print("  ✓ URL 提取... ")
            val extractedUrls = UrlExtractor.extractUrls(testCase.input)
            if (extractedUrls.isNotEmpty() && extractedUrls.first() == testCase.expectedUrl) {
                println("✅ 通过")
                println("    提取到: ${extractedUrls.first()}")
                passedTests++
            } else {
                println("❌ 失败")
                println("    期望: ${testCase.expectedUrl}")
                println("    实际: ${extractedUrls.firstOrNull() ?: "未提取到 URL"}")
            }

            // 测试 2: 平台识别
            print("  ✓ 平台识别... ")
            val detectedPlatform = if (extractedUrls.isNotEmpty()) {
                Platform.detect(extractedUrls.first())
            } else {
                Platform.UNKNOWN
            }

            if (detectedPlatform == testCase.expectedPlatform) {
                println("✅ 通过")
                println("    识别为: ${detectedPlatform.displayName}")
                passedTests++
            } else {
                println("❌ 失败")
                println("    期望: ${testCase.expectedPlatform.displayName}")
                println("    实际: ${detectedPlatform.displayName}")
            }

            println()
        }

        // 输出总结
        println("=" * 80)
        println("测试总结")
        println("=" * 80)
        println("通过: $passedTests / $totalTests")
        println("成功率: ${(passedTests * 100 / totalTests)}%")
        println()

        if (passedTests == totalTests) {
            println("🎉 所有测试通过！")
            println("✅ URL 提取功能正常")
            println("✅ 平台识别功能正常")
        } else {
            println("⚠️  部分测试失败，请检查实现")
        }

        println("=" * 80)

        // 退出码
        System.exit(if (passedTests == totalTests) 0 else 1)
    }

    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}
