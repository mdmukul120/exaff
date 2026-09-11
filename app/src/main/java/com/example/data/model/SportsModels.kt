package com.example.data.model

enum class SportsSource(val bnName: String, val enName: String, val badgeColorHex: Long) {
    LIVE_SPORTS("লাইভ ম্যাচ", "Live Matches", 0xFFE50914),
    FREE_LIVE_SPORTS("স্পোর্টস টিভি চ্যানেল", "Sports TV Channels", 0xFF00B0FF),
    MUKUL_LIVE_SPORTS("মুকুল লাইভ", "Mukul Live", 0xFFFF5722),
    MUKUL_TAPMAD("ট্যাপম্যাড লাইভ", "Tapmad Live", 0xFF00E676),
    MATCH_HIGHLIGHTS("হাইলাইটস", "Highlights", 0xFFFFC107),
    EXTRA_LEAGUE("এক্সট্রা লীগ", "Extra League", 0xFF2979FF),
    LIVE_CRICKET("লাইভ ক্রিকেট", "Live Cricket", 0xFF9C27B0)
}

data class SportsStream(
    val title: String,
    val streamUrl: String,
    val serverName: String = "",
    val quality: String = "HD",
    val headers: Map<String, String> = emptyMap(),
    val isEmbed: Boolean = false
)

data class SportsMatchItem(
    val id: String,
    val title: String,
    val categorySource: SportsSource,
    val sportType: String = "Sports",
    val tournament: String = "",
    val status: String = "LIVE",
    val startTime: String = "",
    val teamAName: String = "",
    val teamBName: String = "",
    val teamAFlag: String = "",
    val teamBFlag: String = "",
    val bannerUrl: String = "",
    val streams: List<SportsStream> = emptyList(),
    val directPlayerUrl: String = ""
)
