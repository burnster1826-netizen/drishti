package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocationBreadcrumb(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float,
  val timestamp: Long,
  val formattedTime: String,
  val speedMps: Float = 0f
)

data class LiveTrackingState(
  val isTrackingActive: Boolean = false,
  val currentLatitude: Double = 37.7749,
  val currentLongitude: Double = -122.4194,
  val accuracyMeters: Float = 3.5f,
  val speedMps: Float = 0f,
  val bearing: Float = 0f,
  val pingCount: Int = 0,
  val breadcrumbs: List<LocationBreadcrumb> = emptyList(),
  val googleMapsUrl: String = "https://www.google.com/maps/search/?api=1&query=37.7749,-122.4194",
  val navigationUrl: String = "https://www.google.com/maps/dir/?api=1&destination=37.7749,-122.4194"
)

class RealtimeLocationTracker(
  private val context: Context
) {
  companion object {
    private const val TAG = "RealtimeLocationTracker"
  }

  private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)

  private val _trackingState = MutableStateFlow(LiveTrackingState())
  val trackingState: StateFlow<LiveTrackingState> = _trackingState.asStateFlow()

  private var locationCallback: LocationCallback? = null
  private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

  /**
   * Starts high-accuracy real-time GPS location streaming updates.
   */
  @SuppressLint("MissingPermission")
  fun startRealtimeTracking() {
    if (locationCallback != null) return // Already tracking

    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
      .setMinUpdateIntervalMillis(1000L)
      .setMinUpdateDistanceMeters(1.0f)
      .setWaitForAccurateLocation(false)
      .build()

    val callback = object : LocationCallback() {
      override fun onLocationResult(result: LocationResult) {
        val loc = result.lastLocation ?: return
        updateCurrentLocation(loc)
      }

      override fun onLocationAvailability(avail: LocationAvailability) {
        Log.i(TAG, "Location availability changed: ${avail.isLocationAvailable}")
      }
    }

    locationCallback = callback

    try {
      fusedLocationClient.requestLocationUpdates(
        locationRequest,
        callback,
        Looper.getMainLooper()
      )

      // Fetch last known location immediately
      fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
        if (loc != null) {
          updateCurrentLocation(loc)
        }
      }

      _trackingState.value = _trackingState.value.copy(isTrackingActive = true)
      Log.i(TAG, "Real-time Google Maps GPS location tracking started")
    } catch (e: SecurityException) {
      Log.e(TAG, "Location permission missing when starting tracking", e)
    }
  }

  /**
   * Stops real-time GPS streaming updates.
   */
  fun stopRealtimeTracking() {
    locationCallback?.let { callback ->
      fusedLocationClient.removeLocationUpdates(callback)
      locationCallback = null
    }
    _trackingState.value = _trackingState.value.copy(isTrackingActive = false)
    Log.i(TAG, "Real-time Google Maps GPS location tracking stopped")
  }

  private fun updateCurrentLocation(loc: Location) {
    val lat = loc.latitude
    val lng = loc.longitude
    val acc = loc.accuracy
    val speed = loc.speed
    val bearing = loc.bearing
    val time = System.currentTimeMillis()
    val formattedTime = timeFormat.format(Date(time))

    val newCrumb = LocationBreadcrumb(
      latitude = lat,
      longitude = lng,
      accuracyMeters = acc,
      timestamp = time,
      formattedTime = formattedTime,
      speedMps = speed
    )

    val updatedBreadcrumbs = (listOf(newCrumb) + _trackingState.value.breadcrumbs).take(20)

    val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
    val navUrl = "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"

    _trackingState.value = _trackingState.value.copy(
      currentLatitude = lat,
      currentLongitude = lng,
      accuracyMeters = acc,
      speedMps = speed,
      bearing = bearing,
      pingCount = _trackingState.value.pingCount + 1,
      breadcrumbs = updatedBreadcrumbs,
      googleMapsUrl = mapsUrl,
      navigationUrl = navUrl
    )
  }

  /**
   * Launches Google Maps App or Web with live coordinates pin.
   */
  fun openInGoogleMapsApp(latitude: Double, longitude: Double) {
    try {
      // 1. Try native Google Maps geo URI
      val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Emergency+SOS+Location)")
      val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(mapIntent)
    } catch (e: Exception) {
      // 2. Fallback to generic browser / Google Maps Web
      val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
      val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(webIntent)
    }
  }

  /**
   * Generates a Google Maps Static Map image URL using MAPS_API_KEY from BuildConfig.
   */
  fun getGoogleMapsStaticImageUrl(latitude: Double, longitude: Double, width: Int = 600, height: Int = 300): String {
    val apiKey = try {
      BuildConfig.MAPS_API_KEY
    } catch (_: Exception) {
      ""
    }
    return if (apiKey.isNotBlank() && apiKey != "YOUR_GOOGLE_MAPS_API_KEY") {
      "https://maps.googleapis.com/maps/api/staticmap?center=$latitude,$longitude&zoom=16&size=${width}x$height&scale=2&maptype=roadmap&markers=color:red%7Clabel:SOS%7C$latitude,$longitude&key=$apiKey"
    } else {
      // Clean fallback dynamic tile representation
      "https://maps.googleapis.com/maps/api/staticmap?center=$latitude,$longitude&zoom=15&size=${width}x$height&markers=color:red%7C$latitude,$longitude"
    }
  }
}
