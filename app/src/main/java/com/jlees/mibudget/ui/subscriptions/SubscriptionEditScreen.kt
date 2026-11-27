package com.jlees.mibudget.ui.subscriptions

import com.jlees.mibudget.ui.common.scrollingPage
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.jlees.mibudget.ui.common.MiDialog
import com.jlees.mibudget.ui.common.MiBottomPanel

import com.jlees.mibudget.ui.common.Sym
import com.jlees.mibudget.ui.common.FormGap
import com.jlees.mibudget.ui.common.FormHeading
import com.jlees.mibudget.ui.common.FormNote
import com.jlees.mibudget.ui.common.bleed

import androidx.compose.ui.draw.clip

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.FilledTonalButton
import com.jlees.mibudget.data.db.displayName
import com.jlees.mibudget.ui.components.DetailRow
import com.jlees.mibudget.ui.components.PreviewHeader
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.text.style.TextOverflow
import com.jlees.mibudget.data.db.SubscriptionPeriodEntity
import com.jlees.mibudget.data.db.cycle
import java.time.LocalDate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.data.db.SubscriptionStatus
import com.jlees.mibudget.domain.BillingCycle
import com.jlees.mibudget.domain.CycleUnit
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.domain.PriceField
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.components.AmountField
import com.jlees.mibudget.ui.components.ItemIconSheet
import com.jlees.mibudget.ui.components.IconSheetMode
import com.jlees.mibudget.ui.components.CategoryField
import com.jlees.mibudget.ui.common.ConfirmDialog
import com.jlees.mibudget.ui.components.DateField
import com.jlees.mibudget.ui.components.MediumDate
import com.jlees.mibudget.ui.components.ItemIcon
import com.jlees.mibudget.ui.components.ReceiptSection
import com.jlees.mibudget.ui.components.PaymentMethodField

