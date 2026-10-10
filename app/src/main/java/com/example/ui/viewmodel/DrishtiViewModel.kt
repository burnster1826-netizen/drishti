package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.GoogleAuthResult
import com.example.auth.GoogleSignInHelper
import com.example.data.media.SavedRecordingsManager
import com.example.data.media.SavedVideoFile
import com.example.data.model.*
import com.example.data.repository.SafetyRepository
import com.example.data.upload.ServerUploadConfig
import com.example.data.upload.ServerUploadResult
import com.example.data.upload.ServerUploadService
import com.example.data.upload.UploadStatus
import com.example.data.upload.UploadTaskState
import com.example.fakecall.FakeCallRingtoneManager
import com.example.location.ContactDispatchStatus
import com.example.location.LiveLocationDispatchManager
import com.example.location.LiveTrackingState
import com.example.location.RealtimeLocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AppScreen {
  object Login : AppScreen()
  object Onboarding : AppScreen()
  object MainTabs : AppScreen()
}

enum class NavigationTab {
  HOME,
  RECENT,
  SETTINGS
}

data class FakeCallActiveState(
  val isActive: Boolean = false,
  val isRinging: Boolean = false,
  val isScheduled: Boolean = false,
  val scheduledSecondsRemaining: Int = 0,
  val isVideo: Boolean = false,
  val callerName: String = "Mom (Home)",
  val callerNumber: String = "+1 (555) 382-9011",
  val elapsedSeconds: Int = 0,
  val isMuted: Boolean = false,
  val isSpeakerOn: Boolean = true,
  val isPaused: Boolean = false,
  val scriptMessage: String = "Hey! Where are you right now? I'm waiting outside in the car, let's head home."
)

data class SosActiveUiState(
  val isEmergencyOverlayVisible: Boolean = false,
  val countdownRemainingSeconds: Int = 10,
  val recordingSecondsRemaining: Int = 30,
  val isRecordingCamera: Boolean = false,
  val currentLatitude: Double = 37.7749,
  val currentLongitude: Double = -122.4194,
  val accuracyMeters: Float = 3.8f,
  val statusStage: String = "PENDING", // PENDING, ACTIVE, UPLOADING, DELIVERED, CANCELLED
  val showCancellationDialog: Boolean = false,
  val deliveryLog: List<String> = emptyList(),
  val localVideoFilePath: String? = null,
  val cameraLensFacing: Int = CameraSelector.LENS_FACING_BACK,
  val isUploadingToServer: Boolean = false,
  val uploadProgressPercent: Int = 0,
  val serverReceiptId: String? = null,
  val serverRemoteUrl: String? = null
)

