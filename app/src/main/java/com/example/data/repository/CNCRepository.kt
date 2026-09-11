package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.CNCPluginItem
import com.example.data.model.CNCRepoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object CNCRepository {
    private const val TAG = "CNCRepository"
    const val CNC_REPO_URL = "https://raw.githubusercontent.com/NivinCNC/CNCVerse-Cloud-Stream-Extension/refs/heads/builds/CNC.json"
    private const val PREFS_NAME = "cnc_plugins_prefs"
    private const val KEY_UNINSTALLED = "uninstalled_plugins"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // In-memory cache for ultra-responsive navigation
    private var cachedRepo: CNCRepoInfo? = null
    private var cachedPlugins: List<CNCPluginItem>? = null

    suspend fun loadRepoAndPlugins(
        context: Context,
        forceRefresh: Boolean = false
    ): Pair<CNCRepoInfo, List<CNCPluginItem>> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedRepo != null && cachedPlugins != null) {
            val uninstalled = getUninstalledSet(context)
            val updated = cachedPlugins!!.map { it.copy(isInstalled = !uninstalled.contains(it.internalName)) }
            return@withContext Pair(cachedRepo!!, updated)
        }

        // 1. Fetch or load Repo Info
        var repoInfo = if (forceRefresh) fetchRemoteRepo() else null
        if (repoInfo == null) {
            repoInfo = fetchRemoteRepo() ?: loadLocalRepo(context)
        }

        // 2. Fetch or load Plugins from pluginLists
        var pluginsList: List<CNCPluginItem> = emptyList()
        val pluginUrls = repoInfo.pluginLists.ifEmpty {
            listOf("https://raw.githubusercontent.com/NivinCNC/CNCVerse-Cloud-Stream-Extension/builds/plugins.json")
        }

        for (url in pluginUrls) {
            val fromNetwork = if (forceRefresh) fetchRemotePlugins(url) else null
            val items = fromNetwork ?: fetchRemotePlugins(url) ?: loadLocalPlugins(context)
            if (items.isNotEmpty()) {
                pluginsList = pluginsList + items
            }
        }

        if (pluginsList.isEmpty()) {
            pluginsList = loadLocalPlugins(context)
        }

        // Apply installed flags (default: all installed!)
        val uninstalled = getUninstalledSet(context)
        val finalPlugins = pluginsList.distinctBy { it.internalName }.map {
            it.copy(isInstalled = !uninstalled.contains(it.internalName))
        }

        cachedRepo = repoInfo
        cachedPlugins = finalPlugins

        Pair(repoInfo, finalPlugins)
    }

    private fun fetchRemoteRepo(): CNCRepoInfo? {
        return try {
            val request = Request.Builder()
                .url(CNC_REPO_URL)
                .header("User-Agent", "Mozilla/5.0 (Android; CloudStream)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        parseRepoJson(body)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote CNC repo: ${e.message}")
            null
        }
    }

    private fun loadLocalRepo(context: Context): CNCRepoInfo {
        return try {
            context.assets.open("cnc_repo.json").use { stream ->
                val json = InputStreamReader(stream).readText()
                parseRepoJson(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local CNC repo: ${e.message}")
            CNCRepoInfo()
        }
    }

    private fun parseRepoJson(jsonStr: String): CNCRepoInfo {
        val json = JSONObject(jsonStr)
        val pluginLists = mutableListOf<String>()
        val arr = json.optJSONArray("pluginLists")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                pluginLists.add(arr.getString(i))
            }
        }
        return CNCRepoInfo(
            name = json.optString("name", "CNC Repo(All Language)"),
            iconUrl = json.optString("iconUrl", "https://raw.githubusercontent.com/NivinCNC/CNCVerse-Cloud-Stream-Extension/refs/heads/builds/cnc.png"),
            description = json.optString("description", "All Language Contents"),
            manifestVersion = json.optInt("manifestVersion", 1),
            pluginLists = if (pluginLists.isNotEmpty()) pluginLists else listOf("https://raw.githubusercontent.com/NivinCNC/CNCVerse-Cloud-Stream-Extension/builds/plugins.json")
        )
    }

    private fun fetchRemotePlugins(url: String): List<CNCPluginItem>? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; CloudStream)")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        parsePluginsJson(body)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote plugins: ${e.message}")
            null
        }
    }

    private fun loadLocalPlugins(context: Context): List<CNCPluginItem> {
        return try {
            context.assets.open("cnc_plugins.json").use { stream ->
                val json = InputStreamReader(stream).readText()
                parsePluginsJson(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local plugins: ${e.message}")
            emptyList()
        }
    }

    private fun parsePluginsJson(jsonStr: String): List<CNCPluginItem> {
        val result = mutableListOf<CNCPluginItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val authorsList = mutableListOf<String>()
                val authorsArr = obj.optJSONArray("authors")
                if (authorsArr != null) {
                    for (a in 0 until authorsArr.length()) {
                        authorsList.add(authorsArr.getString(a))
                    }
                }

                val typesList = mutableListOf<String>()
                val typesArr = obj.optJSONArray("tvTypes")
                if (typesArr != null) {
                    for (t in 0 until typesArr.length()) {
                        typesList.add(typesArr.getString(t))
                    }
                }

                val lang = if (obj.has("language") && !obj.isNull("language")) {
                    obj.optString("language").takeIf { it.isNotBlank() }
                } else null

                val item = CNCPluginItem(
                    name = obj.optString("name", "Unknown Plugin"),
                    internalName = obj.optString("internalName", obj.optString("name")),
                    description = obj.optString("description", ""),
                    version = obj.optInt("version", 1),
                    apiVersion = obj.optInt("apiVersion", 1),
                    language = lang,
                    authors = authorsList,
                    tvTypes = typesList,
                    fileSize = obj.optLong("fileSize", 0L),
                    status = obj.optInt("status", 1),
                    iconUrl = obj.optString("iconUrl", ""),
                    url = obj.optString("url", ""),
                    repositoryUrl = obj.optString("repositoryUrl", ""),
                    fileHash = obj.optString("fileHash", ""),
                    isInstalled = true
                )
                result.add(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing plugins JSON: ${e.message}")
        }
        return result
    }

    private fun getUninstalledSet(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_UNINSTALLED, emptySet()) ?: emptySet()
    }

    fun setPluginInstalled(context: Context, internalName: String, installed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_UNINSTALLED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (installed) {
            current.remove(internalName)
        } else {
            current.add(internalName)
        }
        prefs.edit().putStringSet(KEY_UNINSTALLED, current).apply()

        cachedPlugins = cachedPlugins?.map {
            if (it.internalName == internalName) it.copy(isInstalled = installed) else it
        }
    }

    fun installAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_UNINSTALLED).apply()
        cachedPlugins = cachedPlugins?.map { it.copy(isInstalled = true) }
    }

    fun uninstallAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val allNames = cachedPlugins?.map { it.internalName }?.toSet() ?: emptySet()
        prefs.edit().putStringSet(KEY_UNINSTALLED, allNames).apply()
        cachedPlugins = cachedPlugins?.map { it.copy(isInstalled = false) }
    }

    fun getCuratedContent(): List<com.example.data.model.CNCContentItem> {
        return listOf(
            // ANIME
            com.example.data.model.CNCContentItem(
                id = "cnc_anime_1",
                title = "Solo Leveling: Season 2 (Arise)",
                banglaTitle = "সোলো লেভেলিং (সিজন ২)",
                category = "anime",
                providerName = "AniKoto",
                posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                rating = "9.2",
                year = "2024",
                duration = "Ep 12 • 24m",
                quality = "1080p FHD",
                description = "In a world where hunters must battle deadly monsters to protect humanity, Sung Jinwoo rises from the weakest to the strongest shadow monarch.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_anime_2",
                title = "Jujutsu Kaisen: Shibuya Incident",
                banglaTitle = "জুজুৎসু কাইসেন (শিবুয়া ইনসিডেন্ট)",
                category = "anime",
                providerName = "AniKoto",
                posterUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                rating = "9.0",
                year = "2023",
                duration = "Ep 23 • 24m",
                quality = "1080p FHD",
                description = "The cursed spirits unleash an all-out assault on Halloween in Shibuya, sealing Gojo Satoru and pushing sorcerers to their absolute limits.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_anime_3",
                title = "Demon Slayer: Hashira Training Arc",
                banglaTitle = "ডিমন স্লেয়ার (হাশীরা ট্রেনিং)",
                category = "anime",
                providerName = "AnimeSuge",
                posterUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                rating = "8.9",
                year = "2024",
                duration = "Ep 8 • 25m",
                quality = "1080p FHD",
                description = "Tanjiro and the Demon Slayer Corps undergo grueling rigorous training led by the Hashira in preparation for the final impending showdown with Muzan.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_anime_4",
                title = "One Piece: Egghead Island Arc",
                banglaTitle = "ওয়ান পিস (এগহেড আইল্যান্ড)",
                category = "anime",
                providerName = "AniKoto",
                posterUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                rating = "9.4",
                year = "2024",
                duration = "Ep 1100+ • 24m",
                quality = "1080p FHD",
                description = "Luffy and the Straw Hats arrive at the futuristic island of Dr. Vegapunk, uncovering shocking secrets of the Void Century and world government.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_anime_5",
                title = "Attack on Titan: The Final Season",
                banglaTitle = "অ্যাটাক অন টাইটান (ফাইনাল সিজন)",
                category = "anime",
                providerName = "AnimeSuge",
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                rating = "9.1",
                year = "2023",
                duration = "Special • 1h 25m",
                quality = "1080p FHD",
                description = "The fate of the world hangs in the balance as Eren Yeager initiates the Rumbling to protect Eldia from total destruction.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_anime_6",
                title = "Suzume no Tojimari (Full Movie)",
                banglaTitle = "সুজুমে নো তোজিমারি (মুভি)",
                category = "anime",
                providerName = "BilibiliProvider",
                posterUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
                rating = "8.8",
                year = "2023",
                duration = "2h 02m",
                quality = "4K UHD",
                description = "A 17-year-old girl named Suzume helps a mysterious young man close mystical doors that are releasing disasters all across Japan.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_anime_7",
                title = "Chainsaw Man (Season 1)",
                banglaTitle = "চেইনস ম্যান",
                category = "anime",
                providerName = "HDrezka",
                posterUrl = "https://images.unsplash.com/photo-1569701813229-33284b643e3c?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                rating = "8.7",
                year = "2022",
                duration = "Ep 12 • 25m",
                quality = "1080p FHD",
                description = "Denji is a young man trapped in poverty, paying off debts by harvesting devil corpses with his devil dog Pochita until he transforms into Chainsaw Man.",
                isLive = false
            ),

            // MOVIES & SERIES
            com.example.data.model.CNCContentItem(
                id = "cnc_movie_1",
                title = "Deadpool & Wolverine (2024)",
                banglaTitle = "ডেডপুল অ্যান্ড উলভারিন (২০২৪)",
                category = "movies",
                providerName = "CastleTv",
                posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                rating = "8.1",
                year = "2024",
                duration = "2h 08m",
                quality = "1080p FHD",
                description = "Wolverine is recovering from his injuries when he crosses paths with the loudmouth, Deadpool. They team up to defeat a common enemy.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_movie_2",
                title = "Dune: Part Two (2024)",
                banglaTitle = "ডিউন: পার্ট টু (২০২৪)",
                category = "movies",
                providerName = "CNC Verse",
                posterUrl = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                rating = "8.6",
                year = "2024",
                duration = "2h 46m",
                quality = "4K IMAX",
                description = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_movie_3",
                title = "Kalki 2898 AD (2024)",
                banglaTitle = "কল্কি ২৮৯৮ এডি (২০২৪)",
                category = "movies",
                providerName = "DoFlix",
                posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                rating = "8.2",
                year = "2024",
                duration = "3h 01m",
                quality = "1080p FHD",
                description = "Set in a post-apocalyptic world in 2898 AD, a modern avatar of Vishnu descends on earth to protect the world from dark and evil forces.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_movie_4",
                title = "Surongo (সুড়ঙ্গ - বাংলা মুভি)",
                banglaTitle = "সুড়ঙ্গ (বাংলা ফুল মুভি)",
                category = "movies",
                providerName = "MLSBDProvider",
                posterUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                rating = "8.4",
                year = "2023",
                duration = "2h 28m",
                quality = "1080p HD",
                description = "একটি ব্যাংক ডাকাতির গল্প যেখানে এক ব্যক্তি ভালোবাসা ও প্রতিশোধের তাগিদে অবিশ্বাস্য এক সুড়ঙ্গ তৈরি করে। আফরান নিশো অভিনীত ব্লকবাস্টার বাংলা ছবি।",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_movie_5",
                title = "Priyotoma (প্রিয়তমা - বাংলা মুভি)",
                banglaTitle = "প্রিয়তমা (শাকিব খান ব্লকবাস্টার)",
                category = "movies",
                providerName = "MovieLinkBDProvider",
                posterUrl = "https://images.unsplash.com/photo-1574267432553-4b4628081c31?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                rating = "8.6",
                year = "2023",
                duration = "2h 30m",
                quality = "1080p HD",
                description = "শাকিব খান ও ইধিকা পাল অভিনীত তুমুল জনপ্রিয় রোমান্টিক ট্র্যাজেডি চলচ্চিত্র। এক তরুণের আত্মত্যাগ ও অসীম ভালোবাসার চিরন্তন রূপ।",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_movie_6",
                title = "Hawa (হাওয়া - বাংলা মুভি)",
                banglaTitle = "হাওয়া (চঞ্চল চৌধুরী)",
                category = "movies",
                providerName = "MLSBDProvider",
                posterUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
                rating = "8.5",
                year = "2022",
                duration = "2h 11m",
                quality = "1080p HD",
                description = "মাঝ সমুদ্রে মাছ ধরার ট্রলার ও তাতে ধরা পড়া রহস্যময় এক নারী। মেজবাউর রহমান সুমন পরিচালিত ও চঞ্চল চৌধুরী অভিনীত রূপকথা-ধর্মী থ্রিলার।",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_movie_7",
                title = "Oppenheimer (2023)",
                banglaTitle = "ওপেনহাইমার (অস্কার বিজয়ী)",
                category = "movies",
                providerName = "StreamFlix",
                posterUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                rating = "8.9",
                year = "2023",
                duration = "3h 00m",
                quality = "4K Ultra HD",
                description = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_movie_8",
                title = "12th Fail (2023)",
                banglaTitle = "টুয়েলভথ ফেল",
                category = "movies",
                providerName = "CineTv",
                posterUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                rating = "9.0",
                year = "2023",
                duration = "2h 27m",
                quality = "1080p FHD",
                description = "Based on the real-life story of IPS officer Manoj Kumar Sharma who restarts his academic journey to crack the toughest UPSC examination.",
                isLive = false
            ),

            // LIVE SPORTS & TV
            com.example.data.model.CNCContentItem(
                id = "cnc_live_1",
                title = "T20 & ICC Live Cricket HD Server 1",
                banglaTitle = "টি-টোয়েন্টি ও আইসিসি লাইভ ক্রিকেট সার্ভার ১",
                category = "live",
                providerName = "CricifyProvider",
                posterUrl = "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://edge01.bioscopeplus.com/live/tsports_hd.m3u8",
                rating = "Live",
                year = "2024",
                duration = "24/7 Live HD",
                quality = "1080p 50FPS",
                description = "CricifyProvider লাইভ ক্রিকেট ব্রডকাস্ট। আইসিসি ম্যাচ, বিপিএল, আইপিএল ও দ্বিপাক্ষিক সিরিজ সরাসরি দেখুন বাফারলেস গতিতে।",
                isLive = true
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_live_2",
                title = "T Sports Live HD (Bangladesh Sports)",
                banglaTitle = "টি স্পোর্টস লাইভ এইচডি (বাংলাদেশ স্পোর্টস)",
                category = "live",
                providerName = "SportzxProvider",
                posterUrl = "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://live-cdn.toffeelive.com/cdn/live/tsports/index.m3u8",
                rating = "Live",
                year = "2024",
                duration = "24/7 Live",
                quality = "720p HD",
                description = "বাংলাদেশের একমাত্র প্রিমিয়ার স্পোর্টস চ্যানেল টি স্পোর্টস। ক্রিকেট, ফুটবল এবং আন্তর্জাতিক ক্রীড়া প্রতিযোগিতা লাইভ।",
                isLive = true
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_live_3",
                title = "Gazi TV (GTV) Live Cricket",
                banglaTitle = "জিটিভি লাইভ ক্রিকেট",
                category = "live",
                providerName = "CricifyProvider",
                posterUrl = "https://images.unsplash.com/photo-1531415074868-036b1c5c53ec?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://edge02.bioscopeplus.com/live/willow_hd.m3u8",
                rating = "Live",
                year = "2024",
                duration = "24/7 Live",
                quality = "1080p HD",
                description = "বাংলাদেশ জাতীয় ক্রিকেট দলের হোম ও অ্যাওয়ে সকল দ্বিপাক্ষিক সিরিজের অফিসিয়াল লাইভ স্ট্রিম।",
                isLive = true
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_live_4",
                title = "Somoy TV 24/7 Live (সময় টিভি)",
                banglaTitle = "সময় টিভি লাইভ ২৪/৭",
                category = "live",
                providerName = "LivXowProvider",
                posterUrl = "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://live-somoy.akamaized.net/live/somoy_720p/chunks.m3u8",
                rating = "Live",
                year = "2024",
                duration = "24/7 News",
                quality = "720p HD",
                description = "বাংলাদেশের শীর্ষ সংবাদভিত্তিক টেলিভিশন চ্যানেল সময় টিভির সার্বক্ষণিক সরাসরি সম্প্রচার।",
                isLive = true
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_live_5",
                title = "Jamuna TV Live HD (যমুনা টিভি)",
                banglaTitle = "যমুনা টিভি লাইভ এইচডি",
                category = "live",
                providerName = "LivXowProvider",
                posterUrl = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://jamuna.akamaized.net/live/jamuna_720p/chunks.m3u8",
                rating = "Live",
                year = "2024",
                duration = "24/7 News",
                quality = "720p HD",
                description = "যমুনা টেলিভিশন লাইভ বুলেটিন, অনুসন্ধানী প্রতিবেদন ও দেশ-বিদেশের ব্রেকিং নিউজ।",
                isLive = true
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_live_6",
                title = "Channel i Live HD (চ্যানেল আই)",
                banglaTitle = "চ্যানেল আই লাইভ এইচডি",
                category = "live",
                providerName = "PlayZTVProvider",
                posterUrl = "https://images.unsplash.com/photo-1522869635100-9f4c5e86aa37?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://channel-i.akamaized.net/live/channel_i_720p/chunks.m3u8",
                rating = "Live",
                year = "2024",
                duration = "24/7 HD",
                quality = "720p HD",
                description = "হৃদয়ে বাংলাদেশ - চ্যানেল আইয়ের লাইভ অনুষ্ঠান, নাটক, সিনেমা ও সংবাদ সম্প্রচার।",
                isLive = true
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_live_7",
                title = "ATN Bangla Live (এটিএন বাংলা)",
                banglaTitle = "এটিএন বাংলা লাইভ",
                category = "live",
                providerName = "SKTechProvider",
                posterUrl = "https://images.unsplash.com/photo-1578022761797-b8636ac1773c?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://atnbangla.akamaized.net/live/atn_720p/chunks.m3u8",
                rating = "Live",
                year = "2024",
                duration = "24/7 Entertainment",
                quality = "720p HD",
                description = "অবিরাম বাংলার কথা বলে - এটিএন বাংলা বিনোদন ও সংবাদ চ্যানেলের লাইভ ফিড।",
                isLive = true
            ),

            // AUDIOBOOKS
            com.example.data.model.CNCContentItem(
                id = "cnc_audio_1",
                title = "The Adventures of Sherlock Holmes",
                banglaTitle = "শার্লক হোমসের অভিযান (অডিওবুক)",
                category = "audiobook",
                providerName = "LibriVoxAudiobook",
                posterUrl = "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                rating = "9.5",
                year = "Classic",
                duration = "10 Episodes",
                quality = "High Audio",
                description = "Arthur Conan Doyle's timeless detective stories narrated with complete immersive audio drama and effects.",
                isLive = false
            ),
            com.example.data.model.CNCContentItem(
                id = "cnc_audio_2",
                title = "Pride and Prejudice",
                banglaTitle = "প্রাইড অ্যান্ড প্রেজুডিস (অডিওবুক)",
                category = "audiobook",
                providerName = "GoldenAudiobook",
                posterUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                rating = "9.3",
                year = "Classic",
                duration = "8 Episodes",
                quality = "High Audio",
                description = "Jane Austen's classic romantic novel following the turbulent relationship between Elizabeth Bennet and Fitzwilliam Darcy.",
                isLive = false
            )
        )
    }
}
