package com.jlees.mibudget.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The settings, saved as JSON (filesDir/settings.json). Loaded once off the main thread; every change goes through
 * [update] and is written whole (to a temporary file first, so a crash mid-write can't damage it).
 */
class SettingsRepository(context: Context) {
    private val file = File(context.filesDir, "settings.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    private val state = MutableStateFlow<AppSettings?>(null)
    private val lock = Mutex()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        io.launch {
            val loaded = runCatching { json.decodeFromString<AppSettings>(file.readText()) }.getOrNull()
            state.compareAndSet(null, (loaded ?: AppSettings()).sanitized().migrated())
        }
    }

    /** The settings, once loaded. */
    val settings = state.filterNotNull()

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) = lock.withLock {
        val next = transform(current())
        state.value = next
        withContext(Dispatchers.IO) {
            val tmp = File(file.parentFile, "settings.json.tmp")
            tmp.writeText(json.encodeToString(AppSettings.serializer(), next))
            tmp.renameTo(file)
        }
    }

    suspend fun sentReminders(): Set<String> = current().sentReminders

    /** Keeps the set small: only the most recent 300 keys matter. */
    suspend fun markRemindersSent(keys: Collection<String>) {
        if (keys.isNotEmpty()) update { it.copy(sentReminders = (it.sentReminders + keys).toList().takeLast(300).toSet()) }
    }

    /** Remembers a search typed in the Pill (newest first; at most 10), when Recent Searches is on. */
    suspend fun rememberSearch(q: String) {
        val t = q.trim()
        if (t.length < 2) return
        update { s -> if (!s.recentSearches) s else s.copy(searchHistory = (listOf(t) + s.searchHistory.filter { !it.equals(t, true) }).take(AppSettings.MAX_SEARCHES)) }
    }

    suspend fun forgetSearch(q: String) = update { s -> s.copy(searchHistory = s.searchHistory - q) }
}

