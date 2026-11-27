package com.jlees.mibudget.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jlees.mibudget.MiApp
import com.jlees.mibudget.data.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.jlees.mibudget.data.db.isLive
import com.jlees.mibudget.reminders.RenewalReminders
import java.io.File
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import com.jlees.mibudget.data.backup.ImportPlan
import com.jlees.mibudget.scan.NanoStatus
import com.jlees.mibudget.scan.ScanEngine
import kotlinx.coroutines.withContext


data class DataCounts(val categories: Int = 0, val purchases: Int = 0, val subscriptions: Int = 0)

/** One row in Settings → Scanner. */
data class EngineRow(
    val engine: ScanEngine,
    /** Gemini Nano only. */
    val nano: NanoStatus? = null,
) {
    val ready: Boolean
        get() = when (engine) {
            ScanEngine.STANDARD -> true
            // Selectable once the phone supports it; if Android still has to fetch it, selecting starts that.
            ScanEngine.GEMINI_NANO -> nano != null && nano != NanoStatus.UNSUPPORTED && nano != NanoStatus.UNKNOWN
        }
}

/** The automatic copy taken right before "Erase all data". */
data class SafetyBackupInfo(val label: String, val daysLeft: Int)

class SettingsViewModel(private val app: MiApp) : ViewModel() {
    private val c = app.container

    val settings: StateFlow<AppSettings> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    // A Channel keeps messages until the screen reads them — e.g. "Exported 120 records" arriving
    // while you're on another tab is shown when you come back instead of being dropped.
    // Each message carries whether it's good news that finished at once (the notice shows a ✓ for those only).
    private val messageQueue = kotlinx.coroutines.channels.Channel<Pair<String, Boolean>>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val messages: kotlinx.coroutines.flow.Flow<Pair<String, Boolean>> = messageQueue.receiveAsFlow()

    /** A notice for the screen: short, past tense, never a raw error. [done] = good news, finished (a ✓). */
    private suspend fun say(text: String, done: Boolean = false) = messageQueue.send(text to done)

