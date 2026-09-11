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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.EpisodeItem
import com.example.data.model.MediaItem
import com.example.ui.theme.MukulCardBg
import com.example.ui.theme.MukulCardBorder
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulGold
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary

@Composable
fun SeriesScreen(
    seriesList: List<MediaItem>,
    watchlistIds: Set<String>,
    isBangla: Boolean,
    isDarkMode: Boolean = true,
    onPlayMedia: (MediaItem) -> Unit,
    onPlayEpisode: (MediaItem, EpisodeItem) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenBg = if (isDarkMode) MukulDarkBg else Color(0xFFF6F7FB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)
    val cardBg = if (isDarkMode) MukulCardBg else Color.White
    val cardBorder = if (isDarkMode) MukulCardBorder else Color(0xFFDFE1EB)
    val episodeRowBg = if (isDarkMode) Color(0xFF222330) else Color(0xFFF0F1F7)

    var expandedSeriesId by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterCategories = listOf(
        "All" to if (isBangla) "সকল সিরিজ ও নাটক" else "All Shows",
        "Bongo" to if (isBangla) "বাংগো নাটক ও সিনেমা" else "Bongo Exclusive",
        "Bangla" to if (isBangla) "বাংলা ড্রামা সিরিজ" else "Bangla Drama",
        "Other" to if (isBangla) "ওয়েব সিরিজ" else "Web Series"
    )

    val filteredList = remember(seriesList, selectedFilter) {
        when (selectedFilter) {
            "Bongo" -> seriesList.filter {
                it.provider.equals("bongo", true) ||
                it.category.equals("bongo", true) ||
                it.genre.contains("Bongo", true) ||
                it.title.contains("Bongo", true)
            }
            "Bangla" -> seriesList.filter {
                it.genre.contains("Bangla", true) ||
                it.category.contains("bioscope", true) ||
                it.genre.contains("Drama", true)
            }
            "Other" -> seriesList.filter {
                !it.provider.equals("bongo", true) && !it.category.equals("bongo", true)
            }
            else -> seriesList
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("series_screen")
    ) {
        item {
            Column {
                Text(
                    text = if (isBangla) "ওয়েব সিরিজ ও বাংগো ড্রামা" else "Series, Shows & Bongo",
                    color = textPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    items(filterCategories) { (key, label) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MukulRedPrimary else if (isDarkMode) Color(0xFF1E202E) else Color.White,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.clickable { selectedFilter = key }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        items(filteredList, key = { it.id }) { series ->
            val isExpanded = expandedSeriesId == series.id

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedSeriesId = if (isExpanded) null else series.id
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Thumbnail
                        AsyncImage(
                            model = series.posterUrl.ifEmpty { series.backdropUrl },
                            contentDescription = series.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(80.dp)
                                .height(115.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Details
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MukulRedPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = series.label,
                                    color = MukulRedPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Text(
                                text = series.title,
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MukulGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = series.rating,
                                    color = if (isDarkMode) Color.White else Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•  ${series.duration}",
                                    color = MukulTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = series.description,
                                color = MukulTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Episodes list if expanded or has episodes
                    if (isExpanded && series.episodes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (isBangla) "পর্বসমূহ (${series.episodes.size})" else "Episodes (${series.episodes.size})",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            series.episodes.forEach { ep ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(episodeRowBg, RoundedCornerShape(8.dp))
                                        .clickable { onPlayEpisode(series, ep) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MukulRedPrimary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play Episode",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = ep.title,
                                                color = if (isDarkMode) Color.White else Color.Black,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${if (isBangla) "সময়:" else "Duration:"} ${ep.duration}",
                                                color = MukulTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (series.episodes.isNotEmpty()) {
                                    if (isBangla) "পর্ব দেখুন ▾" else "View Episodes ▾"
                                } else {
                                    if (isBangla) "এখন দেখুন ▸" else "Watch Now ▸"
                                },
                                color = MukulRedPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    if (series.episodes.isNotEmpty()) {
                                        expandedSeriesId = series.id
                                    } else {
                                        onPlayMedia(series)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
