package com.example.data.model

data class MediaItem(
    val id: String,
    val title: String,
    val description: String = "",
    val category: String = "movies", // "movies", "series", "bongo", "bollywood", "hindi-dubbed"
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val streamUrl: String = "",
    val rating: String = "4.7",
    val year: String = "2024",
    val duration: String = "2h 10m",
    val genre: String = "Entertainment",
    val label: String = "Free",
    val episodes: List<EpisodeItem> = emptyList(),
    val cast: List<String> = emptyList(),
    val provider: String = "",
    val link: String = "",
    val downloadUrl: String = "",
    val isDownloaded: Boolean = false,
    val localFilePath: String = "",
    val qualities: List<MediaQuality> = emptyList(),
    val customHeaders: Map<String, String> = emptyMap()
)

data class MediaQuality(
    val title: String,
    val quality: String,
    val directLink: String = "",
    val streamServers: List<StreamServer> = emptyList()
)

data class StreamServer(
    val server: String,
    val link: String,
    val type: String = "mkv"
)

data class DownloadedMedia(
    val id: String,
    val title: String,
    val filePath: String,
    val fileSize: String,
    val dateAdded: String = "",
    val posterUrl: String = "",
    val progress: Int = 100,
    val isDownloading: Boolean = false,
    val downloadId: Long = -1L,
    val folderName: String = "Mukul plus",
    val durationFormatted: String = ""
)

data class EpisodeItem(
    val id: String,
    val title: String,
    val episodeNumber: Int,
    val duration: String = "45m",
    val thumbnail: String = "",
    val streamUrl: String = "",
    val downloadUrl: String = "",
    val provider: String = "",
    val link: String = ""
)

data class LiveChannel(
    val id: String,
    val name: String,
    val logoUrl: String = "",
    val group: String = "General",
    val streamUrl: String = ""
)

data class ContentSection(
    val title: String,
    val items: List<MediaItem>
)

data class HiddenVideoItem(
    val title: String,
    val imageUrl: String,
    val slugUrl: String = "",
    val videoUrls: List<String> = emptyList()
)

