// Settings › The Pill: what shows above it while typing.
package com.jlees.mibudget.ui.settings

import com.jlees.mibudget.ui.common.Sym

fun pillSection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("pill", "Pill", Sym.Toolbar, "How the Pill behaves", listOf(
        SettingsHeading("While Typing"),
        SettingsRow("Recent Searches", "history past searches") {
            SwitchItem(Sym.History, "Recent Searches", "Your past searches appear above the Pill", s.recentSearches) { v ->
                c.set { it.copy(recentSearches = v, searchHistory = if (v) it.searchHistory else emptyList()) }
            }
        },
    ))
}
