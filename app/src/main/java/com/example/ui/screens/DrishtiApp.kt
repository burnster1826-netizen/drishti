package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FullscreenFakeCallModal
import com.example.ui.components.FullscreenSosOverlay
import com.example.ui.components.RouteSafetyDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.DrishtiViewModel
import com.example.ui.viewmodel.NavigationTab

@Composable
fun DrishtiApp(
  viewModel: DrishtiViewModel = viewModel(),
  triggerSosOnInit: Boolean = false,
  onSosTriggerHandled: () -> Unit = {}
) {
  val context = LocalContext.current
  val activity = context as? Activity

  // Automatically trigger SOS if launched from Home Screen Widget
  LaunchedEffect(triggerSosOnInit) {
    if (triggerSosOnInit) {
      viewModel.triggerSos()
      onSosTriggerHandled()
    }
  }

  val isLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
  val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
  val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val contacts by viewModel.contacts.collectAsStateWithLifecycle()
  val fakeMediaList by viewModel.fakeMediaList.collectAsStateWithLifecycle()
  val sosEvents by viewModel.sosEvents.collectAsStateWithLifecycle()
  val incidents by viewModel.incidents.collectAsStateWithLifecycle()
  val routeAnalyses by viewModel.routeAnalyses.collectAsStateWithLifecycle()
  val isAnalyzingRoutes by viewModel.isAnalyzingRoutes.collectAsStateWithLifecycle()
  val routeDestination by viewModel.routeDestination.collectAsStateWithLifecycle()
  val isRouteModalVisible by viewModel.isRouteModalVisible.collectAsStateWithLifecycle()
  val selectedIncidentCategory by viewModel.selectedIncidentCategory.collectAsStateWithLifecycle()
  val maxIncidentDistanceKm by viewModel.maxIncidentDistanceKm.collectAsStateWithLifecycle()

  // Saved Media & Server Upload States
  val savedRecordings by viewModel.savedRecordings.collectAsStateWithLifecycle()
  val uploadTaskState by viewModel.uploadTaskState.collectAsStateWithLifecycle()
  val serverConfig by viewModel.serverConfig.collectAsStateWithLifecycle()
  val authStatusMessage by viewModel.authStatusMessage.collectAsStateWithLifecycle()
  val liveTrackingState by viewModel.liveTrackingState.collectAsStateWithLifecycle()
  val contactDispatchStatuses by viewModel.contactDispatchStatuses.collectAsStateWithLifecycle()

  // Full-screen overlay states
  val sosUiState by viewModel.sosUiState.collectAsStateWithLifecycle()
  val fakeCallState by viewModel.fakeCallState.collectAsStateWithLifecycle()

  val verifiedContactsCount = contacts.count { it.isVerified }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(NavyBackground)
  ) {
    if (!isLoggedIn || !isOnboardingCompleted) {
      // 1. Login & Onboarding flow with Google Sign-In
      LoginAndOnboardingScreen(
        isLoggedIn = isLoggedIn,
        isOnboardingCompleted = isOnboardingCompleted,
        onGoogleSignIn = {
          if (activity != null) {
            viewModel.signInWithGoogle(activity)
          } else {
            viewModel.signInWithGoogleDirect()
          }
        },
        onQuickDemoSignIn = { email, name ->
          viewModel.signInWithGoogleDirect(email = email, name = name)
        },
        onCompleteOnboarding = { viewModel.completeOnboarding() },
        statusMessage = authStatusMessage
      )
    } else {
      // 2. Main 3-Tab Interface
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NavyBackground,
        bottomBar = {
          NavigationBar(
            containerColor = NavySurface,
            contentColor = TextPrimary,
            tonalElevation = 8.dp,
            modifier = Modifier
              .windowInsetsPadding(WindowInsets.navigationBars)
              .testTag("drishti_bottom_navigation")
          ) {
            // Tab 1: Home
            NavigationBarItem(
              selected = currentTab == NavigationTab.HOME,
              onClick = { viewModel.setTab(NavigationTab.HOME) },
              icon = {
                Icon(
                  imageVector = if (currentTab == NavigationTab.HOME) Icons.Default.Shield else Icons.Default.ShieldMoon,
                  contentDescription = "Home Tab",
                  tint = if (currentTab == NavigationTab.HOME) CrimsonEmergencyGlow else TextSecondary
                )
              },
              label = {
                Text(
                  text = "Home",
                  fontSize = 11.sp,
                  color = if (currentTab == NavigationTab.HOME) TextPrimary else TextSecondary
                )
              },
              colors = NavigationBarItemDefaults.colors(
                indicatorColor = CrimsonContainer
              ),
              modifier = Modifier.testTag("tab_home")
            )

            // Tab 2: Recent
            NavigationBarItem(
              selected = currentTab == NavigationTab.RECENT,
              onClick = { viewModel.setTab(NavigationTab.RECENT) },
              icon = {
                Icon(
                  imageVector = Icons.Default.Radar,
                  contentDescription = "Recent Tab",
                  tint = if (currentTab == NavigationTab.RECENT) CrimsonEmergencyGlow else TextSecondary
                )
              },
              label = {
                Text(
                  text = "Recent",
                  fontSize = 11.sp,
                  color = if (currentTab == NavigationTab.RECENT) TextPrimary else TextSecondary
                )
              },
              colors = NavigationBarItemDefaults.colors(
                indicatorColor = CrimsonContainer
              ),
              modifier = Modifier.testTag("tab_recent")
            )

            // Tab 3: Settings
            NavigationBarItem(
              selected = currentTab == NavigationTab.SETTINGS,
              onClick = { viewModel.setTab(NavigationTab.SETTINGS) },
              icon = {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = "Settings Tab",
                  tint = if (currentTab == NavigationTab.SETTINGS) CrimsonEmergencyGlow else TextSecondary
                )
              },
              label = {
                Text(
                  text = "Settings",
                  fontSize = 11.sp,
                  color = if (currentTab == NavigationTab.SETTINGS) TextPrimary else TextSecondary
                )
              },
              colors = NavigationBarItemDefaults.colors(
                indicatorColor = CrimsonContainer
              ),
              modifier = Modifier.testTag("tab_settings")
            )
          }
        }
      ) { innerPadding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          when (currentTab) {
            NavigationTab.HOME -> {
              HomeTabScreen(
                userProfile = userProfile,
                onProfileClick = { viewModel.setTab(NavigationTab.SETTINGS) },
                onSosTrigger = { viewModel.triggerSos() },
                onStartInstantRecording = { viewModel.startInstantVideoRecording() },
                onFindSaferRoute = { viewModel.openRouteModal() },
                onStartFakeCall = { isVid -> viewModel.startFakeCall(isVid) },
                onScheduleFakeCall = { delaySec, isVid, name, number, script ->
                  viewModel.scheduleFakeCall(delaySec, isVid, name, number, script)
                },
                fakeCallState = fakeCallState,
                onCancelScheduledFakeCall = { viewModel.cancelScheduledFakeCall() },
                verifiedContactsCount = verifiedContactsCount,
                onOpenInstallationGuide = { viewModel.setTab(NavigationTab.SETTINGS) }
              )
            }
            NavigationTab.RECENT -> {
              RecentTabScreen(
                incidents = incidents,
                selectedCategory = selectedIncidentCategory,
                maxDistanceKm = maxIncidentDistanceKm,
                onCategorySelected = { viewModel.setIncidentFilter(it) },
                onMaxDistanceChanged = { viewModel.setMaxDistanceFilter(it) }
              )
            }
            NavigationTab.SETTINGS -> {
              SettingsTabScreen(
                userProfile = userProfile,
                contacts = contacts,
                fakeMediaList = fakeMediaList,
                sosEvents = sosEvents,
                savedRecordings = savedRecordings,
                serverConfig = serverConfig,
                uploadTaskState = uploadTaskState,
                onUpdateProfile = { viewModel.updateProfile(it) },
                onAddContact = { name, phone, email, method -> viewModel.addContact(name, phone, email, method) },
                onDeleteContact = { viewModel.deleteContact(it) },
                onVerifyContact = { id, verified -> viewModel.verifyContact(id, verified) },
                onAddFakeMedia = { title, caller, isVid, script -> viewModel.addFakeMedia(title, caller, isVid, script) },
                onDeleteFakeMedia = { viewModel.deleteFakeMedia(it) },
                onSetDefaultFakeMedia = { id, isVid -> viewModel.setDefaultFakeMedia(id, isVid) },
                onPlayVideo = { video -> viewModel.playRecording(context, video) },
                onShareVideo = { video -> viewModel.shareRecording(context, video) },
                onUploadFileToServer = { video -> viewModel.uploadFileToServer(video) },
                onUpdateServerConfig = { cfg -> viewModel.updateServerConfig(cfg) },
                onSwitchGoogleAccount = {
                  if (activity != null) {
                    viewModel.signInWithGoogle(activity)
                  } else {
                    viewModel.signInWithGoogleDirect()
                  }
                },
                onSignOut = { viewModel.signOut() },
                onDeleteAllData = { viewModel.deleteAllData() }
              )
            }
          }
        }
      }
    }

    // Full-screen Emergency SOS Overlay (Uses Back Camera by default)
    if (sosUiState.isEmergencyOverlayVisible) {
      FullscreenSosOverlay(
        sosState = sosUiState,
        liveTrackingState = liveTrackingState,
        contactStatuses = contactDispatchStatuses,
        onCancelSos = { viewModel.cancelSos() },
        onDismiss = { viewModel.dismissSosOverlay() },
        onToggleCamera = { viewModel.toggleCameraLens() },
        onPlaySavedVideo = {
          val latest = savedRecordings.firstOrNull()
          if (latest != null) {
            viewModel.playRecording(context, latest)
          }
        },
        onShareSavedVideo = {
          val latest = savedRecordings.firstOrNull()
          if (latest != null) {
            viewModel.shareRecording(context, latest)
          }
        },
        onToggleContactStream = { contactId ->
          viewModel.toggleContactLocationStream(contactId)
        },
        onOpenGoogleMaps = { lat, lng ->
          viewModel.openInGoogleMaps(lat, lng)
        },
        onShareLiveLocation = {
          viewModel.shareLiveLocationBroadcast(context)
        },
        onSendSmsToContact = { contactStatus ->
          val targetContact = contacts.firstOrNull { it.id == contactStatus.contactId }
          if (targetContact != null) {
            viewModel.sendLocationSmsToContact(context, targetContact)
          }
        },
        onPermissionsGranted = { viewModel.startInstantVideoRecording() },
        onRecordingPathLinked = { path -> viewModel.updateLocalVideoFilePath(path) }
      )
    }

    // Full-screen Fake Call Simulation Modal
    if (fakeCallState.isActive) {
      FullscreenFakeCallModal(
        state = fakeCallState,
        onEndCall = { viewModel.endFakeCall() },
        onAnswerCall = { viewModel.answerFakeCall() },
        onSilenceRingtone = { viewModel.silenceFakeCallRingtone() },
        onToggleMute = { viewModel.toggleFakeCallMute() },
        onToggleSpeaker = { viewModel.toggleFakeCallSpeaker() },
        onTogglePause = { viewModel.toggleFakeCallPause() }
      )
    }

    // Safer Route Bottom Sheet Modal
    if (isRouteModalVisible) {
      RouteSafetyDialog(
        destination = routeDestination,
        onDestinationChange = { viewModel.setRouteDestination(it) },
        onSearch = { viewModel.searchSaferRoutes(it) },
        onDismiss = { viewModel.closeRouteModal() },
        isAnalyzing = isAnalyzingRoutes,
        routeAnalyses = routeAnalyses
      )
    }
  }
}