    val counts: StateFlow<DataCounts> = combine(c.repository.categories, c.repository.purchases, c.repository.subscriptions) { a, b, s ->
        DataCounts(a.size, b.size, s.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataCounts())

    val brandStats: Pair<Int, Int> = c.brands.all.let { all -> all.size to all.count { c.brands.hasGlyph(it) } }

    private val safetyFile = com.jlees.mibudget.data.SafetyCopy.file(app)
    private val _safety = MutableStateFlow<SafetyBackupInfo?>(null)
    val safetyBackup: StateFlow<SafetyBackupInfo?> = _safety

    private val _engines = MutableStateFlow(listOf(EngineRow(ScanEngine.STANDARD)))
    val engines: StateFlow<List<EngineRow>> = _engines
    val nanoMessage = MutableStateFlow<String?>(null)
    /** Null until checked. */
    val nanoSupported = MutableStateFlow<Boolean?>(null)

    private suspend fun refreshEngines() {
        val checked = c.nanoScanner.status()
        // Couldn't tell this time: keep showing what we knew.
        val nano = if (checked == NanoStatus.UNKNOWN) {
            _engines.value.firstOrNull { it.engine == ScanEngine.GEMINI_NANO }?.nano
                ?: if (nanoSupported.value == false) NanoStatus.UNSUPPORTED else NanoStatus.UNKNOWN
        } else checked
        // Lost support for sure (e.g. restored to another phone): fall back to Standard.
        if (nano == NanoStatus.UNSUPPORTED && settings.value.scanEngine == ScanEngine.GEMINI_NANO) {
            c.settings.update { it.copy(scanEngine = ScanEngine.STANDARD) }
        }
        if (nano != NanoStatus.UNKNOWN) nanoSupported.value = nano != NanoStatus.UNSUPPORTED
        _engines.value = ScanEngine.entries.mapNotNull { e ->
            when (e) {
                ScanEngine.STANDARD -> EngineRow(e)
                // Detected automatically: only listed on phones that support it.
                ScanEngine.GEMINI_NANO -> if (nano == NanoStatus.UNSUPPORTED || (nano == NanoStatus.UNKNOWN && nanoSupported.value != true)) null
                    else EngineRow(e, nano = nano)
            }
        }
    }

    init {
        viewModelScope.launch { refreshEngines() }
    }

    /**
     * Keeps the scanner rows fresh (Gemini Nano's setup) — the screen runs this only while the
     * Scanner page is showing, so nothing polls in the background.
     */
    suspend fun watchEngines() {
        while (true) {
            refreshEngines()
            val busy = _engines.value.any { it.nano == NanoStatus.DOWNLOADING }
            kotlinx.coroutines.delay(if (busy) 1_000 else 4_000)
        }
    }

    /** One refresh, e.g. when another Settings page opens. */
    fun refreshEnginesNow() = viewModelScope.launch { refreshEngines() }

    fun selectEngine(e: ScanEngine) {
        update { it.copy(scanEngine = e) }
        // Gemini Nano supported but not on the phone yet: ask Android to fetch it now.
        if (e == ScanEngine.GEMINI_NANO && _engines.value.any { it.engine == e && it.nano == NanoStatus.NEEDS_DOWNLOAD }) downloadNano()
    }

    /** Asks the phone (AICore) to fetch Gemini Nano. */
    // Runs in the app's scope so leaving Settings doesn't stop the download.
    fun downloadNano() {
        // One request at a time: tapping again while Android is fetching it doesn't start another.
        if (nanoJob?.isActive == true) return
        nanoJob = app.appScope.launch {
            runCatching {
                c.nanoScanner.download().collect { nanoMessage.value = it }
            }.onFailure { if (it !is kotlinx.coroutines.CancellationException) nanoMessage.value = "Couldn't start the download" }
            refreshEngines()
        }
    }

    private companion object {
        /** Shared by every Settings screen instance, since the download outlives them. */
        @Volatile var nanoJob: kotlinx.coroutines.Job? = null
    }

    init {
        viewModelScope.launch(Dispatchers.IO) { _safety.value = safetyInfo() }
    }

    /** How many things are in Recently Deleted. */
    val trashCount: StateFlow<Int> = combine(c.repository.deletedPurchases, c.repository.deletedSubscriptions) { p, s -> p.size + s.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Saved › Empty Recently Deleted (asked first: it can't be undone). */
    fun emptyTrash() = c.appScope.launch { c.repository.purgeTrash(before = Long.MAX_VALUE); say("Emptied Recently Deleted") }

    /** Backup & Reset › Reset All Settings: defaults again; purchases and everything else stay. */
    fun resetSettings() = viewModelScope.launch { c.settings.update { it.reset() }; say("Reset all settings") }

    private fun safetyInfo(): SafetyBackupInfo? = com.jlees.mibudget.data.SafetyCopy.info(app)?.let { (made, left) ->
        SafetyBackupInfo(made.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")), left)
    }

    /** Settings → Your data → "Delete safety copy now": you're sure about the erase. */
    fun deleteSafetyBackup() = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) { com.jlees.mibudget.data.SafetyCopy.delete(app) }
        _safety.value = withContext(Dispatchers.IO) { safetyInfo() }
        say(if (ok) "Deleted the safety copy" else "Couldn't delete the safety copy")
    }

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { c.settings.update(transform) }

    fun exportFileName() = c.backup.suggestedFileName()

    fun export(uri: Uri) = viewModelScope.launch {
        runCatching { c.backup.export(uri) }
            .onSuccess { say("Exported $it records", done = true) }
            .onFailure { say("Couldn't export your data") }
    }

    fun checkRemindersNow() {
        RenewalReminders.checkNow(app)
    }

    // ---------------------------------------------------------------- tools: currency rates

    /** The rates the converter uses (built in, or the daily-updates pack). */
    val fxInfo = MutableStateFlow<com.jlees.mibudget.data.FxRepository.Rates?>(null)
    val fxBusy = MutableStateFlow(false)

    fun loadFxInfo() = viewModelScope.launch { fxInfo.value = runCatching { c.fx.current() }.getOrNull() }

    /** Download (or update) the daily-updates pack. */
    fun downloadFxPack() = viewModelScope.launch {
        if (fxBusy.value) return@launch
        fxBusy.value = true
        runCatching { c.fx.downloadPack() }
            .onSuccess {
                fxInfo.value = it
                say("Rates updated to ${fxDate(it.table.date)}", done = true)
            }
            .onFailure { say("Couldn't download rates") }
        fxBusy.value = false
    }

    fun deleteFxPack() = viewModelScope.launch {
        runCatching { c.fx.deletePack() }
        fxInfo.value = runCatching { c.fx.current() }.getOrNull()
        say("Using the built-in rates")
    }


    fun restoreSafetyBackup() = viewModelScope.launch {
        runCatching {
            val loaded = c.backup.load(Uri.fromFile(safetyFile))
            try {
                // Exactly as it was: original ids, icons and colors, no merging with the new defaults.
                c.backup.restoreExact(loaded)
            } finally {
                loaded.release()
            }
        }.onSuccess {
            safetyFile.delete()
            _safety.value = null
            say("Restored ${it.purchasesAdded} purchases and ${it.subscriptionsAdded} subscriptions", done = true)
        }.onFailure { say("Couldn't restore the safety copy") }
    }

    fun eraseAll() = viewModelScope.launch {
        // Safety net: keep a full copy on the phone for a week, so a mistaken erase can be undone.
        runCatching { c.backup.export(Uri.fromFile(safetyFile)) }
            .onFailure { say("Nothing was wiped: couldn't make a safety copy"); return@launch }
        _safety.value = withContext(Dispatchers.IO) { safetyInfo() }
        c.repository.eraseEverything()
        // Their photos were just erased too, so unfinished adds can't be resumed any more.
        com.jlees.mibudget.scan.ScanKind.entries.forEach { c.pendingAdds.clear(it, deleteReceipt = false) }
        // and the settings: the app starts fresh (but the walkthrough and What's New aren't shown again)
        c.settings.update { it.reset().copy(defaultsSeeded = false, searchHistory = emptyList()) }
        app.startupTasks() // re-seed default categories
        say("Wiped all data")
    }
}
