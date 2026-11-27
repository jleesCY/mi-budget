package com.jlees.mibudget

import kotlinx.coroutines.flow.debounce

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.jlees.mibudget.reminders.RenewalReminders

class MiApp : Application() {
    /** Give the rendered brand logos back when Android asks (app in the background, or memory is tight). */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && ::container.isInitialized) container.brands.trimMemory()
    }

    lateinit var container: AppContainer
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** When this run of the app started: start-up clean-up only touches files older than this. */
    private var launchedAt = 0L

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        launchedAt = System.currentTimeMillis()
        container = AppContainer(this)
        RenewalReminders.ensureChannel(this)
        RenewalReminders.schedule(this)
        // The This Month widget follows every change to purchases, subscriptions and budgets while the app runs.
        appScope.launch {
            kotlinx.coroutines.flow.combine(container.repository.purchases, container.repository.subscriptions, container.repository.categoryTree) { _, _, _ -> }
                .debounce(1_000)
                .collect { runCatching { com.jlees.mibudget.widget.MonthWidget.update(this@MiApp) } }
        }
        appScope.launch {
            // Read the ~1 MB brand catalog in the background before a screen needs it.
            runCatching { container.brands.preload() }
            // and the logos of the subscriptions and this month's purchases, at their lists' sizes, so the first swipe
            // to the Subscriptions page (or scroll down the start screen) doesn't stall drawing them
            runCatching {
                val subs = container.repository.subscriptions.first().map { it.brandKey to it.name }
                val recent = container.repository.purchases.first().take(60).map { it.brandKey to it.merchant }
                container.brands.warm(subs, resources.displayMetrics.density, listOf(44, 40, 22))
                container.brands.warm(recent, resources.displayMetrics.density, listOf(44))
            }
            startupTasks()
            // Recently Deleted keeps things as long as Settings › Recently Deleted says, then they go for good (with their photos).
            runCatching { container.repository.purgeTrash(com.jlees.mibudget.domain.Trash.purgeBefore(System.currentTimeMillis(), container.settings.current().keepDeleted.days)) }
            cleanupStorage() // only at launch: nothing is using temporary files yet
        }
    }

    /** Runs on every launch (and after "Erase everything"): first-run seeding, subscription auto-logging. */
    suspend fun startupTasks() {
        val settings = container.settings.current()
        if (!settings.defaultsSeeded) {
            container.repository.seedDefaults()
            container.settings.update { it.copy(defaultsSeeded = true) }
        }
        // Gemini Nano picked but this phone no longer supports it (e.g. restored from another phone).
        if (settings.scanEngine == com.jlees.mibudget.scan.ScanEngine.GEMINI_NANO &&
            container.nanoScanner.status() == com.jlees.mibudget.scan.NanoStatus.UNSUPPORTED
        ) container.settings.update { it.copy(scanEngine = com.jlees.mibudget.scan.ScanEngine.STANDARD) }
        com.jlees.mibudget.data.FxRepository.schedule(this, container.fx.hasPack())
        // Reminders, then auto-logging (so a "renews today" reminder isn't skipped when the app is
        // opened before the 9 AM check).
        RenewalReminders.runCheck(this)
    }

    /**
     * Sweeps up files nothing needs any more. Each file is normally deleted as soon as it's done
     * with; this catches what a crash, a force-stop or a dead battery left behind.
     */
    private suspend fun cleanupStorage() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        // Vision AI (a downloadable scanner, removed in 0.2.0) kept its model (about 2.6 GB) and cache here: free them
        runCatching {
            listOfNotNull(getExternalFilesDir("models"), java.io.File(filesDir, "models"), java.io.File(cacheDir, "litertlm"))
                .forEach { it.deleteRecursively() }
            deleteSharedPreferences("smart_scan")
        }
        // Receipt photos no purchase/subscription uses (unfinished "resume" adds keep theirs).
        runCatching { container.repository.cleanupReceipts(keep = container.pendingAdds.receiptFiles(), before = launchedAt) }
        // Custom icon pictures nothing uses (replaced, or chosen and then not saved).
        runCatching { container.repository.cleanupIcons(keepKeys = container.pendingAdds.iconKeys(), before = launchedAt) }
        // Check splitter photos the saved split doesn't use.
        runCatching { com.jlees.mibudget.ui.tools.SplitStore(this@MiApp).cleanupPhotos(before = launchedAt) }
        // The post-erase safety copy, once its week is up (not only when Settings is opened).
        runCatching { com.jlees.mibudget.data.SafetyCopy.prune(this@MiApp) }
        // Camera captures, AI model inputs, unpacked backups.
        com.jlees.mibudget.data.TempFiles.startupCleanup(this@MiApp, launchedAt)
    }
}
