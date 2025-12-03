package com.tikhub.videoparser.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.data.repository.VideoParserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 视频解析 ViewModel
 *
 * 职责：
 * 1. 管理解析状态（Loading/Success/Error）
 * 2. 调用 Repository 执行解析
 * 3. 向 UI 层提供解析结果
 * 4. 处理剪贴板检测
 */
@HiltViewModel
class VideoParserViewModel @Inject constructor(
    private val repository: VideoParserRepository
) : ViewModel() {

    // 解析状态
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 输入框文本
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

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
                    onSuccess = { parsedMedia ->
                        Timber.i("✅ ViewModel 解析成功: ${parsedMedia::class.simpleName}")
                        _uiState.value = UiState.Success(parsedMedia)
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
        data class Success(val media: ParsedMedia) : UiState()

        /** 解析失败 */
        data class Error(val message: String) : UiState()
    }
}
