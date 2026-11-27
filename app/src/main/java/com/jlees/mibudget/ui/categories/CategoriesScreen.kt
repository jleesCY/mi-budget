package com.jlees.mibudget.ui.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jlees.mibudget.ui.common.listRow
import com.jlees.mibudget.ui.common.bleed
import com.jlees.mibudget.ui.common.MiDialog

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.data.db.CategoryEntity
import com.jlees.mibudget.data.db.cycle
import com.jlees.mibudget.domain.BudgetStatus
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.components.BudgetProgress
import com.jlees.mibudget.ui.components.CategoryBadge
import com.jlees.mibudget.ui.components.CategoryPickerSheet
import com.jlees.mibudget.ui.components.UncategorizedBadge

@Composable
fun CategoriesScreen(
    onBack: (() -> Unit)?,
    onOpenFolder: (String?) -> Unit,
    onJumpToFolder: (String?) -> Unit,
    onViewPurchases: (categoryId: String?, uncategorized: Boolean) -> Unit,
    onOpenPurchase: (String) -> Unit,
    onOpenSubscription: (String) -> Unit,
    vm: CategoriesViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val view by vm.view.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<Pair<CategoryEntity, Boolean>?>(null) } // entity, isNew
    var budgetEditing by rememberSaveable { mutableStateOf<CategoryEntity?>(null) }
    var pickForBudget by rememberSaveable { mutableStateOf(false) }
    var showTemplates by rememberSaveable { mutableStateOf(false) }
    var menu by rememberSaveable { mutableStateOf(false) }
    val folder = state.folder
    val atRoot = vm.folderId == null

    fun newCategory() {
        editing = CategoryEntity(name = "", parentId = folder?.id, icon = folder?.icon ?: "folder", color = folder?.color ?: 0xFF5C6BC0.toInt()) to true
    }

    // the Library's one search Pill (or this page's own when it stands alone)
    val hostedQuery = com.jlees.mibudget.ui.common.LocalSheetQuery.current
    var ownQuery by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val q = (hostedQuery ?: ownQuery).trim()
    com.jlees.mibudget.ui.common.MiScaffold(
        // the chips under the header say which view this is; the whole name didn't fit beside the buttons
        title = folder?.name ?: if (view == CategoriesView.BUDGETS) "Budgets" else "Categories",
        onBack = onBack,
        actions = {
                    if (folder != null) IconButton(onClick = { editing = folder to false }) { Icon(Sym.Edit, "Edit folder") }
                    IconButton(onClick = { menu = true }) { Icon(Sym.MoreVert, "More") }
                    com.jlees.mibudget.ui.common.MiMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Add From Defaults") },
                            leadingIcon = { Icon(Sym.LibraryAdd, null) },
                            enabled = state.missingTemplates.isNotEmpty(),
                            onClick = { menu = false; showTemplates = true },
                        )
                        if (folder != null) DropdownMenuItem(
                            text = { Text("All Purchases in Folder") },
                            leadingIcon = { Icon(Sym.ReceiptLong, null) },
                            onClick = { menu = false; onViewPurchases(folder.id, false) },
                        )
                    }
                },
        pill = com.jlees.mibudget.ui.common.PillSpec(if (view == CategoriesView.BUDGETS) "Search Budgets" else "Search Categories", onTap = {}, query = ownQuery, onQuery = { ownQuery = it }),
        // the add button matches the view: Budgets → add a budget, Categories → add a category
        fabs = com.jlees.mibudget.ui.common.SheetFabs(
            if (view == CategoriesView.BUDGETS && atRoot) com.jlees.mibudget.ui.common.BarButton(Sym.Add, "Add Budget") { pickForBudget = true }
            else com.jlees.mibudget.ui.common.BarButton(Sym.Add, if (folder == null) "Add Category" else "Add Sub-Category") { newCategory() },
        ),
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            // no gap between items: list rows (budgets, purchases, subscriptions) follow List Style themselves, meeting at
            // their lines in a table; everything else brings its own 12 below it (Gap)
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (atRoot) item(span = { GridItemSpan(maxLineSpan) }) {
                // the connected button group Settings' choices use
                com.jlees.mibudget.ui.common.ConnectedChoice(
                    CategoriesView.entries.map { it to if (it == CategoriesView.FOLDERS) "Categories" else "Budgets" },
                    view, Modifier.padding(bottom = Gap),
                ) { vm.view.value = it }
            } else item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(bottom = Gap)) { Breadcrumbs(state.path, onJump = onJumpToFolder) }
            }

            if (view == CategoriesView.BUDGETS && atRoot) {
                // nothing until the data is in, rather than a "$0.00 of $0.00 · No Budgets Yet" that isn't true
                if (state.loading) { item(span = { GridItemSpan(maxLineSpan) }) { com.jlees.mibudget.ui.common.BoneRows() }; return@LazyVerticalGrid }
                item(span = { GridItemSpan(maxLineSpan) }) { Box(Modifier.padding(bottom = Gap)) { BudgetSummary(state) } }
                if (state.budgets.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    com.jlees.mibudget.ui.common.EmptyLine(Sym.Savings, "No Budgets Yet")
                }
                items(state.budgets.filter { q.isEmpty() || it.category.name.contains(q, ignoreCase = true) }, key = { "b-" + it.category.id }, span = { GridItemSpan(maxLineSpan) }) { s ->
                    BudgetRow(s, state.tree.pathLabel(s.category.parentId), onClick = { budgetEditing = s.category })
                }
                return@LazyVerticalGrid
            }

            // ----- Folder view -----
            if (folder != null) item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.padding(bottom = Gap)) { FolderHeader(state, onEditBudget = { budgetEditing = folder }, onViewAll = { onViewPurchases(folder.id, false) }) }
            }
            if (state.items.isEmpty() && folder != null && !state.loading) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "No sub-categories. Tap + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            if (state.items.isEmpty() && folder == null && !state.loading) item(span = { GridItemSpan(maxLineSpan) }) {
                com.jlees.mibudget.ui.common.EmptyLine(Sym.Folder, "No Categories Yet", action = { FilledTonalButton(onClick = { showTemplates = true }) { Text("Add Defaults") } })
            }
            items(state.items.filter { q.isEmpty() || it.category.name.contains(q, ignoreCase = true) }, key = { it.category.id }) { item ->
                Box(Modifier.padding(bottom = Gap)) { FolderCard(item, onClick = { onOpenFolder(item.category.id) }, onLongClick = { editing = item.category to false }) }
            }
            if (atRoot && state.uncategorizedCount > 0) item(key = "uncat") {
                Box(Modifier.padding(bottom = Gap)) { UncategorizedCard(state.uncategorizedCount) { onViewPurchases(null, true) } }
            }

            if (folder != null && state.subscriptionsHere.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("Subscriptions here") }
                items(state.subscriptionsHere, key = { "s-" + it.id }, span = { GridItemSpan(maxLineSpan) }) { s ->
                    // the same row the Subscriptions page shows, edge to edge in a table despite the page's padding
                    com.jlees.mibudget.ui.subscriptions.SubscriptionRow(s, state.tree.byId[s.categoryId ?: ""], onClick = { onOpenSubscription(s.id) }, modifier = Modifier.bleed())
                }
            }
            if (folder != null && state.recent.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Recent purchases", Modifier.weight(1f))
                        TextButton(onClick = { onViewPurchases(folder.id, false) }) { Text("See All ${state.folderPurchaseCount}") }
                    }
                }
                items(state.recent, key = { "p-" + it.id }, span = { GridItemSpan(maxLineSpan) }) { p ->
                    // the same row the start screen shows
                    com.jlees.mibudget.ui.purchases.PurchaseListItem(
                        p = p, category = p.categoryId?.let { state.tree.byId[it] }, method = null,
                        onClick = { onOpenPurchase(p.id) }, modifier = Modifier.bleed(),
                    )
                }
            }
        }
    }

    editing?.let { (cat, isNew) ->
        val deleteAction: () -> Unit = {
            vm.delete(cat)
            editing = null
            if (cat.id == folder?.id) onBack?.invoke()
        }
        CategoryEditSheet(
            initial = cat,
            isNew = isNew,
            tree = state.tree,
            onSave = { vm.save(it); editing = null },
            onDelete = if (isNew) null else deleteAction,
            onDismiss = { editing = null },
        )
    }
    budgetEditing?.let { cat ->
        CategoryEditSheet(
            initial = cat,
            isNew = false,
            tree = state.tree,
            budgetOnly = true,
            onSave = { vm.setBudget(cat, it.budgetCents, it.budgetPeriod); budgetEditing = null },
            onDelete = null,
            onDismiss = { budgetEditing = null },
        )
    }
    if (pickForBudget) {
        CategoryPickerSheet(
            tree = state.tree,
            selectedId = null,
            allowNone = false,
            title = "Budget Which Category?",
            onDismiss = { pickForBudget = false },
            onSelect = { id -> pickForBudget = false; budgetEditing = id?.let { state.tree.byId[it] } },
        )
    }
    if (showTemplates) {
        TemplatePicker(state, onDismiss = { showTemplates = false }, onAdd = { vm.addTemplates(it); showTemplates = false })
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    // a list's group header: 14sp bold onSurfaceVariant
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = modifier.padding(vertical = 10.dp))
}

