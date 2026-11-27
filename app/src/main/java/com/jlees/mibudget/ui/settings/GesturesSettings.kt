// Settings › Gestures: the optional gestures, by where they're done (the Pill, the start screen). Every gesture also has a button.
package com.jlees.mibudget.ui.settings

import com.jlees.mibudget.ui.common.Sym

fun gesturesSection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("gestures", "Gestures", Sym.Vibration, "How gestures work throughout mi. Budget", listOf(
        SettingsHeading("Pill"),
        SettingsRow("Long Press to Search", "hold search purchases") {
            SwitchItem(Sym.Search, "Long Press to Search", "Hold the Pill to search your purchases", s.longPressSearch) { v -> c.set { it.copy(longPressSearch = v) } }
        },
        SettingsHeading("Start Screen"),
        SettingsRow("Swipe Between Months", "month previous next sideways chart title") {
            SwitchItem(Sym.Swipe, "Swipe Between Months", "Swipe the month or the chart sideways to change month", s.swipeMonths) { v -> c.set { it.copy(swipeMonths = v) } }
        },
    ))
}
