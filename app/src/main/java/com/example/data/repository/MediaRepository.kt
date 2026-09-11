package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiService
import com.example.data.api.ExtractorInfoResult
import com.example.data.model.ContentSection
import com.example.data.model.EpisodeItem
import com.example.data.model.LiveChannel
import com.example.data.model.MediaItem
import com.example.data.model.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MediaRepository {
    private val TAG = "MediaRepository"

    suspend fun getHomeContent(): Triple<MediaItem?, List<ContentSection>, List<LiveChannel>> = withContext(Dispatchers.IO) {
        coroutineScope {
            // Background ping
            launch(Dispatchers.IO) {
                try { ApiService.pingServices() } catch (_: Exception) {}
            }

            // Launch parallel fetch requests for all providers and channels
            val bioscopeDeferred = async { try { ApiService.fetchBioscopePage() } catch (e: Exception) { null } }
            val bongoDeferred = async { try { ApiService.fetchBongoScrape() } catch (e: Exception) { null } }
            val hdhubDeferred = async { try { ApiService.fetchExtractorPosts("hdhub4u", 1) } catch (e: Exception) { emptyList() } }
            val moviesmodDeferred = async { try { ApiService.fetchExtractorPosts("moviesmod", 1) } catch (e: Exception) { emptyList() } }
            val topmoviesDeferred = async { try { ApiService.fetchExtractorPosts("topmovies", 1) } catch (e: Exception) { emptyList() } }
            val moviesdriveDeferred = async { try { ApiService.fetchExtractorPosts("moviesdrive", 1) } catch (e: Exception) { emptyList() } }
            val uhdDeferred = async { try { ApiService.fetchExtractorPosts("uhd", 1) } catch (e: Exception) { emptyList() } }
            val iptvDeferred = async { try { ApiService.fetchIptvPlaylist() } catch (e: Exception) { emptyList() } }

            var heroItem: MediaItem? = null
            val sections = mutableListOf<ContentSection>()

            // 1. Process Bioscope
            val bioscopeJson = bioscopeDeferred.await()
            if (!bioscopeJson.isNullOrEmpty()) {
                try {
                    val root = JSONObject(bioscopeJson)
                    val result = root.optJSONObject("result")
                    val jsonSections = result?.optJSONArray("sections")

                    if (jsonSections != null) {
                        for (i in 0 until jsonSections.length()) {
                            val secObj = jsonSections.getJSONObject(i)
                            val title = secObj.optString("title", "Featured")
                            val size = secObj.optString("size")
                            val itemsArray = secObj.optJSONArray("items") ?: continue

                            val mediaList = mutableListOf<MediaItem>()
                            for (j in 0 until itemsArray.length()) {
                                val itemObj = itemsArray.getJSONObject(j)
                                val contentObj = itemObj.optJSONObject("content") ?: itemObj
                                val id = contentObj.optString("id", itemObj.optString("id", "item_$j"))
                                val itemTitle = contentObj.optString("title", itemObj.optString("title", "Untitled"))
                                val desc = contentObj.optString("description")
                                val type = contentObj.optString("type", "movies")
                                val poster = contentObj.optString("poster").ifEmpty {
                                    contentObj.optString("tv_cover").ifEmpty {
                                        contentObj.optString("thumbnail")
                                    }
                                }
                                val backdrop = contentObj.optString("poster_background").ifEmpty {
                                    contentObj.optString("thumbnail_background").ifEmpty { poster }
                                }
                                val streamUrl = contentObj.optString("url")
                                val label = contentObj.optString("label", "Free")
                                val durationSec = contentObj.optInt("duration", 0)
                                val formattedDuration = if (durationSec > 0) {
                                    val hrs = durationSec / 3600
                                    val mins = (durationSec % 3600) / 60
                                    if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                                } else "2h 10m"

                                val castList = mutableListOf<String>()
                                val metas = contentObj.optJSONObject("metas")
                                val castsArray = metas?.optJSONArray("casts")
                                if (castsArray != null) {
                                    for (k in 0 until castsArray.length()) {
                                        val c = castsArray.getJSONObject(k).optString("title")
                                        if (c.isNotEmpty()) castList.add(c)
                                    }
                                }

                                val mediaItem = MediaItem(
                                    id = id,
                                    title = itemTitle,
                                    description = desc,
                                    category = if (type.contains("series", ignoreCase = true)) "series" else "movies",
                                    posterUrl = poster,
                                    backdropUrl = backdrop,
                                    streamUrl = streamUrl,
                                    rating = "4.8",
                                    year = "2024",
                                    duration = formattedDuration,
                                    genre = if (type.contains("series")) "Bangla Series" else "Bangla Cinema",
                                    label = label.ifEmpty { "HD" },
                                    cast = castList
                                )

                                if (size == "hero_slider" && heroItem == null && poster.isNotEmpty()) {
                                    heroItem = mediaItem
                                }
                                mediaList.add(mediaItem)
                            }

                            if (mediaList.isNotEmpty() && size != "hero_slider") {
                                sections.add(ContentSection(title = title, items = mediaList))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing Bioscope JSON: ${e.message}")
                }
            }

            // 2. Process Bongo
            val bongoScrapeJson = bongoDeferred.await()
            if (!bongoScrapeJson.isNullOrEmpty()) {
                try {
                    val bongoRoot = JSONObject(bongoScrapeJson)
                    val bongoData = bongoRoot.optJSONArray("data") ?: bongoRoot.optJSONArray("items")
                    if (bongoData != null) {
                        val bongoList = mutableListOf<MediaItem>()
                        for (b in 0 until bongoData.length()) {
                            val bObj = bongoData.getJSONObject(b)
                            val bId = bObj.optString("id", "bongo_$b")
                            val bTitle = bObj.optString("title", "Bongo Content")
                            val bPoster = bObj.optString("poster").ifEmpty { bObj.optString("thumbnail") }
                            val bDesc = bObj.optString("description")
                            val bType = bObj.optString("type", "movies")
                            val bStream = ApiService.getBongoHlsUrl(bId)
                            val bongoItem = MediaItem(
                                id = "bongo_$bId",
                                title = bTitle,
                                description = bDesc,
                                category = if (bType.contains("series", true)) "series" else "bongo",
                                posterUrl = bPoster,
                                backdropUrl = bPoster,
                                streamUrl = bStream,
                                rating = "4.9",
                                year = "2025",
                                duration = "HD",
                                genre = "Bongo Bangla Originals",
                                label = "BONGO",
                                provider = "bongo",
                                link = bId
                            )
                            bongoList.add(bongoItem)
                        }
                        if (bongoList.isNotEmpty()) {
                            sections.add(0, ContentSection(title = "Bongo Originals & Trending Drama", items = bongoList))
                            if (heroItem == null) {
                                heroItem = bongoList[0]
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing Bongo scrape JSON: ${e.message}")
                }
            }

            // 3. Process Extractor Providers
            val hdhubItems = hdhubDeferred.await()
            val moviesmodItems = moviesmodDeferred.await()
            val topmoviesItems = topmoviesDeferred.await()
            val moviesdriveItems = moviesdriveDeferred.await()
            val uhdItems = uhdDeferred.await()

            if (hdhubItems.isNotEmpty()) {
                sections.add(0, ContentSection(title = "Latest Blockbusters (2026)", items = hdhubItems))
                if (heroItem == null) {
                    heroItem = hdhubItems[0]
                }
            }

            if (moviesmodItems.isNotEmpty()) {
                val modSeries = moviesmodItems.filter { it.category == "series" }
                val modMovies = moviesmodItems.filter { it.category == "movies" }
                if (modMovies.isNotEmpty()) {
                    sections.add(ContentSection(title = "Bollywood & Multi-Audio Hits", items = modMovies))
                }
                if (modSeries.isNotEmpty()) {
                    sections.add(ContentSection(title = "Top Web Series & Dramas", items = modSeries))
                }
            }

            if (topmoviesItems.isNotEmpty()) {
                sections.add(ContentSection(title = "Top Movies & Dubbed Cinema", items = topmoviesItems))
            }

            if (moviesdriveItems.isNotEmpty()) {
                sections.add(ContentSection(title = "MoviesDrive 4K Collection", items = moviesdriveItems))
            }

            if (uhdItems.isNotEmpty()) {
                sections.add(ContentSection(title = "Ultra HD Movies", items = uhdItems))
            }

            // Fallback sections if all remote calls fail or are empty
            if (sections.isEmpty()) {
                sections.addAll(getDefaultSections())
                if (heroItem == null && sections.isNotEmpty() && sections[0].items.isNotEmpty()) {
                    heroItem = sections[0].items[0]
                }
            }

            // 4. Live Channels with fallback
            val fetchedChannels = iptvDeferred.await()
            val liveChannels = if (fetchedChannels.isNotEmpty()) {
                fetchedChannels
            } else {
                getDefaultChannels()
            }

            Triple(heroItem, sections, liveChannels)
        }
    }

    private fun getDefaultSections(): List<ContentSection> = listOf(
        ContentSection(
            title = "Trending Bangla Movies & Series",
            items = listOf(
                MediaItem(
                    id = "fallback_1",
                    title = "Surongo",
                    description = "A gripping thriller about ambition, greed, and a bank vault heist in Bangladesh.",
                    category = "movies",
                    posterUrl = "https://upload.wikimedia.org/wikipedia/en/2/29/Surongo_poster.jpg",
                    backdropUrl = "https://upload.wikimedia.org/wikipedia/en/2/29/Surongo_poster.jpg",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    rating = "4.9",
                    year = "2024",
                    genre = "Bangla Thriller",
                    label = "Full HD",
                    provider = "bongo"
                ),
                MediaItem(
                    id = "fallback_2",
                    title = "Priyotoma",
                    description = "A poignant romantic drama starring Shakib Khan and Idhika Paul.",
                    category = "movies",
                    posterUrl = "https://upload.wikimedia.org/wikipedia/en/d/dc/Priyotoma_film_poster.jpg",
                    backdropUrl = "https://upload.wikimedia.org/wikipedia/en/d/dc/Priyotoma_film_poster.jpg",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    rating = "4.8",
                    year = "2024",
                    genre = "Bangla Action Romance",
                    label = "HD",
                    provider = "bongo"
                ),
                MediaItem(
                    id = "fallback_3",
                    title = "Mohanagar",
                    description = "Acclaimed police thriller series exploring corruption, power, and morality.",
                    category = "series",
                    posterUrl = "https://upload.wikimedia.org/wikipedia/en/6/62/Mohanagar_poster.jpg",
                    backdropUrl = "https://upload.wikimedia.org/wikipedia/en/6/62/Mohanagar_poster.jpg",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    rating = "4.9",
                    year = "2023",
                    genre = "Crime Thriller",
                    label = "Web Series",
                    provider = "bongo"
                )
            )
        )
    )

    private fun getDefaultChannels(): List<LiveChannel> = listOf(
        LiveChannel(
            id = "somoy_tv",
            name = "Somoy TV",
            group = "Bangla",
            logoUrl = "https://i.ibb.co/6P0j7kG/somoy.png",
            streamUrl = "https://live-somoy.akamaized.net/live/somoy_720p/chunks.m3u8"
        ),
        LiveChannel(
            id = "jamuna_tv",
            name = "Jamuna TV",
            group = "Bangla",
            logoUrl = "https://i.ibb.co/q9rNcyv/jamuna.png",
            streamUrl = "https://jamuna.akamaized.net/live/jamuna_720p/chunks.m3u8"
        ),
        LiveChannel(
            id = "channel_i",
            name = "Channel i",
            group = "Bangla",
            logoUrl = "https://i.ibb.co/2N3J34J/channel-i.png",
            streamUrl = "https://channel-i.akamaized.net/live/channel_i_720p/chunks.m3u8"
        ),
        LiveChannel(
            id = "atn_bangla",
            name = "ATN Bangla",
            group = "Bangla",
            logoUrl = "https://i.ibb.co/y4LpW7Q/atn-bangla.png",
            streamUrl = "https://atnbangla.akamaized.net/live/atn_720p/chunks.m3u8"
        ),
        LiveChannel(
            id = "t_sports",
            name = "T Sports Live",
            group = "Sports",
            logoUrl = "https://i.ibb.co/4WH715L/tsports.png",
            streamUrl = "https://4397879b.wurl.com/master/f36d25e7e52f1ba8d7e56eb859c636563214f541/UmFrdXRlblRWLWRlX0ZJRkFQbHVzR2VybWFuX0hMUw/playlist.m3u8"
        ),
        LiveChannel(
            id = "fifa_plus",
            name = "FIFA+ Live",
            group = "Sports",
            logoUrl = "https://images.fifa.com/image/upload/t_s_16_9_cover/v1649234856/fifaplus/logo.png",
            streamUrl = "https://4397879b.wurl.com/master/f36d25e7e52f1ba8d7e56eb859c636563214f541/UmFrdXRlblRWLWRlX0ZJRkFQbHVzR2VybWFuX0hMUw/playlist.m3u8"
        )
    )

    suspend fun getLiveChannels(): List<LiveChannel> = withContext(Dispatchers.IO) {
        ApiService.fetchIptvPlaylist()
    }

    // Resolve details, qualities, and playable streaming link for extractor or Bongo movie/show
    suspend fun resolveMediaDetails(media: MediaItem): MediaItem = withContext(Dispatchers.IO) {
        if (media.provider.equals("bongo", true)) {
            try {
                val metaJson = ApiService.fetchBongoMeta(media.link)
                var updatedItem = media
                if (!metaJson.isNullOrEmpty()) {
                    val metaObj = JSONObject(metaJson)
                    val dataObj = metaObj.optJSONObject("data") ?: metaObj
                    val title = dataObj.optString("title", media.title)
                    val desc = dataObj.optString("description", media.description)
                    val poster = dataObj.optString("poster").ifEmpty { dataObj.optString("thumbnail") }
                    val stream = ApiService.getBongoHlsUrl(media.link)
                    updatedItem = updatedItem.copy(
                        title = title,
                        description = desc,
                        posterUrl = if (poster.isNotEmpty()) poster else media.posterUrl,
                        backdropUrl = if (poster.isNotEmpty()) poster else media.backdropUrl,
                        streamUrl = stream
                    )
                }

                // If it's a show / series, fetch episodes
                if (media.category == "series" || media.category == "bongo") {
                    val eps = ApiService.fetchBongoEpisodes(media.link)
                    if (eps.isNotEmpty()) {
                        updatedItem = updatedItem.copy(episodes = eps)
                    }
                }
                return@withContext updatedItem
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving Bongo media: ${e.message}")
                return@withContext media.copy(streamUrl = ApiService.getBongoHlsUrl(media.link))
            }
        }

        if (media.provider.isEmpty() || media.link.isEmpty()) {
            return@withContext media
        }

        try {
            val infoResult = ApiService.fetchExtractorInfo(media.provider, media.link)
            if (infoResult != null) {
                var updatedEpisodes = media.episodes
                if (infoResult.episodesLink.isNotEmpty()) {
                    val epList = ApiService.fetchExtractorEpisodes(media.provider, infoResult.episodesLink)
                    if (epList.isNotEmpty()) {
                        updatedEpisodes = epList
                    }
                }

                // If movie has direct links in qualities, extract first available stream
                var resolvedStreamUrl = media.streamUrl
                var resolvedDownloadUrl = media.downloadUrl
                var updatedQualities = infoResult.qualities

                val firstDirectLink = infoResult.qualities.firstOrNull { it.directLink.isNotEmpty() }?.directLink
                if (resolvedStreamUrl.isEmpty() && !firstDirectLink.isNullOrEmpty()) {
                    val servers = ApiService.extractStreamServers(media.provider, firstDirectLink)
                    if (servers.isNotEmpty()) {
                        // Pick best streaming server (CF Worker, Pixeldrain, CF Storage)
                        val streamServer = servers.firstOrNull { it.server.contains("CF Worker", true) }
                            ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }
                            ?: servers.firstOrNull { it.server.contains("CF Storage", true) }
                            ?: servers[0]
                        resolvedStreamUrl = streamServer.link

                        // Pick download link (GDrive or first server)
                        val downloadServer = servers.firstOrNull { it.server.contains("GDrive", true) }
                            ?: servers.firstOrNull { it.server.contains("Pixeldrain", true) }
                            ?: servers[0]
                        resolvedDownloadUrl = downloadServer.link
                    }
                }

                return@withContext media.copy(
                    title = if (infoResult.title.isNotEmpty()) infoResult.title else media.title,
                    description = if (infoResult.synopsis.isNotEmpty()) infoResult.synopsis else media.description,
                    posterUrl = if (infoResult.image.isNotEmpty()) infoResult.image else media.posterUrl,
                    backdropUrl = if (infoResult.image.isNotEmpty()) infoResult.image else media.backdropUrl,
                    qualities = updatedQualities,
                    episodes = updatedEpisodes,
                    streamUrl = resolvedStreamUrl,
                    downloadUrl = resolvedDownloadUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving media details: ${e.message}")
        }
        media
    }

    suspend fun resolveStreamUrl(provider: String, directUrl: String): List<StreamServer> = withContext(Dispatchers.IO) {
        ApiService.extractStreamServers(provider, directUrl)
    }
}
