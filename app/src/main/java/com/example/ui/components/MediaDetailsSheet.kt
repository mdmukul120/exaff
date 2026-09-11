package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.LayoutInflater
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.api.ApiService
import com.example.data.model.EpisodeItem
import com.example.data.model.LiveChannel
import com.example.data.model.MediaItem
import com.example.data.repository.DownloadHelper
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsSheet(
    media: MediaItem,
    relatedMedia: List<MediaItem> = emptyList(),
    relatedChannels: List<LiveChannel> = emptyList(),
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onPlayMedia: (title: String, streamUrl: String) -> Unit,
    onChannelClick: (LiveChannel) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isExtracting by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf(media) }
    var episodes by remember { mutableStateOf(media.episodes) }
    var selectedQualityIndex by remember { mutableIntStateOf(0) }

    // In-popup inline player state
    var activeStreamUrl by remember { mutableStateOf(media.streamUrl) }
    var activeTitle by remember { mutableStateOf(media.title) }
    var isInlinePlaying by remember { mutableStateOf(media.streamUrl.isNotEmpty()) }

    // If item is from extractor and hasn't fetched info yet, fetch it
    LaunchedEffect(media) {
        currentItem = media
        episodes = media.episodes
        if (media.streamUrl.isNotEmpty()) {
            activeStreamUrl = media.streamUrl
            activeTitle = media.title
            isInlinePlaying = true
        }

        if (media.provider.isNotEmpty() && media.link.isNotEmpty() && media.qualities.isEmpty()) {
            isExtracting = true
            try {
                val info = ApiService.fetchExtractorInfo(media.provider, media.link)
                if (info != null) {
                    currentItem = currentItem.copy(
                        title = if (info.title.isNotEmpty()) info.title else currentItem.title,
                        description = if (info.synopsis.isNotEmpty()) info.synopsis else currentItem.description,
                        posterUrl = if (info.image.isNotEmpty()) info.image else currentItem.posterUrl,
                        qualities = info.qualities
                    )
                    if (info.episodesLink.isNotEmpty()) {
                        val epList = ApiService.fetchExtractorEpisodes(media.provider, info.episodesLink)
                        episodes = epList
                    }
                }
            } catch (e: Exception) {
                // Ignore failure gracefully
            } finally {
                isExtracting = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF11121C),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("media_details_popup_sheet")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // TOP SECTION: Video Player (or Backdrop with Play Hero)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        if (isInlinePlaying && activeStreamUrl.isNotEmpty()) {
                            InlinePopupPlayer(
                                streamUrl = activeStreamUrl,
                                title = activeTitle,
                                isLive = false,
                                onFullscreen = {
                                    onPlayMedia(activeTitle, activeStreamUrl)
                                    onDismiss()
                                }
                            )
                        } else {
                            // Video Thumbnail / Poster with big Play icon
                            AsyncImage(
                                model = currentItem.backdropUrl.ifEmpty { currentItem.posterUrl },
                                contentDescription = currentItem.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x55000000),
                                                Color(0x33000000),
                                                Color(0xEE11121C)
                                            )
                                        )
                                    )
                            )

                            // Big Center Play Button
                            Surface(
                                shape = CircleShape,
                                color = MukulRedPrimary,
                                shadowElevation = 10.dp,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(60.dp)
                                    .clickable {
                                        if (currentItem.streamUrl.isNotBlank()) {
                                            activeStreamUrl = currentItem.streamUrl.trim()
                                            activeTitle = currentItem.title
                                            isInlinePlaying = true
                                        } else {
                                            coroutineScope.launch {
                                                isExtracting = true
                                                val direct = currentItem.qualities.getOrNull(selectedQualityIndex)?.directLink
                                                    ?: currentItem.link
                                                val servers = ApiService.extractStreamServers(currentItem.provider, direct)
                                                isExtracting = false
                                                val streamLink = servers.firstOrNull { it.server.contains("CF Worker", true) }?.link
                                                    ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }?.link
                                                    ?: servers.firstOrNull()?.link

                                                if (!streamLink.isNullOrBlank()) {
                                                    activeStreamUrl = streamLink.trim()
                                                    activeTitle = currentItem.title
                                                    isInlinePlaying = true
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        if (isBangla) "স্ট্রিম সার্ভার পাওয়া যায়নি" else "Stream server unavailable",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isExtracting) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Close Button (Top Right)
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // DETAILS SECTION
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = currentItem.title,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 25.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Metadata Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentItem.rating,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(text = "•", color = MukulTextSecondary)
                            Text(text = currentItem.year, color = MukulTextSecondary, fontSize = 12.sp)
                            Text(text = "•", color = MukulTextSecondary)
                            Text(text = currentItem.duration, color = MukulTextSecondary, fontSize = 12.sp)
                            Text(text = "•", color = MukulTextSecondary)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MukulRedPrimary.copy(alpha = 0.25f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, MukulRedPrimary)
                            ) {
                                Text(
                                    text = currentItem.label.ifEmpty { "HD" },
                                    color = MukulRedPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Synopsis
                        if (currentItem.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentItem.description,
                                color = MukulTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Quality selector chips if available
                        if (currentItem.qualities.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isBangla) "কোয়ালিটি নির্বাচন:" else "Select Quality:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentItem.qualities.forEachIndexed { idx, q ->
                                    val isSelected = selectedQualityIndex == idx
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MukulRedPrimary else Color(0xFF222436),
                                        modifier = Modifier.clickable { selectedQualityIndex = idx }
                                    ) {
                                        Text(
                                            text = q.quality.ifEmpty { "HD" },
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Fullscreen Watch & Download
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (activeStreamUrl.isNotBlank()) {
                                        onPlayMedia(activeTitle, activeStreamUrl.trim())
                                        onDismiss()
                                    } else if (currentItem.streamUrl.isNotBlank()) {
                                        onPlayMedia(currentItem.title, currentItem.streamUrl.trim())
                                        onDismiss()
                                    } else {
                                        coroutineScope.launch {
                                            isExtracting = true
                                            val direct = currentItem.qualities.getOrNull(selectedQualityIndex)?.directLink
                                                ?: currentItem.link
                                            val servers = ApiService.extractStreamServers(currentItem.provider, direct)
                                            isExtracting = false
                                            val streamLink = servers.firstOrNull { it.server.contains("CF Worker", true) }?.link
                                                ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }?.link
                                                ?: servers.firstOrNull()?.link

                                            if (!streamLink.isNullOrBlank()) {
                                                onPlayMedia(currentItem.title, streamLink.trim())
                                                onDismiss()
                                            } else {
                                                Toast.makeText(context, if (isBangla) "স্ট্রিম সার্ভার পাওয়া যায়নি" else "Stream server unavailable", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("popup_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "ফুলস্ক্রিন দেখুন" else "Fullscreen Player",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val poster = currentItem.posterUrl.ifEmpty { media.posterUrl }
                                        if (currentItem.downloadUrl.isNotEmpty()) {
                                            DownloadHelper.downloadMovie(context, currentItem.title, currentItem.downloadUrl, poster)
                                        } else if (currentItem.streamUrl.isNotEmpty()) {
                                            DownloadHelper.downloadMovie(context, currentItem.title, currentItem.streamUrl, poster)
                                        } else if (activeStreamUrl.isNotEmpty()) {
                                            DownloadHelper.downloadMovie(context, currentItem.title, activeStreamUrl, poster)
                                        } else if (currentItem.provider.isNotEmpty()) {
                                            isExtracting = true
                                            val direct = currentItem.qualities.getOrNull(selectedQualityIndex)?.directLink
                                                ?: currentItem.link
                                            val servers = ApiService.extractStreamServers(currentItem.provider, direct)
                                            isExtracting = false
                                            if (servers.isNotEmpty()) {
                                                val downloadLink = servers.firstOrNull { it.server.contains("GDrive", true) }?.link
                                                    ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }?.link
                                                    ?: servers[0].link
                                                DownloadHelper.downloadMovie(context, currentItem.title, downloadLink, poster)
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MukulRedPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("popup_download_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = MukulRedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "ডাউনলোড" else "Download",
                                    color = MukulRedPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // EPISODES SECTION IF SERIES
                if (episodes.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (isBangla) "পর্বসমূহ (${episodes.size})" else "Episodes (${episodes.size})",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    items(episodes) { ep ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1B1D2C),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable {
                                    if (ep.streamUrl.isNotBlank()) {
                                        activeStreamUrl = ep.streamUrl.trim()
                                        activeTitle = "${currentItem.title} - ${ep.title}"
                                        isInlinePlaying = true
                                    } else if (ep.provider.isNotEmpty() && ep.link.isNotEmpty()) {
                                        coroutineScope.launch {
                                            isExtracting = true
                                            val servers = ApiService.extractStreamServers(ep.provider, ep.link)
                                            isExtracting = false
                                            val streamLink = servers.firstOrNull { it.server.contains("CF Worker", true) }?.link
                                                ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }?.link
                                                ?: servers.firstOrNull()?.link
                                            if (!streamLink.isNullOrBlank()) {
                                                activeStreamUrl = streamLink.trim()
                                                activeTitle = "${currentItem.title} - ${ep.title}"
                                                isInlinePlaying = true
                                            } else {
                                                Toast.makeText(context, if (isBangla) "এপিসোডের স্ট্রিম পাওয়া যায়নি" else "Episode stream unavailable", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MukulRedPrimary,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (ep.duration.isNotEmpty()) {
                                        Text(
                                            text = ep.duration,
                                            color = MukulTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // RELATED MOVIES / DRAMAS BELOW
                val filteredRelated = relatedMedia.filter { it.id != currentItem.id }.take(10)
                if (filteredRelated.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = if (isBangla) "সম্পর্কিত বিনোদন ও সিনেমা" else "Related Movies & Shows",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredRelated, key = { it.id }) { related ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1B1D2C),
                                        modifier = Modifier
                                            .width(110.dp)
                                            .clickable {
                                                currentItem = related
                                                episodes = related.episodes
                                                if (related.streamUrl.isNotBlank()) {
                                                    activeStreamUrl = related.streamUrl.trim()
                                                    activeTitle = related.title
                                                    isInlinePlaying = true
                                                } else {
                                                    isInlinePlaying = false
                                                    activeStreamUrl = ""
                                                }
                                            }
                                    ) {
                                        Column {
                                            AsyncImage(
                                                model = related.posterUrl,
                                                contentDescription = related.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                            )
                                            Text(
                                                text = related.title,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RELATED LIVE TV CHANNELS BELOW
                if (relatedChannels.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = MukulRedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "সম্পর্কিত লাইভ টিভি চ্যানেল" else "Related Live TV Channels",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(relatedChannels.take(8), key = { it.id }) { ch ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1B1D2C),
                                        modifier = Modifier
                                            .width(130.dp)
                                            .clickable {
                                                if (ch.streamUrl.isNotBlank()) {
                                                    activeStreamUrl = ch.streamUrl.trim()
                                                    activeTitle = ch.name
                                                    isInlinePlaying = true
                                                }
                                            }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            AsyncImage(
                                                model = ch.logoUrl,
                                                contentDescription = ch.name,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = ch.name,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = ch.group,
                                                color = MukulTextSecondary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight inline video player for top of the bottom sheet popup
 */
@Composable
private fun InlinePopupPlayer(
    streamUrl: String,
    title: String,
    isLive: Boolean,
    onFullscreen: () -> Unit
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val exoPlayer = remember(streamUrl) {
        val referer = when {
            streamUrl.contains("workers.dev", ignoreCase = true) -> "https://bongobd.com/"
            streamUrl.contains("bongobd", ignoreCase = true) || streamUrl.contains("bongo", ignoreCase = true) -> "https://bongobd.com/"
            streamUrl.contains("bioscope", ignoreCase = true) -> "https://www.bioscopeplus.com/"
            else -> "https://www.bioscopeplus.com/"
        }
        val origin = when {
            streamUrl.contains("workers.dev", ignoreCase = true) -> "https://bongobd.com"
            streamUrl.contains("bongobd", ignoreCase = true) || streamUrl.contains("bongo", ignoreCase = true) -> "https://bongobd.com"
            streamUrl.contains("bioscope", ignoreCase = true) -> "https://www.bioscopeplus.com"
            else -> "https://www.bioscopeplus.com"
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to referer,
                    "Origin" to origin,
                    "Accept" to "*/*"
                )
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(25000)
            .setReadTimeoutMs(30000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                val cleanUrl = streamUrl.trim()
                if (cleanUrl.isNotEmpty() && (cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true) || cleanUrl.startsWith("content://", ignoreCase = true) || cleanUrl.startsWith("file://", ignoreCase = true) || cleanUrl.startsWith("rtmp://", ignoreCase = true) || cleanUrl.startsWith("rtsp://", ignoreCase = true))) {
                    val isHls = cleanUrl.contains(".m3u8", ignoreCase = true) ||
                            cleanUrl.contains("/bongo/hls", ignoreCase = true) ||
                            cleanUrl.contains("/proxy/hls", ignoreCase = true) ||
                            cleanUrl.contains("manifest", ignoreCase = true) ||
                            cleanUrl.contains("playlist", ignoreCase = true) ||
                            cleanUrl.contains("gpcdn", ignoreCase = true) ||
                            isLive

                    val mediaItem = ExoMediaItem.Builder()
                        .setUri(Uri.parse(cleanUrl))
                        .apply {
                            if (isHls) {
                                setMimeType(MimeTypes.APPLICATION_M3U8)
                            } else if (cleanUrl.contains(".mpd", ignoreCase = true)) {
                                setMimeType(MimeTypes.APPLICATION_MPD)
                            }
                        }
                        .build()
                    setMediaItem(mediaItem)
                    prepare()
                } else {
                    isBuffering = false
                    hasError = true
                }
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    hasError = false
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isBuffering = false
                hasError = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx).inflate(com.example.R.layout.view_exo_popup_player, null, false) as PlayerView
                view.apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && !hasError) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
            ) {
                CircularProgressIndicator(
                    color = MukulRedPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        if (hasError) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0D0E15))
            ) {
                Text(
                    text = "Stream unavailable",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }
}
