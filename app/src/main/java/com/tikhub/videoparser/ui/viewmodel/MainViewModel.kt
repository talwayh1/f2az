package com.tikhub.videoparser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.data.model.ParseResultWrapper
import com.tikhub.videoparser.data.repository.VideoParserRepository
import com.tikhub.videoparser.download.DownloadState
import com.tikhub.videoparser.download.DownloadWorker
import com.tikhub.videoparser.download.WorkManagerDownloadManager
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
    data class Success(val result: ParseResultWrapper) : UiState() // 解析成功
    data class Error(val message: String) : UiState() // 解析失败
}

/**
 * 主界面 ViewModel
 * 管理解析状态和下载逻辑
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: VideoParserRepository,
    private val workManagerDownloadManager: WorkManagerDownloadManager,
    private val transcodeManager: com.tikhub.videoparser.download.WorkManagerTranscodeManager
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
                _downloadState.value = DownloadState.Idle
                Timber.d("已重置下载状态为 Idle")

                Timber.d("更新 UI 状态为 Loading")
                _uiState.value = UiState.Loading

                Timber.d("调用 Repository 解析...")
                repository.parse(input)
                    .onSuccess { resultWrapper ->
                        Timber.i("✅ 解析成功!")
                        Timber.d("结果详情: ${resultWrapper.media::class.simpleName}")
                        Timber.d("⏱️ 耗时: ${resultWrapper.getTimeDisplay()}")
                        Timber.d("💰 费用: ${resultWrapper.getCostDisplay()}")

                        currentPlatform = Platform.detect(input)
                        Timber.d("识别平台: $currentPlatform")

                        // 如果是视频，获取真实文件大小
                        val updatedResult = if (resultWrapper.media is ParsedMedia.Video && resultWrapper.media.videoUrl.isNotEmpty()) {
                            Timber.d("检测到视频，开始获取文件大小...")
                            val fileSize = workManagerDownloadManager.getFileSize(
                                url = resultWrapper.media.videoUrl,
                                platform = currentPlatform
                            )
                            Timber.i("获取到视频文件大小: $fileSize 字节")

                            // 更新 video 的 fileSize 字段
                            val updatedMedia = resultWrapper.media.copy(fileSize = fileSize)
                            resultWrapper.copy(media = updatedMedia)
                        } else {
                            resultWrapper
                        }

                        _uiState.value = UiState.Success(updatedResult)
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
     * 使用 WorkManager 下载视频（支持后台下载）
     */
    fun downloadVideoWithWorkManager(media: ParsedMedia.Video) {
        Timber.i("========== 使用 WorkManager 下载视频 ==========")
        val videoUrl = media.videoUrl

        if (videoUrl.isBlank()) {
            Timber.w("视频 URL 为空")
            _downloadState.value = DownloadState.Failed("视频链接为空")
            return
        }

        val fileName = generateVideoFileName(media)
        val workId = workManagerDownloadManager.downloadVideo(videoUrl, currentPlatform, fileName)

        Timber.i("下载任务已提交: $workId")
        _downloadState.value = DownloadState.Downloading(0)

        // 观察下载进度
        workManagerDownloadManager.getWorkInfo(workId).observeForever { workInfo ->
            workInfo?.let { info ->
                when {
                    info.state.isFinished -> {
                        if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                            val filePath = info.outputData.getString("file_path") ?: "未知路径"
                            _downloadState.value = DownloadState.Success(filePath)
                            Timber.i("✅ 视频下载成功: $filePath")
                        } else {
                            _downloadState.value = DownloadState.Failed("下载失败")
                            Timber.e("❌ 视频下载失败")
                        }
                    }
                    info.state == androidx.work.WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                        _downloadState.value = DownloadState.Downloading(progress)
                    }
                }
            }
        }
    }

    /**
     * 下载视频（简化版，通过 URL）
     * 这是一个桥接方法，为了兼容旧的调用方式
     */
    fun downloadVideo(videoUrl: String) {
        Timber.i("========== 使用 WorkManager 下载视频（简化版）==========")
        Timber.i("视频 URL: $videoUrl")

        if (videoUrl.isBlank()) {
            Timber.w("视频 URL 为空")
            _downloadState.value = DownloadState.Failed("视频链接为空")
            return
        }

        // 生成简单的文件名
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        val workId = workManagerDownloadManager.downloadVideo(videoUrl, currentPlatform, fileName)

        Timber.i("下载任务已提交: $workId")
        _downloadState.value = DownloadState.Downloading(0)

        // 观察下载进度
        workManagerDownloadManager.getWorkInfo(workId).observeForever { workInfo ->
            workInfo?.let { info ->
                when {
                    info.state.isFinished -> {
                        if (info.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                            val filePath = info.outputData.getString("file_path") ?: "未知路径"
                            _downloadState.value = DownloadState.Success(filePath)
                            Timber.i("✅ 视频下载成功: $filePath")
                        } else {
                            _downloadState.value = DownloadState.Failed("下载失败")
                            Timber.e("❌ 视频下载失败")
                        }
                    }
                    info.state == androidx.work.WorkInfo.State.RUNNING -> {
                        val progress = info.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                        _downloadState.value = DownloadState.Downloading(progress)
                    }
                }
            }
        }
    }

    /**
     * 批量下载图片（使用 WorkManager）
     */
    fun downloadAllImagesWithWorkManager(imageUrls: List<String>) {
        Timber.i("========== 使用 WorkManager 批量下载图片 ==========")
        Timber.i("图片数量: ${imageUrls.size}")

        if (imageUrls.isEmpty()) {
            _downloadState.value = DownloadState.Failed("图片列表为空")
            return
        }

        val workIds = workManagerDownloadManager.downloadImages(imageUrls, currentPlatform)
        Timber.i("已提交 ${workIds.size} 个下载任务")
        _downloadState.value = DownloadState.Downloading(0)

        // TODO: 可以观察所有任务的进度
    }

    /**
     * 批量下载图片
     */
    fun downloadAllImages(imageUrls: List<String>) {
        downloadAllImagesWithWorkManager(imageUrls)
    }

    /**
     * 生成视频文件名
     */
    private fun generateVideoFileName(media: ParsedMedia.Video): String {
        val title = media.title.take(30).replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]"), "_")
        val timestamp = System.currentTimeMillis()
        return "${title}_$timestamp.mp4"
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
     */
    fun handleClipboard(clipboardText: String, autoFill: Boolean = true) {
        Timber.d("处理剪贴板内容: ${clipboardText.take(50)}...")

        if (autoFill && clipboardText.isNotBlank()) {
            _inputText.value = clipboardText
            Timber.i("剪贴板内容已自动填充")
        }
    }

    /**
     * 转码视频（ByteVC2 -> H.264）
     */
    fun transcodeVideo(filePath: String, videoTitle: String = "视频") {
        Timber.i("========== 开始转码视频 ==========")
        Timber.i("输入文件: $filePath")
        Timber.i("视频标题: $videoTitle")

        viewModelScope.launch {
            try {
                val workId = transcodeManager.startTranscode(
                    inputFilePath = filePath,
                    videoTitle = videoTitle,
                    codecType = "ByteVC2"
                )

                Timber.i("✅ 转码任务已启动: $workId")
                Timber.i("转码将在后台进行，请查看通知栏获取进度")

                // 可以选择观察转码进度
                transcodeManager.getTranscodeProgress(workId).collect { progress ->
                    Timber.d("转码进度: $progress%")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ 启动转码任务失败")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("MainViewModel 已清除")
    }
}
