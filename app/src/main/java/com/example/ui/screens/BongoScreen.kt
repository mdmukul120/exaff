package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.components.MediaCard
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedGlowing
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary

@Composable
fun BongoScreen(
    bongoItems: List<MediaItem>,
    watchlistIds: Set<String>,
    isLoading: Boolean = false,
    isBangla: Boolean = true,
    isDarkMode: Boolean = true,
    onMediaClick: (MediaItem) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenBg = if (isDarkMode) MukulDarkBg else Color(0xFFF6F7FB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)
    val pillBg = if (isDarkMode) Color(0xFF1B1C26) else Color.White
    val pillBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)

    var selectedFilter by remember { mutableStateOf("সকল") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("সকল", "নাটক", "মুভিজ", "সিরিজ", "বুম ও শর্টস")

    val filteredItems = remember(bongoItems, selectedFilter, searchQuery) {
        bongoItems.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "নাটক" -> item.title.contains("নাটক", ignoreCase = true) ||
                        item.genre.contains("Drama", ignoreCase = true) ||
                        item.genre.contains("নাটক", ignoreCase = true)
                "মুভিজ" -> item.category.equals("movies", ignoreCase = true) ||
                        item.genre.contains("Cinema", ignoreCase = true) ||
                        item.genre.contains("Movie", ignoreCase = true)
                "সিরিজ" -> item.category.equals("series", ignoreCase = true) ||
                        item.episodes.isNotEmpty() ||
                        item.genre.contains("Series", ignoreCase = true)
                "বুম ও শর্টস" -> item.title.contains("Boom", ignoreCase = true) ||
                        item.genre.contains("Short", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.genre.contains(searchQuery, ignoreCase = true) ||
                        item.description.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("bongo_screen")
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
                    color = Color(0xFFE50914),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Bongo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isBangla) "বঙ্গ ভিডিও" else "Bongo Videos",
                            color = textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFF0055).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "BONGO",
                                color = Color(0xFFFF0055),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isBangla) "অরিজিনাল নাটক, সিনেমা ও বিশেষ কনটেন্ট (${filteredItems.size})"
                        else "Original dramas, films & exclusives (${filteredItems.size})",
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
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MukulRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (isBangla) "বঙ্গ নাটক বা মুভি খুঁজুন..." else "Search Bongo content...",
                    color = MukulTextSecondary,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MukulRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MukulTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MukulRedPrimary,
                unfocusedBorderColor = if (isDarkMode) Color(0xFF2A2B3D) else Color(0xFFDCDFEA),
                focusedContainerColor = if (isDarkMode) Color(0xFF161722) else Color.White,
                unfocusedContainerColor = if (isDarkMode) Color(0xFF161722) else Color.White,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(50.dp)
        )

        // Filter Category Pills
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val isSelected = selectedFilter == cat
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MukulRedPrimary else pillBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) MukulRedGlowing else pillBorder
                    ),
                    modifier = Modifier.clickable { selectedFilter = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Color.White else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Main Grid Content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MukulRedPrimary)
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isBangla) "কোনো বঙ্গ ভিডিও পাওয়া যায়নি" else "No Bongo videos found",
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBangla) "অন্য ফিল্টার চেষ্টা করুন অথবা রিফ্রেশ দিন" else "Try another category or refresh",
                        color = MukulTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 110.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    MediaCard(
                        media = item,
                        isInWatchlist = watchlistIds.contains(item.id),
                        onClick = { onMediaClick(item) },
                        onToggleWatchlist = { onToggleWatchlist(item.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
