package com.ved.focusapp.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules a one-shot alarm at [endTimeMillis] so completion (notification, stats, sound) runs
 * when the timer ends even if the app is in background.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleCompletionAt(endTimeMillis: Long) {
        val intent = Intent(context, TimerCompletionReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pending)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTimeMillis, pending)
            }
        } catch (_: SecurityException) { }
    }

    fun cancelCompletion() {
        val intent = Intent(context, TimerCompletionReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
