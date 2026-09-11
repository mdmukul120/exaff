package com.example.data.repository

import android.net.Uri
import android.util.Log
import com.example.data.model.SportsMatchItem
import com.example.data.model.SportsSource
import com.example.data.model.SportsStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object SportsRepository {
    private const val TAG = "SportsRepository"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // 1. Live Sports: Upcoming & Live Sports Data
    private const val URL_LIVE_SPORTS =
        "https://raw.githubusercontent.com/sm-monirulislam/Upcoming-and-Live-Sports-Data/main/Sports_data.json?t="

    // 2. Mukul Live Sports: Firebase Realtime Database
    private const val URL_MUKUL_LIVE_SPORTS =
        "https://livefy-tv-64d95-default-rtdb.firebaseio.com/sports_live/events.json"

    // 3. Mukul Tapmad M3U (with resilient fallback to live Gist)
    private const val URL_TAPMAD_PRIMARY =
        "https://raw.githubusercontent.com/srhady/tapmad-bd/refs/heads/main/tapmad_bd.m3u?t=1788308617563"
    private const val URL_TAPMAD_FALLBACK =
        "https://gist.githubusercontent.com/srhady/fc7a6cd99138fa74090822c84cbe8b73/raw/tapmad_bd.m3u"

    // 4. Match Highlights / Replays
    private const val URL_MATCH_HIGHLIGHTS =
        "https://mukul-sports.ai.studio/api/replays/replays.txt"

    // 5. Extra League Live
    private const val URL_EXTRA_LEAGUE =
        "https://raw.githubusercontent.com/mdmukul120/mukul-sportsbd/refs/heads/main/matches.json"

    // 6. Live Cricket (Willow event)
    private const val URL_LIVE_CRICKET =
        "https://raw.githubusercontent.com/srhady/willow-event/refs/heads/main/live_sports.json"

    // 7. Free Live Sports Channels (POWR TV Live 24/7)
    private const val URL_FREE_LIVE_SPORTS =
        "https://ga-prod-api.powr.tv/v2/sites/freelivesports/live-channels/"

    // In-memory caches for fast switching
    private var cachedLiveSports: List<SportsMatchItem>? = null
    private var cachedFreeLiveSports: List<SportsMatchItem>? = null
    private var cachedMukulLiveSports: List<SportsMatchItem>? = null
    private var cachedTapmad: List<SportsMatchItem>? = null
    private var cachedHighlights: List<SportsMatchItem>? = null
    private var cachedExtraLeague: List<SportsMatchItem>? = null
    private var cachedLiveCricket: List<SportsMatchItem>? = null

    suspend fun getSportsForSource(source: SportsSource, forceRefresh: Boolean = false): List<SportsMatchItem> {
        return withContext(Dispatchers.IO) {
            when (source) {
                SportsSource.LIVE_SPORTS -> {
                    if (!forceRefresh && cachedLiveSports != null) return@withContext cachedLiveSports!!
                    fetchLiveSports().also { cachedLiveSports = it }
                }
                SportsSource.FREE_LIVE_SPORTS -> {
                    if (!forceRefresh && cachedFreeLiveSports != null) return@withContext cachedFreeLiveSports!!
                    fetchFreeLiveSportsChannels().also { cachedFreeLiveSports = it }
                }
                SportsSource.MUKUL_LIVE_SPORTS -> {
                    if (!forceRefresh && cachedMukulLiveSports != null) return@withContext cachedMukulLiveSports!!
                    fetchMukulLiveSports().also { cachedMukulLiveSports = it }
                }
                SportsSource.MUKUL_TAPMAD -> {
                    if (!forceRefresh && cachedTapmad != null) return@withContext cachedTapmad!!
                    fetchMukulTapmad().also { cachedTapmad = it }
                }
                SportsSource.MATCH_HIGHLIGHTS -> {
                    if (!forceRefresh && cachedHighlights != null) return@withContext cachedHighlights!!
                    fetchMatchHighlights().also { cachedHighlights = it }
                }
                SportsSource.EXTRA_LEAGUE -> {
                    if (!forceRefresh && cachedExtraLeague != null) return@withContext cachedExtraLeague!!
                    fetchExtraLeague().also { cachedExtraLeague = it }
                }
                SportsSource.LIVE_CRICKET -> {
                    if (!forceRefresh && cachedLiveCricket != null) return@withContext cachedLiveCricket!!
                    fetchLiveCricket().also { cachedLiveCricket = it }
                }
            }
        }
    }

    suspend fun preloadAllSports(): Map<SportsSource, List<SportsMatchItem>> = coroutineScope {
        val liveDeferred = async { getSportsForSource(SportsSource.LIVE_SPORTS) }
        val freeLiveDeferred = async { getSportsForSource(SportsSource.FREE_LIVE_SPORTS) }
        val mukulDeferred = async { getSportsForSource(SportsSource.MUKUL_LIVE_SPORTS) }
        val tapmadDeferred = async { getSportsForSource(SportsSource.MUKUL_TAPMAD) }
        val highlightsDeferred = async { getSportsForSource(SportsSource.MATCH_HIGHLIGHTS) }
        val extraDeferred = async { getSportsForSource(SportsSource.EXTRA_LEAGUE) }
        val cricketDeferred = async { getSportsForSource(SportsSource.LIVE_CRICKET) }

        mapOf(
            SportsSource.LIVE_SPORTS to liveDeferred.await(),
            SportsSource.FREE_LIVE_SPORTS to freeLiveDeferred.await(),
            SportsSource.MUKUL_LIVE_SPORTS to mukulDeferred.await(),
            SportsSource.MUKUL_TAPMAD to tapmadDeferred.await(),
            SportsSource.MATCH_HIGHLIGHTS to highlightsDeferred.await(),
            SportsSource.EXTRA_LEAGUE to extraDeferred.await(),
            SportsSource.LIVE_CRICKET to cricketDeferred.await()
        )
    }

    // 1. Parser for Live Sports JSON
    private suspend fun fetchLiveSports(): List<SportsMatchItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SportsMatchItem>()
        try {
            val raw = httpGet(URL_LIVE_SPORTS) ?: return@withContext emptyList()
            val root = JSONObject(raw)
            val matches = root.optJSONArray("matches") ?: JSONArray()
            for (i in 0 until matches.length()) {
                val m = matches.getJSONObject(i)
                val status = m.optString("status", "LIVE")
                val category = m.optString("Category", "Sports")
                val eventName = m.optString("event_name", "Live Match")

                val eventInfo = m.optJSONObject("eventInfo")
                val teamA = eventInfo?.optString("teamA").orEmpty()
                val teamB = eventInfo?.optString("teamB").orEmpty()
                val teamAFlag = eventInfo?.optString("teamAFlag").orEmpty()
                val teamBFlag = eventInfo?.optString("teamBFlag").orEmpty()
                val tournament = eventInfo?.optString("eventName").orEmpty()
                val eventLogo = eventInfo?.optString("event_logo").orEmpty()
                val startTime = eventInfo?.optString("startTime").orEmpty()

                val streamsArr = m.optJSONArray("streams") ?: JSONArray()
                val streams = mutableListOf<SportsStream>()
                for (s in 0 until streamsArr.length()) {
                    val st = streamsArr.getJSONObject(s)
                    val channelName = st.optString("channel_name", "Stream ${s + 1}")
                    val streamUrl = st.optString("stream_url")
                    val referer = st.optString("referer")
                    val userAgent = st.optString("user_agent")
                    val headersMap = mutableMapOf<String, String>()
                    if (referer.isNotEmpty()) headersMap["Referer"] = referer
                    if (userAgent.isNotEmpty()) headersMap["User-Agent"] = userAgent

                    if (streamUrl.isNotEmpty()) {
                        val extracted = extractStreamInfo(streamUrl)
                        val combinedHeaders = headersMap + extracted.headers
                        streams.add(
                            SportsStream(
                                title = channelName,
                                streamUrl = extracted.url,
                                serverName = channelName,
                                quality = if (channelName.contains("FHD", true)) "1080p" else if (channelName.contains("4K", true)) "4K" else "HD",
                                headers = combinedHeaders,
                                isEmbed = extracted.isEmbed
                            )
                        )
                    }
                }

                if (streams.isNotEmpty() || eventName.isNotEmpty()) {
                    list.add(
                        SportsMatchItem(
                            id = "live_sports_$i",
                            title = if (teamA.isNotEmpty() && teamB.isNotEmpty()) "$teamA vs $teamB" else eventName,
                            categorySource = SportsSource.LIVE_SPORTS,
                            sportType = category.ifEmpty { "Sports" },
                            tournament = tournament,
                            status = status,
                            startTime = startTime,
                            teamAName = teamA,
                            teamBName = teamB,
                            teamAFlag = teamAFlag,
                            teamBFlag = teamBFlag,
                            bannerUrl = eventLogo,
                            streams = streams
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Live Sports: ${e.message}")
        }
        list
    }

    // 2. Parser for Mukul Live Sports (Firebase)
    private suspend fun fetchMukulLiveSports(): List<SportsMatchItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SportsMatchItem>()
        try {
            val raw = httpGet(URL_MUKUL_LIVE_SPORTS) ?: return@withContext emptyList()
            val trimmed = raw.trim()
            val eventsArray = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                val arr = JSONArray()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val itm = obj.optJSONObject(k)
                    if (itm != null) arr.put(itm)
                }
                arr
            } else {
                JSONArray()
            }

            for (i in 0 until eventsArray.length()) {
                val item = eventsArray.optJSONObject(i) ?: continue
                val id = item.optString("id", "mukul_$i")
                val title = item.optString("title", "Live Sports")
                val category = item.optString("cat", "Sports")
                val leagueLogo = item.optString("league_logo")

                val eventInfo = item.optJSONObject("eventInfo")
                val status = eventInfo?.optString("Status", "LIVE") ?: "LIVE"
                val tournament = eventInfo?.optString("eventName", title) ?: title
                val startTime = eventInfo?.optString("startTime").orEmpty()
                val teamA = eventInfo?.optString("teamA").orEmpty()
                val teamB = eventInfo?.optString("teamB").orEmpty()
                val teamAFlag = eventInfo?.optString("teamAFlag").orEmpty()
                val teamBFlag = eventInfo?.optString("teamBFlag").orEmpty()

                val channelsArr = item.optJSONArray("channels_data") ?: JSONArray()
                val streams = mutableListOf<SportsStream>()

                for (c in 0 until channelsArr.length()) {
                    val ch = channelsArr.getJSONObject(c)
                    val chTitle = ch.optString("title", "Server ${c + 1}")
                    val rawLink = ch.optString("link")

                    // Extract actual stream URL and HTTP headers if wrapped in iframe / HLS-PLAYER
                    val extracted = extractStreamInfo(rawLink)
                    if (extracted.url.isNotEmpty()) {
                        streams.add(
                            SportsStream(
                                title = chTitle,
                                streamUrl = extracted.url,
                                serverName = chTitle,
                                quality = if (chTitle.contains("FHD", true)) "1080p" else if (chTitle.contains("SD", true)) "480p" else "720p",
                                headers = extracted.headers,
                                isEmbed = extracted.isEmbed
                            )
                        )
                    }
                }

                list.add(
                    SportsMatchItem(
                        id = "mukul_live_${id}_$i",
                        title = if (teamA.isNotEmpty() && teamB.isNotEmpty()) "$teamA vs $teamB" else title,
                        categorySource = SportsSource.MUKUL_LIVE_SPORTS,
                        sportType = category,
                        tournament = tournament,
                        status = status,
                        startTime = startTime,
                        teamAName = teamA,
                        teamBName = teamB,
                        teamAFlag = teamAFlag,
                        teamBFlag = teamBFlag,
                        bannerUrl = leagueLogo,
                        streams = streams
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Mukul Live Sports: ${e.message}")
        }
        list
    }

    // 3. Parser for Mukul Tapmad M3U
    private suspend fun fetchMukulTapmad(): List<SportsMatchItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SportsMatchItem>()
        try {
            var raw = httpGet(URL_TAPMAD_PRIMARY)
            if (raw == null || raw.contains("404: Not Found") || !raw.contains("#EXTINF")) {
                raw = httpGet(URL_TAPMAD_FALLBACK)
            }
            if (raw == null) return@withContext emptyList()

            val lines = raw.lines()
            var currentTitle = ""
            var currentLogo = ""
            var currentGroup = "Sports"
            var currentId = ""

            val titlePattern = Pattern.compile("tvg-logo=\"([^\"]*)\"")
            val groupPattern = Pattern.compile("group-title=\"([^\"]*)\"")
            val idPattern = Pattern.compile("tvg-id=\"([^\"]*)\"")

            var counter = 0
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("#EXTINF:")) {
                    val logoMatcher = titlePattern.matcher(trimmed)
                    currentLogo = if (logoMatcher.find()) logoMatcher.group(1).orEmpty() else ""

                    val groupMatcher = groupPattern.matcher(trimmed)
                    currentGroup = if (groupMatcher.find()) groupMatcher.group(1).orEmpty() else "Sports"

                    val idMatcher = idPattern.matcher(trimmed)
                    currentId = if (idMatcher.find()) idMatcher.group(1).orEmpty() else "tapmad_$counter"

                    val commaIndex = trimmed.lastIndexOf(',')
                    currentTitle = if (commaIndex != -1) trimmed.substring(commaIndex + 1).trim() else "Tapmad Live"
                } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val streamUrl = trimmed
                    if (currentTitle.isNotEmpty() && streamUrl.isNotEmpty()) {
                        counter++
                        val teams = splitTeams(currentTitle)

                        list.add(
                            SportsMatchItem(
                                id = "tapmad_${currentId}_$counter",
                                title = currentTitle,
                                categorySource = SportsSource.MUKUL_TAPMAD,
                                sportType = currentGroup,
                                tournament = "Tapmad Exclusive",
                                status = "LIVE",
                                startTime = "Live Now",
                                teamAName = teams.first,
                                teamBName = teams.second,
                                bannerUrl = currentLogo,
                                streams = listOf(
                                    SportsStream(
                                        title = "Tapmad Live Stream",
                                        streamUrl = streamUrl,
                                        serverName = "Tapmad Server",
                                        quality = "FHD"
                                    )
                                )
                            )
                        )
                    }
                    currentTitle = ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Tapmad M3U: ${e.message}")
        }
        if (list.isEmpty()) {
            list.addAll(
                listOf(
                    SportsMatchItem(
                        id = "tapmad_tsports_live",
                        title = "T Sports HD Live",
                        categorySource = SportsSource.MUKUL_TAPMAD,
                        sportType = "Cricket / Football",
                        tournament = "Bangladesh Sports Hub",
                        status = "LIVE",
                        startTime = "Live 24/7",
                        teamAName = "T Sports",
                        teamBName = "Live TV",
                        bannerUrl = "https://yt3.googleusercontent.com/ytc/AIdro_mQ16jR6E5N0f5e-lYx1v5B8y4l7x0c_3b9=s900-c-k-c0x00ffffff-no-rj",
                        streams = listOf(
                            SportsStream(
                                title = "T Sports FHD",
                                streamUrl = "https://edge01.bioscopeplus.com/live/tsports_hd.m3u8",
                                serverName = "Bioscope Server",
                                quality = "1080p",
                                headers = mapOf("Referer" to "https://www.bioscopeplus.com/")
                            ),
                            SportsStream(
                                title = "T Sports HD Backup",
                                streamUrl = "https://live-cdn.toffeelive.com/cdn/live/tsports/index.m3u8",
                                serverName = "Toffee Edge",
                                quality = "720p",
                                headers = mapOf("Referer" to "https://toffeelive.com/")
                            )
                        )
                    ),
                    SportsMatchItem(
                        id = "tapmad_willow_live",
                        title = "Willow Cricket HD Live",
                        categorySource = SportsSource.MUKUL_TAPMAD,
                        sportType = "Cricket",
                        tournament = "International Cricket 24/7",
                        status = "LIVE",
                        startTime = "Live 24/7",
                        teamAName = "Willow",
                        teamBName = "Cricket",
                        bannerUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/7/7b/Willow_TV_logo.svg/1200px-Willow_TV_logo.svg.png",
                        streams = listOf(
                            SportsStream(
                                title = "Willow HD Server 1",
                                streamUrl = "https://edge02.bioscopeplus.com/live/willow_hd.m3u8",
                                serverName = "Edge Server 1",
                                quality = "1080p",
                                headers = mapOf("Referer" to "https://www.bioscopeplus.com/")
                            )
                        )
                    )
                )
            )
        }
        list
    }

    // 4. Parser for Match Highlights / Replays
    private suspend fun fetchMatchHighlights(): List<SportsMatchItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SportsMatchItem>()
        try {
            val raw = httpGet(URL_MATCH_HIGHLIGHTS) ?: return@withContext emptyList()
            val lines = raw.lines()

            var currentItem: SportsMatchItem? = null
            var currentStreams = mutableListOf<SportsStream>()
            var counter = 0

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("# ")) {
                    // Save previous item
                    currentItem?.let { prev ->
                        list.add(prev.copy(streams = currentStreams.toList()))
                    }
                    currentStreams = mutableListOf()

                    val title = trimmed.substring(2).trim()
                    counter++
                    val teams = splitTeams(title)
                    currentItem = SportsMatchItem(
                        id = "highlight_$counter",
                        title = title,
                        categorySource = SportsSource.MATCH_HIGHLIGHTS,
                        sportType = "Sports",
                        tournament = "Replay / Highlights",
                        status = "Replay",
                        startTime = "Full Match Replay",
                        teamAName = teams.first,
                        teamBName = teams.second,
                        streams = emptyList()
                    )
                } else if (trimmed.startsWith("~ ")) {
                    val meta = trimmed.substring(2).split('\t')
                    if (currentItem != null && meta.isNotEmpty()) {
                        val category = meta.getOrNull(0)?.trim() ?: "Sports"
                        val league = meta.getOrNull(1)?.trim() ?: ""
                        val thumb = meta.getOrNull(2)?.trim() ?: ""
                        val date = meta.getOrNull(3)?.trim() ?: ""

                        currentItem = currentItem.copy(
                            sportType = category.ifEmpty { "Sports" },
                            tournament = league.ifEmpty { currentItem.tournament },
                            bannerUrl = thumb,
                            startTime = date.ifEmpty { currentItem.startTime }
                        )
                    }
                } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("~")) {
                    val parts = trimmed.split('\t')
                    val serverName = parts.getOrNull(0)?.trim() ?: "Server"
                    val type = parts.getOrNull(1)?.trim() ?: "stream"
                    val url = parts.getOrNull(2)?.trim() ?: parts.getOrNull(0)?.trim().orEmpty()

                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        currentStreams.add(
                            SportsStream(
                                title = serverName,
                                streamUrl = url,
                                serverName = serverName,
                                quality = "HD",
                                isEmbed = type.equals("iframe", true)
                            )
                        )
                    }
                }
            }

            // Add final item
            currentItem?.let { prev ->
                list.add(prev.copy(streams = currentStreams.toList()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Match Highlights: ${e.message}")
        }
        list
    }

    // 5. Parser for Extra League Live
    private suspend fun fetchExtraLeague(): List<SportsMatchItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SportsMatchItem>()
        try {
            val raw = httpGet(URL_EXTRA_LEAGUE) ?: return@withContext emptyList()
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val matchName = obj.optString("match_name")
                val status = obj.optString("status", "Live")
                var image = obj.optString("image")
                val playerUrl = obj.optString("player_url")
                val matchLink = obj.optString("match_link")

                if (image.startsWith("/")) {
                    image = "https://cricketlounge.tv$image"
                }

                val teams = splitTeams(matchName)
                val streams = mutableListOf<SportsStream>()
                if (playerUrl.isNotEmpty()) {
                    streams.add(
                        SportsStream(
                            title = "Main Player",
                            streamUrl = playerUrl,
                            serverName = "Decimal Server",
                            quality = "Live",
                            isEmbed = true
                        )
                    )
                }
                if (matchLink.isNotEmpty() && matchLink != playerUrl) {
                    streams.add(
                        SportsStream(
                            title = "Match Feed",
                            streamUrl = matchLink,
                            serverName = "CricketLounge Feed",
                            quality = "HD",
                            isEmbed = true
                        )
                    )
                }

                list.add(
                    SportsMatchItem(
                        id = "extra_league_$i",
                        title = matchName,
                        categorySource = SportsSource.EXTRA_LEAGUE,
                        sportType = "Cricket",
                        tournament = "Super League Live",
                        status = status.ifEmpty { "Live" },
                        startTime = "Live Stream",
                        teamAName = teams.first,
                        teamBName = teams.second,
                        bannerUrl = image,
                        streams = streams,
                        directPlayerUrl = playerUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Extra League: ${e.message}")
        }
        if (list.isEmpty()) {
            try {
                val fallbackRaw = httpGet("https://raw.githubusercontent.com/mdmukul120/mukul-sportsbd/1312eb22/matches.json")
                if (fallbackRaw != null) {
                    val arr = JSONArray(fallbackRaw)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val matchName = obj.optString("match_name")
                        val status = obj.optString("status", "Live")
                        var image = obj.optString("image")
                        val playerUrl = obj.optString("player_url")
                        val matchLink = obj.optString("match_link")
                        if (image.startsWith("/")) {
                            image = "https://cricketlounge.tv$image"
                        }
                        val teams = splitTeams(matchName)
                        val streams = mutableListOf<SportsStream>()
                        if (playerUrl.isNotEmpty()) {
                            streams.add(
                                SportsStream(
                                    title = "Main Player",
                                    streamUrl = playerUrl,
                                    serverName = "Decimal Server",
                                    quality = "Live",
                                    isEmbed = true
                                )
                            )
                        }
                        list.add(
                            SportsMatchItem(
                                id = "extra_fallback_$i",
                                title = matchName,
                                categorySource = SportsSource.EXTRA_LEAGUE,
                                sportType = "Cricket",
                                tournament = "Super League Live",
                                status = status,
                                startTime = "Live Now",
                                teamAName = teams.first,
                                teamBName = teams.second,
                                bannerUrl = image,
                                streams = streams,
                                directPlayerUrl = playerUrl
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback Extra League failed: ${e.message}")
            }
        }
        list
    }

    // 6. Parser for Live Cricket (Willow event)
    private suspend fun fetchLiveCricket(): List<SportsMatchItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SportsMatchItem>()
        try {
            val raw = httpGet(URL_LIVE_CRICKET) ?: return@withContext emptyList()
            val root = JSONObject(raw)
            val matches = root.optJSONArray("Matches") ?: JSONArray()

            for (i in 0 until matches.length()) {
                val m = matches.getJSONObject(i)
                val id = m.optString("match_id", "willow_$i")
                val title = m.optString("title")
                val synopsis = m.optString("synopsis")
                val status = m.optString("status", "LIVE")
                val time = m.optString("time")
                val coverImage = m.optString("cover_image")
                val matchUrl = m.optString("match_url")

                val streams = mutableListOf<SportsStream>()

                // Alpha Servers
                val alpha = m.optJSONObject("stream_url_alpha")
                if (alpha != null) {
                    val keys = alpha.keys()
                    while (keys.hasNext()) {
                        val serverName = keys.next()
                        val url = alpha.optString(serverName)
                        if (url.isNotEmpty()) {
                            streams.add(
                                SportsStream(
                                    title = "$serverName (Alpha)",
                                    streamUrl = url,
                                    serverName = serverName,
                                    quality = "1080p"
                                )
                            )
                        }
                    }
                }

                // Bravo Servers
                val bravo = m.optJSONObject("stream_url_bravo")
                if (bravo != null) {
                    val keys = bravo.keys()
                    while (keys.hasNext()) {
                        val serverName = keys.next()
                        val url = bravo.optString(serverName)
                        if (url.isNotEmpty()) {
                            streams.add(
                                SportsStream(
                                    title = "$serverName (Bravo)",
                                    streamUrl = url,
                                    serverName = serverName,
                                    quality = "720p"
                                )
                            )
                        }
                    }
                }

                val teams = splitTeams(title)

                list.add(
                    SportsMatchItem(
                        id = "willow_$id",
                        title = title.ifEmpty { synopsis },
                        categorySource = SportsSource.LIVE_CRICKET,
                        sportType = "Cricket",
                        tournament = "Willow Cricket",
                        status = status,
                        startTime = time,
                        teamAName = teams.first,
                        teamBName = teams.second,
                        bannerUrl = coverImage,
                        streams = streams,
                        directPlayerUrl = matchUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Live Cricket: ${e.message}")
        }
        list
    }

    data class ExtractedStream(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val isEmbed: Boolean = false
    )

    private fun extractStreamInfo(input: String): ExtractedStream {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ExtractedStream("")

        var targetUrl = trimmed
        if (trimmed.contains("<iframe", true)) {
            val srcPattern = Pattern.compile("src=[\"']([^\"']+)[\"']")
            val matcher = srcPattern.matcher(trimmed)
            if (matcher.find()) {
                targetUrl = matcher.group(1).orEmpty()
            }
        }

        val headers = mutableMapOf<String, String>()
        var cleanUrl = targetUrl
        var isEmbed = false

        if (targetUrl.contains("play=")) {
            val playIndex = targetUrl.indexOf("play=")
            val substr = targetUrl.substring(playIndex + 5)
            val tokens = substr.split('|')
            cleanUrl = tokens[0].trim()
            for (i in 1 until tokens.size) {
                val token = tokens[i]
                if (token.contains("=")) {
                    val kv = token.split("=", limit = 2)
                    headers[kv[0].trim()] = kv[1].trim()
                }
            }
        } else if (targetUrl.contains("|")) {
            val tokens = targetUrl.split('|')
            cleanUrl = tokens[0].trim()
            for (i in 1 until tokens.size) {
                val token = tokens[i]
                if (token.contains("=")) {
                    val kv = token.split("=", limit = 2)
                    headers[kv[0].trim()] = kv[1].trim()
                }
            }
        }

        if (targetUrl.contains("decimalsports.com", true) ||
            targetUrl.contains("ok.ru", true) ||
            targetUrl.contains("soccerfull", true) ||
            targetUrl.contains("dailymotion", true) ||
            targetUrl.contains("cricketlounge.tv", true)
        ) {
            isEmbed = true
            cleanUrl = targetUrl
        }

        return ExtractedStream(
            url = cleanUrl,
            headers = headers,
            isEmbed = isEmbed
        )
    }

    private fun splitTeams(title: String): Pair<String, String> {
        val separators = listOf(" vs ", " Vs ", " VS ", " v ", " V ", " - ")
        for (sep in separators) {
            if (title.contains(sep)) {
                val parts = title.split(sep)
                if (parts.size >= 2) {
                    return Pair(parts[0].trim(), parts[1].trim())
                }
            }
        }
        return Pair(title, "")
    }

    // 7. Parser for Free Live Sports Channels (POWR TV Live 24/7)
    private suspend fun fetchFreeLiveSportsChannels(): List<SportsMatchItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<SportsMatchItem>()
        try {
            val raw = httpGet(URL_FREE_LIVE_SPORTS) ?: return@withContext emptyList()
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val ch = array.getJSONObject(i)
                val name = ch.optString("name", "Sports Channel")
                val streamUrl = ch.optString("url", "")
                if (streamUrl.isEmpty()) continue

                val desc = ch.optString("description", "")
                val id = ch.optString("_id", ch.optString("id", "fls_$i"))
                val thumbObj = ch.optJSONObject("thumbnails")
                val thumb = ch.optString("thumbnail").ifEmpty {
                    thumbObj?.optString("light")?.ifEmpty {
                        thumbObj.optString("dark", "")
                    }.orEmpty()
                }

                val catLower = (name + " " + desc).lowercase()
                val sportCategory = when {
                    catLower.contains("cricket") -> "Cricket"
                    catLower.contains("football") || catLower.contains("soccer") || catLower.contains("man city") || catLower.contains("fifa") -> "Football"
                    catLower.contains("tennis") -> "Tennis"
                    catLower.contains("basketball") || catLower.contains("overtime") || catLower.contains("nba") -> "Basketball"
                    catLower.contains("pfl") || catLower.contains("fight") || catLower.contains("mma") || catLower.contains("boxing") || catLower.contains("wrestling") -> "Combat / MMA"
                    catLower.contains("racing") || catLower.contains("motorsport") || catLower.contains("nascar") || catLower.contains("f1") || catLower.contains("motor") -> "Motorsports"
                    catLower.contains("poker") || catLower.contains("wpt") || catLower.contains("billiards") || catLower.contains("pool") -> "Poker & Cue"
                    catLower.contains("golf") -> "Golf"
                    catLower.contains("outdoor") || catLower.contains("hunt") || catLower.contains("fish") -> "Outdoor"
                    else -> "Sports 24/7"
                }

                list.add(
                    SportsMatchItem(
                        id = "fls_$id",
                        title = name,
                        categorySource = SportsSource.FREE_LIVE_SPORTS,
                        sportType = sportCategory,
                        tournament = "Free Live Sports 24/7",
                        status = "LIVE",
                        startTime = "24/7 সরাসরি",
                        teamAName = name,
                        bannerUrl = thumb,
                        streams = listOf(
                            SportsStream(
                                title = "$name (1080p HD)",
                                streamUrl = streamUrl,
                                serverName = "FreeLiveSports Server",
                                quality = "HD 1080p"
                            )
                        ),
                        directPlayerUrl = streamUrl
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Free Live Sports: ${e.message}")
        }
        list
    }

    private suspend fun httpGet(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.string()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP GET failed for $url: ${e.message}")
        }
        null
    }
}
