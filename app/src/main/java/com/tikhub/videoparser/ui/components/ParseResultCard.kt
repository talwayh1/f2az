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
import java.util.Locale

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
            AuthorSection(
                avatar = result.authorAvatar,
                nickname = result.authorName,
                platform = result.platform
            )

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // 标题/描述（完整显示，支持复制）
            TitleAndDescriptionSection(
                title = result.title
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 统计数据
            StatisticsSection(
                likes = result.stats.likeCount,
                comments = result.stats.commentCount,
                shares = result.stats.shareCount,
                plays = result.stats.playCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 视频信息或图片列表
            when (result) {
                is ParsedMedia.Video -> {
                    VideoSection(
                        video = result,
                        downloadState = downloadState,
                        onDownloadClick = { onDownloadVideo(result.videoUrl) }
                    )
                }
                is ParsedMedia.ImageNote -> {
                    ImageGallerySection(
                        imageNote = result,
                        downloadState = downloadState,
                        onDownloadAllClick = { onDownloadAllImages(result.imageUrls) }
                    )
                }
            }
        }
    }
}

/**
 * 作者信息区域
 */
@Composable
fun AuthorSection(
    avatar: String,
    nickname: String,
    platform: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        AsyncImage(
            model = avatar,
            contentDescription = "作者头像",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = platform,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 标题和描述区域
 */
@Composable
fun TitleAndDescriptionSection(
    title: String
) {
    val clipboardManager = LocalClipboardManager.current

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.clickable {
                clipboardManager.setText(AnnotatedString(title))
            }
        )
    }
}

/**
 * 统计数据区域
 */
@Composable
fun StatisticsSection(
    likes: Long,
    comments: Long,
    shares: Long,
    plays: Long = 0
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (likes > 0) {
            StatItem(icon = Icons.Default.Favorite, count = likes)
        }
        if (comments > 0) {
            StatItem(icon = Icons.Default.Comment, count = comments)
        }
        if (shares > 0) {
            StatItem(icon = Icons.Default.Share, count = shares)
        }
        if (plays > 0) {
            StatItem(icon = Icons.Default.PlayArrow, count = plays)
        }
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun formatCount(count: Long): String {
    return when {
        count < 1000 -> count.toString()
        count < 10000 -> String.format(Locale.US, "%.1fk", count / 1000.0)
        count < 100000000 -> String.format(Locale.US, "%.1fw", count / 10000.0)
        else -> String.format(Locale.US, "%.1f亿", count / 100000000.0)
    }
}

/**
 * 视频信息区域
 */
@Composable
fun VideoSection(
    video: ParsedMedia.Video,
    downloadState: DownloadState,
    onDownloadClick: () -> Unit
) {
    Column {
        // 封面图
        AsyncImage(
            model = video.coverUrl,
            contentDescription = "视频封面",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 视频信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = video.getFormattedDuration(),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = video.getReadableFileSize(),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 下载按钮
        Button(
            onClick = onDownloadClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = downloadState !is DownloadState.Downloading
        ) {
            when (downloadState) {
                is DownloadState.Downloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载中 ${downloadState.progress}%")
                }
                is DownloadState.Success -> {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载完成")
                }
                is DownloadState.Failed -> {
                    Icon(Icons.Default.Error, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载失败")
                }
                else -> {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载视频")
                }
            }
        }
    }
}

/**
 * 图片画廊区域
 */
@Composable
fun ImageGallerySection(
    imageNote: ParsedMedia.ImageNote,
    downloadState: DownloadState,
    onDownloadAllClick: () -> Unit
) {
    Column {
        Text(
            text = imageNote.getImageCountDescription(),
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 图片网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(imageNote.imageUrls) { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "图片",
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 下载按钮
        Button(
            onClick = onDownloadAllClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = downloadState !is DownloadState.Downloading
        ) {
            when (downloadState) {
                is DownloadState.Downloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载中 ${downloadState.progress}%")
                }
                is DownloadState.Success -> {
                    Icon(Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载完成")
                }
                is DownloadState.Failed -> {
                    Icon(Icons.Default.Error, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下载失败")
                }
                else -> {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存所有图片 (${imageNote.imageUrls.size})")
                }
            }
        }
    }
}
