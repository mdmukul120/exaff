package com.example.data.model

data class CNCRepoInfo(
    val name: String = "CNC Repo(All Language)",
    val iconUrl: String = "https://raw.githubusercontent.com/NivinCNC/CNCVerse-Cloud-Stream-Extension/refs/heads/builds/cnc.png",
    val description: String = "All Language Contents",
    val manifestVersion: Int = 1,
    val pluginLists: List<String> = listOf("https://raw.githubusercontent.com/NivinCNC/CNCVerse-Cloud-Stream-Extension/builds/plugins.json")
)

data class CNCPluginItem(
    val name: String,
    val internalName: String = name,
    val description: String = "",
    val version: Int = 1,
    val apiVersion: Int = 1,
    val language: String? = null,
    val authors: List<String> = emptyList(),
    val tvTypes: List<String> = emptyList(),
    val fileSize: Long = 0L,
    val status: Int = 1,
    val iconUrl: String = "",
    val url: String = "",
    val repositoryUrl: String = "",
    val fileHash: String = "",
    val isInstalled: Boolean = true
) {
    val formattedSize: String
        get() = when {
            fileSize >= 1024 * 1024 -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
            fileSize >= 1024 -> "${fileSize / 1024} KB"
            fileSize > 0 -> "$fileSize B"
            else -> "N/A"
        }

    val languageDisplay: String
        get() = when (language?.lowercase()) {
            "bn" -> "বাংলা (Bengali)"
            "en" -> "English"
            "ta" -> "Tamil"
            "hi" -> "Hindi"
            "ru" -> "Russian"
            "te" -> "Telugu"
            null, "" -> "Multi / All"
            else -> language.uppercase()
        }

    val isBangla: Boolean
        get() = language?.equals("bn", ignoreCase = true) == true ||
                name.contains("BD", ignoreCase = true) ||
                internalName.contains("BD", ignoreCase = true)

    val isLiveOrSports: Boolean
        get() = tvTypes.any { it.equals("Live", ignoreCase = true) } ||
                name.contains("cric", ignoreCase = true) ||
                name.contains("sport", ignoreCase = true)

    val isAnime: Boolean
        get() = tvTypes.any { it.contains("anime", ignoreCase = true) }

    val isAudiobook: Boolean
        get() = tvTypes.any { it.contains("audiobook", ignoreCase = true) || it.contains("other", ignoreCase = true) } ||
                name.contains("audiobook", ignoreCase = true)
}

data class CNCContentItem(
    val id: String,
    val title: String,
    val banglaTitle: String = title,
    val category: String, // "movies", "anime", "live", "audiobook", "series"
    val providerName: String, // e.g., "AniKoto", "Cricify", "MLSBD", "CastleTv", "StreamFlix", "DoFlix"
    val posterUrl: String,
    val backdropUrl: String = posterUrl,
    val streamUrl: String,
    val rating: String = "8.5",
    val year: String = "2024",
    val duration: String = "2h 15m",
    val quality: String = "1080p HD",
    val description: String = "",
    val isLive: Boolean = false,
    val downloadUrl: String = streamUrl
) {
    fun toMediaItem(): MediaItem {
        return MediaItem(
            id = id,
            title = title,
            description = description,
            category = category,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            streamUrl = streamUrl,
            rating = rating,
            year = year,
            duration = duration,
            genre = providerName,
            label = if (isLive) "LIVE" else quality,
            provider = providerName,
            downloadUrl = downloadUrl
        )
    }
}
