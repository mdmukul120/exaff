package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.DownloadHelper
import com.example.ui.components.MediaDetailsSheet
import com.example.ui.components.VideoPlayerView
import com.example.ui.screens.BongoScreen
import com.example.ui.screens.CNCVerseScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HiddenFolderScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.PasswordPromptDialog
import com.example.ui.screens.SearchDialog
import com.example.ui.screens.SeeAllSectionScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SportsScreen
import com.example.ui.theme.MukulBlack
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedGlowing
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import android.content.res.Configuration
import androidx.compose.runtime.mutableStateOf
import com.example.ui.theme.MukulTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val isInPipModeState = mutableStateOf(false)

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            val isInPip by isInPipModeState
            MyApplicationTheme(darkTheme = state.isDarkMode) {
                MukulPlusApp(viewModel = viewModel, isInPip = isInPip)
            }
        }
    }
}

data class NavItem(val bnTitle: String, val enTitle: String, val icon: ImageVector)

@Composable
fun MukulPlusApp(viewModel: MainViewModel = viewModel(), isInPip: Boolean = false) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isBangla = state.language == "bn"
    var showVaultPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCncExtensions(context)
    }

    val navItems = listOf(
        NavItem("হোম", "Home", Icons.Default.Home),
        NavItem("প্রোভাইডার", "Providers", Icons.Default.Movie),
        NavItem("বঙ্গ", "Bongo", Icons.Default.VideoLibrary),
        NavItem("টিভি", "Live TV", Icons.Default.LiveTv),
        NavItem("স্পোর্টস", "Sports", Icons.Default.SportsCricket),
        NavItem("সিএনসি", "CNC Verse", Icons.Default.Extension),
        NavItem("ডাউনলোড", "Downloads", Icons.Default.Download)
    )

    // Handle Android system Back button
    BackHandler(
        enabled = state.activePlayingMedia != null ||
                state.activePlayingChannel != null ||
                state.offlinePlayingMedia != null ||
                state.selectedMediaForDetails != null ||
                state.selectedSectionForSeeAll != null ||
                state.isVaultOpen ||
                state.isSettingsOpen ||
                state.isWatchlistOpen ||
                state.isSearchOpen ||
                state.selectedTab != 0
    ) {
        if (state.activePlayingMedia != null || state.activePlayingChannel != null || state.offlinePlayingMedia != null) {
            viewModel.stopPlayback()
        } else if (state.isVaultOpen) {
            viewModel.closeVault()
        } else if (state.isSettingsOpen) {
            viewModel.setSettingsOpen(false)
        } else if (state.isWatchlistOpen) {
            viewModel.setWatchlistOpen(false)
        } else if (state.selectedMediaForDetails != null) {
            viewModel.closeMediaDetails()
        } else if (state.selectedSectionForSeeAll != null) {
            viewModel.closeSeeAllSection()
        } else if (state.isSearchOpen) {
            viewModel.setSearchOpen(false)
        } else if (state.selectedTab != 0) {
            viewModel.selectTab(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(if (state.isDarkMode) MukulDarkBg else Color(0xFFF4F5F9))) {
        // 1. Splash Screen on Launch
        if (state.isSplashVisible) {
            SplashScreen(
                onTimeout = { viewModel.dismissSplash() }
            )
        } else {
            // 2. Main App Screen Layout
            Scaffold(
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    if (!isInPip && state.activePlayingMedia == null && state.activePlayingChannel == null && state.offlinePlayingMedia == null) {
                        MukulTopBar(
                            isDarkMode = state.isDarkMode,
                            isVaultUnlocked = state.isVaultUnlocked,
                            onVaultClick = {
                                if (state.isVaultUnlocked) {
                                    viewModel.openVaultDirectly()
                                } else {
                                    showVaultPasswordDialog = true
                                }
                            },
                            onSearchClick = { viewModel.setSearchOpen(true) },
                            onProfileClick = { viewModel.setSettingsOpen(true) }
                        )
                    }
                },
                bottomBar = {
                    if (!isInPip && state.activePlayingMedia == null && state.activePlayingChannel == null && state.offlinePlayingMedia == null) {
                        NavigationBar(
                            containerColor = if (state.isDarkMode) MukulBlack else Color.White,
                            contentColor = if (state.isDarkMode) MukulTextPrimary else Color.Black,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .height(64.dp)
                                .testTag("bottom_navigation_bar")
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val isSelected = state.selectedTab == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(index) },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = if (isBangla) item.bnTitle else item.enTitle,
                                            tint = if (isSelected) MukulRedPrimary else MukulTextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isBangla) item.bnTitle else item.enTitle,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) (if (state.isDarkMode) Color.White else Color.Black) else MukulTextSecondary
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        indicatorColor = MukulRedPrimary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Screen Router
                    when (state.selectedTab) {
                        0 -> HomeScreen(
                            heroMedia = state.heroMedia,
                            sections = state.sections,
                            liveChannels = state.allChannels,
                            watchlistIds = state.watchlistIds,
                            isLoading = state.isLoading,
                            isBangla = isBangla,
                            isDarkMode = state.isDarkMode,
                            onMediaClick = { viewModel.openMediaDetails(it) },
                            onChannelClick = { viewModel.playChannel(it) },
                            onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                            onSeeAllChannels = { viewModel.selectTab(3) },
                            onSeeAllSection = { section -> viewModel.openSeeAllSection(section) }
                        )

                        1 -> {
                            val movies = viewModel.getMoviesForCategoryAndPage(state.movieCategory, state.currentMoviePage)
                            MoviesScreen(
                                movies = movies,
                                selectedCategory = state.movieCategory,
                                currentPage = state.currentMoviePage,
                                isLoadingPage = state.isMoviesPageLoading,
                                watchlistIds = state.watchlistIds,
                                isBangla = isBangla,
                                isDarkMode = state.isDarkMode,
                                onSelectCategory = { viewModel.setMovieCategory(it) },
                                onSelectPage = { page -> viewModel.loadMoviePage(state.movieCategory, page) },
                                onMovieClick = { viewModel.openMediaDetails(it) },
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                            )
                        }

                        2 -> {
                            val bongoList = state.sections
                                .flatMap { it.items }
                                .filter {
                                    it.provider.equals("bongo", true) ||
                                            it.category.equals("bongo", true) ||
                                            it.id.startsWith("bongo_")
                                }
                                .distinctBy { it.id }
                            BongoScreen(
                                bongoItems = bongoList,
                                watchlistIds = state.watchlistIds,
                                isLoading = state.isLoading,
                                isBangla = isBangla,
                                isDarkMode = state.isDarkMode,
                                onMediaClick = { viewModel.openMediaDetails(it) },
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                                onRefresh = { viewModel.loadData() }
                            )
                        }

                        3 -> LiveTvScreen(
                            channels = state.allChannels,
                            selectedCategory = state.channelCategory,
                            isBangla = isBangla,
                            isDarkMode = state.isDarkMode,
                            onSelectCategory = { viewModel.setChannelCategory(it) },
                            onChannelClick = { viewModel.playChannel(it) }
                        )

                        4 -> SportsScreen(
                            sportsMap = state.sportsMap,
                            selectedSource = state.selectedSportsSource,
                            isLoading = state.isSportsLoading,
                            isBangla = isBangla,
                            isDarkMode = state.isDarkMode,
                            onSelectSource = { viewModel.selectSportsSource(it) },
                            onPlayMatchStream = { match, stream -> viewModel.playSportsStream(match, stream) },
                            onRefresh = { viewModel.loadSports(forceRefresh = true) }
                        )

                        5 -> CNCVerseScreen(
                            repoInfo = state.cncRepo,
                            plugins = state.cncPlugins,
                            contentItems = state.cncContent,
                            isLoading = state.isCncLoading,
                            isBangla = isBangla,
                            isDarkMode = state.isDarkMode,
                            onRefresh = { viewModel.loadCncExtensions(context, forceRefresh = true) },
                            onToggleInstall = { viewModel.toggleCncPluginInstall(context, it) },
                            onInstallAll = { viewModel.installAllCncPlugins(context) },
                            onUninstallAll = { viewModel.uninstallAllCncPlugins(context) },
                            onPlayMedia = { viewModel.playMedia(it) },
                            onOpenMediaDetails = { viewModel.openMediaDetails(it) }
                        )

                        6 -> DownloadsScreen(
                            isBangla = isBangla,
                            isDarkMode = state.isDarkMode,
                            onPlayOfflineMedia = { viewModel.playOfflineMedia(it) },
                            isVaultUnlocked = state.isVaultUnlocked,
                            onUnlockVault = { viewModel.unlockAndOpenVault(it) },
                            onOpenVault = { viewModel.openVaultDirectly() }
                        )
                    }

                    // Secret Vault / Hidden Folder Layer
                    if (!isInPip && state.isVaultOpen) {
                        HiddenFolderScreen(
                            videos = state.hiddenVideos,
                            isLoading = state.isHiddenVideosLoading,
                            isDarkMode = state.isDarkMode,
                            isBangla = isBangla,
                            onBack = { viewModel.closeVault() },
                            onLock = { viewModel.lockVault() },
                            onRefresh = { viewModel.loadHiddenVideos() },
                            onPlayVideo = { title, url, poster ->
                                viewModel.playDirectStream(title, url, poster)
                            }
                        )
                    }

                    // Settings Layer
                    if (!isInPip && state.isSettingsOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (state.isDarkMode) MukulDarkBg else Color(0xFFF4F5F9))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    IconButton(onClick = { viewModel.setSettingsOpen(false) }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back",
                                            tint = if (state.isDarkMode) Color.White else Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "সেটিংস" else "Settings",
                                        color = if (state.isDarkMode) Color.White else Color.Black,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                SettingsScreen(
                                    currentLanguage = state.language,
                                    onLanguageChange = { viewModel.setLanguage(it) },
                                    isDarkMode = state.isDarkMode,
                                    onThemeToggle = { viewModel.setDarkMode(it) },
                                    selectedQuality = state.selectedQuality,
                                    onQualityChange = { viewModel.setSelectedQuality(it) },
                                    onPlayOfflineMedia = { viewModel.playOfflineMedia(it) }
                                )
                            }
                        }
                    }

                    // Watchlist Layer
                    if (!isInPip && state.isWatchlistOpen) {
                        val watchlistItems = state.sections.flatMap { it.items }.filter { state.watchlistIds.contains(it.id) }.distinctBy { it.id }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (state.isDarkMode) MukulDarkBg else Color(0xFFF4F5F9))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    IconButton(onClick = { viewModel.setWatchlistOpen(false) }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back",
                                            tint = if (state.isDarkMode) Color.White else Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isBangla) "আমার ওয়াচলিস্ট (${watchlistItems.size})" else "My Watchlist (${watchlistItems.size})",
                                        color = if (state.isDarkMode) Color.White else Color.Black,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (watchlistItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = MukulTextSecondary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = if (isBangla) "কোনো কনটেন্ট সংরক্ষিত নেই" else "No items in watchlist",
                                                color = if (state.isDarkMode) Color.White else Color.Black,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    SeeAllSectionScreen(
                                        section = com.example.data.model.ContentSection(
                                            title = if (isBangla) "পছন্দের তালিকা" else "My Watchlist",
                                            items = watchlistItems
                                        ),
                                        watchlistIds = state.watchlistIds,
                                        isBangla = isBangla,
                                        isDarkMode = state.isDarkMode,
                                        onBack = { viewModel.setWatchlistOpen(false) },
                                        onMediaClick = { viewModel.openMediaDetails(it) },
                                        onToggleWatchlist = { viewModel.toggleWatchlist(it) }
                                    )
                                }
                            }
                        }
                    }

                    // Video Player Layer
                    val media = state.activePlayingMedia
                    val channel = state.activePlayingChannel
                    val offlineMedia = state.offlinePlayingMedia

                    if (media != null) {
                        val isSports = media.category == "sports"
                        val isLiveMedia = isSports || media.label.contains("live", true) || media.streamUrl.contains(".m3u8", true)
                        VideoPlayerView(
                            title = media.title,
                            streamUrl = media.streamUrl.ifEmpty { "https://4397879b.wurl.com/master/f36d25e7e52f1ba8d7e56eb859c636563214f541/UmFrdXRlblRWLWRlX0ZJRkFQbHVzR2VybWFuX0hMUw/playlist.m3u8" },
                            isLive = isLiveMedia,
                            customHeaders = media.customHeaders,
                            subtitle = if (isSports) media.description else "${media.year} • ${media.genre}",
                            isInPip = isInPip,
                            onDownloadClick = if (isSports) null else {
                                {
                                    val url = media.downloadUrl.ifEmpty { media.streamUrl }
                                    DownloadHelper.downloadMovie(context, media.title, url, media.posterUrl)
                                }
                            },
                            onClose = { viewModel.stopPlayback() }
                        )
                    } else if (channel != null) {
                        VideoPlayerView(
                            title = channel.name,
                            streamUrl = channel.streamUrl,
                            isLive = true,
                            customHeaders = emptyMap(),
                            subtitle = "${channel.group} • Live Stream",
                            isInPip = isInPip,
                            onClose = { viewModel.stopPlayback() }
                        )
                    } else if (offlineMedia != null) {
                        val playUrl = if (offlineMedia.filePath.startsWith("content://") || offlineMedia.filePath.startsWith("file://")) {
                            offlineMedia.filePath
                        } else {
                            "file://${offlineMedia.filePath}"
                        }
                        VideoPlayerView(
                            title = offlineMedia.title,
                            streamUrl = playUrl,
                            isLive = false,
                            customHeaders = emptyMap(),
                            subtitle = "Offline Playback • ${offlineMedia.fileSize}",
                            isInPip = isInPip,
                            onClose = { viewModel.stopPlayback() }
                        )
                    }

                    // Media Details Bottom Sheet Layer (Movie extraction, quality & in-app downloads)
                    if (!isInPip) {
                        state.selectedMediaForDetails?.let { item ->
                            val related = state.sections.flatMap { it.items }.distinctBy { it.id }
                            MediaDetailsSheet(
                                media = item,
                                relatedMedia = related,
                                relatedChannels = state.allChannels,
                                isBangla = isBangla,
                                onDismiss = { viewModel.closeMediaDetails() },
                                onPlayMedia = { title, streamUrl ->
                                    viewModel.closeMediaDetails()
                                    viewModel.playDirectStream(title, streamUrl)
                                },
                                onChannelClick = { ch ->
                                    viewModel.closeMediaDetails()
                                    viewModel.playChannel(ch)
                                }
                            )
                        }
                    }

                    // Search Dialog Layer
                    if (!isInPip && state.isSearchOpen) {
                        val allMedia = state.sections.flatMap { it.items }.distinctBy { it.id }
                        SearchDialog(
                            searchQuery = state.searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            allMedia = allMedia,
                            allChannels = state.allChannels,
                            onSelectMedia = {
                                viewModel.setSearchOpen(false)
                                viewModel.openMediaDetails(it)
                            },
                            onSelectChannel = {
                                viewModel.setSearchOpen(false)
                                viewModel.playChannel(it)
                            },
                            onDismiss = { viewModel.setSearchOpen(false) }
                        )
                    }

                    // See All Section Full View Layer
                    if (!isInPip) {
                        state.selectedSectionForSeeAll?.let { section ->
                            SeeAllSectionScreen(
                                section = section,
                                watchlistIds = state.watchlistIds,
                                isBangla = isBangla,
                                isDarkMode = state.isDarkMode,
                                onBack = { viewModel.closeSeeAllSection() },
                                onMediaClick = { viewModel.openMediaDetails(it) },
                                onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                                onOpenInMovies = { title ->
                                    viewModel.closeSeeAllSection()
                                    val isBongo = title.contains("Bongo", true)
                                    if (isBongo) {
                                        viewModel.selectTab(2)
                                    } else {
                                        val cat = when {
                                            title.contains("Blockbusters", true) || title.contains("HDHub4U", true) -> "HDHub4U"
                                            title.contains("Bollywood", true) || title.contains("MoviesMod", true) -> "MoviesMod"
                                            title.contains("Top Movies", true) || title.contains("TopMovies", true) -> "TopMovies"
                                            title.contains("Bioscope", true) || title.contains("Cinema", true) -> "Bangla Cinema"
                                            else -> "All"
                                        }
                                        viewModel.setMovieCategory(cat)
                                        viewModel.selectTab(1)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showVaultPasswordDialog) {
            PasswordPromptDialog(
                isDarkMode = state.isDarkMode,
                isBangla = isBangla,
                onUnlockSuccess = {
                    val success = viewModel.unlockAndOpenVault("01716649945")
                    if (success) {
                        showVaultPasswordDialog = false
                    }
                },
                onDismiss = { showVaultPasswordDialog = false }
            )
        }
    }
}

@Composable
fun MukulTopBar(
    isDarkMode: Boolean,
    isVaultUnlocked: Boolean = false,
    onVaultClick: () -> Unit = {},
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(if (isDarkMode) MukulBlack else Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("mukul_top_bar")
    ) {
        // Logo: "Mukul Plus"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("app_logo")
        ) {
            Text(
                text = "Mukul",
                color = if (isDarkMode) Color.White else Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MukulRedPrimary
            ) {
                Text(
                    text = "PLUS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF1B1C26) else Color(0xFFEEEEF4))
                    .testTag("search_icon_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Secret Vault / Hidden Folder Button
            IconButton(
                onClick = onVaultClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF261D12) else Color(0xFFFFF7E6))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), CircleShape)
                    .testTag("vault_icon_button")
            ) {
                Icon(
                    imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = "Secret Vault",
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // User Profile / Settings Action Button
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MukulRedPrimary)
                    .testTag("profile_avatar")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
