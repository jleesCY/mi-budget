package com.jlees.mibudget

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jlees.mibudget.data.AppSettings
import com.jlees.mibudget.ui.components.LocalBrandCatalog
import com.jlees.mibudget.ui.navigation.AppNav
import com.jlees.mibudget.ui.theme.MiTheme
import com.jlees.mibudget.reminders.RenewalReminders

class MainActivity : ComponentActivity() {
    private var sharedImage by mutableStateOf<String?>(null)
    private var openSubscriptionId by mutableStateOf<String?>(null)
    private var openLibrary by mutableStateOf(false)

    companion object {
        /** The widget's heading: open the Library. */
        const val EXTRA_OPEN_LIBRARY = "open_library"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as MiApp).container
        if (savedInstanceState == null) handleIntent(intent)
        // Read the settings before the first frame, so a Dark-theme user doesn't see a white flash
        // (DataStore is small and usually already loaded by start-up; never wait more than 300 ms).
        val firstSettings = kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(300) { container.settings.current() }
        } ?: AppSettings()
        if (firstSettings.hideFromScreenshots) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        if (savedInstanceState == null) container.updates.atLaunch()

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = firstSettings)
            // Look & Feel › Status Bar: hidden, it shows for a moment when swiped down from the top.
            androidx.compose.runtime.LaunchedEffect(settings.showStatusBar) {
                val bars = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                bars.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (settings.showStatusBar) bars.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                else bars.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
            // Look & Feel › Rotate Screen: follow Android's own auto-rotate, or stay upright.
            androidx.compose.runtime.LaunchedEffect(settings.rotateScreen) {
                requestedOrientation = if (settings.rotateScreen) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            // Settings › Privacy & Security › Hide From Screenshots: also blanks the app's preview in Recents.
            androidx.compose.runtime.LaunchedEffect(settings.hideFromScreenshots) {
                if (settings.hideFromScreenshots) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
            val view = androidx.compose.ui.platform.LocalView.current
            val hapticsOn = androidx.compose.runtime.rememberUpdatedState(settings.hapticFeedback)
            val haptics = androidx.compose.runtime.remember(view) { com.jlees.mibudget.ui.common.Haptics(view) { hapticsOn.value } }
            CompositionLocalProvider(
                LocalBrandCatalog provides container.brands,
                com.jlees.mibudget.ui.common.LocalHaptics provides haptics,
                com.jlees.mibudget.ui.common.LocalSettings provides settings,
            ) {
                MiTheme(settings) {
                    AppNav(
                        sharedImage = sharedImage,
                        onSharedImageConsumed = { sharedImage = null },
                        openSubscriptionId = openSubscriptionId,
                        onOpenSubscriptionConsumed = { openSubscriptionId = null },
                        openLibrary = openLibrary,
                        onOpenLibraryConsumed = { openLibrary = false },
                    )
                    com.jlees.mibudget.update.UpdateDialogs(container.updates)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        // Reopened from Recents: Android hands back the intent that first started us — don't replay
        // an old notification tap or share.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        // Anything malformed in an intent from another app is ignored rather than crashing start-up.
        runCatching {
            if (intent.getBooleanExtra(EXTRA_OPEN_LIBRARY, false)) { openLibrary = true; intent.removeExtra(EXTRA_OPEN_LIBRARY); return }
            intent.getStringExtra(RenewalReminders.EXTRA_SUBSCRIPTION_ID)?.let {
                openSubscriptionId = it
                intent.removeExtra(RenewalReminders.EXTRA_SUBSCRIPTION_ID)
                return
            }
            if (intent.action != Intent.ACTION_SEND || intent.type?.startsWith("image/") != true) return
            val uri: Uri? = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri // some apps only fill clipData
            sharedImage = uri?.takeIf(::acceptableShare)?.toString()
            // Consumed: a later configuration change or onNewIntent won't bring it back.
            intent.action = null
        }
    }

    /**
     * Only content:// pictures from other apps. file:// would read our own private files with our
     * permissions, and our own FileProvider could be used the same way.
     */
    private fun acceptableShare(uri: Uri): Boolean =
        uri.scheme.equals("content", ignoreCase = true) && uri.authority != "$packageName.fileprovider"
}
