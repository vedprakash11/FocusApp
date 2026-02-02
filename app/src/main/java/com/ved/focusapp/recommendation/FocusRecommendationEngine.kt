package com.ved.focusapp.recommendation

import com.ved.focusapp.data.SessionRecord
import kotlin.math.roundToInt
import kotlin.math.sqrt
import java.util.Locale

/**
 * Smart (rule-based) focus and break recommendations from recent session data.
 * All logic is on-device, explainable, no ML libraries.
 * Wording is "based on your usage patterns" (Play Store safe); no AI/ML/prediction claims.
 */

data class FocusRecommendation(
    val recommendedFocusMinutes: Int,
    val recommendedShortBreakMinutes: Int?,
    val message: String,
    val completionRatePercent: Int?,
    val rollingAvgMinutes: Double?
)

/** Internal only: used to adjust default focus/break values, not for UI labeling. */
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
    // Formula refines rule-based result: delta = a * (completion_rate - 0.5) * b (scaled so completion_rate meaningfully affects minutes)
    private const val FORMULA_A = 0.6
    private const val FORMULA_B = 10.0
    // User classification: high variance of session durations -> Inconsistent
    private const val HIGH_VARIANCE_SD_MINUTES = 12.0

    // Break adaptation (rule-based only, no formula)
    private const val MIN_SHORT_BREAK = 5
    private const val MAX_SHORT_BREAK = 10
    private const val BREAK_SKIP_RATE_THRESHOLD = 0.40
    private const val BREAK_INCREASE_MINUTES = 2
    private const val BREAK_DECREASE_MINUTES = 1
    private const val RESUME_LATE_RATE_THRESHOLD = 0.50

    /**
     * Compute focus recommendation from last N sessions.
     * Rule-based result first; then formula refines (does not replace): recommended = rule_based + round(a*(completion_rate - 0.5)*b).
     * Final value clamped to [15, 45] and rounded to nearest integer. Formula must NOT override min/max limits.
     */
    fun recommend(
        recentSessions: List<SessionRecord>,
        currentFocusMinutes: Int,
        breakSkipRate: Float? = null,
        resumeLateRate: Float? = null,
        currentShortBreakMinutes: Int = 5
    ): FocusRecommendation {
        val window = recentSessions.takeLast(ROLLING_WINDOW)
        if (window.isEmpty()) {
            val breakRec = recommendShortBreak(breakSkipRate, resumeLateRate, currentShortBreakMinutes)
            return FocusRecommendation(
                recommendedFocusMinutes = currentFocusMinutes,
                recommendedShortBreakMinutes = breakRec,
                message = "",
                completionRatePercent = null,
                rollingAvgMinutes = null
            )
        }

        val total = window.size
        val completed = window.count { it.completed }
        val completionRate = completed.toDouble() / total
        val completionRatePercent = (completionRate * 100).toInt()
        val durations = window.map { it.durationMinutes.toDouble() }
        val rollingAvg = durations.average()
        val earlyStopsInWindow = window.count { !it.completed }

        // --- Rule-based focus (must stay within min/max; formula will refine, not replace) ---
        var ruleBased = currentFocusMinutes.coerceIn(MIN_FOCUS, MAX_FOCUS)
        if (completionRate >= HIGH_COMPLETION_THRESHOLD) {
            ruleBased = (ruleBased + STEP_MINUTES).coerceAtMost(MAX_FOCUS)
        } else if (completionRate <= LOW_COMPLETION_THRESHOLD || earlyStopsInWindow >= EARLY_STOP_COUNT_TO_REDUCE) {
            ruleBased = (ruleBased - STEP_MINUTES).coerceAtLeast(MIN_FOCUS)
        }

        // --- Formula refinement: delta = a * (completion_rate - 0.5) * b; round to int ---
        val formulaDelta = (FORMULA_A * (completionRate - 0.5) * FORMULA_B).roundToInt()
        val recommended = (ruleBased + formulaDelta).coerceIn(MIN_FOCUS, MAX_FOCUS)

        val userType = classify(rollingAvg, completionRate, durations)
        val message = buildMessage(recommended, userType)
        val breakRec = recommendShortBreak(breakSkipRate, resumeLateRate, currentShortBreakMinutes)

        return FocusRecommendation(
            recommendedFocusMinutes = recommended,
            recommendedShortBreakMinutes = breakRec,
            message = message,
            completionRatePercent = completionRatePercent,
            rollingAvgMinutes = rollingAvg
        )
    }

    /**
     * Rule-based break adaptation only (no formula).
     * If user skips >= 40% of breaks -> increase break by +2 min (max 10).
     * If user resumes late frequently -> reduce break by -1 min (min 5).
     */
    private fun recommendShortBreak(
        breakSkipRate: Float?,
        resumeLateRate: Float?,
        currentShortBreakMinutes: Int
    ): Int? {
        if (breakSkipRate == null && resumeLateRate == null) return null
        var rec = currentShortBreakMinutes.coerceIn(MIN_SHORT_BREAK, MAX_SHORT_BREAK)
        if (breakSkipRate != null && breakSkipRate >= BREAK_SKIP_RATE_THRESHOLD) {
            rec = (rec + BREAK_INCREASE_MINUTES).coerceAtMost(MAX_SHORT_BREAK)
        }
        if (resumeLateRate != null && resumeLateRate >= RESUME_LATE_RATE_THRESHOLD) {
            rec = (rec - BREAK_DECREASE_MINUTES).coerceAtLeast(MIN_SHORT_BREAK)
        }
        return rec
    }

    /** Internal classification: Short-focus (avg < 20), Deep-focus (avg >= 30 and completion >= 0.8), Inconsistent (high variance or low completion). */
    private fun classify(rollingAvg: Double, completionRate: Double, durations: List<Double>): FocusUserType {
        val variance = if (durations.size < 2) 0.0 else {
            val mean = durations.average()
            durations.map { (it - mean) * (it - mean) }.average()
        }
        val sd = sqrt(variance)
        return when {
            sd >= HIGH_VARIANCE_SD_MINUTES || completionRate < LOW_COMPLETION_THRESHOLD -> FocusUserType.INCONSISTENT
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
