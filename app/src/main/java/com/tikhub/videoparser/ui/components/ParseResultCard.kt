package com.tikhub.videoparser.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.download.DownloadState
import com.tikhub.videoparser.utils.FormatUtils

/**
 * 解析结果卡片（视频/图文通用）
 * 支持下载状态反馈和完整显示内容
 */
@Composable
fun ParseResultCard(
    result: ParsedMedia,
    downloadState: DownloadState = DownloadState.Idle,
    onDownloadVideo: (String) -> Unit = {},
    onDownloadAllImages: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 作者信息
            result.author?.let { author ->
                AuthorSection(
                    avatar = author.avatar,
                    nickname = author.nickname,
                    signature = author.signature
                )
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 标题/描述（完整显示，支持复制）
            TitleAndDescriptionSection(
                title = result.getDisplayTitle(),
                description = result.desc
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 统计数据
            result.statistics?.let { stats ->
                StatisticsSection(
                    likes = stats.likeCount,
                    comments = stats.commentCount,
                    shares = stats.shareCount
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 视频信息或图片列表
            when {
                result.isVideo() -> {
                    VideoSection(
                        video = result.video!!,
                        downloadState = downloadState,
                        onDownloadClick = { onDownloadVideo(result.video.getNoWatermarkUrl() ?: "") }
                    )
                }
                result.isImageGallery() -> {
                    ImageGallerySection(
                        images = result.images!!,
                        downloadState = downloadState,
                        onDownloadAllClick = { onDownloadAllImages(result.images.map { it.url }) }
                    )
                }
            }

            // 性能统计和 API 信息（如果有）
            if (result.performance != null || result.apiInfo != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                PerformanceAndApiSection(
                    performance = result.performance,
                    apiInfo = result.apiInfo
                )
            }
        }
    }
}

/**
 * 作者信息区域
 */
@Composable
fun AuthorSection(
    avatar: String?,
    nickname: String,
    signature: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        AsyncImage(
            model = avatar,
            contentDescription = "作者头像",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 昵称和签名
        Column {
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleSmall
            )
            if (!signature.isNullOrBlank()) {
                Text(
                    text = signature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 标题和描述区域（完整显示，支持复制）
 */
@Composable
fun TitleAndDescriptionSection(
    title: String,
    description: String?
) {
    val clipboardManager = LocalClipboardManager.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题（可折叠）
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    modifier = Modifier.clickable {
                        expanded = !expanded
                    }
                )

                // 展开/收起和复制按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { expanded = !expanded }
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (expanded) "收起" else "展开全文", style = MaterialTheme.typography.labelSmall)
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(title))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制标题",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 描述（如果有）
        if (!description.isNullOrBlank() && description != title) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(description))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制描述",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 统计数据区域
 */
@Composable
fun StatisticsSection(
    likes: Long,
    comments: Long,
    shares: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        StatItem(
            icon = Icons.Default.Favorite,
            label = "点赞",
            count = FormatUtils.formatCount(likes)
        )
        StatItem(
            icon = Icons.Default.Comment,
            label = "评论",
            count = FormatUtils.formatCount(comments)
        )
        StatItem(
            icon = Icons.Default.Share,
            label = "分享",
            count = FormatUtils.formatCount(shares)
        )
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 视频信息区域（支持下载状态反馈）
 */
@Composable
fun VideoSection(
    video: com.tikhub.videoparser.data.model.VideoInfo,
    downloadState: DownloadState = DownloadState.Idle,
    onDownloadClick: () -> Unit = {}
) {
    Column {
        // 视频预览播放器
        if (!video.playUrl.isNullOrEmpty()) {
            VideoPreviewPlayer(
                videoUrl = video.playUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                autoPlay = false,
                showControls = true
            )
        } else {
            // 如果没有播放地址，显示封面图
            AsyncImage(
                model = video.cover,
                contentDescription = "视频封面",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 视频参数
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "分辨率: ${FormatUtils.formatResolution(video.width, video.height)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "时长: ${FormatUtils.formatDuration(video.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "大小: ${if (video.size > 0) FormatUtils.formatFileSize(video.size) else "未知"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 下载按钮（带状态反馈）
        DownloadButton(
            downloadState = downloadState,
            onDownloadClick = onDownloadClick
        )
    }
}

/**
 * 下载按钮（根据状态显示不同样式）
 */
@Composable
fun DownloadButton(
    downloadState: DownloadState,
    onDownloadClick: () -> Unit
) {
    when (downloadState) {
        is DownloadState.Idle -> {
            // 空闲状态：显示"保存视频"
            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存视频")
            }
        }
        is DownloadState.Downloading -> {
            // 下载中：显示进度条和百分比
            Button(
                onClick = {}, // 下载中不可点击
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                CircularProgressIndicator(
                    progress = { downloadState.progress / 100f },
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "下载中 ${downloadState.progress}%",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        is DownloadState.Success -> {
            // 成功状态：显示"已保存成功"，但仍可重新下载
            Button(
                onClick = onDownloadClick, // 允许重新下载
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("已保存成功（可重新下载）")
            }
        }
        is DownloadState.Failed -> {
            // 失败状态：显示"重试"
            Button(
                onClick = onDownloadClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("下载失败，点击重试")
            }
        }
    }
}

/**
 * 图片九宫格区域（支持下载状态）
 */
@Composable
fun ImageGallerySection(
    images: List<com.tikhub.videoparser.data.model.ImageInfo>,
    downloadState: DownloadState = DownloadState.Idle,
    onDownloadAllClick: () -> Unit = {}
) {
    Column {
        Text(
            text = "共 ${images.size} 张图片",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 九宫格图片（带信息覆盖层）
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(300.dp) // 限制高度
        ) {
            items(images) { imageInfo ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                ) {
                    // 图片
                    AsyncImage(
                        model = imageInfo.url,
                        contentDescription = "图片",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 信息覆盖层（底部）
                    if (imageInfo.width > 0 || imageInfo.height > 0) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp)
                            ) {
                                // 分辨率
                                if (imageInfo.width > 0 && imageInfo.height > 0) {
                                    Text(
                                        text = "${imageInfo.width}×${imageInfo.height}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }

                                // 文件大小（如果有）
                                if (imageInfo.size > 0) {
                                    Text(
                                        text = FormatUtils.formatFileSize(imageInfo.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 保存全部按钮（带状态反馈）
        when (downloadState) {
            is DownloadState.Idle -> {
                Button(
                    onClick = onDownloadAllClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存全部图片")
                }
            }
            is DownloadState.Downloading -> {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    CircularProgressIndicator(
                        progress = { downloadState.progress / 100f },
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "下载中 ${downloadState.progress}%",
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            is DownloadState.Success -> {
                Button(
                    onClick = onDownloadAllClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("已保存（可重新保存）")
                }
            }
            is DownloadState.Failed -> {
                Button(
                    onClick = onDownloadAllClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载失败，点击重试")
                }
            }
        }
    }
}

/**
 * 性能统计和 API 信息区域
 */
@Composable
fun PerformanceAndApiSection(
    performance: com.tikhub.videoparser.data.model.PerformanceInfo?,
    apiInfo: com.tikhub.videoparser.data.model.ApiCallInfo?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "📊 性能与 API 信息",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 性能统计
            performance?.let { perf ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(label = "总耗时", value = "${perf.totalTime}ms")
                    InfoItem(label = "网络", value = "${perf.networkTime}ms")
                    InfoItem(label = "处理", value = "${perf.processingTime}ms")
                }
            }

            // API 信息
            apiInfo?.let { api ->
                if (performance != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "接口: ${api.endpoint}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "平台: ${api.platform}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (api.cost > 0.0) {
                        Text(
                            text = "¥ ${String.format("%.4f", api.cost)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        Text(
                            text = "¥ 0.00",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 信息条目（键值对）
 */
@Composable
fun InfoItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
