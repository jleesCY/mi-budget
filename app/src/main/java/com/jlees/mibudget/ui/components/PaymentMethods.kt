package com.jlees.mibudget.ui.components

import androidx.compose.ui.unit.sp
import com.jlees.mibudget.ui.common.ConfirmDialog

import com.jlees.mibudget.ui.common.MiBottomPanel

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.runtime.saveable.rememberSaveable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jlees.mibudget.data.db.PaymentMethodEntity
import com.jlees.mibudget.data.db.displayName
import com.jlees.mibudget.icons.IconRef
import kotlinx.coroutines.launch
import java.io.File

/** Generic payment icons for things that aren't a brand (cash, a gift card, …). Keys are stored — only add. */
object PaymentGenericIcons {
    val all: List<Triple<String, String, ImageVector>> = listOf(
        Triple("card", "Credit card", Sym.CreditCard),
        Triple("debit", "Debit card", Sym.Payment),
        Triple("cash", "Cash", Sym.Payments),
        Triple("bank", "Bank account", Sym.AccountBalance),
        Triple("wallet", "Wallet", Sym.AccountBalanceWallet),
        Triple("phone", "Phone pay", Sym.PhoneIphone),
        Triple("gift", "Gift card", Sym.CardGiftcard),
        Triple("crypto", "Crypto", Sym.CurrencyBitcoin),
    )

    fun get(key: String): ImageVector = all.firstOrNull { it.first == key }?.third ?: Sym.CreditCard
}

/** Renders a payment method's icon: brand logo, generic symbol, or the user's own picture. */
@Composable
fun PaymentMethodIcon(method: PaymentMethodEntity?, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    PaymentIcon(method?.icon ?: "none", size, modifier, contentDescription = method?.name)
}

@Composable
fun PaymentIcon(icon: String, size: Dp = 40.dp, modifier: Modifier = Modifier, contentDescription: String? = null) {
    when (val ref = IconRef.parse(icon)) {
        // Payment methods never auto-detect from a merchant name; "none" shows a neutral card.
        IconRef.Auto, IconRef.None -> SymbolBadge(Sym.CreditCard, modifier, size, contentDescription)
        is IconRef.BrandRef -> {
            val catalog = LocalBrandCatalog.current
            if (catalog.byId(ref.id)?.takeIf { catalog.hasIcon(it) } == null) SymbolBadge(Sym.CreditCard, modifier, size, contentDescription)
            else ItemIcon(icon, "", null, modifier, size, contentDescription)
        }
        else -> ItemIcon(icon, "", null, modifier, size, contentDescription)
    }
}

/**
 * Field used in the purchase & subscription editors. Picks from your saved payment methods so
 * names are always consistent, and can add a new one on the spot.
 */
