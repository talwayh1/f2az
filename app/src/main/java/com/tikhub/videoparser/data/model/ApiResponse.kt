package com.tikhub.videoparser.data.model

import com.google.gson.annotations.SerializedName

/**
 * TikHub API 通用响应结构
 */
data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int = 0,

    @SerializedName("message")
    val message: String = "",

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("error")
    val error: String? = null
) {
    /**
     * 判断请求是否成功
     */
    fun isSuccess(): Boolean = code == 200 || code == 0

    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String {
        return when {
            !error.isNullOrBlank() -> error
            !message.isNullOrBlank() -> message
            else -> "未知错误"
        }
    }
}
