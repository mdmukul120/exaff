package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiService
import com.example.data.model.CNCContentItem
import com.example.data.model.CNCPluginItem
import com.example.data.model.CNCRepoInfo
import com.example.data.model.ContentSection
import com.example.data.model.DownloadedMedia
import com.example.data.model.HiddenVideoItem
import com.example.data.model.LiveChannel
import com.example.data.model.MediaItem
import com.example.data.model.SportsMatchItem
import com.example.data.model.SportsSource
import com.example.data.model.SportsStream
import com.example.data.repository.CNCRepository
import com.example.data.repository.MediaRepository
import com.example.data.repository.SportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MukulUiState(
    val isSplashVisible: Boolean = true,
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // 0: Home, 1: Movies, 2: Series, 3: Live TV, 4: Downloads, 5: More
    val language: String = "bn", // "bn" (Bangla) or "en" (English)
    val isDarkMode: Boolean = true,
    val selectedQuality: String = "Auto",
    val heroMedia: MediaItem? = null,
    val sections: List<ContentSection> = emptyList(),
    val allChannels: List<LiveChannel> = emptyList(),
    val channelCategory: String = "Bangla", // Bangla is prioritized first!
    val movieCategory: String = "All",
    val activePlayingMedia: MediaItem? = null,
    val activePlayingChannel: LiveChannel? = null,
    val selectedMediaForDetails: MediaItem? = null,
    val offlinePlayingMedia: DownloadedMedia? = null,
    val watchlistIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val errorMessage: String? = null,
    val currentMoviePage: Int = 1,
    val isMoviesPageLoading: Boolean = false,
    val selectedSectionForSeeAll: ContentSection? = null,
    val providerMoviesMap: Map<Pair<String, Int>, List<MediaItem>> = emptyMap(),
    val sportsMap: Map<SportsSource, List<SportsMatchItem>> = emptyMap(),
    val isSportsLoading: Boolean = false,
    val selectedSportsSource: SportsSource = SportsSource.LIVE_SPORTS,
    val selectedSportsMatchForDetails: SportsMatchItem? = null,
    val isSettingsOpen: Boolean = false,
    val isWatchlistOpen: Boolean = false,
    val isVaultOpen: Boolean = false,
    val isVaultUnlocked: Boolean = false,
    val hiddenVideos: List<HiddenVideoItem> = emptyList(),
    val isHiddenVideosLoading: Boolean = false,
    val cncRepo: CNCRepoInfo = CNCRepoInfo(),
    val cncPlugins: List<CNCPluginItem> = emptyList(),
    val cncContent: List<CNCContentItem> = emptyList(),
    val isCncLoading: Boolean = false
)

