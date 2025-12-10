package com.tikhub.videoparser.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tikhub.videoparser.data.preferences.SettingsManager

/**
 * 设置对话框
 *
 * 功能：
 * 1. 日志系统开关（默认开启）
 * 2. API Key 管理（显示/隐藏/修改/删除）
 * 3. 域名设置（自定义域名）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    settingsManager: SettingsManager
) {
    // 状态管理
    val isLogEnabled by settingsManager.isLogEnabled.collectAsState()
    val currentApiKey by settingsManager.apiKey.collectAsState()
    val currentDomain by settingsManager.customDomain.collectAsState()

    var showApiKeyInput by remember { mutableStateOf(false) }
    var showDomainInput by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider()

                // 设置内容（可滚动）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ========== 日志系统设置 ==========
                    SettingsSectionTitle("日志系统")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "启用日志收集",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "开启后将收集运行日志，帮助排查问题",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isLogEnabled,
                                onCheckedChange = { settingsManager.setLogEnabled(it) }
                            )
                        }
                    }

                    // ========== API Key 管理 ==========
                    SettingsSectionTitle("API Key")

                    ApiKeySection(
                        currentApiKey = currentApiKey,
                        settingsManager = settingsManager,
                        onShowInput = { showApiKeyInput = true },
                        onShowDeleteConfirm = { showDeleteConfirm = true }
                    )

                    // ========== 域名设置 ==========
                    SettingsSectionTitle("域名配置")

                    DomainSection(
                        currentDomain = currentDomain,
                        settingsManager = settingsManager,
                        onShowInput = { showDomainInput = true }
                    )

                    // ========== 关于信息 ==========
                    SettingsSectionTitle("关于")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "TikHub 视频解析器",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "版本: 1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "支持的平台",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "抖音 · TikTok · 小红书 · 快手 · B站\n微博 · Instagram · YouTube · 西瓜视频",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // API Key 输入对话框
    if (showApiKeyInput) {
        ApiKeyInputDialog(
            currentApiKey = currentApiKey,
            onDismiss = { showApiKeyInput = false },
            onConfirm = { newKey ->
                settingsManager.setApiKey(newKey)
                showApiKeyInput = false
            }
        )
    }

    // 域名输入对话框
    if (showDomainInput) {
        DomainInputDialog(
            currentDomain = currentDomain,
            settingsManager = settingsManager,
            onDismiss = { showDomainInput = false },
            onConfirm = { newDomain ->
                settingsManager.setCustomDomain(newDomain)
                showDomainInput = false
            }
        )
    }

    // 删除 API Key 确认对话框
    if (showDeleteConfirm) {
        DeleteApiKeyConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                settingsManager.deleteApiKey()
                showDeleteConfirm = false
            }
        )
    }
}

/**
 * 设置区块标题
 */
@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * API Key 管理区域
 */
@Composable
private fun ApiKeySection(
    currentApiKey: String?,
    settingsManager: SettingsManager,
    onShowInput: () -> Unit,
    onShowDeleteConfirm: () -> Unit
) {
    var isApiKeyVisible by remember { mutableStateOf(false) }
    val hasApiKey = !currentApiKey.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // API Key 显示/隐藏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasApiKey) "当前 API Key" else "未配置 API Key",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (hasApiKey) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isApiKeyVisible) currentApiKey!! else settingsManager.getMaskedApiKey(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hasApiKey) {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isApiKeyVisible) "隐藏" else "显示",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 修改按钮
                Button(
                    onClick = onShowInput,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (hasApiKey) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (hasApiKey) "修改" else "添加")
                }

                // 删除按钮（仅当有 API Key 时显示）
                if (hasApiKey) {
                    OutlinedButton(
                        onClick = onShowDeleteConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除")
                    }
                }
            }
        }
    }
}

/**
 * 域名配置区域
 */
@Composable
private fun DomainSection(
    currentDomain: String,
    settingsManager: SettingsManager,
    onShowInput: () -> Unit
) {
    val isCustomDomain = settingsManager.isUsingCustomDomain()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 域名显示
            Column {
                Text(
                    text = "当前域名",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentDomain,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (isCustomDomain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isCustomDomain) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "（自定义域名）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 修改按钮
                Button(
                    onClick = onShowInput,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("修改")
                }

                // 恢复默认按钮
                if (isCustomDomain) {
                    OutlinedButton(
                        onClick = { settingsManager.resetToDefaultDomain() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("恢复默认")
                    }
                }
            }
        }
    }
}

/**
 * API Key 输入对话框
 */
@Composable
private fun ApiKeyInputDialog(
    currentApiKey: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(currentApiKey ?: "") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentApiKey.isNullOrBlank()) "添加 API Key" else "修改 API Key") },
        text = {
            Column {
                Text(
                    text = "请输入您的 TikHub API Key",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "隐藏" else "显示"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(apiKeyInput) },
                enabled = apiKeyInput.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 域名输入对话框
 */
@Composable
private fun DomainInputDialog(
    currentDomain: String,
    settingsManager: SettingsManager,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var domainInput by remember { mutableStateOf(currentDomain) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改域名") },
        text = {
            Column {
                Text(
                    text = "请输入自定义 API 域名",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = {
                        domainInput = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://api.tikhub.io") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "格式示例：https://api.example.com",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (settingsManager.isValidDomain(domainInput)) {
                        onConfirm(domainInput)
                    } else {
                        errorMessage = "域名格式不正确，请使用 http:// 或 https:// 开头"
                    }
                },
                enabled = domainInput.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 删除 API Key 确认对话框
 */
@Composable
private fun DeleteApiKeyConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("确认删除 API Key？") },
        text = {
            Text(
                "删除后将无法使用解析功能，您需要重新配置 API Key。",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