@Composable
fun PaymentMethodField(
    methods: List<PaymentMethodEntity>,
    selectedId: String?,
    legacyName: String,
    onSelect: (String?) -> Unit,
    onCreate: suspend (PaymentMethodEntity) -> Unit,
    importIcon: suspend (Uri) -> String,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val selected = methods.firstOrNull { it.id == selectedId }
    com.jlees.mibudget.ui.common.PickerField(
        "Payment Method", selected?.displayName ?: legacyName.ifBlank { "None" }, onClick = { open = true }, modifier = modifier,
        leading = { PaymentMethodIcon(selected, size = if (com.jlees.mibudget.ui.common.LocalInGroup.current) 40.dp else 32.dp) },
        detail = if (selected == null && legacyName.isNotBlank()) "Not a saved method; tap to pick one" else null,
    )
    if (open) {
        MiBottomPanel(onDismissRequest = { open = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            com.jlees.mibudget.ui.common.PanelTitle("Paid With")
            LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item {
                    com.jlees.mibudget.ui.common.ChoiceRow("None", selectedId == null, { onSelect(null); open = false }) { PaymentIcon("none", 36.dp) }
                }
                items(methods.filter { !it.archived || it.id == selectedId }, key = { it.id }) { m ->
                    com.jlees.mibudget.ui.common.ChoiceRow(m.displayName, selectedId == m.id, { onSelect(m.id); open = false }) { PaymentMethodIcon(m, 36.dp) }
                }
                item {
                    com.jlees.mibudget.ui.common.ChoiceRow("Add Payment Method", false, { creating = true }) { IconCircle(Sym.Add) }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
    if (creating) {
        val scope = rememberCoroutineScope()
        PaymentMethodEditSheet(
            initial = PaymentMethodEntity(name = "", sortOrder = methods.size),
            isNew = true,
            importIcon = importIcon,
            onSave = { m ->
                scope.launch {
                    onCreate(m)
                    onSelect(m.id)
                    creating = false
                    open = false
                }
            },
            onDelete = null,
            onDismiss = { creating = false },
        )
    }
}

/** Create / edit a payment method: name, optional last 4 digits, and an icon. */
@Composable
fun PaymentMethodEditSheet(
    initial: PaymentMethodEntity,
    isNew: Boolean,
    importIcon: suspend (Uri) -> String,
    onSave: (PaymentMethodEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val catalog = LocalBrandCatalog.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var last4 by rememberSaveable { mutableStateOf(initial.last4 ?: "") }
    var icon by rememberSaveable { mutableStateOf(initial.icon) }
    // Until the user picks an icon themselves, follow the name ("Chase Sapphire" → Chase logo).
    var iconTouched by rememberSaveable { mutableStateOf(!isNew) }
    var archived by rememberSaveable { mutableStateOf(initial.archived) }
    var pickIcon by rememberSaveable { mutableStateOf(false) }

    fun onNameChange(v: String) {
        name = v
        if (!iconTouched) icon = catalog.matchPayment(v)?.takeIf { catalog.hasIcon(it) }?.let { "brand:${it.id}" } ?: "generic:card"
    }

    MiBottomPanel(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(com.jlees.mibudget.ui.common.FormGap),
        ) {
            // the panel's title (24sp, as a dialog's), with delete at its end for one that exists
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isNew) "New Payment Method" else "Edit Payment Method", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, modifier = Modifier.weight(1f))
                if (onDelete != null) IconButton(onClick = onDelete) { Icon(Sym.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
            }
            // its picture beside its name, as the purchase editor's: tap it to choose another
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.jlees.mibudget.ui.common.IconTile(onClick = { pickIcon = true }) { PaymentIcon(icon, 56.dp) }
                com.jlees.mibudget.ui.common.LabeledField(label = "Name", value = name, onChange = ::onNameChange, placeholder = "e.g. Chase Sapphire", modifier = Modifier.weight(1f), keyboard = KeyboardOptions(capitalization = KeyboardCapitalization.Words), singleLine = true)
                com.jlees.mibudget.ui.common.LabeledField(label = "Last 4", value = last4, onChange = { v -> last4 = v.filter(Char::isDigit).take(4) }, placeholder = "", modifier = Modifier.width(96.dp), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }

            if (!isNew) com.jlees.mibudget.ui.common.SwitchField(
                "Hide From Pickers", archived, { archived = it },
                subtitle = "For a closed card; past purchases keep showing it",
            )

            // the panel's button bar: Cancel outlined, Save filled, equal width, 48 tall
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text("Cancel", fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSave(initial.copy(name = name.trim(), last4 = last4.ifBlank { null }, icon = icon, archived = archived))
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) { Text("Save", fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (pickIcon) {
        ItemIconSheet(
            mode = IconSheetMode.PAYMENT,
            name = name,
            current = icon,
            category = null,
            importImage = importIcon,
            onSelect = { ref, _ -> icon = ref ?: "none"; iconTouched = true; pickIcon = false },
            onDismiss = { pickIcon = false },
        )
    }
}
