package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.media.SavedVideoFile
import com.example.data.model.*
import com.example.data.upload.ServerUploadConfig
import com.example.data.upload.UploadStatus
import com.example.data.upload.UploadTaskState
import com.example.ui.theme.*

@Composable
fun SettingsTabScreen(
  userProfile: UserProfile,
  contacts: List<EmergencyContact>,
  fakeMediaList: List<FakeCallMediaItem>,
  sosEvents: List<SosEvent>,
  savedRecordings: List<SavedVideoFile> = emptyList(),
  serverConfig: ServerUploadConfig = ServerUploadConfig(),
  uploadTaskState: UploadTaskState = UploadTaskState(),
  onUpdateProfile: (UserProfile) -> Unit,
  onAddContact: (name: String, phone: String, email: String, method: ContactNotificationMethod) -> Boolean,
  onDeleteContact: (String) -> Unit,
  onVerifyContact: (String, Boolean) -> Unit,
  onAddFakeMedia: (title: String, callerName: String, isVideo: Boolean, script: String) -> Unit,
  onDeleteFakeMedia: (String) -> Unit,
  onSetDefaultFakeMedia: (String, Boolean) -> Unit,
  onPlayVideo: (SavedVideoFile) -> Unit = {},
  onShareVideo: (SavedVideoFile) -> Unit = {},
  onUploadFileToServer: (SavedVideoFile) -> Unit = {},
  onUpdateServerConfig: (ServerUploadConfig) -> Unit = {},
  onSwitchGoogleAccount: () -> Unit = {},
  onSignOut: () -> Unit,
  onDeleteAllData: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showAddContactDialog by remember { mutableStateOf(false) }
  var showAddMediaDialog by remember { mutableStateOf(false) }
  var showSosHistoryDialog by remember { mutableStateOf(false) }
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }
  var showUploadConfigDialog by remember { mutableStateOf(false) }
  var showArchitectureGuideDialog by remember { mutableStateOf(false) }
  var showInstallGuideDialog by remember { mutableStateOf(false) }

  // Custom emergency message editable dialog
  var isEditingMessage by remember { mutableStateOf(false) }
  var tempEmergencyMessage by remember { mutableStateOf(userProfile.emergencyMessage) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NavyBackground)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    Text(
      text = "Settings & Safety Center",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = TextPrimary
    )
    Text(
      text = "Manage Google account, phone recordings, server upload & emergency contacts",
      fontSize = 12.sp,
      color = TextSecondary,
      modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
    )

    // 1. GOOGLE PROFILE & ACCOUNT SECTION
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = NavySurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavyBorder, RoundedCornerShape(18.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(Color(0xFF1E293B))
              .border(2.dp, Color(0xFF4285F4), CircleShape)
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = "Google Account",
              tint = Color(0xFF4285F4),
              modifier = Modifier.size(34.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = userProfile.name,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = userProfile.email,
              fontSize = 12.sp,
              color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(SafetyEmerald)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Google Account Linked",
                fontSize = 11.sp,
                color = SafetyEmerald,
                fontWeight = FontWeight.Medium
              )
            }
          }

          IconButton(
            onClick = onSignOut,
            modifier = Modifier.testTag("sign_out_btn")
          ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = CrimsonEmergencyGlow)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onSwitchGoogleAccount,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Switch Account", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = { isEditingMessage = true },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Edit SOS Msg", fontSize = 12.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 2. SAVED PHONE RECORDINGS (MEDIA VAULT)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Saved Phone Recordings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Saved in Movies/Drishti_SOS • Gallery visible", fontSize = 11.sp, color = TextSecondary)
      }

      Badge(
        containerColor = SafetyEmeraldContainer,
        contentColor = SafetyEmerald
      ) {
        Text("${savedRecordings.size} On Phone", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (savedRecordings.isEmpty()) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Videocam, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text("No emergency recordings yet", color = TextSecondary, fontSize = 13.sp)
          Text("Recordings from active SOS sessions are saved automatically to your phone.", color = TextTertiary, fontSize = 11.sp)
        }
      }
    } else {
      savedRecordings.take(4).forEach { video ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, NavyBorder, RoundedCornerShape(14.dp))
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NavySurfaceVariant),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.VideoCameraBack, contentDescription = null, tint = CrimsonEmergencyGlow, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(video.fileName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text("${video.sizeFormatted} • ${video.formattedDate}", fontSize = 11.sp, color = TextSecondary)
                }
              }

              // Status badge
              if (video.isUploadedToServer) {
                Box(
                  modifier = Modifier
                    .background(SafetyEmeraldContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text("SYNCED TO SERVER", color = SafetyEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
              } else {
                Box(
                  modifier = Modifier
                    .background(SafetyAmberContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text("SAVED ON PHONE", color = SafetyAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons: Play, Share, Upload to Server
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Button(
                onClick = { onPlayVideo(video) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).height(34.dp)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", fontSize = 11.sp, color = TextPrimary)
              }

              Button(
                onClick = { onShareVideo(video) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.weight(1f).height(34.dp)
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", fontSize = 11.sp, color = TextPrimary)
              }

              Button(
                onClick = { onUploadFileToServer(video) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonContainer),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.weight(1.3f).height(34.dp)
              ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp), tint = CrimsonEmergencyGlow)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Upload Server", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. SERVER UPLOAD CONFIGURATION & HOW IT WORKS
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Server Upload Engine", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Backend sync, endpoints & cloud storage", fontSize = 11.sp, color = TextSecondary)
      }

      IconButton(onClick = { showArchitectureGuideDialog = true }) {
        Icon(Icons.Default.Info, contentDescription = "How it works", tint = SafetyCyan)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = NavySurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavyBorder, RoundedCornerShape(18.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        // Upload Status if active
        if (uploadTaskState.status == UploadStatus.UPLOADING) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SafetyCyan, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Uploading ${uploadTaskState.fileName}: ${uploadTaskState.progressPercent}%", color = SafetyCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(6.dp))
          LinearProgressIndicator(
            progress = { uploadTaskState.progressPercent / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = SafetyCyan,
            trackColor = NavySurfaceVariant
          )
          Spacer(modifier = Modifier.height(10.dp))
        } else if (uploadTaskState.status == UploadStatus.SUCCESS) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafetyEmerald, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Last Upload Complete: ${uploadTaskState.serverReceiptId}", color = SafetyEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Server Target Endpoint", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(serverConfig.serverUrl, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
          }

          IconButton(onClick = { showUploadConfigDialog = true }) {
            Icon(Icons.Default.Settings, contentDescription = "Configure Server", tint = SafetyCyan)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Auto-Upload on SOS Finish", fontSize = 13.sp, color = TextPrimary)
            Text("Automatically streams video to server when 30s recording finishes", fontSize = 11.sp, color = TextSecondary)
          }
          Switch(
            checked = serverConfig.autoUploadOnSos,
            onCheckedChange = { onUpdateServerConfig(serverConfig.copy(autoUploadOnSos = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SafetyEmerald)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Technical Guide Quick Action
        OutlinedButton(
          onClick = { showArchitectureGuideDialog = true },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = SafetyCyan),
          border = androidx.compose.foundation.BorderStroke(1.dp, SafetyCyan.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = SafetyCyan)
          Spacer(modifier = Modifier.width(6.dp))
          Text("How Does File Upload & Phone Storage Work?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 4. PHONE APK INSTALLATION & SIDELOADING GUIDE (Fixes "This option is off")
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = NavySurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, SafetyEmerald.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        .clickable { showInstallGuideDialog = true }
        .testTag("install_on_phone_guide_card")
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SafetyEmeraldContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.InstallMobile, contentDescription = null, tint = SafetyEmerald, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text("Installing APK on Your Phone?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              Text("Fix 'This option is off' & Play Protect in 3 steps", fontSize = 11.sp, color = SafetyEmerald)
            }
          }
          Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "When installing any APK file downloaded from browser, Android displays 'For your security, your phone is not allowed to install unknown apps from this source' and marks the switch as 'This option is off'. Tap here to see how to enable it and install in 30 seconds.",
          color = TextSecondary,
          fontSize = 11.sp,
          lineHeight = 15.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 5. EMERGENCY CONTACTS SECTION
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Emergency Contacts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Alerted during active SOS events", fontSize = 11.sp, color = TextSecondary)
      }

      Button(
        onClick = { showAddContactDialog = true },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency),
        modifier = Modifier.testTag("add_contact_btn")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (contacts.isEmpty()) {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      ) {
        Text("No emergency contacts registered yet.", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
      }
    } else {
      contacts.forEach { contact ->
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, if (contact.isVerified) SafetyEmerald.copy(alpha = 0.4f) else NavyBorder, RoundedCornerShape(14.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(contact.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                if (contact.isVerified) {
                  Box(
                    modifier = Modifier
                      .background(SafetyEmeraldContainer, RoundedCornerShape(4.dp))
                      .padding(horizontal = 4.dp, vertical = 1.dp)
                  ) {
                    Text("VERIFIED", color = SafetyEmerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
              Text("${contact.phone} • ${contact.email}", fontSize = 11.sp, color = TextSecondary)
            }

            IconButton(onClick = { onDeleteContact(contact.id) }) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Contact", tint = TextTertiary, modifier = Modifier.size(18.dp))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Danger Zone
    Button(
      onClick = { showDeleteConfirmDialog = true },
      shape = RoundedCornerShape(14.dp),
      colors = ButtonDefaults.buttonColors(containerColor = NavySurfaceVariant),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .border(1.dp, CrimsonEmergency.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    ) {
      Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = CrimsonEmergencyGlow, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Erase All Local Data & Logs", color = CrimsonEmergencyGlow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(24.dp))
  }

  // DIALOG: Server Upload Configuration
  if (showUploadConfigDialog) {
    var tempUrl by remember { mutableStateOf(serverConfig.serverUrl) }
    var tempKey by remember { mutableStateOf(serverConfig.apiKey) }
    var tempBucket by remember { mutableStateOf(serverConfig.targetBucketName) }

    AlertDialog(
      onDismissRequest = { showUploadConfigDialog = false },
      containerColor = NavySurface,
      title = { Text("Server Upload Configuration", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text("Configure where emergency video recordings and GPS telemetry are sent:", color = TextSecondary, fontSize = 12.sp)
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = tempUrl,
            onValueChange = { tempUrl = it },
            label = { Text("Server Upload URL (POST endpoint)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = tempKey,
            onValueChange = { tempKey = it },
            label = { Text("Authorization Header / API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = tempBucket,
            onValueChange = { tempBucket = it },
            label = { Text("Cloud Storage Bucket Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateServerConfig(
              serverConfig.copy(
                serverUrl = tempUrl.ifBlank { "https://httpbin.org/post" },
                apiKey = tempKey,
                targetBucketName = tempBucket.ifBlank { "drishti-sos-vault" }
              )
            )
            showUploadConfigDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency)
        ) {
          Text("Save Config", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showUploadConfigDialog = false }) { Text("Cancel", color = TextSecondary) }
      }
    )
  }

  // DIALOG: Educational Architecture Guide (Answers "How can we upload file to server & save on phone")
  if (showArchitectureGuideDialog) {
    AlertDialog(
      onDismissRequest = { showArchitectureGuideDialog = false },
      containerColor = NavySurface,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CloudSync, contentDescription = null, tint = SafetyCyan)
          Spacer(modifier = Modifier.width(8.dp))
          Text("How Server Upload & Storage Works", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(
            text = "1. SAVING IN THE USER'S PHONE:",
            color = SafetyEmerald,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "• MediaStore API: The video is saved directly to MediaStore.Video.Media.EXTERNAL_CONTENT_URI under Movies/Drishti_SOS. This makes the file visible in Google Photos and device Gallery immediately.\n" +
                   "• Scoped Storage Backup: In addition to MediaStore, a redundant copy is saved in app external storage (sos_recordings/) so evidence is never lost even if gallery caches are cleared.\n" +
                   "• MediaScannerConnection: On recording finish, Android's MediaScanner indexes the file so third-party apps can see it right away.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(vertical = 4.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "2. UPLOADING TO A SERVER:",
            color = SafetyCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "• Direct Multipart HTTP POST (Active in Drishti): Uses OkHttp MultipartBody to stream the video bytes in chunks from the phone storage to your server endpoint with real-time progress callbacks (0% to 100%).\n" +
                   "• Pre-signed S3 / GCS URLs (Cloud): For production cloud scale, the Android app requests a temporary signed URL from your backend and PUTs the video directly into AWS S3 or Google Cloud Storage.\n" +
                   "• Background Resilience (WorkManager): If the network is poor, uploads are queued in a CoroutineWorker that retries automatically once Wi-Fi or cellular connectivity is restored.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(vertical = 4.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "3. SENSORS & PRIVACY:",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "• Drishti uses the device Back Camera sensor by default for situational fidelity.\n" +
                   "• GPS coordinates and sensor data are attached in the HTTP multipart headers for emergency verification.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(vertical = 4.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showArchitectureGuideDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency)
        ) {
          Text("Got It", color = Color.White)
        }
      }
    )
  }

  // DIALOG: Add Contact
  if (showAddContactDialog) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(ContactNotificationMethod.ALL_CHANNELS) }

    AlertDialog(
      onDismissRequest = { showAddContactDialog = false },
      containerColor = NavySurface,
      title = { Text("Add Emergency Contact", color = TextPrimary) },
      text = {
        Column {
          OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (name.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())) {
              onAddContact(name, phone, email, method)
              showAddContactDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency)
        ) {
          Text("Save Contact", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddContactDialog = false }) { Text("Cancel", color = TextSecondary) }
      }
    )
  }

  // DIALOG: Edit Custom Emergency Message
  if (isEditingMessage) {
    AlertDialog(
      onDismissRequest = { isEditingMessage = false },
      containerColor = NavySurface,
      title = { Text("Edit Emergency Message", color = TextPrimary) },
      text = {
        OutlinedTextField(
          value = tempEmergencyMessage,
          onValueChange = { tempEmergencyMessage = it },
          label = { Text("Custom Message") },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateProfile(userProfile.copy(emergencyMessage = tempEmergencyMessage))
            isEditingMessage = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency)
        ) {
          Text("Save", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { isEditingMessage = false }) { Text("Cancel", color = TextSecondary) }
      }
    )
  }

  // DIALOG: Confirm Delete All Account Data
  if (showDeleteConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      containerColor = NavySurface,
      title = { Text("Erase All Data?", color = CrimsonEmergencyGlow, fontWeight = FontWeight.Bold) },
      text = {
        Text("This will permanently purge all SOS events, uploaded fake call media, emergency contacts, and profile data from local device storage.", color = TextSecondary, fontSize = 13.sp)
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteAllData()
            showDeleteConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency)
        ) {
          Text("Yes, Permanently Erase", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel", color = TextSecondary) }
      }
    )
  }

  // DIALOG: Phone APK Installation Guide (Fix "This option is off")
  if (showInstallGuideDialog) {
    AlertDialog(
      onDismissRequest = { showInstallGuideDialog = false },
      containerColor = NavySurface,
      shape = RoundedCornerShape(20.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.HelpCenter, contentDescription = null, tint = SafetyEmerald)
          Spacer(modifier = Modifier.width(8.dp))
          Text("How to Install on Phone", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(
            text = "WHY DID YOUR PHONE SAY 'THIS OPTION IS OFF'?",
            color = SafetyEmerald,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Android protects your phone by turning OFF unknown app installation for all browsers and file managers by default. It is not an error—you just need to toggle the switch ON once.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(vertical = 4.dp)
          )

          Spacer(modifier = Modifier.height(12.dp))

          // STEP 1
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("STEP 1: Turn 'Allow from this source' ON", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "1. Tap the downloaded APK file in Chrome or your Files app.\n" +
                       "2. A dialog pops up: 'For your security, phone not allowed to install...'.\n" +
                       "3. Tap 'Settings'.\n" +
                       "4. You will see 'Allow from this source' with the toggle showing 'This option is off'.\n" +
                       "5. Tap the switch toggle to turn it ON (Allow)!\n" +
                       "6. Tap Back and tap 'Install'.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // STEP 2
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("STEP 2: Google Play Protect Warning", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Because this is a newly built development APK not yet published on Google Play Store, Play Protect might say 'Unsafe app blocked' or 'Blocked by Play Protect'.\n" +
                       "• Tap 'More details'\n" +
                       "• Tap 'Install anyway'.",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // STEP 3
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text("STEP 3: If 'Restricted setting' appears (Android 13/14/15)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "If Android shows a greyed-out button saying 'Restricted setting':\n" +
                       "1. Go to Phone Settings -> Apps.\n" +
                       "2. Select Chrome (or Files / Drishti).\n" +
                       "3. Tap the 3 dots in the top-right corner.\n" +
                       "4. Tap 'Allow restricted settings' and enter your phone PIN.\n" +
                       "5. Now open the APK and it installs smoothly!",
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { showInstallGuideDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = SafetyEmerald)
        ) {
          Text("Understood, Ready to Install", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      }
    )
  }
}