@Composable
fun SubscriptionEditScreen(
    onBack: () -> Unit,
    onOpenPurchase: (String) -> Unit,
    vm: SubscriptionEditViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val form by vm.form.collectAsStateWithLifecycle()
    val notFound by vm.notFound.collectAsStateWithLifecycle()
    if (notFound) {
        com.jlees.mibudget.ui.components.ItemNotFound("Subscription Not Found", onBack)
        return
    }
    val tree by vm.tree.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    var brandPicker by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var showScanText by rememberSaveable { mutableStateOf(false) }
    var customCycle by rememberSaveable { mutableStateOf(false) }
    var showResume by rememberSaveable { mutableStateOf(false) }
    var editingPeriod by rememberSaveable { mutableStateOf<SubscriptionPeriodEntity?>(null) }
    val syncPrompt by vm.syncPrompt.collectAsStateWithLifecycle()
    val allMethods by vm.paymentMethods.collectAsStateWithLifecycle()
    // Existing subscriptions open as a read-only preview; the Edit button switches to the editor.
    var editing by rememberSaveable { mutableStateOf(vm.isNew) }
    val leaveEditing: () -> Unit = { vm.reload(); editing = false }
    // After saving an existing one, go back to its (refreshed) preview.
    val afterSave: () -> Unit = {
        if (vm.isNew) onBack() else { vm.reload(); editing = false }
        Unit
    }
    val currentAfterSave by androidx.compose.runtime.rememberUpdatedState(afterSave)
    androidx.compose.runtime.LaunchedEffect(vm) { vm.saved.collect { currentAfterSave() } }

    // the summary and the editor are two pages: the editor slides in over the summary, and Back slides it away with
    // the finger (the system gesture or the Pill's swipe)
    val page: @Composable (Boolean) -> Unit = { asEditor ->
        com.jlees.mibudget.ui.common.MiScaffold(
            title = if (vm.isNew) "New Subscription" else if (asEditor) "Edit Subscription" else "Subscription",
            onBack = { if (asEditor && !vm.isNew) leaveEditing() else onBack() },
            // its actions (Edit, Delete; or Save) are the split Pill at the bottom, below. Leaving a new one keeps it for
            // "Resume"; the Pill's trash throws it away.
        ) { padding ->
            if (!asEditor) {
                SubscriptionPreview(
                    form, tree, history, allMethods,
                    receipt = form.receiptFile?.let(vm::receiptFile),
                    onOpenPurchase = onOpenPurchase,
                    modifier = Modifier.padding(top = padding.calculateTopPadding()), bottom = padding.calculateBottomPadding(),
                )
                return@MiScaffold
            }
            Column(
                Modifier.fillMaxSize().scrollingPage(padding, rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = com.jlees.mibudget.ui.common.PillSpace),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (form.fromScan) com.jlees.mibudget.ui.common.FormBanner(
                    Sym.DocumentScanner, "Filled In From Your Scan", "Check it over before you save.",
                    action = if (form.scanText != null) "View Text" else null, onAction = { showScanText = true },
                )

                // laid out as Settings: each section's rows one segmented block under its heading
                com.jlees.mibudget.ui.common.FormGroup {
                    // the service, with its picture (picked automatically as you type; tap it to choose another)
                    com.jlees.mibudget.ui.common.LabeledField(
                        label = "Service", value = form.name, onChange = vm::setName, placeholder = "Netflix, Spotify…",
                        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        leading = {
                            com.jlees.mibudget.ui.common.IconTile(onClick = { brandPicker = true }) {
                                ItemIcon(form.brandKey, form.name.ifBlank { "?" }, form.categoryId?.let { tree.byId[it] }, size = 56.dp)
                            }
                        },
                    )
                    CategoryField(tree, form.categoryId, vm::setCategory, Modifier.fillMaxWidth())
                }
                val suggested = form.suggestedCategoryId
                if (suggested != null && suggested != form.categoryId) {
                    com.jlees.mibudget.ui.common.MiChip("Suggested: ${tree.byId[suggested]?.name ?: ""}", false, leading = Sym.AutoAwesome, role = androidx.compose.ui.semantics.Role.Button) { vm.setCategory(suggested) }
                }

                PriceSection(form, vm)

                FormHeading("Billing")
                val pastCount by produceState(0, form.anchorDate, form.cycleUnit, form.cycleCount, form.autoLog, form.status, form.trialEndDate, form.price) {
                    value = vm.pastPaymentCount()
                }
                com.jlees.mibudget.ui.common.FormGroup {
                    val custom = customCycle || form.cycle !in BillingCycle.presets
                    com.jlees.mibudget.ui.common.FormRow(
                        Sym.Autorenew, "Billing Cycle",
                        subtitle = form.amountCents?.takeIf { form.cycle != BillingCycle.MONTHLY }?.let { cents ->
                            "≈ ${Money.format(form.cycle.monthlyCost(cents))} per month · ${Money.format(form.cycle.yearlyCost(cents))} per year"
                        },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.jlees.mibudget.ui.common.ChipChoice(
                                BillingCycle.presets.map { it to it.label } + (null to "Custom"),
                                if (custom) null else form.cycle,
                                { p -> if (p == null) customCycle = true else { customCycle = false; vm.setCycle(p) } },
                            )
                            if (custom) CustomCycleRow(form.cycle, vm::setCycle)
                        }
                    }
                    DateField(if (form.status == SubscriptionStatus.TRIAL) "Started on (trial began)" else "Started on (first payment)", form.anchorDate, { it?.let(vm::setAnchor) }, Modifier.fillMaxWidth())
                    com.jlees.mibudget.ui.common.SwitchField(
                        "Next Payment Automatically", !form.nextDueOverridden, vm::setAutoNextDue,
                        subtitle = if (!form.nextDueOverridden) "${form.nextDueDate.format(MediumDate)} · ${dueLabel(form.nextDueDate).removePrefix("renews ")}"
                        else "Pick the next payment date yourself",
                        icon = Sym.Event,
                    )
                    if (form.nextDueOverridden) {
                        DateField("Next payment", form.nextDueDate, { it?.let(vm::setNextDue) }, Modifier.fillMaxWidth())
                    }
                }
                if (vm.isNew && pastCount > 0) FormNote(
                    "${com.jlees.mibudget.domain.plural(pastCount, "past payment")} since the start date will be added to your purchases" +
                        (form.amountCents?.let { " (${Money.format(it * pastCount)} total)" } ?: "") + ".",
                )

                FormHeading("Status")
                val stopped = form.status == SubscriptionStatus.PAUSED || form.status == SubscriptionStatus.CANCELLED
                com.jlees.mibudget.ui.common.FormGroup {
                    com.jlees.mibudget.ui.common.FormRow(Sym.Routine, "Status", subtitle = if (stopped) "Billing stops after the date below. Nothing before it changes." else null) {
                        com.jlees.mibudget.ui.common.ConnectedChoice(SubscriptionStatus.entries.map { it to it.label }, form.status) { s ->
                            val goingLive = s == SubscriptionStatus.ACTIVE || s == SubscriptionStatus.TRIAL
                            // Turning a stopped subscription back on = resume on a date (keeps the past intact).
                            if (goingLive && vm.needsResume) showResume = true else vm.setStatus(s)
                        }
                    }
                    if (stopped) DateField("Last billed / stopped on", form.endDate, { it?.let(vm::setEndDate) }, Modifier.fillMaxWidth())
                    if (form.status == SubscriptionStatus.TRIAL) DateField("Trial ends", form.trialEndDate, vm::setTrialEnd, Modifier.fillMaxWidth(), allowClear = true)
                }
                if (stopped && !vm.isNew) FilledTonalButton(onClick = { showResume = true }) {
                    Icon(Sym.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Resume on a date…")
                }

                FormHeading("Payments")
                val defaultDays by vm.defaultReminderDays.collectAsStateWithLifecycle()
                val methods by vm.paymentMethods.collectAsStateWithLifecycle()
                com.jlees.mibudget.ui.common.FormGroup {
                    com.jlees.mibudget.ui.common.SwitchField(
                        "Auto-log Payments", form.autoLog, vm::setAutoLog,
                        subtitle = "A purchase on each billing date, so it counts toward your budgets", icon = Sym.Payments,
                    )
                    com.jlees.mibudget.ui.common.FormRow(Sym.Notifications, "Renewal Reminder") {
                        com.jlees.mibudget.ui.common.ChipChoice(
                            listOf<Pair<Int?, String>>(
                                null to "Default (${daysLabel(defaultDays)})",
                                0 to "On the day", 1 to "1 day before", 3 to "3 days before", 7 to "1 week before", -1 to "Off",
                            ),
                            form.reminderDays, vm::setReminderDays,
                        )
                    }
                    PaymentMethodField(
                        methods = methods,
                        selectedId = form.paymentMethodId,
                        legacyName = form.paymentMethod,
                        onSelect = vm::setPaymentMethodId,
                        onCreate = vm::createPaymentMethod,
                        importIcon = vm::importPaymentIcon,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    com.jlees.mibudget.ui.common.LabeledField(label = "Notes", value = form.note, onChange = vm::setNote, placeholder = "Account email, plan, how to cancel…", modifier = Modifier.fillMaxWidth(), minLines = 1, singleLine = false, icon = Sym.Notes)
                    ReceiptSection(form.receiptFile?.let(vm::receiptFile), vm::attachReceipt, vm::removeReceipt)
                }

                PriceHistorySection(form, onEdit = { editingPeriod = it }, onAdd = {
                    editingPeriod = SubscriptionPeriodEntity(
                        subscriptionId = form.id ?: "",
                        startDate = form.anchorDate.minusMonths(6),
                        endDate = form.anchorDate.minusDays(1),
                        amountCents = form.amountCents ?: 0,
                        cycleUnit = form.cycleUnit,
                        cycleCount = form.cycleCount,
                    )
                })

                if (!vm.isNew) {
                    FormHeading("Payment History")
                    FormNote("${com.jlees.mibudget.domain.plural(history.count, "payment")} · ${Money.format(history.total)} total")
                    // the same rows as every purchase list
                    history.recent.forEach { p ->
                        com.jlees.mibudget.ui.purchases.PurchaseListItem(
                            p, p.categoryId?.let { tree.byId[it] }, method = null, onClick = { onOpenPurchase(p.id) }, modifier = Modifier.bleed(),
                        )
                    }
                    FilledTonalButton(onClick = { vm.logPaymentNow() }) {
                        Icon(Sym.Payments, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log a Payment")
                    }
                }
            }
        }
    }
    com.jlees.mibudget.ui.common.EditorPages(editing, canLeave = !vm.isNew, onLeave = leaveEditing, summary = { page(false) }, editor = { page(true) })

    if (brandPicker) {
        ItemIconSheet(
            mode = IconSheetMode.MERCHANT,
            name = form.name,
            current = form.brandKey,
            category = form.categoryId?.let { tree.byId[it] },
            importImage = vm::importPaymentIcon,
            onSelect = { ref, brand -> vm.setIcon(ref, brand); brandPicker = false },
            onDismiss = { brandPicker = false },
        )
    }
    // the page's split Pill, grown out of the list page's Pill: Delete | Edit on the summary; Delete (or Discard, for a
    // new one) | Save while editing. Delete is always at the far left, the main action at the far right. Swiping it right goes back (out of editing first).
    com.jlees.mibudget.ui.common.ActionPill(com.jlees.mibudget.ui.common.ActionPillSpec(
        segments = if (!editing) listOf(
            com.jlees.mibudget.ui.common.PillSegment("Delete", Sym.Delete, danger = true) { vm.delete(onBack) },
            com.jlees.mibudget.ui.common.PillSegment("Edit", Sym.Edit, main = true) { editing = true },
        ) else listOf(
            if (vm.isNew) com.jlees.mibudget.ui.common.PillSegment("Discard This Subscription", Sym.Delete, danger = true) { confirmDiscard = true }
            else com.jlees.mibudget.ui.common.PillSegment("Delete", Sym.Delete, danger = true) { vm.delete(onBack) },
            com.jlees.mibudget.ui.common.PillSegment(if (vm.isNew) "Add Subscription" else "Save Changes", Sym.Check, main = true, enabled = form.canSave) { vm.save() },
        ),
        onBack = { if (editing && !vm.isNew) leaveEditing() else onBack() },
        backPops = !(editing && !vm.isNew),
    ))
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard This Subscription?",
            text = "What you've entered (and any scanned picture) will be deleted. This can't be undone.",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = { confirmDiscard = false; vm.discard(onBack) },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (showResume) {
        ResumeDialog(
            currentTotal = form.price.total,
            onDismiss = { showResume = false },
            onResume = { date, total -> vm.resume(date, total); showResume = false },
        )
    }
    editingPeriod?.let { p ->
        PeriodSheet(
            initial = p,
            isNew = form.periods.none { it.id == p.id },
            currentStart = form.anchorDate,
            onSave = { vm.upsertPeriod(it); editingPeriod = null },
            onDelete = { vm.removePeriod(p.id); editingPeriod = null },
            onDismiss = { editingPeriod = null },
        )
    }

    syncPrompt?.let { p ->
        MiDialog(
            onDismissRequest = vm::cancelSync,
            title = { Text("Update Past Payments?") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("This subscription already has logged payments. Apply your changes to them too?")
                    Spacer(Modifier.height(4.dp))
                    p.changes.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "\"Only future\" saves the subscription but leaves every past payment exactly as it is.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { vm.answerSync(applyToPast = true) }) { Text("Update Past") } },
            dismissButton = { TextButton(onClick = { vm.answerSync(applyToPast = false) }) { Text("Only Future") } },
        )
    }
    if (showScanText) com.jlees.mibudget.ui.components.ScanTextDialog(
        listOf(com.jlees.mibudget.ui.components.ScanTextSection("Google's text reader", form.scanText ?: "")),
        onDismiss = { showScanText = false },
    )
}

/**
 * Three linked boxes: base price + fees & taxes = total charged.
 * Base or fees edited → total recalculates. Total edited → fees recalculate. Base is never overwritten.
 */
@Composable
private fun PriceSection(form: SubscriptionForm, vm: SubscriptionEditViewModel) {
    val p = form.price
    fun hint(f: PriceField) = if (p.derived == f && p.text(f).isNotBlank()) "Calculated" else null
    FormHeading("Price")
    com.jlees.mibudget.ui.common.FormGroup {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AmountField(
                p.base, { vm.setPrice(PriceField.BASE, it) }, Modifier.weight(1f),
                label = "Base price", large = false, supportingText = hint(PriceField.BASE), imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            )
            AmountField(
                p.fees, { vm.setPrice(PriceField.FEES, it) }, Modifier.weight(1f),
                label = "+ Fees & taxes", large = false, supportingText = hint(PriceField.FEES), imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            )
        }
        AmountField(
            p.total, { vm.setPrice(PriceField.TOTAL, it) }, Modifier.fillMaxWidth(),
            label = "Total charged ${form.cycle.perLabel}",
            supportingText = hint(PriceField.TOTAL) ?: "What's actually charged (this gets logged)",
            icon = Sym.Payments,
        )
    }
}

/** Current period + earlier periods (e.g. student pricing), with "Add Earlier Period". */
@Composable
private fun PriceHistorySection(form: SubscriptionForm, onEdit: (SubscriptionPeriodEntity) -> Unit, onAdd: () -> Unit) {
    FormHeading("Price History")
    FormNote("Earlier stretches with a different price or cycle. Past payments are logged at those prices.")
    val stopped = form.status == SubscriptionStatus.PAUSED || form.status == SubscriptionStatus.CANCELLED
    PeriodRow(
        title = "${form.amountCents?.let(Money::format) ?: "—"}${form.cycle.shortSuffix}",
        subtitle = "Current · from ${form.anchorDate.format(MediumDate)}" +
            (if (stopped && form.endDate != null) " to ${form.endDate.format(MediumDate)}" else " on"),
        highlight = true,
        onClick = null,
    )
    form.periods.sortedByDescending { it.startDate }.forEach { p ->
        PeriodRow(
            title = "${Money.format(p.amountCents)}${p.cycle.shortSuffix}" + if (p.label.isNotBlank()) " · ${p.label}" else "",
            subtitle = "${p.startDate.format(MediumDate)} – ${p.endDate.format(MediumDate)}",
            highlight = false,
            onClick = { onEdit(p) },
        )
    }
    FilledTonalButton(onClick = onAdd) {
        Icon(Sym.History, null, Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add Earlier Period")
    }
}

@Composable
private fun PeriodRow(title: String, subtitle: String, highlight: Boolean, onClick: (() -> Unit)?) {
    // a mi. list row: the price, "Current · from …" (the current one in primary), ✎ on the ones that can change
    val cs = MaterialTheme.colorScheme
    com.jlees.mibudget.ui.common.MiListRow(
        title = title,
        // the current stretch leads with "Current" in primary; earlier ones are just their dates
        lead = if (highlight) androidx.compose.ui.text.AnnotatedString("Current") else null,
        details = androidx.compose.ui.text.AnnotatedString(if (highlight) subtitle.removePrefix("Current · ") else subtitle),
        leading = {
            Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (highlight) cs.primaryContainer else cs.surfaceContainerLow), contentAlignment = Alignment.Center) {
                Icon(if (highlight) Sym.Autorenew else Sym.History, null, Modifier.size(22.dp), tint = if (highlight) cs.onPrimaryContainer else cs.onSurfaceVariant)
            }
        },
        modifier = Modifier.bleed(),
        onClick = onClick,
        trailing = if (onClick != null) { { Icon(Sym.Edit, "Edit", Modifier.size(20.dp), tint = cs.onSurfaceVariant) } } else null,
    )
}

/** Resume a paused/cancelled subscription on a date, optionally at a new price. */
@Composable
private fun ResumeDialog(currentTotal: String, onDismiss: () -> Unit, onResume: (LocalDate, String?) -> Unit) {
    var date by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var total by rememberSaveable { mutableStateOf(currentTotal) }
    MiDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resume Subscription") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The earlier run is kept as price history and its payments stay exactly as they are. Billing restarts on this date.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                DateField("Resumes on", date, { date = it }, Modifier.fillMaxWidth())
                AmountField(total, { total = it }, Modifier.fillMaxWidth(), label = "Total charged going forward", large = false)
            }
        },
        confirmButton = {
            TextButton(enabled = date != null, onClick = { date?.let { onResume(it, total.ifBlank { null }) } }) { Text("Resume") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Add / edit an earlier period of the subscription (its own dates, price and cycle). */
@Composable
private fun PeriodSheet(
    initial: SubscriptionPeriodEntity,
    isNew: Boolean,
    currentStart: LocalDate,
    onSave: (SubscriptionPeriodEntity) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var label by rememberSaveable { mutableStateOf(initial.label) }
    var start by rememberSaveable { mutableStateOf(initial.startDate) }
    var end by rememberSaveable { mutableStateOf(initial.endDate) }
    var total by rememberSaveable { mutableStateOf(Money.toInput(initial.amountCents)) }
    var cycle by rememberSaveable { mutableStateOf(initial.cycle) }
    var customPeriodCycle by rememberSaveable { mutableStateOf(initial.cycle !in BillingCycle.presets) }
    val cents = Money.parse(total)
    val valid = !end.isBefore(start) && (cents ?: 0) > 0
    MiBottomPanel(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isNew) "Add Earlier Period" else "Edit Period", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                if (!isNew) IconButton(onClick = onDelete) { Icon(Sym.Delete, "Delete Period") }
            }
            com.jlees.mibudget.ui.common.LabeledField(label = "Label (optional)", value = label, onChange = { label = it }, placeholder = "e.g. Student plan", modifier = Modifier.fillMaxWidth(), keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = androidx.compose.ui.text.input.ImeAction.Next), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("From", start, { it?.let { d -> start = d } }, Modifier.weight(1f))
                DateField("To", end, { it?.let { d -> end = d } }, Modifier.weight(1f))
            }
            if (end.isBefore(start)) Text("\"To\" must be on or after \"From\".", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (!end.isBefore(currentStart)) Text(
                "This overlaps the current period (from ${currentStart.format(MediumDate)}); the current price wins on overlapping dates.",
                color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall,
            )
            AmountField(total, { total = it }, Modifier.fillMaxWidth(), label = "Total charged per billing", large = false)
            Text("Billing cycle", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BillingCycle.presets.forEach { p ->
                    FilterChip(selected = cycle == p && !customPeriodCycle, onClick = { customPeriodCycle = false; cycle = p }, label = { Text(p.label) })
                }
                FilterChip(
                    selected = customPeriodCycle || cycle !in BillingCycle.presets,
                    onClick = { customPeriodCycle = true },
                    label = { Text("Custom") },
                )
            }
            if (customPeriodCycle || cycle !in BillingCycle.presets) CustomCycleRow(cycle) { cycle = it }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    enabled = valid,
                    onClick = {
                        onSave(
                            initial.copy(
                                label = label.trim(), startDate = start, endDate = end, amountCents = cents ?: 0,
                                cycleUnit = cycle.unit, cycleCount = cycle.count,
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
            Text(
                "Changes are applied when you save the subscription.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

fun daysLabel(days: Int): String = when {
    days < 0 -> "off"
    days == 0 -> "on the day"
    days == 1 -> "1 day before"
    days == 7 -> "1 week before"
    else -> "$days days before"
}

/**
 * "Every [N] [months]". The number is edited as text, so it can be cleared while you type a new one;
 * the cycle only changes once it's a valid count (1 up to the unit's maximum).
 */
@Composable
private fun CustomCycleRow(cycle: BillingCycle, onChange: (BillingCycle) -> Unit) {
    var text by rememberSaveable(cycle.unit) { mutableStateOf(cycle.count.toString()) }
    // Follow outside changes (e.g. a scan filled it in), but not while the box is mid-edit.
    LaunchedEffect(cycle.count) { if (text.toIntOrNull() != cycle.count && text.isNotEmpty()) text = cycle.count.toString() }
    val n = text.toIntOrNull()
    val tooBig = n != null && n > cycle.unit.maxCount
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Every", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        com.jlees.mibudget.ui.common.ClearableField(value = text, onChange = { v ->
                text = v.filter(Char::isDigit).take(3)
                text.toIntOrNull()?.takeIf { it in 1..cycle.unit.maxCount }?.let { onChange(BillingCycle(cycle.unit, it)) }
            }, placeholder = "", modifier = Modifier.width(80.dp).semantics { contentDescription = "Number of ${cycle.unit.plural}" }, keyboard = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done), singleLine = true, isError = text.isEmpty() || n == 0 || tooBig)
        Spacer(Modifier.width(8.dp))
        UnitMenu(cycle.unit, cycle.count) { u -> onChange(BillingCycle(u, u.clamp(cycle.count))) }
    }
    if (tooBig) Text(
        "Up to ${cycle.unit.maxCount} ${cycle.unit.plural}",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun UnitMenu(unit: CycleUnit, count: Int, onSelect: (CycleUnit) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { open = true }) { Text(if (count == 1) unit.singular else unit.plural) }
        com.jlees.mibudget.ui.common.MiMenu(expanded = open, onDismissRequest = { open = false }) {
            CycleUnit.entries.forEach { u ->
                DropdownMenuItem(text = { Text(u.plural) }, onClick = { onSelect(u); open = false })
            }
        }
    }
}

private val FullDate: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)

/** Read-only view of a saved subscription. */
@Composable
private fun SubscriptionPreview(
    form: SubscriptionForm,
    tree: com.jlees.mibudget.domain.CategoryTree,
    history: PaymentHistory,
    methods: List<com.jlees.mibudget.data.db.PaymentMethodEntity>,
    receipt: java.io.File?,
    onOpenPurchase: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Room at the end for the navigation bar (the page scrolls on behind it). */
    bottom: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (form.id == null) { androidx.compose.foundation.layout.Box(modifier) { com.jlees.mibudget.ui.common.DetailSkeleton() }; return } // still loading
    val live = form.status == SubscriptionStatus.ACTIVE || form.status == SubscriptionStatus.TRIAL
    val method = form.paymentMethodId?.let { id -> methods.firstOrNull { it.id == id } }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PreviewHeader(
            avatar = { ItemIcon(form.brandKey, form.name.ifBlank { "?" }, form.categoryId?.let { tree.byId[it] }, size = 72.dp) },
            title = form.name,
            amount = Money.format(form.amountCents ?: 0) + form.cycle.shortSuffix,
            caption = if (live) "Next payment ${form.nextDueDate.format(FullDate)}" else form.endDate?.let { "Ended ${it.format(FullDate)}" },
            chip = form.status.label,
            chipColor = when (form.status) {
                SubscriptionStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                SubscriptionStatus.TRIAL -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        // its sections as the editor's, each one block under its heading, as Settings
        com.jlees.mibudget.ui.common.FormGroup {
            DetailRow(
                Sym.Category, "Category",
                form.categoryId?.let { tree.pathLabel(it) }?.ifBlank { null } ?: "Uncategorized",
            )
        }
        FormHeading("Billing")
        com.jlees.mibudget.ui.common.FormGroup {
            DetailRow(Sym.Autorenew, "Billing cycle", form.cycle.label)
            if (form.price.base.isNotBlank() && form.price.fees.isNotBlank()) {
                DetailRow(Sym.Payments, "Price", "${form.price.base} + ${form.price.fees} fees & taxes")
            }
            DetailRow(Sym.Event, "Started", form.anchorDate.format(FullDate))
            // Only while it's actually on a free trial (a trial date is kept after switching to Active,
            // so past trial payments stay free, but it's no longer the current status).
            if (form.status == SubscriptionStatus.TRIAL) {
                form.trialEndDate?.let { DetailRow(Sym.HourglassBottom, "Free trial ends", it.format(FullDate)) }
            }
        }
        FormHeading("Payments")
        com.jlees.mibudget.ui.common.FormGroup {
            DetailRow(Sym.Payments, "Log payments automatically", if (form.autoLog) "On" else "Off")
            DetailRow(
                Sym.Notifications, "Renewal reminder",
                form.reminderDays.let { d ->
                    when {
                        d == null -> "App default"
                        d < 0 -> "Off"
                        d == 0 -> "On the day"
                        d == 1 -> "1 day before"
                        else -> "$d days before"
                    }
                },
            )
            val methodName = method?.displayName ?: form.paymentMethod.ifBlank { null }
            if (methodName != null) DetailRow(
                Sym.CreditCard, "Paid with", methodName,
                leading = if (method != null) { { com.jlees.mibudget.ui.components.PaymentMethodIcon(method, size = 24.dp) } } else null,
            )
            if (form.note.isNotBlank()) DetailRow(Sym.Notes, "Notes", form.note)
        }
        receipt?.let { com.jlees.mibudget.ui.components.ReceiptPreview(it) }
        if (form.periods.isNotEmpty()) {
            FormHeading("Earlier Prices")
            com.jlees.mibudget.ui.common.FormGroup {
                form.periods.sortedByDescending { it.startDate }.forEach { p ->
                    DetailRow(
                        Sym.History,
                        "${p.startDate.format(MediumDate)} – ${p.endDate.format(MediumDate)}",
                        "${Money.format(p.amountCents)}${p.cycle.shortSuffix}" + if (p.label.isNotBlank()) " · ${p.label}" else "",
                    )
                }
            }
        }
        FormHeading("Payment History")
        FormNote("${com.jlees.mibudget.domain.plural(history.count, "payment")} · ${Money.format(history.total)} total")
        // the same rows as every purchase list
        history.recent.forEach { p ->
            com.jlees.mibudget.ui.purchases.PurchaseListItem(
                p, p.categoryId?.let { tree.byId[it] }, method = null, onClick = { onOpenPurchase(p.id) }, modifier = Modifier.bleed(),
            )
        }
        // room for the split Pill (Delete | Edit) and the navigation bar at the bottom
        Spacer(Modifier.height(com.jlees.mibudget.ui.common.PillSpace + bottom))
    }
}
