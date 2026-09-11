package com.example.data.api

import android.net.Uri
import android.util.Log
import com.example.data.model.EpisodeItem
import com.example.data.model.HiddenVideoItem
import com.example.data.model.LiveChannel
import com.example.data.model.MediaItem
import com.example.data.model.MediaQuality
import com.example.data.model.StreamServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ExtractorInfoResult(
    val title: String,
    val synopsis: String,
    val image: String,
    val imdbId: String,
    val type: String,
    val qualities: List<MediaQuality>,
    val episodesLink: String = ""
)

object ApiService {
    private const val TAG = "MukulApiService"
    private const val EXTRACTOR_BASE = "https://api.sorrybrorewards.com/v2/extractor/api"
    const val BONGO_WORKER_BASE = "https://hamyra-api.mdibrahimkhalil516.workers.dev"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // 1. Fetch Bongo Scraped Catalog directly from Cloudflare Worker
    suspend fun fetchBongoScrape(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BONGO_WORKER_BASE/bongo/scrape")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.string()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Bongo scrape: ${e.message}")
        }
        null
    }

    // Fetch Bongo Content Metadata
    suspend fun fetchBongoMeta(contentId: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BONGO_WORKER_BASE/bongo/meta?id=$contentId")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.string()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Bongo meta: ${e.message}")
        }
        null
    }

    // Fetch Bongo Show Episodes
    suspend fun fetchBongoEpisodes(systemId: String): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<EpisodeItem>()
        try {
            val request = Request.Builder()
                .url("$BONGO_WORKER_BASE/bongo/show-episodes?systemId=$systemId")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val raw = response.body?.string().orEmpty()
                    val root = JSONObject(raw)
                    val data = root.optJSONArray("data") ?: root.optJSONArray("episodes") ?: JSONArray()
                    for (i in 0 until data.length()) {
                        val ep = data.getJSONObject(i)
                        val epId = ep.optString("id", "ep_$i")
                        val title = ep.optString("title", "Episode ${i + 1}")
                        val duration = ep.optString("duration", "40m")
                        val thumb = ep.optString("thumbnail").ifEmpty { ep.optString("image") }
                        val stream = "$BONGO_WORKER_BASE/bongo/hls?id=$epId"
                        list.add(
                            EpisodeItem(
                                id = epId,
                                title = title,
                                episodeNumber = i + 1,
                                duration = duration,
                                thumbnail = thumb,
                                streamUrl = stream,
                                provider = "bongo",
                                link = epId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Bongo episodes: ${e.message}")
        }
        list
    }

    // Helper to get Bongo HLS Stream URL
    fun getBongoHlsUrl(contentOrEpisodeId: String): String {
        return "$BONGO_WORKER_BASE/bongo/hls?id=$contentOrEpisodeId"
    }

    // Helper to get Universal Video Proxy URL
    fun getUniversalVideoProxy(directUrl: String): String {
        val encoded = Uri.encode(directUrl)
        return "$BONGO_WORKER_BASE/proxy/video?url=$encoded"
    }

    // Helper to get Universal HLS Proxy URL
    fun getUniversalHlsProxy(m3u8Url: String): String {
        val encoded = Uri.encode(m3u8Url)
        return "$BONGO_WORKER_BASE/proxy/hls?url=$encoded"
    }

    // 1. Ping Proxy & Tracking Pixel
    suspend fun pingServices() = withContext(Dispatchers.IO) {
        try {
            val pingReq = Request.Builder()
                .url("https://streambd-proxy.aininjaibrahim.workers.dev/?ping=1")
                .header("User-Agent", "MukulPlus/2.0")
                .build()
            client.newCall(pingReq).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Proxy ping skipped: ${e.message}")
        }

        try {
            val pixelReq = Request.Builder()
                .url("https://sleepoverlimitprofound.com/pixel/ase")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(pixelReq).execute().close()
        } catch (e: Exception) {
            Log.w(TAG, "Pixel tracking skipped: ${e.message}")
        }
    }

    // 2. Fetch Bioscope+ Live API
    suspend fun fetchBioscopePage(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api-static.bioscopelive.com/v2?language=en&country=BD&platform=web")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Origin", "https://www.bioscopeplus.com")
                .header("Referer", "https://www.bioscopeplus.com/")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.string()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Bioscope API: ${e.message}")
        }
        null
    }

    // 3. Fetch IPTV M3U Playlist & Prioritize Bangla Channels First
    suspend fun fetchIptvPlaylist(): List<LiveChannel> = withContext(Dispatchers.IO) {
        val allChannels = mutableListOf<LiveChannel>()
        try {
            val request = Request.Builder()
                .url("https://raw.githubusercontent.com/abusaeeidx/Ayna-BDIX-IPTV-Playlist/refs/heads/main/ayna-playlist.m3u")
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val m3uContent = response.body?.string().orEmpty()
                    allChannels.addAll(parseM3u(m3uContent))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IPTV playlist: ${e.message}")
        }

        // Sort so Bangla channels are ALWAYS first
        val banglaChannels = allChannels.filter { it.group.equals("Bangla", ignoreCase = true) }
        val otherChannels = allChannels.filter { !it.group.equals("Bangla", ignoreCase = true) }
        banglaChannels + otherChannels
    }

    private fun parseM3u(content: String): List<LiveChannel> {
        val list = mutableListOf<LiveChannel>()
        val lines = content.lines()
        var currentName = ""
        var currentLogo = ""
        var currentGroup = "General"
        var currentId = ""

        val tvgNamePattern = Pattern.compile("tvg-name=\"([^\"]+)\"")
        val tvgLogoPattern = Pattern.compile("tvg-logo=\"([^\"]+)\"")
        val groupPattern = Pattern.compile("group-title=\"([^\"]+)\"")
        val tvgIdPattern = Pattern.compile("tvg-id=\"([^\"]+)\"")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                val groupMatcher = groupPattern.matcher(trimmed)
                if (groupMatcher.find()) {
                    currentGroup = groupMatcher.group(1) ?: "General"
                }

                val logoMatcher = tvgLogoPattern.matcher(trimmed)
                if (logoMatcher.find()) {
                    currentLogo = logoMatcher.group(1) ?: ""
                }

                val idMatcher = tvgIdPattern.matcher(trimmed)
                if (idMatcher.find()) {
                    currentId = idMatcher.group(1) ?: ""
                }

                val nameMatcher = tvgNamePattern.matcher(trimmed)
                if (nameMatcher.find()) {
                    currentName = nameMatcher.group(1) ?: ""
                } else {
                    val commaIndex = trimmed.lastIndexOf(',')
                    if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        currentName = trimmed.substring(commaIndex + 1).trim()
                    }
                }
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                if (currentName.isNotEmpty()) {
                    // Check if it belongs to Bangla group
                    val isBangla = currentGroup.equals("Bangla", ignoreCase = true) ||
                            currentGroup.equals("Indian Bangla", ignoreCase = true) ||
                            currentName.contains("Channel I", ignoreCase = true) ||
                            currentName.contains("NTV", ignoreCase = true) ||
                            currentName.contains("RTV", ignoreCase = true) ||
                            currentName.contains("Somoy", ignoreCase = true) ||
                            currentName.contains("ATN", ignoreCase = true) ||
                            currentName.contains("Bangla Vision", ignoreCase = true) ||
                            currentName.contains("Gazi", ignoreCase = true) ||
                            currentName.contains("Deepto", ignoreCase = true) ||
                            currentName.contains("Maasranga", ignoreCase = true) ||
                            currentName.contains("Ekattor", ignoreCase = true) ||
                            currentName.contains("Jamuna", ignoreCase = true) ||
                            currentName.contains("Duronto", ignoreCase = true)

                    val finalGroup = if (isBangla) "Bangla" else currentGroup.ifEmpty { "General" }

                    list.add(
                        LiveChannel(
                            id = if (currentId.isNotEmpty()) currentId else "ch_${list.size + 1}",
                            name = currentName,
                            logoUrl = currentLogo,
                            group = finalGroup,
                            streamUrl = trimmed
                        )
                    )
                }
                currentName = ""
                currentLogo = ""
                currentGroup = "General"
                currentId = ""
            }
        }
        return list
    }

    // 4. Fetch Extractor Posts from providers (moviesmod, hdhub4u, topmovies, moviesdrive, etc.)
    suspend fun fetchExtractorPosts(provider: String, page: Int = 1): List<MediaItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaItem>()
        try {
            val url = "$EXTRACTOR_BASE/posts?provider=$provider&filter=&page=$page"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string().orEmpty()
                    val root = JSONObject(jsonStr)
                    if (root.optBoolean("success", false)) {
                        val dataArr = root.optJSONArray("data") ?: JSONArray()
                        for (i in 0 until dataArr.length()) {
                            val obj = dataArr.getJSONObject(i)
                            val title = obj.optString("title", "Untitled")
                            val link = obj.optString("link")
                            val image = obj.optString("image")

                            val isSeries = title.contains("Season", ignoreCase = true) ||
                                    title.contains("Series", ignoreCase = true) ||
                                    title.contains("Episode", ignoreCase = true)

                            val category = if (isSeries) "series" else "movies"

                            // Clean clean title
                            val cleanTitle = title.replace(Regex("\\[.*?\\]"), "")
                                .replace(Regex("\\{.*?\\}"), "")
                                .replace(Regex("\\|.*"), "")
                                .trim()

                            list.add(
                                MediaItem(
                                    id = "ext_${provider}_${i}_${cleanTitle.hashCode()}",
                                    title = cleanTitle.ifEmpty { title },
                                    description = title,
                                    category = category,
                                    posterUrl = image,
                                    backdropUrl = image,
                                    streamUrl = "", // extracted on click
                                    rating = "4.8",
                                    year = if (title.contains("2026")) "2026" else if (title.contains("2025")) "2025" else "2024",
                                    duration = if (isSeries) "Series" else "Movie",
                                    genre = if (title.contains("Hindi", ignoreCase = true)) "Hindi / Dubbed" else "Blockbuster",
                                    label = "HD",
                                    provider = provider,
                                    link = link
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching extractor posts for $provider: ${e.message}")
        }
        list
    }

    // Multi-provider cross-page search across all providers (hdhub4u, moviesmod, topmovies, moviesdrive, uhd)
    suspend fun searchAllProviders(query: String, page: Int = 1): List<MediaItem> = withContext(Dispatchers.IO) {
        val providers = listOf("hdhub4u", "moviesmod", "topmovies", "moviesdrive", "uhd")
        val results = mutableListOf<MediaItem>()
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext emptyList()
        val encodedQuery = Uri.encode(cleanQuery)

        coroutineScope {
            val deferredList = providers.map { prov ->
                async {
                    val provItems = mutableListOf<MediaItem>()
                    try {
                        val url = "$EXTRACTOR_BASE/posts?provider=$prov&filter=$encodedQuery&page=$page"
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0")
                            .build()

                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val jsonStr = response.body?.string().orEmpty()
                                val root = JSONObject(jsonStr)
                                if (root.optBoolean("success", false)) {
                                    val dataArr = root.optJSONArray("data") ?: JSONArray()
                                    for (i in 0 until dataArr.length()) {
                                        val obj = dataArr.getJSONObject(i)
                                        val title = obj.optString("title", "Untitled")
                                        val link = obj.optString("link")
                                        val image = obj.optString("image")
                                        val isSeries = title.contains("Season", ignoreCase = true) ||
                                                title.contains("Series", ignoreCase = true) ||
                                                title.contains("Episode", ignoreCase = true)
                                        val cleanTitle = title.replace(Regex("\\[.*?\\]"), "")
                                            .replace(Regex("\\{.*?\\}"), "")
                                            .replace(Regex("\\|.*"), "")
                                            .trim()

                                        provItems.add(
                                            MediaItem(
                                                id = "search_${prov}_${page}_${i}_${cleanTitle.hashCode()}",
                                                title = cleanTitle.ifEmpty { title },
                                                description = title,
                                                category = if (isSeries) "series" else "movies",
                                                posterUrl = image,
                                                backdropUrl = image,
                                                streamUrl = "",
                                                rating = "4.8",
                                                year = if (title.contains("2026")) "2026" else if (title.contains("2025")) "2025" else "2024",
                                                duration = if (isSeries) "Series" else "HD Movie",
                                                genre = "Multi-Provider ($prov)",
                                                label = prov.uppercase(),
                                                provider = prov,
                                                link = link
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Search provider $prov error: ${e.message}")
                    }
                    provItems
                }
            }

            deferredList.forEach { deferred ->
                results.addAll(deferred.await())
            }
        }
        results.distinctBy { it.title.lowercase() }
    }

    // 5. Fetch Extractor Info for movie / series details and direct links
    suspend fun fetchExtractorInfo(provider: String, link: String): ExtractorInfoResult? = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("provider", provider)
                put("link", link)
            }.toString()

            val request = Request.Builder()
                .url("$EXTRACTOR_BASE/info")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val root = JSONObject(response.body?.string().orEmpty())
                    if (root.optBoolean("success", false)) {
                        val data = root.optJSONObject("data") ?: return@withContext null
                        val title = data.optString("title", "")
                        val synopsis = data.optString("synopsis", "")
                        val image = data.optString("image", "")
                        val imdbId = data.optString("imdbId", "")
                        val type = data.optString("type", "movie")

                        val qualities = mutableListOf<MediaQuality>()
                        var episodesLink = ""
                        val linkListArr = data.optJSONArray("linkList") ?: JSONArray()
                        for (i in 0 until linkListArr.length()) {
                            val itemObj = linkListArr.getJSONObject(i)
                            val qTitle = itemObj.optString("title")
                            val qQuality = itemObj.optString("quality", "HD")
                            if (episodesLink.isEmpty()) {
                                episodesLink = itemObj.optString("episodesLink", "")
                            }

                            val directLinksArr = itemObj.optJSONArray("directLinks")
                            val directLink = if (directLinksArr != null && directLinksArr.length() > 0) {
                                directLinksArr.getJSONObject(0).optString("link")
                            } else ""

                            qualities.add(
                                MediaQuality(
                                    title = qTitle,
                                    quality = qQuality,
                                    directLink = directLink
                                )
                            )
                        }

                        return@withContext ExtractorInfoResult(
                            title = title,
                            synopsis = synopsis,
                            image = image,
                            imdbId = imdbId,
                            type = type,
                            qualities = qualities,
                            episodesLink = episodesLink
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching info for $provider: ${e.message}")
        }
        null
    }

    // 6. Fetch Extractor Episodes for Series
    suspend fun fetchExtractorEpisodes(provider: String, episodesUrl: String): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<EpisodeItem>()
        try {
            val jsonBody = JSONObject().apply {
                put("provider", provider)
                put("url", episodesUrl)
            }.toString()

            val request = Request.Builder()
                .url("$EXTRACTOR_BASE/episodes")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val root = JSONObject(response.body?.string().orEmpty())
                    if (root.optBoolean("success", false)) {
                        val dataArr = root.optJSONArray("data") ?: JSONArray()
                        for (i in 0 until dataArr.length()) {
                            val epObj = dataArr.getJSONObject(i)
                            val epTitle = epObj.optString("title", "Episode ${i + 1}")
                            val epLink = epObj.optString("link")

                            list.add(
                                EpisodeItem(
                                    id = "ep_${provider}_${i + 1}",
                                    title = epTitle,
                                    episodeNumber = i + 1,
                                    duration = "45m",
                                    provider = provider,
                                    link = epLink
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching episodes: ${e.message}")
        }
        list
    }

    // 7. Extract playable & downloadable streaming servers
    suspend fun extractStreamServers(provider: String, directUrl: String): List<StreamServer> = withContext(Dispatchers.IO) {
        val servers = mutableListOf<StreamServer>()
        try {
            val jsonBody = JSONObject().apply {
                put("provider", provider)
                put("url", directUrl)
            }.toString()

            val request = Request.Builder()
                .url("$EXTRACTOR_BASE/stream")
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val root = JSONObject(response.body?.string().orEmpty())
                    if (root.optBoolean("success", false)) {
                        val data = root.optJSONObject("data")
                        val serversArr = data?.optJSONArray("servers") ?: JSONArray()
                        for (i in 0 until serversArr.length()) {
                            val sObj = serversArr.getJSONObject(i)
                            val serverName = sObj.optString("server", "Server")
                            val link = sObj.optString("link")
                            val type = sObj.optString("type", "mkv")
                            if (link.isNotEmpty()) {
                                servers.add(StreamServer(server = serverName, link = link, type = type))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting stream: ${e.message}")
        }
        servers
    }

    // Fetch scraped MP4 videos for the password-locked secret folder
    suspend fun fetchHiddenFolderVideos(): List<HiddenVideoItem> = withContext(Dispatchers.IO) {
        val url = "https://raw.githubusercontent.com/mdmukul120/Gubara/refs/heads/main/scraped_videos.json"
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string().orEmpty()
                    if (jsonStr.isNotEmpty()) {
                        val jsonArray = JSONArray(jsonStr)
                        val result = mutableListOf<HiddenVideoItem>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val title = obj.optString("title", "Video ${i + 1}")
                            val slugUrl = obj.optString("slug_url", "")
                            val imageUrl = obj.optString("image_url", "")
                            val mp4Array = obj.optJSONArray("mp4_videos")
                            val videoList = mutableListOf<String>()
                            if (mp4Array != null) {
                                for (j in 0 until mp4Array.length()) {
                                    val vUrl = mp4Array.optString(j)
                                    if (!vUrl.isNullOrBlank()) {
                                        videoList.add(vUrl)
                                    }
                                }
                            }
                            if (videoList.isNotEmpty() || imageUrl.isNotEmpty()) {
                                result.add(
                                    HiddenVideoItem(
                                        title = title,
                                        imageUrl = imageUrl,
                                        slugUrl = slugUrl,
                                        videoUrls = videoList
                                    )
                                )
                            }
                        }
                        return@withContext result
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching hidden videos: ${e.message}")
        }
        emptyList()
    }
}
