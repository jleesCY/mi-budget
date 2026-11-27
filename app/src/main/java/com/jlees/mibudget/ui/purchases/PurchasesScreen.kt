package com.jlees.mibudget.ui.purchases

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import com.jlees.mibudget.ui.common.listReveal
import androidx.compose.foundation.layout.navigationBars

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.foundation.layout.Box
import com.jlees.mibudget.ui.common.MiPill
import com.jlees.mibudget.ui.common.MainBar
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.jlees.mibudget.data.db.fullyRefunded

import androidx.compose.runtime.saveable.rememberSaveable
import com.jlees.mibudget.ui.common.appear
import com.jlees.mibudget.ui.common.disappear
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import com.jlees.mibudget.ui.components.ChartSlice
import com.jlees.mibudget.ui.components.ChartTypeButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.data.ChartType
import com.jlees.mibudget.data.db.PurchaseEntity
import com.jlees.mibudget.data.db.PurchaseSource
import com.jlees.mibudget.domain.DatePreset
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.components.CategoryPickerSheet
import com.jlees.mibudget.ui.components.ItemIcon
import com.jlees.mibudget.ui.components.PaymentMethodIcon
import com.jlees.mibudget.data.db.displayName
import com.jlees.mibudget.ui.components.SpendingChart
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Composable
fun PurchasesScreen(
    onBack: (() -> Unit)?,
    onAdd: (categoryId: String?) -> Unit,
    onOpen: (String) -> Unit,
    onScan: () -> Unit,
    onOpenSubscription: (String) -> Unit,
    /** Reopen the unfinished new purchase (null = nothing to resume). */
    onResume: (() -> Unit)? = null,
    /** The main bar's pages (null on a filtered list, which has no main bar). */
    sheets: com.jlees.mibudget.ui.common.BarSheets? = null,
    /** A shortcut card's place: a Library page, or the Calendar. */
    onLibrary: (com.jlees.mibudget.ui.navigation.LibraryPage) -> Unit = {},
    onPaymentMethods: () -> Unit = {},
    onCheckSplit: () -> Unit = {},
    onCurrency: () -> Unit = {},
    onTipCalculator: () -> Unit = {},
    vm: PurchasesViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val f = state.filter
    var showFilters by rememberSaveable { mutableStateOf(false) }
    // Long-pressing the Pill turns it into a search field
    var searching by rememberSaveable { mutableStateOf(query.isNotBlank()) }
    var showBulkCategory by rememberSaveable { mutableStateOf(false) }
    val actionPills = com.jlees.mibudget.ui.common.LocalActionPills.current
    var showCategoryFilter by rememberSaveable { mutableStateOf(false) }
    val inSelection = selection.isNotEmpty()
    // Back closes the top layer first: the selection, then searching.
    androidx.activity.compose.BackHandler(enabled = inSelection) { vm.clearSelection() }
    androidx.activity.compose.BackHandler(enabled = !inSelection && searching) { searching = false; vm.setQuery("") }
    val haptics = com.jlees.mibudget.ui.common.LocalHaptics.current
    val settings = com.jlees.mibudget.ui.common.LocalSettings.current
    // a search left alone for a moment goes into Recent Searches (when that's on)
    androidx.compose.runtime.LaunchedEffect(query, searching) {
        if (searching && query.isNotBlank()) { kotlinx.coroutines.delay(1500); vm.rememberSearch(query) }
    }


    val title = when {
        f.categoryIds.size == 1 && vm.isScoped -> state.tree.byId[f.categoryIds.first()]?.name ?: "Purchases"
        f.uncategorizedOnly && vm.isScoped -> "Uncategorized"
        f.paymentMethodIds.size == 1 && vm.isScoped ->
            state.paymentMethods.firstOrNull { it.id == f.paymentMethodIds.first() }?.displayName ?: "Purchases"
        else -> "Purchases"
    }
    var noMore by remember { mutableStateOf(0) } // bumps when a month swipe has nowhere to go (the Pill shakes)
    val pill = com.jlees.mibudget.ui.common.PillSpec(
        text = if (searching) "Search Purchases" else "Add a Purchase",
        onTap = { onAdd(f.categoryIds.singleOrNull()) },
        leading = onResume?.let { com.jlees.mibudget.ui.common.PillAction(Sym.History, "Resume", onClick = it) },
        trailing = if (searching) listOf(com.jlees.mibudget.ui.common.PillAction(Sym.Close, "Stop Searching") { searching = false; vm.setQuery("") })
        else listOf(com.jlees.mibudget.ui.common.PillAction(Sym.PhotoCamera, "Scan a Receipt", accent = true, onClick = onScan)),
        onLongPress = if (settings.longPressSearch) { { searching = true } } else null,
        query = if (searching) query else null,
        onQuery = if (searching) vm::setQuery else null,
        suggestions = if (settings.recentSearches) settings.searchHistory else emptyList(),
        onForgetSuggestion = vm::forgetSearch,
    )

    val list: @Composable (PaddingValues, @Composable () -> Unit) -> Unit = { padding, header ->
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        // the purchases come a chunk at a time, as the browser's History (a month, or a long search, can hold hundreds)
        val total = state.groups.sumOf { it.items.size }
        val count = com.jlees.mibudget.ui.common.rememberChunkedCount(listState, total, reset = f to query)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + com.jlees.mibudget.ui.common.PillSpace),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") { Box(Modifier.monthSwipe(settings.swipeMonths && !vm.isScoped, { dir -> if (!vm.stepMonth(-dir)) noMore++ }, { haptics?.tick() })) { header() } }
            // until the month's purchases are in: the page's skeleton (the total, the chart, the rows), not a "$0.00"
            if (state.loading) { item(key = "skeleton") { if (vm.isScoped) com.jlees.mibudget.ui.common.BoneRows() else com.jlees.mibudget.ui.common.HomeSkeleton() }; return@LazyColumn }
            item(key = "chart", contentType = "chart") { Box(Modifier.monthSwipe(settings.swipeMonths && !vm.isScoped, { dir -> if (!vm.stepMonth(-dir)) noMore++ }, { haptics?.tick() })) {
                HeroChart(
                    state = state,
                    onType = vm::setChartType,
                    onSlice = vm::drillInto,
                    onDrillUp = vm::drillUp,
                )
            } }
            item(key = "chips") {
                QuickFilterRow(
                    state = state,
                    showDate = vm.isScoped,
                    onPreset = { p -> if (p == DatePreset.CUSTOM) showFilters = true else vm.updateFilter { it.copy(datePreset = p) } },
                    onPickCategory = { showCategoryFilter = true },
                    onClearCategory = { id -> vm.updateFilter { it.copy(categoryIds = it.categoryIds - id) } },
                    onClearUncategorized = { vm.updateFilter { it.copy(uncategorizedOnly = false) } },
                    onClearAll = { vm.clearFilters() },
                )
            }
            if (state.settings.nudgeUncategorized && state.uncategorizedCount > 0 && !f.uncategorizedOnly) {
                item(key = "nudge") {
                    UncategorizedNudge(state.uncategorizedCount) {
                        vm.updateFilter { it.copy(uncategorizedOnly = true, categoryIds = emptySet(), datePreset = DatePreset.ALL) }
                    }
                }
            }
            val o = state.overview
            if (!vm.isScoped && o.trialsEndingSoon.isNotEmpty()) item(key = "trials") {
                Card(
                    onClick = { onOpenSubscription(o.trialsEndingSoon.first().id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Sym.HourglassBottom, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(12.dp))
                        // Ended trials (now being charged) come first; see Subscriptions to keep or cancel.
                        val ended = o.trialsEndingSoon.filter { com.jlees.mibudget.domain.Renewals.trialEnded(it, java.time.LocalDate.now()) }
                        val soon = o.trialsEndingSoon - ended.toSet()
                        Text(
                            listOfNotNull(
                                ended.takeIf { it.isNotEmpty() }?.let { l -> "Free trial ended: " + l.joinToString { it.name } },
                                soon.takeIf { it.isNotEmpty() }?.let { l -> "Free trial ending soon: " + l.joinToString { it.name } },
                            ).joinToString(" · "),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (!vm.isScoped && !searching) item(key = "shortcuts", contentType = "shortcuts") {
                Shortcuts(state, onOpenSubscription, onLibrary)
            }
            if (!state.loading && state.count == 0) {
                item(key = "empty") {
                    com.jlees.mibudget.ui.common.EmptyLine(
                        icon = Sym.ReceiptLong,
                        text = if (state.canClear || searching) "No Matches" else "No Purchases Yet",
                        action = if (state.canClear) {
                            { FilledTonalButton(onClick = { vm.clearFilters() }) { Text("Clear Filters") } }
                        } else null,
                    )
                }
            }
            var left = count
            state.groups.forEach { group ->
                if (left <= 0) return@forEach
                val rows = group.items.take(left)
                left -= rows.size
                if (group.date != LocalDate.MIN) {
                    stickyHeader(key = "h-${group.date}", contentType = "day-header") {
                        // the header covers rows scrolling under it, so it takes the page's colour: a sheet's when scoped
                        DayHeader(group.date, group.total, if (vm.isScoped) MaterialTheme.colorScheme.surfaceContainerLow else with(com.jlees.mibudget.ui.theme.LocalMi.current) { MaterialTheme.colorScheme.homeBackground })
                    }
                }
                items(rows, key = { it.id }, contentType = { "purchase" }) { p ->
                    SwipeRow(
                        // swipe right: refund it (not money that came in, nor one already refunded); left: delete
                        canRefund = p.amountCents > 0 && !p.fullyRefunded && !inSelection,
                        enabled = !inSelection,
                        // at once, with Undo (a deleted one waits in Recently Deleted too): nothing Undo can bring
                        // back asks first
                        onRefund = { vm.refundOne(p) },
                        onDelete = { vm.deleteOne(p) },
                        tick = { haptics?.tick() },
                        modifier = Modifier.animateItem(fadeInSpec = null).padding(bottom = com.jlees.mibudget.ui.common.listGap()),
                    ) {
                        PurchaseListItem(
                            p = p,
                            category = p.categoryId?.let { state.tree.byId[it] },
                            method = p.paymentMethodId?.let { state.methodsById[it] },
                            selected = p.id in selection,
                            onClick = { if (inSelection) vm.toggleSelect(p.id) else onOpen(p.id) },
                            onLongClick = { haptics?.longPress(); vm.toggleSelect(p.id) },
                            gap = false,
                        )
                    }
                }
            }
        }
    }

    val selectionBar: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = vm::clearSelection) { Icon(Sym.Close, "Clear Selection") }
            Text("${selection.size} Selected", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showBulkCategory = true }) { Icon(Sym.DriveFileMove, "Set Category") }
            IconButton(onClick = {
                // at once, with Undo; they wait in Recently Deleted too
                val batch = vm.deleteSelected(state.groups.flatMap { it.items })
                val count = batch.size
                com.jlees.mibudget.ui.common.Notices.undo("Deleted ${com.jlees.mibudget.domain.plural(count, "purchase")}", onGone = { vm.forgetDeleted(batch) }) { vm.undoDelete(batch) }
            }) { Icon(Sym.Delete, "Delete") }
        }
    }
    val filterButton: @Composable () -> Unit = {
        IconButton(onClick = { showFilters = true }) {
            BadgedBox(badge = { if (f.activeCount > 0) Badge { Text("${f.activeCount}") } }) {
                Icon(Sym.Tune, "Filters")
            }
        }
    }

    if (vm.isScoped) {
        com.jlees.mibudget.ui.common.MiScaffold(
            title = title, onBack = onBack,
            actions = { filterButton() },
            pill = if (inSelection) null else pill,
        ) { padding -> list(padding) { if (inSelection) selectionBar() } }
    } else androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = with(com.jlees.mibudget.ui.theme.LocalMi.current) { MaterialTheme.colorScheme.homeBackground }) { Box(Modifier.fillMaxSize()) {
        // the list stays below the status bar (rows never scroll up behind it); at the bottom it scrolls on behind the
        // see-through navigation bar, the last row ending clear above the main bar
        Box(Modifier.fillMaxSize().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars.union(androidx.compose.foundation.layout.WindowInsets.displayCutout).only(androidx.compose.foundation.layout.WindowInsetsSides.Top)).clipToBounds()) {
            list(PaddingValues(bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())) {
                if (inSelection) selectionBar()
                else HomeHeader(
                    title = com.jlees.mibudget.domain.MonthNav.title(f, state.today),
                    preset = f.datePreset,
                    onPreset = { p -> if (p == DatePreset.CUSTOM) showFilters = true else vm.updateFilter { it.copy(datePreset = p, customRange = null) } },
                )
            }
        }
        val shake = com.jlees.mibudget.ui.common.shakeOffset(noMore % 2 == 1)
        var menuOpen by remember { mutableStateOf(false) }
        var menuAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        var addOpen by remember { mutableStateOf(false) }
        var addAnchor by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
        val bar = sheets
        if (!inSelection && bar != null) Box(Modifier.fillMaxSize().graphicsLayer { translationX = shake * density }) {
            // holding the main bar's Pill searches: it widens into a search Pill (Back or ✕ turns it back)
            androidx.compose.animation.AnimatedVisibility(
                searching, Modifier.fillMaxSize(),
                enter = androidx.compose.animation.scaleIn(com.jlees.mibudget.ui.common.Motion.noBounce(), initialScale = 0.5f) +
                    androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200, delayMillis = 60)),
                exit = androidx.compose.animation.fadeOut(com.jlees.mibudget.ui.common.Motion.exit(90)),
            ) { Box(Modifier.fillMaxSize()) { MiPill(pill, focusOnShow = true) } }
            androidx.compose.animation.AnimatedVisibility(
                !searching, Modifier.fillMaxSize(),
                enter = androidx.compose.animation.fadeIn(com.jlees.mibudget.ui.common.Motion.enter(200)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)) + androidx.compose.animation.scaleOut(targetScale = 0.9f),
            ) {
                Box(Modifier.fillMaxSize()) {
                    MainBar(
                        plus = com.jlees.mibudget.ui.common.BarButton(Sym.Add, "Add Purchase") { addOpen = true },
                        left = com.jlees.mibudget.ui.common.BarButton(Sym.CalendarMonth, "Calendar") { bar.open(com.jlees.mibudget.ui.common.BarPage.UP) },
                        right = com.jlees.mibudget.ui.common.BarButton(Sym.MoreVert, "Menu") { menuOpen = true },
                        leftEdge = com.jlees.mibudget.ui.common.BarButton(Sym.LibraryBooks, "Library") { bar.open(com.jlees.mibudget.ui.common.BarPage.LEFT) },
                        rightEdge = com.jlees.mibudget.ui.common.BarButton(Sym.Settings, "Settings") { bar.open(com.jlees.mibudget.ui.common.BarPage.RIGHT) },
                        onPageDrag = bar::drag,
                        onPageRelease = { _, open, speed -> bar.settle(open, speed) },
                        onLongPress = if (settings.longPressSearch) { { searching = true } } else null,
                        onPillBounds = { bar.barPill = it }, onRightBounds = { menuAnchor = it }, onPlusBounds = { addAnchor = it },
                        // the bar's Pill steps aside while a page's Pill (morphing out of it) takes its place; until that Pill
                        // knows where it rests (the very first open), the bar's stays, so there's never a frame without one
                        // and while a page opened from here shows its split Pill grown out of this one
                        alpha = { if ((bar.page != null && bar.sheetPill != null) || actionPills?.covers(com.jlees.mibudget.ui.common.PillOrigin.BAR) == true) 0f else 1f },
                    )
                }
            }
        }
        // the +: scan a receipt or type it in
        com.jlees.mibudget.ui.common.AddPrompt(
            open = addOpen, origin = addAnchor, onDismiss = { addOpen = false }, title = "Add a Purchase",
            onScan = onScan, onManual = { onAdd(f.categoryIds.singleOrNull()) }, onResume = onResume,
        )
        // the ⋮: everything about this month, in the item menu growing from it
        val o = state.overview
        ItemMenuFor(
            open = menuOpen, origin = menuAnchor, onDismiss = { menuOpen = false },
            title = com.jlees.mibudget.domain.MonthNav.title(f, state.today),
            subtitle = "${Money.format(state.total)} spent · ${com.jlees.mibudget.domain.plural(state.count, "purchase")}",
            filter = f, canClear = state.canClear,
            onSearch = { searching = true }, onFilters = { showFilters = true },
            onCheckSplit = onCheckSplit, onCurrency = onCurrency, onTipCalculator = onTipCalculator,
            onSubscriptions = { onLibrary(com.jlees.mibudget.ui.navigation.LibraryPage.SUBSCRIPTIONS) },
            onPaymentMethods = onPaymentMethods,
            onClear = { vm.clearFilters() }, renewalsSoon = o.upcoming.size,
        )
    } }

    if (showFilters) {
        FilterSheet(
            initial = f.copy(query = query),
            base = vm.baseFilter,
            tree = state.tree,
            paymentMethods = state.paymentMethods,
            onApply = { vm.setFilter(it); showFilters = false },
            onDismiss = { showFilters = false },
        )
    }
    if (showCategoryFilter) {
        CategoryPickerSheet(
            tree = state.tree,
            selectedId = f.categoryIds.singleOrNull(),
            title = "Filter by Category",
            noneLabel = "Uncategorized only",
            onDismiss = { showCategoryFilter = false },
            onSelect = { id ->
                vm.updateFilter {
                    if (id == null) it.copy(uncategorizedOnly = true, categoryIds = emptySet())
                    else it.copy(categoryIds = it.categoryIds + id, uncategorizedOnly = false)
                }
                showCategoryFilter = false
            },
        )
    }
    if (showBulkCategory) {
        CategoryPickerSheet(
            tree = state.tree,
            selectedId = null,
            title = "Move ${selection.size} to…",
            onDismiss = { showBulkCategory = false },
            onSelect = { vm.categorizeSelected(it); showBulkCategory = false },
        )
    }
}

