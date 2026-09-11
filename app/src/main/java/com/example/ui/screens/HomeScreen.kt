package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentSection
import com.example.data.model.LiveChannel
import com.example.data.model.MediaItem
import com.example.ui.components.ChannelCard
import com.example.ui.components.HeroBanner
import com.example.ui.components.MediaCard
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary

@Composable
fun HomeScreen(
    heroMedia: MediaItem?,
    sections: List<ContentSection>,
    liveChannels: List<LiveChannel>,
    watchlistIds: Set<String>,
    isLoading: Boolean,
    isBangla: Boolean,
    isDarkMode: Boolean = true,
    onMediaClick: (MediaItem) -> Unit,
    onChannelClick: (LiveChannel) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onSeeAllChannels: () -> Unit,
    onSeeAllSection: (ContentSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenBg = if (isDarkMode) MukulDarkBg else Color(0xFFF6F7FB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)
    val cardBg = if (isDarkMode) Color(0xFF161722) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF27293D) else Color(0xFFE2E4EE)

    if (isLoading) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .background(screenBg)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MukulRedPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isBangla) "লোড হচ্ছে..." else "Loading Mukul plus...",
                    color = MukulTextSecondary,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    val heroList = remember(heroMedia, sections) {
        val list = mutableListOf<MediaItem>()
        if (heroMedia != null) list.add(heroMedia)
        val topFromSections = sections.flatMap { it.items }.distinctBy { it.id }.take(6)
        for (item in topFromSections) {
            if (list.none { it.id == item.id }) {
                list.add(item)
            }
        }
        list.take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("home_screen")
    ) {
        // Automatic Image Slider Hero Banner
        if (heroList.isNotEmpty()) {
            item(key = "hero_banner") {
                HeroBanner(
                    featuredItems = heroList,
                    watchlistIds = watchlistIds,
                    onWatchNow = { onMediaClick(it) },
                    onToggleWatchlist = { onToggleWatchlist(it) }
                )
            }
        }

        // Live Channels Carousel (Circular style, Bangla channels first!)
        if (liveChannels.isNotEmpty()) {
            item(key = "live_channels_section") {
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = MukulRedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (isBangla) "লাইভ টিভি চ্যানেল" else "Live TV Channels",
                                color = textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onSeeAllChannels)
                        ) {
                            Text(
                                text = if (isBangla) "সকল চ্যানেল" else "See All",
                                color = MukulRedPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MukulRedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Bangla channels are already sorted to the front in repository
                        items(liveChannels.take(20), key = { it.id }) { channel ->
                            ChannelCard(
                                channel = channel,
                                onClick = { onChannelClick(channel) },
                                circleSize = 72.dp
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Real Media Sections (100% real content from Bioscope and SorryBro extractors)
        items(sections, key = { it.title }) { section ->
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = section.title,
                        color = textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onSeeAllSection(section) }
                            .testTag("see_all_section_${section.title.hashCode()}")
                    ) {
                        Text(
                            text = if (isBangla) "আরও দেখুন" else "See All",
                            color = MukulRedPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MukulRedPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(section.items, key = { it.id }) { media ->
                        MediaCard(
                            media = media,
                            isInWatchlist = watchlistIds.contains(media.id),
                            onClick = { onMediaClick(media) },
                            onToggleWatchlist = { onToggleWatchlist(media.id) }
                        )
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
