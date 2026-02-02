package com.ved.focusapp.dnd

import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * Do Not Disturb: enable on focus start, restore on focus end/pause/reset.
 * When focus is on: no notifications, no call popups (full DND).
 * Only focus phase toggles DND. If notification policy access not granted, no-op.
 */
class DndHelper(private val notificationManager: NotificationManager) {

    private var savedInterruptionFilter: Int = -1
    private var savedNotificationPolicy: NotificationManager.Policy? = null

    /**
     * Enable DND when focus starts. Saves current filter and policy; sets full silence (no notifications, no calls).
     */
    fun enableDndOnFocusStart() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!hasNotificationPolicyAccess()) return
        saveCurrentAndSetDnd()
    }

    /**
     * Restore previous DND state when focus ends, or user pauses/resets during focus.
     */
    fun restoreDndOnFocusEnd() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        if (!hasNotificationPolicyAccess()) return
        restoreSaved()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasNotificationPolicyAccess(): Boolean {
        return try {
            notificationManager.isNotificationPolicyAccessGranted
        } catch (_: SecurityException) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun saveCurrentAndSetDnd() {
        try {
            savedInterruptionFilter = notificationManager.currentInterruptionFilter
            savedNotificationPolicy = notificationManager.notificationPolicy
            // Strict policy: no priority categories, no call senders, no message senders (0 = none)
            val strictPolicy = NotificationManager.Policy(0, 0, 0)
            notificationManager.setNotificationPolicy(strictPolicy)
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        } catch (_: SecurityException) { /* no-op */ }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun restoreSaved() {
        try {
            val filter = if (savedInterruptionFilter >= 0) savedInterruptionFilter
            else NotificationManager.INTERRUPTION_FILTER_ALL
            savedNotificationPolicy?.let { notificationManager.setNotificationPolicy(it) }
            savedNotificationPolicy = null
            notificationManager.setInterruptionFilter(filter)
            savedInterruptionFilter = -1
        } catch (_: SecurityException) { /* no-op */ }
    }

    companion object {
        /**
         * Check if app can manage DND (user has granted "Do Not Disturb access").
         * Used to show/hide DND-related UI; actual enable/restore still no-ops if not granted.
         */
        @JvmStatic
        fun canManageDnd(notificationManager: NotificationManager): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    notificationManager.isNotificationPolicyAccessGranted
                } catch (_: SecurityException) {
                    false
                }
            } else false
        }

        /**
         * Intent to open DND access settings (optional; for "Grant DND access" in settings).
         */
        @JvmStatic
        fun intentForDndSettings(): android.content.Intent? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            } else null
        }
    }
}
