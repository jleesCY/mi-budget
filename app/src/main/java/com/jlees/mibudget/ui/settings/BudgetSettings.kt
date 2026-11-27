// Settings › Budgeting, Notifications, Scanner and Tools: mi. Budget's own sections.
package com.jlees.mibudget.ui.settings

import androidx.compose.foundation.layout.size

import com.jlees.mibudget.data.WeekStart
import com.jlees.mibudget.scan.ScanEngine
import com.jlees.mibudget.ui.common.Sym

fun budgetingSection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("budgeting", "Budgeting", Sym.Savings, "How budgets and weeks work", listOf(
        SettingsRow("Week Starts On", "sunday monday first day") {
            ChoiceItem(Sym.CalendarViewWeek, "Week Starts On", null, WeekStart.entries.map { it to it.label }, s.weekStart) { d -> c.set { it.copy(weekStart = d) } }
        },
        SettingsRow("Uncategorized Reminder", "nudge banner category") {
            SwitchItem(Sym.Lightbulb, "Uncategorized Reminder", "A banner when purchases need a category", s.nudgeUncategorized) { v -> c.set { it.copy(nudgeUncategorized = v) } }
        },
        SettingsRow("Payment Methods", "cards bank accounts wallets") {
            LinkItem(Sym.CreditCard, "Payment Methods", "Cards, bank accounts and wallets", onClick = c.openPaymentMethods)
        },
    ))
}

fun notificationsSection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("notifications", "Notifications", Sym.Notifications, "How reminders behave", buildList {
        add(SettingsRow("Renewal Reminders", "subscriptions trial notify bill due") {
            SwitchItem(Sym.NotificationsActive, "Renewal Reminders", "Before subscriptions renew and free trials end", s.renewalReminders) { v ->
                c.set { it.copy(renewalReminders = v) }
                if (v && !c.permission.granted) c.permission.request()
            }
        })
        // the rest only apply with reminders on
        if (s.renewalReminders) {
            if (!c.permission.granted) add(SettingsRow("Notifications Are Blocked", "permission allow") {
                Item(Sym.NotificationsOff, "Notifications Are Blocked", "Tap to allow notifications", danger = true, onClick = c.permission::request)
            })
            add(SettingsRow("Remind Me", "days before same day week") {
                // a slider through the days ahead (the same day to a week), the choice at the row's end
                StepSliderItem(
                    Sym.Schedule, "Remind Me", "How long before a renewal",
                    value = s.reminderDaysBefore.coerceIn(0, 7), range = 0..7,
                    label = { d -> when (d) { 0 -> "Same day"; 1 -> "1 day"; else -> "$d days" } },
                    onChange = { d -> c.set { it.copy(reminderDaysBefore = d) } },
                    // the reminders are worked out again once, when the finger lets go
                    onDone = { c.vm.checkRemindersNow() },
                )
            })
        }
    })
}

fun scannerSection(c: SettingsCtx): SettingsSection {
    val s = c.s
    return SettingsSection("scanner", "Scanner", Sym.DocumentScanner, "How receipts are read", buildList {
        add(SettingsHeading("Scanner"))
        c.engines.forEach { row ->
            add(SettingsRow(row.engine.title, "scanner engine model ai gemini nano standard ml kit") {
                EngineItem(
                    row = row, selected = s.scanEngine == row.engine,
                    nanoMessage = if (row.engine == ScanEngine.GEMINI_NANO) c.nanoMessage else null,
                    onSelect = { c.vm.selectEngine(row.engine) },
                )
            })
        }
    })
}

fun toolsSection(c: SettingsCtx): SettingsSection {
    val f = c.fx
    val hasPack = f?.source == com.jlees.mibudget.data.FxRepository.Source.PACK
    return SettingsSection("tools", "Tools", Sym.Handyman, "How the currency converter gets its rates", buildList {
        add(SettingsRow("Built-in Rates", "currency exchange offline") {
            Item(Sym.CurrencyExchange, "Built-in Rates", when {
                f == null -> "Loading…"
                hasPack -> "Used if the update pack is deleted"
                else -> "In use · from ${fxDate(f.table.date)} · ${f.table.currencies.size} currencies"
            })
        })
        add(SettingsRow("Daily Rate Updates", "currency download pack frankfurter") {
            Item(Sym.Update, "Daily Rate Updates", when {
                c.fxBusy -> "Downloading…"
                hasPack && f != null -> "From ${fxDate(f.table.date)} · updates daily when you're online"
                else -> "Keep rates current with a 10 KB download"
            }, trailing = {
                when {
                    c.fxBusy -> androidx.compose.material3.CircularProgressIndicator(androidx.compose.ui.Modifier.size(androidx.compose.ui.unit.Dp(24f)), strokeWidth = androidx.compose.ui.unit.Dp(2f))
                    hasPack -> androidx.compose.material3.IconButton(onClick = c.askDeleteFx) { androidx.compose.material3.Icon(Sym.DeleteOutline, "Delete Update Pack") }
                    else -> androidx.compose.material3.IconButton(onClick = { c.vm.downloadFxPack() }) { androidx.compose.material3.Icon(Sym.Download, "Download Update Pack") }
                }
            })
        })
        if (hasPack) add(SettingsRow("Update Now", "refresh rates today") { Item(Sym.Refresh, "Update Now", "Get today's rates", onClick = { c.vm.downloadFxPack() }) })
    })
}


/** "2026-10-02" → "Oct 2, 2026" (or as it is, if it isn't a date). */
internal fun fxDate(iso: String): String = runCatching {
    java.time.LocalDate.parse(iso).format(com.jlees.mibudget.ui.components.MediumDate)
}.getOrDefault(iso)
