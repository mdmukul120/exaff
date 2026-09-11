package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sports
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
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedGlowing
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary

@Composable
fun SportsScreen(
    sportsMap: Map<SportsSource, List<SportsMatchItem>>,
    selectedSource: SportsSource,
    isLoading: Boolean,
    isBangla: Boolean,
    isDarkMode: Boolean = true,
    onSelectSource: (SportsSource) -> Unit,
    onPlayMatchStream: (SportsMatchItem, SportsStream) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val screenBg = if (isDarkMode) MukulDarkBg else Color(0xFFF6F7FB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)
    val cardBg = if (isDarkMode) Color(0xFF161722) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)

    var matchForServerDialog by remember { mutableStateOf<SportsMatchItem?>(null) }
    var selectedSportFilter by remember { mutableStateOf("সকল") }

    val currentMatches = sportsMap[selectedSource] ?: emptyList()

    val filteredMatches = remember(currentMatches, selectedSportFilter) {
        if (selectedSportFilter == "সকল") currentMatches
        else currentMatches.filter { match ->
            when (selectedSportFilter) {
                "ক্রিকেট" -> match.sportType.contains("cricket", ignoreCase = true) ||
                        match.title.contains("cricket", ignoreCase = true) ||
                        match.tournament.contains("IPL", ignoreCase = true) ||
                        match.tournament.contains("BPL", ignoreCase = true) ||
                        match.tournament.contains("T20", ignoreCase = true)
                "ফুটবল" -> match.sportType.contains("football", ignoreCase = true) ||
                        match.sportType.contains("soccer", ignoreCase = true) ||
                        match.title.contains("football", ignoreCase = true) ||
                        match.tournament.contains("Premier", ignoreCase = true) ||
                        match.tournament.contains("La Liga", ignoreCase = true) ||
                        match.tournament.contains("Champions", ignoreCase = true)
                else -> true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("sports_screen")
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00B0FF),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = "Sports",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isBangla) "লাইভ স্পোর্টস" else "Live Sports",
                            color = textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MukulRedPrimary
                        ) {
                            Text(
                                text = "LIVE HD",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isBangla) "সরাসরি খেলা ও বিশ্বমানের স্পোর্টস চ্যানেল"
                        else "Live matches & global sports channels",
                        color = MukulTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF222436) else Color(0xFFE2E4EE))
                    .testTag("sports_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MukulRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Primary Source Selector Tabs (Horizontal Scroll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SportsSource.values()) { source ->
                val isSelected = selectedSource == source
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(source.badgeColorHex) else if (isDarkMode) Color(0xFF1B1C26) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(source.badgeColorHex) else cardBorder
                    ),
                    modifier = Modifier.clickable { onSelectSource(source) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        if (source == SportsSource.FREE_LIVE_SPORTS) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFF00B0FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isBangla) source.bnName else source.enName,
                            color = if (isSelected) Color.White else textPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Sub-filter for Sport Type (Cricket, Football, All)
        if (selectedSource != SportsSource.FREE_LIVE_SPORTS) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sportTypes = listOf("সকল", "ক্রিকেট", "ফুটবল")
                items(sportTypes) { type ->
                    val isSelected = selectedSportFilter == type
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MukulRedPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) MukulRedPrimary else Color.Transparent),
                        modifier = Modifier.clickable { selectedSportFilter = type }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            if (type == "ক্রিকেট") {
                                Icon(Icons.Default.SportsCricket, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isSelected) MukulRedPrimary else MukulTextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                            } else if (type == "ফুটবল") {
                                Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isSelected) MukulRedPrimary else MukulTextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = type,
                                color = if (isSelected) MukulRedPrimary else MukulTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // Main Content Area
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MukulRedPrimary)
            }
        } else if (filteredMatches.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Sports,
                        contentDescription = null,
                        tint = MukulTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isBangla) "বর্তমানে এই ক্যাটাগরিতে কোনো লাইভ স্ট্রিম নেই"
                        else "No live streams available right now",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBangla) "অন্য ক্যাটাগরি দেখুন বা রিফ্রেশ বাটনে চাপুন"
                        else "Switch to other sources or tap refresh",
                        color = MukulTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (selectedSource == SportsSource.FREE_LIVE_SPORTS) {
            // Free Live Sports Channel Grid (Powr.tv API channels)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMatches, key = { it.id }) { channel ->
                    FreeLiveSportsChannelCard(
                        channel = channel,
                        isDarkMode = isDarkMode,
                        onClick = {
                            if (channel.streams.isNotEmpty()) {
                                onPlayMatchStream(channel, channel.streams[0])
                            } else if (channel.directPlayerUrl.isNotEmpty()) {
                                onPlayMatchStream(channel, SportsStream("Live", channel.directPlayerUrl))
                            }
                        }
                    )
                }
            }
        } else {
            // Live Match Cards
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMatches, key = { it.id }) { match ->
                    SportsMatchCard(
                        match = match,
                        isDarkMode = isDarkMode,
                        isBangla = isBangla,
                        onPlayClick = {
                            if (match.streams.size == 1) {
                                onPlayMatchStream(match, match.streams[0])
                            } else if (match.streams.size > 1) {
                                matchForServerDialog = match
                            } else if (match.directPlayerUrl.isNotEmpty()) {
                                onPlayMatchStream(match, SportsStream("Stream 1", match.directPlayerUrl))
                            } else {
                                Toast.makeText(context, "কোনো স্ট্রিম লিংক পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Server / Quality Selection Dialog
    if (matchForServerDialog != null) {
        val match = matchForServerDialog!!
        Dialog(onDismissRequest = { matchForServerDialog = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkMode) Color(0xFF1E202E) else Color.White,
                border = BorderStroke(1.dp, MukulRedGlowing.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBangla) "সার্ভার নির্বাচন করুন" else "Select Stream Server",
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { matchForServerDialog = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MukulTextSecondary)
                        }
                    }

                    Text(
                        text = match.title,
                        color = MukulRedPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    match.streams.forEachIndexed { idx, stream ->
                        Button(
                            onClick = {
                                matchForServerDialog = null
                                onPlayMatchStream(match, stream)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (idx == 0) MukulRedPrimary else if (isDarkMode) Color(0xFF26283C) else Color(0xFFE8EAF4)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (idx == 0) Color.White else textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stream.serverName.ifEmpty { "সার্ভার ${idx + 1}" },
                                        color = if (idx == 0) Color.White else textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (idx == 0) Color.White.copy(alpha = 0.2f) else MukulRedPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = stream.quality.ifEmpty { "HD" },
                                        color = if (idx == 0) Color.White else MukulRedPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

@Composable
private fun FreeLiveSportsChannelCard(
    channel: SportsMatchItem,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkMode) Color(0xFF161722) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (channel.bannerUrl.isNotEmpty()) {
                    AsyncImage(
                        model = channel.bannerUrl,
                        contentDescription = channel.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Color(0xFF00B0FF),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Top Live Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE50914),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "● LIVE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Play circle button overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = channel.title,
                    color = textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (channel.tournament.isNotEmpty()) {
                    Text(
                        text = channel.tournament,
                        color = MukulTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SportsMatchCard(
    match: SportsMatchItem,
    isDarkMode: Boolean,
    isBangla: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkMode) Color(0xFF161722) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onPlayClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Tournament + Live Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = match.tournament.ifEmpty { match.sportType },
                    color = Color(0xFF00B0FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE50914)
                ) {
                    Text(
                        text = "● LIVE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Teams Row or Match Title
            if (match.teamAName.isNotEmpty() && match.teamBName.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Team A
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (match.teamAFlag.isNotEmpty()) {
                            AsyncImage(
                                model = match.teamAFlag,
                                contentDescription = match.teamAName,
                                modifier = Modifier.size(24.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = match.teamAName,
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkMode) Color(0xFF222436) else Color(0xFFE8EAF4),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "VS",
                            color = MukulRedPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (match.teamBFlag.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            AsyncImage(
                                model = match.teamBFlag,
                                contentDescription = match.teamBName,
                                modifier = Modifier.size(24.dp).clip(CircleShape)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = match.title,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (match.streams.isNotEmpty()) "${match.streams.size} টি স্ট্রিম উপলব্ধ" else "সরাসরি দেখুন",
                    color = MukulTextSecondary,
                    fontSize = 11.sp
                )

                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "খেলা দেখুন" else "Watch",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
