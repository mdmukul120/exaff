package com.example.ui.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Rational
import android.view.LayoutInflater
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoFile
import com.example.R
import com.example.data.model.MediaItem as MukulMediaItem
import com.example.data.model.LiveChannel
import com.example.data.model.DownloadedMedia
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulCardBg
import com.example.ui.theme.MukulCardBorder
import com.example.ui.theme.MukulGold
import com.example.ui.theme.MukulRedGlowing
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary
import kotlinx.coroutines.delay

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    title: String,
    streamUrl: String,
    isLive: Boolean = false,
    subtitle: String = "",
    mediaType: String = if (isLive) "channel" else "movie",
    customHeaders: Map<String, String> = emptyMap(),
    relatedMovies: List<MukulMediaItem> = emptyList(),
    relatedChannels: List<LiveChannel> = emptyList(),
    offlineFiles: List<DownloadedMedia> = emptyList(),
    isInPip: Boolean = false,
    onSelectMovie: ((MukulMediaItem) -> Unit)? = null,
    onSelectChannel: ((LiveChannel) -> Unit)? = null,
    onSelectOfflineMedia: ((DownloadedMedia) -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isFullscreen by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var currentSpeed by remember { mutableFloatStateOf(1.0f) }
    var isLocked by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showAspectDialog by remember { mutableStateOf(false) }

    var forceWebPlayer by remember(streamUrl) { mutableStateOf(false) }

    val isWebEmbed = remember(streamUrl) {
        val lower = streamUrl.lowercase()
        lower.contains("decimalsports.com") ||
        lower.contains("ok.ru") ||
        lower.contains("soccerfull") ||
        lower.contains("cricketlounge") ||
        lower.contains("dailymotion.com/embed") ||
        lower.contains("<iframe")
    }

    val activeUseWebPlayer = isWebEmbed || forceWebPlayer

    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60 * 1000L)
            Toast.makeText(context, "স্লিপ টাইমার শেষ হয়েছে। প্লেব্যাক বন্ধ করা হয়েছে।", Toast.LENGTH_LONG).show()
            sleepTimerMinutes = 0
        }
    }

    val onEnterPip = {
        val act = context.findActivity()
        if (act == null) {
            Toast.makeText(context, "PiP মোড চালু করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hasPip = context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                if (!hasPip) {
                    Toast.makeText(context, "এই ডিভাইসে PiP মোড সমর্থিত নয়", Toast.LENGTH_SHORT).show()
                } else {
                    val aspectRatio = Rational(16, 9)
                    val paramsBuilder = PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        paramsBuilder.setAutoEnterEnabled(true)
                        paramsBuilder.setSeamlessResizeEnabled(true)
                    }
                    val entered = act.enterPictureInPictureMode(paramsBuilder.build())
                    if (!entered) {
                        Toast.makeText(context, "PiP মোড শুরু করা যায়নি", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Throwable) {
                try {
                    act.enterPictureInPictureMode()
                } catch (ex: Throwable) {
                    Toast.makeText(context, "PiP শুরু করার সময় সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "PiP মোড Android 8.0 বা তদূর্ধ্ব ভার্সনে কাজ করে", Toast.LENGTH_SHORT).show()
        }
    }

    // Setup ExoPlayer with resilient referrer & CORS headers
    val exoPlayer = remember(streamUrl, customHeaders) {
        var cleanUrl = streamUrl.trim()
        val requestHeaders = mutableMapOf<String, String>()

        if (cleanUrl.contains("|")) {
            val parts = cleanUrl.split("|")
            cleanUrl = parts[0].trim()
            for (p in parts.drop(1)) {
                if (p.contains("=")) {
                    val kv = p.split("=", limit = 2)
                    requestHeaders[kv[0].trim()] = kv[1].trim()
                }
            }
        }

        customHeaders.forEach { (k, v) ->
            if (v.isNotEmpty()) requestHeaders[k] = v
        }

        val referer = requestHeaders["Referer"] ?: when {
            cleanUrl.contains("workers.dev", ignoreCase = true) -> "https://bongobd.com/"
            cleanUrl.contains("bongobd", ignoreCase = true) || cleanUrl.contains("bongo", ignoreCase = true) -> "https://bongobd.com/"
            cleanUrl.contains("bioscope", ignoreCase = true) -> "https://www.bioscopeplus.com/"
            cleanUrl.contains("tapmad", ignoreCase = true) -> "https://www.tapmad.com/"
            cleanUrl.contains("fancode", ignoreCase = true) -> "https://www.fancode.com/"
            cleanUrl.contains("freelivesports", ignoreCase = true) || cleanUrl.contains("powr.tv", ignoreCase = true) -> "https://freelivesports.tv/"
            else -> null
        }
        val origin = requestHeaders["Origin"] ?: when {
            cleanUrl.contains("workers.dev", ignoreCase = true) -> "https://bongobd.com"
            cleanUrl.contains("bongobd", ignoreCase = true) || cleanUrl.contains("bongo", ignoreCase = true) -> "https://bongobd.com"
            cleanUrl.contains("bioscope", ignoreCase = true) -> "https://www.bioscopeplus.com"
            cleanUrl.contains("tapmad", ignoreCase = true) -> "https://www.tapmad.com"
            cleanUrl.contains("fancode", ignoreCase = true) -> "https://www.fancode.com"
            cleanUrl.contains("freelivesports", ignoreCase = true) || cleanUrl.contains("powr.tv", ignoreCase = true) -> "https://freelivesports.tv"
            else -> null
        }

        if (referer != null) {
            requestHeaders["Referer"] = referer
        }
        if (origin != null) {
            requestHeaders["Origin"] = origin
        }
        if (!requestHeaders.containsKey("Accept")) {
            requestHeaders["Accept"] = "*/*"
        }

        val userAgent = requestHeaders.remove("User-Agent")
            ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(requestHeaders)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(25000)
            .setReadTimeoutMs(30000)

        val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(defaultDataSourceFactory)

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
                val isLocalPath = cleanUrl.startsWith("/") || cleanUrl.startsWith("file://", ignoreCase = true)
                val isHttp = cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true)
                val isContent = cleanUrl.startsWith("content://", ignoreCase = true)
                val isRtmp = cleanUrl.startsWith("rtmp://", ignoreCase = true) || cleanUrl.startsWith("rtsp://", ignoreCase = true)

                if (cleanUrl.isNotEmpty() && (isLocalPath || isHttp || isContent || isRtmp)) {
                    val isHls = cleanUrl.contains(".m3u8", ignoreCase = true) ||
                            cleanUrl.contains("/bongo/hls", ignoreCase = true) ||
                            cleanUrl.contains("/proxy/hls", ignoreCase = true) ||
                            cleanUrl.contains("manifest", ignoreCase = true) ||
                            cleanUrl.contains("playlist", ignoreCase = true) ||
                            cleanUrl.contains("gpcdn", ignoreCase = true) ||
                            isLive

                    val mediaUri = when {
                        cleanUrl.startsWith("/") -> Uri.fromFile(java.io.File(cleanUrl))
                        else -> Uri.parse(cleanUrl)
                    }

                    val mediaItem = MediaItem.Builder()
                        .setUri(mediaUri)
                        .apply {
                            if (isHls) {
                                setMimeType(MimeTypes.APPLICATION_M3U8)
                            } else if (cleanUrl.contains(".mpd", ignoreCase = true)) {
                                setMimeType(MimeTypes.APPLICATION_MPD)
                            } else if (cleanUrl.endsWith(".mp4", ignoreCase = true)) {
                                setMimeType(MimeTypes.VIDEO_MP4)
                            } else if (cleanUrl.endsWith(".mkv", ignoreCase = true)) {
                                setMimeType(MimeTypes.VIDEO_MATROSKA)
                            } else if (cleanUrl.endsWith(".webm", ignoreCase = true)) {
                                setMimeType(MimeTypes.VIDEO_WEBM)
                            }
                        }
                        .build()
                    setMediaItem(mediaItem)
                    prepare()
                } else {
                    hasError = true
                    isBuffering = false
                }
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    hasError = false
                    totalDuration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Position tracker coroutine
    LaunchedEffect(exoPlayer, isDraggingSlider) {
        while (true) {
            if (!isDraggingSlider && exoPlayer.isPlaying) {
                currentPosition = exoPlayer.currentPosition
                if (totalDuration > 0) {
                    sliderPosition = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                }
            }
            delay(500)
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    if (showSpeedDialog) {
        SpeedDialog(
            currentSpeed = currentSpeed,
            onSelectSpeed = {
                currentSpeed = it
                exoPlayer.playbackParameters = PlaybackParameters(it)
                Toast.makeText(context, "স্পিড সেট হয়েছে: ${it}x", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showTimerDialog) {
        SleepTimerDialog(
            currentMinutes = sleepTimerMinutes,
            onSelectMinutes = {
                sleepTimerMinutes = it
                if (it > 0) {
                    Toast.makeText(context, "টাইমার সেট হয়েছে: $it মিনিট", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "টাইমার বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showTimerDialog = false }
        )
    }

    if (showAspectDialog) {
        AspectRatioDialog(
            currentMode = resizeMode,
            onSelectMode = { resizeMode = it },
            onDismiss = { showAspectDialog = false }
        )
    }

    if (isInPip) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (activeUseWebPlayer) {
                WebEmbedPlayerView(url = streamUrl)
            } else {
                PlayerSurface(exoPlayer = exoPlayer, resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT)
            }
        }
        return
    }

    if (isFullscreen) {
        // FULLSCREEN IMMERSIVE MODE
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    controlsVisible = !controlsVisible
                }
                .testTag("video_player_fullscreen_container")
        ) {
            if (activeUseWebPlayer) {
                WebEmbedPlayerView(url = streamUrl)
            } else {
                PlayerSurface(exoPlayer = exoPlayer, resizeMode = resizeMode)
            }

            if (isBuffering && !activeUseWebPlayer) {
                CircularProgressIndicator(
                    color = MukulRedPrimary,
                    modifier = Modifier.size(48.dp).align(Alignment.Center)
                )
            }

            if (hasError && !activeUseWebPlayer) {
                PlayerErrorView(
                    onRetry = {
                        hasError = false
                        isBuffering = true
                        exoPlayer.prepare()
                        exoPlayer.play()
                    },
                    onPlayInWebEngine = {
                        hasError = false
                        forceWebPlayer = true
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (isLocked) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xDD151724),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MukulRedGlowing),
                        modifier = Modifier.clickable {
                            isLocked = false
                            Toast.makeText(context, "স্ক্রিন আনলক হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Unlock",
                                tint = MukulRedGlowing,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "পর্দা আনলক",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Controls Overlay
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    FullscreenControlsOverlay(
                        title = title,
                        subtitle = subtitle,
                        isLive = isLive,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        totalDuration = totalDuration,
                        sliderPosition = sliderPosition,
                        isDraggingSlider = isDraggingSlider,
                        isMuted = isMuted,
                        currentSpeed = currentSpeed,
                        sleepTimerMinutes = sleepTimerMinutes,
                        onSeek = { seekToRatio ->
                            val target = (seekToRatio * totalDuration).toLong()
                            exoPlayer.seekTo(target)
                        },
                        onSliderDrag = { isDragging, pos ->
                            isDraggingSlider = isDragging
                            sliderPosition = pos
                        },
                        onPlayPause = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        onRewind = {
                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(target)
                        },
                        onForward = {
                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(target)
                        },
                        onToggleMute = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                            Toast.makeText(context, if (isMuted) "সাউন্ড মিউট করা হয়েছে" else "সাউন্ড চালু করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        onOpenSpeedDialog = { showSpeedDialog = true },
                        onOpenTimerDialog = { showTimerDialog = true },
                        onOpenAspectDialog = { showAspectDialog = true },
                        onLockScreen = {
                            isLocked = true
                            Toast.makeText(context, "স্ক্রিন লক করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        onEnterPip = { onEnterPip() },
                        onToggleFullscreen = {
                            isFullscreen = false
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        },
                        onDownloadClick = onDownloadClick,
                        onClose = onClose
                    )
                }
            }
        }
    } else {
        // COMPACT POPUP BOTTOM SHEET MODE
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("video_player_compact_container")
        ) {
            // 1. Top Gap translucent scrim: reveals app behind, tap to dismiss popup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )

            // 2. Popup window rising from bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(top = 52.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MukulDarkBg)
            ) {
                // Top drag handle pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF4A4E68))
                    )
                }

                // Video Viewport (16:9)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            controlsVisible = !controlsVisible
                        }
                        .testTag("video_player_compact_viewport")
                ) {
                    if (activeUseWebPlayer) {
                        WebEmbedPlayerView(url = streamUrl)
                    } else {
                        PlayerSurface(exoPlayer = exoPlayer, resizeMode = resizeMode)
                    }

                    if (isBuffering && !activeUseWebPlayer) {
                        CircularProgressIndicator(
                            color = MukulRedPrimary,
                            modifier = Modifier.size(36.dp).align(Alignment.Center)
                        )
                    }

                    if (hasError && !activeUseWebPlayer) {
                        PlayerErrorView(
                            onRetry = {
                                hasError = false
                                isBuffering = true
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            onPlayInWebEngine = {
                                hasError = false
                                forceWebPlayer = true
                            },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // ALL PLAYBACK CONTROLS INSIDE THE VIDEO PLAYER VIEWPORT
                    androidx.compose.animation.AnimatedVisibility(
                        visible = controlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66000000))
                        ) {
                            // Top row inside player: Close, Title, LIVE tag, Aspect, Speed, Fullscreen
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color(0xCC000000), Color.Transparent)
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = onClose,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x77000000))
                                            .testTag("compact_close_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (isLive) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MukulRedPrimary,
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = "● LIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Aspect Ratio Toggle
                                    IconButton(
                                        onClick = {
                                            resizeMode = when (resizeMode) {
                                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            }
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x77000000))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AspectRatio,
                                            contentDescription = "Aspect Ratio",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Speed Selector
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0x77000000),
                                        modifier = Modifier.clickable { showSpeedDialog = true }
                                    ) {
                                        Text(
                                            text = "${currentSpeed}x",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Engine switch (ExoPlayer vs Web Player)
                                    IconButton(
                                        onClick = {
                                            forceWebPlayer = !forceWebPlayer
                                            Toast.makeText(
                                                context,
                                                if (forceWebPlayer) "ওয়েব প্লেয়ার ইঞ্জিন চালু" else "নেটিভ এক্সোপ্লেয়ার চালু",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (forceWebPlayer) Color(0xFF00B0FF) else Color(0x77000000))
                                    ) {
                                        Icon(
                                            imageVector = if (forceWebPlayer) Icons.Default.Language else Icons.Default.SmartDisplay,
                                            contentDescription = "Switch Engine",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Fullscreen button
                                    IconButton(
                                        onClick = {
                                            isFullscreen = true
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x77000000))
                                            .testTag("compact_expand_fullscreen_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = "Fullscreen",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Center Playback Buttons: 10s Rewind, Center Play/Pause, 10s Forward
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                if (!isLive) {
                                    IconButton(
                                        onClick = {
                                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                            exoPlayer.seekTo(target)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x77000000))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay10,
                                            contentDescription = "Rewind 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                    },
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(MukulRedPrimary.copy(alpha = 0.9f))
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                if (!isLive) {
                                    IconButton(
                                        onClick = {
                                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                            exoPlayer.seekTo(target)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x77000000))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Forward10,
                                            contentDescription = "Forward 10s",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom Controls: Time, Scrubber Slider, Mute, PiP
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0xCC000000))
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                if (!isLive && totalDuration > 0) {
                                    Slider(
                                        value = if (isDraggingSlider) sliderPosition else (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f),
                                        onValueChange = {
                                            isDraggingSlider = true
                                            sliderPosition = it
                                        },
                                        onValueChangeFinished = {
                                            isDraggingSlider = false
                                            val target = (sliderPosition * totalDuration).toLong()
                                            exoPlayer.seekTo(target)
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = MukulRedPrimary,
                                            activeTrackColor = MukulRedPrimary,
                                            inactiveTrackColor = Color(0x66FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(18.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                isMuted = !isMuted
                                                exoPlayer.volume = if (isMuted) 0f else 1f
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                                contentDescription = "Mute",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = if (isLive) "🔴 সরাসরি সম্প্রচার" else "${formatTime(currentPosition)} / ${formatTime(totalDuration)}",
                                            color = if (isLive) MukulRedGlowing else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    IconButton(
                                        onClick = { onEnterPip() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureInPicture,
                                            contentDescription = "PiP",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            // Compact Viewport Details & Controller Panel below
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Movie Title & Details
                Text(
                    text = title,
                    color = MukulTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = MukulTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Pills: Download (if non-live) and Sleep Timer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isLive && onDownloadClick != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF222436),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDownloadClick() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = MukulRedGlowing,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ডাউনলোড",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (sleepTimerMinutes > 0) MukulRedPrimary else Color(0xFF222436),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimerDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sleepTimerMinutes > 0) "${sleepTimerMinutes}মি বাকি" else "স্লিপ টাইমার",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Context-specific content strictly separated:
                // When playing a movie: ONLY movies are shown below.
                // When playing a TV channel: ONLY TV channels are shown below.
                // When playing an offline file: ONLY downloaded files are shown below.
                Spacer(modifier = Modifier.height(24.dp))

                if (mediaType == "channel") {
                    PlayerChannelsSection(
                        channels = relatedChannels,
                        currentChannelTitle = title,
                        onSelectChannel = { ch -> onSelectChannel?.invoke(ch) }
                    )
                } else if (mediaType == "offline") {
                    PlayerOfflineFilesSection(
                        files = offlineFiles,
                        currentFileTitle = title,
                        onSelectFile = { file -> onSelectOfflineMedia?.invoke(file) }
                    )
                } else {
                    PlayerMoviesSection(
                        movies = relatedMovies,
                        currentMovieTitle = title,
                        onSelectMovie = { movie -> onSelectMovie?.invoke(movie) }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun PlayerChannelsSection(
    channels: List<LiveChannel>,
    currentChannelTitle: String,
    onSelectChannel: (LiveChannel) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("সকল") }
    val categories = remember(channels) {
        listOf("সকল") + channels.map { it.group.trim() }.filter { it.isNotEmpty() }.distinct().take(6)
    }
    val filteredChannels = remember(channels, selectedCategory) {
        if (selectedCategory == "সকল") channels else channels.filter { it.group.equals(selectedCategory, true) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = MukulRedGlowing,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "লাইভ টিভি চ্যানেলসমূহ (${channels.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MukulRedPrimary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "LIVE TV",
                    color = MukulRedGlowing,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories.size) { idx ->
                val cat = categories[idx]
                val isSelected = cat == selectedCategory
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MukulRedPrimary else Color(0xFF1E202F),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MukulRedGlowing else Color(0xFF2B2E42)),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else MukulTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // List of Channels
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            filteredChannels.forEach { ch ->
                val isCurrent = ch.name.equals(currentChannelTitle, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) MukulRedPrimary.copy(alpha = 0.25f) else Color(0xFF1B1C28),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrent) MukulRedGlowing else Color(0xFF282A3E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectChannel(ch) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF12131A),
                            modifier = Modifier.size(44.dp)
                        ) {
                            if (ch.logoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = ch.logoUrl,
                                    contentDescription = ch.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = null,
                                        tint = MukulRedPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ch.name,
                                color = if (isCurrent) MukulRedGlowing else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ch.group.ifEmpty { "General" },
                                color = MukulTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (isCurrent) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MukulRedPrimary
                            ) {
                                Text(
                                    text = "চলছে",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MukulTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerMoviesSection(
    movies: List<MukulMediaItem>,
    currentMovieTitle: String,
    onSelectMovie: (MukulMediaItem) -> Unit
) {
    val otherMovies = remember(movies, currentMovieTitle) {
        movies.filter { !it.title.equals(currentMovieTitle, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MukulRedGlowing,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "আরও সম্পর্কিত মুভি ও সিনেমা (${otherMovies.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MukulRedPrimary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "MOVIES",
                    color = MukulRedGlowing,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid of 3 items per row
        val chunkedMovies = remember(otherMovies) { otherMovies.chunked(3) }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            chunkedMovies.forEach { rowMovies ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowMovies.forEach { movie ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1B1C28),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282A3E)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectMovie(movie) }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                ) {
                                    AsyncImage(
                                        model = movie.posterUrl.ifEmpty { movie.backdropUrl },
                                        contentDescription = movie.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (movie.rating.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                                            color = Color.Black.copy(alpha = 0.75f),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MukulGold,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = movie.rating,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = movie.title,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    if (rowMovies.size < 3) {
                        repeat(3 - rowMovies.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerOfflineFilesSection(
    files: List<DownloadedMedia>,
    currentFileTitle: String,
    onSelectFile: (DownloadedMedia) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = null,
                    tint = MukulRedGlowing,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mukul plus ডাউনলোড ভিডিও (${files.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MukulRedPrimary.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "OFFLINE",
                    color = MukulRedGlowing,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            files.forEach { file ->
                val isCurrent = file.title.equals(currentFileTitle, ignoreCase = true) ||
                        file.filePath.equals(currentFileTitle, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) MukulRedPrimary.copy(alpha = 0.25f) else Color(0xFF1B1C28),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrent) MukulRedGlowing else Color(0xFF282A3E)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFile(file) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF12131A),
                            modifier = Modifier.size(width = 44.dp, height = 54.dp)
                        ) {
                            if (file.posterUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = file.posterUrl,
                                    contentDescription = file.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.VideoFile,
                                        contentDescription = null,
                                        tint = MukulRedPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.title,
                                color = if (isCurrent) MukulRedGlowing else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${file.fileSize} • ${file.dateAdded}",
                                color = MukulTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        if (isCurrent) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MukulRedPrimary
                            ) {
                                Text(
                                    text = "চলছে",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MukulTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSurface(
    exoPlayer: ExoPlayer,
    resizeMode: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.view_exo_player, null, false) as PlayerView
            view.apply {
                player = exoPlayer
                useController = false
                this.resizeMode = resizeMode
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            playerView.resizeMode = resizeMode
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun WebEmbedPlayerView(
    url: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                val isM3u8 = url.contains(".m3u8", ignoreCase = true) || url.contains("/hls", ignoreCase = true)
                if (isM3u8) {
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
                            <style>
                                * { margin: 0; padding: 0; box-sizing: border-box; }
                                html, body { width: 100%; height: 100%; background: #000; overflow: hidden; display: flex; align-items: center; justify-content: center; }
                                video { width: 100%; height: 100%; object-fit: contain; }
                            </style>
                        </head>
                        <body>
                            <video id="player" controls autoplay playsinline></video>
                            <script>
                                var video = document.getElementById('player');
                                var src = '$url';
                                if (Hls.isSupported()) {
                                    var hls = new Hls({ enableWorker: true, lowLatencyMode: true });
                                    hls.loadSource(src);
                                    hls.attachMedia(video);
                                    hls.on(Hls.Events.MANIFEST_PARSED, function() { video.play(); });
                                } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                                    video.src = src;
                                    video.play();
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL("https://freelivesports.tv", html, "text/html", "UTF-8", null)
                } else {
                    loadUrl(url)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
fun SpeedDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E202E),
        title = {
            Text(
                text = "প্লেব্যাক স্পিড নির্বাচন করুন",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectSpeed(speed)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = {
                                onSelectSpeed(speed)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MukulRedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (speed == 1.0f) "1.0x (স্বাভাবিক)" else "${speed}x",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (currentSpeed == speed) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = MukulRedGlowing)
            }
        }
    )
}

@Composable
fun SleepTimerDialog(
    currentMinutes: Int,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0 to "বন্ধ (Off)",
        15 to "১৫ মিনিট",
        30 to "৩০ মিনিট",
        45 to "৪৫ মিনিট",
        60 to "৬০ মিনিট"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E202E),
        title = {
            Text(
                text = "স্লিপ টাইমার নির্ধারণ করুন",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { (mins, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMinutes(mins)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = currentMinutes == mins,
                            onClick = {
                                onSelectMinutes(mins)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MukulRedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (currentMinutes == mins) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = MukulRedGlowing)
            }
        }
    )
}

@Composable
fun AspectRatioDialog(
    currentMode: Int,
    onSelectMode: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "ফিট (Fit - 16:9)",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "জুম (Zoom - Fill Screen)",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "স্ট্রেচ (Stretch)"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E202E),
        title = {
            Text(
                text = "স্ক্রিন অনুপাত (Aspect Ratio)",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMode(mode)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = {
                                onSelectMode(mode)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MukulRedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = MukulRedGlowing)
            }
        }
    )
}

@Composable
private fun PlayerErrorView(
    onRetry: () -> Unit,
    onPlayInWebEngine: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "ভিডিও লোড হতে সমস্যা হয়েছে",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("পুনরায় চেষ্টা", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (onPlayInWebEngine != null) {
                Button(
                    onClick = onPlayInWebEngine,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Web Engine",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ওয়েব প্লেয়ার", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FullscreenControlsOverlay(
    title: String,
    subtitle: String,
    isLive: Boolean,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    sliderPosition: Float,
    isDraggingSlider: Boolean,
    isMuted: Boolean,
    currentSpeed: Float,
    sleepTimerMinutes: Int,
    onSeek: (Float) -> Unit,
    onSliderDrag: (Boolean, Float) -> Unit,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onToggleMute: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onOpenTimerDialog: () -> Unit,
    onOpenAspectDialog: () -> Unit,
    onLockScreen: () -> Unit,
    onEnterPip: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onDownloadClick: (() -> Unit)?,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x77000000))
    ) {
        // Top Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xCC000000), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLive) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MukulRedPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Speed Controller
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0x66000000),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clickable { onOpenSpeedDialog() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = MukulRedGlowing,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${currentSpeed}x",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sleep Timer
                IconButton(
                    onClick = onOpenTimerDialog,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (sleepTimerMinutes > 0) MukulRedPrimary else Color(0x55000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Mute / Unmute
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Aspect Ratio
                IconButton(
                    onClick = onOpenAspectDialog,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Lock Screen
                IconButton(
                    onClick = onLockScreen,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // PiP
                IconButton(
                    onClick = onEnterPip,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "PiP",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (!isLive && onDownloadClick != null) {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x55000000))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x55000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Center Controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.align(Alignment.Center)
        ) {
            if (!isLive) {
                IconButton(
                    onClick = onRewind,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MukulRedPrimary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            if (!isLive) {
                IconButton(
                    onClick = onForward,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Bottom Controls Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (!isLive && totalDuration > 0) {
                Slider(
                    value = if (isDraggingSlider) sliderPosition else (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f),
                    onValueChange = { onSliderDrag(true, it) },
                    onValueChangeFinished = {
                        onSliderDrag(false, sliderPosition)
                        onSeek(sliderPosition)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MukulRedPrimary,
                        activeTrackColor = MukulRedPrimary,
                        inactiveTrackColor = Color(0x55FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLive) "🔴 লাইভ স্ট্রিমিং" else "${formatTime(currentPosition)} / ${formatTime(totalDuration)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenAspectDialog,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
