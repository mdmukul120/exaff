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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun MoviesScreen(
    movies: List<MediaItem>,
    selectedCategory: String,
    currentPage: Int = 1,
    isLoadingPage: Boolean = false,
    watchlistIds: Set<String>,
    isBangla: Boolean,
    isDarkMode: Boolean = true,
    onSelectCategory: (String) -> Unit,
    onSelectPage: (Int) -> Unit,
    onMovieClick: (MediaItem) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenBg = if (isDarkMode) MukulDarkBg else Color(0xFFF6F7FB)
    val pillBg = if (isDarkMode) Color(0xFF1B1C26) else Color.White
    val pillBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to top whenever category or page changes
    LaunchedEffect(selectedCategory, currentPage) {
        gridState.animateScrollToItem(0)
    }

    // Comprehensive Provider Movie Categories
    val categories = listOf(
        "All",
        "HDHub4U",
        "MoviesMod",
        "TopMovies",
        "MoviesDrive",
        "UHD Movies",
        "Toffee",
        "Bioscope",
        "Hoichoi",
        "Hindi Dubbed",
        "Bangla Cinema"
    )

    // Number of available pagination pages (1 to 25)
    val totalAvailablePages = (1..25).toList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("movies_screen")
    ) {
        // 1. Categories / Providers Filter Bar
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                val label = when (category) {
                    "All" -> if (isBangla) "সকল প্রোভাইডার" else "All Providers"
                    "HDHub4U" -> "HDHub4U"
                    "MoviesMod" -> "MoviesMod"
                    "TopMovies" -> "TopMovies"
                    "MoviesDrive" -> "MoviesDrive"
                    "UHD Movies" -> if (isBangla) "UHD ৪কে" else "UHD 4K"
                    "Toffee" -> "Toffee Movies"
                    "Bioscope" -> if (isBangla) "বায়োস্কোপ বাংলা" else "Bioscope"
                    "Hoichoi" -> "Hoichoi"
                    "Hindi Dubbed" -> if (isBangla) "হিন্দি ডাবড" else "Hindi Dubbed"
                    "Bangla Cinema" -> if (isBangla) "বাংলা সিনেমা" else "Bangla Cinema"
                    else -> category
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MukulRedPrimary else pillBg,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, pillBorder),
                    modifier = Modifier
                        .clickable { onSelectCategory(category) }
                        .testTag("category_pill_$category")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else if (isDarkMode) MukulTextSecondary else Color(0xFF5A5D6E),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 2. Provider Sub-Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MukulRedPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = selectedCategory,
                        color = MukulRedGlowing,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoadingPage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = MukulRedPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (isBangla) "পেজ $currentPage (${movies.size} টি মুভি)" else "Page $currentPage (${movies.size} movies)",
                    color = MukulTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 3. Movies Grid & Empty / Loading States
        if (isLoadingPage && movies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MukulRedPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isBangla) "পেজ $currentPage এর মুভিগুলো লোড হচ্ছে..." else "Loading Page $currentPage Movies...",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (movies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MukulTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isBangla) "এই পেজে কোনো মুভি পাওয়া যায়নি" else "No movies found on this page",
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MukulRedPrimary,
                        modifier = Modifier.clickable { onSelectPage(1) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "প্রথম পেজে যান" else "Go to Page 1",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(movies, key = { it.id }) { movie ->
                    MediaCard(
                        media = movie,
                        isInWatchlist = watchlistIds.contains(movie.id),
                        onClick = { onMovieClick(movie) },
                        onToggleWatchlist = { onToggleWatchlist(movie.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Bottom Pagination Bar: Displayed after the page's movies finish
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) Color(0xFF161824) else Color(0xFFE9EBF4),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDarkMode) Color(0xFF282B3E) else Color(0xFFDCDFEA)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .testTag("bottom_pagination_bar")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Page status label
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isBangla) "পৃষ্ঠা পরিবর্তন করুন" else "Select Page",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MukulRedPrimary
                                ) {
                                    Text(
                                        text = if (isBangla) "পেজ $currentPage / ২৫" else "Page $currentPage / 25",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Scrollable 1, 2, 3, 4, 5 ... 25 bar with < and >
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Previous Page Button
                                IconButton(
                                    onClick = {
                                        if (currentPage > 1 && !isLoadingPage) {
                                            onSelectPage(currentPage - 1)
                                            coroutineScope.launch { gridState.animateScrollToItem(0) }
                                        }
                                    },
                                    enabled = currentPage > 1 && !isLoadingPage,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isDarkMode) Color(0xFF222436) else Color.White)
                                        .testTag("bottom_prev_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Page",
                                        tint = if (currentPage > 1) textPrimary else MukulTextSecondary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Number Buttons (1..25)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(totalAvailablePages) { pageNum ->
                                        val isCurrent = pageNum == currentPage
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrent) MukulRedPrimary else (if (isDarkMode) Color(0xFF222436) else Color.White),
                                            border = if (isCurrent) null else androidx.compose.foundation.BorderStroke(0.5.dp, pillBorder),
                                            modifier = Modifier
                                                .clickable {
                                                    if (!isLoadingPage && pageNum != currentPage) {
                                                        onSelectPage(pageNum)
                                                        coroutineScope.launch { gridState.animateScrollToItem(0) }
                                                    }
                                                }
                                                .testTag("bottom_page_number_$pageNum")
                                        ) {
                                            Text(
                                                text = "$pageNum",
                                                color = if (isCurrent) Color.White else textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Next Page Button
                                IconButton(
                                    onClick = {
                                        if (currentPage < 25 && !isLoadingPage) {
                                            onSelectPage(currentPage + 1)
                                            coroutineScope.launch { gridState.animateScrollToItem(0) }
                                        }
                                    },
                                    enabled = currentPage < 25 && !isLoadingPage,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isDarkMode) Color(0xFF222436) else Color.White)
                                        .testTag("bottom_next_page_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next Page",
                                        tint = if (currentPage < 25) textPrimary else MukulTextSecondary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Big Action: Load Next Page
                            if (currentPage < 25) {
                                Button(
                                    onClick = {
                                        if (!isLoadingPage) {
                                            onSelectPage(currentPage + 1)
                                            coroutineScope.launch { gridState.animateScrollToItem(0) }
                                        }
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("load_next_page_action_button")
                                ) {
                                    if (isLoadingPage) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBangla) "লোড হচ্ছে..." else "Loading...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        Text(
                                            text = if (isBangla) "পরবর্তী পেজ (${currentPage + 1}) লোড করুন" else "Load Next Page (${currentPage + 1})",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
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
}
