// Settings › Backup & Reset: a file you keep (no cloud, no account), and starting over.
package com.jlees.mibudget.ui.settings

import com.jlees.mibudget.ui.common.Sym

fun backupSection(c: SettingsCtx): SettingsSection = SettingsSection("data", "Backup & Reset", Sym.Save, "Back up, restore, reset", buildList {
    add(SettingsHeading("Backup"))
    add(SettingsRow("Back Up", "backup save export file copy export settings") {
        Item(Sym.Upload, "Back Up", "Save your purchases, subscriptions and settings to a file", onClick = c.exportBackup)
    })
    add(SettingsRow("Restore", "restore backup import file budgey import settings") {
        LinkItem(Sym.Download, "Restore", "Bring back everything from a backup file", onClick = c.openImport)
    })
    add(SettingsHeading("Reset"))
    add(SettingsRow("Reset All Settings", "defaults reset settings") {
        Item(Sym.RestartAlt, "Reset All Settings", "Back to defaults; your purchases are kept", onClick = c.askResetSettings)
    })
    // a wipe is kept on the phone for a week, in case it was a mistake
    c.safety?.let { info ->
        add(SettingsRow("Restore Wiped Data", "safety copy undo erase wipe") {
            Item(Sym.Restore, "Restore Wiped Data", "A copy from ${info.label}, kept ${com.jlees.mibudget.domain.plural(info.daysLeft, "more day")}", onClick = c.askRestoreSafety)
        })
        add(SettingsRow("Delete Safety Copy", "remove safety copy")
        { Item(Sym.DeleteForever, "Delete Safety Copy", "Remove it now instead of in ${com.jlees.mibudget.domain.plural(info.daysLeft, "day")}", danger = true, onClick = c.askDeleteSafety) })
    }
    add(SettingsRow("Wipe All Data", "erase delete everything reset start over") {
        Item(Sym.Delete, "Wipe All Data", "Delete every purchase, subscription and setting", danger = true, onClick = c.askWipe)
    })
})
