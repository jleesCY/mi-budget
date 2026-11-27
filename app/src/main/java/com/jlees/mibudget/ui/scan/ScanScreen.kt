package com.jlees.mibudget.ui.scan

import com.jlees.mibudget.ui.common.MiDialog

import com.jlees.mibudget.ui.common.Sym

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.jlees.mibudget.ui.components.ScanTextDialog
import com.jlees.mibudget.ui.components.ScanTextSection
import androidx.compose.runtime.remember
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.jlees.mibudget.scan.ScanEngine
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import com.jlees.mibudget.domain.Money
import com.jlees.mibudget.scan.ScanKind
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.components.ItemIcon
import com.jlees.mibudget.ui.components.rememberImageSource
import com.jlees.mibudget.ui.components.MediumDate
import java.io.File

/**
 * Camera / screenshot → on-device OCR → review → hand off to the purchase or subscription editor.
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onContinue: (ScanKind) -> Unit,
    vm: ScanViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.messages.collect { com.jlees.mibudget.ui.common.Notices.show(it) } }
    // Re-check what the rescan buttons can do whenever this screen is in view (Gemini Nano can become ready later).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                vm.refreshRescan()
                delay(5_000)
            }
        }
    }
    var launched by rememberSaveable { mutableStateOf(false) }

    // Android's chooser: take a photo, or pick a screenshot / image from any gallery app.
    val pickImage = rememberImageSource { uri ->
        if (uri != null) vm.process(uri) else if (vm.state.value is ScanState.Waiting) onBack()
    }

    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            when (vm.route.mode) {
                "shared" -> vm.route.sharedUri?.let { vm.process(Uri.parse(it)) }
                "resume" -> if (vm.state.value !is ScanState.Done) onBack() // nothing to resume any more
                else -> pickImage()
            }
        }
    }

    com.jlees.mibudget.ui.common.MiScaffold(
        title = "Scan",
        actions = {
                    if (state is ScanState.Done) TextButton(onClick = { vm.discard(); onBack() }) { Text("Discard") }
                },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (val s = state) {
                ScanState.Waiting -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = pickImage) { Icon(Sym.DocumentScanner, null); Spacer(Modifier.size(8.dp)); Text("Scan") }
                    Text(
                        "Take a photo or pick a screenshot — mi. Budget reads receipts, screens, emails and more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
                ScanState.Working -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ContainedLoadingIndicator(Modifier.size(96.dp))
                    Spacer(Modifier.height(16.dp))
                    val status by vm.status.collectAsStateWithLifecycle()
                    Text(
                        status,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        // Screen readers announce progress ("Reading with Gemini Nano…") as it changes.
                        modifier = Modifier.padding(horizontal = 32.dp)
                            .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = vm::cancelScan) { Text("Cancel") }
                }
                is ScanState.Failed -> Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Sym.ErrorOutline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Text(s.message, textAlign = TextAlign.Center)
                    s.detail?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(16.dp))
                    // Same photo again (if it's still there), or a different one.
                    Button(onClick = { if (!vm.retrySamePhoto()) { vm.reset(); pickImage() } }) { Text("Try Again") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { vm.reset(); pickImage() }) { Text("Choose Another Photo") }
                }
                is ScanState.Done -> Review(
                    s, vm,
                    onContinue = { vm.commit()?.let(onContinue) },
                    onRetake = {
                        vm.reset()
                        pickImage()
                    },
                )
            }
        }
    }
}

@Composable
private fun Review(
    s: ScanState.Done,
    vm: ScanViewModel,
    onContinue: () -> Unit,
    onRetake: () -> Unit,
) {
    val r = s.result
    val rescan by vm.rescan.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = vm.receiptFile(s.receiptFile),
            contentDescription = "Scanned image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(28.dp)),
        )
        // See exactly what was read from the picture.
        var showText by rememberSaveable { mutableStateOf(false) }
        OutlinedButton(onClick = { showText = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Sym.Notes, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("View text")
        }
        if (showText) ScanTextDialog(
            listOfNotNull(
                s.aiReply?.let { ScanTextSection("${s.readWith.title}'s answer", it) },
                ScanTextSection("Google's text reader", r.rawText),
            ),
            onDismiss = { showText = false },
        )
        // Scanning from Purchases always makes a purchase, from Subscriptions a subscription. Only a
        // picture shared into mi. Budget from another app (no tab to go by) asks what it is.
        if (vm.route.target == "auto") Text("What is this?", style = MaterialTheme.typography.titleMedium)
        if (vm.route.target == "auto") SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = s.kind == ScanKind.PURCHASE,
                onClick = { vm.setKind(ScanKind.PURCHASE) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
                icon = { Icon(Sym.ShoppingBag, null, Modifier.size(18.dp)) },
            ) { Text("Purchase") }
            SegmentedButton(
                selected = s.kind == ScanKind.SUBSCRIPTION,
                onClick = { vm.setKind(ScanKind.SUBSCRIPTION) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
                icon = { Icon(Sym.Autorenew, null, Modifier.size(18.dp)) },
            ) { Text("Subscription") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ListItem(
                overlineContent = { Text("Merchant") },
                headlineContent = { Text(r.merchant ?: "Not found") },
                trailingContent = { if (r.brand != null) ItemIcon(r.brand.id, r.merchant ?: "", null, size = 36.dp) },
                colors = itemColors,
            )
            ListItem(
                overlineContent = { Text(if (r.isRefund) "Refund" else "Amount") },
                headlineContent = {
                    Text(
                        r.amountCents?.let { (if (r.isRefund) "+" else "") + Money.format(it) } ?: "Not found",
                        color = if (r.isRefund) com.jlees.mibudget.ui.theme.AppColors.refund else androidx.compose.ui.graphics.Color.Unspecified,
                    )
                },
                supportingContent = if (r.isRefund) { { Text("Money back — it'll be saved as a refund") } } else null,
                colors = itemColors,
            )
            if (r.amountCandidates.size > 1) Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                r.amountCandidates.forEach { c ->
                    FilterChip(selected = c == r.amountCents, onClick = { vm.pickAmount(c) }, label = { Text(Money.format(c)) })
                }
            }
            ListItem(overlineContent = { Text("Date") }, headlineContent = { Text(r.date?.format(MediumDate) ?: "Not found (today)") }, colors = itemColors)
            if (s.kind == ScanKind.SUBSCRIPTION) {
                ListItem(overlineContent = { Text("Billing") }, headlineContent = { Text(r.cycle?.label ?: "Monthly (assumed)") }, colors = itemColors)
                r.nextBillingDate?.let { ListItem(overlineContent = { Text("Next payment") }, headlineContent = { Text(it.format(MediumDate)) }, colors = itemColors) }
                r.trialEndDate?.let { ListItem(overlineContent = { Text("Trial ends") }, headlineContent = { Text(it.format(MediumDate)) }, colors = itemColors) }
            }
        }
        Text(
            "You can fix anything on the next screen.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Rescans: Rescan reads it again with Google's text reader; Advanced Rescan with Gemini Nano, where the phone has it
        run {
            // Stacked full-width so both labels always fit, even with large system fonts.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { vm.rescan() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Sym.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Rescan")
                }
                // Advanced Rescan: Gemini Nano, on phones that have it
                if (rescan.nanoReady) FilledTonalButton(onClick = { vm.advancedRescan() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Sym.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.size(6.dp)); Text("Advanced Rescan")
                }
            }
            Text(
                buildString {
                    append("Rescan reads the photo again")
                    if (rescan.nanoReady) append(" · Advanced Rescan uses Gemini Nano")
                    if (s.readWith != ScanEngine.STANDARD) append("\nShowing ${s.readWith.title}'s reading.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) { Text("Retake") }
            Button(onClick = onContinue, modifier = Modifier.weight(1f)) { Text("Continue") }
        }
    }
}
