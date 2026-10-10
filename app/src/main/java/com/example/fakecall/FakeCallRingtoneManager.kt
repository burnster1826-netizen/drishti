package com.example.fakecall

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class FakeCallRingtoneManager(
  private val context: Context
) {
  companion object {
    private const val TAG = "FakeCallRingtoneManager"
  }

  private var ringtone: Ringtone? = null
  private var isRinging = false

  private val vibrator: Vibrator? by lazy {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      vibratorManager?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  /**
   * Starts playing the real Android system ringtone and vibrating the phone.
   */
  fun startRingtoneAndVibration() {
    if (isRinging) return
    isRinging = true

    try {
      // 1. Fetch system default ringtone
      val ringtoneUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(
        context,
        RingtoneManager.TYPE_RINGTONE
      ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

      ringtone = RingtoneManager.getRingtone(context, ringtoneUri)?.apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        }
        play()
      }
      Log.i(TAG, "Fake call ringtone playback started")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to play default system ringtone", e)
    }

    // 2. Start repeating phone vibration pattern [0ms pause, 1000ms vibrate, 1000ms pause]
    try {
      vibrator?.let { v ->
        if (v.hasVibrator()) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 1000, 1000)
            val amplitudes = intArrayOf(0, 255, 0)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
            v.vibrate(effect)
          } else {
            @Suppress("DEPRECATION")
            v.vibrate(longArrayOf(0, 1000, 1000), 0)
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start phone vibration", e)
    }
  }

  /**
   * Immediately stops ringtone playback and vibration.
   */
  fun stopRingtoneAndVibration() {
    isRinging = false
    try {
      ringtone?.let {
        if (it.isPlaying) {
          it.stop()
        }
      }
      ringtone = null
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping ringtone", e)
    }

    try {
      vibrator?.cancel()
    } catch (e: Exception) {
      Log.e(TAG, "Error cancelling vibration", e)
    }
  }
}
