package com.ved.focusapp.recommendation

import com.ved.focusapp.data.SessionRecord
import java.util.Locale

/**
 * Smart (rule-based) focus recommendations from recent session data.
 * All logic is on-device, explainable, no ML libraries.
 * Used only to suggest duration; wording is "based on your usage" (Play Store safe).
 */

data class FocusRecommendation(
    val recommendedFocusMinutes: Int,
    val message: String,
    val completionRatePercent: Int?,
    val rollingAvgMinutes: Double?
)

/** Simple user type from rules; used only to adjust suggested default, not shown as a label. */
enum class FocusUserType { SHORT_FOCUS, DEEP_FOCUS, INCONSISTENT }

object FocusRecommendationEngine {

    private const val MIN_FOCUS = 15
    private const val MAX_FOCUS = 45
    private const val STEP_MINUTES = 5
    private const val ROLLING_WINDOW = 7
    private const val HIGH_COMPLETION_THRESHOLD = 0.80
    private const val LOW_COMPLETION_THRESHOLD = 0.50
    private const val EARLY_STOP_COUNT_TO_REDUCE = 3
    private const val SHORT_AVG_MAX = 20
    private const val DEEP_AVG_MIN = 30

    /**
     * Compute recommendation from last N sessions.
     * - completion_rate = completed_sessions / total_sessions
     * - rolling_avg = average duration over last 7 sessions
     * - If completion_rate >= 80% -> increase focus by +5 (max 45)
     * - If completion_rate <= 50% OR user stopped early 3+ times in window -> reduce by -5 (min 15)
     * - Otherwise -> keep current
     */
    fun recommend(
        recentSessions: List<SessionRecord>,
        currentFocusMinutes: Int
    ): FocusRecommendation {
        val window = recentSessions.takeLast(ROLLING_WINDOW)
        if (window.isEmpty()) {
            return FocusRecommendation(
                recommendedFocusMinutes = currentFocusMinutes,
                message = "", // No data yet
                completionRatePercent = null,
                rollingAvgMinutes = null
            )
        }

        val total = window.size
        val completed = window.count { it.completed }
        val completionRate = completed.toDouble() / total
        val completionRatePercent = (completionRate * 100).toInt()
        val rollingAvg = window.map { it.durationMinutes }.average()
        val earlyStopsInWindow = window.count { !it.completed }

        var recommended = currentFocusMinutes.coerceIn(MIN_FOCUS, MAX_FOCUS)

        // Rule: high completion -> increase (max 45)
        if (completionRate >= HIGH_COMPLETION_THRESHOLD) {
            recommended = (recommended + STEP_MINUTES).coerceAtMost(MAX_FOCUS)
        }
        // Rule: low completion or 3+ early stops -> decrease (min 15)
        else if (completionRate <= LOW_COMPLETION_THRESHOLD || earlyStopsInWindow >= EARLY_STOP_COUNT_TO_REDUCE) {
            recommended = (recommended - STEP_MINUTES).coerceAtLeast(MIN_FOCUS)
        }

        val userType = classify(rollingAvg, completionRate)
        val message = buildMessage(recommended, userType)

        return FocusRecommendation(
            recommendedFocusMinutes = recommended,
            message = message,
            completionRatePercent = completionRatePercent,
            rollingAvgMinutes = rollingAvg
        )
    }

    /** Simple rule-based classification; used only to tailor message, not shown as "AI" label. */
    private fun classify(rollingAvg: Double, completionRate: Double): FocusUserType {
        return when {
            rollingAvg < SHORT_AVG_MAX -> FocusUserType.SHORT_FOCUS
            rollingAvg >= DEEP_AVG_MIN && completionRate >= HIGH_COMPLETION_THRESHOLD -> FocusUserType.DEEP_FOCUS
            else -> FocusUserType.INCONSISTENT
        }
    }

    private fun buildMessage(recommendedMinutes: Int, userType: FocusUserType): String {
        return String.format(
            Locale.US,
            "Based on your recent sessions, %d minutes works best for you.",
            recommendedMinutes
        )
    }
}
