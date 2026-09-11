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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.api.ApiService
import com.example.data.model.LiveChannel
import com.example.data.model.MediaItem
import com.example.ui.theme.MukulCardBg
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulGold
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary
import kotlinx.coroutines.delay

@Composable
fun SearchDialog(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    allMedia: List<MediaItem>,
    allChannels: List<LiveChannel>,
    onSelectMedia: (MediaItem) -> Unit,
    onSelectChannel: (LiveChannel) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var onlineResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var searchPage by remember { mutableIntStateOf(1) }

    // Live debounced online multi-provider search across all pages
    LaunchedEffect(searchQuery, searchPage) {
        val trimmed = searchQuery.trim()
        if (trimmed.length >= 2) {
            isSearchingOnline = true
            delay(450)
            try {
                val results = ApiService.searchAllProviders(trimmed, searchPage)
                onlineResults = results
            } catch (e: Exception) {
                onlineResults = emptyList()
            } finally {
                isSearchingOnline = false
            }
        } else {
            onlineResults = emptyList()
            isSearchingOnline = false
        }
    }

    val filteredMedia = remember(searchQuery, allMedia) {
        if (searchQuery.isBlank()) emptyList() else {
            allMedia.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.genre.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredChannels = remember(searchQuery, allChannels) {
        if (searchQuery.isBlank()) emptyList() else {
            allChannels.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.group.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val combinedMedia = remember(filteredMedia, onlineResults) {
        (filteredMedia + onlineResults).distinctBy { it.title.lowercase().trim() }
    }

    val filterChips = listOf(
        "All" to "সকল (${combinedMedia.size + filteredChannels.size})",
        "Online" to "অনলাইন প্রোভাইডার (${onlineResults.size})",
        "Local" to "হোম কন্টেন্ট (${filteredMedia.size})",
        "LiveTV" to "লাইভ টিভি (${filteredChannels.size})"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MukulDarkBg)
                .padding(top = 16.dp)
                .testTag("search_dialog")
        ) {
            // Search Input Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchPage = 1
                        onQueryChange(it)
                    },
                    placeholder = { Text("Search movies, shows, live TV, HDHub, MoviesMod...", color = MukulTextSecondary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MukulRedPrimary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSearchingOnline) {
                                CircularProgressIndicator(
                                    color = MukulRedPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp).padding(end = 4.dp)
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MukulTextSecondary
                                    )
                                }
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MukulCardBg,
                        unfocusedContainerColor = MukulCardBg,
                        focusedBorderColor = MukulRedPrimary,
                        unfocusedBorderColor = Color(0xFF2B2C3B),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MukulCardBg)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Category Filter Row
            if (searchQuery.isNotBlank()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterChips) { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MukulTextSecondary
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MukulRedPrimary,
                                containerColor = MukulCardBg
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MukulRedPrimary else Color(0xFF2B2C3B)
                            )
                        )
                    }
                }
            }

            // Results List
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 1. Online & Local Movies
                val showMedia = when (selectedFilter) {
                    "Online" -> onlineResults
                    "Local" -> filteredMedia
                    "LiveTV" -> emptyList()
                    else -> combinedMedia
                }

                if (showMedia.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)
                        ) {
                            Text(
                                text = "মুভি ও সিরিজ (${showMedia.size})",
                                color = MukulTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isSearchingOnline) {
                                Text(
                                    text = "অনলাইনে খোঁজা হচ্ছে...",
                                    color = MukulGold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    items(showMedia, key = { "m_${it.id}" }) { media ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MukulCardBg, RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectMedia(media)
                                    onDismiss()
                                }
                                .padding(10.dp)
                        ) {
                            AsyncImage(
                                model = media.posterUrl.ifEmpty { media.backdropUrl },
                                contentDescription = media.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp, 72.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = MukulRedPrimary.copy(alpha = 0.2f),
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = media.label.ifEmpty { media.provider.uppercase() },
                                            color = MukulRedPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = media.year,
                                        color = MukulTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = media.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = if (media.provider.isNotEmpty()) "উৎস: ${media.provider.uppercase()} • ${media.duration}" else media.genre,
                                    color = MukulTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MukulRedPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Live Channels
                val showChannels = if (selectedFilter == "Online" || selectedFilter == "Local") emptyList() else filteredChannels
                if (showChannels.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "লাইভ টিভি চ্যানেল (${showChannels.size})",
                            color = MukulTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    items(showChannels, key = { "ch_${it.id}" }) { channel ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MukulCardBg, RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectChannel(channel)
                                    onDismiss()
                                }
                                .padding(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(50.dp, 40.dp)
                                    .background(Color(0xFF151620), RoundedCornerShape(6.dp))
                            ) {
                                if (channel.logoUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = channel.logoUrl,
                                        contentDescription = channel.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = MukulRedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${channel.group} • Live Stream",
                                    color = MukulTextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MukulRedPrimary
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Empty state
                if (searchQuery.isNotEmpty() && showMedia.isEmpty() && showChannels.isEmpty() && !isSearchingOnline) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MukulTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "\"$searchQuery\" এর জন্য কিছু পাওয়া যায়নি",
                                color = MukulTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "সকল প্রোভাইডার চেক করা হয়েছে। অন্য নাম দিয়ে চেষ্টা করুন।",
                                color = MukulTextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Multi-page Pagination Footer for online provider search
            if (searchQuery.isNotBlank() && (onlineResults.isNotEmpty() || searchPage > 1)) {
                Surface(
                    color = MukulCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        IconButton(
                            onClick = { if (searchPage > 1) searchPage-- },
                            enabled = searchPage > 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Page",
                                tint = if (searchPage > 1) Color.White else MukulTextSecondary.copy(alpha = 0.4f)
                            )
                        }

                        Text(
                            text = "সকল প্রোভাইডার পেজ $searchPage",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = { searchPage++ }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Page",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
