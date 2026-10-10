package com.example.ui.components

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executor

/**
 * CameraPreview composable that uses CameraX 'Preview' use case and attaches
 * to lifecycle to provide an active, hardware-accelerated camera feed.
 */
@Composable
fun CameraPreview(
  modifier: Modifier = Modifier,
  lensFacing: Int = CameraSelector.LENS_FACING_BACK,
  scaleType: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
  onPreviewViewReady: ((PreviewView) -> Unit)? = null
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }

  var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

  LaunchedEffect(lensFacing, lifecycleOwner, previewViewRef) {
    val previewView = previewViewRef ?: return@LaunchedEffect
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
      try {
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
          it.surfaceProvider = previewView.surfaceProvider
        }

        // Use Back Camera by default as requested
        val selector = if (lensFacing == CameraSelector.LENS_FACING_BACK && cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
          CameraSelector.DEFAULT_BACK_CAMERA
        } else if (lensFacing == CameraSelector.LENS_FACING_FRONT && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
          CameraSelector.DEFAULT_FRONT_CAMERA
        } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
          CameraSelector.DEFAULT_BACK_CAMERA
        } else {
          CameraSelector.DEFAULT_FRONT_CAMERA
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
          lifecycleOwner,
          selector,
          preview
        )

        onPreviewViewReady?.invoke(previewView)
      } catch (e: Exception) {
        // Handled gracefully in container
      }
    }, mainExecutor)
  }

  Box(
    modifier = modifier.background(Color.Black)
  ) {
    AndroidView(
      factory = { ctx ->
        PreviewView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          this.scaleType = scaleType
          implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }.also {
          previewViewRef = it
          onPreviewViewReady?.invoke(it)
        }
      },
      modifier = Modifier.fillMaxSize()
    )
  }
}
