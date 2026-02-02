package com.ved.focusapp.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ved.focusapp.data.PreferencesStorage
import com.ved.focusapp.data.SessionRecord
import com.ved.focusapp.data.TimerPhase
import com.ved.focusapp.dnd.DndHelper
import com.ved.focusapp.notification.NotificationHelper

/**
 * Runs when the timer end-time alarm fires (e.g. app in background).
 * Updates stats if focus, shows notification, sound/vibration, clears state.
 */
class TimerCompletionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val storage = PreferencesStorage(context)
        val phase = storage.timerPhase
        val endTime = storage.timerEndTimeMillis
        if (phase == TimerPhase.Idle || endTime <= 0) return
        val now = System.currentTimeMillis()
        if (now < endTime) return

        val notificationHelper = NotificationHelper(context)
        val dndHelper = DndHelper(context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)

        if (phase.isFocus) {
            recordFocusCompletion(storage, phase)
            dndHelper.restoreDndOnFocusEnd()
        }
        playSessionEndFeedback(context, storage)
        when (phase) {
            TimerPhase.Focus -> notificationHelper.showFocusEnded()
            TimerPhase.ShortBreak, TimerPhase.LongBreak -> notificationHelper.showBreakEnded()
            else -> { }
        }
        storage.clearTimerState()
    }

    private fun recordFocusCompletion(storage: PreferencesStorage, phase: TimerPhase) {
        storage.totalSessions = storage.totalSessions + 1
        val today = storage.todayKey()
        val durationMinutes = when (phase) {
            TimerPhase.Focus -> storage.focusMinutes
            else -> 25
        }
        storage.addDailyMinutes(today, durationMinutes)
        storage.lastCompletionDate = today
        storage.incrementSessionsThisRound()
        storage.addFocusSessionRecord(
            SessionRecord(
                timestampMillis = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                completed = true
            )
        )
    }

    private fun playSessionEndFeedback(context: Context, storage: PreferencesStorage) {
        if (storage.soundEnabled) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone.play()
                android.os.Handler(context.mainLooper).postDelayed({ ringtone.stop() }, 2000)
            } catch (_: Exception) { }
        }
        if (storage.vibrationEnabled) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(300)
                }
            } catch (_: Exception) { }
        }
    }
}
