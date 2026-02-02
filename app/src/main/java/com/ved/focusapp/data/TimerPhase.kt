package com.ved.focusapp.data

/**
 * Pomodoro timer phases. Idle = no active session; others are active phases.
 */
enum class TimerPhase(val key: String) {
    Idle("idle"),
    Focus("focus"),
    ShortBreak("shortBreak"),
    LongBreak("longBreak");

    companion object {
        fun fromKey(key: String?): TimerPhase = entries.find { it.key == key } ?: Idle
    }

    val isBreak: Boolean get() = this == ShortBreak || this == LongBreak
    val isFocus: Boolean get() = this == Focus
}
