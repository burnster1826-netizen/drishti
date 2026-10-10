package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DrishtiDarkColorScheme = darkColorScheme(
  primary = CrimsonEmergency,
  onPrimary = Color.White,
  primaryContainer = CrimsonContainer,
  onPrimaryContainer = Color(0xFFFFD9DF),

  secondary = CrimsonEmergencyGlow,
  onSecondary = Color.White,
  secondaryContainer = NavySurfaceVariant,
  onSecondaryContainer = TextPrimary,

  tertiary = SafetyEmerald,
  onTertiary = Color.White,
  tertiaryContainer = SafetyEmeraldContainer,
  onTertiaryContainer = Color(0xFFA7F3D0),

  background = NavyBackground,
  onBackground = TextPrimary,

  surface = NavySurface,
  onSurface = TextPrimary,
  surfaceVariant = NavySurfaceVariant,
  onSurfaceVariant = TextSecondary,

  outline = NavyBorder,
  outlineVariant = Color(0xFF1E293B),

  error = CrimsonEmergency,
  onError = Color.White
)

@Composable
fun DrishtiTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = DrishtiDarkColorScheme,
    typography = Typography,
    content = content
  )
}
