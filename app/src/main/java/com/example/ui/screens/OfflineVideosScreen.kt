package com.example.ui.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.DownloadedMedia
import com.example.data.repository.DownloadHelper
import com.example.ui.theme.MukulCardBg
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OfflineVideosScreen(
    isBangla: Boolean,
    isDarkMode: Boolean,
    onPlayOfflineMedia: (DownloadedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0: In-App Downloads, 1: Device Storage Videos
    var inAppDownloads by remember { mutableStateOf<List<DownloadedMedia>>(emptyList()) }
    var deviceVideos by remember { mutableStateOf<List<DownloadedMedia>>(emptyList()) }
    var hasStoragePermission by remember {
        mutableStateOf(checkStoragePermission(context))
    }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        hasStoragePermission = granted
        if (granted) {
            refreshTrigger++
            Toast.makeText(
                context,
                if (isBangla) "পারমিশন সফলভাবে অনুমোদিত হয়েছে" else "Storage permission granted",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                if (isBangla) "ডিভাইস ভিডিও দেখতে মেমোরি পারমিশন প্রয়োজন" else "Permission needed to access device videos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Storage File Picker fallback (zero permission needed)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri) ?: "Local_Video.mp4"
            val item = DownloadedMedia(
                id = uri.toString(),
                title = fileName.replace(Regex("\\.[a-zA-Z0-9]+$"), "").replace("_", " "),
                filePath = uri.toString(),
                fileSize = "Device File",
                dateAdded = "Now"
            )
            onPlayOfflineMedia(item)
        }
    }

    // Reload files on refresh
    LaunchedEffect(refreshTrigger, hasStoragePermission) {
        inAppDownloads = DownloadHelper.getDownloadedFiles(context)
        if (hasStoragePermission) {
            deviceVideos = queryDeviceVideos(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkMode) MukulDarkBg else Color(0xFFF4F5F9))
            .padding(horizontal = 16.dp)
            .testTag("offline_videos_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = if (isBangla) "অফলাইন প্লেয়ার ও ডাউনলোড" else "Offline Player & Downloads",
                    color = if (isDarkMode) MukulTextPrimary else Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isBangla) "ইন্টারনেট ছাড়া সরাসরি অফলাইনে ভিডিও দেখুন" else "Watch downloaded & device videos offline",
                    color = MukulTextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { refreshTrigger++ },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF1E202E) else Color(0xFFE2E4EE))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MukulRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Selector (In-App Downloads vs Device Videos)
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = if (isDarkMode) Color(0xFF161824) else Color(0xFFE7E9F3),
            contentColor = MukulRedPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSection]),
                    color = MukulRedPrimary,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "ইন-অ্যাপ ফাইল (${inAppDownloads.size})" else "Downloads (${inAppDownloads.size})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                selectedContentColor = MukulRedPrimary,
                unselectedContentColor = MukulTextSecondary
            )

            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "ডিভাইস ভিডিও" else "Device Storage",
                            fontSize = 13.sp,
                            fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                },
                selectedContentColor = MukulRedPrimary,
                unselectedContentColor = MukulTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Open custom file directly button (always works zero permission)
        OutlinedButton(
            onClick = {
                openDocumentLauncher.launch(arrayOf("video/*"))
            },
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MukulRedPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FileOpen,
                contentDescription = null,
                tint = MukulRedPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isBangla) "ফাইল ম্যানেজার থেকে যেকোনো ভিডিও খুলুন" else "Open Any Video File From Storage",
                color = MukulRedPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        if (selectedSection == 0) {
            // In-App Downloads List
            if (inAppDownloads.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = MukulTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBangla) "কোনো ডাউনলোডকৃত ভিডিও নেই" else "No downloaded movies or shows yet",
                            color = if (isDarkMode) Color.White else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBangla) "হোম বা মুভিজ থেকে 'ডাউনলোড' এ ট্যাপ করে অফলাইনে দেখার জন্য সংরক্ষণ করুন।" else "Tap 'Download' on any movie or series to watch offline!",
                            color = MukulTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(inAppDownloads, key = { it.id }) { item ->
                        OfflineVideoCard(
                            item = item,
                            isDarkMode = isDarkMode,
                            isBangla = isBangla,
                            onPlay = { onPlayOfflineMedia(item) },
                            onDelete = {
                                DownloadHelper.deleteDownloadedFile(item.filePath)
                                refreshTrigger++
                                Toast.makeText(
                                    context,
                                    if (isBangla) "ফাইল ডিলিট করা হয়েছে" else "File deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        } else {
            // Device Storage Videos
            if (!hasStoragePermission) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MukulRedPrimary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBangla) "স্টোরেজ পারমিশন প্রয়োজন" else "Storage Permission Needed",
                            color = if (isDarkMode) Color.White else Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBangla) "আপনার ফোনের মেমোরি কার্ড ও স্টোরেজে থাকা ভিডিওগুলো Mukul Plus-এ প্লে করতে অনুমতি দিন।" else "Grant permission to scan and play video files stored in your phone's memory.",
                            color = MukulTextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionLauncher.launch(permissionsToRequest)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isBangla) "অনুমতি দিন (Grant Permission)" else "Grant Permission",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else if (deviceVideos.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = MukulTextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isBangla) "ফোনে কোনো ভিডিও পাওয়া যায়নি" else "No video files found on device",
                            color = if (isDarkMode) Color.White else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBangla) "উপরের 'ফাইল ম্যানেজার' বাটন ব্যবহার করে সরাসরি ফাইল নির্বাচন করুন।" else "You can also tap the button above to pick a file directly.",
                            color = MukulTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(deviceVideos, key = { it.id }) { item ->
                        OfflineVideoCard(
                            item = item,
                            isDarkMode = isDarkMode,
                            isBangla = isBangla,
                            onPlay = { onPlayOfflineMedia(item) },
                            onDelete = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineVideoCard(
    item: DownloadedMedia,
    isDarkMode: Boolean,
    isBangla: Boolean,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkMode) MukulCardBg else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDarkMode) Color(0xFF222436) else Color(0xFFE2E4EE)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Poster / Play Thumbnail Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDarkMode) Color(0xFF141620) else Color(0xFFEEF0FA),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (item.posterUrl.isNotEmpty()) {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MukulRedPrimary.copy(alpha = 0.9f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = if (isDarkMode) MukulTextPrimary else Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MukulRedPrimary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.fileSize,
                            color = MukulRedPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (item.dateAdded.isNotEmpty()) {
                        Text(
                            text = "• ${item.dateAdded}",
                            color = MukulTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun queryDeviceVideos(context: Context): List<DownloadedMedia> {
    val list = mutableListOf<DownloadedMedia>()
    try {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

            var count = 0
            while (c.moveToNext() && count < 60) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "Video"
                val sizeBytes = c.getLong(sizeCol)
                val dateSec = c.getLong(dateCol)

                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                val sizeMb = sizeBytes / (1024 * 1024)
                val formattedSize = if (sizeMb > 1024) String.format(Locale.US, "%.1f GB", sizeMb / 1024.0) else "$sizeMb MB"
                val dateStr = if (dateSec > 0) dateFormat.format(Date(dateSec * 1000)) else ""

                list.add(
                    DownloadedMedia(
                        id = id.toString(),
                        title = name.replace(Regex("\\.[a-zA-Z0-9]+$"), "").replace("_", " "),
                        filePath = uri.toString(),
                        fileSize = formattedSize,
                        dateAdded = dateStr
                    )
                )
                count++
            }
        }
    } catch (e: Exception) {
        // ignored
    }
    return list
}

private fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
    }
    return name ?: uri.lastPathSegment
}
