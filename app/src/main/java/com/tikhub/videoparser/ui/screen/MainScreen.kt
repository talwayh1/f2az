package com.tikhub.videoparser.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tikhub.videoparser.ui.components.ParseResultCard
import com.tikhub.videoparser.ui.viewmodel.MainViewModel
import com.tikhub.videoparser.ui.viewmodel.UiState

/**
 * 主界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val sdkStatus by viewModel.sdkStatus.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("TikHub 视频解析")
                        // SDK 状态指示器
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (sdkStatus)
                                        MaterialTheme.colorScheme.tertiary  // 绿色
                                    else
                                        MaterialTheme.colorScheme.error,    // 红色
                                    shape = CircleShape
                                )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // 日志按钮
                    IconButton(onClick = onNavigateToLogs) {
                        Icon(Icons.Default.Description, "查看日志")
                    }
                    // 设置按钮
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 输入框区域
            InputSection(
                inputText = inputText,
                onInputChange = { viewModel.updateInputText(it) },
                onPasteClick = {
                    clipboardManager.getText()?.text?.let { text ->
                        viewModel.handleClipboard(text, autoFill = true)
                    }
                },
                onClearClick = { viewModel.updateInputText("") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 解析按钮
            Button(
                onClick = { viewModel.parse() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = inputText.isNotBlank() && uiState !is UiState.Loading
            ) {
                Text(
                    text = if (uiState is UiState.Loading) "解析中..." else "开始解析",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 结果展示区域
            when (val state = uiState) {
                is UiState.Idle -> {
                    // 空闲状态：显示提示
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "支持平台",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "抖音 • TikTok • 小红书 • 快手",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is UiState.Loading -> {
                    CircularProgressIndicator()
                }

                is UiState.Success -> {
                    ParseResultCard(
                        result = state.result,
                        downloadState = downloadState,
                        onDownloadVideo = { url ->
                            viewModel.downloadVideo(url)
                        },
                        onDownloadAllImages = { urls ->
                            viewModel.downloadAllImages(urls)
                        }
                    )
                }

                is UiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "解析失败",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 输入框区域组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit
) {
    OutlinedTextField(
        value = inputText,
        onValueChange = onInputChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("粘贴抖音/小红书/快手/TikTok 链接...") },
        trailingIcon = {
            Row {
                if (inputText.isNotBlank()) {
                    IconButton(onClick = onClearClick) {
                        Icon(Icons.Default.Clear, contentDescription = "清空")
                    }
                }
                IconButton(onClick = onPasteClick) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "粘贴")
                }
            }
        },
        maxLines = 3,
        singleLine = false
    )
}
