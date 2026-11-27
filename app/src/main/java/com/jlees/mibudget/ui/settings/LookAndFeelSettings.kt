// Settings › Look & Feel: colours, text, layout and haptics.
package com.jlees.mibudget.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.jlees.mibudget.data.AppSettings
import com.jlees.mibudget.data.DYNAMIC_ACCENT
import com.jlees.mibudget.data.PanArea
import com.jlees.mibudget.data.ThemeMode
import com.jlees.mibudget.data.hexColor
import com.jlees.mibudget.ui.common.Sym

fun lookAndFeelSection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("look", "Look & Feel", Sym.Palette, "Colors, UI and haptics", buildList {
        add(SettingsHeading("Colors"))
        add(SettingsRow("Theme", "light dark system mode") {
            ChoiceItem(Sym.Routine, "Theme", null, ThemeMode.entries.map { it to it.label }, s.themeMode) { m -> c.set { it.copy(themeMode = m) } }
        })
        // Pure Black only matters in dark: hidden in Light
        if (s.themeMode != ThemeMode.LIGHT) add(SettingsRow("Pure Black", "oled amoled black dark battery") {
            SwitchItem(Sym.Contrast, "Pure Black", "True black backgrounds in dark mode", s.pureBlack) { v -> c.set { it.copy(pureBlack = v) } }
        })
        add(SettingsRow("Accent Color", "colour wallpaper material you dynamic adaptive green") {
            // Adaptive (the wallpaper's colours) first, on Android 12+ only (before that Android has none); then mi.'s seeds
            val adaptive = android.os.Build.VERSION.SDK_INT >= 31
            val swatches = (if (adaptive) listOf(Triple(MaterialTheme.colorScheme.tertiaryContainer, "Adaptive", Sym.Wallpaper)) else emptyList()) +
                AppSettings.ACCENTS.mapIndexed { i, hex -> Triple(Color(hexColor(hex)), AppSettings.ACCENT_NAMES[i], null) }
            val first = if (adaptive) 1 else 0
            // (Adaptive kept from a newer phone, on an older one: the app's own colour is shown chosen)
            val chosen = if (s.accentColor == DYNAMIC_ACCENT) (if (adaptive) 0 else AppSettings.ACCENTS.indexOf(com.jlees.mibudget.data.APP_ACCENT)) else AppSettings.ACCENTS.indexOf(s.accentColor.uppercase()) + first
            Item(Sym.Palette, "Accent Color", if (adaptive) "Based on your wallpaper, or choose your own" else null, below = {
                SwatchRow(swatches, chosen) { i -> c.set { it.copy(accentColor = if (adaptive && i == 0) DYNAMIC_ACCENT else AppSettings.ACCENTS[i - first]) } }
            })
        })
        add(SettingsHeading("Text"))
        add(SettingsRow("Font Size", "text scale app larger smaller") {
            SliderItem(Sym.FormatSize, "Font Size", "UI font size throughout mi. Budget", s.fontScale, 0.8f, 1.2f) { v -> c.set { it.copy(fontScale = v) } }
        })
        add(SettingsRow("Font", "typeface rounded system google sans flex") {
            ChoiceItem(Sym.FontDownload, "Font", null, com.jlees.mibudget.data.AppFont.entries.map { it to it.label }, s.font) { f -> c.set { it.copy(font = f) } }
        })
        add(SettingsHeading("Overflowed Text"))
        add(SettingsRow("Pan Overflowed Text", "pan long text scroll marquee overflow cut off ellipsis slide names") {
            SwitchItem(Sym.ArrowForward, "Pan Overflowed Text", "Text that doesn't fit slides to show the rest", s.panLongText) { v -> c.set { it.copy(panLongText = v) } }
        })
        // where it pans: only shown with Pan Overflowed Text on
        if (s.panLongText) PanArea.entries.forEach { area ->
            add(SettingsRow(area.label, "pan overflowed long text scroll marquee ${area.label.lowercase()}") {
                val icon = when (area) {
                    PanArea.SETTINGS -> Sym.Settings; PanArea.PURCHASES -> Sym.ReceiptLong; PanArea.SUBSCRIPTIONS -> Sym.Autorenew
                    PanArea.BUDGETS -> Sym.Savings; PanArea.DELETED -> Sym.RestoreFromTrash
                }
                SwitchItem(icon, area.label, null, area in s.panAreas) { on -> c.set { it.copy(panAreas = if (on) it.panAreas + area else it.panAreas - area) } }
            })
        }
        add(SettingsHeading("Layout"))
        add(SettingsRow("Corners", "radius shape round square") {
            ChoiceItem(Sym.RoundedCorner, "Corners", null, listOf(0 to "Square", 12 to "Soft", 28 to "Round"), s.cornerRadius) { r -> c.set { it.copy(cornerRadius = r) } }
        })
        add(SettingsRow("List Spacing", "spacing density padding compact airy") {
            ChoiceItem(Sym.DensityMedium, "List Spacing", "How much space between list items", com.jlees.mibudget.data.Spacing.entries.map { it to it.label }, s.uiPadding) { v -> c.set { it.copy(uiPadding = v) } }
        })
        add(SettingsRow("List Style", "table cards rows plain flat purchases subscriptions layout") {
            ChoiceItem(Sym.List, "List Style", "How your lists look", com.jlees.mibudget.data.ListStyle.entries.map { it to it.label }, s.listStyle) { v -> c.set { it.copy(listStyle = v) } }
        })
        add(SettingsRow("Rows Loaded at Once", "lazy loading chunk batch performance smooth long lists more scroll") {
            val chunks = com.jlees.mibudget.data.LIST_CHUNKS
            StepSliderItem(
                Sym.List, "Rows Loaded at Once", "Long lists load this many rows at a time, and more as you scroll",
                chunks.indexOf(s.listChunk).coerceAtLeast(0), chunks.indices, label = { "${chunks[it]} rows" },
                onChange = { i -> c.set { it.copy(listChunk = chunks[i]) } },
            )
        })
        add(SettingsRow("Status Bar", "clock battery immersive full screen") {
            SwitchItem(Sym.BatteryChargingFull, "Status Bar", null, s.showStatusBar) { v -> c.set { it.copy(showStatusBar = v) } }
        })
        add(SettingsRow("Rotate Screen", "landscape portrait orientation sideways auto rotate lock") {
            SwitchItem(Sym.ScreenRotation, "Rotate Screen", "Turn mi. Budget sideways with your phone", s.rotateScreen) { v -> c.set { it.copy(rotateScreen = v) } }
        })
        add(SettingsHeading("Feedback"))
        add(SettingsRow("Gesture Haptics", "haptic feedback vibration vibrate haptics touch feel") {
            SwitchItem(Sym.Vibration, "Gesture Haptics", "Haptic feedback when gestures are performed", s.hapticFeedback) { v -> c.set { it.copy(hapticFeedback = v) } }
        })
    })
}
