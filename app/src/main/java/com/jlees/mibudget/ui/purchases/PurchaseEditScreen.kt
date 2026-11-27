package com.jlees.mibudget.ui.purchases

import com.jlees.mibudget.ui.common.scrollingPage
import com.jlees.mibudget.ui.common.MiDialog

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.material3.OutlinedButton

import androidx.compose.foundation.clickable
import androidx.compose.material3.FilledTonalButton
import com.jlees.mibudget.data.db.PaymentMethodEntity
import com.jlees.mibudget.data.db.PurchaseSource
import com.jlees.mibudget.data.db.displayName
import com.jlees.mibudget.domain.CategoryTree
import com.jlees.mibudget.ui.components.DetailRow
import com.jlees.mibudget.ui.components.PaymentMethodIcon
import com.jlees.mibudget.ui.components.PreviewHeader
import com.jlees.mibudget.ui.components.ReceiptPreview
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.components.AmountField
import com.jlees.mibudget.ui.components.ItemIconSheet
import com.jlees.mibudget.ui.components.IconSheetMode
import com.jlees.mibudget.ui.components.CategoryField
import com.jlees.mibudget.ui.common.ConfirmDialog
import com.jlees.mibudget.ui.components.DateField
import com.jlees.mibudget.ui.components.ItemIcon
import com.jlees.mibudget.ui.components.ReceiptSection
import com.jlees.mibudget.ui.components.PaymentMethodField

