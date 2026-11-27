package com.jlees.mibudget.ui.settings

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.jlees.mibudget.ui.common.Sym

import com.jlees.mibudget.data.db.spentCents
import androidx.compose.runtime.saveable.rememberSaveable
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.AppContainer
import com.jlees.mibudget.data.db.PaymentMethodEntity
import com.jlees.mibudget.data.db.displayName
import com.jlees.mibudget.domain.BudgetPeriod
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.components.PaymentMethodEditSheet
import com.jlees.mibudget.ui.components.PaymentMethodIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MethodRow(val method: PaymentMethodEntity, val purchaseCount: Int, val monthTotal: Long, val subscriptionCount: Int)

class PaymentMethodsViewModel(private val c: AppContainer) : ViewModel() {
    val rows: StateFlow<List<MethodRow>?> = combine(
        c.repository.paymentMethods, c.repository.purchases, c.repository.subscriptions,
    ) { methods, purchases, subs ->
        val month = BudgetPeriod.MONTHLY.rangeContaining(LocalDate.now())
        val byMethod = purchases.groupBy { it.paymentMethodId }
        methods.map { m ->
            val ps = byMethod[m.id].orEmpty()
            MethodRow(m, ps.size, ps.filter { it.date in month }.sumOf { it.spentCents }, subs.count { it.paymentMethodId == m.id })
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(m: PaymentMethodEntity) = viewModelScope.launch { c.repository.savePaymentMethod(m) }
    fun delete(m: PaymentMethodEntity) = viewModelScope.launch {
        val undo = c.repository.deletePaymentMethod(m.id) ?: return@launch
        com.jlees.mibudget.ui.common.Notices.undo("Deleted ${m.displayName}") { c.appScope.launch { undo() } }
    }
    suspend fun importIcon(uri: Uri): String = c.paymentIcons.importImage(uri)
}

/** Settings → Payment methods: the cards, accounts and wallets you pick from everywhere else. */
@Composable
fun PaymentMethodsScreen(
    onBack: () -> Unit,
    onViewPurchases: (String) -> Unit,
    vm: PaymentMethodsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<Pair<PaymentMethodEntity, Boolean>?>(null) }

    // the Library's one search Pill (or this page's own when it stands alone)
    val hostedQuery = com.jlees.mibudget.ui.common.LocalSheetQuery.current
    var ownQuery by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val q = (hostedQuery ?: ownQuery).trim()
    com.jlees.mibudget.ui.common.MiScaffold(
        title = "Payment Methods",
        onBack = onBack,
        pill = com.jlees.mibudget.ui.common.PillSpec("Search Payment Methods", onTap = {}, query = ownQuery, onQuery = { ownQuery = it }),
        fabs = com.jlees.mibudget.ui.common.SheetFabs(com.jlees.mibudget.ui.common.BarButton(Sym.Add, "Add Payment Method") { editing = PaymentMethodEntity(name = "", sortOrder = rows?.size ?: 0) to true }),
    ) { padding ->
        val list = rows?.filter { q.isEmpty() || it.method.displayName.contains(q, ignoreCase = true) }
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (list == null) item { com.jlees.mibudget.ui.common.BoneRows(4) }
            if (list != null && list.isEmpty()) item {
                com.jlees.mibudget.ui.common.EmptyLine(Sym.CreditCard, "No Payment Methods Yet")
            }
            items(list.orEmpty(), key = { it.method.id }) { r ->
                val m = r.method
                val cs = MaterialTheme.colorScheme
                com.jlees.mibudget.ui.common.MiListRow(
                    title = m.displayName,
                    lead = androidx.compose.ui.text.AnnotatedString("${com.jlees.mibudget.domain.plural(r.purchaseCount, "purchase")}"),
                    details = androidx.compose.ui.text.AnnotatedString(listOfNotNull(
                        if (r.monthTotal != 0L) "${Money.format(r.monthTotal)} this month" else null,
                        if (r.subscriptionCount > 0) "${com.jlees.mibudget.domain.plural(r.subscriptionCount, "subscription")}" else null,
                        if (m.archived) "Hidden" else null,
                    ).joinToString(" · ")),
                    leading = { PaymentMethodIcon(m, size = 44.dp) },
                    modifier = Modifier.animateItem(),
                    onClick = { editing = m to false },
                    dim = m.archived,
                    trailing = { IconButton(onClick = { onViewPurchases(m.id) }) { Icon(Sym.ReceiptLong, "View purchases", tint = cs.onSurfaceVariant) } },
                )
            }
        }
    }

    editing?.let { (m, isNew) ->
        PaymentMethodEditSheet(
            initial = m,
            isNew = isNew,
            importIcon = vm::importIcon,
            onSave = { vm.save(it); editing = null },
            onDelete = if (isNew) null else ({ vm.delete(m); editing = null }),
            onDismiss = { editing = null },
        )
    }
}
