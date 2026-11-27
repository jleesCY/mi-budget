// Recently Deleted: purchases and subscriptions you deleted, kept for 30 days so nothing is lost by accident.
package com.jlees.mibudget.ui.trash

import kotlinx.coroutines.flow.flowOn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import com.jlees.mibudget.ui.common.MiDialog

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.AppContainer
import com.jlees.mibudget.data.db.cycle
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.common.EmptyLine
import com.jlees.mibudget.ui.common.MiScaffold
import com.jlees.mibudget.ui.common.Notices
import com.jlees.mibudget.ui.components.ItemIcon
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One deleted thing: a purchase or a subscription. */
data class Trashed(val id: String, val subscription: Boolean, val name: String, val brandKey: String?, val amount: String, val deletedAt: Long)

class RecentlyDeletedViewModel(private val c: AppContainer) : ViewModel() {
    val items: StateFlow<List<Trashed>?> = combine(c.repository.deletedPurchases, c.repository.deletedSubscriptions) { ps, ss ->
        (ps.map { Trashed(it.id, false, it.merchant, it.brandKey, Money.format(it.amountCents), it.deletedAt ?: 0) } +
            ss.map { Trashed(it.id, true, it.name, it.brandKey, Money.format(it.amountCents) + it.cycle.shortSuffix, it.deletedAt ?: 0) })
            .sortedByDescending { it.deletedAt }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun restore(t: Trashed) = viewModelScope.launch {
        if (t.subscription) c.repository.restoreFromTrash(emptyList(), listOf(t.id)) else c.repository.restoreFromTrash(listOf(t.id), emptyList())
        Notices.show("Restored ${t.name}", done = true)
    }

    /** Can't be undone: asked first. */
    fun deleteForever(t: Trashed) = c.appScope.launch {
        if (t.subscription) c.repository.purgeOne(subscriptionId = t.id) else c.repository.purgeOne(purchaseId = t.id)
    }

    fun empty() = c.appScope.launch { c.repository.purgeTrash(before = Long.MAX_VALUE) }
}

@Composable
fun RecentlyDeletedScreen(onBack: () -> Unit, vm: RecentlyDeletedViewModel = viewModel(factory = AppViewModels.Factory)) {
    val items by vm.items.collectAsStateWithLifecycle()
    var confirmEmpty by rememberSaveable { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<Trashed?>(null) }
    var ownQuery by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val q = ownQuery.trim()
    MiScaffold(
        title = "Recently Deleted", onBack = onBack,
        pill = com.jlees.mibudget.ui.common.PillSpec("Search Recently Deleted", onTap = {}, query = ownQuery, onQuery = { ownQuery = it }),
        actions = { if (!items.isNullOrEmpty()) TextButton(onClick = { confirmEmpty = true }) { Text("Empty", color = MaterialTheme.colorScheme.error) } },
    ) { padding ->
        val all = items
        if (all == null) { androidx.compose.foundation.layout.Box(Modifier.padding(top = padding.calculateTopPadding())) { com.jlees.mibudget.ui.common.BoneRows() }; return@MiScaffold }
        val list = all.filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        // a chunk at a time, as the browser's History
        val count = com.jlees.mibudget.ui.common.rememberChunkedCount(listState, list.size, reset = q)
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 8.dp)) {
            if (list.isEmpty()) item { EmptyLine(Sym.RestoreFromTrash, "Nothing Deleted") }
            items(list.take(count), key = { (if (it.subscription) "s" else "p") + it.id }) { t ->
                val daysLeft = com.jlees.mibudget.domain.Trash.daysLeft(t.deletedAt, System.currentTimeMillis(), com.jlees.mibudget.ui.common.LocalSettings.current.keepDeleted.days)
                var menu by remember { mutableStateOf(false) }
                // a mi. list row; tapping it restores it (the quick way back), ⋮ for the rest
                com.jlees.mibudget.ui.common.MiListRow(
                    title = t.name,
                    lead = androidx.compose.ui.text.AnnotatedString("$daysLeft day${if (daysLeft == 1L) "" else "s"} left"),
                    details = androidx.compose.ui.text.AnnotatedString("${if (t.subscription) "Subscription" else "Purchase"} · ${t.amount}"),
                    leading = { ItemIcon(t.brandKey, t.name, null) },
                    modifier = Modifier.animateItem(),
                    onClick = { vm.restore(t) }, onClickLabel = "Restore",
                    trailing = {
                    androidx.compose.foundation.layout.Box {
                        IconButton(onClick = { menu = true }) { Icon(Sym.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        com.jlees.mibudget.ui.common.MiMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Restore") }, leadingIcon = { Icon(Sym.RestoreFromTrash, null) }, onClick = { menu = false; vm.restore(t) })
                            DropdownMenuItem(
                                text = { Text("Delete Forever", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Sym.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; confirmOne = t },
                            )
                        }
                    }
                    },
                )
            }
        }
    }
    if (confirmEmpty) MiDialog(
        onDismissRequest = { confirmEmpty = false },
        title = { Text("Empty Recently Deleted?") },
        text = { Text("Everything here is deleted for good, with its receipt pictures. This can't be undone.") },
        confirmButton = { TextButton(onClick = { confirmEmpty = false; vm.empty() }) { Text("Empty", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancel") } },
    )
    confirmOne?.let { t ->
        MiDialog(
            onDismissRequest = { confirmOne = null },
            title = { Text("Delete ${t.name} Forever?") },
            text = { Text("This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmOne = null; vm.deleteForever(t) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmOne = null }) { Text("Cancel") } },
        )
    }
}