@Composable
fun PurchaseEditScreen(
    onBack: () -> Unit,
    vm: PurchaseEditViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val form by vm.form.collectAsStateWithLifecycle()
    val notFound by vm.notFound.collectAsStateWithLifecycle()
    if (notFound) {
        com.jlees.mibudget.ui.components.ItemNotFound("Purchase Not Found", onBack)
        return
    }
    val tree by vm.tree.collectAsStateWithLifecycle()
    val merchants by vm.merchants.collectAsStateWithLifecycle()
    val methods by vm.paymentMethods.collectAsStateWithLifecycle()
    var brandPicker by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var askCategory by rememberSaveable { mutableStateOf(false) }
    var showScanText by rememberSaveable { mutableStateOf(false) }
    // Existing purchases open as a read-only preview; the Edit button switches to the editor.
    var editing by rememberSaveable { mutableStateOf(vm.isNew) }
    val leaveEditing: () -> Unit = { vm.reload(); editing = false }
    val afterSave: () -> Unit = {
        if (vm.isNew) onBack() else editing = false
        Unit
    }

    fun save() {
        if (form.categoryId == null) askCategory = true else vm.save(afterSave)
    }

    // the summary and the editor are two pages: the editor slides in over the summary, and Back slides it away with
    // the finger (the system gesture or the Pill's swipe)
    val page: @Composable (Boolean) -> Unit = { asEditor ->
        com.jlees.mibudget.ui.common.MiScaffold(
            title = if (vm.isNew) "New Purchase" else if (asEditor) "Edit Purchase" else "Purchase",
            onBack = { if (asEditor && !vm.isNew) leaveEditing() else onBack() },
            // its actions (Edit, Refund, Delete; or Save) are the split Pill at the bottom, below. Leaving a new one keeps it
            // for "Resume"; the Pill's trash throws it away.
        ) { padding ->
            if (!asEditor) {
                PurchasePreview(form, tree, methods, vm, modifier = Modifier.padding(top = padding.calculateTopPadding()), bottom = padding.calculateBottomPadding())
                return@MiScaffold
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .scrollingPage(padding, rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = com.jlees.mibudget.ui.common.PillSpace),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (form.scanText != null) com.jlees.mibudget.ui.common.FormBanner(
                    Sym.DocumentScanner, "Filled In From Your Scan", "Check it over before you save.",
                    action = "View Text", onAction = { showScanText = true },
                )

                // laid out as Settings: each section's rows one segmented block under its heading
                com.jlees.mibudget.ui.common.FormGroup {
                    // the merchant, with its picture (picked automatically as you type; tap it to choose another)
                    com.jlees.mibudget.ui.common.LabeledField(
                        label = "Merchant / description", value = form.merchant, onChange = vm::setMerchant, placeholder = "Where you spent it",
                        keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                        leading = {
                            com.jlees.mibudget.ui.common.IconTile(onClick = { brandPicker = true }) {
                                ItemIcon(form.brandKey, form.merchant.ifBlank { "?" }, form.categoryId?.let { tree.byId[it] }, size = 56.dp)
                            }
                        },
                    )
                    CategoryField(tree, form.categoryId, vm::setCategory, Modifier.fillMaxWidth(), highlightEmpty = true)
                }
                val suggestions = remember(form.merchant, merchants) {
                    val q = form.merchant.trim().lowercase()
                    if (q.length < 2) emptyList()
                    else merchants.filter { it.lowercase().startsWith(q) && !it.equals(form.merchant.trim(), true) }.take(6)
                }
                val suggested = form.suggestedCategoryId?.takeIf { it != form.categoryId }
                if (suggestions.isNotEmpty() || suggested != null) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { s -> com.jlees.mibudget.ui.common.MiChip(s, false, leading = Sym.History, role = androidx.compose.ui.semantics.Role.Button) { vm.setMerchant(s) } }
                        if (suggested != null) com.jlees.mibudget.ui.common.MiChip("Suggested: ${tree.byId[suggested]?.name ?: ""}", false, leading = Sym.AutoAwesome, role = androidx.compose.ui.semantics.Role.Button) { vm.setCategory(suggested) }
                    }
                }

                com.jlees.mibudget.ui.common.FormHeading("Price")
                com.jlees.mibudget.ui.common.FormGroup {
                    AmountField(form.amountText, vm::setAmount, Modifier.fillMaxWidth(), isError = form.amountText.isNotEmpty() && form.amountCents == null, icon = Sym.Payments)
                    if (form.amountCandidates.size > 1) com.jlees.mibudget.ui.common.FormRow(Sym.DocumentScanner, "Other amounts found") {
                        com.jlees.mibudget.ui.common.ChipChoice(form.amountCandidates.map { it to Money.format(it) }, form.amountCents, { vm.setAmount(Money.toInput(it)) })
                    }
                    // mi. Budget tracks spending, not income: money back is a refund on the purchase itself (Refund on its
                    // page). Only a scanned return receipt is saved as a refund of its own; this lets a wrong guess be undone.
                    if (form.isRefund) com.jlees.mibudget.ui.common.SwitchField("Refund", true, vm::setRefund, subtitle = "Scanned as a return receipt. Turn off if it was a purchase.", icon = Sym.Undo)
                }

                com.jlees.mibudget.ui.common.FormHeading("Payment")
                com.jlees.mibudget.ui.common.FormGroup {
                    DateField("Date", form.date, { it?.let(vm::setDate) }, Modifier.fillMaxWidth())
                    PaymentMethodField(
                        methods = methods,
                        selectedId = form.paymentMethodId,
                        legacyName = form.paymentMethod,
                        onSelect = vm::setPaymentMethodId,
                        onCreate = vm::createPaymentMethod,
                        importIcon = vm::importPaymentIcon,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    com.jlees.mibudget.ui.common.LabeledField(label = "Note (optional)", value = form.note, onChange = vm::setNote, placeholder = "Anything to remember", modifier = Modifier.fillMaxWidth(), minLines = 1, singleLine = false, icon = Sym.Notes)
                    ReceiptSection(form.receiptFile?.let(vm::receiptFile), vm::attachReceipt, vm::removeReceipt)
                }
            }
        }
    }
    com.jlees.mibudget.ui.common.EditorPages(editing, canLeave = !vm.isNew, onLeave = leaveEditing, summary = { page(false) }, editor = { page(true) })

    if (brandPicker) {
        ItemIconSheet(
            mode = IconSheetMode.MERCHANT,
            name = form.merchant,
            current = form.brandKey,
            category = form.categoryId?.let { tree.byId[it] },
            importImage = vm::importPaymentIcon,
            onSelect = { ref, brand -> vm.setIcon(ref, brand); brandPicker = false },
            onDismiss = { brandPicker = false },
        )
    }
    // the page's split Pill, grown out of the list page's Pill: Delete | Refund | Edit on the summary; Delete (or
    // Discard, for a new one) | Save while editing. Delete is always at the far left, the main action at the far right. Swiping it right goes back (out of editing first).
    var askRefund by rememberSaveable { mutableStateOf(false) }
    val canRefund = !form.isRefund && (form.amountCents ?: 0) > 0
    com.jlees.mibudget.ui.common.ActionPill(com.jlees.mibudget.ui.common.ActionPillSpec(
        segments = if (!editing) listOfNotNull(
            com.jlees.mibudget.ui.common.PillSegment("Delete", Sym.Delete, danger = true) { vm.delete(onBack) },
            if (canRefund) com.jlees.mibudget.ui.common.PillSegment(if (form.refundedCents == 0L) "Refund" else "Change Refund", Sym.Undo) { askRefund = true } else null,
            com.jlees.mibudget.ui.common.PillSegment("Edit", Sym.Edit, main = true) { editing = true },
        ) else listOf(
            if (vm.isNew) com.jlees.mibudget.ui.common.PillSegment("Discard This Purchase", Sym.Delete, danger = true) { confirmDiscard = true }
            else com.jlees.mibudget.ui.common.PillSegment("Delete", Sym.Delete, danger = true) { vm.delete(onBack) },
            com.jlees.mibudget.ui.common.PillSegment(if (vm.isNew) "Add Purchase" else "Save Changes", Sym.Check, main = true, enabled = form.canSave) { save() },
        ),
        onBack = { if (editing && !vm.isNew) leaveEditing() else onBack() },
        backPops = !(editing && !vm.isNew),
    ))
    if (askRefund) RefundDialog(
        total = form.amountCents ?: 0, current = form.refundedCents,
        onRefund = { askRefund = false; vm.refund(it) }, onDismiss = { askRefund = false },
    )
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard This Purchase?",
            text = "What you've entered (and any scanned picture) will be deleted. This can't be undone.",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = { confirmDiscard = false; vm.discard(onBack) },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (askCategory) {
        // Gentle nudge: budgets only count categorized purchases.
        MiDialog(
            onDismissRequest = { askCategory = false },
            title = { Text("Add a Category?") },
            text = { Text("Uncategorized purchases don't count toward any budget. You can always categorize it later from the Purchases list.") },
            confirmButton = { TextButton(onClick = { askCategory = false }) { Text("Choose Category") } },
            dismissButton = { TextButton(onClick = { askCategory = false; vm.save(afterSave) }) { Text("Save Anyway") } },
        )
    }
    if (showScanText) com.jlees.mibudget.ui.components.ScanTextDialog(
        listOf(com.jlees.mibudget.ui.components.ScanTextSection("Google's text reader", form.scanText ?: "")),
        onDismiss = { showScanText = false },
    )
}

