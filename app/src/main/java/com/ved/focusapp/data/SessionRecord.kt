package com.ved.focusapp.data

/**
 * Single focus session record for smart recommendations.
 * Stored locally only; used for completion rate and rolling average.
 */
data class SessionRecord(
    val timestampMillis: Long,
    val durationMinutes: Int,
    val completed: Boolean
)
