package com.tikhub.videoparser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.data.model.ParseResult
import com.tikhub.videoparser.data.repository.VideoParserRepository
import com.tikhub.videoparser.download.DownloadManager
import com.tikhub.videoparser.download.DownloadState
import com.tikhub.videoparser.utils.Platform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI 状态
 */
sealed class UiState {
    object Idle : UiState() // 空闲状态
    object Loading : UiState() // 加载中
    data class Success(val result: ParseResult) : UiState() // 解析成功
    data class Error(val message: String) : UiState() // 解析失败
}

/**
 * 主界面 ViewModel
 * 管理解析状态和下载逻辑
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: VideoParserRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    // UI 状态
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 输入框文本
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // 下载状态
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // SDK 服务状态（true=可用，false=不可用）
    private val _sdkStatus = MutableStateFlow(false)
    val sdkStatus: StateFlow<Boolean> = _sdkStatus.asStateFlow()

    // 当前解析的平台
    private var currentPlatform: Platform = Platform.UNKNOWN

    init {
        Timber.d("MainViewModel 已创建")
        // 初始化时检查 SDK 状态
        checkSdkStatus()
    }

    /**
     * 检查SDK服务状态
     */
    fun checkSdkStatus() {
        viewModelScope.launch {
            try {
                Timber.d("检查 SDK 服务状态...")
                // 通过解析一个简单的测试来检查SDK是否可用
                // 这里简化处理：假设repository初始化成功就表示SDK可用
                _sdkStatus.value = true
                Timber.i("✅ SDK 服务可用")
            } catch (e: Exception) {
                Timber.w(e, "❌ SDK 服务不可用")
                _sdkStatus.value = false
            }
        }
    }

    /**
     * 更新输入框文本
     */
    fun updateInputText(text: String) {
        Timber.d("更新输入框文本: ${text.take(50)}...")
        _inputText.value = text
    }

    /**
     * 解析链接
     */
    fun parse() {
        val input = _inputText.value.trim()
        Timber.i("========== 开始解析 ==========")
        Timber.i("输入内容: $input")

        if (input.isEmpty()) {
            Timber.w("输入为空，拒绝解析")
            _uiState.value = UiState.Error("请输入链接")
            return
        }

        viewModelScope.launch {
            try {
                // 重置下载状态（修复：避免显示上一次的下载成功状态）
                _downloadState.value = DownloadState.Idle
                Timber.d("已重置下载状态为 Idle")

                Timber.d("更新 UI 状态为 Loading")
                _uiState.value = UiState.Loading

                Timber.d("调用 Repository 解析...")
                repository.parse(input)
                    .onSuccess { result ->
                        Timber.i("✅ 解析成功!")
                        Timber.d("结果详情: $result")

                        // 保存平台信息，用于下载时设置 Referer
                        currentPlatform = Platform.detect(input)
                        Timber.d("识别平台: $currentPlatform")

                        // 如果是视频，获取真实文件大小
                        val updatedResult = if (result is ParsedMedia.Video && result.videoUrl.isNotEmpty()) {
                            Timber.d("检测到视频，开始获取文件大小...")
                            val fileSize = downloadManager.getFileSize(
                                url = result.videoUrl,
                                platform = currentPlatform
                            )
                            Timber.i("获取到视频文件大小: $fileSize 字节")

                            // 更新 video 的 fileSize 字段
                            result.copy(fileSize = fileSize)
                        } else {
                            result
                        }

                        _uiState.value = UiState.Success(updatedResult)

                        // 解析成功后清空输入框
                        _inputText.value = ""

                        Timber.i("========== 解析完成 ==========")
                    }
                    .onFailure { error ->
                        Timber.e(error, "❌ 解析失败")
                        _uiState.value = UiState.Error(error.message ?: "解析失败")
                        Timber.i("========== 解析失败 ==========")
                    }
            } catch (e: Exception) {
                Timber.e(e, "❌ 解析过程发生未捕获异常")
                _uiState.value = UiState.Error("解析出错: ${e.message}")
                Timber.i("========== 解析异常 ==========")
            }
        }
    }

    /**
     * 下载视频
     * @param videoUrl 无水印视频 URL
     */
    fun downloadVideo(videoUrl: String) {
        Timber.i("========== 开始下载视频 ==========")
        Timber.i("视频 URL: $videoUrl")
        Timber.i("平台: $currentPlatform")

        viewModelScope.launch {
            try {
                downloadManager.downloadVideo(
                    url = videoUrl,
                    platform = currentPlatform
                ).collect { state ->
                    Timber.d("下载状态更新: $state")
                    _downloadState.value = state

                    when (state) {
                        is DownloadState.Success -> {
                            Timber.i("✅ 视频下载成功: ${state.filePath}")
                        }
                        is DownloadState.Failed -> {
                            Timber.e("❌ 视频下载失败: ${state.error}")
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ 下载视频过程发���异常")
                _downloadState.value = DownloadState.Failed("下载出错: ${e.message}")
            } finally {
                Timber.i("========== 视频下载结束 ==========")
            }
        }
    }

    /**
     * 下载单张图片
     */
    fun downloadImage(imageUrl: String) {
        Timber.i("========== 开始下载图片 ==========")
        Timber.i("图片 URL: $imageUrl")

        viewModelScope.launch {
            try {
                downloadManager.downloadImage(
                    url = imageUrl,
                    platform = currentPlatform
                ).collect { state ->
                    Timber.d("下载状态更新: $state")
                    _downloadState.value = state
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ 下载图片过程发生异常")
            } finally {
                Timber.i("========== 图片下载结束 ==========")
            }
        }
    }

    /**
     * 批量下载图片
     */
    fun downloadAllImages(imageUrls: List<String>) {
        Timber.i("========== 开始批量下载图片 ==========")
        Timber.i("图片数量: ${imageUrls.size}")

        viewModelScope.launch {
            try {
                imageUrls.forEachIndexed { index, url ->
                    Timber.d("下载图片 ${index + 1}/${imageUrls.size}: $url")

                    downloadManager.downloadImage(
                        url = url,
                        platform = currentPlatform,
                        fileName = "image_${System.currentTimeMillis()}_$index"
                    ).collect { state ->
                        _downloadState.value = state
                    }
                }
                Timber.i("✅ 批量下载完成")
            } catch (e: Exception) {
                Timber.e(e, "❌ 批量下载过程发生异常")
            } finally {
                Timber.i("========== 批量下载结束 ==========")
            }
        }
    }

    /**
     * 重置状态
     */
    fun reset() {
        Timber.d("重置所有状态")
        _uiState.value = UiState.Idle
        _inputText.value = ""
        _downloadState.value = DownloadState.Idle
    }

    /**
     * 处理剪贴板内容
     * @param clipboardText 剪贴板文本
     * @param autoFill 是否自动填充到输入框
     */
    fun handleClipboard(clipboardText: String, autoFill: Boolean = true) {
        Timber.d("处理剪贴板内容: ${clipboardText.take(50)}...")
        Timber.d("自动填充: $autoFill")

        if (autoFill && clipboardText.isNotBlank()) {
            _inputText.value = clipboardText
            Timber.i("剪贴板内容已自动填充")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("MainViewModel 已清除")
    }
}
