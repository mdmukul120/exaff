package com.example.data.repository

import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.example.data.model.DownloadedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActiveDownload(
    val downloadId: Long,
    val title: String,
    val downloadUrl: String,
    val posterUrl: String = "",
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val progress: Float = 0f,
    val speedFormatted: String = "0 KB/s",
    val etaFormatted: String = "",
    val statusText: String = "ডাউনলোড হচ্ছে...",
    val destinationPath: String = "",
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false
)

object DownloadHelper {

    private val _activeDownloads = MutableStateFlow<List<ActiveDownload>>(emptyList())
    val activeDownloads: StateFlow<List<ActiveDownload>> = _activeDownloads.asStateFlow()

    private var pollingJob: Job? = null
    private val previousBytesMap = mutableMapOf<Long, Long>()
    private var lastPollTimeMs = System.currentTimeMillis()

    fun getMukulPlusFolder(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val mukulPlusDir = File(publicDownloads, "Mukul plus")
        if (!mukulPlusDir.exists()) {
            mukulPlusDir.mkdirs()
        }
        return mukulPlusDir
    }

    fun downloadMovie(context: Context, title: String, downloadUrl: String, posterUrl: String = "") {
        if (downloadUrl.isEmpty()) {
            Toast.makeText(context, "ডাউনলোড লিংক পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val sanitized = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${sanitized.take(40)}.mp4"

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) {
                Toast.makeText(context, "DownloadManager পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                return
            }

            // Create target file inside "Download/Mukul plus" folder
            val mukulDir = getMukulPlusFolder()
            val targetFile = File(mukulDir, fileName)

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Mukul plus: $title")
                setDescription("Mukul plus ফোল্ডারে মুভি সেভ হচ্ছে...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                try {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Mukul plus/$fileName")
                } catch (e: Exception) {
                    // Fallback to absolute file URI if scoped storage restricts direct public dir setter
                    setDestinationUri(Uri.fromFile(targetFile))
                }
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = dm.enqueue(request)

            if (posterUrl.isNotEmpty()) {
                savePosterForFile(context, fileName, posterUrl)
                savePosterForFile(context, targetFile.nameWithoutExtension, posterUrl)
                savePosterForFile(context, title, posterUrl)
            }

            val newDownload = ActiveDownload(
                downloadId = downloadId,
                title = title,
                downloadUrl = downloadUrl,
                posterUrl = posterUrl,
                destinationPath = targetFile.absolutePath,
                statusText = "কানেক্ট হচ্ছে..."
            )

            _activeDownloads.value = _activeDownloads.value + newDownload
            startProgressPolling(context)

            Toast.makeText(context, "Mukul plus ফোল্ডারে ডাউনলোড শুরু হয়েছে: $title", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ডাউনলোড এরর: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun downloadPluginPackage(context: Context, pluginName: String, packageUrl: String) {
        if (packageUrl.isEmpty()) {
            Toast.makeText(context, "ডাউনলোড লিংক পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return
            val sanitized = pluginName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val fileName = "${sanitized.take(40)}.cs3"
            val mukulDir = getMukulPlusFolder()
            val targetFile = File(mukulDir, fileName)

            val request = DownloadManager.Request(Uri.parse(packageUrl)).apply {
                setTitle("CNC Plugin: $pluginName")
                setDescription("CloudStream 3 Extension (.cs3)")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                try {
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Mukul plus/$fileName")
                } catch (e: Exception) {
                    setDestinationUri(Uri.fromFile(targetFile))
                }
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            dm.enqueue(request)
            Toast.makeText(context, "প্লাগইন ডাউনলোড শুরু হয়েছে: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ডাউনলোড ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startProgressPolling(context: Context) {
        if (pollingJob?.isActive == true) return

        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            val appContext = context.applicationContext
            val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return@launch

            while (isActive) {
                val currentDownloads = _activeDownloads.value
                val activeList = currentDownloads.filter { !it.isCompleted && !it.isFailed }

                if (activeList.isEmpty()) {
                    break
                }

                val now = System.currentTimeMillis()
                val deltaSec = ((now - lastPollTimeMs) / 1000.0).coerceAtLeast(0.5)
                lastPollTimeMs = now

                val updatedList = currentDownloads.map { item ->
                    if (item.isCompleted || item.isFailed) {
                        item
                    } else {
                        val query = DownloadManager.Query().setFilterById(item.downloadId)
                        val cursor = try {
                            dm.query(query)
                        } catch (e: Exception) {
                            null
                        }

                        if (cursor != null && cursor.moveToFirst()) {
                            val bytesCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                            val bytesSoFar = if (bytesCol != -1) cursor.getLong(bytesCol) else 0L
                            val totalBytes = if (totalCol != -1) cursor.getLong(totalCol) else 0L
                            val status = if (statusCol != -1) cursor.getInt(statusCol) else DownloadManager.STATUS_RUNNING

                            val lastBytes = previousBytesMap[item.downloadId] ?: bytesSoFar
                            previousBytesMap[item.downloadId] = bytesSoFar

                            val bytesDiff = (bytesSoFar - lastBytes).coerceAtLeast(0L)
                            val speedBps = (bytesDiff / deltaSec).toLong()
                            val speedText = formatSpeed(speedBps)

                            val progress = if (totalBytes > 0) {
                                (bytesSoFar.toFloat() / totalBytes * 100f).coerceIn(0f, 100f)
                            } else {
                                0f
                            }

                            val etaText = if (totalBytes > 0 && speedBps > 1024) {
                                val remainingBytes = totalBytes - bytesSoFar
                                val remSec = (remainingBytes / speedBps).toInt()
                                formatEta(remSec)
                            } else {
                                ""
                            }

                            val isSuccess = status == DownloadManager.STATUS_SUCCESSFUL
                            val isFailed = status == DownloadManager.STATUS_FAILED

                            if (isSuccess) {
                                // Scan file so it appears in device storage gallery and file manager
                                MediaScannerConnection.scanFile(
                                    appContext,
                                    arrayOf(item.destinationPath),
                                    arrayOf("video/mp4"),
                                    null
                                )
                            }

                            val statusText = when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> "সম্পূর্ণ হয়েছে ✓"
                                DownloadManager.STATUS_FAILED -> "ব্যর্থ হয়েছে"
                                DownloadManager.STATUS_PAUSED -> "স্থগিত রয়েছে"
                                DownloadManager.STATUS_PENDING -> "অপেক্ষা করছে..."
                                else -> "ডাউনলোড হচ্ছে..."
                            }

                            cursor.close()

                            item.copy(
                                bytesDownloaded = bytesSoFar,
                                totalBytes = totalBytes,
                                progress = progress,
                                speedFormatted = speedText,
                                etaFormatted = etaText,
                                statusText = statusText,
                                isCompleted = isSuccess,
                                isFailed = isFailed
                            )
                        } else {
                            cursor?.close()
                            item
                        }
                    }
                }

                _activeDownloads.value = updatedList
                delay(800)
            }
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format(Locale.US, "%d KB/s", bytesPerSec / 1024)
            bytesPerSec > 0 -> "$bytesPerSec B/s"
            else -> "0 KB/s"
        }
    }

    private fun formatEta(seconds: Int): String {
        return when {
            seconds < 60 -> "$seconds সেকেন্ড বাকি"
            seconds < 3600 -> "${seconds / 60} মিনিট ${seconds % 60} সে. বাকি"
            else -> "${seconds / 3600} ঘণ্টা বাকি"
        }
    }

    fun cancelDownload(context: Context, downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        dm?.remove(downloadId)
        _activeDownloads.value = _activeDownloads.value.filter { it.downloadId != downloadId }
        previousBytesMap.remove(downloadId)
    }

    fun savePosterForFile(context: Context, key: String, posterUrl: String) {
        try {
            val prefs = context.getSharedPreferences("mukul_downloads_meta", Context.MODE_PRIVATE)
            val cleanKey = key.trim().lowercase()
            prefs.edit().putString(cleanKey, posterUrl).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun getPosterForFile(context: Context, key: String): String {
        return try {
            val prefs = context.getSharedPreferences("mukul_downloads_meta", Context.MODE_PRIVATE)
            val cleanKey = key.trim().lowercase()
            prefs.getString(cleanKey, "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun getMukulPlusSavedFiles(context: Context): List<DownloadedMedia> {
        val directories = listOfNotNull(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Mukul plus"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Mukul_plus"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Mukul plus"),
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Mukul plus"),
            File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Mukul_plus"),
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        )

        val foundFiles = mutableListOf<File>()
        val seenPaths = mutableSetOf<String>()

        for (dir in directories) {
            if (dir.exists() && dir.isDirectory) {
                val files = dir.listFiles()?.filter {
                    it.isFile && (it.name.endsWith(".mp4", true) ||
                            it.name.endsWith(".mkv", true) ||
                            it.name.endsWith(".webm", true) ||
                            it.name.endsWith(".avi", true))
                } ?: emptyList()

                for (f in files) {
                    if (seenPaths.add(f.absolutePath) && f.length() > 0) {
                        foundFiles.add(f)
                    }
                }
            }
        }

        val dateFormat = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
        val result = foundFiles.map { file ->
            val sizeMb = file.length() / (1024 * 1024)
            val formattedSize = if (sizeMb >= 1024) {
                String.format(Locale.US, "%.2f GB", sizeMb / 1024.0)
            } else {
                "$sizeMb MB"
            }

            val poster = getPosterForFile(context, file.name).ifEmpty {
                getPosterForFile(context, file.nameWithoutExtension).ifEmpty {
                    getPosterForFile(context, file.nameWithoutExtension.replace("_", " "))
                }
            }

            DownloadedMedia(
                id = file.name,
                title = file.nameWithoutExtension.replace("_", " "),
                filePath = file.absolutePath,
                fileSize = formattedSize,
                dateAdded = dateFormat.format(Date(file.lastModified())),
                posterUrl = poster,
                folderName = "Mukul plus"
            )
        }.toMutableList()

        return result.sortedByDescending { it.dateAdded }
    }

    fun getAllDeviceVideos(context: Context): List<DownloadedMedia> {
        val result = mutableListOf<DownloadedMedia>()
        val seenPaths = mutableSetOf<String>()
        val dateFormat = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())

        // 1. Load from standard Mukul plus folders
        val mukulFiles = getMukulPlusSavedFiles(context)
        for (m in mukulFiles) {
            if (seenPaths.add(m.filePath)) {
                result.add(m.copy(folderName = "Mukul plus"))
            }
        }

        // 2. Query MediaStore for all video files on the device across partitions
        try {
            val projection = arrayOf(
                android.provider.MediaStore.Video.Media._ID,
                android.provider.MediaStore.Video.Media.DISPLAY_NAME,
                android.provider.MediaStore.Video.Media.DATA,
                android.provider.MediaStore.Video.Media.SIZE,
                android.provider.MediaStore.Video.Media.DATE_MODIFIED,
                android.provider.MediaStore.Video.Media.DURATION,
                android.provider.MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )
            val sortOrder = "${android.provider.MediaStore.Video.Media.DATE_MODIFIED} DESC"

            context.contentResolver.query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.DATE_MODIFIED)
                val durationCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.DURATION)
                val bucketCol = cursor.getColumnIndex(android.provider.MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video_$id"
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val sizeBytes = cursor.getLong(sizeCol)
                    val dateModified = cursor.getLong(dateCol) * 1000L
                    val durationMs = if (durationCol >= 0) cursor.getLong(durationCol) else 0L
                    val bucketName = if (bucketCol >= 0) cursor.getString(bucketCol) ?: "অন্যান্য" else "অন্যান্য"

                    val finalPath = if (path.isNotEmpty()) path else {
                        android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        ).toString()
                    }

                    if (seenPaths.add(finalPath)) {
                        val sizeMb = sizeBytes / (1024 * 1024)
                        val formattedSize = if (sizeMb >= 1024) {
                            String.format(Locale.US, "%.2f GB", sizeMb / 1024.0)
                        } else {
                            "$sizeMb MB"
                        }

                        val durationStr = if (durationMs > 0) {
                            val sec = (durationMs / 1000) % 60
                            val min = (durationMs / (1000 * 60)) % 60
                            val hr = durationMs / (1000 * 60 * 60)
                            if (hr > 0) String.format("%d:%02d:%02d", hr, min, sec) else String.format("%02d:%02d", min, sec)
                        } else ""

                        val poster = getPosterForFile(context, name)

                        result.add(
                            DownloadedMedia(
                                id = id.toString(),
                                title = name.substringBeforeLast(".").replace("_", " "),
                                filePath = finalPath,
                                fileSize = formattedSize,
                                dateAdded = if (dateModified > 0) dateFormat.format(Date(dateModified)) else "",
                                posterUrl = poster,
                                folderName = bucketName,
                                durationFormatted = durationStr
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    fun importVideoFile(context: Context, uri: Uri): DownloadedMedia? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "Imported_Video_${System.currentTimeMillis()}.mp4"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            // Copy to Mukul plus folder for persistence
            val destFile = File(getMukulPlusFolder(), fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("video/*"),
                null
            )

            val sizeMb = destFile.length() / (1024 * 1024)
            val formattedSize = if (sizeMb >= 1024) {
                String.format(Locale.US, "%.2f GB", sizeMb / 1024.0)
            } else {
                "$sizeMb MB"
            }

            val dateFormat = SimpleDateFormat("dd MMM, yyyy • hh:mm a", Locale.getDefault())
            DownloadedMedia(
                id = destFile.name,
                title = destFile.nameWithoutExtension.replace("_", " "),
                filePath = destFile.absolutePath,
                fileSize = formattedSize,
                dateAdded = dateFormat.format(Date(destFile.lastModified()))
            )
        } catch (e: Exception) {
            null
        }
    }

    fun deleteDownloadedFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    fun getDownloadedFiles(context: Context): List<DownloadedMedia> = getMukulPlusSavedFiles(context)
}
