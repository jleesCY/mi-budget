// The "This Month" home-screen widget: spent so far, what's left of your monthly budgets, and the next renewal. Its
// numbers come from the same rules as the start screen; it refreshes when purchases change (while the app runs) and
// every few hours.
package com.jlees.mibudget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jlees.mibudget.MainActivity
import com.jlees.mibudget.MiApp
import com.jlees.mibudget.R
import com.jlees.mibudget.data.db.spentCents
import com.jlees.mibudget.domain.BudgetPeriod
import com.jlees.mibudget.domain.Budgets
import com.jlees.mibudget.domain.CategoryTree
import com.jlees.mibudget.domain.DateRange
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.domain.Renewals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MonthWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { update(context) } finally { pending.finish() }
        }
    }

    companion object {
        /** Redraws every This Month widget (nothing happens when there are none). */
        suspend fun update(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MonthWidget::class.java))
            if (ids.isEmpty()) return
            val app = context.applicationContext as MiApp
            val db = app.container.repository.database
            val settings = app.container.settings.current()
            val today = LocalDate.now()
            val month = BudgetPeriod.MONTHLY.rangeContaining(today)
            val purchases = db.purchaseDao().getAll()
            val tree = CategoryTree(db.categoryDao().getAll())
            val inMonth = purchases.filter { it.date in month }
            val spent = inMonth.sumOf { it.spentCents }
            // the overall monthly budget, as on the start screen: top-level budgets only (a child's is part of its parent's)
            val top = Budgets.statuses(tree, purchases, today, settings.firstDayOfWeek)
                .filter { s -> tree.path(s.category.id).dropLast(1).none { it.budgetCents != null } }
            val budget = top.sumOf { Math.round(it.budgetCents * it.category.budgetPeriod.perMonth) }
            val ids2 = top.flatMap { tree.subtreeIds(it.category.id) }.toSet()
            val budgeted = purchases.filter { it.date in month && it.categoryId in ids2 }.sumOf { it.spentCents }
            val next = Renewals.byDate(db.subscriptionDao().getAll(), DateRange(today, today.plusDays(31)), today)
                .entries.sortedBy { it.key }.firstOrNull()?.let { (d, subs) -> d to subs.first() }

            val views = RemoteViews(context.packageName, R.layout.widget_month).apply {
                setTextViewText(R.id.widget_spent, Money.format(spent))
                setTextViewText(R.id.widget_budget, when {
                    inMonth.isEmpty() -> "No Purchases Yet"
                    budget <= 0 -> "Spent this month"
                    budgeted <= budget -> "${Money.format(budget - budgeted)} left of ${Money.format(budget)}"
                    else -> "${Money.format(budgeted - budget)} over your budgets"
                })
                setTextViewText(R.id.widget_next, next?.let { (d, s) ->
                    "Next: ${s.name} · ${if (d == today) "Today" else d.format(DateTimeFormatter.ofPattern("MMM d"))} · ${Money.format(s.amountCents)}"
                } ?: "No renewals this month")
                // anywhere opens the start screen; the heading opens the Library
                setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ))
                setOnClickPendingIntent(R.id.widget_heading, PendingIntent.getActivity(
                    context, 1, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra(MainActivity.EXTRA_OPEN_LIBRARY, true),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ))
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
