package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.DownloadedMedia
import com.example.data.repository.ActiveDownload
import com.example.data.repository.DownloadHelper
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedGlowing
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary
import java.io.File

@Composable
fun DownloadsScreen(
    isBangla: Boolean,
    isDarkMode: Boolean,
    onPlayOfflineMedia: (DownloadedMedia) -> Unit,
    isVaultUnlocked: Boolean = false,
    onUnlockVault: (String) -> Boolean = { false },
    onOpenVault: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDownloads by DownloadHelper.activeDownloads.collectAsState()
    var allVideos by remember { mutableStateOf<List<DownloadedMedia>>(emptyList()) }
    var selectedPartition by remember { mutableStateOf("সমস্ত ভিডিও") }
    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(checkStoragePermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasPermission = granted
        refreshKey++
    }

    // Pick video from device to import
    val importVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val imported = DownloadHelper.importVideoFile(context, uri)
            if (imported != null) {
                Toast.makeText(context, "ভিডিও যুক্ত করা হয়েছে", Toast.LENGTH_SHORT).show()
                refreshKey++
            }
        }
    }

    // Load partitioned video files from device
    LaunchedEffect(refreshKey, hasPermission) {
        isLoading = true
        allVideos = try {
            DownloadHelper.getAllDeviceVideos(context)
        } catch (e: Exception) {
            emptyList()
        }
        isLoading = false
    }

    // Dynamic partition list based on discovered folders
    val partitions = remember(allVideos) {
        val found = allVideos.map { it.folderName }.filter { it.isNotBlank() }.distinct()
        listOf("সমস্ত ভিডিও") + found
    }

    val filteredVideos = remember(allVideos, selectedPartition, searchQuery) {
        allVideos.filter { video ->
            val matchesPartition = if (selectedPartition == "সমস্ত ভিডিও") true
            else video.folderName.equals(selectedPartition, ignoreCase = true)

            val matchesSearch = if (searchQuery.isBlank()) true
            else video.title.contains(searchQuery, ignoreCase = true) ||
                    video.filePath.contains(searchQuery, ignoreCase = true)

            matchesPartition && matchesSearch
        }
    }

    val screenBg = if (isDarkMode) MukulDarkBg else Color(0xFFF6F7FB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)
    val cardBg = if (isDarkMode) Color(0xFF161722) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("downloads_screen")
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = if (isBangla) "ভিডিও গ্যালারি ও ডাউনলোড" else "Video Gallery & Downloads",
                    color = textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = if (isBangla) "ডিভাইস পার্টিশন ভিত্তিক সমস্ত ভিডিও (${filteredVideos.size} টি)"
                    else "All videos by device partitions (${filteredVideos.size})",
                    color = MukulTextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Secret Vault / Locked Folder Button
                IconButton(
                    onClick = {
                        if (isVaultUnlocked) {
                            onOpenVault()
                        } else {
                            showPasswordDialog = true
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0xFF2B2214) else Color(0xFFFFF7E6))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Secret Vault",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // View toggle: Grid vs List
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0xFF222436) else Color(0xFFE2E4EE))
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle View",
                        tint = MukulRedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Refresh Button
                IconButton(
                    onClick = { refreshKey++ },
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
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (isBangla) "ভিডিও খুঁজুন..." else "Search videos...",
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
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MukulTextSecondary, modifier = Modifier.size(16.dp))
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
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .height(48.dp)
        )

        // Device Partition Pills (Horizontal scroll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Secret Folder Lock Partition Chip
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isVaultUnlocked) Color(0xFFF59E0B).copy(alpha = 0.18f) else if (isDarkMode) Color(0xFF241C15) else Color(0xFFFFF8EE),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.clickable {
                        if (isVaultUnlocked) {
                            onOpenVault()
                        } else {
                            showPasswordDialog = true
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isBangla) "গোপন ফোল্ডার" else "Secret Vault",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(partitions) { part ->
                val isSelected = selectedPartition == part
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MukulRedPrimary else if (isDarkMode) Color(0xFF1B1C26) else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) MukulRedGlowing else cardBorder),
                    modifier = Modifier.clickable { selectedPartition = part }
                ) {
                    Text(
                        text = part,
                        color = if (isSelected) Color.White else textPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Active Downloads Banner (if any running)
        val runningDownloads = activeDownloads.filter { !it.isCompleted && !it.isFailed }
        if (runningDownloads.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                runningDownloads.forEach { download ->
                    ActiveDownloadBanner(download = download, isDarkMode = isDarkMode, isBangla = isBangla)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // Permission Banner (if not granted)
        if (!hasPermission) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MukulRedPrimary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MukulRedPrimary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "ডিভাইসের সব ভিডিও দেখতে অনুমতি দিন" else "Storage permission required",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "পার্টিশন ও ডাউনলোড ফোল্ডার স্ক্যান করার জন্য পারমিশন দিন" else "Grant access to scan device partitions",
                            color = MukulTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(perms)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isBangla) "অনুমতি দিন" else "Grant",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Main Video Gallery / List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MukulRedPrimary)
            }
        } else if (filteredVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoFile,
                        contentDescription = null,
                        tint = MukulTextSecondary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isBangla) "এই পার্টিশনে কোনো ভিডিও পাওয়া যায়নি" else "No videos found in this partition",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBangla) "ডিভাইস থেকে ভিডিও ইমপোর্ট করুন অথবা ডাউনলোড করুন" else "Import video from storage or download",
                        color = MukulTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { importVideoLauncher.launch("video/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isBangla) "ভিডিও ইমপোর্ট করুন" else "Import Video", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (isGridView) {
            // Gallery Grid View
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredVideos, key = { it.filePath }) { video ->
                    GalleryVideoCard(
                        video = video,
                        isDarkMode = isDarkMode,
                        onClick = { onPlayOfflineMedia(video) },
                        onDelete = {
                            DownloadHelper.deleteDownloadedFile(video.filePath)
                            refreshKey++
                            Toast.makeText(context, "ভিডিও মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        onShare = {
                            shareVideo(context, video.filePath)
                        }
                    )
                }
            }
        } else {
            // Clean List View
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredVideos, key = { it.filePath }) { video ->
                    ListVideoItem(
                        video = video,
                        isDarkMode = isDarkMode,
                        onClick = { onPlayOfflineMedia(video) },
                        onDelete = {
                            DownloadHelper.deleteDownloadedFile(video.filePath)
                            refreshKey++
                            Toast.makeText(context, "ভিডিও মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    if (showPasswordDialog) {
        PasswordPromptDialog(
            isDarkMode = isDarkMode,
            isBangla = isBangla,
            onUnlockSuccess = {
                val success = onUnlockVault("01716649945")
                if (success) {
                    showPasswordDialog = false
                    onOpenVault()
                }
            },
            onDismiss = { showPasswordDialog = false }
        )
    }
}

@Composable
private fun GalleryVideoCard(
    video: DownloadedMedia,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkMode) Color(0xFF161722) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            // Thumbnail / Poster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = video.posterUrl.ifEmpty { video.filePath },
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Partition Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = video.folderName,
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Play Button Center
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(MukulRedPrimary.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Duration Badge
                if (video.durationFormatted.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = video.durationFormatted,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Info Details
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = video.title,
                    color = textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = video.fileSize,
                        color = MukulTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row {
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = MukulTextSecondary, modifier = Modifier.size(13.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MukulRedPrimary, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListVideoItem(
    video: DownloadedMedia,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkMode) Color(0xFF161722) else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)
    val textPrimary = if (isDarkMode) MukulTextPrimary else Color(0xFF15171E)

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp, 45.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = video.posterUrl.ifEmpty { video.filePath },
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(MukulRedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    color = textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.folderName} • ${video.fileSize}",
                    color = MukulTextSecondary,
                    fontSize = 10.sp
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MukulRedPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ActiveDownloadBanner(
    download: ActiveDownload,
    isDarkMode: Boolean,
    isBangla: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkMode) Color(0xFF1E2030) else Color(0xFFE8EAF4),
        border = BorderStroke(1.dp, MukulRedPrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = download.title,
                    color = if (isDarkMode) MukulTextPrimary else Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(download.progress * 100).toInt()}% • ${download.speedFormatted}",
                    color = MukulRedPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { download.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = MukulRedPrimary,
                trackColor = if (isDarkMode) Color(0xFF33354A) else Color(0xFFCBD0E0)
            )
        }
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun shareVideo(context: Context, path: String) {
    try {
        val file = File(path)
        val uri = if (path.startsWith("content://")) Uri.parse(path)
        else androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "ভিডিও শেয়ার করুন"))
    } catch (e: Exception) {
        Toast.makeText(context, "শেয়ার করা সম্ভব হয়নি: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
