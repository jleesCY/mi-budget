package com.jlees.mibudget.ui.calendar

import androidx.compose.ui.text.withStyle
import androidx.compose.runtime.setValue

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import com.jlees.mibudget.ui.purchases.PurchaseListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.components.ItemIcon
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.sqrt

private const val PAGE_COUNT = 1200
private const val START_PAGE = PAGE_COUNT / 2
private val CELL_HEIGHT: Dp = 58.dp
private val CELL_GAP: Dp = 4.dp
private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
fun CalendarScreen(
    onOpenPurchase: (String) -> Unit,
    onOpenSubscription: (String) -> Unit,
    onAddPurchase: (LocalDate) -> Unit,
    vm: CalendarViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val data by vm.data.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val baseMonth = remember { YearMonth.now() }
    val pager = rememberPagerState(initialPage = START_PAGE + monthsBetween(baseMonth, YearMonth.from(selected))) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val visibleMonth = baseMonth.plusMonths((pager.currentPage - START_PAGE).toLong())

    // Keep the selected day inside the month being shown.
    LaunchedEffect(visibleMonth) {
        if (YearMonth.from(vm.selected.value) != visibleMonth) {
            vm.select(if (YearMonth.from(data.today) == visibleMonth) data.today else visibleMonth.atDay(1))
        }
    }

    val monthRenewals = rememberRenewals(data, visibleMonth)
    val monthTotal = remember(data, visibleMonth) { data.monthTotal(visibleMonth) }
    val upcomingInMonth = remember(monthRenewals) { monthRenewals.values.flatten().filter { it.projected } }

    var ownQuery by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val q = ownQuery.trim()
    com.jlees.mibudget.ui.common.MiScaffold(
        title = "Calendar",
        // the day's purchases, searched from the Pill; adding is the button above it
        pill = com.jlees.mibudget.ui.common.PillSpec("Search This Day", onTap = {}, query = ownQuery, onQuery = { ownQuery = it }),
        fabs = com.jlees.mibudget.ui.common.SheetFabs(com.jlees.mibudget.ui.common.BarButton(Sym.Add, "Add Purchase") { onAddPurchase(selected) }, vivid = true),
        actions = {
                    TextButton(onClick = {
                        vm.select(data.today)
                        scope.launch { pager.animateScrollToPage(START_PAGE + monthsBetween(baseMonth, YearMonth.from(data.today))) }
                    }) {
                        Icon(Sym.Today, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Today")
                    }
                },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                        Icon(Sym.KeyboardArrowLeft, "Previous month")
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(visibleMonth.format(monthTitle), style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        Text(
                            buildString {
                                append("${Money.format(monthTotal)} spent")
                                if (upcomingInMonth.isNotEmpty()) append(" · ${com.jlees.mibudget.domain.plural(upcomingInMonth.size, "upcoming renewal")}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                        Icon(Sym.KeyboardArrowRight, "Next month")
                    }
                }
            }
            item(key = "weekdays") { WeekdayRow(data) }
            item(key = "grid") {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxWidth().height(CELL_HEIGHT * 6 + CELL_GAP * 5),
                    key = { it },
                ) { page ->
                    val month = baseMonth.plusMonths((page - START_PAGE).toLong())
                    MonthGrid(month, data, selected, onSelect = vm::select)
                }
            }

            // ----- Selected day -----
            val dayPurchases = data.purchasesByDate[selected].orEmpty().filter { q.isEmpty() || it.merchant.contains(q, ignoreCase = true) }
            // Logged payments already appear as purchases; list upcoming + unlogged past renewals here.
            val dayRenewals = monthRenewals[selected].orEmpty().filter { it.projected || !it.logged }
            item(key = "day-header") {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            com.jlees.mibudget.ui.components.dayLabel(selected, data.today),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${Money.format(data.totalsByDate[selected] ?: 0L)} · ${com.jlees.mibudget.domain.plural(dayPurchases.size, "purchase")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AssistChip(
                        onClick = { onAddPurchase(selected) },
                        label = { Text("Purchase") },
                        leadingIcon = { Icon(Sym.Add, null, Modifier.size(18.dp)) },
                    )
                }
            }
            if (data.loading) item(key = "skeleton") { com.jlees.mibudget.ui.common.BoneRows(3) }
            items(dayRenewals, key = { "r-" + (it.subscriptionId ?: it.name) }) { r ->
                // a renewal on this day: the same mi. list row as a purchase, its state in tertiary, ↻ by the price
                val cs = MaterialTheme.colorScheme
                com.jlees.mibudget.ui.common.MiListRow(
                    title = r.name,
                    lead = androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(color = cs.tertiary)) { append(if (r.projected) "Renews" else "Not logged") }
                    },
                    details = androidx.compose.ui.text.AnnotatedString(r.categoryId?.let { data.tree.byId[it]?.name } ?: "Subscription"),
                    leading = { ItemIcon(r.brandKey, r.name, r.categoryId?.let { data.tree.byId[it] }) },
                    onClick = r.subscriptionId?.let { id -> { onOpenSubscription(id) } },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Sym.Autorenew, null, Modifier.size(16.dp), tint = cs.tertiary)
                            Spacer(Modifier.width(4.dp))
                            Text(Money.format(r.amountCents), color = cs.onSurface, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                        }
                    },
                )
            }
            items(dayPurchases, key = { it.id }) { p ->
                // Exactly the row the Purchases tab shows: category, payment method, receipt & subscription marks.
                PurchaseListItem(
                    p = p,
                    category = p.categoryId?.let { data.tree.byId[it] },
                    method = p.paymentMethodId?.let { data.paymentMethods[it] },
                    onClick = { onOpenPurchase(p.id) },
                )
            }
            if (dayPurchases.isEmpty() && dayRenewals.isEmpty()) item(key = "day-empty") {
                // The empty state every mi. list uses.
                com.jlees.mibudget.ui.common.EmptyLine(com.jlees.mibudget.ui.common.Sym.CalendarToday, "Nothing This Day")
            }
        }
    }
}

