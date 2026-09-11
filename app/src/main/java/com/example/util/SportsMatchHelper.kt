package com.example.util

import com.example.data.model.SportsMatchItem
import com.example.data.model.SportsSource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class MatchScheduleInfo(
    val isPastEnded: Boolean,
    val isLive: Boolean,
    val isUpcoming: Boolean,
    val countdownBn: String,
    val countdownEn: String,
    val displayBadgeBn: String,
    val displayBadgeEn: String,
    val displayTime: String
)

object SportsMatchHelper {

    fun evaluateMatch(match: SportsMatchItem): MatchScheduleInfo {
        // Match highlights / replays are on-demand videos; they are never "ended" or filtered out
        if (match.categorySource == SportsSource.MATCH_HIGHLIGHTS) {
            return MatchScheduleInfo(
                isPastEnded = false,
                isLive = false,
                isUpcoming = false,
                countdownBn = "হাইলাইটস",
                countdownEn = "Highlights",
                displayBadgeBn = "হাইলাইটস",
                displayBadgeEn = "HIGHLIGHTS",
                displayTime = match.startTime.ifEmpty { "ফুল ম্যাচ রিপ্লে" }
            )
        }

        val statusClean = match.status.trim()
        val statusLower = statusClean.lowercase(Locale.ROOT)
        val timeLower = match.startTime.trim().lowercase(Locale.ROOT)

        // Check if explicitly finished or cancelled
        val endedStatuses = listOf(
            "finished", "completed", "ended", "ft", "full time", "result",
            "stumps", "abandoned", "cancelled", "postponed", "closed"
        )
        for (ended in endedStatuses) {
            if (statusLower.contains(ended)) {
                return MatchScheduleInfo(
                    isPastEnded = true,
                    isLive = false,
                    isUpcoming = false,
                    countdownBn = "ম্যাচ সমাপ্ত",
                    countdownEn = "Ended",
                    displayBadgeBn = "সমাপ্ত",
                    displayBadgeEn = "ENDED",
                    displayTime = match.startTime.ifEmpty { "ম্যাচ সমাপ্ত" }
                )
            }
        }

        // Check if explicitly live
        val isExplicitLive = statusLower.contains("live") ||
                statusLower.contains("in progress") ||
                statusLower.contains("running") ||
                statusLower.contains("break") ||
                timeLower.contains("live now") ||
                timeLower.contains("24/7")

        // Parse start time if available
        val now = System.currentTimeMillis()
        val parsedTime = parseTimeMillis(match.startTime)

        if (parsedTime != null) {
            val diff = parsedTime - now
            if (diff > 0) {
                // Match is in the future -> UPCOMING
                val totalMinutes = (diff / 60000L).coerceAtLeast(1)
                val days = totalMinutes / (24 * 60)
                val hours = (totalMinutes % (24 * 60)) / 60
                val mins = totalMinutes % 60

                val (cdBn, cdEn) = when {
                    days > 0 -> Pair("${days} দিন ${hours}ঘ বাকি", "Starts in ${days}d ${hours}h")
                    hours > 0 -> Pair("শুরু হতে ${hours}ঘ ${mins}মি বাকি", "Starts in ${hours}h ${mins}m")
                    else -> Pair("শুরু হতে ${mins}মি বাকি", "Starts in ${mins}m")
                }

                return MatchScheduleInfo(
                    isPastEnded = false,
                    isLive = false,
                    isUpcoming = true,
                    countdownBn = cdBn,
                    countdownEn = cdEn,
                    displayBadgeBn = "শীঘ্রই শুরু",
                    displayBadgeEn = "UPCOMING",
                    displayTime = match.startTime
                )
            } else {
                // Time has passed
                val elapsedMinutes = (-diff) / 60000L
                // Standard match duration: ~4 hours (240 mins)
                if (elapsedMinutes <= 270L || isExplicitLive) {
                    return MatchScheduleInfo(
                        isPastEnded = false,
                        isLive = true,
                        isUpcoming = false,
                        countdownBn = "🔴 সরাসরি সম্প্রচার চলছে",
                        countdownEn = "🔴 LIVE NOW",
                        displayBadgeBn = "সরাসরি",
                        displayBadgeEn = "LIVE",
                        displayTime = if (match.startTime.isNotBlank()) match.startTime else "Live"
                    )
                } else {
                    // Match ended more than 4.5 hours ago
                    return MatchScheduleInfo(
                        isPastEnded = true,
                        isLive = false,
                        isUpcoming = false,
                        countdownBn = "ম্যাচ সমাপ্ত",
                        countdownEn = "Ended",
                        displayBadgeBn = "সমাপ্ত",
                        displayBadgeEn = "ENDED",
                        displayTime = "Ended"
                    )
                }
            }
        }

        // When timestamp could not be parsed:
        if (isExplicitLive) {
            return MatchScheduleInfo(
                isPastEnded = false,
                isLive = true,
                isUpcoming = false,
                countdownBn = "🔴 সরাসরি সম্প্রচার চলছে",
                countdownEn = "🔴 LIVE NOW",
                displayBadgeBn = "সরাসরি",
                displayBadgeEn = "LIVE",
                displayTime = if (match.startTime.isNotBlank()) match.startTime else "Live"
            )
        }

        // If marked upcoming in status
        val isUpcomingStatus = statusLower.contains("upcoming") || statusLower.contains("soon")
        return MatchScheduleInfo(
            isPastEnded = false,
            isLive = !isUpcomingStatus,
            isUpcoming = isUpcomingStatus,
            countdownBn = if (isUpcomingStatus) "শীঘ্রই শুরু হবে" else "সরাসরি সম্প্রচার",
            countdownEn = if (isUpcomingStatus) "Upcoming" else "Live",
            displayBadgeBn = if (isUpcomingStatus) "শীঘ্রই" else "সরাসরি",
            displayBadgeEn = if (isUpcomingStatus) "UPCOMING" else "LIVE",
            displayTime = match.startTime.ifEmpty { if (isUpcomingStatus) "Upcoming" else "Live" }
        )
    }

    private fun parseTimeMillis(timeStr: String): Long? {
        if (timeStr.isBlank()) return null
        val trimmed = timeStr.trim()

        // Numeric timestamp check
        if (trimmed.all { it.isDigit() }) {
            val num = trimmed.toLongOrNull() ?: return null
            return if (num < 100_000_000_000L) num * 1000L else num
        }

        // Relative time pattern e.g. "Live at 4 PM BDT" or "Live at 7:15 PM BDT"
        if (trimmed.contains("at", ignoreCase = true) && (trimmed.contains("am", ignoreCase = true) || trimmed.contains("pm", ignoreCase = true))) {
            val cal = Calendar.getInstance()
            val timePortion = trimmed.substringAfter("at", "").trim()
            val cleanTime = timePortion.replace("BDT", "").replace("IST", "").replace("BST", "").trim()
            val formats = listOf("h:mm a", "h.mm a", "h a")
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US)
                    val parsed = sdf.parse(cleanTime)
                    if (parsed != null) {
                        val parsedCal = Calendar.getInstance().apply { time = parsed }
                        cal.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY))
                        cal.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE))
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        return cal.timeInMillis
                    }
                } catch (_: Exception) {}
            }
        }

        val patterns = listOf(
            "MM/dd/yyyy hh:mm:ss a",
            "dd/MM/yyyy hh:mm:ss a",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd MMM yyyy HH:mm",
            "dd-MM-yyyy HH:mm"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(trimmed)
                if (date != null) return date.time
            } catch (_: Exception) {}
        }

        return null
    }
}
