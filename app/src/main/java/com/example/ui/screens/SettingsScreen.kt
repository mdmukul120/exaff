package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadedMedia
import com.example.data.repository.DownloadHelper
import com.example.ui.theme.MukulCardBg
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary

@Composable
fun SettingsScreen(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    selectedQuality: String,
    onQualityChange: (String) -> Unit,
    onPlayOfflineMedia: (DownloadedMedia) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var downloadedFiles by remember { mutableStateOf<List<DownloadedMedia>>(emptyList()) }
    var refreshKey by remember { mutableStateOf(0) }

    val openDocumentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Local_Video.mp4"
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

    LaunchedEffect(refreshKey) {
        downloadedFiles = DownloadHelper.getDownloadedFiles(context)
    }

    val isBangla = currentLanguage == "bn"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDarkMode) MukulDarkBg else Color(0xFFF4F5F9))
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }
                Text(
                    text = if (isBangla) "অ্যাপ সেটিংস" else "App Settings",
                    color = if (isDarkMode) MukulTextPrimary else Color(0xFF111218),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 1. Language Selection (ভাষা নির্বাচন)
        item {
            SettingsCard(isDarkMode = isDarkMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = MukulRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "অ্যাপের ভাষা" else "App Language",
                            color = if (isDarkMode) MukulTextPrimary else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isBangla) "বাংলা / English পরিবর্তন করুন" else "Switch between Bangla / English",
                            color = MukulTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LanguageOptionChip(
                        title = "বাংলা (Bangla)",
                        isSelected = currentLanguage == "bn",
                        onClick = { onLanguageChange("bn") },
                        modifier = Modifier.weight(1f)
                    )
                    LanguageOptionChip(
                        title = "English",
                        isSelected = currentLanguage == "en",
                        onClick = { onLanguageChange("en") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Theme Selection (ডার্ক / লাইট মোড)
        item {
            SettingsCard(isDarkMode = isDarkMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness4,
                        contentDescription = "Theme",
                        tint = MukulRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "ডার্ক মোড (Dark Theme)" else "Dark Theme",
                            color = if (isDarkMode) MukulTextPrimary else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isBangla) "চোখের আরাম ও ব্যাটারি সাশ্রয়ী" else "Easy on the eyes and saves battery",
                            color = MukulTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onThemeToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MukulRedPrimary,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }
        }

        // 3. Video Playback Quality
        item {
            SettingsCard(isDarkMode = isDarkMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = "Quality",
                        tint = MukulRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isBangla) "ডিফল্ট ভিডিও কোয়ালিটি" else "Default Video Quality",
                            color = if (isDarkMode) MukulTextPrimary else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isBangla) "স্ট্রিমিং ও ডাউনলোডের মান" else "Preferred stream resolution",
                            color = MukulTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val qualities = listOf("Auto", "1080p", "720p", "480p")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    qualities.forEach { q ->
                        val isSelected = selectedQuality == q
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MukulRedPrimary else if (isDarkMode) Color(0xFF222433) else Color(0xFFE2E4EE),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQualityChange(q) }
                        ) {
                            Text(
                                text = q,
                                color = if (isSelected) Color.White else if (isDarkMode) MukulTextSecondary else Color(0xFF333344),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 4. In-App Downloaded Movies & Offline Playback (অ্যাপ্লিকেশন এর ভিতরই থাকবে)
        item {
            SettingsCard(isDarkMode = isDarkMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads",
                        tint = MukulRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "ইন-অ্যাপ ডাউনলোড ফাইল (${downloadedFiles.size})" else "Downloaded Media (${downloadedFiles.size})",
                            color = if (isDarkMode) MukulTextPrimary else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isBangla) "ইন্টারনেট ছাড়া সরাসরি অ্যাপে দেখুন" else "Watch offline without internet inside app",
                            color = MukulTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (downloadedFiles.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = if (isBangla) "কোনো ডাউনলোডকৃত ফাইল নেই।\nমুভি বা সিরিজের 'ডাউনলোড' বাটনে ক্লিক করে অফলাইনে উপভোগ করুন!" else "No downloaded files yet.\nClick Download on any movie to watch offline!",
                            color = MukulTextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        downloadedFiles.forEach { item ->
                            DownloadedItemRow(
                                item = item,
                                isDarkMode = isDarkMode,
                                isBangla = isBangla,
                                onPlay = { onPlayOfflineMedia(item) },
                                onDelete = {
                                    DownloadHelper.deleteDownloadedFile(item.filePath)
                                    refreshKey++
                                    Toast.makeText(
                                        context,
                                        if (isBangla) "ফাইল মুছে ফেলা হয়েছে" else "File deleted",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("video/*"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MukulRedPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MukulRedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "ডিভাইস স্টোরেজ থেকে ভিডিও প্লে করুন" else "Play Video From Device Storage",
                        color = MukulRedPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. Cache Clear (ক্যাশ পরিষ্কার)
        item {
            SettingsCard(isDarkMode = isDarkMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = "Clear Cache",
                        tint = MukulRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "ক্যাশ পরিষ্কার করুন" else "Clear App Cache",
                            color = if (isDarkMode) MukulTextPrimary else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isBangla) "অ্যাপের গতি বৃদ্ধি ও জায়গা খালি করতে" else "Free up temporary space and speed up app",
                            color = MukulTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            try {
                                context.cacheDir?.deleteRecursively()
                                Toast.makeText(
                                    context,
                                    if (isBangla) "ক্যাশ মেমোরি পরিষ্কার করা হয়েছে!" else "Cache memory cleared successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                // ignored
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MukulRedPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isBangla) "পরিষ্কার" else "Clear",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 6. About Mukul Plus
        item {
            SettingsCard(isDarkMode = isDarkMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = MukulRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mukul Plus v2.0.0",
                            color = if (isDarkMode) MukulTextPrimary else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBangla) "সকল কন্টেন্ট সম্পূর্ণ ফ্রি • আনলিমিটেড স্ট্রিমিং ও ডাউনলোড" else "All content 100% Free • Unlimited Streaming & Downloads",
                            color = MukulTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) MukulCardBg else Color.White,
        border = if (isDarkMode) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222433)) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E4EE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun LanguageOptionChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MukulRedPrimary else Color(0xFF202230),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MukulRedPrimary else Color(0xFF323447)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else MukulTextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun DownloadedItemRow(
    item: DownloadedMedia,
    isDarkMode: Boolean,
    isBangla: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkMode) Color(0xFF191B26) else Color(0xFFECEEF6),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MukulRedPrimary,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onPlay)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = if (isDarkMode) MukulTextPrimary else Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.fileSize} • ${item.dateAdded}",
                    color = MukulTextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
