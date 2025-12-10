package com.tikhub.videoparser.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * 日志查看页面（分页加载版本）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var logLines by remember { mutableStateOf<List<String>>(emptyList()) }
    var totalLines by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var autoRefresh by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    // 加载日志
    fun loadLogs() {
        try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            val logFiles = logDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()

            if (logFiles.isEmpty()) {
                logLines = listOf("暂无日志文件")
                totalLines = 0
            } else {
                val latestLog = logFiles.first()
                val allLines = latestLog.readLines()
                totalLines = allLines.size

                // 只显示最后1000行（分页渲染，所以可以支持更多）
                // 最新的日志排在最前面
                logLines = if (allLines.size > 1000) {
                    allLines.takeLast(1000).reversed() +
                        listOf("... (前 ${allLines.size - 1000} 行已省略，总共 $totalLines 行)")
                } else {
                    allLines.reversed()
                }
            }
        } catch (e: Exception) {
            logLines = listOf("加载日志失败：${e.message}")
            totalLines = 0
        }
    }

    // 删除所有日志
    fun deleteAllLogs() {
        try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            logDir.listFiles()?.forEach { it.delete() }
            logLines = listOf("所有日志已清空")
            totalLines = 0
            android.widget.Toast.makeText(context, "日志已清空", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            logLines = listOf("清空日志失败：${e.message}")
            android.widget.Toast.makeText(
                context,
                "清空日志失败：${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 初始加载
    LaunchedEffect(Unit) {
        loadLogs()
    }

    // 自动刷新日志（每2秒刷新一次）
    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            kotlinx.coroutines.delay(2000) // 每2秒刷新一次
            loadLogs()
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要清空所有日志吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteAllLogs()
                    showDeleteDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("查看日志${if (totalLines > 0) " ($totalLines 行)" else ""}")
                        if (autoRefresh) {
                            Text(
                                "自动刷新中...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // 自动刷新开关
                    IconButton(
                        onClick = { autoRefresh = !autoRefresh }
                    ) {
                        Icon(
                            if (autoRefresh) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (autoRefresh) "暂停自动刷新" else "开启自动刷新"
                        )
                    }
                    // 手动刷新按钮
                    IconButton(onClick = { loadLogs() }) {
                        Icon(Icons.Default.Refresh, "手动刷新")
                    }
                    // 复制按钮
                    IconButton(onClick = {
                        if (logLines.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(logLines.joinToString("\n")))
                            android.widget.Toast.makeText(
                                context,
                                "已复制 ${logLines.size} 条日志",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "没有可复制的日志",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, "复制全部")
                    }
                    // 清空按钮
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "清空日志")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 提示卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "📋 日志说明",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "• 最新日志显示在最前面\n• 自动刷新：每 2 秒更新一次（可点击暂停按钮关闭）\n• 使用分页加载，显示最近 1000 行日志\n• 点击复制按钮可复制显示的日志\n• 日志文件位置：Android/data/com.tikhub.videoparser/files/logs/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 日志内容 - 使用 LazyColumn 分页渲染
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(logLines) { index, line ->
                        LogLineItem(
                            line = line,
                            index = index
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单行日志显示组件（支持隔行换色）
 */
@Composable
private fun LogLineItem(
    line: String,
    index: Int
) {
    // 隔行换色：奇数行和偶数行使用不同的背景色（更明显的对比）
    val backgroundColor = if (index % 2 == 0) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)  // 增加透明度使对比更明显
    }

    // 根据日志内容判断日志级别，设置不同的文本颜色
    val textColor = when {
        line.contains("ERROR", ignoreCase = true) || line.contains("错误") || line.contains("E/") -> Color(0xFFEF5350)  // 红色
        line.contains("WARN", ignoreCase = true) || line.contains("警告") || line.contains("W/") -> Color(0xFFFF9800)   // 橙色
        line.contains("INFO", ignoreCase = true) || line.contains("信息") || line.contains("I/") -> Color(0xFF66BB6A)   // 绿色
        line.contains("DEBUG", ignoreCase = true) || line.contains("调试") || line.contains("D/") -> Color(0xFF42A5F5)  // 蓝色
        line.startsWith("...") -> MaterialTheme.colorScheme.onSurfaceVariant // 省略提示行
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = line,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.fillMaxWidth(),
            softWrap = true,  // 允许自动换行
            lineHeight = MaterialTheme.typography.bodySmall.lineHeight
        )
    }
}