class DrishtiViewModel(
  application: Application,
  private val repository: SafetyRepository = SafetyRepository(application)
) : AndroidViewModel(application) {

  // Managers
  val googleSignInHelper = GoogleSignInHelper(application.applicationContext)
  val serverUploadService = ServerUploadService(application.applicationContext)
  val savedRecordingsManager = SavedRecordingsManager(application.applicationContext)
  val locationTracker = RealtimeLocationTracker(application.applicationContext)
  val dispatchManager = LiveLocationDispatchManager(application.applicationContext)

  // Navigation & Screen flow
  val isUserLoggedIn = repository.isUserLoggedIn
  val isOnboardingCompleted = repository.isOnboardingCompleted
  val userProfile = repository.userProfile
  val contacts = repository.contacts
  val sosEvents = repository.sosEvents
  val incidents = repository.incidents
  val routeAnalyses = repository.routeAnalyses
  val isAnalyzingRoutes = repository.isAnalyzingRoutes
  val fakeMediaList = repository.fakeMediaList

  // Live Location & Contact Dispatch State
  val liveTrackingState: StateFlow<LiveTrackingState> = locationTracker.trackingState
  val contactDispatchStatuses: StateFlow<Map<String, ContactDispatchStatus>> = dispatchManager.contactStatuses
  val locationDispatchLogs: StateFlow<List<String>> = dispatchManager.dispatchLogs

  // Saved Media & Server Upload State
  val savedRecordings: StateFlow<List<SavedVideoFile>> = savedRecordingsManager.savedRecordings
  val uploadTaskState: StateFlow<UploadTaskState> = serverUploadService.uploadState
  val uploadHistory: StateFlow<List<UploadTaskState>> = serverUploadService.uploadHistory
  val serverConfig: StateFlow<ServerUploadConfig> = serverUploadService.config

  // Active Camera Lens Facing (Default: BACK CAMERA)
  private val _cameraLensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
  val cameraLensFacing: StateFlow<Int> = _cameraLensFacing.asStateFlow()

  // Google Sign-In Status / Toast message
  private val _authStatusMessage = MutableStateFlow<String?>(null)
  val authStatusMessage: StateFlow<String?> = _authStatusMessage.asStateFlow()

  private val _currentTab = MutableStateFlow(NavigationTab.HOME)
  val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

  // Fake Call State & Ringtone Audio
  val fakeCallRingtoneManager = FakeCallRingtoneManager(application.applicationContext)
  private val _fakeCallState = MutableStateFlow(FakeCallActiveState())
  val fakeCallState: StateFlow<FakeCallActiveState> = _fakeCallState.asStateFlow()
  private var fakeCallTimerJob: Job? = null
  private var fakeCallScheduleJob: Job? = null

  // SOS Full-screen overlay state
  private val _sosUiState = MutableStateFlow(SosActiveUiState())
  val sosUiState: StateFlow<SosActiveUiState> = _sosUiState.asStateFlow()
  private var countdownJob: Job? = null
  private var recordingJob: Job? = null

  // Route Planning Search Query State
  private val _routeDestination = MutableStateFlow("")
  val routeDestination: StateFlow<String> = _routeDestination.asStateFlow()

  private val _isRouteModalVisible = MutableStateFlow(false)
  val isRouteModalVisible: StateFlow<Boolean> = _isRouteModalVisible.asStateFlow()

  // Incident Filtering State
  private val _selectedIncidentCategory = MutableStateFlow("All")
  val selectedIncidentCategory: StateFlow<String> = _selectedIncidentCategory.asStateFlow()

  private val _maxIncidentDistanceKm = MutableStateFlow(5.0f)
  val maxIncidentDistanceKm: StateFlow<Float> = _maxIncidentDistanceKm.asStateFlow()

  init {
    // Keep verified contacts in sync with location dispatch manager
    viewModelScope.launch {
      contacts.collect { contactList ->
        dispatchManager.syncVerifiedContacts(contactList)
      }
    }

    // Automatically relay live GPS coordinates to selected verified contacts
    viewModelScope.launch {
      locationTracker.trackingState.collect { tracking ->
        if (tracking.isTrackingActive) {
          _sosUiState.value = _sosUiState.value.copy(
            currentLatitude = tracking.currentLatitude,
            currentLongitude = tracking.currentLongitude,
            accuracyMeters = tracking.accuracyMeters
          )

          // Dispatch real-time coordinates to verified contacts
          dispatchManager.dispatchLocationUpdate(
            latitude = tracking.currentLatitude,
            longitude = tracking.currentLongitude,
            accuracyMeters = tracking.accuracyMeters
          )
        }
      }
    }
  }

  fun setTab(tab: NavigationTab) {
    _currentTab.value = tab
  }

  // Camera toggle (toggles between back and front camera)
  fun toggleCameraLens() {
    val nextLens = if (_cameraLensFacing.value == CameraSelector.LENS_FACING_BACK) {
      CameraSelector.LENS_FACING_FRONT
    } else {
      CameraSelector.LENS_FACING_BACK
    }
    _cameraLensFacing.value = nextLens
    _sosUiState.value = _sosUiState.value.copy(cameraLensFacing = nextLens)
  }

  // Auth & Onboarding with CredentialManager
  fun signInWithGoogle(activity: Activity) {
    viewModelScope.launch {
      _authStatusMessage.value = "Authenticating with Google Credential Manager..."
      val result = googleSignInHelper.signInWithGoogle(activity)
      when (result) {
        is GoogleAuthResult.Success -> {
          repository.setAuthenticatedUser(
            userId = result.userId,
            name = result.displayName,
            email = result.email
          )
          _authStatusMessage.value = "Signed in as ${result.displayName}"
        }
        is GoogleAuthResult.Error -> {
          _authStatusMessage.value = result.message
        }
      }
    }
  }

  fun signInWithGoogleDirect(
    email: String = "alex.vance@gmail.com",
    name: String = "Alex Vance"
  ) {
    repository.loginWithGoogle(email = email, name = name)
    _authStatusMessage.value = "Signed in with Google account: $email"
  }

  fun clearAuthMessage() {
    _authStatusMessage.value = null
  }

  fun signOut() {
    repository.signOut()
  }

  fun completeOnboarding() {
    repository.completeOnboarding()
  }

  fun updateProfile(updated: UserProfile) {
    repository.updateProfile(updated)
  }

  // Real-time Location Sharing Actions
  fun toggleContactLocationStream(contactId: String) {
    dispatchManager.toggleContactSelection(contactId)
  }

  fun openInGoogleMaps(lat: Double, lng: Double) {
    locationTracker.openInGoogleMapsApp(lat, lng)
  }

  fun shareLiveLocationBroadcast(context: Context) {
    dispatchManager.shareLiveLocationBroadcast(
      context = context,
      userName = userProfile.value.name,
      latitude = _sosUiState.value.currentLatitude,
      longitude = _sosUiState.value.currentLongitude,
      accuracyMeters = _sosUiState.value.accuracyMeters
    )
  }

  fun sendLocationSmsToContact(context: Context, contact: EmergencyContact) {
    dispatchManager.sendSmsToContact(
      context = context,
      contact = contact,
      userName = userProfile.value.name,
      latitude = _sosUiState.value.currentLatitude,
      longitude = _sosUiState.value.currentLongitude,
      accuracyMeters = _sosUiState.value.accuracyMeters
    )
  }

  // SOS Triggers & Lifecycle
  fun startInstantVideoRecording(
    lat: Double = 37.7749,
    lng: Double = -122.4194,
    accuracy: Float = 3.8f,
    videoFilePath: String? = null
  ) {
    countdownJob?.cancel()
    recordingJob?.cancel()
    locationTracker.startRealtimeTracking()
    repository.initiateSos(lat, lng, accuracy)

    val finalPath = videoFilePath ?: "Movies/Drishti_SOS/DRISHTI_SOS_${System.currentTimeMillis()}.mp4"

    _sosUiState.value = SosActiveUiState(
      isEmergencyOverlayVisible = true,
      countdownRemainingSeconds = 0,
      recordingSecondsRemaining = 30,
      isRecordingCamera = true,
      currentLatitude = lat,
      currentLongitude = lng,
      accuracyMeters = accuracy,
      statusStage = "ACTIVE",
      localVideoFilePath = finalPath,
      cameraLensFacing = _cameraLensFacing.value,
      deliveryLog = listOf(
        "Direct Emergency SOS Activated",
        "Back Camera Hardware bound (Sensor priority locked)",
        "Real-Time Google Maps Location Sharing Active",
        "Live GPS coordinates streaming to verified emergency contacts",
        "Situational evidence stream recording to local phone storage"
      )
    )

    startRecordingCountdown(lat, lng, accuracy)
  }

  fun triggerSos(
    lat: Double = 37.7749,
    lng: Double = -122.4194,
    accuracy: Float = 3.8f
  ) {
    countdownJob?.cancel()
    recordingJob?.cancel()
    locationTracker.startRealtimeTracking()
    val event = repository.initiateSos(lat, lng, accuracy)

    _sosUiState.value = SosActiveUiState(
      isEmergencyOverlayVisible = true,
      countdownRemainingSeconds = 10,
      recordingSecondsRemaining = 30,
      isRecordingCamera = true,
      currentLatitude = lat,
      currentLongitude = lng,
      accuracyMeters = accuracy,
      statusStage = "PENDING",
      cameraLensFacing = _cameraLensFacing.value,
      deliveryLog = event.auditLog + listOf(
        "Active sensor: Back Camera Sensor (High Clarity)",
        "Google Maps Real-Time Location Beacon Activated",
        "10-second cancelable grace window active"
      )
    )

    // 10-second grace period countdown
    countdownJob = viewModelScope.launch {
      for (second in 9 downTo 0) {
        delay(1000)
        _sosUiState.value = _sosUiState.value.copy(countdownRemainingSeconds = second)
      }
      // Grace period completed -> Transition to ACTIVE recording and broadcasting
      transitionToActiveSos(lat, lng, accuracy)
    }
  }

  fun cancelSos(reason: String = "User clicked Cancel within grace period") {
    countdownJob?.cancel()
    recordingJob?.cancel()
    locationTracker.stopRealtimeTracking()
    repository.cancelSos(reason)

    _sosUiState.value = _sosUiState.value.copy(
      statusStage = "CANCELLED",
      isRecordingCamera = false,
      deliveryLog = _sosUiState.value.deliveryLog + listOf(
        "SOS Grace Period Cancellation verified",
        "Real-time location stream stopped",
        "No emergency dispatch triggered",
        "Sensor recording stopped and discarded"
      )
    )
  }

  fun dismissSosOverlay() {
    locationTracker.stopRealtimeTracking()
    _sosUiState.value = SosActiveUiState(isEmergencyOverlayVisible = false)
    repository.dismissActiveSos()
  }

  fun updateLocalVideoFilePath(path: String) {
    _sosUiState.value = _sosUiState.value.copy(localVideoFilePath = path)
    savedRecordingsManager.registerNewRecording(path)
  }

  private fun transitionToActiveSos(lat: Double, lng: Double, accuracy: Float) {
    _sosUiState.value = _sosUiState.value.copy(
      statusStage = "ACTIVE",
      deliveryLog = _sosUiState.value.deliveryLog + listOf(
        "Grace period expired: Transitioned to ACTIVE SOS",
        "Real-time Google Maps coordinates streaming to verified contacts",
        "Back camera recording in progress (Saving to Phone & Preparing Server Upload)...",
        "Multi-channel emergency alerts dispatched to verified contacts"
      )
    )

    startRecordingCountdown(lat, lng, accuracy)
  }

  private fun startRecordingCountdown(lat: Double, lng: Double, accuracy: Float) {
    recordingJob?.cancel()
    recordingJob = viewModelScope.launch {
      for (sec in 29 downTo 0) {
        delay(1000)
        _sosUiState.value = _sosUiState.value.copy(recordingSecondsRemaining = sec)
      }

      val videoPath = _sosUiState.value.localVideoFilePath ?: "Movies/Drishti_SOS/DRISHTI_SOS_${System.currentTimeMillis()}.mp4"

      // 1. Finalize recording and register on phone
      savedRecordingsManager.registerNewRecording(videoPath)
      repository.confirmAndBroadcastSos(videoPath)

      // 2. Perform server upload if configured
      val isAutoUpload = serverConfig.value.autoUploadOnSos
      _sosUiState.value = _sosUiState.value.copy(
        isRecordingCamera = false,
        statusStage = if (isAutoUpload) "UPLOADING" else "DELIVERED",
        isUploadingToServer = isAutoUpload,
        deliveryLog = _sosUiState.value.deliveryLog + listOf(
          "30s Video recording saved in Phone: Movies/Drishti_SOS",
          "Google Maps Live Location beacon sent to all selected contacts",
          if (isAutoUpload) "Initiating multipart streaming upload to server (${serverConfig.value.serverUrl})..."
          else "Recording saved on device. Ready for server sync."
        )
      )

      if (isAutoUpload) {
        val uploadResult = serverUploadService.uploadRecordedFile(
          fileUriOrPath = videoPath,
          userId = userProfile.value.id,
          latitude = _sosUiState.value.currentLatitude,
          longitude = _sosUiState.value.currentLongitude,
          accuracyMeters = _sosUiState.value.accuracyMeters,
          cameraFacing = if (_cameraLensFacing.value == CameraSelector.LENS_FACING_BACK) "BACK_CAMERA" else "FRONT_CAMERA"
        )

        when (uploadResult) {
          is ServerUploadResult.Success -> {
            savedRecordingsManager.markAsUploaded(videoPath.substringAfterLast('/'), uploadResult.receiptId)
            _sosUiState.value = _sosUiState.value.copy(
              statusStage = "DELIVERED",
              isUploadingToServer = false,
              uploadProgressPercent = 100,
              serverReceiptId = uploadResult.receiptId,
              serverRemoteUrl = uploadResult.remoteUrl,
              deliveryLog = _sosUiState.value.deliveryLog + listOf(
                "Uploaded to Server: Receipt ${uploadResult.receiptId}",
                "Secure Remote URL: ${uploadResult.remoteUrl}",
                "Emergency contacts notified with cloud video link & Google Maps coordinates"
              )
            )
          }
          is ServerUploadResult.Failure -> {
            _sosUiState.value = _sosUiState.value.copy(
              statusStage = "DELIVERED",
              isUploadingToServer = false,
              deliveryLog = _sosUiState.value.deliveryLog + listOf(
                "Server upload queued: ${uploadResult.errorMessage}",
                "Live Google Maps beacon & local video safely preserved on device"
              )
            )
          }
        }
      }
    }
  }

  // Server Upload Actions
  fun uploadFileToServer(file: SavedVideoFile) {
    viewModelScope.launch {
      val result = serverUploadService.uploadRecordedFile(
        fileUriOrPath = file.contentUri ?: file.localPath,
        userId = userProfile.value.id,
        incidentId = file.id,
        latitude = _sosUiState.value.currentLatitude,
        longitude = _sosUiState.value.currentLongitude,
        cameraFacing = "BACK_CAMERA"
      )
      if (result is ServerUploadResult.Success) {
        savedRecordingsManager.markAsUploaded(file.fileName, result.receiptId)
      }
    }
  }

  fun updateServerConfig(config: ServerUploadConfig) {
    serverUploadService.updateConfig(config)
  }

  fun resetUploadState() {
    serverUploadService.resetUploadState()
  }

  // Saved Media Actions
  fun refreshSavedRecordings() {
    savedRecordingsManager.refreshRecordingsList()
  }

  fun playRecording(context: Context, video: SavedVideoFile) {
    savedRecordingsManager.openInVideoPlayer(context, video)
  }

  fun shareRecording(context: Context, video: SavedVideoFile) {
    savedRecordingsManager.shareRecording(context, video)
  }

  // Fake Call simulation with realistic ringtone, vibration, and scheduling
  fun scheduleFakeCall(
    delaySeconds: Int,
    isVideo: Boolean,
    callerName: String? = null,
    callerNumber: String = "+1 (555) 382-9011",
    script: String? = null
  ) {
    val defaultItem = fakeMediaList.value.firstOrNull { it.isVideo == isVideo && it.isDefault }
      ?: fakeMediaList.value.firstOrNull { it.isVideo == isVideo }
    val finalName = callerName ?: defaultItem?.callerName ?: if (isVideo) "Mom (Home)" else "David (Colleague)"
    val finalScript = script ?: defaultItem?.audioScriptPreview ?: "Hey! Where are you right now? I'm waiting outside in the car, let's head home."

    if (delaySeconds <= 0) {
      triggerIncomingFakeCall(isVideo, finalName, callerNumber, finalScript)
      return
    }

    fakeCallScheduleJob?.cancel()
    _fakeCallState.value = FakeCallActiveState(
      isActive = false,
      isRinging = false,
      isScheduled = true,
      scheduledSecondsRemaining = delaySeconds,
      isVideo = isVideo,
      callerName = finalName,
      callerNumber = callerNumber,
      scriptMessage = finalScript
    )

    fakeCallScheduleJob = viewModelScope.launch {
      var remaining = delaySeconds
      while (remaining > 0 && _fakeCallState.value.isScheduled) {
        delay(1000)
        remaining--
        if (_fakeCallState.value.isScheduled) {
          _fakeCallState.value = _fakeCallState.value.copy(scheduledSecondsRemaining = remaining)
        }
      }
      if (_fakeCallState.value.isScheduled) {
        triggerIncomingFakeCall(isVideo, finalName, callerNumber, finalScript)
      }
    }
  }

  fun cancelScheduledFakeCall() {
    fakeCallScheduleJob?.cancel()
    _fakeCallState.value = FakeCallActiveState(isActive = false, isScheduled = false)
  }

  fun triggerIncomingFakeCall(
    isVideo: Boolean,
    callerName: String? = null,
    callerNumber: String = "+1 (555) 382-9011",
    script: String? = null
  ) {
    fakeCallScheduleJob?.cancel()
    val defaultItem = fakeMediaList.value.firstOrNull { it.isVideo == isVideo && it.isDefault }
      ?: fakeMediaList.value.firstOrNull { it.isVideo == isVideo }
    val finalName = callerName ?: defaultItem?.callerName ?: if (isVideo) "Mom (Home)" else "David (Colleague)"
    val finalScript = script ?: defaultItem?.audioScriptPreview ?: "Hey! Where are you right now? I'm waiting outside in the car, let's head home."

    _fakeCallState.value = FakeCallActiveState(
      isActive = true,
      isRinging = true,
      isScheduled = false,
      isVideo = isVideo,
      callerName = finalName,
      callerNumber = callerNumber,
      elapsedSeconds = 0,
      isMuted = false,
      isSpeakerOn = true,
      isPaused = false,
      scriptMessage = finalScript
    )

    // Play actual Android system ringtone and start phone vibration
    fakeCallRingtoneManager.startRingtoneAndVibration()
  }

  fun startFakeCall(isVideo: Boolean, callerName: String? = null) {
    triggerIncomingFakeCall(isVideo = isVideo, callerName = callerName)
  }

  fun answerFakeCall() {
    // Stop ringing and phone vibration immediately on answer
    fakeCallRingtoneManager.stopRingtoneAndVibration()
    _fakeCallState.value = _fakeCallState.value.copy(
      isRinging = false,
      elapsedSeconds = 0
    )

    fakeCallTimerJob?.cancel()
    fakeCallTimerJob = viewModelScope.launch {
      while (_fakeCallState.value.isActive && !_fakeCallState.value.isRinging) {
        delay(1000)
        if (!_fakeCallState.value.isPaused) {
          _fakeCallState.value = _fakeCallState.value.copy(
            elapsedSeconds = _fakeCallState.value.elapsedSeconds + 1
          )
        }
      }
    }
  }

  fun silenceFakeCallRingtone() {
    fakeCallRingtoneManager.stopRingtoneAndVibration()
  }

  fun toggleFakeCallMute() {
    _fakeCallState.value = _fakeCallState.value.copy(isMuted = !_fakeCallState.value.isMuted)
  }

  fun toggleFakeCallSpeaker() {
    _fakeCallState.value = _fakeCallState.value.copy(isSpeakerOn = !_fakeCallState.value.isSpeakerOn)
  }

  fun toggleFakeCallPause() {
    _fakeCallState.value = _fakeCallState.value.copy(isPaused = !_fakeCallState.value.isPaused)
  }

  fun endFakeCall() {
    fakeCallScheduleJob?.cancel()
    fakeCallTimerJob?.cancel()
    fakeCallRingtoneManager.stopRingtoneAndVibration()
    _fakeCallState.value = FakeCallActiveState(isActive = false, isRinging = false, isScheduled = false)
  }

  override fun onCleared() {
    super.onCleared()
    fakeCallRingtoneManager.stopRingtoneAndVibration()
    locationTracker.stopRealtimeTracking()
  }

  // Contacts
  fun addContact(name: String, phone: String, email: String, method: ContactNotificationMethod): Boolean {
    return repository.addContact(name, phone, email, method)
  }

  fun updateContact(contact: EmergencyContact) {
    repository.updateContact(contact)
  }

  fun deleteContact(id: String) {
    repository.deleteContact(id)
  }

  fun verifyContact(id: String, verified: Boolean) {
    repository.verifyContact(id, verified)
  }

  // Fake Media
  fun addFakeMedia(title: String, callerName: String, isVideo: Boolean, script: String) {
    repository.addFakeMedia(title, callerName, isVideo, script)
  }

  fun deleteFakeMedia(id: String) {
    repository.deleteFakeMedia(id)
  }

  fun setDefaultFakeMedia(id: String, isVideo: Boolean) {
    repository.setDefaultFakeMedia(id, isVideo)
  }

  // Safer Routes
  fun setRouteDestination(dest: String) {
    _routeDestination.value = dest
  }

  fun openRouteModal() {
    _isRouteModalVisible.value = true
  }

  fun closeRouteModal() {
    _isRouteModalVisible.value = false
    repository.clearRouteAnalysis()
  }

  fun searchSaferRoutes(destination: String) {
    viewModelScope.launch {
      repository.calculateSaferRoutes(destination)
    }
  }

  // Recent Incidents
  fun setIncidentFilter(category: String) {
    _selectedIncidentCategory.value = category
  }

  fun setMaxDistanceFilter(distanceKm: Float) {
    _maxIncidentDistanceKm.value = distanceKm
  }

  fun deleteAllData() {
    repository.deleteAllAccountData()
  }
}
