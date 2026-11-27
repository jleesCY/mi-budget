// Settings › Recently Deleted: how long deleted purchases and subscriptions are kept, and emptying it.
package com.jlees.mibudget.ui.settings

import com.jlees.mibudget.data.KeepDeleted
import com.jlees.mibudget.ui.common.Sym

fun recentlyDeletedSection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("deleted", "Recently Deleted", Sym.RestoreFromTrash, "How deleted things are kept", buildList {
        add(SettingsRow("Recently Deleted", "trash restore undo deleted bin") {
            LinkItem(Sym.RestoreFromTrash, "Recently Deleted", if (c.trashCount == 0) "Nothing deleted" else "${com.jlees.mibudget.domain.plural(c.trashCount, "item")}", onClick = c.openRecentlyDeleted)
        })
        add(SettingsRow("Keep Deleted Items", "days week month trash how long") {
            ChoiceItem(Sym.Schedule, "Keep Deleted Items", "How long deleted things stay in Recently Deleted", KeepDeleted.entries.map { it to it.label }, s.keepDeleted) { v -> c.set { it.copy(keepDeleted = v) } }
        })
        if (c.trashCount > 0) add(SettingsRow("Empty Recently Deleted", "clear trash delete forever") {
            Item(Sym.Delete, "Empty Recently Deleted", "Delete everything in it for good", danger = true, onClick = c.askEmptyTrash)
        })
    })
}