/**
 * The start screen's top: the month (or range) as a big title (tap it for other ranges; swipe the Pill for the next
 * month), filters, and Settings.
 */
@Composable
private fun HomeHeader(title: String, preset: DatePreset, onPreset: (DatePreset) -> Unit) {
    var menu by rememberSaveable { mutableStateOf(false) }
    ClockAndDate()
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            Row(
                Modifier.clip(CircleShape).clickable(onClickLabel = "Change dates") { menu = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Icon(Sym.ArrowDropDown, null)
            }
            com.jlees.mibudget.ui.common.MiMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DatePreset.entries.forEach { p ->
                    DropdownMenuItem(
                        text = { Text(if (p == DatePreset.CUSTOM) "Custom Range…" else p.label.split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }) },
                        trailingIcon = { if (p == preset) Icon(Sym.Check, null) },
                        onClick = { menu = false; onPreset(p) },
                    )
                }
            }
        }
    }
}

/**
 * A row that can be swiped: right to refund it ([canRefund]), left to delete it. It only takes the gesture when it's
 * clearly sideways (SwipeRules.SIDEWAYS: scrolling the list past it, even a little diagonally, never moves it), and acts
 * only far enough (30% of the row) or on a hard flick already 12% of the way (SwipeRules.acts), with a haptic tick as it
 * arms. Its colour fades in from 40% and its icon grows (SwipeRules). Letting go there asks (refund or delete) and the
 * row springs back home, whatever's answered; short of it, it springs back.
 */
