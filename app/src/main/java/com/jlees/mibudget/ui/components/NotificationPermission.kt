package com.jlees.mibudget.ui.components

import androidx.compose.runtime.rememberUpdatedState
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jlees.mibudget.reminders.RenewalReminders

@Stable
class NotificationPermissionState internal constructor(
    granted: Boolean,
    private val onRequest: () -> Unit,
) {
    /** mi. Budget can post renewal reminders (permission granted, app and channel not turned off). */
    var granted by mutableStateOf(granted)
        internal set

    /**
     * Asks for permission. When Android won't show its dialog (it was already declined, or
     * notifications were turned off in system settings) this opens the settings page instead, so a
     * tap is never silently ignored.
     */
    fun request() = onRequest()

    internal companion object {
        private const val PREFS = "ui"
        private const val ASKED = "asked_notifications"

        /** Has mi. Budget ever asked on its own? (so it only asks once, in context) */
        fun askedBefore(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ASKED, false)
        fun markAsked(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ASKED, true).apply()
    }
}

/** Opens the system page where notifications for mi. Budget (or just its reminder channel) can be turned on. */
fun openNotificationSettings(context: Context) {
    val nm = NotificationManagerCompat.from(context)
    val appOn = nm.areNotificationsEnabled()
    val intent = if (appOn) {
        // The app is allowed but the reminders channel is off: go straight to that channel.
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, RenewalReminders.CHANNEL_ID)
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Tracks POST_NOTIFICATIONS (Android 13+), whether notifications are on, and the reminder channel. */
@Composable
fun rememberNotificationPermission(onResult: (Boolean) -> Unit = {}): NotificationPermissionState {
    val context = LocalContext.current
    val latestOnResult by rememberUpdatedState(onResult)
    lateinit var state: NotificationPermissionState
    // When the dialog was launched: an answer within a blink means Android didn't show it at all.
    val launchedAt = remember { longArrayOf(0L) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        state.granted = RenewalReminders.canNotify(context)
        if (!ok && SystemClock.elapsedRealtime() - launchedAt[0] < 400) openNotificationSettings(context)
        latestOnResult(state.granted)
    }
    // mi. asks in its own words first (a plain question), then shows Android's prompt
    var asking by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val systemAsk = {
        NotificationPermissionState.markAsked(context)
        launchedAt[0] = SystemClock.elapsedRealtime()
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    state = remember {
        NotificationPermissionState(
            granted = RenewalReminders.canNotify(context),
            onRequest = {
                val permissionMissing = Build.VERSION.SDK_INT >= 33 && !RenewalReminders.hasPermission(context)
                // refused for good (Android won't ask again): only here, after the user switched reminders on, open its settings
                if (permissionMissing) asking = true else openNotificationSettings(context)
            },
        )
    }
    if (asking) com.jlees.mibudget.ui.common.ModalFrame({ asking = false }) {
        com.jlees.mibudget.ui.common.DialogCard("Allow Reminders?", actions = {
            com.jlees.mibudget.ui.common.DialogAction("Not Now") { asking = false }
            com.jlees.mibudget.ui.common.DialogAction("Allow") { asking = false; systemAsk() }
        }) { androidx.compose.material3.Text("Allow mi. Budget to send notifications? They remind you before subscriptions renew and free trials end.") }
    }
    // Re-check when returning from system settings.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.granted = RenewalReminders.canNotify(context) }
    return state
}

/** Asks for notifications once, by itself — used where reminders first matter (you have a subscription). */
@Composable
fun AskForNotificationsOnce(when_: Boolean, permission: NotificationPermissionState) {
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(when_) {
        // Only Android's own dialog, never a jump to system settings nobody asked for.
        if (when_ && Build.VERSION.SDK_INT >= 33 && !RenewalReminders.hasPermission(context) &&
            !NotificationPermissionState.askedBefore(context)
        ) permission.request()
    }
}
