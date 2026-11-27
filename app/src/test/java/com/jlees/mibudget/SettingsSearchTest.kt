package com.jlees.mibudget

import com.jlees.mibudget.ui.common.Sym
import com.jlees.mibudget.ui.settings.SettingsHeading
import com.jlees.mibudget.ui.settings.SettingsRow
import com.jlees.mibudget.ui.settings.SettingsSection
import com.jlees.mibudget.ui.settings.placeRows
import com.jlees.mibudget.ui.settings.searchSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    private val look = SettingsSection("look", "Look & Feel", Sym.Palette, "", listOf(
        SettingsHeading("Colors"),
        SettingsRow("Theme", "light dark") {},
        SettingsRow("Pure Black", "oled amoled") {},
        SettingsHeading("Layout"),
        SettingsRow("Corners", "radius") {},
    ))
    private val data = SettingsSection("data", "Backup & Reset", Sym.Save, "", listOf(SettingsRow("Export Settings", "backup") {}))

    @Test fun findsByTitleKeywordOrHeading() {
        assertEquals(listOf("Pure Black"), searchSettings(listOf(look, data), "amoled").single().second.map { it.row.label })
        assertEquals(listOf("Theme", "Pure Black"), searchSettings(listOf(look), "colors").single().second.map { it.row.label })
        assertEquals("Colors", searchSettings(listOf(look), "theme").single().second.single().heading)
    }

    @Test fun aSectionTitleFindsAllItsRows() {
        assertEquals(3, searchSettings(listOf(look, data), "look").single().second.size)
    }

    @Test fun nothingAndBlank() {
        assertTrue(searchSettings(listOf(look, data), "zzz").isEmpty())
        assertTrue(searchSettings(listOf(look, data), "  ").isEmpty())
    }

    @Test fun headingsStartNewBlocks() {
        val placed = placeRows(look.rows).filter { !it.row.header }
        assertEquals(listOf(0 to 2, 1 to 2, 0 to 1), placed.map { it.index to it.count })
    }
}