/** The space under everything in the grid that isn't a list row (rows bring List Style's own gap). */
private val Gap = 12.dp

@Composable
private fun Breadcrumbs(path: List<CategoryEntity>, onJump: (String?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onJump(null) }) { Text("All") }
        path.forEachIndexed { i, c ->
            Icon(Sym.KeyboardArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            if (i == path.lastIndex) Text(c.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp))
            else TextButton(onClick = { onJump(c.id) }) { Text(c.name) }
        }
    }
}

@Composable
private fun FolderHeader(state: CategoriesUiState, onEditBudget: () -> Unit, onViewAll: () -> Unit) {
    val folder = state.folder ?: return
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(folder, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(Money.format(state.folderSpentThisMonth), style = MaterialTheme.typography.headlineMedium)
                    Text("spent this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            val status = state.folderStatus
            if (status != null) {
                Text("${folder.budgetPeriod.label} budget", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                BudgetProgress(status)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onEditBudget,
                    label = { Text(if (status == null) "Set Budget" else "Edit Budget") },
                    leadingIcon = { Icon(Sym.Savings, null, Modifier.size(18.dp)) },
                )
                AssistChip(
                    onClick = onViewAll,
                    label = { Text("${state.folderPurchaseCount} purchases") },
                    leadingIcon = { Icon(Sym.ReceiptLong, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
}

@Composable
private fun FolderCard(item: FolderItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    val c = item.category
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clipClickable(onClick, onLongClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                CategoryBadge(c, size = 48.dp)
                Spacer(Modifier.weight(1f))
                if (item.childCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Sym.Folder, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(2.dp))
                        Text("${item.childCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                Money.format(item.spent) + if (item.status == null) " this month" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.status?.let { s ->
                Spacer(Modifier.height(8.dp))
                BudgetProgress(s, showLabels = false)
                Spacer(Modifier.height(4.dp))
                Text(
                    com.jlees.mibudget.ui.components.budgetStatusWords(s),
                    style = MaterialTheme.typography.labelSmall,
                    color = com.jlees.mibudget.ui.components.budgetTextColor(s),
                )
            }
        }
    }
}

@Composable
private fun UncategorizedCard(count: Int, onClick: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth().clipClickable(onClick, onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            UncategorizedBadge(size = 48.dp)
            Spacer(Modifier.height(12.dp))
            Text("Uncategorized", style = MaterialTheme.typography.titleMedium)
            Text("$count to sort", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
private fun BudgetSummary(state: CategoriesUiState) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("This month", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "${Money.format(state.monthlySpentTotal)} of ${Money.format(state.monthlyBudgetTotal)}",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            val over = state.budgets.count { it.over }
            val ahead = state.budgets.count { it.aheadOfPace }
            Text(
                buildString {
                    append("${state.budgets.size} budgets")
                    if (over > 0) append(" · $over over")
                    if (ahead > 0) append(" · $ahead ahead of pace")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun BudgetRow(s: BudgetStatus, parentPath: String, onClick: () -> Unit) {
    // a mi. list row (Cards or Table, List Spacing's gap) with its progress under it
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.bleed().fillMaxWidth().padding(bottom = com.jlees.mibudget.ui.common.listGap())
            .listRow().clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryBadge(s.category, size = 44.dp)
            Spacer(Modifier.width(15.dp))
            Column(Modifier.weight(1f)) {
                com.jlees.mibudget.ui.common.OneLine(s.category.name, color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                // as every list row's details: the lead in primary, 6, then the rest
                Row {
                    Text(s.category.budgetPeriod.label, Modifier.padding(end = 6.dp), color = cs.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    if (parentPath.isNotEmpty()) Text(parentPath, color = cs.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("${(s.fraction * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = com.jlees.mibudget.ui.components.budgetTextColor(s))
        }
        Spacer(Modifier.height(10.dp))
        BudgetProgress(s)
    }
}

@Composable
private fun TemplatePicker(state: CategoriesUiState, onDismiss: () -> Unit, onAdd: (Set<String>) -> Unit) {
    var selected by rememberSaveable { mutableStateOf(state.missingTemplates.map { it.key }.toSet()) }
    MiDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Default Categories") },
        text = {
            Column(Modifier.height(400.dp).verticalScroll(rememberScrollState())) {
                state.missingTemplates.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = if (t.key in selected) selected - t.key else selected + t.key },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = t.key in selected, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(t.name)
                            if (t.children.isNotEmpty()) Text(
                                t.children.joinToString { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = selected.isNotEmpty(), onClick = { onAdd(selected) }) { Text("Add ${selected.size}") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Rounded by the Corners setting, tap to open, hold to edit. */
@Composable
private fun Modifier.clipClickable(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    this.clip(MaterialTheme.shapes.large).combinedClickable(
        onClick = onClick, onClickLabel = "Open", onLongClick = onLongClick, onLongClickLabel = "Edit category",
    )
