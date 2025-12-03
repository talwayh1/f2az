package com.tikhub.videoparser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.tikhub.videoparser.data.model.ParseResult
import com.tikhub.videoparser.ui.viewmodel.MainViewModel
import com.tikhub.videoparser.utils.FormatUtils
import com.tikhub.videoparser.utils.Platform
import kotlinx.coroutines.launch

/**
 * 平台解析结果展示组件
 * 根据不同平台展示定制化的UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformResultView(
    result: ParseResult,
    platform: Platform,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxSize(0.95f)
            .aspectRatio(9f / 16f),
        title = {
            PlatformHeader(platform = platform, onClose = onDismiss)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 作者信息卡片
                item {
                    AuthorInfoCard(author = result.author, platform = platform)
                }

                // 内容展示区域
                item {
                    ContentDisplayCard(result = result, platform = platform, viewModel = viewModel)
                }

                // 详细信息卡片
                item {
                    DetailInfoCard(result = result, platform = platform)
                }

                // 统计数据卡片
                item {
                    StatisticsCard(statistics = result.statistics)
                }

                // 技术参数卡片（视频）
                if (result.isVideo()) {
                    item {
                        VideoParametersCard(video = result.video!!)
                    }
                }

                // 下载操作区域
                item {
                    DownloadActionCard(
                        result = result,
                        platform = platform,
                        downloadState = downloadState,
                        onDownloadVideo = { url ->
                            viewModel.downloadVideo(url)
                        },
                        onDownloadImages = { urls ->
                            viewModel.downloadAllImages(urls)
                        },
                        onDownloadSingleImage = { url ->
                            viewModel.downloadImage(url)
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

/**
 * 平台头部
 */
@Composable
private fun PlatformHeader(platform: Platform, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 平台图标
            Icon(
                imageVector = platform.getIcon(),
                contentDescription = platform.name,
                tint = platform.getColor(),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${platform.getDisplayName()} 解析结果",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "关闭")
        }
    }
}

/**
 * 作者信息卡片
 */
