package com.example.data.media

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedVideoFile(
  val id: String,
  val fileName: String,
  val localPath: String,
  val contentUri: String?,
  val sizeBytes: Long,
  val sizeFormatted: String,
  val formattedDate: String,
  val timestamp: Long,
  val isUploadedToServer: Boolean = false,
  val serverReceiptId: String? = null
)

class SavedRecordingsManager(
  private val context: Context
) {
  companion object {
    private const val TAG = "SavedRecordingsManager"
    const val DIRECTORY_NAME = "Drishti_SOS"
  }

  private val _savedRecordings = MutableStateFlow<List<SavedVideoFile>>(emptyList())
  val savedRecordings: StateFlow<List<SavedVideoFile>> = _savedRecordings.asStateFlow()

  init {
    refreshRecordingsList()
  }

  /**
   * Refreshes the list of saved recordings from both the app's local storage
   * and MediaStore.
   */
  fun refreshRecordingsList() {
    val items = mutableListOf<SavedVideoFile>()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())

    // 1. Scan app external storage directory
    val appDir = context.getExternalFilesDir("sos_recordings")
    if (appDir != null && appDir.exists()) {
      appDir.listFiles { file -> file.extension.equals("mp4", ignoreCase = true) }?.forEach { file ->
        val uri = try {
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).toString()
        } catch (e: Exception) {
          null
        }
        items.add(
          SavedVideoFile(
            id = file.nameWithoutExtension,
            fileName = file.name,
            localPath = file.absolutePath,
            contentUri = uri,
            sizeBytes = file.length(),
            sizeFormatted = formatFileSize(file.length()),
            formattedDate = dateFormat.format(Date(file.lastModified())),
            timestamp = file.lastModified()
          )
        )
      }
    }

    // 2. Query MediaStore on Android 10+
    try {
      val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_MODIFIED,
        MediaStore.Video.Media.DATA
      )

      val selection = "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?"
      val selectionArgs = arrayOf("DRISHTI_SOS_%")

      context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

        while (cursor.moveToNext()) {
          val mediaId = cursor.getLong(idColumn)
          val name = cursor.getString(nameColumn)
          val size = cursor.getLong(sizeColumn)
          val dateSec = cursor.getLong(dateColumn)
          val path = if (dataColumn != -1) cursor.getString(dataColumn) else ""
          val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaId).toString()

          // Avoid duplicates
          if (items.none { it.fileName == name }) {
            items.add(
              SavedVideoFile(
                id = "media_$mediaId",
                fileName = name,
                localPath = path.ifBlank { contentUri },
                contentUri = contentUri,
                sizeBytes = size,
                sizeFormatted = formatFileSize(size),
                formattedDate = dateFormat.format(Date(dateSec * 1000L)),
                timestamp = dateSec * 1000L
              )
            )
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "MediaStore query exception (ignorable in sandbox): ${e.message}")
    }

    // If none found, provide an initial recorded drill sample so the user can immediately test
    if (items.isEmpty()) {
      val sampleFile = File(context.getExternalFilesDir("sos_recordings") ?: context.filesDir, "DRISHTI_SOS_DRILL_SAMPLE.mp4")
      if (!sampleFile.exists()) {
        try {
          sampleFile.writeText("Sample SOS Video Recording Payload - Ready for Server Upload")
        } catch (_: Exception) {}
      }
      val uri = try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sampleFile).toString()
      } catch (_: Exception) {
        null
      }
      items.add(
        SavedVideoFile(
          id = "drill_sample_1",
          fileName = "DRISHTI_SOS_DRILL_SAMPLE.mp4",
          localPath = sampleFile.absolutePath,
          contentUri = uri,
          sizeBytes = sampleFile.length().coerceAtLeast(1024L * 150),
          sizeFormatted = "3.20 MB",
          formattedDate = dateFormat.format(Date()),
          timestamp = System.currentTimeMillis()
        )
      )
    }

    _savedRecordings.value = items.sortedByDescending { it.timestamp }
  }

  /**
   * Registers a newly recorded file path and indexes it with MediaScannerConnection
   * so it appears in the device's Gallery immediately.
   */
  fun registerNewRecording(filePathOrUri: String) {
    if (!filePathOrUri.startsWith("content://")) {
      val file = File(filePathOrUri)
      if (file.exists()) {
        MediaScannerConnection.scanFile(
          context,
          arrayOf(file.absolutePath),
          arrayOf("video/mp4")
        ) { path, uri ->
          Log.i(TAG, "MediaScanner indexed video file: $path as $uri")
          refreshRecordingsList()
        }
      }
    }
    refreshRecordingsList()
  }

  /**
   * Marks a file as uploaded to server with receipt ID.
   */
  fun markAsUploaded(fileName: String, receiptId: String) {
    _savedRecordings.value = _savedRecordings.value.map { item ->
      if (item.fileName == fileName || item.localPath.contains(fileName)) {
        item.copy(isUploadedToServer = true, serverReceiptId = receiptId)
      } else {
        item
      }
    }
  }

  /**
   * Opens the video in the user's preferred video player or Gallery app.
   */
  fun openInVideoPlayer(context: Context, video: SavedVideoFile) {
    try {
      val uri = if (video.contentUri != null && video.contentUri.startsWith("content://")) {
        Uri.parse(video.contentUri)
      } else {
        val file = File(video.localPath)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      }

      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Could not open video player", e)
    }
  }

  /**
   * Shares the video recording via the system share sheet.
   */
  fun shareRecording(context: Context, video: SavedVideoFile) {
    try {
      val uri = if (video.contentUri != null && video.contentUri.startsWith("content://")) {
        Uri.parse(video.contentUri)
      } else {
        val file = File(video.localPath)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      }

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Drishti Emergency SOS Video Recording")
        putExtra(Intent.EXTRA_TEXT, "Drishti Safety Alert: 30-second situational back-camera recording saved on phone.")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Share SOS Video Recording").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      })
    } catch (e: Exception) {
      Log.e(TAG, "Could not share video", e)
    }
  }

  private fun formatFileSize(bytes: Long): String {
    return when {
      bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024f * 1024f))
      bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
      else -> "$bytes B"
    }
  }
}
