package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserProfile
import com.example.ui.components.SosEmergencyButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.FakeCallActiveState

@Composable
fun HomeTabScreen(
  userProfile: UserProfile,
  onProfileClick: () -> Unit,
  onSosTrigger: () -> Unit,
  onStartInstantRecording: () -> Unit,
  onFindSaferRoute: () -> Unit,
  onStartFakeCall: (isVideo: Boolean) -> Unit = {},
  onScheduleFakeCall: (delaySec: Int, isVideo: Boolean, callerName: String, callerNumber: String, script: String) -> Unit = { _, _, _, _, _ -> },
  fakeCallState: FakeCallActiveState = FakeCallActiveState(),
  onCancelScheduledFakeCall: () -> Unit = {},
  verifiedContactsCount: Int,
  onOpenInstallationGuide: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var showFakeCallMenu by remember { mutableStateOf(false) }

  // Fake call dialog configuration state
  var selectedCallerIndex by remember { mutableStateOf(0) }
  var isVideoSelected by remember { mutableStateOf(false) }
  var selectedDelaySeconds by remember { mutableStateOf(0) } // 0 = now

  val callerPresets = listOf(
    Triple("Mom (Home)", "+1 (555) 234-8901", "Hey! Where are you? Dinner is ready and I'm parked outside waiting. Come down right now!"),
    Triple("Dad", "+1 (555) 892-1204", "Hey, are you on your way home? Call me back as soon as you step outside."),
    Triple("Office Boss", "+1 (555) 441-7788", "Hi, urgent update on the project report. Can you speak right away?"),
    Triple("Dr. Sharma (Clinic)", "+1 (555) 912-3456", "Hello, confirming your appointment is starting in 10 minutes. Please check in."),
    Triple("Emergency Ride", "+1 (555) 670-4321", "Your driver has arrived at the pickup location. Vehicle hazard lights on.")
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NavyBackground)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    // 1. TOP HEADER (App Name "Drishti" at top-left, Profile icon at top-right)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          painter = painterResource(id = R.drawable.img_drishti_logo),
          contentDescription = "Drishti Logo",
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Drishti",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            letterSpacing = 1.sp
          )
          Text(
            text = "GUARDIAN ACTIVE",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = SafetyEmerald,
            letterSpacing = 1.sp
          )
        }
      }

      // Profile avatar button at top-right
      IconButton(
        onClick = onProfileClick,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(NavySurfaceVariant)
          .border(1.dp, NavyBorder, CircleShape)
          .testTag("top_profile_icon_btn")
      ) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = "User Profile & Settings",
          tint = TextPrimary,
          modifier = Modifier.size(24.dp)
        )
      }
    }

    // Scheduled Fake Call Active Banner
    if (fakeCallState.isScheduled) {
      Spacer(modifier = Modifier.height(10.dp))
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, SafetyAmber, RoundedCornerShape(14.dp))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = SafetyAmber, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Fake Call in ${fakeCallState.scheduledSecondsRemaining}s",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary
              )
              Text(
                text = "Caller: ${fakeCallState.callerName}",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          TextButton(
            onClick = onCancelScheduledFakeCall,
            modifier = Modifier.testTag("cancel_scheduled_fake_call")
          ) {
            Text("Cancel", color = CrimsonEmergencyGlow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Verified Guardian Status Card
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = NavySurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(SafetyEmeraldContainer)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = SafetyEmerald,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Protection Network Ready",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "$verifiedContactsCount verified emergency contacts connected",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Badge(
          containerColor = SafetyEmeraldContainer,
          contentColor = SafetyEmerald
        ) {
          Text(
            text = "ACTIVE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // 2. PRIMARY SOS EMERGENCY BUTTON (CENTERSTAGE)
    Box(
      modifier = Modifier.fillMaxWidth(),
      contentAlignment = Alignment.Center
    ) {
      SosEmergencyButton(
        onClick = onSosTrigger,
        modifier = Modifier.padding(vertical = 12.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 3. QUICK ACTION TILES: "Find Safer Route" & "Fake Call Simulator"
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Find Safer Route Action Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier
          .weight(1f)
          .height(115.dp)
          .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
          .clickable { onFindSaferRoute() }
          .testTag("home_quick_safer_route")
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1E293B))
          ) {
            Icon(
              imageVector = Icons.Default.AltRoute,
              contentDescription = null,
              tint = SafetyBlue,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Safer Route",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "AI incident radar & paths",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }
      }

      // Fake Call Simulator Action Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        modifier = Modifier
          .weight(1f)
          .height(115.dp)
          .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
          .clickable { showFakeCallMenu = true }
          .testTag("home_quick_fake_call")
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF1E293B))
          ) {
            Icon(
              imageVector = Icons.Default.Call,
              contentDescription = null,
              tint = SafetyEmerald,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Text(
              text = "Fake Call",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Schedule or trigger escape call",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Dedicated Quick Video Recording Action Card (Back Camera default)
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = NavySurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, CrimsonEmergency.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        .clickable { onStartInstantRecording() }
        .testTag("quick_record_video_banner")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(CrimsonContainer)
          ) {
            Icon(
              imageVector = Icons.Default.Videocam,
              contentDescription = null,
              tint = CrimsonEmergencyGlow,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Evidence Video Recording",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Back Camera • Saves to Phone & Cloud",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Box(
          modifier = Modifier
            .background(CrimsonEmergency, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text("START", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Phone Installation Sideload Help Card
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavyBorder, RoundedCornerShape(14.dp))
        .clickable { onOpenInstallationGuide() }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.InstallMobile, contentDescription = null, tint = SafetyEmerald, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Installing APK on phone?",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextPrimary
          )
          Text(
            text = "If phone shows 'This option is off' or Play Protect block, tap here for quick 30s fix.",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }

  // Realistic Fake Call Config & Trigger Dialog
  if (showFakeCallMenu) {
    AlertDialog(
      onDismissRequest = { showFakeCallMenu = false },
      containerColor = NavySurface,
      shape = RoundedCornerShape(20.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = SafetyEmerald)
          Spacer(modifier = Modifier.width(10.dp))
          Text("Fake Call Simulator", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
      },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(
            text = "Simulates an authentic incoming call with actual system ringtone and vibration to discreetly exit awkward or risky situations.",
            color = TextSecondary,
            fontSize = 12.sp
          )

          Spacer(modifier = Modifier.height(14.dp))

          // 1. SELECT CALLER IDENTITY
          Text(
            text = "1. CHOOSE CALLER ID",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SafetyEmerald,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))

          callerPresets.forEachIndexed { index, (name, number, _) ->
            Card(
              shape = RoundedCornerShape(10.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (selectedCallerIndex == index) SafetyEmeraldContainer.copy(alpha = 0.3f) else NavySurfaceVariant
              ),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .border(
                  width = 1.dp,
                  color = if (selectedCallerIndex == index) SafetyEmerald else Color.Transparent,
                  shape = RoundedCornerShape(10.dp)
                )
                .clickable { selectedCallerIndex = index }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = if (selectedCallerIndex == index) SafetyEmerald else TextSecondary,
                    modifier = Modifier.size(28.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                    Text(number, fontSize = 11.sp, color = TextSecondary)
                  }
                }
                if (selectedCallerIndex == index) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafetyEmerald, modifier = Modifier.size(18.dp))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 2. CALL TYPE (Audio vs Video)
          Text(
            text = "2. CALL TYPE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SafetyEmerald,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = !isVideoSelected,
              onClick = { isVideoSelected = false },
              label = { Text("Voice Call") },
              leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp)) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SafetyEmeraldContainer,
                selectedLabelColor = SafetyEmerald
              ),
              modifier = Modifier.weight(1f)
            )

            FilterChip(
              selected = isVideoSelected,
              onClick = { isVideoSelected = true },
              label = { Text("Video Call") },
              leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp)) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CrimsonContainer,
                selectedLabelColor = CrimsonEmergencyGlow
              ),
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 3. SCHEDULE DELAY
          Text(
            text = "3. WHEN SHOULD PHONE RING?",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SafetyEmerald,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(6.dp))

          val delayOptions = listOf(0 to "Now", 10 to "10s", 30 to "30s", 60 to "1 min", 120 to "2 min")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            delayOptions.forEach { (sec, label) ->
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (selectedDelaySeconds == sec) SafetyEmerald else NavySurfaceVariant)
                  .clickable { selectedDelaySeconds = sec }
                  .padding(vertical = 8.dp)
              ) {
                Text(
                  text = label,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (selectedDelaySeconds == sec) Color.Black else TextPrimary
                )
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showFakeCallMenu = false
            val chosen = callerPresets[selectedCallerIndex]
            if (selectedDelaySeconds == 0) {
              onStartFakeCall(isVideoSelected)
            } else {
              onScheduleFakeCall(
                selectedDelaySeconds,
                isVideoSelected,
                chosen.first,
                chosen.second,
                chosen.third
              )
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SafetyEmerald),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("trigger_fake_call_confirm_btn")
        ) {
          Text(
            text = if (selectedDelaySeconds == 0) "Ring Phone Now" else "Schedule in ${selectedDelaySeconds}s",
            color = Color.Black,
            fontWeight = FontWeight.Bold
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { showFakeCallMenu = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}
