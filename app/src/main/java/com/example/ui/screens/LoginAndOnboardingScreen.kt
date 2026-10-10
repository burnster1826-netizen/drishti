package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.*

data class PermissionRequirement(
  val title: String,
  val icon: ImageVector,
  val description: String,
  var isGranted: Boolean = false
)

@Composable
fun LoginAndOnboardingScreen(
  isLoggedIn: Boolean,
  isOnboardingCompleted: Boolean,
  onGoogleSignIn: () -> Unit,
  onQuickDemoSignIn: (email: String, name: String) -> Unit = { _, _ -> },
  onCompleteOnboarding: () -> Unit,
  statusMessage: String? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var isAuthenticating by remember { mutableStateOf(false) }
  var showAccountChooserDialog by remember { mutableStateOf(false) }

  // State of permission toggles
  var locationGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    )
  }
  var cameraGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )
  }
  var micGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  var notificationsGranted by remember {
    mutableStateOf(
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      } else true
    )
  }

  // System permission request launchers
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted -> cameraGranted = granted }

  val micLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted -> micGranted = granted }

  val locationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted -> locationGranted = granted }

  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted -> notificationsGranted = granted }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = listOf(
            NavyBackground,
            Color(0xFF0F172A),
            NavyBackground
          )
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(24.dp)
  ) {
    if (!isLoggedIn) {
      // 1. LOGIN SCREEN WITH GOOGLE SIGN-IN
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Drishti Logo
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(NavySurfaceVariant)
            .border(2.dp, CrimsonEmergency, CircleShape)
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_drishti_logo),
            contentDescription = "Drishti App Emblem",
            modifier = Modifier.size(90.dp)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = "DRISHTI",
          fontSize = 32.sp,
          fontWeight = FontWeight.Black,
          color = TextPrimary,
          letterSpacing = 3.sp
        )

        Text(
          text = "Personal Safety & Emergency Guardian",
          fontSize = 14.sp,
          color = TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Status message if any
        if (statusMessage != null) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurface),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, SafetyCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          ) {
            Text(
              text = statusMessage,
              color = SafetyCyan,
              fontSize = 12.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(12.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
        }

        // Primary Google Sign-In Button (Modern Credential Manager)
        Button(
          onClick = {
            isAuthenticating = true
            onGoogleSignIn()
          },
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("login_with_google_btn")
        ) {
          if (isAuthenticating) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = NavyBackground)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Signing in with Google...", color = NavyBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          } else {
            // Google G Icon representation
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = "Google Sign-In",
              tint = Color(0xFF4285F4),
              modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Continue with Google",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1F2937)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Alternative: Choose test account for quick sign-in
        OutlinedButton(
          onClick = { showAccountChooserDialog = true },
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
          border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("select_test_google_account_btn")
        ) {
          Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextSecondary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Select / Demo Google Account", fontSize = 13.sp, color = TextSecondary)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
          text = "Google Sign-In securely syncs your verified emergency contacts, back-camera video evidence recordings, and real-time cloud dispatch.",
          fontSize = 12.sp,
          color = TextTertiary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
      }

      // Dialog to choose Google account
      if (showAccountChooserDialog) {
        AlertDialog(
          onDismissRequest = { showAccountChooserDialog = false },
          title = {
            Text("Google Account Sign-In", color = TextPrimary, fontWeight = FontWeight.Bold)
          },
          text = {
            Column {
              Text(
                "Choose a Google account to sign into Drishti Guardian:",
                color = TextSecondary,
                fontSize = 13.sp
              )
              Spacer(modifier = Modifier.height(16.dp))

              val accounts = listOf(
                Pair("Alex Vance", "alex.vance@gmail.com"),
                Pair("Priya Sharma", "priya.sharma@family.org"),
                Pair("David Miller", "d.miller@campus.edu")
              )

              accounts.forEach { (name, email) ->
                Card(
                  shape = RoundedCornerShape(12.dp),
                  colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                      onQuickDemoSignIn(email, name)
                      showAccountChooserDialog = false
                    }
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SafetyCyanContainer),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = name.take(1),
                        fontWeight = FontWeight.Bold,
                        color = SafetyCyan
                      )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                      Text(email, color = TextSecondary, fontSize = 12.sp)
                    }
                  }
                }
              }
            }
          },
          confirmButton = {
            TextButton(onClick = { showAccountChooserDialog = false }) {
              Text("Cancel", color = TextSecondary)
            }
          },
          containerColor = NavySurface
        )
      }
    } else {
      // 2. ONBOARDING & PERMISSIONS EXPLANATION
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Essential Safety Permissions",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Text(
          text = "Drishti activates back-camera sensors and location strictly during explicit SOS triggers. Files are saved to your phone and synced to the server.",
          fontSize = 13.sp,
          color = TextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
        )

        // Permission Card 1: Location
        PermissionToggleCard(
          title = "High-Accuracy GPS Location",
          description = "Attaches live coordinates and clickable navigation maps to emergency alerts when SOS is initiated.",
          icon = Icons.Default.LocationOn,
          isGranted = locationGranted,
          onToggle = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Permission Card 2: Back Camera
        PermissionToggleCard(
          title = "Emergency Back Camera Access",
          description = "Records a 30-second high-definition situational evidence clip using your phone's back camera.",
          icon = Icons.Default.CameraAlt,
          isGranted = cameraGranted,
          onToggle = { cameraLauncher.launch(Manifest.permission.CAMERA) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Permission Card 3: Microphone
        PermissionToggleCard(
          title = "Microphone Audio Evidence",
          description = "Captures ambient distress audio and powers realistic interactive fake phone calls.",
          icon = Icons.Default.Mic,
          isGranted = micGranted,
          onToggle = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Permission Card 4: Notifications
        PermissionToggleCard(
          title = "Critical Safety Alerts",
          description = "Displays instant safety checks, cancellation confirmations, and local hazard alerts.",
          icon = Icons.Default.Notifications,
          isGranted = notificationsGranted,
          onToggle = {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
              notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              notificationsGranted = true
            }
          }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Permission Card 5: Gallery & Media Storage
        var storageGranted by remember {
          mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT <= 28) {
              ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
              ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else true
          )
        }

        val storageLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission()
        ) { granted -> storageGranted = granted }

        PermissionToggleCard(
          title = "Phone Gallery Autosave",
          description = "Saves emergency video recordings to your phone's Gallery (Movies/Drishti_SOS) for offline access.",
          icon = Icons.Default.VideoLibrary,
          isGranted = storageGranted,
          onToggle = {
            if (android.os.Build.VERSION.SDK_INT <= 28) {
              storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
              storageLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
              storageGranted = true
            }
          }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Device settings fallback button
        TextButton(
          onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
          }
        ) {
          Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Open System App Settings", color = TextSecondary, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Complete Onboarding Button
        Button(
          onClick = onCompleteOnboarding,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("complete_onboarding_btn")
        ) {
          Text(
            text = "Enter Drishti Guardian",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
private fun PermissionToggleCard(
  title: String,
  description: String,
  icon: ImageVector,
  isGranted: Boolean,
  onToggle: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = NavySurface),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, if (isGranted) SafetyEmerald.copy(alpha = 0.5f) else NavyBorder, RoundedCornerShape(16.dp))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(if (isGranted) SafetyEmeraldContainer else NavySurfaceVariant)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isGranted) SafetyEmerald else TextSecondary,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = description,
          fontSize = 11.sp,
          color = TextSecondary,
          lineHeight = 14.sp
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Switch(
        checked = isGranted,
        onCheckedChange = { onToggle() },
        colors = SwitchDefaults.colors(
          checkedThumbColor = Color.White,
          checkedTrackColor = SafetyEmerald,
          uncheckedThumbColor = TextSecondary,
          uncheckedTrackColor = NavySurfaceVariant
        )
      )
    }
  }
}
