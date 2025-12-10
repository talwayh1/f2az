package com.tikhub.videoparser.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tikhub.videoparser.data.model.ParsedMedia
import com.tikhub.videoparser.utils.Platform
import com.tikhub.videoparser.utils.iconRes
import com.tikhub.videoparser.utils.themeColor

/**
 * 解析结果展示卡片（Jetpack Compose 版本）
 *
 * 特性：
 * 1. 多态渲染：根据 ParsedMedia 类型自动选择布局
 * 2. 平台识别：显示平台图标和品牌色
 * 3. 统计信息：格式化显示点赞、评论等数据
 * 4. 交互支持：视频播放、图片查看、下载功能
 * 5. 解析信息：显示耗时、费用、接口信息
 *
 * @param media ParsedMedia 数据（Video 或 ImageNote）
 * @param parseResultWrapper 解析结果包装（包含耗时和费用信息）
 * @param onPlayVideo 点击播放视频的回调
 * @param onViewImage 点击查看图片的回调
 * @param onDownload 点击下载的回调
 * @param downloadState 下载状态
 */
@Composable
fun MediaResultCard(
    media: ParsedMedia,
    parseResultWrapper: com.tikhub.videoparser.data.model.ParseResultWrapper? = null,
    onPlayVideo: (String) -> Unit = {},
    onViewImage: (List<String>, Int) -> Unit = { _, _ -> },
    onDownload: () -> Unit = {},
    onTranscode: (String) -> Unit = {},  // 🎯 新增：转码回调
    downloadState: com.tikhub.videoparser.download.DownloadState = com.tikhub.videoparser.download.DownloadState.Idle,
    downloadedFilePath: String? = null,  // 🎯 新增：已下载文件路径
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. 通用头部：作者信息 + 平台标识
            AuthorHeader(
                authorName = media.authorName,
                authorAvatar = media.authorAvatar,
                platform = media.platform
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. 内容展示区：根据类型分发
            when (media) {
                is ParsedMedia.Video -> {
                    VideoContent(
                        video = media,
                        onPlay = onPlayVideo
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    VideoInfoSection(video = media)
                }
                is ParsedMedia.ImageNote -> {
                    ImageNoteContent(
                        imageNote = media,
                        onViewImage = onViewImage
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ImageInfoSection(imageNote = media)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. 标题和统计信息（可复制）
            TitleSection(
                title = media.title,
                stats = media.stats
            )

            // 4. 解析信息展示（如果有数据）
            if (parseResultWrapper != null) {
                Spacer(modifier = Modifier.height(12.dp))
                ParseInfoSection(parseResultWrapper = parseResultWrapper, platform = media.platform)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. 操作按钮
            ActionButtons(
                media = media,
                onDownload = onDownload,
                onTranscode = onTranscode,
                downloadState = downloadState,
                downloadedFilePath = downloadedFilePath
            )
        }
    }
}

/**
 * 作者信息头部
 */
@Composable
private fun AuthorHeader(
    authorName: String,
    authorAvatar: String,
    platform: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 作者头像
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(authorAvatar)
                .crossfade(true)
                .build(),
            contentDescription = "作者头像",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 作者名称
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = authorName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 平台名称 + 图标
        val platformEnum = Platform.values().find { it.apiParam == platform }
        if (platformEnum != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 平台文字
                Text(
                    text = platformEnum.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(platformEnum.themeColor)
                )
                // 平台图标
                Icon(
                    painter = painterResource(id = platformEnum.iconRes),
                    contentDescription = platformEnum.displayName,
                    tint = Color(platformEnum.themeColor),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 视频内容展示
 */
@Composable
private fun VideoContent(
    video: ParsedMedia.Video,
    onPlay: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(
                if (video.width > 0 && video.height > 0) {
                    video.width.toFloat() / video.height
                } else {
                    16f / 9f
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .clickable { onPlay(video.videoUrl) }
    ) {
        // 封面图
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(video.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = "视频封面",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 播放按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "播放",
                    tint = Color.White,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }

        // 视频时长
        if (video.duration > 0) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = video.getFormattedDuration(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // 视频信息标签
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Text(
                text = video.getAspectRatioDescription(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * 图文内容展示（显示所有图片）
 */
@Composable
private fun ImageNoteContent(
    imageNote: ParsedMedia.ImageNote,
    onViewImage: (List<String>, Int) -> Unit
) {
    when {
        imageNote.imageUrls.size == 1 -> {
            // 单图：大图展示
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageNote.imageUrls.first())
                    .crossfade(true)
                    .build(),
                contentDescription = "图片",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onViewImage(imageNote.imageUrls, 0) },
                contentScale = ContentScale.Crop
            )
        }
        imageNote.imageUrls.size <= 4 -> {
            // 2-4 张图：网格布局
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                items(imageNote.imageUrls) { imageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "图片",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                val index = imageNote.imageUrls.indexOf(imageUrl)
                                onViewImage(imageNote.imageUrls, index)
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        else -> {
            // 多图（最多显示12张，使用固定高度避免无限约束）
            val displayImages = imageNote.imageUrls.take(12)
            val rows = kotlin.math.ceil(displayImages.size / 3.0).toInt()
            val gridHeight = (rows * 120 + (rows - 1) * 4).dp // 每行120dp + 间距4dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight) // 设置固定高度避免无限约束
                    .clip(RoundedCornerShape(12.dp)),
                userScrollEnabled = false // 禁用内部滚动，使用外部滚动
            ) {
                items(displayImages) { imageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "图片",
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                val index = imageNote.imageUrls.indexOf(imageUrl)
                                onViewImage(imageNote.imageUrls, index)
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // 如果图片超过12张，显示提示
            if (imageNote.imageUrls.size > 12) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "还有 ${imageNote.imageUrls.size - 12} 张图片未显示，点击图片查看全部",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }

    // 图片数量标签
    if (imageNote.imageUrls.size > 1) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = imageNote.getImageCountDescription(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 视频信息区域
 */
@Composable
private fun VideoInfoSection(video: ParsedMedia.Video) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 🎯 新增：编码技术信息（如果有数据）
            if (!video.codecType.isNullOrBlank() || video.fps > 0 || !video.qualityTag.isNullOrBlank()) {
                // 技术信息标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📹 编码信息",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // 视频来源标签
                    if (!video.videoSource.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = video.getSourceDescription(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 编码格式和帧率
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (!video.codecType.isNullOrBlank()) {
                        InfoItem(label = "编码格式", value = video.codecType)
                    }
                    if (video.fps > 0) {
                        InfoItem(label = "帧率", value = "${video.fps} fps")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分隔线
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 基本信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "清晰度", value = video.qualityTag ?: video.getQualityDescription())
                InfoItem(label = "时长", value = video.getFormattedDuration())
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(label = "分辨率", value = video.getResolutionDescription())
                InfoItem(label = "大小", value = video.getReadableFileSize())
            }

            if (video.bitrate > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(label = "码率", value = video.getReadableBitrate())
                    if (video.fps == 0) {
                        // 如果没有实际 FPS 数据,显示估算值
                        InfoItem(label = "帧率(估算)", value = video.getEstimatedFPS())
                    }
                }
            }
        }
    }
}

/**
 * 图片信息区域
 */
@Composable
private fun ImageInfoSection(imageNote: ParsedMedia.ImageNote) {
    val firstImageInfo = imageNote.getFirstImageInfo()
    val totalSize = imageNote.getTotalImageSize()

    if (firstImageInfo != null || imageNote.imageUrls.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(label = "图片数量", value = "${imageNote.imageUrls.size}张")
                    if (firstImageInfo != null) {
                        InfoItem(label = "首图", value = firstImageInfo)
                    }
                }

                if (imageNote.imageSizes != null && imageNote.imageSizes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoItem(label = "总大小", value = totalSize)
                }
            }
        }
    }
}

/**
 * 信息项组件
 */
@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 标题区域（可复制、可选择）
 */
@Composable
private fun TitleSection(title: String, stats: com.tikhub.videoparser.data.model.StatsInfo) {
    val context = LocalContext.current

    Column {
        // 标题（可选择文字、可一键复制）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // 使用 SelectionContainer 让文字可以被选择复制
            SelectionContainer(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 一键复制按钮
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("标题", title)
                    )
                    android.widget.Toast.makeText(context, "标题已复制", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制标题",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 统计信息
        Text(
            text = stats.getFormattedStats(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 解析信息区域
 */
@Composable
private fun ParseInfoSection(
    parseResultWrapper: com.tikhub.videoparser.data.model.ParseResultWrapper,
    platform: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📊 解析信息",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                // 性能等级标签
                val perfLevel = parseResultWrapper.getPerformanceLevel()
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${perfLevel.emoji} ${perfLevel.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 解析详情
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 耗时
                InfoChip(
                    icon = "⏱️",
                    label = "耗时",
                    value = parseResultWrapper.getTimeDisplay()
                )
                // 费用
                InfoChip(
                    icon = "💰",
                    label = "费用",
                    value = parseResultWrapper.getCostDisplay()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 接口信息
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔗",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "接口: /api/hybrid/${platform}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 信息芯片组件
 */
@Composable
private fun InfoChip(icon: String, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 操作按钮区域
 */
@Composable
private fun ActionButtons(
    media: ParsedMedia,
    onDownload: () -> Unit,
    onTranscode: (String) -> Unit,
    downloadState: com.tikhub.videoparser.download.DownloadState,
    downloadedFilePath: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 下载进度条（在按钮上方显示）
        if (downloadState is com.tikhub.videoparser.download.DownloadState.Downloading) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { downloadState.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "下载中 ${downloadState.progress}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 下载按钮
        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = downloadState !is com.tikhub.videoparser.download.DownloadState.Downloading
        ) {
            when (downloadState) {
                is com.tikhub.videoparser.download.DownloadState.Downloading -> {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载中...")
                }
                is com.tikhub.videoparser.download.DownloadState.Success -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${downloadState.getSuccessMessage()} - 再次下载")
                }
                is com.tikhub.videoparser.download.DownloadState.Failed -> {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("下载失败 - 重试")
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (media) {
                            is ParsedMedia.Video -> "下载视频"
                            is ParsedMedia.ImageNote -> "保存图片 (${media.imageUrls.size})"
                        }
                    )
                }
            }
        }

        // 🎯 转码按钮（仅对 ByteVC2 视频显示）
        if (media is ParsedMedia.Video &&
            media.codecType == "ByteVC2" &&
            downloadedFilePath != null &&
            downloadState is com.tikhub.videoparser.download.DownloadState.Success) {

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onTranscode(downloadedFilePath) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Transform,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("转码为 H.264 (兼容格式)")
            }

            // 转码提示信息
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ ByteVC2 编码可能无法在部分设备播放，建议转码",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        // 下载状态消息
        when (downloadState) {
            is com.tikhub.videoparser.download.DownloadState.Success -> {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已保存到: ${downloadState.filePath}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            is com.tikhub.videoparser.download.DownloadState.Failed -> {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "错误: ${downloadState.error}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            else -> {}
        }
    }
}

// ========================================
// 预览
// ========================================

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PreviewVideoCard() {
    MaterialTheme {
        MediaResultCard(
            media = ParsedMedia.Video(
                id = "123456",
                platform = "douyin",
                authorName = "测试作者",
                authorAvatar = "",
                title = "这是一个测试视频的标题，用于展示布局效果",
                coverUrl = "",
                stats = com.tikhub.videoparser.data.model.StatsInfo(
                    likeCount = 12345,
                    commentCount = 678,
                    playCount = 987654
                ),
                videoUrl = "",
                duration = 125,
                width = 1080,
                height = 1920
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PreviewImageNoteCard() {
    MaterialTheme {
        MediaResultCard(
            media = ParsedMedia.ImageNote(
                id = "123456",
                platform = "xiaohongshu",
                authorName = "测试作者",
                authorAvatar = "",
                title = "这是一个测试图文笔记的标题，展示多图布局效果",
                coverUrl = "",
                stats = com.tikhub.videoparser.data.model.StatsInfo(
                    likeCount = 5678,
                    commentCount = 234,
                    collectCount = 890
                ),
                imageUrls = List(9) { "" }
            )
        )
    }
}
