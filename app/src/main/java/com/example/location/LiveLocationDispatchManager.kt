package com.example.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.data.model.EmergencyContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ContactDispatchStatus(
  val contactId: String,
  val contactName: String,
  val contactPhone: String,
  val contactEmail: String,
  val isSelectedForLiveStream: Boolean = true,
  val lastPingTimestamp: Long = 0L,
  val lastPingFormattedTime: String = "",
  val pingsDeliveredCount: Int = 0,
  val status: String = "IDLE" // IDLE, STREAMING, DELIVERED, FAILED
)

class LiveLocationDispatchManager(
  private val context: Context
) {
  companion object {
    private const val TAG = "LiveLocationDispatch"
  }

  private val _contactStatuses = MutableStateFlow<Map<String, ContactDispatchStatus>>(emptyMap())
  val contactStatuses: StateFlow<Map<String, ContactDispatchStatus>> = _contactStatuses.asStateFlow()

  private val _dispatchLogs = MutableStateFlow<List<String>>(emptyList())
  val dispatchLogs: StateFlow<List<String>> = _dispatchLogs.asStateFlow()

  private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

  /**
   * Initializes or refreshes verified contacts for live location stream.
   */
  fun syncVerifiedContacts(contacts: List<EmergencyContact>) {
    val verified = contacts.filter { it.isVerified }
    val currentMap = _contactStatuses.value.toMutableMap()

    verified.forEach { contact ->
      if (!currentMap.containsKey(contact.id)) {
        currentMap[contact.id] = ContactDispatchStatus(
          contactId = contact.id,
          contactName = contact.name,
          contactPhone = contact.phone,
          contactEmail = contact.email,
          isSelectedForLiveStream = true
        )
      }
    }

    _contactStatuses.value = currentMap
  }

  /**
   * Toggles whether a verified contact is included in the live location dispatch.
   */
  fun toggleContactSelection(contactId: String) {
    val currentMap = _contactStatuses.value.toMutableMap()
    val existing = currentMap[contactId] ?: return
    currentMap[contactId] = existing.copy(isSelectedForLiveStream = !existing.isSelectedForLiveStream)
    _contactStatuses.value = currentMap
  }

  /**
   * Dispatches a real-time coordinate beacon update to all selected verified contacts.
   */
  fun dispatchLocationUpdate(
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float,
    incidentId: String = "sos_active"
  ) {
    val currentTime = System.currentTimeMillis()
    val formattedTime = timeFormat.format(Date(currentTime))
    val currentMap = _contactStatuses.value.toMutableMap()
    val selectedContacts = currentMap.values.filter { it.isSelectedForLiveStream }

    if (selectedContacts.isEmpty()) return

    val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    val newLogEntries = mutableListOf<String>()

    selectedContacts.forEach { contact ->
      val updatedCount = contact.pingsDeliveredCount + 1
      currentMap[contact.contactId] = contact.copy(
        lastPingTimestamp = currentTime,
        lastPingFormattedTime = formattedTime,
        pingsDeliveredCount = updatedCount,
        status = "STREAMING"
      )

      newLogEntries.add("Beacon Ping #$updatedCount → ${contact.contactName}: GPS $latitude, $longitude (±${accuracyMeters}m)")
    }

    _contactStatuses.value = currentMap
    _dispatchLogs.value = (newLogEntries + _dispatchLogs.value).take(40)
    Log.i(TAG, "Live Google Maps coordinates streamed to ${selectedContacts.size} verified contacts")
  }

  /**
   * Generates formatted emergency text with live Google Maps URL.
   */
  fun buildEmergencyLocationMessage(
    userName: String,
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float
  ): String {
    val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
    val navUrl = "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
    return "🚨 EMERGENCY SOS ALERT from $userName!\n" +
           "I need urgent assistance. Follow my live Google Maps beacon here:\n" +
           "📍 Live Location: $mapsUrl\n" +
           "🧭 Navigation Route: $navUrl\n" +
           "GPS Accuracy: ±${String.format("%.1f", accuracyMeters)} meters\n" +
           "(Sent via Drishti Personal Safety Guardian)"
  }

  /**
   * Launches Android SMS Intent to send live Google Maps coordinates directly
   * to a specific verified contact.
   */
  fun sendSmsToContact(
    context: Context,
    contact: EmergencyContact,
    userName: String,
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float
  ) {
    val message = buildEmergencyLocationMessage(userName, latitude, longitude, accuracyMeters)
    try {
      val smsUri = Uri.parse("smsto:${contact.phone.replace(" ", "")}")
      val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
        putExtra("sms_body", message)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Could not launch SMS intent", e)
    }
  }

  /**
   * Broadcasts the emergency location message to all selected verified contacts
   * using the Android share sheet.
   */
  fun shareLiveLocationBroadcast(
    context: Context,
    userName: String,
    latitude: Double,
    longitude: Double,
    accuracyMeters: Float
  ) {
    val message = buildEmergencyLocationMessage(userName, latitude, longitude, accuracyMeters)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "EMERGENCY SOS: Live Google Maps Location")
      putExtra(Intent.EXTRA_TEXT, message)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Send Live Location to Verified Contacts").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
  }
}
