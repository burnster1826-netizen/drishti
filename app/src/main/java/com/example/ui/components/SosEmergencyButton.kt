package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonEmergency
import com.example.ui.theme.CrimsonEmergencyGlow

@Composable
fun SosEmergencyButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  // Infinite pulsing ring animation for immediate emergency clarity
  val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.14f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  val outerAlpha by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.10f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "outer_alpha"
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(260.dp)
  ) {
    // Outer pulsating halo 1
    Box(
      modifier = Modifier
        .size(240.dp)
        .scale(pulseScale)
        .clip(CircleShape)
        .background(CrimsonEmergency.copy(alpha = outerAlpha))
    )

    // Secondary soft glow
    Box(
      modifier = Modifier
        .size(200.dp)
        .clip(CircleShape)
        .background(CrimsonEmergency.copy(alpha = 0.22f))
    )

    // Main Interactive Touch Target
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(175.dp)
        .shadow(elevation = 18.dp, shape = CircleShape, spotColor = CrimsonEmergencyGlow)
        .clip(CircleShape)
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              CrimsonEmergencyGlow,
              CrimsonEmergency,
              Color(0xFF880E2B)
            )
          )
        )
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(bounded = true, color = Color.White),
          role = Role.Button,
          onClick = onClick,
          enabled = enabled
        )
        .testTag("sos_emergency_button")
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = "Emergency Alert Icon",
          tint = Color.White,
          modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "SOS",
          fontSize = 38.sp,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = 2.sp
        )
        Text(
          text = "TAP FOR HELP",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White.copy(alpha = 0.9f),
          letterSpacing = 1.sp
        )
      }
    }
  }
}
