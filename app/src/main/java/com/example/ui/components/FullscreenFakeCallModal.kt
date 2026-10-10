package com.example.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.FakeCallActiveState

@Composable
fun FullscreenFakeCallModal(
  state: FakeCallActiveState,
  onEndCall: () -> Unit,
  onAnswerCall: () -> Unit = {},
  onSilenceRingtone: () -> Unit = {},
  onToggleMute: () -> Unit = {},
  onToggleSpeaker: () -> Unit = {},
  onTogglePause: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val minutes = state.elapsedSeconds / 60
  val seconds = state.elapsedSeconds % 60
  val timeFormatted = String.format("%02d:%02d", minutes, seconds)

  // Pulsing animation for incoming call ring
  val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        brush = Brush.verticalGradient(
          colors = if (state.isRinging) {
            listOf(Color(0xFF0F172A), Color(0xFF020617))
          } else if (state.isVideo) {
            listOf(Color(0xFF0F172A), Color(0xFF020617))
          } else {
            listOf(Color(0xFF1E1B4B), Color(0xFF090D16))
          }
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding()
      .testTag("fake_call_fullscreen")
  ) {
    if (state.isRinging) {
      // ==========================================
      // PHASE 1: REALISTIC INCOMING CALL SCREEN
      // ==========================================
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Caller Header
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(top = 36.dp)
        ) {
          Text(
            text = if (state.isVideo) "Incoming Video Call" else "Incoming Voice Call",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SafetyEmerald,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = state.callerName,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = state.callerNumber,
            fontSize = 16.sp,
            color = TextSecondary
          )
        }

        // Center Pulsing Avatar
        Box(contentAlignment = Alignment.Center) {
          // Animated glow ripple ring
          Box(
            modifier = Modifier
              .size(170.dp)
              .scale(pulseScale)
              .clip(CircleShape)
              .background(SafetyEmerald.copy(alpha = 0.18f))
          )
          // Solid avatar circle
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(120.dp)
              .clip(CircleShape)
              .background(NavySurfaceVariant)
              .border(3.dp, SafetyEmerald, CircleShape)
          ) {
            Icon(
              imageVector = if (state.isVideo) Icons.Default.Face else Icons.Default.Person,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(64.dp)
            )
          }
        }

        // Bottom Actions: Decline / Accept & Silence
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(bottom = 20.dp)
        ) {
          // Silence Ringtone button
          TextButton(
            onClick = onSilenceRingtone,
            modifier = Modifier.testTag("fake_call_silence_ringtone")
          ) {
            Icon(Icons.Default.VolumeOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Silence Ringtone", color = TextSecondary, fontSize = 12.sp)
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // DECLINE (Red button)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              IconButton(
                onClick = onEndCall,
                modifier = Modifier
                  .size(76.dp)
                  .clip(CircleShape)
                  .background(CrimsonEmergency)
                  .testTag("fake_call_decline")
              ) {
                Icon(
                  imageVector = Icons.Default.CallEnd,
                  contentDescription = "Decline Call",
                  tint = Color.White,
                  modifier = Modifier.size(36.dp)
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text("Decline", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            // ACCEPT (Green button with pulse)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              IconButton(
                onClick = onAnswerCall,
                modifier = Modifier
                  .size(76.dp)
                  .scale(pulseScale)
                  .clip(CircleShape)
                  .background(SafetyEmerald)
                  .testTag("fake_call_accept")
              ) {
                Icon(
                  imageVector = Icons.Default.Call,
                  contentDescription = "Answer Call",
                  tint = Color.Black,
                  modifier = Modifier.size(36.dp)
                )
              }
              Spacer(modifier = Modifier.height(8.dp))
              Text("Answer", color = SafetyEmerald, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else {
      // ==========================================
      // PHASE 2: CONNECTED IN-CALL SCREEN
      // ==========================================
      if (state.isVideo) {
        // Simulated video feed background
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(NavySurfaceVariant)
                .border(2.dp, SafetyEmerald, CircleShape)
            ) {
              Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp)
              )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "[LIVE VIDEO STREAM SIMULATED]",
              fontSize = 11.sp,
              color = SafetyEmerald,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = NavySurface.copy(alpha = 0.9f)),
              modifier = Modifier.fillMaxWidth(0.9f)
            ) {
              Text(
                text = "\"${state.scriptMessage}\"",
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.padding(14.dp),
                textAlign = TextAlign.Center
              )
            }
          }

          // Picture-in-picture local preview box
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(16.dp)
              .size(90.dp, 130.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF1E293B))
              .border(1.dp, NavyBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "User Camera",
              tint = TextSecondary,
              modifier = Modifier.size(32.dp)
            )
          }
        }
      }

      // Top Caller Info & Timer
      Column(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 36.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        if (!state.isVideo) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(90.dp)
              .clip(CircleShape)
              .background(NavySurfaceVariant)
              .border(2.dp, SafetyEmerald, CircleShape)
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(48.dp)
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
        }

        Text(
          text = state.callerName,
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (state.isPaused) "Call On Hold" else timeFormatted,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = if (state.isPaused) SafetyAmber else SafetyEmerald
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = if (state.isVideo) "Encrypted Video Call" else "Encrypted Voice Call",
          fontSize = 12.sp,
          color = TextSecondary
        )

        // Script text bubble for audio calls
        if (!state.isVideo) {
          Spacer(modifier = Modifier.height(20.dp))
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = NavySurfaceVariant),
            modifier = Modifier
              .fillMaxWidth(0.9f)
              .border(1.dp, NavyBorder, RoundedCornerShape(14.dp))
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "Caller Dialogue Script:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SafetyEmerald
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "\"${state.scriptMessage}\"",
                fontSize = 13.sp,
                color = TextPrimary
              )
            }
          }
        }
      }

      // Bottom In-Call Controls
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mute
          IconButton(
            onClick = onToggleMute,
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(if (state.isMuted) Color.White else NavySurfaceVariant)
              .testTag("fake_call_mute")
          ) {
            Icon(
              imageVector = if (state.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = "Mute",
              tint = if (state.isMuted) Color.Black else Color.White
            )
          }

          // Speaker
          IconButton(
            onClick = onToggleSpeaker,
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(if (state.isSpeakerOn) SafetyEmerald else NavySurfaceVariant)
              .testTag("fake_call_speaker")
          ) {
            Icon(
              imageVector = if (state.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
              contentDescription = "Speaker",
              tint = if (state.isSpeakerOn) Color.Black else Color.White
            )
          }

          // Pause / Hold
          IconButton(
            onClick = onTogglePause,
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(if (state.isPaused) SafetyAmber else NavySurfaceVariant)
              .testTag("fake_call_pause")
          ) {
            Icon(
              imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
              contentDescription = "Hold",
              tint = if (state.isPaused) Color.Black else Color.White
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // End Call
        IconButton(
          onClick = onEndCall,
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(CrimsonEmergency)
            .testTag("fake_call_end")
        ) {
          Icon(
            imageVector = Icons.Default.CallEnd,
            contentDescription = "End Call",
            tint = Color.White,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "End Call",
          fontSize = 12.sp,
          color = TextSecondary,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
