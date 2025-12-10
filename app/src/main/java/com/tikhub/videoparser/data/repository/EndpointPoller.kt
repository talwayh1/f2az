package com.tikhub.videoparser.data.repository

import com.tikhub.videoparser.data.model.ApiResponse
import kotlinx.coroutines.withTimeout
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 通用接口轮询器
 *
 * 功能：
 * 1. 自动轮询多个备用接口
 * 2. 智能错误分类（区分可重试/不可重试错误）
 * 3. 超时控制（避免长时间等待）
 * 4. 统一日志记录
 *
 * 优势：
 * - 减少代码重复（从8个平台各100行 → 1个通用函数）
 * - 提升响应速度（快速失败，避免无效重试）
 * - 易于维护和扩展
 */
object EndpointPoller {

    /**
     * 轮询多个接口直到成功
     *
     * @param T API响应数据类型
     * @param R 映射后的结果类型
     * @param endpoints 接口列表（名称 + 调用函数）
     * @param mapper 数据映射函数
     * @param timeoutMs 单个接口超时时间（毫秒）
     * @return Result<R> 成功返回映射后的数据，失败返回最后一个错误
     */
    suspend fun <T, R> poll(
        endpoints: List<Pair<String, suspend () -> ApiResponse<T>>>,
        mapper: (T) -> R,
        timeoutMs: Long = 15000  // 默认15秒超时
    ): Result<R> {
        var lastError: Exception? = null
        val totalEndpoints = endpoints.size

        for ((index, pair) in endpoints.withIndex()) {
            val (name, fetch) = pair
            val attemptNumber = index + 1

            try {
                Timber.d("🔄 尝试 $name ($attemptNumber/$totalEndpoints)")

                // 添加超时控制
                val response = withTimeout(timeoutMs) {
                    fetch()
                }

                // 检查响应状态
                if (response.code == 200 && response.data != null) {
                    Timber.d("✅ $name 返回成功，开始数据映射")

                    // 尝试数据映射
                    return try {
                        val result = mapper(response.data)
                        Timber.i("✅ 接口调用成功: $name")
                        Result.success(result)
                    } catch (e: Exception) {
                        Timber.e(e, "❌ $name 数据映射失败")
                        lastError = Exception("数据映射失败: ${e.message}", e)

                        // 映射失败也尝试下一个接口
                        if (index == endpoints.lastIndex) {
                            return Result.failure(lastError)
                        }
                        continue
                    }
                } else {
                    // API返回非200状态码
                    val errorMsg = "code=${response.code}, message=${response.message}"
                    Timber.w("⚠️ $name 返回失败: $errorMsg")
                    lastError = Exception(response.message)
                }

            } catch (e: Exception) {
                Timber.e(e, "❌ $name 异常: ${e.javaClass.simpleName}")
                lastError = e

                // 判断是否应该继续重试
                if (!shouldRetry(e)) {
                    Timber.w("🚫 检测到不可重试错误，停止轮询")
                    return Result.failure(e)
                }
            }

            // 如果是最后一个接口也失败了
            if (index == endpoints.lastIndex) {
                Timber.e("💥 所有接口均失败 ($totalEndpoints/$totalEndpoints)")
                return Result.failure(lastError ?: Exception("所有接口均失败"))
            }
        }

        return Result.failure(lastError ?: Exception("所有接口均失败"))
    }

    /**
     * 判断错误是否应该重试
     *
     * 重试策略：
     * - 网络错误（超时、DNS失败等）→ 重试
     * - 服务器错误（5xx）→ 重试
     * - 客户端错误（4xx）→ 不重试（数据本身有问题）
     */
    private fun shouldRetry(error: Exception): Boolean {
        return when (error) {
            // HTTP错误
            is HttpException -> {
                when (error.code()) {
                    400 -> false  // 请求参数错误
                    401 -> false  // 未授权（API Key无效）
                    403 -> false  // 禁止访问
                    404 -> false  // 资源不存在
                    429 -> true   // 请求过多，可以重试
                    500 -> true   // 服务器内部错误
                    502 -> true   // 网关错误
                    503 -> true   // 服务不可用
                    504 -> true   // 网关超时
                    else -> false
                }
            }

            // 网络错误（应该重试）
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            is IOException -> true

            // 其他错误（默认不重试）
            else -> false
        }
    }
}
