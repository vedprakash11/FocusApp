package com.ved.focusapp.timer

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ved.focusapp.data.PreferencesStorage
import com.ved.focusapp.data.SessionRecord
import com.ved.focusapp.data.TimerPhase
import com.ved.focusapp.dnd.DndHelper
import com.ved.focusapp.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Central timer state: phase, remaining seconds, running/paused, sessions this round.
 * UI reads state and calls start/pause/resume/reset. Persists end time; on resume restores and does not auto-start tick.
 */
class TimerStateHolder(
    private val context: Context,
    private val storage: PreferencesStorage,
    private val engine: TimerEngine,
    private val notificationHelper: NotificationHelper,
    private val dndHelper: DndHelper,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _phase = MutableStateFlow(TimerPhase.Idle)
    val phase: StateFlow<TimerPhase> = _phase.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** 1–4 focus sessions completed in current round; after 4, next break is long. */
    private val _sessionsThisRound = MutableStateFlow(0)
    val sessionsThisRound: StateFlow<Int> = _sessionsThisRound.asStateFlow()

    init {
        viewModelScope.launch {
            restoreFromStorage(triggerCompletionIfExpired = false)
        }
    }

    /**
     * Restore state from SharedPreferences. If remaining <= 0, run session-end logic (stats, notification, sound, auto-start or Idle).
     * If remaining > 0, show paused (do not start tick).
     */
    fun restoreFromStorage(triggerCompletionIfExpired: Boolean) {
        val p = storage.timerPhase
        val endTime = storage.timerEndTimeMillis
        val wasRunning = storage.timerWasRunning

        if (p == TimerPhase.Idle || endTime <= 0) {
            _phase.value = TimerPhase.Idle
            _remainingSeconds.value = 0
            _isRunning.value = false
            _sessionsThisRound.value = storage.getSessionsThisRound()
            return
        }

        val remainingMs = engine.remainingMs(endTime)
        if (remainingMs <= 0 && triggerCompletionIfExpired) {
            handleSessionComplete(p, endTime, fromRestore = true)
            return
        }

        _phase.value = p
        _remainingSeconds.value = (remainingMs / 1000).toInt().coerceAtLeast(0)
        _isRunning.value = false
        _sessionsThisRound.value = storage.getSessionsThisRound()
        engine.stopTicking()
    }

    private fun handleSessionComplete(completedPhase: TimerPhase, endTimeMillis: Long, fromRestore: Boolean) {
        engine.stopTicking()
        alarmScheduler.cancelCompletion()
        if (completedPhase.isFocus) {
            recordFocusCompletion(endTimeMillis)
            dndHelper.restoreDndOnFocusEnd()
        } else if (completedPhase == TimerPhase.ShortBreak || completedPhase == TimerPhase.LongBreak) {
            storage.addBreakOutcome(false)
            storage.breakEndedAtMillis = System.currentTimeMillis()
        }
        playSessionEndFeedback()
        when (completedPhase) {
            TimerPhase.Focus -> notificationHelper.showFocusEnded()
            TimerPhase.ShortBreak, TimerPhase.LongBreak -> notificationHelper.showBreakEnded()
            else -> { }
        }
        storage.clearTimerState()
        _phase.value = TimerPhase.Idle
        _remainingSeconds.value = 0
        _isRunning.value = false
        val sessions = storage.getSessionsThisRound()

        if (storage.autoStartNext) {
            when (completedPhase) {
                TimerPhase.Focus -> startPhase(if (sessions >= 4) TimerPhase.LongBreak else TimerPhase.ShortBreak)
                TimerPhase.ShortBreak, TimerPhase.LongBreak -> {
                    if (completedPhase == TimerPhase.LongBreak) storage.resetSessionsThisRound()
                    startPhase(TimerPhase.Focus)
                }
                else -> { }
            }
        } else {
            if (completedPhase == TimerPhase.LongBreak) storage.resetSessionsThisRound()
            _sessionsThisRound.value = storage.getSessionsThisRound()
        }
    }

    private fun recordFocusCompletion(endTimeMillis: Long) {
        val durationMs = getPhaseDurationMs(TimerPhase.Focus)
        val durationMinutes = (durationMs / 60000).toInt().coerceAtLeast(1)
        storage.totalSessions = storage.totalSessions + 1
        val today = storage.todayKey()
        storage.addDailyMinutes(today, durationMinutes)
        storage.lastCompletionDate = today
        storage.incrementSessionsThisRound()
        // For smart recommendations: completed focus session
        storage.addFocusSessionRecord(
            SessionRecord(
                timestampMillis = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                completed = true
            )
        )
    }

    private fun getPhaseDurationMs(phase: TimerPhase): Long {
        val min = when (phase) {
            TimerPhase.Focus -> storage.focusMinutes
            TimerPhase.ShortBreak -> storage.shortBreakMinutes
            TimerPhase.LongBreak -> storage.longBreakMinutes
            else -> 0
        }
        return min * 60_000L
    }

    private fun playSessionEndFeedback() {
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

    fun startPhase(targetPhase: TimerPhase) {
        if (targetPhase == TimerPhase.Idle) return
        if (targetPhase == TimerPhase.Focus) {
            val breakEndedAt = storage.breakEndedAtMillis
            if (breakEndedAt > 0) {
                val elapsed = System.currentTimeMillis() - breakEndedAt
                if (elapsed <= 24 * 60 * 60 * 1000L) {
                    storage.addResumeLateOutcome(elapsed > 60_000)
                }
                storage.breakEndedAtMillis = 0
            }
            dndHelper.enableDndOnFocusStart()
        }
        val durationMs = when (targetPhase) {
            TimerPhase.Focus -> storage.focusMinutes * 60_000L
            TimerPhase.ShortBreak -> storage.shortBreakMinutes * 60_000L
            TimerPhase.LongBreak -> storage.longBreakMinutes * 60_000L
            else -> return
        }
        val endTime = System.currentTimeMillis() + durationMs
        storage.persistTimerState(targetPhase, endTime, true)
        alarmScheduler.scheduleCompletionAt(endTime)
        _phase.value = targetPhase
        _remainingSeconds.value = (durationMs / 1000).toInt()
        _isRunning.value = true
        _sessionsThisRound.value = storage.getSessionsThisRound()

        engine.startTicking(
            endTimeMillis = endTime,
            onTick = { remainingMs ->
                _remainingSeconds.update { (remainingMs / 1000).toInt().coerceAtLeast(0) }
                storage.timerEndTimeMillis = endTime
            },
            onComplete = {
                handleSessionComplete(targetPhase, endTime, fromRestore = false)
            }
        )
    }

    fun pause() {
        val p = _phase.value
        val endTime = storage.timerEndTimeMillis
        if (p == TimerPhase.Idle || endTime <= 0) return
        engine.stopTicking()
        alarmScheduler.cancelCompletion()
        if (p.isFocus) dndHelper.restoreDndOnFocusEnd()
        storage.persistTimerState(p, endTime, false)
        _isRunning.value = false
        val remainingMs = engine.remainingMs(endTime)
        _remainingSeconds.value = (remainingMs / 1000).toInt().coerceAtLeast(0)
    }

    fun resume() {
        val p = _phase.value
        val endTime = storage.timerEndTimeMillis
        if (p == TimerPhase.Idle || endTime <= 0) return
        val remainingMs = engine.remainingMs(endTime)
        if (remainingMs <= 0) {
            handleSessionComplete(p, endTime, fromRestore = true)
            return
        }
        if (p.isFocus) dndHelper.enableDndOnFocusStart()
        storage.timerWasRunning = true
        _isRunning.value = true
        alarmScheduler.scheduleCompletionAt(endTime)
        engine.startTicking(
            endTimeMillis = endTime,
            onTick = { ms ->
                _remainingSeconds.update { (ms / 1000).toInt().coerceAtLeast(0) }
                storage.timerEndTimeMillis = endTime
            },
            onComplete = { handleSessionComplete(p, endTime, fromRestore = false) }
        )
    }

    fun reset() {
        val p = _phase.value
        val endTime = storage.timerEndTimeMillis
        engine.stopTicking()
        alarmScheduler.cancelCompletion()
        if (p.isFocus) {
            dndHelper.restoreDndOnFocusEnd()
            val durationMs = getPhaseDurationMs(TimerPhase.Focus)
            val startTime = endTime - durationMs
            val elapsedMinutes = ((System.currentTimeMillis() - startTime) / 60_000).toInt().coerceIn(0, 60)
            storage.addFocusSessionRecord(
                SessionRecord(
                    timestampMillis = System.currentTimeMillis(),
                    durationMinutes = elapsedMinutes,
                    completed = false
                )
            )
        } else if (p == TimerPhase.ShortBreak || p == TimerPhase.LongBreak) {
            storage.addBreakOutcome(true)
        }
        storage.clearTimerState()
        _phase.value = TimerPhase.Idle
        _remainingSeconds.value = 0
        _isRunning.value = false
        _sessionsThisRound.value = storage.getSessionsThisRound()
    }

    /**
     * Lifecycle: onPause — stop tick only; state (phase, endTime, wasRunning) stays persisted.
     * Timer survives backgrounding; remaining time is recalculated from endTime on resume.
     */
    fun onAppPause() {
        if (_isRunning.value) {
            engine.stopTicking()
            storage.timerWasRunning = true
        }
    }

    /**
     * Lifecycle: onResume — restore from storage; recalculate remaining from endTime.
     * If remaining <= 0, run session-end logic; if remaining > 0, show paused (user taps Resume).
     */
    fun onAppResume() {
        restoreFromStorage(triggerCompletionIfExpired = true)
    }

    /** Format remaining as MM:SS */
    fun formatRemaining(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    fun getFocusMinutes(): Int = storage.focusMinutes
    fun getShortBreakMinutes(): Int = storage.shortBreakMinutes
    fun getLongBreakMinutes(): Int = storage.longBreakMinutes

    class Factory(
        private val context: Context,
        private val storage: PreferencesStorage,
        private val engine: TimerEngine,
        private val notificationHelper: NotificationHelper,
        private val dndHelper: DndHelper,
        private val alarmScheduler: AlarmScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TimerStateHolder(context, storage, engine, notificationHelper, dndHelper, alarmScheduler) as T
        }
    }
}