class MainViewModel(
    private val repository: MediaRepository = MediaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MukulUiState())
    val uiState: StateFlow<MukulUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadSports()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (hero, sections, channels) = repository.getHomeContent()
                val initialMap = mutableMapOf<Pair<String, Int>, List<MediaItem>>()
                sections.forEach { section ->
                    section.items.forEach { item ->
                        val p = item.provider.lowercase().trim()
                        if (p.isNotEmpty()) {
                            val list = initialMap.getOrPut(Pair(p, 1)) { mutableListOf() } as MutableList
                            if (!list.any { it.id == item.id }) {
                                list.add(item)
                            }
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        heroMedia = hero,
                        sections = sections,
                        allChannels = channels,
                        providerMoviesMap = initialMap
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load content. Please check network."
                    )
                }
            }
        }
    }

    fun dismissSplash() {
        _uiState.update { it.copy(isSplashVisible = false) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setLanguage(lang: String) {
        _uiState.update { it.copy(language = lang) }
    }

    fun setDarkMode(isDark: Boolean) {
        _uiState.update { it.copy(isDarkMode = isDark) }
    }

    fun setSelectedQuality(quality: String) {
        _uiState.update { it.copy(selectedQuality = quality) }
    }

    fun openMediaDetails(media: MediaItem) {
        _uiState.update { it.copy(selectedMediaForDetails = media) }
    }

    fun closeMediaDetails() {
        _uiState.update { it.copy(selectedMediaForDetails = null) }
    }

    fun openSeeAllSection(section: ContentSection) {
        _uiState.update { it.copy(selectedSectionForSeeAll = section) }
    }

    fun closeSeeAllSection() {
        _uiState.update { it.copy(selectedSectionForSeeAll = null) }
    }

    fun playMedia(media: MediaItem) {
        _uiState.update {
            it.copy(
                activePlayingMedia = media,
                activePlayingChannel = null,
                offlinePlayingMedia = null,
                selectedMediaForDetails = null
            )
        }
    }

    fun playDirectStream(title: String, streamUrl: String, posterUrl: String = "") {
        val tempMedia = MediaItem(
            id = "direct_${streamUrl.hashCode()}",
            title = title,
            streamUrl = streamUrl,
            posterUrl = posterUrl
        )
        playMedia(tempMedia)
    }

    fun unlockAndOpenVault(password: String): Boolean {
        if (password.trim() == "01716649945") {
            _uiState.update { it.copy(isVaultUnlocked = true, isVaultOpen = true) }
            if (_uiState.value.hiddenVideos.isEmpty()) {
                loadHiddenVideos()
            }
            return true
        }
        return false
    }

    fun openVaultDirectly() {
        if (_uiState.value.isVaultUnlocked) {
            _uiState.update { it.copy(isVaultOpen = true) }
            if (_uiState.value.hiddenVideos.isEmpty()) {
                loadHiddenVideos()
            }
        }
    }

    fun closeVault() {
        _uiState.update { it.copy(isVaultOpen = false) }
    }

    fun lockVault() {
        _uiState.update { it.copy(isVaultOpen = false, isVaultUnlocked = false) }
    }

    fun loadHiddenVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isHiddenVideosLoading = true) }
            val videos = ApiService.fetchHiddenFolderVideos()
            _uiState.update { it.copy(hiddenVideos = videos, isHiddenVideosLoading = false) }
        }
    }

    fun playChannel(channel: LiveChannel) {
        _uiState.update {
            it.copy(
                activePlayingChannel = channel,
                activePlayingMedia = null,
                offlinePlayingMedia = null,
                selectedMediaForDetails = null
            )
        }
    }

    fun playOfflineMedia(downloaded: DownloadedMedia) {
        _uiState.update {
            it.copy(
                offlinePlayingMedia = downloaded,
                activePlayingMedia = null,
                activePlayingChannel = null,
                selectedMediaForDetails = null
            )
        }
    }

    fun stopPlayback() {
        _uiState.update {
            it.copy(
                activePlayingMedia = null,
                activePlayingChannel = null,
                offlinePlayingMedia = null
            )
        }
    }

    fun toggleWatchlist(id: String) {
        _uiState.update { state ->
            val updated = state.watchlistIds.toMutableSet()
            if (updated.contains(id)) {
                updated.remove(id)
            } else {
                updated.add(id)
            }
            state.copy(watchlistIds = updated)
        }
    }

    fun setChannelCategory(category: String) {
        _uiState.update { it.copy(channelCategory = category) }
    }

    fun setMovieCategory(category: String) {
        _uiState.update { it.copy(movieCategory = category, currentMoviePage = 1) }
        loadMoviePage(category, 1)
    }

    fun loadMoviePage(category: String, page: Int) {
        val providerKey = when (category) {
            "HDHub4U" -> "hdhub4u"
            "MoviesMod" -> "moviesmod"
            "TopMovies" -> "topmovies"
            "MoviesDrive" -> "moviesdrive"
            "UHD Movies" -> "uhd"
            "Bongo" -> "bongo"
            "Bioscope" -> "bioscope"
            "Hindi Dubbed" -> "topmovies"
            "Bangla Cinema" -> "bioscope"
            else -> "hdhub4u"
        }

        val cacheKey = Pair(providerKey, page)
        val currentCached = _uiState.value.providerMoviesMap[cacheKey]
        if (currentCached != null && currentCached.isNotEmpty()) {
            _uiState.update { it.copy(currentMoviePage = page) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMoviesPageLoading = true, currentMoviePage = page) }
            try {
                val items = if (providerKey == "bongo" || providerKey == "bioscope") {
                    val existing = _uiState.value.sections.flatMap { it.items }.filter {
                        it.provider.equals(providerKey, true) || it.category.contains(providerKey, true)
                    }
                    existing
                } else {
                    com.example.data.api.ApiService.fetchExtractorPosts(providerKey, page)
                }

                _uiState.update { state ->
                    val updatedMap = state.providerMoviesMap.toMutableMap()
                    if (items.isNotEmpty()) {
                        updatedMap[cacheKey] = items
                    }
                    state.copy(
                        isMoviesPageLoading = false,
                        providerMoviesMap = updatedMap
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isMoviesPageLoading = false) }
            }
        }
    }

    fun getMoviesForCategoryAndPage(category: String, page: Int): List<MediaItem> {
        val state = _uiState.value
        val providerKey = when (category) {
            "HDHub4U" -> "hdhub4u"
            "MoviesMod" -> "moviesmod"
            "TopMovies" -> "topmovies"
            "MoviesDrive" -> "moviesdrive"
            "UHD Movies" -> "uhd"
            "Bongo" -> "bongo"
            "Bioscope" -> "bioscope"
            "Hindi Dubbed" -> "topmovies"
            "Bangla Cinema" -> "bioscope"
            else -> null
        }

        if (category == "All") {
            if (page == 1) {
                val allMovies = state.sections.flatMap { it.items }.distinctBy { it.id }
                if (allMovies.isNotEmpty()) return allMovies
            }
            val pItems = state.providerMoviesMap[Pair("hdhub4u", page)]
            if (!pItems.isNullOrEmpty()) return pItems
        }

        if (providerKey != null) {
            val cached = state.providerMoviesMap[Pair(providerKey, page)]
            if (!cached.isNullOrEmpty()) return cached

            if (page == 1) {
                val fromSections = state.sections.flatMap { it.items }.filter {
                    it.provider.equals(providerKey, true) ||
                    it.category.contains(providerKey, true) ||
                    (category == "Hindi Dubbed" && (it.title.contains("Dubbed", true) || it.title.contains("Dual", true))) ||
                    (category == "Bangla Cinema" && (it.genre.contains("Bangla", true) || it.category.contains("bioscope", true)))
                }.distinctBy { it.id }
                if (fromSections.isNotEmpty()) return fromSections
            }
        }

        return emptyList()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSearchOpen(open: Boolean) {
        _uiState.update { it.copy(isSearchOpen = open, searchQuery = if (!open) "" else it.searchQuery) }
    }

    fun loadSports(source: SportsSource? = null, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSportsLoading = true) }
            try {
                if (source != null) {
                    val items = SportsRepository.getSportsForSource(source, forceRefresh)
                    _uiState.update { state ->
                        val updated = state.sportsMap.toMutableMap()
                        updated[source] = items
                        state.copy(
                            sportsMap = updated,
                            isSportsLoading = false,
                            selectedSportsSource = source
                        )
                    }
                } else {
                    val all = SportsRepository.preloadAllSports()
                    _uiState.update {
                        it.copy(
                            sportsMap = all,
                            isSportsLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSportsLoading = false) }
            }
        }
    }

    fun selectSportsSource(source: SportsSource) {
        _uiState.update { it.copy(selectedSportsSource = source) }
        val current = _uiState.value.sportsMap[source]
        if (current == null || current.isEmpty()) {
            loadSports(source)
        }
    }

    fun openSportsMatchDetails(match: SportsMatchItem) {
        _uiState.update { it.copy(selectedSportsMatchForDetails = match) }
    }

    fun closeSportsMatchDetails() {
        _uiState.update { it.copy(selectedSportsMatchForDetails = null) }
    }

    fun playSportsStream(match: SportsMatchItem, stream: SportsStream) {
        val media = MediaItem(
            id = "sports_${match.id}_${stream.title.hashCode()}",
            title = "${match.title} (${stream.title})",
            category = "sports",
            posterUrl = match.bannerUrl.ifEmpty { match.teamAFlag },
            backdropUrl = match.bannerUrl.ifEmpty { match.teamBFlag },
            streamUrl = stream.streamUrl,
            label = match.status,
            genre = match.sportType,
            description = "${match.tournament} • ${match.startTime}",
            customHeaders = stream.headers
        )
        playMedia(media)
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
    }

    fun setWatchlistOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isWatchlistOpen = isOpen) }
    }

    fun loadCncExtensions(context: Context, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCncLoading = true) }
            try {
                val (repo, plugins) = CNCRepository.loadRepoAndPlugins(context, forceRefresh)
                val content = CNCRepository.getCuratedContent()
                _uiState.update {
                    it.copy(
                        cncRepo = repo,
                        cncPlugins = plugins,
                        cncContent = content,
                        isCncLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCncLoading = false) }
            }
        }
    }

    fun toggleCncPluginInstall(context: Context, plugin: CNCPluginItem) {
        val newInstalled = !plugin.isInstalled
        CNCRepository.setPluginInstalled(context, plugin.internalName, newInstalled)
        _uiState.update { state ->
            val updated = state.cncPlugins.map {
                if (it.internalName == plugin.internalName) it.copy(isInstalled = newInstalled) else it
            }
            state.copy(cncPlugins = updated)
        }
    }

    fun installAllCncPlugins(context: Context) {
        CNCRepository.installAll(context)
        _uiState.update { state ->
            val updated = state.cncPlugins.map { it.copy(isInstalled = true) }
            state.copy(cncPlugins = updated)
        }
    }

    fun uninstallAllCncPlugins(context: Context) {
        CNCRepository.uninstallAll(context)
        _uiState.update { state ->
            val updated = state.cncPlugins.map { it.copy(isInstalled = false) }
            state.copy(cncPlugins = updated)
        }
    }
}
