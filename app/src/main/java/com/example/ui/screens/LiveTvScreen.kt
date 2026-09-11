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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveChannel
import com.example.ui.components.ChannelCard
import com.example.ui.theme.MukulCardBg
import com.example.ui.theme.MukulDarkBg
import com.example.ui.theme.MukulRedPrimary
import com.example.ui.theme.MukulTextPrimary
import com.example.ui.theme.MukulTextSecondary

@Composable
fun LiveTvScreen(
    channels: List<LiveChannel>,
    selectedCategory: String,
    isBangla: Boolean,
    isDarkMode: Boolean = true,
    onSelectCategory: (String) -> Unit,
    onChannelClick: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenBg = if (isDarkMode) MukulDarkBg else Color(0xFFF6F7FB)
    val cardBg = if (isDarkMode) MukulCardBg else Color.White
    val cardBorder = if (isDarkMode) Color(0xFF2B2C3B) else Color(0xFFDFE1EB)
    val pillBg = if (isDarkMode) Color(0xFF1B1C26) else Color.White
    val pillBorder = if (isDarkMode) Color(0xFF262738) else Color(0xFFDFE1EB)
    val textColor = if (isDarkMode) Color.White else Color(0xFF15171E)

    var searchQuery by remember { mutableStateOf("") }

    // Ensure "Bangla" is prioritized as the first category tab!
    val categories = remember(channels) {
        val groups = channels.map { it.group }.filter { it.isNotEmpty() && !it.equals("Bangla", true) }.distinct().sorted()
        listOf("Bangla", "All") + groups
    }

    val filteredChannels = channels.filter { channel ->
        val matchesCategory = if (selectedCategory == "All") {
            true
        } else {
            channel.group.equals(selectedCategory, ignoreCase = true)
        }
        val matchesSearch = if (searchQuery.isEmpty()) true else channel.name.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .testTag("live_tv_screen")
    ) {
        // Channel Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = if (isBangla) "চ্যানেল খুঁজুন (চ্যানেল আই, এনটিভি, স্পোর্টস)..." else "Search 230+ live channels...",
                    color = MukulTextSecondary,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MukulTextSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = MukulRedPrimary,
                unfocusedBorderColor = cardBorder,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Category Filter Pills (Bangla placed first!)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                val displayName = if (category == "Bangla") {
                    if (isBangla) "বাংলা চ্যানেল" else "Bangla"
                } else if (category == "All") {
                    if (isBangla) "সকল" else "All"
                } else {
                    category
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MukulRedPrimary else pillBg,
                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, pillBorder),
                    modifier = Modifier.clickable { onSelectCategory(category) }
                ) {
                    Text(
                        text = displayName,
                        color = if (isSelected) Color.White else if (isDarkMode) MukulTextSecondary else Color(0xFF5A5D6E),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Channels Count Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isBangla) "${filteredChannels.size} টি চ্যানেল সক্রিয় রয়েছে" else "${filteredChannels.size} Channels Available",
                color = MukulTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Circular Channels Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 86.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredChannels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) },
                    circleSize = 74.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
