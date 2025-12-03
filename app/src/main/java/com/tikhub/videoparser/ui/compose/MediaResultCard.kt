package com.tikhub.videoparser.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 *
 * @param media ParsedMedia 数据（Video 或 ImageNote）
 * @param onPlayVideo 点击播放视频的回调
 * @param onViewImage 点击查看图片的回调
 * @param onDownload 点击下载的回调
 */
@Composable
fun MediaResultCard(
    media: ParsedMedia,
    onPlayVideo: (String) -> Unit = {},
    onViewImage: (List<String>, Int) -> Unit = { _, _ -> },
    onDownload: () -> Unit = {},
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
                is ParsedMedia.Video -> VideoContent(
                    video = media,
                    onPlay = onPlayVideo
                )
                is ParsedMedia.ImageNote -> ImageNoteContent(
                    imageNote = media,
                    onViewImage = onViewImage
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. 标题和统计信息
            Text(
                text = media.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = media.stats.getFormattedStats(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. 操作按钮
            ActionButtons(
                media = media,
                onDownload = onDownload
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

        // 平台图标
        val platformEnum = Platform.values().find { it.apiParam == platform }
        if (platformEnum != null) {
            Icon(
                painter = painterResource(id = platformEnum.iconRes),
                contentDescription = platformEnum.displayName,
                tint = Color(platformEnum.themeColor),
                modifier = Modifier.size(28.dp)
            )
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
 * 图文内容展示
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
                items(imageNote.imageUrls.take(4)) { imageUrl ->
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
            // 多图（九宫格）
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                items(imageNote.imageUrls.take(9)) { imageUrl ->
                    Box {
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

                        // 显示剩余图片数量
                        if (imageNote.imageUrls.indexOf(imageUrl) == 8 && imageNote.imageUrls.size > 9) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${imageNote.imageUrls.size - 9}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
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
 * 操作按钮区域
 */
@Composable
private fun ActionButtons(
    media: ParsedMedia,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 下载按钮
        Button(
            onClick = onDownload,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
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

        // 分享按钮
        OutlinedButton(
            onClick = { /* TODO: 实现分享功能 */ },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "分享")
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
