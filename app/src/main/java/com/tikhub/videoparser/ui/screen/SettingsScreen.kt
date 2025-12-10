package com.tikhub.videoparser.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tikhub.videoparser.ui.viewmodel.SettingsViewModel

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val autoRefreshLog by viewModel.autoRefreshLog.collectAsState()
    val logServerRunning by viewModel.logServerRunning.collectAsState()
    val logServerUrl by viewModel.logServerUrl.collectAsState()

    var apiKeyInput by remember { mutableStateOf(apiKey) }
    var baseUrlInput by remember { mutableStateOf(baseUrl) }
    var showApiKey by remember { mutableStateOf(false) }

    // 当保存成功时更新输入框
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            apiKeyInput = apiKey
            baseUrlInput = baseUrl
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Key 设置
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Key, "API Key")
                        Text(
                            text = "TikHub API 配置",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    HorizontalDivider()

                    // API Key 输入
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("API Key") },
                        placeholder = { Text("请输入 TikHub API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            TextButton(onClick = { showApiKey = !showApiKey }) {
                                Text(if (showApiKey) "隐藏" else "显示")
                            }
                        },
                        singleLine = true
                    )

                    // Base URL 输入
                    OutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        label = { Text("API 基础地址") },
                        placeholder = { Text("https://api.tikhub.io/") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = baseUrlInput.isNotBlank() && !baseUrlInput.matches(Regex("^https?://.*"))
                    )

                    // 域名快速选择按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { baseUrlInput = "https://api.tikhub.io/" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🌍 国际域名")
                        }
                        OutlinedButton(
                            onClick = { baseUrlInput = "https://api.tikhub.dev/" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🇨🇳 中国镜像")
                        }
                    }

                    Text(
                        text = "💡 提示：中国大陆用户建议使用中国镜像以获得更快的访问速度",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 提示文本
                    Text(
                        text = "💡 API Key 获取方式：\n1. 访问 https://tikhub.io\n2. 注册并登录账号\n3. 在控制台获取 API Key",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 保存按钮
                    Button(
                        onClick = {
                            viewModel.saveApiSettings(apiKeyInput, baseUrlInput)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = apiKeyInput.isNotBlank() && baseUrlInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, "保存")
                        Spacer(Modifier.width(8.dp))
                        Text("保存设置")
                    }

                    // 操作按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 清空按钮
                        OutlinedButton(
                            onClick = {
                                viewModel.clearApiKey()
                                apiKeyInput = ""
                                baseUrlInput = ""
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("清空")
                        }

                        // 重置按钮
                        OutlinedButton(
                            onClick = {
                                viewModel.resetApiKey()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, "重置", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("重置默认")
                        }
                    }

                    // 成功提示
                    if (saveSuccess) {
                        Text(
                            text = "✅ 设置已保存",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 日志设置
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "日志设置",
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    // 自动刷新开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自动刷新日志",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "解析时自动滚动显示实时日志",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = autoRefreshLog,
                            onCheckedChange = { enabled ->
                                viewModel.toggleAutoRefreshLog(enabled)
                            }
                        )
                    }
                }
            }

            // 远程日志查看
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (logServerRunning) Icons.Default.Wifi else Icons.Default.WifiOff,
                            "远程日志",
                            tint = if (logServerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "远程日志查看",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    HorizontalDivider()

                    // 服务器状态
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "启用远程日志服务器",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "在PC浏览器中实时查看应用日志",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = logServerRunning,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    viewModel.startLogServer()
                                } else {
                                    viewModel.stopLogServer()
                                }
                            }
                        )
                    }

                    // 显示访问地址
                    if (logServerRunning && logServerUrl != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "📱 访问地址",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = logServerUrl!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = "💡 在PC浏览器中打开此地址即可查看日志\n⚠️ 请确保手机和PC在同一WiFi网络下",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // 使用说明
                    Text(
                        text = "📖 使用说明：\n1. 确保手机和PC连接到同一WiFi网络\n2. 开启远程日志服务器\n3. 在PC浏览器中访问显示的地址\n4. 即可实时查看应用日志",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 关于信息
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "关于 TikHub",
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    Text(
                        text = "TikHub 是一个强大的短视频平台数据解析服务，支持抖音、TikTok、小红书、快手等多个平台的视频/图文解析和下载。",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "官方文档：https://docs.tikhub.io",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
