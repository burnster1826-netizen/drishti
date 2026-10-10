package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraXVideoRecorder
import com.example.location.ContactDispatchStatus
import com.example.location.LiveTrackingState
import com.example.ui.theme.*
import com.example.ui.viewmodel.SosActiveUiState

@Composable
fun FullscreenSosOverlay(
  sosState: SosActiveUiState,
  liveTrackingState: LiveTrackingState = LiveTrackingState(),
  contactStatuses: Map<String, ContactDispatchStatus> = emptyMap(),
  onCancelSos: () -> Unit,
  onDismiss: () -> Unit,
  onToggleCamera: (() -> Unit)? = null,
  onPlaySavedVideo: (() -> Unit)? = null,
  onShareSavedVideo: (() -> Unit)? = null,
  onToggleContactStream: ((String) -> Unit)? = null,
  onOpenGoogleMaps: ((Double, Double) -> Unit)? = null,
  onShareLiveLocation: (() -> Unit)? = null,
  onSendSmsToContact: ((ContactDispatchStatus) -> Unit)? = null,
  onPermissionsGranted: (() -> Unit)? = null,
  onRecordingPathLinked: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val videoRecorder = remember { CameraXVideoRecorder(context) }
  var recordingFilePath by remember { mutableStateOf<String?>(null) }
  var internalLensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

  // Sync with ViewModel lensFacing
  LaunchedEffect(sosState.cameraLensFacing) {
    internalLensFacing = sosState.cameraLensFacing
  }

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  var hasAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }

  var hasLocationPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    )
  }

  var hasStoragePermission by remember {
    mutableStateOf(
      if (android.os.Build.VERSION.SDK_INT <= 28) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
      } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { perms ->
    val cameraOk = perms[Manifest.permission.CAMERA] ?: false
    val audioOk = perms[Manifest.permission.RECORD_AUDIO] ?: false
    val locOk = (perms[Manifest.permission.ACCESS_FINE_LOCATION] ?: false) ||
                (perms[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)

    val storageOk = if (android.os.Build.VERSION.SDK_INT <= 28) {
      perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: true
    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      perms[Manifest.permission.READ_MEDIA_VIDEO] ?: true
    } else true

    hasCameraPermission = cameraOk
    hasAudioPermission = audioOk
    hasLocationPermission = locOk
    hasStoragePermission = storageOk

    if (cameraOk) {
      onPermissionsGranted?.invoke()
    }
  }

  var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }

  // Setup CameraX VideoCapture with Back Camera by default
  LaunchedEffect(hasCameraPermission, lifecycleOwner, previewViewInstance, internalLensFacing) {
    if (hasCameraPermission) {
      videoRecorder.setupVideoCapture(
        lifecycleOwner = lifecycleOwner,
        previewView = previewViewInstance,
        lensFacing = internalLensFacing
      )
    }
  }

  // Automatically initiate recording session when SOS enters ACTIVE mode
  LaunchedEffect(sosState.statusStage, hasCameraPermission) {
    if (sosState.statusStage == "ACTIVE" && hasCameraPermission) {
      val path = videoRecorder.startRecording(
        onStarted = { filePath ->
          recordingFilePath = filePath
          onRecordingPathLinked?.invoke(filePath)
        },
        onFinalized = { filePath, isSuccess ->
          recordingFilePath = filePath
        }
      )
      recordingFilePath = path
      onRecordingPathLinked?.invoke(path)
    }
  }

  // Clean up recording on cancellation or dismiss
  DisposableEffect(Unit) {
    onDispose {
      videoRecorder.stopRecording()
    }
  }

  // Automatically request Location, Camera, Audio & Storage permissions
  LaunchedEffect(Unit) {
    if (!hasCameraPermission || !hasAudioPermission || !hasLocationPermission || !hasStoragePermission) {
      val permissionsToRequest = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
      if (android.os.Build.VERSION.SDK_INT <= 28) {
        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
      }
      permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            Color(0xFF26050C),
            NavyBackground,
            Color(0xFF090D16)
          )
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(14.dp)
      .testTag("sos_fullscreen_overlay")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top status badge
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(
                when (sosState.statusStage) {
                  "CANCELLED" -> SafetyAmber
                  "DELIVERED" -> SafetyEmerald
                  "UPLOADING" -> SafetyCyan
                  else -> CrimsonEmergencyGlow
                }
              )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "PROTOCOL: ${sosState.statusStage}",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }

        if (sosState.statusStage == "DELIVERED" || sosState.statusStage == "CANCELLED") {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("dismiss_sos_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Emergency Overlay",
              tint = TextPrimary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Stage 1: 10s Countdown Window (Cancelable)
      if (sosState.statusStage == "PENDING") {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CrimsonEmergency.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Emergency Alert Dispatches in",
              color = TextSecondary,
              fontSize = 12.sp
            )
            Text(
              text = "${sosState.countdownRemainingSeconds}",
              fontSize = 54.sp,
              fontWeight = FontWeight.Black,
              color = CrimsonEmergencyGlow
            )
            Text(
              text = "SECONDS (GRACE PERIOD)",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = TextSecondary,
              letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
              progress = { sosState.countdownRemainingSeconds / 10f },
              modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(CircleShape),
              color = CrimsonEmergency,
              trackColor = NavySurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prominent Cancel Button
        Button(
          onClick = onCancelSos,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, Color(0xFF64748B), RoundedCornerShape(14.dp))
            .testTag("cancel_sos_button")
        ) {
          Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = null,
            tint = CrimsonEmergencyGlow,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "CANCEL SOS (I'M SAFE)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
        }
      }

      // Camera recording View (Compact)
      if (sosState.statusStage == "ACTIVE" || sosState.statusStage == "PENDING") {
        Spacer(modifier = Modifier.height(8.dp))

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Black),
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
        ) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            if (hasCameraPermission) {
              CameraPreview(
                modifier = Modifier.fillMaxSize(),
                lensFacing = internalLensFacing,
                scaleType = PreviewView.ScaleType.FILL_CENTER,
                onPreviewViewReady = { pView ->
                  previewViewInstance = pView
                }
              )
            }

            // HUD watermark
            Row(
              modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (internalLensFacing == CameraSelector.LENS_FACING_BACK) Icons.Default.CameraRear else Icons.Default.CameraFront,
                contentDescription = null,
                tint = SafetyEmerald,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (internalLensFacing == CameraSelector.LENS_FACING_BACK)
                  "BACK SENSOR ACTIVE"
                else
                  "FRONT SENSOR ACTIVE",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // Camera Flip Button
            IconButton(
              onClick = {
                val next = if (internalLensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                internalLensFacing = next
                onToggleCamera?.invoke()
              },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "Flip Camera",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            }

            // REC badge
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Red.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(Color.White)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "REC ${30 - sosState.recordingSecondsRemaining}s",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // Bottom-right GPS & File stamp
            Text(
              text = "GPS: ${String.format("%.4f", sosState.currentLatitude)}, ${String.format("%.4f", sosState.currentLongitude)}",
              color = Color.White.copy(alpha = 0.8f),
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 🚨 REAL-TIME GOOGLE MAPS LOCATION SHARING BEACON CARD
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, SafetyEmerald.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(SafetyEmerald)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "GOOGLE MAPS LIVE BEACON",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = SafetyEmerald,
                letterSpacing = 1.sp
              )
            }

            Text(
              text = "📡 Pings: ${liveTrackingState.pingCount}",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              color = SafetyCyan
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Coordinates & Accuracy row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(NavySurfaceVariant, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "📍 ${String.format("%.5f", sosState.currentLatitude)}, ${String.format("%.5f", sosState.currentLongitude)}",
                color = TextPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Accuracy: ±${String.format("%.1f", sosState.accuracyMeters)}m • Speed: ${String.format("%.1f", liveTrackingState.speedMps)} m/s",
                color = TextSecondary,
                fontSize = 10.sp
              )
            }

            // Open in Google Maps button
            IconButton(
              onClick = {
                onOpenGoogleMaps?.invoke(sosState.currentLatitude, sosState.currentLongitude)
              },
              modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF1E293B), CircleShape)
            ) {
              Icon(
                imageVector = Icons.Default.Map,
                contentDescription = "Open Google Maps",
                tint = SafetyCyan,
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Recipient Verified Contacts Live Status
          Text(
            text = "STREAMING TO VERIFIED CONTACTS:",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextTertiary,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(4.dp))

          if (contactStatuses.isEmpty()) {
            Text(
              text = "Syncing verified contact channels...",
              color = TextSecondary,
              fontSize = 11.sp
            )
          } else {
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(contactStatuses.values.toList()) { contactStatus ->
                Card(
                  shape = RoundedCornerShape(8.dp),
                  colors = CardDefaults.cardColors(
                    containerColor = if (contactStatus.isSelectedForLiveStream) SafetyEmeraldContainer else NavySurfaceVariant
                  ),
                  modifier = Modifier.clickable {
                    onToggleContactStream?.invoke(contactStatus.contactId)
                  }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (contactStatus.isSelectedForLiveStream) SafetyEmerald else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "${contactStatus.contactName} (${contactStatus.pingsDeliveredCount})",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (contactStatus.isSelectedForLiveStream) SafetyEmerald else TextSecondary
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Action Buttons: Open Maps & Send Broadcast SMS
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Button(
              onClick = {
                onOpenGoogleMaps?.invoke(sosState.currentLatitude, sosState.currentLongitude)
              },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.weight(1f).height(34.dp)
            ) {
              Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp), tint = SafetyCyan)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Google Maps", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = { onShareLiveLocation?.invoke() },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = CrimsonContainer),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
              modifier = Modifier.weight(1.3f).height(34.dp)
            ) {
              Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrimsonEmergencyGlow)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Send Live SMS", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Stage Completed: Delivered
      if (sosState.statusStage == "DELIVERED") {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SafetyEmerald.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Delivered",
              tint = SafetyEmerald,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Emergency Alert & Google Maps Broadcast Delivered",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )
            Text(
              text = "Live Google Maps link sent to selected verified contacts with 30s video recording.",
              color = TextSecondary,
              fontSize = 11.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons: Play & Share
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = { onPlaySavedVideo?.invoke() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play Video", fontSize = 12.sp)
              }

              OutlinedButton(
                onClick = { onShareSavedVideo?.invoke() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", fontSize = 12.sp)
              }
            }
          }
        }
      }

      if (sosState.statusStage == "CANCELLED") {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SafetyAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = "Safe",
              tint = SafetyAmber,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "SOS Alert Cancelled Safely",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Grace period cancellation verified. Google Maps location stream terminated.",
              color = TextSecondary,
              fontSize = 11.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Audit Log Stream
      Text(
        text = "LIVE LOCATION & TRANSMISSION LOG",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = TextTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(4.dp))

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .border(1.dp, NavyBorder, RoundedCornerShape(14.dp))
      ) {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
        ) {
          items(sosState.deliveryLog) { logEntry ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
              verticalAlignment = Alignment.Top
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = SafetyEmerald,
                modifier = Modifier
                  .size(14.dp)
                  .padding(top = 2.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = logEntry,
                color = TextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (sosState.statusStage == "DELIVERED" || sosState.statusStage == "CANCELLED") {
        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
        ) {
          Text(
            text = "RETURN TO HOME",
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}
