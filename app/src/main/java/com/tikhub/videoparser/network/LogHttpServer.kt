package com.tikhub.videoparser.network

import android.content.Context
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * HTTP日志服务器
 * 提供Web界面远程查看应用日志
 */
class LogHttpServer(
    private val context: Context,
    port: Int = 8080
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "LogHttpServer"
        private const val MAX_LOG_LINES = 2000 // 最多显示2000行日志
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Timber.tag(TAG).d("收到请求: $uri")

        return when (uri) {
            "/" -> serveIndexPage()
            "/logs" -> serveLogsJson()
            "/logs/raw" -> serveRawLogs()
            "/logs/download" -> downloadLogs()
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
        }
    }

    /**
     * 提供主页HTML
     */
    private fun serveIndexPage(): Response {
        val html = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>TikHub 日志查看器</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 {
            font-size: 2em;
            margin-bottom: 10px;
        }
        .header p {
            opacity: 0.9;
            font-size: 1.1em;
        }
        .controls {
            padding: 20px 30px;
            background: #f8f9fa;
            border-bottom: 1px solid #dee2e6;
            display: flex;
            gap: 15px;
            flex-wrap: wrap;
            align-items: center;
        }
        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 14px;
            font-weight: 500;
            transition: all 0.3s;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }
        .btn-primary {
            background: #667eea;
            color: white;
        }
        .btn-primary:hover {
            background: #5568d3;
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        }
        .btn-success {
            background: #28a745;
            color: white;
        }
        .btn-success:hover {
            background: #218838;
        }
        .btn-danger {
            background: #dc3545;
            color: white;
        }
        .btn-danger:hover {
            background: #c82333;
        }
        .btn-secondary {
            background: #6c757d;
            color: white;
        }
        .btn-secondary:hover {
            background: #5a6268;
        }
        .status {
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 500;
            margin-left: auto;
        }
        .status.active {
            background: #d4edda;
            color: #155724;
        }
        .status.paused {
            background: #fff3cd;
            color: #856404;
        }
        .filter-bar {
            padding: 15px 30px;
            background: #f8f9fa;
            border-bottom: 1px solid #dee2e6;
            display: flex;
            gap: 10px;
            align-items: center;
        }
        .filter-bar input {
            flex: 1;
            padding: 10px 15px;
            border: 1px solid #ced4da;
            border-radius: 6px;
            font-size: 14px;
        }
        .filter-bar select {
            padding: 10px 15px;
            border: 1px solid #ced4da;
            border-radius: 6px;
            font-size: 14px;
            background: white;
        }
        .log-container {
            height: calc(100vh - 400px);
            overflow-y: auto;
            padding: 20px 30px;
            font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
            font-size: 13px;
            line-height: 1.6;
        }
        .log-line {
            padding: 6px 12px;
            margin: 2px 0;
            border-radius: 4px;
            white-space: pre-wrap;
            word-break: break-all;
        }
        .log-line:nth-child(even) {
            background: #f8f9fa;
        }
        .log-line.error {
            background: #f8d7da;
            color: #721c24;
            border-left: 4px solid #dc3545;
        }
        .log-line.warn {
            background: #fff3cd;
            color: #856404;
            border-left: 4px solid #ffc107;
        }
        .log-line.info {
            background: #d1ecf1;
            color: #0c5460;
            border-left: 4px solid #17a2b8;
        }
        .log-line.debug {
            background: #d4edda;
            color: #155724;
            border-left: 4px solid #28a745;
        }
        .stats {
            padding: 15px 30px;
            background: #f8f9fa;
            border-top: 1px solid #dee2e6;
            display: flex;
            justify-content: space-between;
            font-size: 14px;
            color: #6c757d;
        }
        .loading {
            text-align: center;
            padding: 40px;
            color: #6c757d;
        }
        .spinner {
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            animation: spin 1s linear infinite;
            margin: 0 auto 20px;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📱 TikHub 日志查看器</h1>
            <p>实时查看应用运行日志</p>
        </div>

        <div class="controls">
            <button class="btn btn-primary" onclick="refreshLogs()">
                🔄 刷新日志
            </button>
            <button class="btn btn-danger" id="autoRefreshBtn" onclick="toggleAutoRefresh()">
                ⏸️ 暂停刷新
            </button>
            <button class="btn btn-secondary" onclick="downloadLogs()">
                💾 下载日志
            </button>
            <button class="btn btn-danger" onclick="clearDisplay()">
                🗑️ 清空显示
            </button>
            <span class="status active" id="status">自动刷新: 已暂停</span>
        </div>

        <div class="filter-bar">
            <input type="text" id="searchInput" placeholder="🔍 搜索日志内容..." onkeyup="filterLogs()">
            <select id="levelFilter" onchange="filterLogs()">
                <option value="all">所有级别</option>
                <option value="error">ERROR</option>
                <option value="warn">WARN</option>
                <option value="info">INFO</option>
                <option value="debug">DEBUG</option>
            </select>
        </div>

        <div class="log-container" id="logContainer">
            <div class="loading">
                <div class="spinner"></div>
                <p>正在加载日志...</p>
            </div>
        </div>

        <div class="stats">
            <span id="totalLines">总行数: 0</span>
            <span id="filteredLines">显示: 0</span>
            <span id="lastUpdate">最后更新: --</span>
        </div>
    </div>

    <script>
        let autoRefresh = true;  // 默认开启自动刷新
        let refreshInterval = null;
        let allLogs = [];

        // 刷新日志
        async function refreshLogs() {
            try {
                const response = await fetch('/logs');
                const data = await response.json();
                allLogs = data.logs || [];

                document.getElementById('totalLines').textContent = '总行数: ' + allLogs.length;
                document.getElementById('lastUpdate').textContent = '最后更新: ' + new Date().toLocaleTimeString();

                filterLogs();
            } catch (error) {
                console.error('加载日志失败:', error);
                document.getElementById('logContainer').innerHTML =
                    '<div class="loading"><p style="color: #dc3545;">❌ 加载日志失败: ' + error.message + '</p></div>';
            }
        }

        // 过滤日志
        function filterLogs() {
            const searchText = document.getElementById('searchInput').value.toLowerCase();
            const levelFilter = document.getElementById('levelFilter').value;

            let filtered = allLogs;

            // 按级别过滤
            if (levelFilter !== 'all') {
                filtered = filtered.filter(log =>
                    log.toLowerCase().includes(levelFilter.toLowerCase())
                );
            }

            // 按搜索文本过滤
            if (searchText) {
                filtered = filtered.filter(log =>
                    log.toLowerCase().includes(searchText)
                );
            }

            // 渲染日志
            const container = document.getElementById('logContainer');
            if (filtered.length === 0) {
                container.innerHTML = '<div class="loading"><p>📭 没有匹配的日志</p></div>';
            } else {
                container.innerHTML = filtered.map(log => {
                    let className = 'log-line';
                    if (log.includes('ERROR') || log.includes('错误')) className += ' error';
                    else if (log.includes('WARN') || log.includes('警告')) className += ' warn';
                    else if (log.includes('INFO') || log.includes('信息')) className += ' info';
                    else if (log.includes('DEBUG') || log.includes('调试')) className += ' debug';

                    return '<div class="' + className + '">' + escapeHtml(log) + '</div>';
                }).join('');
            }

            document.getElementById('filteredLines').textContent = '显示: ' + filtered.length;
        }

        // 切换自动刷新
        function toggleAutoRefresh() {
            autoRefresh = !autoRefresh;
            const btn = document.getElementById('autoRefreshBtn');
            const status = document.getElementById('status');

            if (autoRefresh) {
                btn.innerHTML = '⏸️ 暂停刷新';
                btn.className = 'btn btn-danger';
                status.textContent = '自动刷新: 进行中';
                status.className = 'status active';
                refreshInterval = setInterval(refreshLogs, 2000);
                refreshLogs();
            } else {
                btn.innerHTML = '▶️ 开启自动刷新';
                btn.className = 'btn btn-success';
                status.textContent = '自动刷新: 已暂停';
                status.className = 'status paused';
                if (refreshInterval) {
                    clearInterval(refreshInterval);
                    refreshInterval = null;
                }
            }
        }

        // 下载日志
        function downloadLogs() {
            window.open('/logs/download', '_blank');
        }

        // 清空显示
        function clearDisplay() {
            allLogs = [];
            document.getElementById('logContainer').innerHTML =
                '<div class="loading"><p>📭 显示已清空</p></div>';
            document.getElementById('totalLines').textContent = '总行数: 0';
            document.getElementById('filteredLines').textContent = '显示: 0';
        }

        // HTML转义
        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        // 页面加载时初始化
        window.onload = function() {
            // 启动自动刷新
            toggleAutoRefresh();
        };
    </script>
</body>
</html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }

    /**
     * 提供JSON格式的日志数据
     */
    private fun serveLogsJson(): Response {
        try {
            val logs = readLogFiles()
            val gson = Gson()
            val json = gson.toJson(mapOf("logs" to logs))
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "读取日志失败")
            val gson = Gson()
            val errorJson = gson.toJson(mapOf("error" to (e.message ?: "未知错误")))
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", errorJson)
        }
    }

    /**
     * 提供纯文本格式的日志
     */
    private fun serveRawLogs(): Response {
        try {
            val logs = readLogFiles()
            val text = logs.joinToString("\n")
            return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", text)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "读取日志失败")
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "读取日志失败: ${e.message}"
            )
        }
    }

    /**
     * 下载日志文件
     */
    private fun downloadLogs(): Response {
        try {
            val logs = readLogFiles()
            val text = logs.joinToString("\n")
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "tikhub_logs_${dateFormat.format(Date())}.txt"

            return newFixedLengthResponse(Response.Status.OK, "text/plain; charset=utf-8", text).apply {
                addHeader("Content-Disposition", "attachment; filename=\"$fileName\"")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "下载日志失败")
            return newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "下载日志失败: ${e.message}"
            )
        }
    }

    /**
     * 读取日志文件
     */
    private fun readLogFiles(): List<String> {
        val logDir = File(context.getExternalFilesDir(null), "logs")
        if (!logDir.exists() || !logDir.isDirectory) {
            return listOf("日志目录不存在")
        }

        val logFiles = logDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        if (logFiles.isEmpty()) {
            return listOf("暂无日志文件")
        }

        // 读取最新的日志文件
        val latestLog = logFiles.first()
        val allLines = latestLog.readLines()

        // 只返回最后N行（最新的日志），并反转顺序，让最新的日志排在最前面
        return if (allLines.size > MAX_LOG_LINES) {
            allLines.takeLast(MAX_LOG_LINES).reversed()
        } else {
            allLines.reversed()
        }
    }

    /**
     * JSON字符串转义
     */
    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
