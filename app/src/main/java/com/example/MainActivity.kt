package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.screens.DrishtiApp
import com.example.ui.theme.DrishtiTheme
import com.example.widget.SosWidgetProvider

class MainActivity : ComponentActivity() {

  private var triggerSosState by mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (intent?.getBooleanExtra(SosWidgetProvider.EXTRA_TRIGGER_SOS, false) == true) {
      triggerSosState = true
    }

    setContent {
      DrishtiTheme {
        DrishtiApp(
          triggerSosOnInit = triggerSosState,
          onSosTriggerHandled = { triggerSosState = false }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra(SosWidgetProvider.EXTRA_TRIGGER_SOS, false)) {
      triggerSosState = true
    }
  }
}

