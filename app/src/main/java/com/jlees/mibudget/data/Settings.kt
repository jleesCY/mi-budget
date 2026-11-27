// Every setting, in one serializable class (saved as JSON; see SettingsRepository). Each field has its default; choices
// are enums implementing Choice, saved by a @SerialName that never changes. Defaults change for people who already
// have the app only through migrated() (with a test); new fields need nothing (their default applies).
package com.jlees.mibudget.data

import com.jlees.mibudget.scan.ScanEngine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

/** A multi-way option: its words in Settings. */
interface Choice { val label: String }

@Serializable enum class ThemeMode(override val label: String) : Choice {
    @SerialName("light") LIGHT("Light"), @SerialName("dark") DARK("Dark"), @SerialName("system") SYSTEM("System")
}

@Serializable enum class AppFont(override val label: String) : Choice {
    @SerialName("standard") STANDARD("Standard"), @SerialName("rounded") ROUNDED("Rounded"), @SerialName("system") SYSTEM("System")
}

@Serializable enum class Spacing(override val label: String) : Choice {
    @SerialName("compact") COMPACT("Compact"), @SerialName("normal") NORMAL("Normal"), @SerialName("airy") AIRY("Airy")
}

@Serializable enum class ListStyle(override val label: String) : Choice {
    @SerialName("cards") CARDS("Cards"), @SerialName("table") TABLE("Table")
}

/** The places one-line text can pan (Look & Feel › Pan Overflowed Text). */
@Serializable enum class PanArea(override val label: String) : Choice {
    @SerialName("settings") SETTINGS("Settings"),
    @SerialName("purchases") PURCHASES("Purchases"),
    @SerialName("subscriptions") SUBSCRIPTIONS("Subscriptions"),
    @SerialName("budgets") BUDGETS("Budgets"),
    @SerialName("deleted") DELETED("Recently Deleted"),
}

@Serializable enum class ChartType(override val label: String) : Choice {
    @SerialName("donut") DONUT("Donut"), @SerialName("pie") PIE("Pie"), @SerialName("rings") RADIAL("Rings"),
    @SerialName("bars") BARS("Bars"), @SerialName("trend") TREND("Trend"),
}

@Serializable enum class ClockStyle(override val label: String) : Choice {
    @SerialName("none") NONE("None"), @SerialName("12h") H12("12h"), @SerialName("24h") H24("24h")
}

@Serializable enum class DatePlacement(override val label: String) : Choice {
    @SerialName("none") NONE("None"), @SerialName("above") ABOVE("Above"), @SerialName("below") BELOW("Below")
}

@Serializable enum class WeekStart(override val label: String, val day: DayOfWeek) : Choice {
    @SerialName("sunday") SUNDAY("Sunday", DayOfWeek.SUNDAY), @SerialName("monday") MONDAY("Monday", DayOfWeek.MONDAY)
}

/** How long deleted purchases and subscriptions stay in Recently Deleted. */
@Serializable enum class KeepDeleted(override val label: String, val days: Long) : Choice {
    @SerialName("week") WEEK("1 Week", 7), @SerialName("month") MONTH("30 Days", 30), @SerialName("quarter") QUARTER("90 Days", 90)
}

/** The accent setting's value for Adaptive (the wallpaper's colours). Otherwise the seed's "#RRGGBB". */
const val DYNAMIC_ACCENT = "dynamic"
/** mi. Budget's own colour, the default accent. */
const val APP_ACCENT = "#34C759"
const val SETTINGS_VERSION = 1