@Composable
private fun AuthorInfoCard(author: com.tikhub.videoparser.data.model.AuthorInfo?, platform: Platform) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            AsyncImage(
                model = author?.avatar,
                contentDescription = "头像",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 昵称
                Text(
                    text = author?.nickname ?: "未知作者",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 个性签名
                if (!author?.signature.isNullOrBlank()) {
                    Text(
                        text = author!!.signature!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // UID
                if (!author?.uid.isNullOrBlank()) {
                    Text(
                        text = "UID: ${author!!.uid}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 内容展示卡片
 */
@Composable
private fun ContentDisplayCard(
    result: ParseResult,
    platform: Platform,
    viewModel: MainViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Text(
                text = result.getDisplayTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 根据类型展示内容
            when {
                result.isVideo() -> {
                    VideoContentView(video = result.video!!, viewModel = viewModel)
                }
                result.isImageGallery() -> {
                    ImageGalleryView(images = result.images!!)
                }
            }
        }
    }
}

/**
 * 视频内容展示
 */
@Composable
private fun VideoContentView(
    video: com.tikhub.videoparser.data.model.VideoInfo,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(Unit) {
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            val noWatermarkUrl = video.getNoWatermarkUrl()
            if (!noWatermarkUrl.isNullOrBlank()) {
                setMediaItem(MediaItem.fromUri(noWatermarkUrl))
                prepare()
                playWhenReady = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }

    // 视频播放器
    exoPlayer?.let { player ->
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(video.width.toFloat() / video.height.toFloat())
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

/**
 * 图片画廊展示
 */
@Composable
private fun ImageGalleryView(images: List<com.tikhub.videoparser.data.model.ImageInfo>) {
    if (images.size == 1) {
        // 单张图片
        AsyncImage(
            model = images.first().url,
            contentDescription = "图片",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        // 多张图片网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(300.dp),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(images.take(9)) { imageInfo ->
                AsyncImage(
                    model = imageInfo.url,
                    contentDescription = "图片",
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { /* 点击查看大图 */ }
                )
            }
        }

        if (images.size > 9) {
            Text(
                text = "共 ${images.size} 张图片，显示前 9 张",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * 详细信息卡片
 */
@Composable
private fun DetailInfoCard(result: ParseResult, platform: Platform) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "详细信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 分享链接
            if (!result.shareUrl.isNullOrBlank()) {
                InfoRow("分享链接", result.shareUrl!!, Icons.Default.Link)
            }

            // 创建时间
            result.createTime?.let { time ->
                InfoRow(
                    "发布时间",
                    FormatUtils.formatTimestamp(time),
                    Icons.Default.Schedule
                )
            }

            // 内容类型
            InfoRow(
                "内容类型",
                if (result.isVideo()) "视频" else "图文",
                Icons.Default.Category
            )
        }
    }
}

/**
 * 统计数据卡片
 */
@Composable
private fun StatisticsCard(statistics: com.tikhub.videoparser.data.model.Statistics?) {
    if (statistics == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "数据统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StatItem(Icons.Default.Favorite, "点赞", statistics.likeCount)
                }
                item {
                    StatItem(Icons.Default.Comment, "评论", statistics.commentCount)
                }
                item {
                    StatItem(Icons.Default.Share, "分享", statistics.shareCount)
                }
                item {
                    StatItem(Icons.Default.Download, "下载", statistics.downloadCount)
                }
                item {
                    StatItem(Icons.Default.Bookmark, "收藏", statistics.collectCount)
                }
                item {
                    StatItem(Icons.Default.PlayArrow, "播放", statistics.playCount)
                }
            }
        }
    }
}

/**
 * 视频参数卡片
 */
@Composable
private fun VideoParametersCard(video: com.tikhub.videoparser.data.model.VideoInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "视频参数",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow("分辨率", "${video.width}×${video.height}", Icons.Default.HighQuality)
            InfoRow("时长", FormatUtils.formatDuration(video.duration), Icons.Default.Timer)
            InfoRow("文件大小", FormatUtils.formatFileSize(video.size), Icons.Default.Storage)
            InfoRow("码率", "${video.bitrate} bps", Icons.Default.Speed)
            if (!video.ratio.isNullOrBlank()) {
                InfoRow("宽高比", video.ratio, Icons.Default.AspectRatio)
            }
        }
    }
}

/**
 * 下载操作卡片
 */
@Composable
private fun DownloadActionCard(
    result: ParseResult,
    platform: Platform,
    downloadState: com.tikhub.videoparser.download.DownloadState,
    onDownloadVideo: (String) -> Unit,
    onDownloadImages: (List<String>) -> Unit,
    onDownloadSingleImage: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "下载操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                result.isVideo() -> {
                    val videoUrl = result.video!!.getNoWatermarkUrl()
                    if (!videoUrl.isNullOrBlank()) {
                        Button(
                            onClick = { onDownloadVideo(videoUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = downloadState !is com.tikhub.videoparser.download.DownloadState.Downloading
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (downloadState is com.tikhub.videoparser.download.DownloadState.Downloading) "下载中..." else "保存视频")
                        }
                    }
                }
                result.isImageGallery() -> {
                    // 单张图片下载
                    result.images!!.take(3).forEachIndexed { index, imageInfo ->
                        OutlinedButton(
                            onClick = { onDownloadSingleImage(imageInfo.url) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            enabled = downloadState !is com.tikhub.videoparser.download.DownloadState.Downloading
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保存图片 ${index + 1}")
                        }
                    }

                    // 批量下载
                    if (result.images!!.size > 3) {
                        Button(
                            onClick = { onDownloadImages(result.images!!.map { it.url }) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            enabled = downloadState !is com.tikhub.videoparser.download.DownloadState.Downloading
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保存全部 ${result.images!!.size} 张图片")
                        }
                    }
                }
            }

            // 下载状态显示
            when (downloadState) {
                is com.tikhub.videoparser.download.DownloadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = (downloadState.progress / 100f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    Text(
                        text = "正在下载... ${downloadState.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                is com.tikhub.videoparser.download.DownloadState.Success -> {
                    Text(
                        text = "✅ 下载成功: ${downloadState.filePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is com.tikhub.videoparser.download.DownloadState.Failed -> {
                    Text(
                        text = "❌ 下载失败: ${downloadState.error}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(4.dp))
}

/**
 * 统计项组件
 */
@Composable
private fun StatItem(icon: ImageVector, label: String, count: Long) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = FormatUtils.formatNumber(count),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Platform 扩展函数
 */
@Composable
private fun Platform.getIcon(): ImageVector {
    return when (this) {
        Platform.DOUYIN -> Icons.Default.VideoLibrary
        Platform.TIKTOK -> Icons.Default.MusicVideo
        Platform.XIAOHONGSHU -> Icons.Default.PhotoLibrary
        Platform.KUAISHOU -> Icons.Default.Movie
        else -> Icons.Default.PlayArrow
    }
}

@Composable
private fun Platform.getColor(): Color {
    return when (this) {
        Platform.DOUYIN -> Color(0xFF000000)
        Platform.TIKTOK -> Color(0xFF000000)
        Platform.XIAOHONGSHU -> Color(0xFFFF2442)
        Platform.KUAISHOU -> Color(0xFFFF6600)
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun Platform.getDisplayName(): String {
    return when (this) {
        Platform.DOUYIN -> "抖音"
        Platform.TIKTOK -> "TikTok"
        Platform.XIAOHONGSHU -> "小红书"
        Platform.KUAISHOU -> "快手"
        else -> "未知平台"
    }
}