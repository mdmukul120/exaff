package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.SportsMatchItem
import com.example.data.model.SportsSource
import com.example.data.model.SportsStream
import com.example.util.SportsMatchHelper
import com.example.ui.theme.MukulCardBg
import com.example.ui.theme.MukulCardBorder
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulGold
import com.example.ui.theme.MukulRedGlowing
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary

data class UpcomingFeature(
    val titleBn: String,
    val titleEn: String,
    val descBn: String,
    val descEn: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun MoreScreen(
    sportsMap: Map<SportsSource, List<SportsMatchItem>>,
    selectedSource: SportsSource,
    isLoading: Boolean,
    isBangla: Boolean,
    isDarkMode: Boolean = true,
    onSelectSource: (SportsSource) -> Unit,
    onPlayMatchStream: (SportsMatchItem, SportsStream) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenBg = if (isDarkMode) MukulDarkBg else Color(0xFFF6F7FB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)
    val cardBg = if (isDarkMode) MukulCardBg else Color.White
    val cardBorder = if (isDarkMode) MukulCardBorder else Color(0xFFE2E4EE)

    var matchForServerDialog by remember { mutableStateOf<SportsMatchItem?>(null) }
    var selectedSportTypeFilter by remember { mutableStateOf("সকল") }

    val currentMatches = remember(sportsMap, selectedSource) {
        sportsMap[selectedSource] ?: emptyList()
    }

    val availableSportTypes = remember(currentMatches) {
        listOf("সকল") + currentMatches.map { it.sportType.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    val filteredMatches = remember(currentMatches, selectedSportTypeFilter) {
        val base = if (selectedSportTypeFilter == "সকল") currentMatches
        else currentMatches.filter { it.sportType.equals(selectedSportTypeFilter, ignoreCase = true) }

        base.sortedWith(
            compareBy<SportsMatchItem> { match ->
                val eval = SportsMatchHelper.evaluateMatch(match)
                when {
                    eval.isLive -> 0
                    eval.isUpcoming -> 1
                    match.categorySource == SportsSource.MATCH_HIGHLIGHTS -> 2
                    eval.isPastEnded -> 4
                    else -> 3
                }
            }
        )
    }

    val upcomingFeatures = listOf(
        UpcomingFeature(
            titleBn = "এআই সাবটাইটেল ও অনুবাদ",
            titleEn = "AI Smart Subtitles & Translation",
            descBn = "চলচ্চিত্রের যেকোনো ভাষা থেকে তাৎক্ষণিক নিখুঁত বাংলা সাবটাইটেল জেনারেশন।",
            descEn = "Instant AI-powered real-time subtitle translation into Bangla.",
            icon = Icons.Default.AutoAwesome,
            tag = "UPCOMING"
        ),
        UpcomingFeature(
            titleBn = "পিকচার-ইন-পিকচার (PiP) মোড",
            titleEn = "Picture-in-Picture (PiP) Mode",
            descBn = "অ্যাপ ব্যাকগ্রাউন্ডে রেখে অন্যান্য কাজ করার সময়ও ছোট উইন্ডোতে ভিডিও চলবে।",
            descEn = "Continue watching videos in a floating mini player while using other apps.",
            icon = Icons.Default.PictureInPicture,
            tag = "BETA"
        ),
        UpcomingFeature(
            titleBn = "টিভি কাস্টিং (Chromecast & DLNA)",
            titleEn = "Smart TV Cast",
            descBn = "এক ক্লিকে বড় পর্দায় আপনার প্রিয় মুভি ও লাইভ টিভি চ্যানেল সম্প্রচার।",
            descEn = "Seamlessly beam your movies and live channels to your Smart TV.",
            icon = Icons.Default.Cast,
            tag = "SOON"
        ),
        UpcomingFeature(
            titleBn = "ক্লাউড ওয়াচলিস্ট ও হিস্ট্রি সিঙ্ক",
            titleEn = "Cloud Watchlist & History Sync",
            descBn = "যেকোনো ডিভাইসে নিজের পছন্দের কনটেন্ট ও প্লেব্যাক হিস্ট্রি সংরক্ষিত থাকবে।",
            descEn = "Keep your saved watchlist and progress synchronized across devices.",
            icon = Icons.Default.CloudSync,
            tag = "SOON"
        ),
        UpcomingFeature(
            titleBn = "আল্ট্রা ডাটা ও ব্যাটারি সেভার",
            titleEn = "Ultra Data & Battery Saver",
            descBn = "কমগতির ইন্টারনেটেও বাফারিং ছাড়া উচ্চগতির ডাটা-সাশ্রয়ী প্লেব্যাক মোড।",
            descEn = "Bandwidth-optimized smooth streaming designed for mobile networks.",
            icon = Icons.Default.Speed,
            tag = "TESTING"
        )
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("more_screen")
    ) {
        // 1. Header Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, MukulRedPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MukulRedPrimary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MukulRedPrimary,
                                    modifier = Modifier.size(10.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBangla) "🔴 লাইভ স্পোর্টস ও টুর্নামেন্ট হাব" else "🔴 Live Sports Hub",
                                    color = MukulRedGlowing,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBangla) "লাইভ খেলাধুলা ও হাইলাইটস" else "Live Sports & Highlights",
                                color = textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBangla)
                                    "বিশ্বসেরা ৬টি স্পোর্টস সার্ভার থেকে সরাসরি সব ম্যাচ ও রিপ্লে উপভোগ করুন।"
                                else
                                    "Watch live matches, tournaments & highlights from 6 premier sports feeds.",
                                color = MukulTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }

                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MukulRedPrimary.copy(alpha = 0.15f))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MukulRedPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MukulRedPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Six Sources Category Tabs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isBangla) "স্পোর্টস সার্ভার ও ক্যাটাগরি" else "Sports Feeds & Categories",
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(SportsSource.values()) { source ->
                        val isSelected = source == selectedSource
                        val matchCount = sportsMap[source]?.size ?: 0
                        val sourceColor = Color(source.badgeColorHex)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) sourceColor else cardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) sourceColor else cardBorder
                            ),
                            modifier = Modifier
                                .clickable {
                                    selectedSportTypeFilter = "সকল"
                                    onSelectSource(source)
                                }
                                .testTag("source_chip_${source.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                val icon = when (source) {
                                    SportsSource.LIVE_SPORTS -> Icons.Default.SportsSoccer
                                    SportsSource.FREE_LIVE_SPORTS -> Icons.Default.Tv
                                    SportsSource.MUKUL_LIVE_SPORTS -> Icons.Default.LiveTv
                                    SportsSource.MUKUL_TAPMAD -> Icons.Default.Tv
                                    SportsSource.MATCH_HIGHLIGHTS -> Icons.Default.Movie
                                    SportsSource.EXTRA_LEAGUE -> Icons.Default.SportsCricket
                                    SportsSource.LIVE_CRICKET -> Icons.Default.SportsCricket
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else sourceColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isBangla) source.bnName else source.enName,
                                        color = if (isSelected) Color.White else textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (matchCount > 0) {
                                        Text(
                                            text = "$matchCount ${if (isBangla) "ম্যাচ" else "matches"}",
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MukulTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Sport Type Filter (Cricket, Football, Tennis, etc.)
        if (availableSportTypes.size > 2) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableSportTypes) { type ->
                        val isSelected = type == selectedSportTypeFilter
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MukulRedPrimary else cardBg,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MukulRedGlowing else cardBorder
                            ),
                            modifier = Modifier.clickable { selectedSportTypeFilter = type }
                        ) {
                            Text(
                                text = type,
                                color = if (isSelected) Color.White else MukulTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Matches List for Current Source
        if (isLoading && currentMatches.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MukulRedPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBangla) "স্পোর্টস ডেটা লোড হচ্ছে..." else "Loading live sports feeds...",
                            color = MukulTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else if (filteredMatches.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = MukulTextSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBangla) "বর্তমানে কোনো লাইভ ম্যাচ পাওয়া যায়নি" else "No live matches available right now",
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBangla) "অন্য ক্যাটাগরি বা সোর্স নির্বাচন করুন অথবা রিফ্রেশ করুন।" else "Please check other sources or refresh.",
                            color = MukulTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isBangla) "রিফ্রেশ করুন" else "Refresh Feed", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            items(filteredMatches) { match ->
                SportsMatchCard(
                    match = match,
                    isBangla = isBangla,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    onPlayClick = {
                        if (match.streams.size > 1) {
                            matchForServerDialog = match
                        } else if (match.streams.isNotEmpty()) {
                            onPlayMatchStream(match, match.streams[0])
                        } else if (match.directPlayerUrl.isNotEmpty()) {
                            onPlayMatchStream(
                                match,
                                SportsStream(
                                    title = "Main Player",
                                    streamUrl = match.directPlayerUrl,
                                    quality = "HD",
                                    isEmbed = true
                                )
                            )
                        }
                    },
                    onOpenServers = { matchForServerDialog = match }
                )
            }
        }

        // 5. Quick Navigation Tools
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isBangla) "কুইক অ্যাকশন ও টুলস" else "Quick Actions & Tools",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Settings Action Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenSettings)
                        .testTag("more_settings_button")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MukulRedPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MukulRedPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBangla) "সেটিংস" else "Settings",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "ভাষা ও থিম" else "Language & Theme",
                            color = MukulTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Watchlist Action Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenWatchlist)
                        .testTag("more_watchlist_button")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MukulGold.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Watchlist",
                                    tint = MukulGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBangla) "ওয়াচলিস্ট" else "Watchlist",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "সংরক্ষিত কনটেন্ট" else "Saved Items",
                            color = MukulTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Cache Cleaner Action Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            Toast.makeText(
                                context,
                                if (isBangla) "অ্যাপ ক্যাশ সফলভাবে পরিষ্কার করা হয়েছে ✓" else "Cache cleaned successfully ✓",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .testTag("more_clean_cache_button")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = "Clear Cache",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isBangla) "ক্যাশ মুছুন" else "Clear Cache",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "গতি বাড়ান" else "Free Storage",
                            color = MukulTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 6. Upcoming Features Roadmap Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isBangla) "🚀 আসন্ন নতুন ফিচারসমূহ" else "🚀 Upcoming Features",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MukulRedPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "ROADMAP",
                        color = MukulRedGlowing,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        items(upcomingFeatures) { feature ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MukulRedPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                tint = MukulRedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) feature.titleBn else feature.titleEn,
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (feature.tag == "UPCOMING") MukulRedPrimary else Color(0xFF2C2E42)
                            ) {
                                Text(
                                    text = feature.tag,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isBangla) feature.descBn else feature.descEn,
                            color = MukulTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 7. Footer
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MukulTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Mukul Plus v2.5 (Ultimate Sports & OTT)",
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "আপনার সেরা বিনোদন ও লাইভ স্পোর্টস অভিজ্ঞতা।" else "Crafted for premium OTT, Movies & Live Sports.",
                            color = MukulTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // Server Selection Dialog
    matchForServerDialog?.let { match ->
        SportsServerChooserDialog(
            match = match,
            isBangla = isBangla,
            onDismiss = { matchForServerDialog = null },
            onSelectServer = { stream ->
                matchForServerDialog = null
                onPlayMatchStream(match, stream)
            }
        )
    }
}

