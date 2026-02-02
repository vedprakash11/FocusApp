package com.ved.focusapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ved.focusapp.MainActivity
import com.ved.focusapp.R

/**
 * One channel for Focus Timer. Shows "Focus ended" and "Break ended" notifications.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.notification_channel_desc) }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showFocusEnded() {
        showNotification(
            id = NOTIFICATION_ID_FOCUS_ENDED,
            title = context.getString(R.string.notification_focus_ended_title),
            body = context.getString(R.string.notification_focus_ended_body)
        )
    }

    fun showBreakEnded() {
        showNotification(
            id = NOTIFICATION_ID_BREAK_ENDED,
            title = context.getString(R.string.notification_break_ended_title),
            body = context.getString(R.string.notification_break_ended_body)
        )
    }

    private fun showNotification(id: Int, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            notificationManager.notify(id, notification)
        } catch (_: SecurityException) { /* no permission */ }
    }

    companion object {
        const val CHANNEL_ID = "focustimer_channel"
        private const val NOTIFICATION_ID_FOCUS_ENDED = 1
        private const val NOTIFICATION_ID_BREAK_ENDED = 2
    }
}
