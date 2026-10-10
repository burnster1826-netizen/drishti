package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncidentReport
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentTabScreen(
  incidents: List<IncidentReport>,
  selectedCategory: String,
  maxDistanceKm: Float,
  onCategorySelected: (String) -> Unit,
  onMaxDistanceChanged: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  var isMapViewSelected by remember { mutableStateOf(false) }

  val categories = listOf("All", "Poor Lighting & Hazards", "Theft & Robbery", "Harassment", "Crowd / Protest")

  val filteredIncidents = incidents.filter {
    (selectedCategory == "All" || it.category == selectedCategory) &&
      it.distanceKm <= maxDistanceKm
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NavyBackground)
      .padding(horizontal = 20.dp, vertical = 12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Recent Incident Radar",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = "Tiny Fish AI Police Blotter • Real-time ingest",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }

      // Map vs List Toggle
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(NavySurfaceVariant)
          .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
      ) {
        IconButton(
          onClick = { isMapViewSelected = false },
          modifier = Modifier
            .size(38.dp)
            .background(if (!isMapViewSelected) CrimsonEmergency else Color.Transparent)
            .testTag("list_view_toggle")
        ) {
          Icon(Icons.Default.List, contentDescription = "List View", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        IconButton(
          onClick = { isMapViewSelected = true },
          modifier = Modifier
            .size(38.dp)
            .background(if (isMapViewSelected) CrimsonEmergency else Color.Transparent)
            .testTag("map_view_toggle")
        ) {
          Icon(Icons.Default.Map, contentDescription = "Map View", tint = Color.White, modifier = Modifier.size(20.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Category Filter Chips Row
    ScrollableTabRow(
      selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
      edgePadding = 0.dp,
      containerColor = Color.Transparent,
      divider = {},
      indicator = {}
    ) {
      categories.forEach { cat ->
        val isSelected = cat == selectedCategory
        FilterChip(
          selected = isSelected,
          onClick = { onCategorySelected(cat) },
          label = {
            Text(
              text = cat,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) Color.White else TextSecondary
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CrimsonEmergency,
            containerColor = NavySurfaceVariant
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isSelected) CrimsonEmergency else NavyBorder
          ),
          modifier = Modifier.padding(end = 6.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Distance Radius Slider
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Search Radius: ${String.format("%.1f", maxDistanceKm)} km",
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary
      )
      Text(
        text = "${filteredIncidents.size} incidents found",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = SafetyEmerald
      )
    }

    Slider(
      value = maxDistanceKm,
      onValueChange = onMaxDistanceChanged,
      valueRange = 0.5f..10.0f,
      colors = SliderDefaults.colors(
        thumbColor = CrimsonEmergency,
        activeTrackColor = CrimsonEmergency,
        inactiveTrackColor = NavySurfaceVariant
      )
    )

    // Legal & Reliability Banner
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = NavySurface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
    ) {
      Row(
        modifier = Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Public incident data may be delayed. Ingested via Tiny Fish AI agent. Last cache updated 4 min ago.",
          fontSize = 11.sp,
          color = TextTertiary,
          lineHeight = 14.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (isMapViewSelected) {
      // High-Fidelity Tactical Map View
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
          .testTag("incidents_map_container")
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          // Tactical grid drawing simulation
          Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("RADAR COVERAGE: 37.7749° N, 122.4194° W", color = SafetyEmerald, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
              Text("ZOOM 15X", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            // Clustered incident pins simulation
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              filteredIncidents.forEach { item ->
                Row(
                  modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(if (item.category.contains("Theft") || item.category.contains("Harass")) CrimsonEmergency else SafetyAmber)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("${item.title} (${item.distanceKm} km)", color = Color.White, fontSize = 11.sp)
                }
              }
            }

            Text("Current user location centered with active perimeter guard", color = TextTertiary, fontSize = 10.sp)
          }
        }
      }
    } else {
      // List View
      if (filteredIncidents.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SafetyEmerald, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("No Incidents in This Perimeter", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("No verified public safety reports matching your criteria", fontSize = 12.sp, color = TextSecondary)
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(filteredIncidents) { item ->
            IncidentItemCard(incident = item)
          }
        }
      }
    }
  }
}

@Composable
private fun IncidentItemCard(incident: IncidentReport) {
  val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(incident.timestamp))

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = NavySurface),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .background(
              if (incident.category.contains("Theft") || incident.category.contains("Harass")) {
                CrimsonContainer
              } else {
                SafetyAmberContainer
              },
              RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = incident.category,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (incident.category.contains("Theft") || incident.category.contains("Harass")) CrimsonEmergencyGlow else SafetyAmber
          )
        }

        Text(
          text = "${incident.distanceKm} km away",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = incident.title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )

      Text(
        text = incident.locationName,
        fontSize = 12.sp,
        color = TextSecondary,
        modifier = Modifier.padding(top = 2.dp)
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafetyEmerald, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Reliability: ${(incident.confidenceScore * 100).toInt()}% • Source: ${incident.sourceName}",
            fontSize = 10.sp,
            color = TextTertiary,
            maxLines = 1
          )
        }

        Text(
          text = dateStr,
          fontSize = 10.sp,
          color = TextTertiary
        )
      }
    }
  }
}