private fun monthsBetween(a: YearMonth, b: YearMonth): Int = (b.year - a.year) * 12 + (b.monthValue - a.monthValue)

@Composable
private fun WeekdayRow(data: CalendarData) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        repeat(7) { i ->
            val dow = data.firstDayOfWeek.plus(i.toLong())
            Text(
                dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * A month's renewal markers, worked out off the UI thread (subscription history can go back years)
 * and cached per data snapshot, so swiping between months stays smooth.
 */
@Composable
private fun rememberRenewals(data: CalendarData, month: YearMonth): Map<LocalDate, List<RenewalMark>> {
    val state = androidx.compose.runtime.produceState(data.cachedRenewals(month) ?: emptyMap(), data, month) {
        data.cachedRenewals(month)?.let { value = it; return@produceState }
        value = emptyMap() // never show another month's markers while this one is worked out
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { data.renewalsIn(month) }
    }
    return state.value
}

@Composable
private fun MonthGrid(month: YearMonth, data: CalendarData, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val renewals = rememberRenewals(data, month)
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value - data.firstDayOfWeek.value + 7) % 7
    val gridStart = first.minusDays(offset.toLong())
    val maxDay = remember(data, month) {
        (1..month.lengthOfMonth()).maxOfOrNull { data.totalsByDate[month.atDay(it)] ?: 0L }?.coerceAtLeast(1L) ?: 1L
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
        repeat(6) { week ->
            Row(Modifier.fillMaxWidth().height(CELL_HEIGHT), horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                repeat(7) { d ->
                    val date = gridStart.plusDays((week * 7 + d).toLong())
                    if (date.month != month.month) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DayCell(
                            date = date,
                            total = data.totalsByDate[date] ?: 0L,
                            maxTotal = maxDay,
                            renewalCount = renewals[date]?.size ?: 0,
                            purchaseCount = data.purchasesByDate[date]?.size ?: 0,
                            isSelected = date == selected,
                            isToday = date == data.today,
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    total: Long,
    maxTotal: Long,
    renewalCount: Int,
    purchaseCount: Int,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    // Spending heat: square-root scale so small days are still visible next to a rent day.
    val intensity = if (total > 0) sqrt(total.toFloat() / maxTotal).coerceIn(0f, 1f) else 0f
    val bg = when {
        isSelected -> scheme.primary
        total > 0 -> scheme.primary.copy(alpha = 0.08f + 0.30f * intensity)
        else -> Color.Transparent
    }
    val fg = if (isSelected) scheme.onPrimary else scheme.onSurface
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(bg)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, scheme.primary, shape) else Modifier)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.fillMaxSize().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Day number top-left, renewal mark top-right — they never overlap.
            Text(
                date.dayOfMonth.toString(),
                modifier = Modifier.align(Alignment.Start).padding(start = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday && !isSelected) scheme.primary else fg,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            if (total != 0L) Text(
                Money.formatCompact(total),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        // Top-right corner: how many purchases that day, with the renewal mark under it.
        Column(
            Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (purchaseCount > 0) CountBadge(purchaseCount, isSelected)
            if (renewalCount > 0) {
                // Small, soft renewal mark.
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) scheme.onPrimary.copy(alpha = 0.25f) else scheme.tertiaryContainer.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Sym.Autorenew,
                        contentDescription = "${com.jlees.mibudget.domain.plural(renewalCount, "renewal")}",
                        tint = if (isSelected) scheme.onPrimary else scheme.onTertiaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }
    }
}

/** Number of purchases on a day, capped at "99+". */
@Composable
private fun CountBadge(count: Int, onSelected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val label = "${com.jlees.mibudget.domain.plural(count, "purchase")}"
    Box(
        Modifier
            .height(14.dp)
            .widthIn(min = 14.dp)
            .clip(CircleShape)
            .background(if (onSelected) scheme.onPrimary.copy(alpha = 0.25f) else scheme.secondaryContainer)
            .padding(horizontal = 3.dp)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Medium,
            color = if (onSelected) scheme.onPrimary else scheme.onSecondaryContainer,
            maxLines = 1,
            softWrap = false,
        )
    }
}