private val FullDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

/** Read-only view of a saved purchase. */
@Composable
private fun PurchasePreview(
    form: PurchaseForm,
    tree: CategoryTree,
    methods: List<PaymentMethodEntity>,
    vm: PurchaseEditViewModel,
    modifier: Modifier = Modifier,
    /** Room at the end for the navigation bar (the page scrolls on behind it). */
    bottom: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (!form.loaded) { androidx.compose.foundation.layout.Box(modifier) { com.jlees.mibudget.ui.common.DetailSkeleton() }; return }
    val method = form.paymentMethodId?.let { id -> methods.firstOrNull { it.id == id } }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PreviewHeader(
            avatar = { ItemIcon(form.brandKey, form.merchant.ifBlank { "?" }, form.categoryId?.let { tree.byId[it] }, size = 72.dp) },
            title = form.merchant,
            amount = (if (form.isRefund) "+" else "") + Money.format(form.amountCents ?: 0),
            amountColor = if (form.isRefund || form.refundedCents != 0L) com.jlees.mibudget.ui.theme.AppColors.refund else MaterialTheme.colorScheme.onSurface,
            caption = form.date.format(FullDate),
            chip = when {
                form.isRefund -> "Refund"
                form.refundedCents != 0L && form.refundedCents == form.amountCents -> "Refunded"
                form.refundedCents != 0L -> "Partly Refunded"
                else -> null
            },
            chipColor = com.jlees.mibudget.ui.theme.AppColors.refund,
        )
        // its sections as the editor's, each one block under its heading, as Settings
        com.jlees.mibudget.ui.common.FormGroup {
            DetailRow(
                Sym.Category, "Category",
                form.categoryId?.let { tree.pathLabel(it) }?.ifBlank { null } ?: "Uncategorized",
            )
            when {
                form.subscriptionId != null -> DetailRow(Sym.Autorenew, "Added by", "A subscription payment")
                form.source == PurchaseSource.SCAN -> DetailRow(Sym.DocumentScanner, "Added by", "Scanning a picture")
                else -> Unit
            }
        }
        if (form.refundedCents != 0L) {
            com.jlees.mibudget.ui.common.FormHeading("Refund")
            com.jlees.mibudget.ui.common.FormGroup {
                DetailRow(
                    Sym.Undo, "Refunded",
                    Money.format(form.refundedCents) + (form.refundedOn?.let { " on ${it.format(FullDate)}" } ?: ""),
                )
                DetailRow(Sym.Savings, "Counts toward budgets", Money.format((form.amountCents ?: 0) - form.refundedCents))
            }
        }
        val methodName = method?.displayName ?: form.paymentMethod.ifBlank { null }
        if (methodName != null || form.note.isNotBlank() || form.receiptFile != null) {
            com.jlees.mibudget.ui.common.FormHeading("Payment")
            if (methodName != null || form.note.isNotBlank()) com.jlees.mibudget.ui.common.FormGroup {
                if (methodName != null) DetailRow(
                    Sym.CreditCard, "Paid with", methodName,
                    leading = if (method != null) { { PaymentMethodIcon(method, size = 24.dp) } } else null,
                )
                if (form.note.isNotBlank()) DetailRow(Sym.Notes, "Note", form.note)
            }
            form.receiptFile?.let { ReceiptPreview(vm.receiptFile(it)) }
        }
        // room for the split Pill (Delete | Refund | Edit) and the navigation bar at the bottom
        Spacer(Modifier.height(com.jlees.mibudget.ui.common.PillSpace + bottom))
    }
}

/**
 * Refund all of a purchase (the default) or part of it. A refunded purchase stays in history, crossed out; only what
 * wasn't refunded counts toward budgets. With a refund already there, it can be changed or taken off.
 */
@Composable
private fun RefundDialog(total: Long, current: Long, onRefund: (Long) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(Money.toInput(if (current != 0L) current else total)) }
    val cents = Money.parse(text)
    val ok = cents != null && cents in 1..total
    MiDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == 0L) "Refund Purchase" else "Change Refund") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("It stays in your history, but only what wasn't refunded counts toward budgets.")
                AmountField(text, { text = it }, Modifier.fillMaxWidth(), isError = text.isNotEmpty() && !ok)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = cents == total, onClick = { text = Money.toInput(total) }, label = { Text("All of it") })
                    FilterChip(selected = cents == total / 2, onClick = { text = Money.toInput(total / 2) }, label = { Text("Half") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { cents?.let(onRefund) }, enabled = ok) { Text("Refund") } },
        dismissButton = {
            Row {
                if (current != 0L) TextButton(onClick = { onRefund(0) }) { Text("Remove Refund", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
