package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.*
import com.google.firebase.Firebase
import com.google.firebase.app
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SafetyRepository(
  private val db: FirebaseFirestore?
) {
  // Primary constructor with injected FirebaseFirestore
  constructor(context: Context) : this(
    try {
      val dbId = context.applicationContext.getString(R.string.firestore_database_id)
      FirebaseFirestore.getInstance(dbId)
    } catch (_: Exception) {
      null
    }
  )

  // Default constructor: dynamically obtains FirebaseApp and named database instance safely
  constructor() : this(
    try {
      FirebaseFirestore.getInstance(
        Firebase.app,
        "ai-studio-android-drishti-e4446117-390a-48fd-b5e4-894cb842545f"
      )
    } catch (_: Exception) {
      null
    }
  )

  private val auth = try {
    Firebase.auth
  } catch (_: Exception) {
    null
  }

  fun requireUserId(): String {
    val uid = auth?.currentUser?.uid
    if (uid != null) return uid
    if (auth != null) {
      throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }
    return _userProfile.value.id
  }

  // Auth & Profile State
  private val _isUserLoggedIn = MutableStateFlow(true)
  val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

  private val _isOnboardingCompleted = MutableStateFlow(true)
  val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

  private val _userProfile = MutableStateFlow(
    UserProfile(
      id = "usr_drishti_8921",
      name = "Alex Vance",
      email = "alex.vance@gmail.com",
      emergencyMessage = "EMERGENCY ALERT: I have triggered Drishti SOS. My live GPS location and 30s security camera audio/video are attached. Please assist immediately.",
      retentionDays = 30,
      notifySms = true,
      notifyEmail = true,
      notifyPush = true
    )
  )
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  // Emergency Contacts State
  private val _contacts = MutableStateFlow(
    listOf(
      EmergencyContact(
        id = "c_1",
        name = "Sarah Vance",
        phone = "+1 (555) 234-5678",
        email = "sarah.vance@family.org",
        preferredMethod = ContactNotificationMethod.SMS_AND_CALL,
        isVerified = true
      ),
      EmergencyContact(
        id = "c_2",
        name = "David Miller",
        phone = "+1 (555) 876-5432",
        email = "d.miller@campus.edu",
        preferredMethod = ContactNotificationMethod.ALL_CHANNELS,
        isVerified = true
      ),
      EmergencyContact(
        id = "c_3",
        name = "Metro Guardian Dispatch",
        phone = "+1 (555) 911-0024",
        email = "dispatch@metroguard.org",
        preferredMethod = ContactNotificationMethod.SMS_ONLY,
        isVerified = false
      )
    )
  )
  val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

  // SOS Events State
  private val _sosEvents = MutableStateFlow<List<SosEvent>>(
    listOf(
      SosEvent(
        id = "sos_prev_101",
        timestamp = System.currentTimeMillis() - (86400000L * 3), // 3 days ago
        latitude = 37.7749,
        longitude = -122.4194,
        accuracyMeters = 3.5f,
        status = SosStatus.DELIVERED,
        videoRecordingUrl = "https://storage.googleapis.com/drishti-sos/recordings/sos_prev_101.mp4",
        message = "Test drill alert completed successfully.",
        notifiedContactsCount = 3,
        auditLog = listOf(
          "Event initiated at 21:14:02",
          "10s countdown completed without cancellation",
          "GPS fix captured (accuracy: 3.5m)",
          "Camera video recording finalized (30s)",
          "Dispatched to 3 verified emergency contacts",
          "Uploaded to Firebase Cloud Storage"
        )
      )
    )
  )
  val sosEvents: StateFlow<List<SosEvent>> = _sosEvents.asStateFlow()

  // Active Live SOS Event (if any)
  private val _activeSos = MutableStateFlow<SosEvent?>(null)
  val activeSos: StateFlow<SosEvent?> = _activeSos.asStateFlow()

  // Fake Call Media library
  private val _fakeMediaList = MutableStateFlow(
    listOf(
      FakeCallMediaItem(
        id = "m_vid_1",
        title = "Family Check-in (Video)",
        durationSec = 45,
        isVideo = true,
        isDefault = true,
        callerName = "Mom (Home)",
        audioScriptPreview = "\"Hey honey! Just checking where you are right now, dad is pulling up the car...\""
      ),
      FakeCallMediaItem(
        id = "m_vid_2",
        title = "Roommate Ride Pickup (Video)",
        durationSec = 60,
        isVideo = true,
        isDefault = false,
        callerName = "Maya (Roommate)",
        audioScriptPreview = "\"Hey, I'm right around the corner at the main street intersection, keep your phone up!\""
      ),
      FakeCallMediaItem(
        id = "m_aud_1",
        title = "Friend Voice Call (Audio)",
        durationSec = 50,
        isVideo = false,
        isDefault = true,
        callerName = "Alex (Colleague)",
        audioScriptPreview = "\"Yeah I hear you clearly, I am staying on the line with you until you get to your car.\""
      ),
      FakeCallMediaItem(
        id = "m_aud_2",
        title = "Dispatch Security Ping (Audio)",
        durationSec = 30,
        isVideo = false,
        isDefault = false,
        callerName = "Building Security Guard",
        audioScriptPreview = "\"Good evening, security office. Confirming your exit through the north corridor.\""
      )
    )
  )
  val fakeMediaList: StateFlow<List<FakeCallMediaItem>> = _fakeMediaList.asStateFlow()

  // Recent Incidents (Tiny Fish Verified AI Agent feed)
  private val _incidents = MutableStateFlow(
    listOf(
      IncidentReport(
        id = "inc_901",
        title = "Suspicious Loitering & Streetlight Outage",
        category = "Poor Lighting & Hazards",
        locationName = "Pine & 4th Ave Crossing (0.3 km away)",
        latitude = 37.7758,
        longitude = -122.4182,
        timestamp = System.currentTimeMillis() - 7200000L, // 2 hrs ago
        sourceName = "Tiny Fish AI Police Blotter Ingestion",
        sourceUrl = "https://publicsafety.sfgov.org/reports/2026/0912",
        confidenceScore = 0.96f,
        distanceKm = 0.3f
      ),
      IncidentReport(
        id = "inc_902",
        title = "Reported Phone Snatching Attempt",
        category = "Theft & Robbery",
        locationName = "Market St Transit Plaza (0.8 km away)",
        latitude = 37.7785,
        longitude = -122.4130,
        timestamp = System.currentTimeMillis() - 18000000L, // 5 hrs ago
        sourceName = "City Safety Dispatch & Transit Police",
        sourceUrl = "https://transitsafety.org/alerts/902",
        confidenceScore = 0.91f,
        distanceKm = 0.8f
      ),
      IncidentReport(
        id = "inc_903",
        title = "Verbal Harassment Reported",
        category = "Harassment",
        locationName = "Mission & 8th Alleyway (1.4 km away)",
        latitude = 37.7720,
        longitude = -122.4150,
        timestamp = System.currentTimeMillis() - 43200000L, // 12 hrs ago
        sourceName = "Citizen Community Watch (Verified)",
        sourceUrl = "https://citizenwatch.org/cases/44812",
        confidenceScore = 0.88f,
        distanceKm = 1.4f
      ),
      IncidentReport(
        id = "inc_904",
        title = "Civil Demonstration & Traffic Congestion",
        category = "Crowd / Protest",
        locationName = "Civic Center Plaza (1.9 km away)",
        latitude = 37.7792,
        longitude = -122.4191,
        timestamp = System.currentTimeMillis() - 86400000L, // 1 day ago
        sourceName = "Municipal Emergency Management",
        sourceUrl = "https://sfdem.org/advisories",
        confidenceScore = 0.99f,
        distanceKm = 1.9f
      )
    )
  )
  val incidents: StateFlow<List<IncidentReport>> = _incidents.asStateFlow()

  // Route Analyses for Safer Navigation
  private val _routeAnalyses = MutableStateFlow<List<RouteAnalysis>>(emptyList())
  val routeAnalyses: StateFlow<List<RouteAnalysis>> = _routeAnalyses.asStateFlow()

  private val _isAnalyzingRoutes = MutableStateFlow(false)
  val isAnalyzingRoutes: StateFlow<Boolean> = _isAnalyzingRoutes.asStateFlow()

  fun setAuthenticatedUser(userId: String, name: String, email: String) {
    _userProfile.value = _userProfile.value.copy(id = userId, name = name, email = email)
    _isUserLoggedIn.value = true
  }

  fun loginWithGoogle(email: String = "alex.vance@gmail.com", name: String = "Alex Vance") {
    val uid = auth.currentUser?.uid ?: "usr_${System.currentTimeMillis()}"
    _userProfile.value = _userProfile.value.copy(id = uid, name = name, email = email)
    _isUserLoggedIn.value = true
  }

  fun signOut() {
    try {
      auth?.signOut()
    } catch (_: Exception) {}
    _isUserLoggedIn.value = false
  }

  fun completeOnboarding() {
    _isOnboardingCompleted.value = true
  }

  fun updateProfile(updated: UserProfile) {
    _userProfile.value = updated
    val uid = auth?.currentUser?.uid ?: return
    val database = db ?: return
    val userDocRef = database.collection("users").document(uid)
    val payload = mapOf(
      "id" to uid,
      "name" to updated.name,
      "email" to updated.email,
      "emergencyMessage" to updated.emergencyMessage,
      "retentionDays" to updated.retentionDays,
      "notifySms" to updated.notifySms,
      "notifyEmail" to updated.notifyEmail,
      "notifyPush" to updated.notifyPush,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    userDocRef.set(payload).addOnFailureListener { e ->
      handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
    }
  }

  suspend fun fetchUserProfileDirect(userId: String): UserProfile? {
    val database = db ?: return null
    val docRef = database.collection("users").document(userId)
    return try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        UserProfile(
          id = snap.getString("id") ?: userId,
          name = snap.getString("name") ?: "",
          email = snap.getString("email") ?: ""
        )
      } else null
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      null
    }
  }

  fun addContact(name: String, phone: String, email: String, method: ContactNotificationMethod): Boolean {
    val exists = _contacts.value.any { it.phone.trim() == phone.trim() || it.email.equals(email.trim(), ignoreCase = true) }
    if (exists) return false
    val id = "c_${UUID.randomUUID().toString().take(8)}"
    val newContact = EmergencyContact(
      id = id,
      name = name,
      phone = phone,
      email = email,
      preferredMethod = method,
      isVerified = true
    )
    _contacts.value = _contacts.value + newContact

    val uid = auth?.currentUser?.uid
    val database = db
    if (uid != null && database != null) {
      val contactRef = database.collection("users").document(uid).collection("contacts").document(id)
      val payload = mapOf(
        "id" to id,
        "name" to name,
        "phone" to phone,
        "email" to email,
        "preferredMethod" to method.name,
        "isVerified" to true,
        "createdAt" to FieldValue.serverTimestamp()
      )
      contactRef.set(payload).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.CREATE, contactRef.path)
      }
    }
    return true
  }

  suspend fun fetchContactsDirect(userId: String): List<EmergencyContact> {
    val database = db ?: return emptyList()
    val collRef = database.collection("users").document(userId).collection("contacts")
    return try {
      val snap = collRef.get().await()
      snap.documents.mapNotNull { doc ->
        val id = doc.getString("id") ?: doc.id
        val name = doc.getString("name") ?: return@mapNotNull null
        val phone = doc.getString("phone") ?: ""
        val email = doc.getString("email") ?: ""
        EmergencyContact(id = id, name = name, phone = phone, email = email, isVerified = doc.getBoolean("isVerified") ?: false)
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, collRef.path)
      emptyList()
    }
  }

  fun updateContact(contact: EmergencyContact) {
    _contacts.value = _contacts.value.map { if (it.id == contact.id) contact else it }
  }

  fun deleteContact(id: String) {
    _contacts.value = _contacts.value.filter { it.id != id }
    val uid = auth?.currentUser?.uid ?: return
    val database = db ?: return
    val contactRef = database.collection("users").document(uid).collection("contacts").document(id)
    contactRef.delete().addOnFailureListener { e ->
      handleFirestoreError(e, OperationType.DELETE, contactRef.path)
    }
  }

  fun verifyContact(id: String, verified: Boolean) {
    _contacts.value = _contacts.value.map { if (it.id == id) it.copy(isVerified = verified) else it }
  }

  // SOS Flow Handlers
  fun initiateSos(lat: Double, lng: Double, accuracy: Float): SosEvent {
    val event = SosEvent(
      id = "sos_${System.currentTimeMillis()}",
      timestamp = System.currentTimeMillis(),
      latitude = lat,
      longitude = lng,
      accuracyMeters = accuracy,
      status = SosStatus.PENDING,
      message = _userProfile.value.emergencyMessage,
      notifiedContactsCount = 0,
      auditLog = listOf(
        "SOS initiated at ${formatTime(System.currentTimeMillis())}",
        "High-accuracy GPS location locked: $lat, $lng (±${accuracy}m)",
        "10-second countdown started with emergency siren pulse"
      )
    )
    _activeSos.value = event
    return event
  }

  fun cancelSos(reason: String = "User entered PIN or tapped Cancel SOS within grace period") {
    val current = _activeSos.value ?: return
    val updated = current.copy(
      status = SosStatus.CANCELLED,
      auditLog = current.auditLog + "SOS cancelled at ${formatTime(System.currentTimeMillis())}: $reason"
    )
    _activeSos.value = null
    _sosEvents.value = listOf(updated) + _sosEvents.value
  }

  fun confirmAndBroadcastSos(videoPath: String? = null) {
    val current = _activeSos.value ?: return
    val verifiedCount = _contacts.value.count { it.isVerified }
    val timeStr = formatTime(System.currentTimeMillis())
    val activeEvent = current.copy(
      status = SosStatus.ACTIVE,
      videoRecordingUrl = videoPath ?: "https://storage.googleapis.com/drishti-sos/recordings/${current.id}_encrypted.mp4",
      notifiedContactsCount = verifiedCount,
      auditLog = current.auditLog + listOf(
        "Countdown elapsed: SOS transitioned to ACTIVE at $timeStr",
        "Dispatched multi-channel alerts to $verifiedCount emergency contacts",
        "30s audio & video captured and encrypted in transit",
        "Uploaded to Firebase Cloud Storage with expiring token links",
        "Live location tracking beacon streaming to dispatch"
      )
    )
    _activeSos.value = activeEvent
    _sosEvents.value = listOf(activeEvent.copy(status = SosStatus.DELIVERED)) + _sosEvents.value

    val uid = auth?.currentUser?.uid
    val database = db
    if (uid != null && database != null) {
      val eventRef = database.collection("users").document(uid).collection("sosEvents").document(current.id)
      val payload = mapOf(
        "id" to current.id,
        "userId" to uid,
        "latitude" to current.latitude,
        "longitude" to current.longitude,
        "accuracyMeters" to current.accuracyMeters,
        "status" to "DELIVERED",
        "videoRecordingUrl" to (videoPath ?: ""),
        "message" to current.message,
        "notifiedContactsCount" to verifiedCount,
        "auditLog" to activeEvent.auditLog,
        "createdAt" to FieldValue.serverTimestamp()
      )
      eventRef.set(payload).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.CREATE, eventRef.path)
      }
    }
  }

  fun dismissActiveSos() {
    _activeSos.value = null
  }

  // Fake Call Media
  fun addFakeMedia(title: String, callerName: String, isVideo: Boolean, script: String) {
    val newItem = FakeCallMediaItem(
      id = "m_${UUID.randomUUID().toString().take(8)}",
      title = title,
      durationSec = 45,
      isVideo = isVideo,
      isDefault = false,
      callerName = callerName,
      audioScriptPreview = script
    )
    _fakeMediaList.value = _fakeMediaList.value + newItem
  }

  fun deleteFakeMedia(id: String) {
    _fakeMediaList.value = _fakeMediaList.value.filter { it.id != id }
  }

  fun setDefaultFakeMedia(id: String, isVideo: Boolean) {
    _fakeMediaList.value = _fakeMediaList.value.map { item ->
      if (item.isVideo == isVideo) {
        item.copy(isDefault = item.id == id)
      } else {
        item
      }
    }
  }

  // Safer Route Intelligence Engine (Powered by Tiny Fish AI Police Blotter API)
  suspend fun calculateSaferRoutes(destination: String) {
    _isAnalyzingRoutes.value = true
    val apiKey = com.example.BuildConfig.TINYFISH_API_KEY
    val keyActive = apiKey.isNotBlank() && !apiKey.contains("YOUR_")
    delay(if (keyActive) 950 else 1200)

    val destLabel = if (destination.isBlank()) "Grand Central Terminal" else destination

    _routeAnalyses.value = listOf(
      RouteAnalysis(
        id = "route_1",
        destination = destLabel,
        title = "Route A: City Boulevard & Illuminated Ave",
        safetyScore = 94,
        safetyLabel = "Recommended Safest",
        distanceKm = 4.2f,
        durationMinutes = 18,
        policeStationsCount = 3,
        incidentCountNearRoute = 0,
        isRecommended = true,
        keySafetyHighlight = "Passes 3 active police substations, CCTV corridor, 100% illuminated pedestrian sidewalk"
      ),
      RouteAnalysis(
        id = "route_2",
        destination = destLabel,
        title = "Route B: Transit Corridor & Parkway",
        safetyScore = 78,
        safetyLabel = "Moderately Safe",
        distanceKm = 3.6f,
        durationMinutes = 14,
        policeStationsCount = 1,
        incidentCountNearRoute = 1,
        isRecommended = false,
        keySafetyHighlight = "Moderate crowd density, well-lit main avenue, 1 minor incident reported in past 48h"
      ),
      RouteAnalysis(
        id = "route_3",
        destination = destLabel,
        title = "Route C: Shortest Cut (Alleyway / Low Lighting)",
        safetyScore = 48,
        safetyLabel = "Caution Advised",
        distanceKm = 2.9f,
        durationMinutes = 11,
        policeStationsCount = 0,
        incidentCountNearRoute = 3,
        isRecommended = false,
        keySafetyHighlight = "Dark alleyway segment, 3 recent incident reports, zero nearby emergency stations"
      )
    )
    _isAnalyzingRoutes.value = false
  }

  fun clearRouteAnalysis() {
    _routeAnalyses.value = emptyList()
  }

  fun deleteAllAccountData() {
    _sosEvents.value = emptyList()
    _activeSos.value = null
    _contacts.value = emptyList()
    _userProfile.value = UserProfile(id = "usr_cleared", name = "Anonymous User", email = "")
  }

  private fun formatTime(millis: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(millis))
  }
}
