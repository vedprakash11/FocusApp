package com.ved.focusapp.data

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SharedPreferences wrapper for config, timer state, and stats.
 * Keys match the spec for parity with the Flutter version.
 */
class PreferencesStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ---------- Config ----------
    var focusMinutes: Int
        get() = prefs.getInt(KEY_FOCUS_MINUTES, DEFAULT_FOCUS_MINUTES).coerceIn(1, 60)
        set(value) = prefs.edit().putInt(KEY_FOCUS_MINUTES, value.coerceIn(1, 60)).apply()

    var shortBreakMinutes: Int
        get() = prefs.getInt(KEY_SHORT_BREAK_MINUTES, DEFAULT_SHORT_BREAK_MINUTES).coerceIn(1, 30)
        set(value) = prefs.edit().putInt(KEY_SHORT_BREAK_MINUTES, value.coerceIn(1, 30)).apply()

    var longBreakMinutes: Int
        get() = prefs.getInt(KEY_LONG_BREAK_MINUTES, DEFAULT_LONG_BREAK_MINUTES).coerceIn(1, 60)
        set(value) = prefs.edit().putInt(KEY_LONG_BREAK_MINUTES, value.coerceIn(1, 60)).apply()

    var autoStartNext: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START_NEXT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START_NEXT, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    // ---------- Timer state ----------
    var timerPhase: TimerPhase
        get() = TimerPhase.fromKey(prefs.getString(KEY_TIMER_PHASE, null))
        set(value) = prefs.edit().putString(KEY_TIMER_PHASE, value.key).apply()

    var timerEndTimeMillis: Long
        get() = prefs.getLong(KEY_TIMER_END_TIME_MILLIS, 0L)
        set(value) = prefs.edit().putLong(KEY_TIMER_END_TIME_MILLIS, value).apply()

    var timerWasRunning: Boolean
        get() = prefs.getBoolean(KEY_TIMER_WAS_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_TIMER_WAS_RUNNING, value).apply()

    fun clearTimerState() {
        prefs.edit()
            .remove(KEY_TIMER_PHASE)
            .remove(KEY_TIMER_END_TIME_MILLIS)
            .remove(KEY_TIMER_WAS_RUNNING)
            .apply()
    }

    fun persistTimerState(phase: TimerPhase, endTimeMillis: Long, wasRunning: Boolean) {
        prefs.edit()
            .putString(KEY_TIMER_PHASE, phase.key)
            .putLong(KEY_TIMER_END_TIME_MILLIS, endTimeMillis)
            .putBoolean(KEY_TIMER_WAS_RUNNING, wasRunning)
            .apply()
    }

    // ---------- Stats ----------
    var totalSessions: Int
        get() = prefs.getInt(KEY_TOTAL_SESSIONS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_SESSIONS, value).apply()

    var lastCompletionDate: String?
        get() = prefs.getString(KEY_LAST_COMPLETION_DATE, null)
        set(value) = if (value != null) prefs.edit().putString(KEY_LAST_COMPLETION_DATE, value).apply() else Unit

    fun getDailyMinutes(dateKey: String): Int = prefs.getInt(KEY_DAILY_MINUTES_PREFIX + dateKey, 0)

    fun addDailyMinutes(dateKey: String, minutes: Int) {
        val current = getDailyMinutes(dateKey)
        prefs.edit().putInt(KEY_DAILY_MINUTES_PREFIX + dateKey, current + minutes).apply()
    }

    /** 0–4: focus sessions completed in current round. After 4, next break is long. */
    fun getSessionsThisRound(): Int = prefs.getInt(KEY_SESSIONS_THIS_ROUND, 0).coerceIn(0, 4)

    fun incrementSessionsThisRound() {
        val current = getSessionsThisRound()
        if (current < 4) prefs.edit().putInt(KEY_SESSIONS_THIS_ROUND, current + 1).apply()
    }

    fun resetSessionsThisRound() {
        prefs.edit().putInt(KEY_SESSIONS_THIS_ROUND, 0).apply()
    }

    fun todayKey(): String = dateFormat.format(Date())

    // ---------- Keys ----------
    companion object {
        private const val PREFS_NAME = "focus_timer_prefs"

        private const val KEY_FOCUS_MINUTES = "focus_minutes"
        private const val KEY_SHORT_BREAK_MINUTES = "short_break_minutes"
        private const val KEY_LONG_BREAK_MINUTES = "long_break_minutes"
        private const val KEY_AUTO_START_NEXT = "auto_start_next"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

        private const val KEY_TIMER_PHASE = "timer_phase"
        private const val KEY_TIMER_END_TIME_MILLIS = "timer_end_time_millis"
        private const val KEY_TIMER_WAS_RUNNING = "timer_was_running"

        private const val KEY_TOTAL_SESSIONS = "total_sessions"
        private const val KEY_LAST_COMPLETION_DATE = "last_completion_date"
        private const val KEY_DAILY_MINUTES_PREFIX = "daily_minutes_"
        private const val KEY_SESSIONS_THIS_ROUND = "sessions_this_round"

        private const val DEFAULT_FOCUS_MINUTES = 25
        private const val DEFAULT_SHORT_BREAK_MINUTES = 5
        private const val DEFAULT_LONG_BREAK_MINUTES = 15
    }
}