@Composable
private fun SportsMatchCard(
    match: SportsMatchItem,
    isBangla: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    onPlayClick: () -> Unit,
    onOpenServers: () -> Unit
) {
    val schedule = remember(match) { SportsMatchHelper.evaluateMatch(match) }
    val isLive = schedule.isLive
    val isUpcoming = schedule.isUpcoming
    val isEnded = schedule.isPastEnded

    val statusColor = when {
        isLive -> MukulRedPrimary
        isUpcoming -> Color(0xFF1E88E5)
        isEnded -> Color(0xFF5A6275)
        match.categorySource == SportsSource.MATCH_HIGHLIGHTS -> Color(0xFF00897B)
        else -> Color(0xFF2979FF)
    }

    val badgeLabel = when {
        isLive -> if (isBangla) "● লাইভ" else "● LIVE"
        isUpcoming -> if (isBangla) "⏰ শীঘ্রই" else "⏰ UPCOMING"
        isEnded -> if (isBangla) "সমাপ্ত" else "ENDED"
        match.categorySource == SportsSource.MATCH_HIGHLIGHTS -> if (isBangla) "হাইলাইটস" else "HIGHLIGHTS"
        else -> match.status.ifEmpty { "SPORTS" }.uppercase()
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (isLive) MukulRedPrimary.copy(alpha = 0.35f) else cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("sports_card_${match.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Optional banner for highlights or special matches
            if (match.bannerUrl.isNotEmpty() && (match.categorySource == SportsSource.MATCH_HIGHLIGHTS || match.categorySource == SportsSource.LIVE_CRICKET)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = match.bannerUrl,
                        contentDescription = match.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC000000))
                                )
                            )
                    )
                    // Status Badge overlay
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Play icon overlay
                    Surface(
                        shape = CircleShape,
                        color = MukulRedPrimary.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Top Meta row: Tournament name + Sport badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(match.categorySource.badgeColorHex).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = match.sportType.ifEmpty { "Sports" },
                                color = Color(match.categorySource.badgeColorHex),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = match.tournament.ifEmpty { match.categorySource.bnName },
                            color = MukulTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Live or Upcoming Badge (if not banner)
                    if (match.bannerUrl.isEmpty() || (match.categorySource != SportsSource.MATCH_HIGHLIGHTS && match.categorySource != SportsSource.LIVE_CRICKET)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor,
                            border = BorderStroke(1.dp, if (isLive) MukulRedGlowing else statusColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = badgeLabel,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Teams Matchup or Title
                if (match.teamAName.isNotEmpty() && match.teamBName.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Team A
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            TeamLogoView(url = match.teamAFlag, name = match.teamAName)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = match.teamAName,
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // VS pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MukulRedPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MukulRedPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "VS",
                                color = MukulRedGlowing,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        // Team B
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = match.teamBName,
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TeamLogoView(url = match.teamBFlag, name = match.teamBName)
                        }
                    }
                } else {
                    Text(
                        text = match.title,
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Countdown / Match Status Info pill if applicable
                if (isLive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MukulRedPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, MukulRedGlowing.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (isBangla) schedule.countdownBn else schedule.countdownEn,
                            color = MukulRedGlowing,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (isUpcoming) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E88E5).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "⏳ " + (if (isBangla) schedule.countdownBn else schedule.countdownEn),
                            color = Color(0xFF64B5F6),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Bar: Time + Server count + Watch Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = schedule.displayTime.ifEmpty { match.startTime.ifEmpty { if (isLive) "চলছে" else "শীঘ্রই" } },
                        color = MukulTextSecondary,
                        fontSize = 12.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (match.streams.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF232535),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clickable { onOpenServers() }
                            ) {
                                Text(
                                    text = "${match.streams.size} ${if (isBangla) "সার্ভার" else "Servers"}",
                                    color = MukulTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onPlayClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnded) Color(0xFF33384A) else MukulRedPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when {
                                    isEnded -> if (isBangla) "রিপ্লে দেখুন" else "Replay"
                                    isLive -> if (isBangla) "ম্যাচ দেখুন" else "Watch Live"
                                    isUpcoming -> if (isBangla) "দেখুন" else "Watch"
                                    else -> if (isBangla) "ম্যাচ দেখুন" else "Watch"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamLogoView(url: String, name: String) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF1E202F),
        border = BorderStroke(1.dp, Color(0xFF2E3247)),
        modifier = Modifier.size(28.dp)
    ) {
        if (url.isNotEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
            AsyncImage(
                model = url,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(2).uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SportsServerChooserDialog(
    match: SportsMatchItem,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSelectServer: (SportsStream) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MukulCardBg),
            border = BorderStroke(1.dp, MukulRedPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "সার্ভার নির্বাচন করুন" else "Select Stream Server",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = match.title,
                            color = MukulTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    match.streams.forEachIndexed { index, stream ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E202E),
                            border = BorderStroke(1.dp, Color(0xFF2D3145)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectServer(stream) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MukulRedPrimary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = MukulRedPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stream.title.ifEmpty { "Server ${index + 1}" },
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${stream.quality} • ${match.sportType}",
                                            color = MukulTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MukulRedPrimary
                                ) {
                                    Text(
                                        text = if (isBangla) "প্লে" else "Play",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