@Composable
private fun SwipeRow(
    canRefund: Boolean, enabled: Boolean, onRefund: () -> Unit, onDelete: () -> Unit, tick: () -> Unit,
    modifier: Modifier = Modifier, content: @Composable () -> Unit,
) {
    val rules = com.jlees.mibudget.ui.common.SwipeRules
    val tx = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var width by remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    val refund by androidx.compose.runtime.rememberUpdatedState(onRefund)
    val delete by androidx.compose.runtime.rememberUpdatedState(onDelete)
    val canRight by androidx.compose.runtime.rememberUpdatedState(canRefund)
    val progress = { tx.value / width }
    Box(modifier.fillMaxWidth().onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }) {
        // what's behind: only the side being uncovered, in its action's colour, taking the row's shape
        val p = progress()
        if (p != 0f) {
            val refunding = p > 0
            val cs = MaterialTheme.colorScheme
            val bg = if (refunding) cs.primaryContainer else cs.errorContainer
            val ink = if (refunding) cs.onPrimaryContainer else cs.onErrorContainer
            Row(
                Modifier.matchParentSize().listReveal().graphicsLayer { alpha = rules.colorAlpha(kotlin.math.abs(progress())) }.background(bg).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (refunding) Arrangement.Start else Arrangement.End,
            ) {
                Icon(if (refunding) Sym.Undo else Sym.Delete, null,
                    Modifier.graphicsLayer { val sc = rules.iconScale(kotlin.math.abs(progress())); scaleX = sc; scaleY = sc }, tint = ink)
            }
        }
        Box(
            Modifier.fillMaxWidth().graphicsLayer { translationX = tx.value }
                // screen readers can't swipe: the same actions, on the row
                .semantics {
                    customActions = listOfNotNull(
                        if (canRefund) androidx.compose.ui.semantics.CustomAccessibilityAction("Refund") { refund(); true } else null,
                        androidx.compose.ui.semantics.CustomAccessibilityAction("Delete") { delete(); true },
                    )
                }
                .then(if (!enabled) Modifier else Modifier.pointerInput(Unit) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // wait until it's clearly one way: sideways (and SIDEWAYS times more than up or down) takes it;
                        // up or down first leaves it to the list's scrolling
                        var total = androidx.compose.ui.geometry.Offset.Zero
                        var claimed = false
                        while (true) {
                            val e = awaitPointerEvent()
                            val c = e.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                            if (!c.pressed || c.isConsumed) return@awaitEachGesture
                            total += c.positionChange()
                            val ax = kotlin.math.abs(total.x); val ay = kotlin.math.abs(total.y)
                            if (ay > slop && ay * rules.SIDEWAYS >= ax) return@awaitEachGesture
                            if (ax > slop && ax >= ay * rules.SIDEWAYS && (total.x < 0 || canRight)) { claimed = true; c.consume(); break }
                        }
                        if (!claimed) return@awaitEachGesture
                        scope.launch { tx.stop() }
                        val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                        var armed = false
                        // where the row is, kept here (the animatable updates a frame later)
                        var pos = tx.value
                        horizontalDrag(down.id) { c ->
                            tracker.addPosition(c.uptimeMillis, c.position)
                            pos = (pos + c.positionChange().x).let { if (canRight) it else it.coerceAtMost(0f) }
                            val now = pos
                            scope.launch { tx.snapTo(now) }
                            c.consume()
                            val armedNow = kotlin.math.abs(pos / width) >= rules.ARM_AT
                            if (armedNow != armed) { armed = armedNow; tick() }
                        }
                        val speed = tracker.calculateVelocity().x / density // dp a second
                        val at = pos / width
                        if (rules.acts(at, speed)) { if (at > 0) refund() else delete() }
                        // back home either way (an action asks first; the row isn't left out)
                        scope.launch { tx.animateTo(0f, com.jlees.mibudget.ui.common.Motion.spatial(), initialVelocity = speed * density) }
                    }
                }),
        ) { content() }
    }
}

