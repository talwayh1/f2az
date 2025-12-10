package com.tikhub.videoparser.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 日志查看对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }

    // 加载日志函数
    fun loadLogs() {
        isLoading = true
        try {
            // 获取 Logcat 日志
            val process = Runtime.getRuntime().exec("logcat -d -t 200")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val logList = mutableListOf<String>()

            reader.forEachLine { line ->
                // 只显示应用相关的日志
                if (line.contains("TikHub") || line.contains("tikhub") || line.contains("videoparser")) {
                    logList.add(line)
                }
            }

            logs = logList.takeLast(100) // 只显示最后100条
            isLoading = false
        } catch (e: Exception) {
            Timber.e(e, "获取日志失败")
            logs = listOf("获取日志失败: ${e.message}")
            isLoading = false
        }
    }

    // 初始加载
    LaunchedEffect(Unit) {
        loadLogs()
    }

    // 清除确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空") },
            text = { Text("确定要清空日志缓冲区吗？此操作会清除 Logcat 中的所有日志。") },
            confirmButton = {
                TextButton(onClick = {
                    try {
                        Runtime.getRuntime().exec("logcat -c")
                        logs = emptyList()
                        Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Timber.e(e, "清空日志失败")
                        Toast.makeText(context, "清空日志失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    showClearDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
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
                        text = "应用日志",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 刷新按钮
                        IconButton(
                            onClick = { loadLogs() }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "刷新",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // 复制日志按钮
                        IconButton(
                            onClick = {
                                if (logs.isNotEmpty()) {
                                    val logText = logs.joinToString("\n")
                                    clipboardManager.setText(AnnotatedString(logText))
                                    Toast.makeText(context, "已复制 ${logs.size} 条日志", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "没有可复制的日志", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "复制全部",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // 清空日志按钮
                        IconButton(
                            onClick = { showClearDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "清空日志",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // 关闭按钮
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 日志内容
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无日志",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        items(logs) { log ->
                            Text(
                                text = log,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                color = when {
                                    log.contains("E/") -> MaterialTheme.colorScheme.error
                                    log.contains("W/") -> MaterialTheme.colorScheme.tertiary
                                    log.contains("D/") -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
