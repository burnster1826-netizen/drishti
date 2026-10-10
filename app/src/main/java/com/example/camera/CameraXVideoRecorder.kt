package com.example.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

class CameraXVideoRecorder(
  private val context: Context
) {
  private var videoCapture: VideoCapture<Recorder>? = null
  private var activeRecording: Recording? = null
  private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
  private var cameraProvider: ProcessCameraProvider? = null

  companion object {
    private const val TAG = "CameraXVideoRecorder"
  }

  fun setupVideoCapture(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView? = null,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    onReady: (() -> Unit)? = null
  ) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
      try {
        val provider = cameraProviderFuture.get()
        cameraProvider = provider

        val qualitySelector = QualitySelector.from(
          Quality.SD,
          FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )

        val recorder = Recorder.Builder()
          .setQualitySelector(qualitySelector)
          .setExecutor(mainExecutor)
          .build()

        videoCapture = VideoCapture.withOutput(recorder)

        // Prioritize BACK CAMERA as requested ("use back camera not front camera")
        val cameraSelector = if (lensFacing == CameraSelector.LENS_FACING_BACK && provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
          CameraSelector.DEFAULT_BACK_CAMERA
        } else if (lensFacing == CameraSelector.LENS_FACING_FRONT && provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
          CameraSelector.DEFAULT_FRONT_CAMERA
        } else if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
          CameraSelector.DEFAULT_BACK_CAMERA
        } else {
          CameraSelector.DEFAULT_FRONT_CAMERA
        }

        provider.unbindAll()

        if (previewView != null) {
          val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
          }
          provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            videoCapture
          )
        } else {
          provider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            videoCapture
          )
        }

        Log.i(TAG, "CameraX VideoCapture and Preview successfully bound using Back Camera ($cameraSelector)")
        onReady?.invoke()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to bind CameraX VideoCapture and Preview use cases", e)
      }
    }, mainExecutor)
  }

  @SuppressLint("MissingPermission")
  fun startRecording(
    onStarted: ((filePathOrUri: String) -> Unit)? = null,
    onFinalized: ((filePathOrUri: String, isSuccess: Boolean) -> Unit)? = null
  ): String {
    val currentCapture = videoCapture
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val videoFileName = "DRISHTI_SOS_${timeStamp}"

    val hasAudioPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    // 1. Prepare MediaStore Output for public Gallery insertion
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, "$videoFileName.mp4")
      put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Drishti_SOS")
      }
    }

    val mediaStoreOutput = MediaStoreOutputOptions.Builder(
      context.contentResolver,
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    // Fallback local file target path if MediaStore is restricted
    val localFile = File(context.getExternalFilesDir("sos_recordings") ?: context.filesDir, "$videoFileName.mp4")
    val defaultPath = localFile.absolutePath

    if (currentCapture == null) {
      Log.w(TAG, "VideoCapture not yet bound. Fallback path allocated: $defaultPath")
      onStarted?.invoke(defaultPath)
      return defaultPath
    }

    try {
      val pendingRecording = currentCapture.output.prepareRecording(context, mediaStoreOutput)

      if (hasAudioPermission) {
        pendingRecording.withAudioEnabled()
      }

      var savedUriString = defaultPath

      activeRecording = pendingRecording.start(mainExecutor) { recordEvent ->
        when (recordEvent) {
          is VideoRecordEvent.Start -> {
            Log.i(TAG, "Video recording session started to Gallery: $videoFileName")
            onStarted?.invoke(savedUriString)
          }
          is VideoRecordEvent.Finalize -> {
            if (!recordEvent.hasError()) {
              val outputUri = recordEvent.outputResults.outputUri
              val finalLocation = if (outputUri != Uri.EMPTY) outputUri.toString() else defaultPath
              Log.i(TAG, "Video recording saved to Gallery: $finalLocation")
              // Trigger media scanner so it appears in device's Gallery/Google Photos immediately
              try {
                android.media.MediaScannerConnection.scanFile(
                  context,
                  arrayOf(localFile.absolutePath),
                  arrayOf("video/mp4"),
                  null
                )
              } catch (_: Exception) {}
              onFinalized?.invoke(finalLocation, true)
            } else {
              Log.e(TAG, "Video recording finalized with error: ${recordEvent.error}")
              onFinalized?.invoke(defaultPath, false)
            }
          }
          else -> {}
        }
      }

      return "Gallery/Movies/Drishti_SOS/$videoFileName.mp4"
    } catch (e: Exception) {
      Log.e(TAG, "Exception with MediaStore recording, falling back to FileOutputOptions", e)
      // Fallback to direct app file output
      try {
        val fileOptions = FileOutputOptions.Builder(localFile).build()
        val fileRecording = currentCapture.output.prepareRecording(context, fileOptions)
        if (hasAudioPermission) {
          fileRecording.withAudioEnabled()
        }
        activeRecording = fileRecording.start(mainExecutor) { event ->
          if (event is VideoRecordEvent.Finalize && !event.hasError()) {
            onFinalized?.invoke(defaultPath, true)
          }
        }
        onStarted?.invoke(defaultPath)
      } catch (err: Exception) {
        Log.e(TAG, "Fallback recording also encountered error", err)
      }
      return defaultPath
    }
  }

  fun stopRecording() {
    activeRecording?.stop()
    activeRecording = null
  }
}