@Composable
private fun QuickFilterRow(
    state: PurchasesUiState,
    /** The start screen picks dates with its title instead. */
    showDate: Boolean = true,
    onPreset: (DatePreset) -> Unit,
    onPickCategory: () -> Unit,
    onClearCategory: (String) -> Unit,
    onClearUncategorized: () -> Unit,
    onClearAll: () -> Unit,
) {
    val f = state.filter
    var presetMenu by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDate) Column {
            FilterChip(
                selected = true,
                onClick = { presetMenu = true },
                label = { Text(f.datePreset.label) },
                leadingIcon = { Icon(Sym.DateRange, null, Modifier.size(18.dp)) },
                trailingIcon = { Icon(Sym.ArrowDropDown, null, Modifier.size(18.dp)) },
            )
            com.jlees.mibudget.ui.common.MiMenu(expanded = presetMenu, onDismissRequest = { presetMenu = false }) {
                DatePreset.entries.forEach { p ->
                    DropdownMenuItem(text = { Text(p.label) }, onClick = { presetMenu = false; onPreset(p) })
                }
            }
        }
        f.categoryIds.forEach { id ->
            val c = state.tree.byId[id]
            InputChip(
                selected = true,
                onClick = { onClearCategory(id) },
                label = { Text(c?.name ?: "Deleted category") },
                trailingIcon = { Icon(Sym.Close, "Remove", Modifier.size(18.dp)) },
            )
        }
        if (f.uncategorizedOnly) InputChip(
            selected = true,
            onClick = onClearUncategorized,
            label = { Text("Uncategorized") },
            trailingIcon = { Icon(Sym.Close, "Remove", Modifier.size(18.dp)) },
        )
        FilterChip(
            selected = false,
            onClick = onPickCategory,
            label = { Text(if (f.categoryIds.isEmpty()) "Category" else "Add category") },
            leadingIcon = { Icon(Sym.Folder, null, Modifier.size(18.dp)) },
        )
        if (state.canClear) TextButton(onClick = onClearAll) { Text("Clear All") }
    }
}

