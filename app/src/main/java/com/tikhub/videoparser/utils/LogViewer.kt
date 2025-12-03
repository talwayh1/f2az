package com.tikhub.videoparser.utils

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日志级别枚举
 */
enum class LogLevel(val priority: Int, val color: Color, val icon: String) {
    VERBOSE(Log.VERBOSE, Color.Gray, "V"),
    DEBUG(Log.DEBUG, Color.Blue, "D"),
    INFO(Log.INFO, Color.Green, "I"),
    WARN(Log.WARN, Color(0xFFFF9800), "W"),
    ERROR(Log.ERROR, Color.Red, "E")
}

/**
 * 日志条目数据类
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val fullMessage: String
)

/**
 * 日志管理器
 * 收集和管理应用日志
 */
@Singleton
class LogManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    private val _isRecording = MutableStateFlow(true)
    val isRecording: StateFlow<Boolean> = _isRecording

    init {
        // 初始化时加载最近的日志
        loadRecentLogs()
    }

    /**
     * 添加日志条目
     */
    fun addLog(priority: Int, tag: String?, message: String) {
        if (!_isRecording.value) return

        val level = when (priority) {
            Log.VERBOSE -> LogLevel.VERBOSE
            Log.DEBUG -> LogLevel.DEBUG
            Log.INFO -> LogLevel.INFO
            Log.WARN -> LogLevel.WARN
            Log.ERROR -> LogLevel.ERROR
            else -> LogLevel.DEBUG
        }

        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val safeTag = tag ?: "Unknown"
        val fullMessage = message.replace("\n", "↵") // 显示换行符

        val entry = LogEntry(
            timestamp = timestamp,
            level = level,
            tag = safeTag,
            message = message,
            fullMessage = fullMessage
        )

        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, entry) // 最新日志在前

        // 限制日志数量，避免内存占用过多
        if (currentLogs.size > 1000) {
            currentLogs.removeAt(currentLogs.size - 1)
        }

        _logs.value = currentLogs
    }

    /**
     * 清空日志
     */
    fun clearLogs() {
        _logs.value = emptyList()
        Timber.i("日志已清空")
    }

    /**
     * 切换录制状态
     */
    fun toggleRecording() {
        _isRecording.value = !_isRecording.value
        Timber.i("日志录制${if (_isRecording.value) "已启用" else "已暂停"}")
    }

    /**
     * 从文件加载最近的日志
     */
    private fun loadRecentLogs() {
        // 这里可以实现从日志文件加载历史日志
        Timber.d("加载最近的日志文件")
    }

    /**
     * 导出日志到文件
     */
    suspend fun exportLogs(): String = withContext(Dispatchers.IO) {
        val logDir = File(context.getExternalFilesDir(null), "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val logFile = File(logDir, "exported_logs_$timestamp.txt")

        val currentLogs = _logs.value
        logFile.bufferedWriter().use { writer ->
            writer.write("TikHub 视频解析器 - 导出日志\n")
            writer.write("导出时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            writer.write("日志条数: ${currentLogs.size}\n")
            writer.write("=".repeat(80) + "\n\n")

            currentLogs.forEach { entry ->
                writer.write("${entry.timestamp} ${entry.level.icon}/[${entry.tag}]: ${entry.message}\n")
            }
        }

        Timber.i("日志已导出到: ${logFile.absolutePath}")
        logFile.absolutePath
    }
}

/**
 * 自定义 Timber 日志树，收集日志到 LogManager
 */
class CollectingLogTree(private val logManager: LogManager) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        logManager.addLog(priority, tag, message)

        // 如果有异常，也记录异常信息
        t?.let { throwable ->
            val stackTrace = Log.getStackTraceString(throwable)
            logManager.addLog(priority, tag, stackTrace)
        }
    }
}

/**
 * 日志查看器 Compose 组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewer(
    logManager: LogManager,
    onDismiss: () -> Unit
) {
    val logs by logManager.logs.collectAsStateWithLifecycle()
    val isRecording by logManager.isRecording.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var exportPath by remember { mutableStateOf("") }

    // 自动滚动到最新日志
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty() && listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxSize(0.95f)
            .aspectRatio(9f / 16f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 实时日志",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Row {
                    // 录制状态切换
                    IconButton(
                        onClick = { logManager.toggleRecording() }
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Refresh else Icons.Default.Clear,
                            contentDescription = if (isRecording) "暂停录制" else "开始录制",
                            tint = if (isRecording) Color.Green else Color.Gray
                        )
                    }

                    // 清空日志
                    IconButton(
                        onClick = { logManager.clearLogs() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清空日志",
                            tint = Color.Red
                        )
                    }

                    // 导出日志
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                exportPath = logManager.exportLogs()
                                showExportDialog = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "导出日志",
                            tint = Color.Blue
                        )
                    }
                }
            }
        },
        text = {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        LogEntryItem(log = log)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )

    // 导出成功对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("日志导出成功") },
            text = {
                Column {
                    Text("日志已导出到:")
                    Text(
                        text = exportPath,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Blue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
}

/**
 * 单个日志条目组件
 */
@Composable
private fun LogEntryItem(log: LogEntry) {
    // 根据索引计算背景颜色（隔行变色）
    val backgroundColor = when (log.level) {
        LogLevel.ERROR -> Color(0xFF3D1A1A) // 深红色背景
        LogLevel.WARN -> Color(0xFF3D2A1A)  // 深橙色背景
        LogLevel.INFO -> Color(0xFF1A3D2A)  // 深绿色背景
        LogLevel.DEBUG -> Color(0xFF1A2A3D) // 深蓝色背景
        LogLevel.VERBOSE -> Color(0xFF2D2D2D) // 默认灰色
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 日志级别图标
                Text(
                    text = log.level.icon,
                    color = log.level.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(
                            log.level.color.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 时间戳
                Text(
                    text = log.timestamp,
                    color = Color(0xFFB0B0B0),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Tag
                Text(
                    text = "[${log.tag}]",
                    color = Color(0xFF81C784),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 消息内容（支持换行显示）
            Text(
                text = log.message,
                color = Color(0xFFE0E0E0),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}