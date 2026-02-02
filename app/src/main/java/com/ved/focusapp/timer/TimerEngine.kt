package com.ved.focusapp.timer

import android.os.Handler
import android.os.Looper

/**
 * Timer engine using end-time (absolute timestamp). No wake locks or foreground service.
 * Computes remaining = endTime - now; ticks every second when running; invokes completion when remaining <= 0.
 */
class TimerEngine {

    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null

    /**
     * Compute remaining milliseconds from end time. Returns 0 if already past.
     */
    fun remainingMs(endTimeMillis: Long): Long {
        val now = System.currentTimeMillis()
        return (endTimeMillis - now).coerceAtLeast(0L)
    }

    /**
     * Start ticking: every second call [onTick] with remaining ms. When remaining <= 0, call [onComplete] and stop.
     */
    fun startTicking(
        endTimeMillis: Long,
        onTick: (remainingMs: Long) -> Unit,
        onComplete: () -> Unit
    ) {
        stopTicking()
        tickRunnable = object : Runnable {
            override fun run() {
                val remaining = remainingMs(endTimeMillis)
                if (remaining <= 0) {
                    stopTicking()
                    onComplete()
                    return
                }
                onTick(remaining)
                handler.postDelayed(this, 1000L)
            }
        }
        // First tick immediately
        handler.post(tickRunnable!!)
    }

    fun stopTicking() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
    }

    fun isTicking(): Boolean = tickRunnable != null
}