@Composable
private fun UncategorizedNudge(count: Int, onReview: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Sym.Lightbulb, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                "$count purchase${if (count == 1) " needs" else "s need"} a category so budgets stay accurate.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onReview) { Text("Review") }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate, total: Long, background: androidx.compose.ui.graphics.Color) {
    val label = com.jlees.mibudget.ui.components.dayLabel(date)
    Row(
        // as mi. Browser's list headers: 20 in, 10 above and below, 14sp bold onSurfaceVariant
        Modifier.fillMaxWidth().background(background).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(Money.format(total), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}


/** Large, centered chart with the total spent right underneath it. */
@Composable
private fun HeroChart(
    state: PurchasesUiState,
    onType: (ChartType) -> Unit,
    onSlice: (ChartSlice) -> Unit,
    onDrillUp: () -> Unit,
) {
    val o = state.overview
    val f = state.filter
    // Month-to-date comparisons only make sense for the plain "this month, everything" view.
    val plainMonth = f.datePreset == DatePreset.THIS_MONTH && f.activeCount == 0 && state.chartParent == null
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // what this month comes to first, then where it went (the chart and its tags), then the purchases
        Text(
            Money.format(state.total),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Text(
            "spent ${state.rangeLabel} · ${com.jlees.mibudget.domain.plural(state.count, "purchase")}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (plainMonth && !state.loading) {
            o.deltaPercent?.let { d ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        if (d > 0) Sym.TrendingUp else Sym.TrendingDown, null, Modifier.size(18.dp),
                        tint = if (d > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${if (d > 0) "+" else ""}$d% vs. same point last month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (o.monthlyBudget > 0) {
                Spacer(Modifier.height(12.dp))
                val fraction = (o.budgetedSpent.toFloat() / o.monthlyBudget).coerceIn(0f, 1f)
                com.jlees.mibudget.ui.components.ProgressBar(
                    fraction, if (o.budgetRemaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    Modifier.fillMaxWidth(0.8f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (o.budgetRemaining >= 0)
                        "${Money.format(o.budgetRemaining)} left of ${Money.format(o.monthlyBudget)} · about ${Money.format(o.perDayLeft)}/day"
                    else "${Money.format(-o.budgetRemaining)} over your ${Money.format(o.monthlyBudget)} monthly budgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (state.chartParent != null) {
                IconButton(onClick = onDrillUp) { Icon(Sym.ArrowUpward, "Up one level") }
            } else Spacer(Modifier.size(48.dp))
            Text(
                state.chartParent?.let { state.tree.pathLabel(it) } ?: "",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ChartTypeButton(state.settings.chartType, onType)
        }
        SpendingChart(
            type = state.settings.chartType,
            slices = state.slices,
            daily = state.daily,
            caption = "No spending ${state.rangeLabel}",
            onSliceClick = onSlice,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val ShortcutWidth = 220.dp
private val ShortcutHeight = 168.dp

/**
 * Jump-off cards to the other tabs (what the Home tab used to hold). Fixed size, so they never
 * squash while scrolling sideways.
 */
@Composable
private fun Shortcuts(state: PurchasesUiState, onOpenSubscription: (String) -> Unit, onLibrary: (com.jlees.mibudget.ui.navigation.LibraryPage) -> Unit) {
    val o = state.overview
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "renewals") {
            ShortcutCard("Upcoming Renewals", Sym.Autorenew, { onLibrary(com.jlees.mibudget.ui.navigation.LibraryPage.SUBSCRIPTIONS) }) {
                if (o.upcoming.isEmpty()) {
                    ShortcutEmpty("Nothing in the next 2 weeks")
                } else {
                    Text(
                        "${Money.format(o.upcomingTotal)} in 2 weeks",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    o.upcoming.take(3).forEach { u ->
                        Row(
                            Modifier.fillMaxWidth().clip(CircleShape).clickable { onOpenSubscription(u.subscription.id) }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ItemIcon(u.subscription.brandKey, u.subscription.name, u.subscription.categoryId?.let { state.tree.byId[it] }, size = 22.dp)
                            Spacer(Modifier.width(6.dp))
                            Text(u.subscription.name, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(shortDue(u.date, state.today), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
        item(key = "budgets") {
            ShortcutCard("Budgets", Sym.Savings, { onLibrary(com.jlees.mibudget.ui.navigation.LibraryPage.BUDGETS) }) {
                if (o.budgets.isEmpty()) {
                    ShortcutEmpty("Give a category a budget to track it here")
                } else {
                    Text(
                        "${com.jlees.mibudget.domain.plural(o.budgetCount, "budget")} · closest to the limit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    o.budgets.take(3).forEach { b ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(b.category.name, Modifier.width(76.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(6.dp))
                            com.jlees.mibudget.ui.components.ProgressBar(
                                b.fraction, if (b.over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("${(b.fraction * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item(key = "week") {
            ShortcutCard("Last 7 Days", Sym.CalendarMonth, { onLibrary(com.jlees.mibudget.ui.navigation.LibraryPage.CALENDAR) }) {
                Text(
                    Money.format(o.week.sumOf { it.second }),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                WeekBars(o.week, state.today, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun ShortcutCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.width(ShortcutWidth).height(ShortcutHeight),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Sym.KeyboardArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun ShortcutEmpty(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

private fun shortDue(date: LocalDate, today: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when (days) {
        0L -> "today"
        1L -> "tmrw"
        else -> "${days}d"
    }
}

@Composable
private fun WeekBars(week: List<Pair<LocalDate, Long>>, today: LocalDate, modifier: Modifier) {
    val max = (week.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        week.forEach { (day, cents) ->
            val isToday = day == today
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val frac = (cents.toFloat() / max).coerceIn(0f, 1f)
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (cents > 0) maxOf(frac, 0.06f) else 0.04f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isToday) 1f else if (cents > 0) 0.45f else 0.15f))
                    )
                }
                Text(
                    day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Start Screen › Clock and Date: the date (16sp semibold) and the clock (72sp bold), 15 apart, starting near the top. */
@Composable
private fun ClockAndDate() {
    val s = com.jlees.mibudget.ui.common.LocalSettings.current
    if (s.clock == com.jlees.mibudget.data.ClockStyle.NONE && s.date == com.jlees.mibudget.data.DatePlacement.NONE) return
    var now by remember { mutableStateOf(java.time.LocalDateTime.now()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000L - System.currentTimeMillis() % 60_000L); now = java.time.LocalDateTime.now() } }
    val date: @Composable () -> Unit = {
        Text(now.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
    Column(Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)) {
        if (s.date == com.jlees.mibudget.data.DatePlacement.ABOVE) date()
        if (s.clock != com.jlees.mibudget.data.ClockStyle.NONE) Text(
            now.format(java.time.format.DateTimeFormatter.ofPattern(if (s.clock == com.jlees.mibudget.data.ClockStyle.H24) "HH:mm" else "h:mm")),
            color = MaterialTheme.colorScheme.onSurface, fontSize = 72.sp, fontWeight = FontWeight.Bold,
        )
        if (s.date == com.jlees.mibudget.data.DatePlacement.BELOW) date()
    }
}

/**
 * Swiping the month (the start screen's title and chart) sideways: right for the month before, left for the one after.
 * Only sideways drags: the list still scrolls, and the chart's touch-and-hold still previews slices.
 */
private fun Modifier.monthSwipe(enabled: Boolean, onStep: (Int) -> Unit, tick: () -> Unit): Modifier = if (!enabled) this else pointerInput(Unit) {
    var dx = 0f
    var armed = false
    val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
    detectHorizontalDragGestures(
        onDragStart = { dx = 0f; armed = false; tracker.resetTracking() },
        onDragEnd = {
            val vx = tracker.calculateVelocity().x / density / 1000f // dp/ms
            val dir = com.jlees.mibudget.ui.common.PillRules.side(dx / density, vx)
            if (dir != 0) onStep(dir)
        },
        onHorizontalDrag = { change, amount ->
            dx += amount
            tracker.addPosition(change.uptimeMillis, change.position)
            val now = com.jlees.mibudget.ui.common.PillRules.side(dx / density, 0f) != 0
            if (now != armed) { tick(); armed = now }
        },
    )
}

/** The start screen's item menu: Search and Filters, the app's tools, and links to renewals and payment methods. */
@Composable
private fun ItemMenuFor(
    open: Boolean, origin: androidx.compose.ui.geometry.Rect?, onDismiss: () -> Unit, title: String, subtitle: String,
    filter: com.jlees.mibudget.domain.PurchaseFilter, canClear: Boolean,
    onSearch: () -> Unit, onFilters: () -> Unit,
    onCheckSplit: () -> Unit, onCurrency: () -> Unit, onTipCalculator: () -> Unit,
    onSubscriptions: () -> Unit, onPaymentMethods: () -> Unit, onClear: () -> Unit, renewalsSoon: Int,
) {
    com.jlees.mibudget.ui.common.ItemMenu(
        open = open, origin = origin, onDismiss = onDismiss, icon = Sym.CalendarMonth, title = title, subtitle = subtitle,
        tiles = listOf(
            com.jlees.mibudget.ui.common.MenuTile(Sym.Search, "Search", onClick = onSearch),
            com.jlees.mibudget.ui.common.MenuTile(Sym.Tune, "Filters", on = filter.activeCount > 0, onClick = onFilters),
        ),
        // the tools built into the app (the same as the Library's Tools page)
        tools = listOf(
            com.jlees.mibudget.ui.common.MenuTool(Sym.CallSplit, "Split Check", onClick = onCheckSplit),
            com.jlees.mibudget.ui.common.MenuTool(Sym.CurrencyExchange, "Converter", onClick = onCurrency),
            com.jlees.mibudget.ui.common.MenuTool(Sym.Percent, "Tip", onClick = onTipCalculator),
        ),
        rows = listOfNotNull(
            com.jlees.mibudget.ui.common.MenuRow(Sym.Autorenew, if (renewalsSoon > 0) "Renewals · $renewalsSoon soon" else "Renewals", onClick = onSubscriptions),
            com.jlees.mibudget.ui.common.MenuRow(Sym.CreditCard, "Payment Methods", onClick = onPaymentMethods),
            if (canClear) com.jlees.mibudget.ui.common.MenuRow(Sym.Close, "Clear Filters", onClick = onClear) else null,
        ),
    )
}
