package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouteAnalysis
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSafetyDialog(
  destination: String,
  onDestinationChange: (String) -> Unit,
  onSearch: (String) -> Unit,
  onDismiss: () -> Unit,
  isAnalyzing: Boolean,
  routeAnalyses: List<RouteAnalysis>,
  modifier: Modifier = Modifier
) {
  var selectedRouteId by remember { mutableStateOf<String?>(null) }
  var navigationStartedToast by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = NavySurface,
    contentColor = TextPrimary,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .navigationBarsPadding()
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Safer Route Intelligence",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Powered by Tiny Fish AI & verified emergency blotters",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Destination input field
      OutlinedTextField(
        value = destination,
        onValueChange = onDestinationChange,
        label = { Text("Where are you heading?", color = TextSecondary) },
        placeholder = { Text("e.g. City Center, Library, Train Station", color = TextTertiary) },
        leadingIcon = {
          Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = CrimsonEmergency)
        },
        trailingIcon = {
          if (destination.isNotBlank()) {
            IconButton(onClick = { onSearch(destination) }, modifier = Modifier.testTag("search_route_btn")) {
              Icon(Icons.Default.Search, contentDescription = "Analyze Routes", tint = SafetyEmerald)
            }
          }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CrimsonEmergency,
          unfocusedBorderColor = NavyBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          cursorColor = CrimsonEmergency
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("destination_input")
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Trigger calculate button
      Button(
        onClick = { onSearch(destination.ifBlank { "City Center Station" }) },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonEmergency),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("analyze_routes_button")
      ) {
        if (isAnalyzing) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Analyzing CCTV & Police Coverage...", color = Color.White, fontWeight = FontWeight.SemiBold)
        } else {
          Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Find Safest Walking Routes", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }

      // Legal disclaimer
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Disclaimer: Provided as safety guidance, not a guaranteed protection shield. In immediate danger, tap SOS or call local emergency dispatch.",
        fontSize = 11.sp,
        color = TextTertiary,
        lineHeight = 14.sp
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Routes results
      if (routeAnalyses.isNotEmpty()) {
        Text(
          text = "ANALYZED ROUTES",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(routeAnalyses) { route ->
            val isSelected = selectedRouteId == route.id
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) NavySurfaceVariant else NavyBackground
              ),
              modifier = Modifier
                .fillMaxWidth()
                .border(
                  width = if (isSelected) 2.dp else 1.dp,
                  color = if (route.isRecommended) SafetyEmerald else NavyBorder,
                  shape = RoundedCornerShape(16.dp)
                )
                .clickable { selectedRouteId = route.id }
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (route.isRecommended) {
                      Box(
                        modifier = Modifier
                          .background(SafetyEmerald, RoundedCornerShape(6.dp))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Text("SAFEST", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                      }
                      Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                      text = route.title,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary
                    )
                  }

                  Text(
                    text = "${route.safetyScore}/100",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (route.safetyScore >= 80) SafetyEmerald else if (route.safetyScore >= 60) SafetyAmber else CrimsonEmergency
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = route.keySafetyHighlight,
                  fontSize = 12.sp,
                  color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "Dist: ${route.distanceKm} km • Walk: ${route.durationMinutes} min",
                    fontSize = 11.sp,
                    color = TextTertiary
                  )
                  Text(
                    text = "Police stations: ${route.policeStationsCount} • Incidents: ${route.incidentCountNearRoute}",
                    fontSize = 11.sp,
                    color = TextTertiary
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = { navigationStartedToast = true },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = SafetyEmerald),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("start_turn_navigation_btn")
        ) {
          Icon(Icons.Default.Directions, contentDescription = null, tint = Color.Black)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Launch Turn-by-Turn Navigation", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (navigationStartedToast) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Navigating via selected safest corridor. Drishti background guardian active.",
            color = SafetyEmerald,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}
