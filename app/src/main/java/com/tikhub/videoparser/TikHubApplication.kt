package com.tikhub.videoparser

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.tikhub.videoparser.utils.CombinedLoggerTree
import com.tikhub.videoparser.utils.LogManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * TikHub 应用入口
 * 使用 Hilt 进行依赖注入
 * 实现 Configuration.Provider 以使用 Hilt 的 WorkerFactory
 */
@HiltAndroidApp
class TikHubApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var logManager: LogManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // 初始化 Logger 库（必须在 Timber 之前）
        initLogger()

        // 初始化 Timber 日志系统
        initTimber()

        // 设置全局异常捕获器
        setupGlobalExceptionHandler()

        Timber.i("========================================")
        Timber.i("🚀 TikHub VideoParser 应用启动")
        Timber.i("📦 开发版本: ${BuildConfig.VERSION_NAME}")
        Timber.i("📱 版本代码: ${BuildConfig.VERSION_CODE}")
        Timber.i("🔧 构建类型: ${BuildConfig.BUILD_TYPE}")
        Timber.i("========================================")
        Timber.i("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
        Timber.i("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Timber.i("日志级别: 全量详细日志（开发模式）")
        Timber.i("========================================")
    }

    /**
     * 初始化 Logger 库
     * 配置漂亮的格式化日志输出，包含线程信息、方法调用栈等
     */
    private fun initLogger() {
        val formatStrategy: FormatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(true)           // 显示线程信息
            .methodCount(5)                  // 显示方法调用栈（5层）
            .methodOffset(0)                 // 方法偏移
            .tag("TikHub")                   // 全局 Tag
            .build()

        Logger.addLogAdapter(object : AndroidLogAdapter(formatStrategy) {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                // Debug 模式下输出所有级别的日志
                return BuildConfig.DEBUG
            }
        })

        Logger.i("Logger 已初始化 - 漂亮的格式化日志已启用")
    }

    /**
     * 初始化 Timber 日志系统
     * - Debug 模式：使用 Logger 输出漂亮的格式化日志 + 收集到 LogManager
     * - Release 模式：仅保存到文件
     */
    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            // Debug 模式：使用 CombinedLoggerTree，同时输出到 Logger 和 LogManager
            Timber.plant(CombinedLoggerTree(logManager))
            Timber.i("Timber 已初始化 (Debug 模式 - Logger + LogManager 组合)")
        } else {
            // Release 模式：简化日志树
            Timber.plant(object : Timber.DebugTree() {
                private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // Release 模式下只记录 WARN 和 ERROR 级别
                    if (priority >= Log.WARN) {
                        val timestamp = dateFormat.format(Date())
                        val enhancedTag = "[$timestamp] $tag"
                        super.log(priority, enhancedTag, message, t)
                    }
                }
            })
            Timber.i("Timber 已初始化 (Release 模式)")
        }

        // 同时添加文件日志树（用于保存日志到文件）
        try {
            Timber.plant(FileLoggingTree(this))
            Timber.i("文件日志树已启用")
        } catch (e: Exception) {
            Timber.e(e, "文件日志树初始化失败")
        }
    }

    /**
     * 设置全局异常捕获器
     * 捕获所有未处理的异常，记录日志并保存到文件
     */
    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Timber.e(throwable, "【致命崩溃】线程: ${thread.name}")

                // 保存崩溃日志到文件
                saveCrashLog(throwable)

                // 调用原始异常处理器
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Timber.i("全局异常捕获器已设置")
    }

    /**
     * 保存崩溃日志到文件
     */
    private fun saveCrashLog(throwable: Throwable) {
        try {
            val logDir = File(getExternalFilesDir(null), "logs/crash")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val logFile = File(logDir, "crash_$timestamp.log")

            FileWriter(logFile).use { writer ->
                writer.write("========================================\n")
                writer.write("TikHub 崩溃日志\n")
                writer.write("时间: $timestamp\n")
                writer.write("版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
                writer.write("设备: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                writer.write("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                writer.write("========================================\n\n")
                writer.write("异常信息:\n")
                writer.write(Log.getStackTraceString(throwable))
            }

            Timber.i("崩溃日志已保存: ${logFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 文件日志树
     * 将日志同时写入文件，便于排查问题
     */
    private class FileLoggingTree(private val application: Application) : Timber.Tree() {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            try {
                val logDir = File(application.getExternalFilesDir(null), "logs")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }

                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val logFile = File(logDir, "tikhub_$date.log")

                FileWriter(logFile, true).use { writer ->
                    val timestamp = dateFormat.format(Date())
                    val level = when (priority) {
                        Log.VERBOSE -> "V"
                        Log.DEBUG -> "D"
                        Log.INFO -> "I"
                        Log.WARN -> "W"
                        Log.ERROR -> "E"
                        Log.ASSERT -> "A"
                        else -> "?"
                    }

                    writer.write("$timestamp $level/$tag: $message\n")

                    if (t != null) {
                        writer.write(Log.getStackTraceString(t))
                        writer.write("\n")
                    }
                }

                // 清理旧日志（保留最近 7 天）
                cleanOldLogs(logDir)

            } catch (e: Exception) {
                // 文件日志失败不影响应用运行
            }
        }

        /**
         * 清理 7 天前的日志文件
         */
        private fun cleanOldLogs(logDir: File) {
            try {
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                logDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < sevenDaysAgo) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // 清理失败不影响应用运行
            }
        }
    }

    /**
     * 配置 WorkManager 使用 Hilt 的 WorkerFactory
     * 这样 DownloadWorker 就能正确注入依赖了
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()
}
