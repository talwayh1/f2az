package com.tikhub.videoparser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.data.model.ParseResultWrapper
import com.tikhub.videoparser.data.repository.VideoParserRepository
import com.tikhub.videoparser.download.DownloadState
import com.tikhub.videoparser.download.DownloadWorker
import com.tikhub.videoparser.download.WorkManagerDownloadManager
import com.tikhub.videoparser.utils.Platform
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * 视频解析 ViewModel
 *
 * 职责：
 * 1. 管理解析状态（Loading/Success/Error）
 * 2. 调用 Repository 执行解析
 * 3. 向 UI 层提供解析结果
 * 4. 处理剪贴板检测
 * 5. 管理下载状态和下载任务
 */
@HiltViewModel
class VideoParserViewModel @Inject constructor(
    private val repository: VideoParserRepository,
    private val workManagerDownloadManager: WorkManagerDownloadManager,
    private val workManager: WorkManager
) : ViewModel() {

    // 解析状态
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 输入框文本
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // 下载状态
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // 当前下载任务 ID
    private var currentDownloadWorkId: UUID? = null

    // 当前平台
    private var currentPlatform: Platform = Platform.UNKNOWN

    // 下载次数计数器（用于显示"第N次下载"）
    private var downloadCountMap = mutableMapOf<String, Int>()

    /**
     * 获取当前媒体的下载次数
     */
    private fun getDownloadCount(mediaId: String): Int {
        return downloadCountMap.getOrDefault(mediaId, 0)
    }

    /**
     * 递增下载次数
     */
    private fun incrementDownloadCount(mediaId: String): Int {
        val newCount = getDownloadCount(mediaId) + 1
        downloadCountMap[mediaId] = newCount
        return newCount
    }

    /**
     * 解析链接
     *
     * @param input 用户输入的文本
     */
    fun parse(input: String) {
        if (input.isBlank()) {
            _uiState.value = UiState.Error("请输入链接")
            return
        }

        viewModelScope.launch {
            Timber.d("========== ViewModel 开始解析 ==========")
            _uiState.value = UiState.Loading

            try {
                val result = repository.parse(input)

                result.fold(
                    onSuccess = { resultWrapper ->
                        Timber.i("✅ ViewModel 解析成功: ${resultWrapper.media::class.simpleName}")
                        Timber.d("⏱️ 耗时: ${resultWrapper.getTimeDisplay()}")
                        Timber.d("💰 费用: ${resultWrapper.getCostDisplay()}")

                        // 清空下载计数器，为新解析重置计数
                        downloadCountMap.clear()
                        resetDownloadState()

                        _uiState.value = UiState.Success(resultWrapper)
                        _inputText.value = "" // 解析成功后自动清空输入框
                    },
                    onFailure = { error ->
                        Timber.e(error, "❌ ViewModel 解析失败")
                        _uiState.value = UiState.Error(error.message ?: "解析失败")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "💥 ViewModel 发生异常")
                _uiState.value = UiState.Error(e.message ?: "未知错误")
            }

            Timber.d("========== ViewModel 解析结束 ==========")
        }
    }

    /**
     * 更新输入框文本
     */
    fun updateInputText(text: String) {
        _inputText.value = text
    }

    /**
     * 清空结果
     */
    fun clearResult() {
        _uiState.value = UiState.Idle
    }

    /**
     * 重置状态
     */
    fun reset() {
        _uiState.value = UiState.Idle
        _inputText.value = ""
        _downloadState.value = DownloadState.Idle
    }

    /**
     * 下载视频
     */
    fun downloadVideo(video: ParsedMedia.Video) {
        Timber.i("========== ViewModel 开始下载视频 ==========")

        // 递增下载次数
        val downloadCount = incrementDownloadCount(video.id)
        Timber.d("下载次数: $downloadCount")

        val fileName = "${video.platform}_${video.id}_${System.currentTimeMillis()}.mp4"
        val platform = Platform.values().find { it.apiParam == video.platform } ?: Platform.UNKNOWN

        // 提交下载任务
        val workId = workManagerDownloadManager.downloadVideo(
            url = video.videoUrl,
            platform = platform,
            fileName = fileName
        )

        currentDownloadWorkId = workId
        _downloadState.value = DownloadState.Downloading(0, downloadCount)

        // 观察下载进度
        observeDownloadProgress(workId, downloadCount)
    }

    /**
     * 下载图片（批量）
     */
    fun downloadImages(imageNote: ParsedMedia.ImageNote) {
        Timber.i("========== ViewModel 开始下载图片 (${imageNote.imageUrls.size}张) ==========")

        // 递增下载次数
        val downloadCount = incrementDownloadCount(imageNote.id)
        Timber.d("下载次数: $downloadCount")

        val platform = Platform.values().find { it.apiParam == imageNote.platform } ?: Platform.UNKNOWN

        // 提交批量下载任务
        val workIds = workManagerDownloadManager.downloadImages(
            urls = imageNote.imageUrls,
            platform = platform
        )

        if (workIds.isNotEmpty()) {
            currentDownloadWorkId = workIds.first()
            _downloadState.value = DownloadState.Downloading(0, downloadCount)

            // 观察第一个任务的进度
            observeDownloadProgress(workIds.first(), downloadCount)
        }
    }

    /**
     * 观察下载进度
     */
    private fun observeDownloadProgress(workId: UUID, downloadCount: Int) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(workId).observeForever { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt(DownloadWorker.KEY_PROGRESS, 0)
                        val downloaded = workInfo.progress.getLong(DownloadWorker.KEY_DOWNLOADED_BYTES, 0)
                        val total = workInfo.progress.getLong(DownloadWorker.KEY_TOTAL_BYTES, 0)

                        _downloadState.value = DownloadState.Downloading(progress, downloadCount)
                        Timber.d("下载进度: $progress%, $downloaded/$total bytes")
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val filePath = workInfo.outputData.getString("file_path") ?: ""
                        _downloadState.value = DownloadState.Success(filePath, downloadCount)
                        Timber.i("✅ 下载成功: $filePath (第${downloadCount}次)")
                    }
                    WorkInfo.State.FAILED -> {
                        _downloadState.value = DownloadState.Failed("下载失败", downloadCount)
                        Timber.e("❌ 下载失败")
                    }
                    WorkInfo.State.CANCELLED -> {
                        _downloadState.value = DownloadState.Failed("下载已取消", downloadCount)
                        Timber.w("⚠️ 下载已取消")
                    }
                    else -> {
                        // ENQUEUED or BLOCKED
                        Timber.d("下载状态: ${workInfo?.state}")
                    }
                }
            }
        }
    }

    /**
     * 重置下载状态（允许重复下载）
     */
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
        currentDownloadWorkId = null
    }

    /**
     * UI 状态（Sealed Class）
     */
    sealed class UiState {
        /** 空闲状态 */
        object Idle : UiState()

        /** 加载中 */
        object Loading : UiState()

        /** 解析成功 */
        data class Success(val result: ParseResultWrapper) : UiState()

        /** 解析失败 */
        data class Error(val message: String) : UiState()
    }
}