@Serializable
data class AppSettings(
    // ---- Look & Feel
    val themeMode: ThemeMode = ThemeMode.DARK,
    val pureBlack: Boolean = false,
    val accentColor: String = APP_ACCENT,
    val fontScale: Float = 1f,
    val font: AppFont = AppFont.STANDARD,
    val panLongText: Boolean = false,
    val panAreas: Set<PanArea> = setOf(PanArea.SETTINGS),
    val cornerRadius: Int = 28,
    val uiPadding: Spacing = Spacing.NORMAL,
    val listStyle: ListStyle = ListStyle.TABLE,
    /** Long lists load this many rows at a time, and more as they're scrolled (one of [LIST_CHUNKS]). */
    val listChunk: Int = 25,
    val showStatusBar: Boolean = true,
    val rotateScreen: Boolean = true,
    val hapticFeedback: Boolean = true,
    // ---- The Pill
    val recentSearches: Boolean = true,
    // ---- Start screen
    val chartType: ChartType = ChartType.DONUT,
    val clock: ClockStyle = ClockStyle.NONE,
    val date: DatePlacement = DatePlacement.NONE,
    // ---- Gestures
    val longPressSearch: Boolean = true,
    val swipeMonths: Boolean = true,
    // ---- Budget
    val weekStart: WeekStart = WeekStart.SUNDAY,
    val nudgeUncategorized: Boolean = true,
    val renewalReminders: Boolean = true,
    /** Days before a renewal to notify (0 = on the day). */
    val reminderDaysBefore: Int = 1,
    val scanEngine: ScanEngine = ScanEngine.STANDARD,
    // ---- Saved
    val keepDeleted: KeepDeleted = KeepDeleted.MONTH,
    // ---- Privacy & Security
    val hideFromScreenshots: Boolean = false,

    // ---- Not settings you see (state kept with them; the ones that belong to one phone stay out of backups)
    val settingsVersion: Int = SETTINGS_VERSION,
    /** Past searches in the Pill, newest first. */
    val searchHistory: List<String> = emptyList(),
    /** The version whose What's New was last shown ("" = never: a new install, which gets the walkthrough). */
    val lastWhatsNew: String = "",
    val walkthroughDone: Boolean = false,
    /** When an update check last found nothing new (the next waits 12 hours). */
    val lastUpdateCheck: Long = 0,
    val defaultsSeeded: Boolean = false,
    /** Keys of renewal reminders already shown (so each fires once). */
    val sentReminders: Set<String> = emptySet(),
) {
    val firstDayOfWeek: DayOfWeek get() = weekStart.day

    /** Damaged or hand-edited values (an import) back in range. */
    fun sanitized() = copy(
        fontScale = (Math.round(fontScale.coerceIn(0.8f, 1.2f) * 10) / 10f),
        cornerRadius = cornerRadius.takeIf { it in CORNER_CHOICES } ?: 28,
        accentColor = accentColor.takeIf { it == DYNAMIC_ACCENT || it.uppercase() in ACCENTS } ?: APP_ACCENT,
        reminderDaysBefore = reminderDaysBefore.coerceIn(0, 14),
        searchHistory = searchHistory.filter { it.isNotBlank() }.take(MAX_SEARCHES),
        sentReminders = sentReminders.toList().takeLast(300).toSet(),
    )

    /** Upgrades settings saved by an older version (each step bumps [settingsVersion]). None yet. */
    fun migrated(): AppSettings {
        var s = this
        // when a default or a stored value changes: if (s.settingsVersion < 2) s = s.copy(…, settingsVersion = 2)
        if (s.settingsVersion < SETTINGS_VERSION) s = s.copy(settingsVersion = SETTINGS_VERSION)
        return s
    }

    /** Without what belongs to this phone only, for a backup. */
    fun forBackup() = copy(
        lastWhatsNew = "", walkthroughDone = false, lastUpdateCheck = 0,
        defaultsSeeded = false, sentReminders = emptySet(),
    )

    /** An imported backup's settings, keeping this phone's own state. */
    fun restoredFrom(b: AppSettings) = b.sanitized().migrated().copy(
        lastWhatsNew = lastWhatsNew, walkthroughDone = walkthroughDone, lastUpdateCheck = lastUpdateCheck,
        defaultsSeeded = defaultsSeeded, sentReminders = sentReminders,
    )

    /** Reset All Settings: defaults, keeping what isn't a setting you see. */
    fun reset() = AppSettings().copy(
        searchHistory = searchHistory, lastWhatsNew = lastWhatsNew,
        walkthroughDone = walkthroughDone, lastUpdateCheck = lastUpdateCheck, defaultsSeeded = defaultsSeeded,
        sentReminders = sentReminders, scanEngine = scanEngine,
    )

    companion object {
        /** The accent swatches, in the order the picker shows them (after Adaptive). mi. Budget's green is the default. */
        val ACCENTS = listOf("#8E8E93", "#3D9BFF", "#34C759", "#FF3B30", "#FF9500", "#FFCC00", "#AF52DE")
        val ACCENT_NAMES = listOf("Gray", "Blue", "Green", "Red", "Orange", "Yellow", "Purple")
        const val GRAY_ACCENT = "#8E8E93"
        const val MAX_SEARCHES = 10
    }
}

/** The Corners setting's choices (dp). */
val CORNER_CHOICES = listOf(0, 12, 28)

/** "#RRGGBB" → ARGB, mi. Budget's green when it isn't a colour. */
fun hexColor(hex: String?): Long {
    val h = hex?.removePrefix("#") ?: return 0xFF34C759
    return h.toLongOrNull(16)?.takeIf { h.length == 6 }?.let { 0xFF000000 or it } ?: 0xFF34C759
}

/** The choices for Settings › Rows Loaded at Once. */
val LIST_CHUNKS = listOf(10, 25, 50, 100)
