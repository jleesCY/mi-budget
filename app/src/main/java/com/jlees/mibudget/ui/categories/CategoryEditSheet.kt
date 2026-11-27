package com.jlees.mibudget.ui.categories

import com.jlees.mibudget.ui.common.MiBottomPanel

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.data.db.CategoryEntity
import com.jlees.mibudget.domain.BudgetPeriod
import com.jlees.mibudget.domain.CategoryTree
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.ui.components.AmountField
import com.jlees.mibudget.ui.components.CategoryBadge
import com.jlees.mibudget.ui.components.CategoryPickerSheet
import com.jlees.mibudget.ui.components.ColorPicker
import com.jlees.mibudget.ui.common.ConfirmDialog
import com.jlees.mibudget.ui.components.IconPicker
import com.jlees.mibudget.ui.components.UncategorizedBadge

/** Create or edit a category folder, including its optional budget. */
@Composable
fun CategoryEditSheet(
    initial: CategoryEntity,
    isNew: Boolean,
    tree: CategoryTree,
    onSave: (CategoryEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    budgetOnly: Boolean = false,
) {
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var parentId by rememberSaveable { mutableStateOf(initial.parentId) }
    var icon by rememberSaveable { mutableStateOf(initial.icon) }
    var color by rememberSaveable { mutableStateOf(initial.color) }
    var hasBudget by rememberSaveable { mutableStateOf(initial.budgetCents != null || budgetOnly) }
    var budgetText by rememberSaveable { mutableStateOf(initial.budgetCents?.let(Money::toInput) ?: "") }
    var period by rememberSaveable { mutableStateOf(initial.budgetPeriod) }
    var pickParent by rememberSaveable { mutableStateOf(false) }

    val preview = initial.copy(name = name, icon = icon, color = color)
    val budgetCents = Money.parse(budgetText)
    val valid = name.isNotBlank() && (!hasBudget || (budgetCents ?: 0) > 0)

    MiBottomPanel(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(preview, size = 52.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        budgetOnly -> "Budget for ${initial.name}"
                        isNew -> "New category"
                        else -> "Edit category"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                if (onDelete != null && !budgetOnly) IconButton(onClick = onDelete) { Icon(Sym.Delete, "Delete") }
            }

            if (!budgetOnly) {
                com.jlees.mibudget.ui.common.LabeledField(label = "Name", value = name, onChange = { name = it }, placeholder = "", modifier = Modifier.fillMaxWidth(), keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words), singleLine = true)
                val parent = parentId?.let { tree.byId[it] }
                ListItem(
                    overlineContent = { Text("Parent category") },
                    headlineContent = { Text(parent?.let { tree.pathLabel(it.id) } ?: "None") },
                    leadingContent = { if (parent != null) CategoryBadge(parent, size = 36.dp) else UncategorizedBadge(size = 36.dp) },
                    modifier = Modifier.clickable { pickParent = true },
                )
            }

            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Budget", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Spending in this category and all its sub-categories counts toward it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = hasBudget, onCheckedChange = { hasBudget = it })
            }
            if (hasBudget) {
                AmountField(budgetText, { budgetText = it }, Modifier.fillMaxWidth(), label = "Budget amount")
                // Chips (not a segmented row) so "Quarterly" never gets squeezed on narrow screens.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetPeriod.entries.forEach { p ->
                        FilterChip(selected = period == p, onClick = { period = p }, label = { Text(p.label) })
                    }
                }
            }

            if (!budgetOnly) {
                HorizontalDivider()
                Text("Color", style = MaterialTheme.typography.titleMedium)
                ColorPicker(color, { color = it })
                Text("Icon", style = MaterialTheme.typography.titleMedium)
                IconPicker(icon, color, { icon = it })
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    enabled = valid,
                    onClick = {
                        onSave(
                            initial.copy(
                                name = name.trim(), parentId = parentId, icon = icon, color = color,
                                budgetCents = if (hasBudget) budgetCents else null, budgetPeriod = period,
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (pickParent) {
        CategoryPickerSheet(
            tree = tree,
            selectedId = parentId,
            title = "Parent Category",
            noneLabel = "None",
            noneSupporting = "Shown on its own, not inside another category",
            excludeSubtreeOf = if (isNew) null else initial.id,
            onDismiss = { pickParent = false },
            onSelect = { parentId = it; pickParent = false },
        )
    }
}
