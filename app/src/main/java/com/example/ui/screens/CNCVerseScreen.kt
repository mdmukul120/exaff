package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CNCContentItem
import com.example.data.model.CNCPluginItem
import com.example.data.model.CNCRepoInfo
import com.example.data.model.MediaItem
import com.example.data.repository.DownloadHelper
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedPrimary

@Composable
fun CNCVerseScreen(
    repoInfo: CNCRepoInfo,
    plugins: List<CNCPluginItem>,
    contentItems: List<CNCContentItem>,
    isLoading: Boolean,
    isBangla: Boolean,
    isDarkMode: Boolean,
    onRefresh: () -> Unit,
    onToggleInstall: (CNCPluginItem) -> Unit,
    onInstallAll: () -> Unit,
    onUninstallAll: () -> Unit,
    onPlayMedia: (MediaItem) -> Unit,
    onOpenMediaDetails: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryTab by remember { mutableStateOf("movies") } // "movies", "anime", "live", "audiobook", "plugins"
    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderFilter by remember { mutableStateOf("All") }
    var selectedPluginForDetails by remember { mutableStateOf<CNCPluginItem?>(null) }

    val bgColor = if (isDarkMode) MukulDarkBg else Color(0xFFF7F8FA)
    val cardBg = if (isDarkMode) Color(0xFF1E202F) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111827)
    val textSecondary = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    val installedCount = plugins.count { it.isInstalled }

    // Navigation Category Tabs
    val categoryTabs = listOf(
        CategoryTab("movies", if (isBangla) "মুভি ও সিরিজ" else "Movies & TV", Icons.Default.Movie),
        CategoryTab("anime", if (isBangla) "অ্যানিমে" else "Anime & Drama", Icons.Default.SmartDisplay),
        CategoryTab("live", if (isBangla) "লাইভ টিভি ও স্পোর্টস" else "Live Sports & TV", Icons.Default.LiveTv),
        CategoryTab("audiobook", if (isBangla) "অডিওবুক" else "Audiobooks", Icons.Default.Book),
        CategoryTab("plugins", if (isBangla) "ইন্সটল ফাইল (${plugins.size})" else "Extensions (${plugins.size})", Icons.Default.Extension)
    )

    // Filter content based on category tab, provider, and search
    val currentCategoryContent = contentItems.filter { item ->
        val matchesCategory = when (selectedCategoryTab) {
            "movies" -> item.category == "movies" || item.category == "series"
            "anime" -> item.category == "anime"
            "live" -> item.category == "live"
            "audiobook" -> item.category == "audiobook"
            else -> true
        }
        val matchesProvider = selectedProviderFilter == "All" || item.providerName.equals(selectedProviderFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.banglaTitle.contains(searchQuery, ignoreCase = true) ||
                item.providerName.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)

        matchesCategory && matchesProvider && matchesSearch
    }

    // Unique providers available for current tab
    val availableProvidersForTab = contentItems
        .filter { item ->
            when (selectedCategoryTab) {
                "movies" -> item.category == "movies" || item.category == "series"
                "anime" -> item.category == "anime"
                "live" -> item.category == "live"
                "audiobook" -> item.category == "audiobook"
                else -> true
            }
        }
        .map { it.providerName }
        .distinct()

    // Filter plugins list when on "plugins" tab
    val filteredPlugins = plugins.filter { plugin ->
        val matchesSearch = searchQuery.isBlank() ||
                plugin.name.contains(searchQuery, ignoreCase = true) ||
                plugin.internalName.contains(searchQuery, ignoreCase = true) ||
                plugin.description.contains(searchQuery, ignoreCase = true) ||
                plugin.language?.contains(searchQuery, ignoreCase = true) == true ||
                plugin.tvTypes.any { it.contains(searchQuery, ignoreCase = true) }

        val matchesFilter = when (selectedProviderFilter) {
            "Installed" -> plugin.isInstalled
            "Bangla" -> plugin.isBangla
            "LiveSports" -> plugin.isLiveOrSports
            "Anime" -> plugin.isAnime
            "Audiobook" -> plugin.isAudiobook
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .testTag("cnc_verse_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 85.dp)
        ) {
            // 1. Hero Repo Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF281440),
                                        if (isDarkMode) Color(0xFF181C2B) else Color(0xFF2B2F44)
                                    )
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0F1017))
                                    .border(1.dp, Color(0xFF9333EA).copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = repoInfo.iconUrl,
                                    contentDescription = "CNC Verse Icon",
                                    modifier = Modifier.size(38.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = repoInfo.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF10B981)
                                    ) {
                                        Text(
                                            text = "v${repoInfo.manifestVersion}",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = if (isBangla) "ইন্সটল ফাইল থেকে মুভি, অ্যানিমে ও লাইভ স্ট্রিম" else "Watch Movies, Anime & Live Streams from Installed Files",
                                    color = Color(0xFFD1D5DB),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = if (isBangla) "$installedCount/${plugins.size} ফাইল সক্রিয়" else "$installedCount/${plugins.size} Active Files",
                                            color = Color(0xFF34D399),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = if (isBangla) "ক্লাউডস্ট্রিম ৩ ফরম্যাট" else "CloudStream 3 (.cs3)",
                                            color = Color(0xFFA78BFA),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Quick refresh
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Main Category Selection Bar (Movies, Anime, Live TV, Audiobook, Plugins)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryTabs) { tab ->
                        val isSelected = selectedCategoryTab == tab.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MukulRedPrimary else if (isDarkMode) Color(0xFF222638) else Color(0xFFE2E8F0),
                            border = if (isSelected) null else BorderStroke(
                                1.dp,
                                if (isDarkMode) Color(0xFF33384F) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .clickable {
                                    selectedCategoryTab = tab.id
                                    selectedProviderFilter = "All"
                                }
                                .testTag("cnc_tab_${tab.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.label,
                                    color = if (isSelected) Color.White else textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 3. Search Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = when (selectedCategoryTab) {
                                    "movies" -> if (isBangla) "মুভি বা সিরিজের নাম দিয়ে খুঁজুন (সুড়ঙ্গ, প্রিয়তমা, Dune)..." else "Search movies & series..."
                                    "anime" -> if (isBangla) "অ্যানিমে খুঁজুন (Solo Leveling, Jujutsu Kaisen)..." else "Search anime & drama..."
                                    "live" -> if (isBangla) "লাইভ ক্রিকেট বা টিভি চ্যানেল খুঁজুন..." else "Search live cricket & TV channels..."
                                    "audiobook" -> if (isBangla) "অডিওবুক খুঁজুন..." else "Search audiobooks..."
                                    else -> if (isBangla) "প্লাগইনের নাম বা ভাষা খুঁজুন..." else "Search plugins & extensions..."
                                },
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
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
                                        tint = textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = MukulRedPrimary,
                            unfocusedBorderColor = if (isDarkMode) Color(0xFF2B3045) else Color(0xFFE5E7EB),
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cnc_search_field")
                    )
                }
            }

            // 4. Provider / File Source Filter Strip (when not on "plugins" tab)
            if (selectedCategoryTab != "plugins" && availableProvidersForTab.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            val isAllSelected = selectedProviderFilter == "All"
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isAllSelected) MukulRedPrimary.copy(alpha = 0.2f) else if (isDarkMode) Color(0xFF1E2235) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAllSelected) MukulRedPrimary else if (isDarkMode) Color(0xFF2E344E) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.clickable { selectedProviderFilter = "All" }
                            ) {
                                Text(
                                    text = if (isBangla) "সকল ফাইল (${currentCategoryContent.size})" else "All Files",
                                    color = if (isAllSelected) MukulRedPrimary else textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        items(availableProvidersForTab) { providerName ->
                            val isSelected = selectedProviderFilter == providerName
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.2f) else if (isDarkMode) Color(0xFF1E2235) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF10B981) else if (isDarkMode) Color(0xFF2E344E) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.clickable { selectedProviderFilter = providerName }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "$providerName (ফাইল)",
                                        color = if (isSelected) Color(0xFF10B981) else textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. If "plugins" tab is selected, render the 36 Plugins List
            if (selectedCategoryTab == "plugins") {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isBangla) "মোট ${filteredPlugins.size} টি প্লাগইন ফাইল" else "${filteredPlugins.size} Extension Files",
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = onInstallAll) {
                                Text(text = if (isBangla) "সব সক্রিয় করুন" else "Install All", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                            if (installedCount > 0) {
                                TextButton(onClick = onUninstallAll) {
                                    Text(text = if (isBangla) "রিসেট" else "Reset", fontSize = 11.sp, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }

                items(filteredPlugins, key = { it.internalName }) { plugin ->
                    PluginItemCard(
                        plugin = plugin,
                        isDarkMode = isDarkMode,
                        isBangla = isBangla,
                        onToggleInstall = { onToggleInstall(plugin) },
                        onDetailsClick = { selectedPluginForDetails = plugin },
                        onDownloadPackage = {
                            DownloadHelper.downloadPluginPackage(context, plugin.name, plugin.url)
                        },
                        onBrowseContent = {
                            if (plugin.isAnime) {
                                selectedCategoryTab = "anime"
                                selectedProviderFilter = plugin.name
                            } else if (plugin.isLiveOrSports) {
                                selectedCategoryTab = "live"
                                selectedProviderFilter = plugin.name
                            } else {
                                selectedCategoryTab = "movies"
                                selectedProviderFilter = plugin.name
                            }
                        }
                    )
                }
            } else {
                // 6. Content Items List (Movies, Anime, Live Sports & TV, Audiobooks)
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = when (selectedCategoryTab) {
                                "movies" -> if (isBangla) "মুভি ও সিরিজ তালিকা (${currentCategoryContent.size} টি)" else "Movies & TV Shows (${currentCategoryContent.size})"
                                "anime" -> if (isBangla) "অ্যানিমে ও ড্রামা সিরিজ (${currentCategoryContent.size} টি)" else "Anime & Drama Series (${currentCategoryContent.size})"
                                "live" -> if (isBangla) "লাইভ ক্রিকেট ও টিভি চ্যানেল (${currentCategoryContent.size} টি)" else "Live Sports & TV Channels (${currentCategoryContent.size})"
                                else -> if (isBangla) "অডিওবুক তালিকা (${currentCategoryContent.size} টি)" else "Audiobooks (${currentCategoryContent.size})"
                            },
                            color = textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = if (isBangla) "সরাসরি দেখুন" else "Stream Ready",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(currentCategoryContent, key = { it.id }) { contentItem ->
                    ContentItemCard(
                        item = contentItem,
                        isDarkMode = isDarkMode,
                        isBangla = isBangla,
                        onPlayClick = {
                            onPlayMedia(contentItem.toMediaItem())
                        },
                        onDownloadClick = {
                            DownloadHelper.downloadMovie(
                                context = context,
                                title = contentItem.title,
                                downloadUrl = contentItem.downloadUrl,
                                posterUrl = contentItem.posterUrl
                            )
                        },
                        onDetailsClick = {
                            onOpenMediaDetails(contentItem.toMediaItem())
                        }
                    )
                }

                if (currentCategoryContent.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = textSecondary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isBangla) "কোনো কনটেন্ট পাওয়া যায়নি" else "No content found",
                                    color = textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBangla) "অন্য কোনো নাম বা প্রোভাইডার নির্বাচন করুন" else "Try selecting another provider file",
                                    color = textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Plugin Details Dialog
    selectedPluginForDetails?.let { plugin ->
        PluginDetailsModal(
            plugin = plugin,
            isDarkMode = isDarkMode,
            isBangla = isBangla,
            onDismiss = { selectedPluginForDetails = null },
            onToggleInstall = {
                onToggleInstall(plugin)
                selectedPluginForDetails = plugin.copy(isInstalled = !plugin.isInstalled)
            },
            onDownloadPackage = {
                DownloadHelper.downloadPluginPackage(context, plugin.name, plugin.url)
            },
            onWatchContent = {
                selectedPluginForDetails = null
                if (plugin.isAnime) {
                    selectedCategoryTab = "anime"
                    selectedProviderFilter = plugin.name
                } else if (plugin.isLiveOrSports) {
                    selectedCategoryTab = "live"
                    selectedProviderFilter = plugin.name
                } else {
                    selectedCategoryTab = "movies"
                    selectedProviderFilter = plugin.name
                }
            }
        )
    }
}

private data class CategoryTab(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun ContentItemCard(
    item: CNCContentItem,
    isDarkMode: Boolean,
    isBangla: Boolean,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    val cardBg = if (isDarkMode) Color(0xFF181A26) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111827)
    val textSecondary = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val borderColor = if (isDarkMode) Color(0xFF25283B) else Color(0xFFE5E7EB)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable { onPlayClick() }
            .testTag("content_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Poster with play overlay
                Box(
                    modifier = Modifier
                        .size(width = 85.dp, height = 110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDarkMode) Color(0xFF25283B) else Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Quality Badge on Top-Left
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 6.dp),
                        color = if (item.isLive) Color(0xFFEF4444) else Color(0xFF1F2937).copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = if (item.isLive) "LIVE 🔴" else item.quality,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    // Play icon in center
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Provider File Tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "ফাইল: ${item.providerName}",
                                color = Color(0xFFA78BFA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.rating,
                                color = if (isDarkMode) Color(0xFFFCD34D) else Color(0xFFD97706),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (isBangla) item.banglaTitle else item.title,
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isBangla && item.banglaTitle != item.title) {
                        Text(
                            text = item.title,
                            color = textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${item.year} • ${item.duration}",
                        color = textSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = item.description,
                        color = textSecondary,
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Play Stream / Download / Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onPlayClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("play_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "এখনই দেখুন" else "Watch Now",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onDownloadClick,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF10B981)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF10B981)
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBangla) "ডাউনলোড" else "Save",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onDetailsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginItemCard(
    plugin: CNCPluginItem,
    isDarkMode: Boolean,
    isBangla: Boolean,
    onToggleInstall: () -> Unit,
    onDetailsClick: () -> Unit,
    onDownloadPackage: () -> Unit,
    onBrowseContent: () -> Unit
) {
    val cardBg = if (isDarkMode) Color(0xFF181A26) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111827)
    val textSecondary = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val borderColor = if (isDarkMode) Color(0xFF25283B) else Color(0xFFE5E7EB)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable { onDetailsClick() }
            .testTag("plugin_file_${plugin.internalName}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Plugin Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDarkMode) Color(0xFF25283B) else Color(0xFFF3F4F6))
                        .border(
                            1.dp,
                            if (plugin.isBangla) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF8B5CF6).copy(alpha = 0.3f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (plugin.iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = plugin.iconUrl,
                            contentDescription = plugin.name,
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = plugin.name,
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Version badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDarkMode) Color(0xFF2F344A) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "v${plugin.version}",
                                color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF1E40AF),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        if (plugin.isBangla) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981)
                            ) {
                                Text(
                                    text = "বাংলা",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = plugin.language?.uppercase() ?: "ALL",
                                color = Color(0xFF3B82F6),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        plugin.tvTypes.take(2).forEach { type ->
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (isDarkMode) Color(0xFF282C3F) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = type,
                                    color = textSecondary,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Text(
                            text = "• ${plugin.formattedSize}",
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Install/Installed Toggle
                if (plugin.isInstalled) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { onToggleInstall() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "সক্রিয় আছে" else "Active",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onToggleInstall,
                        colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (isBangla) "সক্রিয় করুন" else "Activate",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Watch Content button
                    Button(
                        onClick = onBrowseContent,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0xFF3B82F6) else Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isBangla) "কনটেন্ট দেখুন" else "Watch",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Download .cs3 file
                    IconButton(
                        onClick = onDownloadPackage,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download .cs3",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Details
                    IconButton(
                        onClick = onDetailsClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginDetailsModal(
    plugin: CNCPluginItem,
    isDarkMode: Boolean,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onToggleInstall: () -> Unit,
    onDownloadPackage: () -> Unit,
    onWatchContent: () -> Unit
) {
    val context = LocalContext.current
    val dialogBg = if (isDarkMode) Color(0xFF1E202F) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111827)
    val textSecondary = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0xFF2E3245) else Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    if (plugin.iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = plugin.iconUrl,
                            contentDescription = plugin.name,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = plugin.name,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plugin.internalName,
                        color = textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (plugin.description.isNotBlank()) {
                    Text(
                        text = plugin.description,
                        color = textPrimary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                DetailRow(label = if (isBangla) "সংস্করণ" else "Version", value = "v${plugin.version} (API ${plugin.apiVersion})", textPrimary = textPrimary, textSecondary = textSecondary)
                DetailRow(label = if (isBangla) "ভাষা" else "Language", value = plugin.languageDisplay, textPrimary = textPrimary, textSecondary = textSecondary)
                DetailRow(label = if (isBangla) "ধরন" else "Types", value = plugin.tvTypes.joinToString(", ").ifEmpty { "General" }, textPrimary = textPrimary, textSecondary = textSecondary)
                DetailRow(label = if (isBangla) "ফাইলের আকার" else "File Size", value = plugin.formattedSize, textPrimary = textPrimary, textSecondary = textSecondary)
                DetailRow(label = if (isBangla) "লেখক" else "Authors", value = plugin.authors.joinToString(", ").ifEmpty { "NivinCNC" }, textPrimary = textPrimary, textSecondary = textSecondary)

                Spacer(modifier = Modifier.height(6.dp))

                // Action buttons inside modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Plugin URL", plugin.url)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "লিংক কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = if (isBangla) "কপি লিংক" else "Copy URL", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDownloadPackage,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = if (isBangla) "প্যাকেজ .cs3" else "Get .cs3", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onWatchContent,
                colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isBangla) "কনটেন্ট দেখুন" else "Watch Content",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isBangla) "বন্ধ করুন" else "Close",
                    color = textSecondary
                )
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, textPrimary: Color, textSecondary: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = textSecondary, fontSize = 12.sp)
        Text(text = value, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
