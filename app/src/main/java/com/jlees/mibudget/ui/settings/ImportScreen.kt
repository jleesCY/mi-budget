package com.jlees.mibudget.ui.settings

import com.jlees.mibudget.ui.common.MiSwitch
import androidx.compose.foundation.lazy.itemsIndexed
import com.jlees.mibudget.ui.common.MiDialog

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.data.backup.DuplicateStrategy
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.components.CategoryBadge
import com.jlees.mibudget.ui.components.CategoryPickerSheet
import com.jlees.mibudget.ui.components.UncategorizedBadge

@Composable
fun ImportScreen(onBack: () -> Unit, vm: ImportViewModel = viewModel(factory = AppViewModels.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    val localTree by vm.localTree.collectAsStateWithLifecycle()
    var pickDestination by rememberSaveable { mutableStateOf(false) }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.load(uri) }
    val openFile = { opener.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "*/*")) }

    // Stay put while importing (it only takes a moment, and leaving would hide the result).
    androidx.activity.compose.BackHandler(enabled = state.importing) {}

    com.jlees.mibudget.ui.common.MiScaffold(
        title = "Restore",
        onBack = onBack,
        actions = { if (state.backup != null) TextButton(onClick = openFile, enabled = !state.importing) { Text("Other File") } },
        bottomBar = {
            val b = state.backup
            if (b != null) com.jlees.mibudget.ui.common.FormActionBar(
                if (state.importing) "Restoring…" else "Restore ${state.plan.categoryIds.size} of ${b.file.categories.size} Categories",
                !state.importing && state.hasSelection,
            ) { vm.runImport() }
        },
    ) { padding ->
        val b = state.backup
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            b == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                com.jlees.mibudget.ui.common.EmptyLine(Sym.FileOpen, "Choose a Backup", action = { Button(onClick = openFile) { Text("Choose File") } })
            }
            else -> {
                val tree = b.categoryTree
                val cats = tree.flattened()
                // built from Settings' pieces: headings over segmented blocks of rows, 20 in from the edges
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 16.dp),
                ) {
                    item {
                        com.jlees.mibudget.ui.common.FormBanner(
                            Sym.FileOpen, "Backup from ${b.file.device.ifBlank { "another phone" }}",
                            "${b.file.exportedAt.take(10)} · ${b.file.categories.size} categories · ${b.file.purchases.size} purchases · ${b.file.subscriptions.size} subscriptions",
                        )
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { SectionHeader(Sym.Category, "Categories & Budgets") }
                            TextButton(onClick = { vm.selectAll(true) }, modifier = Modifier.padding(top = 10.dp)) { Text("All") }
                            TextButton(onClick = { vm.selectAll(false) }, modifier = Modifier.padding(top = 10.dp)) { Text("None") }
                        }
                    }
                    itemsIndexed(cats, key = { _, it -> it.first.id }) { i, (cat, depth) ->
                        val sub = tree.subtreeIds(cat.id)
                        val selectedInSub = sub.count { it in state.plan.categoryIds }
                        val some = selectedInSub in 1 until sub.size || (selectedInSub > 0 && cat.id !in state.plan.categoryIds)
                        val counts = state.counts[cat.id]
                        Box(Modifier.segment(i, cats.size)) {
                            Item(
                                Sym.Category, cat.name,
                                buildString {
                                    append("${counts?.purchases ?: 0} purchases")
                                    if ((counts?.subscriptions ?: 0) > 0) append(" · ${counts?.subscriptions} subscriptions")
                                    cat.budgetCents?.let { append(" · ${Money.format(it)} ${cat.budgetPeriod.label.lowercase()}") }
                                    if (some) append(" · some sub-categories")
                                },
                                onClick = { vm.toggleCategory(cat.id) },
                                // sub-categories sit a step in under their parent
                                leading = { Box(Modifier.padding(start = (depth * 16).dp)) { CategoryBadge(cat, size = 40.dp) } },
                                trailing = { MiSwitch(selectedInSub > 0, null) },
                            )
                        }
                    }
                    item {
                        com.jlees.mibudget.ui.common.FormNote("Purchases and subscriptions come along with their category.", Modifier.padding(top = 8.dp))
                    }
                    val extras = buildList<@Composable () -> Unit> {
                        if (state.uncategorizedPurchases > 0) add {
                            SwitchItem(Sym.ReceiptLong, "Uncategorized Purchases", "${state.uncategorizedPurchases} in the backup", state.plan.includeUncategorizedPurchases) { v ->
                                vm.updatePlan { it.copy(includeUncategorizedPurchases = v) }
                            }
                        }
                        if (state.uncategorizedSubscriptions > 0) add {
                            SwitchItem(Sym.Autorenew, "Uncategorized Subscriptions", "${state.uncategorizedSubscriptions} in the backup", state.plan.includeUncategorizedSubscriptions) { v ->
                                vm.updatePlan { it.copy(includeUncategorizedSubscriptions = v) }
                            }
                        }
                        if (b.file.settings != null) add {
                            SwitchItem(Sym.Settings, "App Settings", "Theme, chart and preferences", state.plan.includeSettings) { v -> vm.updatePlan { it.copy(includeSettings = v) } }
                        }
                    }
                    if (extras.isNotEmpty()) {
                        item { SectionHeader(Sym.Add, "Also Bring") }
                        itemsIndexed(extras) { i, row -> Box(Modifier.segment(i, extras.size)) { row() } }
                    }
                    item {
                        SectionHeader(Sym.Tune, "Options")
                        val dest = state.plan.destinationParentId?.let { localTree.byId[it] }
                        val rows = listOf<@Composable () -> Unit>(
                            {
                                Item(
                                    Sym.Folder, "Put Them In", dest?.let { localTree.pathLabel(it.id) } ?: "On their own, at the top level",
                                    onClick = { pickDestination = true },
                                    leading = if (dest != null) { { CategoryBadge(dest, size = 40.dp) } } else null,
                                    trailing = { Icon(Sym.KeyboardArrowRight, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                )
                            },
                            { SwitchItem(Sym.LibraryAdd, "Merge Same Names", "Join categories that have the same name as yours", state.plan.mergeCategoriesByName) { v -> vm.updatePlan { it.copy(mergeCategoriesByName = v) } } },
                            { SwitchItem(Sym.ContentCopy, "Skip Look-Alikes", "Leave out purchases that look like ones you have", state.plan.skipLookalikePurchases) { v -> vm.updatePlan { it.copy(skipLookalikePurchases = v) } } },
                            {
                                ChoiceItem(
                                    Sym.Restore, "Already Here", state.plan.duplicates.description,
                                    DuplicateStrategy.entries.map { it to it.label }, state.plan.duplicates, vm::setDuplicates,
                                )
                            },
                        )
                        Column { rows.forEachIndexed { i, row -> Box(Modifier.segment(i, rows.size)) { row() } } }
                    }
                }
            }
        }
    }

    if (pickDestination) {
        CategoryPickerSheet(
            tree = localTree,
            selectedId = state.plan.destinationParentId,
            title = "Restore Into…",
            noneLabel = "None",
            noneSupporting = "Keep them on their own, not inside another category",
            onDismiss = { pickDestination = false },
            onSelect = { id -> vm.updatePlan { it.copy(destinationParentId = id) }; pickDestination = false },
        )
    }
    state.result?.let { r ->
        MiDialog(
            onDismissRequest = onBack,
            title = { Text("Restore Complete") },
            text = {
                Text(
                    buildString {
                        appendLine("Categories: ${r.categoriesAdded} added, ${r.categoriesMerged} merged")
                        appendLine("Purchases: ${r.purchasesAdded} added, ${r.purchasesSkipped} skipped")
                        appendLine("Subscriptions: ${r.subscriptionsAdded} added, ${r.subscriptionsSkipped} skipped")
                        if (r.damagedSkipped > 0) appendLine("Left out (damaged in the backup): ${r.damagedSkipped}")
                        if (r.settingsApplied) append("Settings applied")
                    }
                )
            },
            confirmButton = { TextButton(onClick = onBack) { Text("Done") } },
        )
    }
    state.error?.let { e ->
        MiDialog(
            onDismissRequest = vm::clearError,
            title = { Text("Couldn't Restore") },
            text = { Text(e) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("Close") } },
        )
    }
}
