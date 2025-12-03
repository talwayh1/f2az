package com.tikhub.videoparser.utils

import android.util.Log
import com.orhanobut.logger.Logger
import timber.log.Timber

/**
 * 自定义 Timber Tree，将日志输出到 Logger 库
 * 这样可以获得漂亮的格式化日志输出，包含 JSON 格式化、线程信息等
 */
class LoggerTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 根据优先级选择对应的 Logger 方法
        when (priority) {
            Log.VERBOSE -> Logger.t(tag).v(message)
            Log.DEBUG -> Logger.t(tag).d(message)
            Log.INFO -> Logger.t(tag).i(message)
            Log.WARN -> {
                if (t != null) {
                    Logger.t(tag).w(message, t)
                } else {
                    Logger.t(tag).w(message)
                }
            }
            Log.ERROR -> {
                if (t != null) {
                    Logger.t(tag).e(t, message)
                } else {
                    Logger.t(tag).e(message)
                }
            }
            Log.ASSERT -> Logger.t(tag).wtf(message)
        }
    }
}

/**
 * 自定义 Timber Tree，同时输出到 Logger 和 LogManager
 * 用于在 Debug 模式下同时显示漂亮的日志和收集日志到内存
 */
class CombinedLoggerTree(private val logManager: LogManager?) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // 1. 输出到 Logger（格式化的漂亮日志）
        when (priority) {
            Log.VERBOSE -> Logger.t(tag).v(message)
            Log.DEBUG -> Logger.t(tag).d(message)
            Log.INFO -> Logger.t(tag).i(message)
            Log.WARN -> {
                if (t != null) {
                    Logger.t(tag).w(message, t)
                } else {
                    Logger.t(tag).w(message)
                }
            }
            Log.ERROR -> {
                if (t != null) {
                    Logger.t(tag).e(t, message)
                } else {
                    Logger.t(tag).e(message)
                }
            }
            Log.ASSERT -> Logger.t(tag).wtf(message)
        }

        // 2. 同时收集到 LogManager（用于应用内日志查看）
        logManager?.addLog(priority, tag, message)

        // 3. 如果有异常，也记录异常堆栈
        if (t != null) {
            val stackTrace = Log.getStackTraceString(t)
            logManager?.addLog(priority, tag, stackTrace)
        }
    }
}

/**
 * JSON 日志辅助工具
 * 用于方便地输出 JSON 格式的日志
 */
object JsonLogger {

    /**
     * 输出 JSON 格式的日志
     * Logger 会自动格式化 JSON，使其更易读
     */
    fun json(tag: String? = null, jsonString: String) {
        if (tag != null) {
            Logger.t(tag).json(jsonString)
        } else {
            Logger.json(jsonString)
        }
    }

    /**
     * 输出任意对象为 JSON
     */
    fun json(tag: String? = null, obj: Any) {
        try {
            val gson = com.google.gson.Gson()
            val jsonString = gson.toJson(obj)
            json(tag, jsonString)
        } catch (e: Exception) {
            Timber.e(e, "Failed to convert object to JSON")
        }
    }
}

/**
 * 线程信息日志辅助工具
 */
object ThreadLogger {

    /**
     * 输出当前线程信息
     */
    fun logCurrentThread(tag: String? = null, message: String = "") {
        val thread = Thread.currentThread()
        val threadInfo = """
            Thread Info:
            - Name: ${thread.name}
            - ID: ${thread.id}
            - Priority: ${thread.priority}
            - State: ${thread.state}
            - Is Daemon: ${thread.isDaemon}
            ${if (message.isNotEmpty()) "- Message: $message" else ""}
        """.trimIndent()

        if (tag != null) {
            Logger.t(tag).d(threadInfo)
        } else {
            Logger.d(threadInfo)
        }
    }
}
