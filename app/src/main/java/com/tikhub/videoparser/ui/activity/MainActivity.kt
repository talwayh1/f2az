package com.tikhub.videoparser.ui.activity

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.data.preferences.SettingsManager
import com.tikhub.videoparser.download.DownloadState
import com.tikhub.videoparser.ui.compose.MediaResultCard
import com.tikhub.videoparser.ui.screen.LogsScreen
import com.tikhub.videoparser.ui.screen.SettingsScreen
import com.tikhub.videoparser.ui.theme.TikHubVideoParserTheme
import com.tikhub.videoparser.ui.viewmodel.VideoParserViewModel
import com.tikhub.videoparser.download.WorkManagerDownloadManager
import com.tikhub.videoparser.utils.Platform
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 主界面 Activity
 *
 * 功能：
 * 1. 自动检测剪贴板链接
 * 2. 输入框 + 解析按钮
 * 3. 展示解析结果卡片
 * 4. 支持视频播放和图片查看
 * 5. 支持下载功能
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: VideoParserViewModel by viewModels()

    @Inject
    lateinit var workManagerDownloadManager: WorkManagerDownloadManager

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TikHubVideoParserTheme {
                MainScreen(
                    viewModel = viewModel,
                    settingsManager = settingsManager,
                    onCheckClipboard = { checkClipboard() },
                    onDownload = { media -> handleDownload(media) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 自动检测剪贴板（可选，用户体验更好）
        checkClipboardOnResume()
    }

    /**
     * 处理下载
     */
    private fun handleDownload(media: ParsedMedia) {
        Timber.i("========== MainActivity 处理下载 ==========")

        when (media) {
            is ParsedMedia.Video -> {
                viewModel.downloadVideo(media)
                Toast.makeText(this, "开始下载视频", Toast.LENGTH_SHORT).show()
            }
            is ParsedMedia.ImageNote -> {
                viewModel.downloadImages(media)
                Toast.makeText(this, "开始下载 ${media.imageUrls.size} 张图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 检查剪贴板
     */
    private fun checkClipboard(): String? {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboardManager?.primaryClip

        if (clipData != null && clipData.itemCount > 0) {
            val text = clipData.getItemAt(0).text?.toString()
            if (!text.isNullOrBlank() && (text.startsWith("http://") || text.startsWith("https://"))) {
                Timber.d("剪贴板检测到链接: $text")
                return text
            }
        }

        return null
    }

    /**
     * Activity 恢复时自动检测剪贴板
     */
    private fun checkClipboardOnResume() {
        val clipboardText = checkClipboard()
        if (clipboardText != null) {
            viewModel.updateInputText(clipboardText)
            Toast.makeText(this, "已从剪贴板读取链接", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * 主界面 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VideoParserViewModel,
    @Suppress("UNUSED_PARAMETER") settingsManager: SettingsManager,
    onCheckClipboard: () -> String?,
    onDownload: (ParsedMedia) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    var showSettingsScreen by remember { mutableStateOf(false) }
    var showLogsScreen by remember { mutableStateOf(false) }

    // 如果显示设置页面，则显示全屏设置页面
    if (showSettingsScreen) {
        SettingsScreen(
            onBack = { showSettingsScreen = false }
        )
        return
    }

    // 如果显示日志页面，则显示全屏日志页面
    if (showLogsScreen) {
        LogsScreen(
            onBack = { showLogsScreen = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TikHub 视频解析",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // 日志按钮
                    IconButton(onClick = { showLogsScreen = true }) {
                        Icon(Icons.Default.Description, "查看日志")
                    }
                    // 设置按钮
                    IconButton(onClick = { showSettingsScreen = true }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 输入区域
            InputSection(
                inputText = inputText,
                onInputTextChange = { viewModel.updateInputText(it) },
                onPasteClick = {
                    val clipText = onCheckClipboard()
                    if (clipText != null) {
                        viewModel.updateInputText(clipText)
                        Toast.makeText(context, "已粘贴链接", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
                    }
                },
                onClearClick = { viewModel.updateInputText("") },
                onParseClick = { viewModel.parse(inputText) },
                isLoading = uiState is VideoParserViewModel.UiState.Loading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 结果区域
            ResultSection(
                uiState = uiState,
                downloadState = downloadState,
                onDownload = onDownload,
                onResetDownloadState = { viewModel.resetDownloadState() },
                onPlayVideo = { videoUrl ->
                    // TODO: 实现视频播放
                    Toast.makeText(context, "播放视频: $videoUrl", Toast.LENGTH_SHORT).show()
                },
                onViewImage = { imageUrls, index ->
                    // TODO: 实现图片查看
                    Toast.makeText(context, "查看图片 ${index + 1}/${imageUrls.size}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

/**
 * 输入区域
 */
@Composable
fun InputSection(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    onParseClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "输入视频链接",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 输入框
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("粘贴抖音、小红书、TikTok等链接") },
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = onClearClick) {
                            Icon(Icons.Default.Clear, contentDescription = "清空")
                        }
                    }
                },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 粘贴按钮
                OutlinedButton(
                    onClick = onPasteClick,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("粘贴")
                }

                // 解析按钮
                Button(
                    onClick = onParseClick,
                    modifier = Modifier.weight(2f),
                    enabled = inputText.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("解析中...")
                    } else {
                        Text("开始解析")
                    }
                }
            }
        }
    }
}

/**
 * 结果区域
 */
@Composable
fun ResultSection(
    uiState: VideoParserViewModel.UiState,
    downloadState: DownloadState,
    onDownload: (ParsedMedia) -> Unit,
    onResetDownloadState: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onViewImage: (List<String>, Int) -> Unit
) {
    when (uiState) {
        is VideoParserViewModel.UiState.Idle -> {
            // 空闲状态：显示提示
            EmptyState()
        }
        is VideoParserViewModel.UiState.Loading -> {
            // 加载中：显示进度
            LoadingState()
        }
        is VideoParserViewModel.UiState.Success -> {
            // 成功：显示结果卡片
            MediaResultCard(
                media = uiState.result.media,
                parseResultWrapper = uiState.result,
                onPlayVideo = onPlayVideo,
                onViewImage = onViewImage,
                onDownload = {
                    // 如果已经下载成功，先重置状态再重新下载
                    if (downloadState is DownloadState.Success) {
                        onResetDownloadState()
                    }
                    onDownload(uiState.result.media)
                },
                downloadState = downloadState
            )
        }
        is VideoParserViewModel.UiState.Error -> {
            // 失败：显示错误
            ErrorState(message = uiState.message)
        }
    }
}

/**
 * 空闲状态
 */
@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "支持 11 个平台",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "抖音 · TikTok · 小红书 · 快手\nB站 · 微博 · 西瓜视频\nInstagram · YouTube · 微视",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * 加载状态
 */
@Composable
fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "正在解析中...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误状态
 */
@Composable
fun ErrorState(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "解析失败",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
