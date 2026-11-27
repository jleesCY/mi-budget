// Settings › Start Screen: the month's chart, and an optional clock and date above it.
package com.jlees.mibudget.ui.settings

import com.jlees.mibudget.data.ChartType
import com.jlees.mibudget.data.ClockStyle
import com.jlees.mibudget.data.DatePlacement
import com.jlees.mibudget.ui.common.ConnectedIconChoice
import com.jlees.mibudget.ui.common.Sym
import com.jlees.mibudget.ui.components.chartIcon

fun startScreenSection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("home", "Start Screen", Sym.Home, "How the start screen appears", listOf(
        SettingsRow("Chart", "donut pie rings bars trend graph spending") {
            // five choices: icon-only, the chosen one shows its name
            Item(Sym.DonutLarge, "Chart", "How this month's spending is drawn", below = {
                ConnectedIconChoice(ChartType.entries.map { Triple(it, it.label, chartIcon(it)) }, s.chartType) { t -> c.set { it.copy(chartType = t) } }
            })
        },
        SettingsRow("Clock", "time 12 24 hour") {
            ChoiceItem(Sym.History, "Clock", null, ClockStyle.entries.map { it to it.label }, s.clock) { v -> c.set { it.copy(clock = v) } }
        },
        SettingsRow("Date", "today day above below") {
            ChoiceItem(Sym.CalendarToday, "Date", null, DatePlacement.entries.map { it to it.label }, s.date) { v -> c.set { it.copy(date = v) } }
        },
    ))
}
