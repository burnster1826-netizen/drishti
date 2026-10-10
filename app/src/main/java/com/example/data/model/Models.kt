package com.example.data.model

data class UserProfile(
  val id: String = "user_default_01",
  val name: String = "Alex Vance",
  val email: String = "alex.vance@gmail.com",
  val photoUrl: String? = null,
  val emergencyMessage: String = "EMERGENCY: I need immediate assistance! Here is my live location and situation recording.",
  val retentionDays: Int = 30,
  val notifySms: Boolean = true,
  val notifyEmail: Boolean = true,
  val notifyPush: Boolean = true
)

data class EmergencyContact(
  val id: String,
  val name: String,
  val phone: String,
  val email: String,
  val preferredMethod: ContactNotificationMethod = ContactNotificationMethod.SMS_AND_CALL,
  val isVerified: Boolean = false
)

enum class ContactNotificationMethod {
  SMS_AND_CALL,
  SMS_ONLY,
  EMAIL_ONLY,
  ALL_CHANNELS
}

enum class SosStatus {
  PENDING,    // 10s countdown running
  CANCELLED,  // user canceled within 10s
  ACTIVE,     // alerts dispatched, recording live
  UPLOADED,   // recording uploaded to storage
  DELIVERED   // emergency contacts confirmed receipt
}

data class SosEvent(
  val id: String,
  val timestamp: Long = System.currentTimeMillis(),
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float = 4.2f,
  val status: SosStatus = SosStatus.PENDING,
  val videoRecordingUrl: String? = null,
  val message: String = "",
  val notifiedContactsCount: Int = 0,
  val isQueuedOffline: Boolean = false,
  val auditLog: List<String> = emptyList()
)

data class IncidentReport(
  val id: String,
  val title: String,
  val category: String, // e.g. "Harassment", "Theft", "Poor Lighting", "Assault", "Protest"
  val locationName: String,
  val latitude: Double,
  val longitude: Double,
  val timestamp: Long,
  val sourceName: String, // "Tiny Fish Verified Police Blotter", "Citizen Watch"
  val sourceUrl: String,
  val confidenceScore: Float, // 0.0 to 1.0
  val distanceKm: Float
)

data class RouteAnalysis(
  val id: String,
  val destination: String,
  val title: String,
  val safetyScore: Int, // 0-100
  val safetyLabel: String, // "Highest Safety", "Balanced", "Fastest"
  val distanceKm: Float,
  val durationMinutes: Int,
  val policeStationsCount: Int,
  val incidentCountNearRoute: Int,
  val isRecommended: Boolean = false,
  val keySafetyHighlight: String
)

data class FakeCallMediaItem(
  val id: String,
  val title: String,
  val durationSec: Int,
  val isVideo: Boolean,
  val isDefault: Boolean,
  val callerName: String = "Mom (Home)",
  val audioScriptPreview: String = "Hey! Where are you right now? I'm waiting outside for you...",
  val localResourceOrUri: String = ""
)
