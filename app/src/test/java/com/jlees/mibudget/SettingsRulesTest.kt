package com.jlees.mibudget

import com.jlees.mibudget.data.AppFont
import com.jlees.mibudget.data.AppSettings
import com.jlees.mibudget.data.ChartType
import com.jlees.mibudget.data.DYNAMIC_ACCENT
import com.jlees.mibudget.data.SETTINGS_VERSION
import com.jlees.mibudget.data.ThemeMode
import com.jlees.mibudget.data.WeekStart
import com.jlees.mibudget.data.backup.SettingsDto
import com.jlees.mibudget.data.backup.applyDto
import com.jlees.mibudget.data.hexColor
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class SettingsRulesTest {
    @Test fun defaultsAreMiDefaults() {
        val s = AppSettings()
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals("#34C759", s.accentColor) // mi. Budget's own green
        assertEquals(28, s.cornerRadius)
        assertEquals(AppFont.STANDARD, s.font)
        assertTrue(s.hapticFeedback && s.showStatusBar && s.rotateScreen)
    }

    @Test fun sanitizedRepairsDamagedValues() {
        val s = AppSettings(fontScale = 3f, cornerRadius = 7, accentColor = "#123456", reminderDaysBefore = 99,
            searchHistory = List(30) { "q$it" } + "").sanitized()
        assertEquals(1.2f, s.fontScale)
        assertEquals(28, s.cornerRadius)
        assertEquals("#34C759", s.accentColor)
        assertEquals(14, s.reminderDaysBefore)
        assertEquals(AppSettings.MAX_SEARCHES, s.searchHistory.size)
        assertEquals(DYNAMIC_ACCENT, AppSettings(accentColor = DYNAMIC_ACCENT).sanitized().accentColor)
    }

    @Test fun migratedBringsOldSettingsUpToDate() {
        assertEquals(SETTINGS_VERSION, AppSettings(settingsVersion = 0).migrated().settingsVersion)
        // a migration never changes what the user picked
        val picked = AppSettings(settingsVersion = 0, themeMode = ThemeMode.LIGHT, accentColor = "#FF3B30")
        assertEquals(ThemeMode.LIGHT, picked.migrated().themeMode)
        assertEquals("#FF3B30", picked.migrated().accentColor)
    }

    @Test fun backupsLeaveOutWhatBelongsToThisPhone() {
        val mine = AppSettings(lastWhatsNew = "0.2.0", walkthroughDone = true, sentReminders = setOf("x"), themeMode = ThemeMode.LIGHT)
        val b = mine.forBackup()
        assertEquals("", b.lastWhatsNew)
        assertFalse(b.walkthroughDone)
        assertTrue(b.sentReminders.isEmpty())
        assertEquals(ThemeMode.LIGHT, b.themeMode)
        // restoring keeps this phone's own state
        val here = AppSettings(walkthroughDone = true, lastWhatsNew = "0.2.0")
        val restored = here.restoredFrom(b)
        assertTrue(restored.walkthroughDone)
        assertEquals("0.2.0", restored.lastWhatsNew)
        assertEquals(ThemeMode.LIGHT, restored.themeMode)
    }

    @Test fun resetKeepsHiddenState() {
        val s = AppSettings(themeMode = ThemeMode.LIGHT, walkthroughDone = true, searchHistory = listOf("coffee")).reset()
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertTrue(s.walkthroughDone)
        assertEquals(listOf("coffee"), s.searchHistory)
    }

    @Test fun savedNamesNeverChange() {
        // the JSON uses each choice's @SerialName, so renaming a label never loses a setting
        val json = Json { encodeDefaults = true }
        val text = json.encodeToString(AppSettings.serializer(), AppSettings(themeMode = ThemeMode.SYSTEM, chartType = ChartType.RADIAL))
        assertTrue(text.contains("\"themeMode\":\"system\""))
        assertTrue(text.contains("\"chartType\":\"rings\""))
        val back = Json { ignoreUnknownKeys = true }.decodeFromString(AppSettings.serializer(), "{\"themeMode\":\"light\",\"futureSetting\":1}")
        assertEquals(ThemeMode.LIGHT, back.themeMode)
    }

    @Test fun budgeyBackupSettingsMapOver() {
        val s = AppSettings().applyDto(SettingsDto(themeMode = "LIGHT", seedColor = "#FF3F9B3A", amoled = true, firstDayOfWeek = "MONDAY",
            font = "GOOGLE_SANS_FLEX", roundedFont = true, textSize = "LARGE", chartType = "RADIAL"))
        assertEquals(ThemeMode.LIGHT, s.themeMode)
        assertEquals("#34C759", s.accentColor) // a budgie green isn't a mi. accent: kept as it was
        assertTrue(s.pureBlack)
        assertEquals(WeekStart.MONDAY, s.weekStart)
        assertEquals(DayOfWeek.MONDAY, s.firstDayOfWeek)
        assertEquals(AppFont.ROUNDED, s.font)
        assertEquals(1.1f, s.fontScale)
        assertEquals(ChartType.RADIAL, s.chartType)
        assertEquals("#3D9BFF", AppSettings().applyDto(SettingsDto(seedColor = "#FF3D9BFF")).accentColor)
        assertEquals(DYNAMIC_ACCENT, AppSettings().applyDto(SettingsDto(dynamicColor = true)).accentColor)
    }

    @Test fun hexColorFallsBackToGreen() {
        assertEquals(0xFF3D9BFF, hexColor("#3D9BFF"))
        assertEquals(0xFF34C759, hexColor("nonsense"))
        assertEquals(0xFF34C759, hexColor(null))
    }
}
