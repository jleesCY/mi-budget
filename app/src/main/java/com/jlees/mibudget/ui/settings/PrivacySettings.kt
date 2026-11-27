// Settings › Privacy & Security: finances are private, so the app can stay out of screenshots and Recents.
package com.jlees.mibudget.ui.settings

import com.jlees.mibudget.ui.common.Sym

fun privacySection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("privacy", "Privacy & Security", Sym.Shield, "How mi. Budget keeps your finances private", listOf(
        SettingsRow("Hide From Screenshots", "recents secure private screen flag") {
            SwitchItem(Sym.VisibilityOff, "Hide From Screenshots", "Keep mi. Budget out of screenshots and Recents", s.hideFromScreenshots) { v -> c.set { it.copy(hideFromScreenshots = v) } }
        },
    ))
}
