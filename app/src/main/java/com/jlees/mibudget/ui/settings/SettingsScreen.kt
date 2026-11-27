// Settings: a main page (the sections under headings, with the Pill searching every setting) and one page per section
// (← its symbol and title, rows in segmented blocks). Each section is built in its own file (LookAndFeelSettings.kt…);
// this file is the screen: the list, search, paging, and the confirmations rows ask for.
package com.jlees.mibudget.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jlees.mibudget.BuildConfig
import com.jlees.mibudget.MiApp
import com.jlees.mibudget.data.PanArea
import com.jlees.mibudget.scan.ScanEngine
import com.jlees.mibudget.ui.AppViewModels
import com.jlees.mibudget.ui.common.ConfirmDialog
import com.jlees.mibudget.ui.common.MiScaffold
import com.jlees.mibudget.ui.common.Notices
import com.jlees.mibudget.ui.common.PanIn
import com.jlees.mibudget.ui.common.PillSpec
import com.jlees.mibudget.ui.common.Sym
import com.jlees.mibudget.ui.components.rememberNotificationPermission

@Composable
fun SettingsScreen(
    onImport: () -> Unit,
    onPaymentMethods: () -> Unit,
    onRecentlyDeleted: () -> Unit = {},
    onReplayWalkthrough: () -> Unit = {},
    vm: SettingsViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val counts by vm.counts.collectAsStateWithLifecycle()
    val safety by vm.safetyBackup.collectAsStateWithLifecycle()
    val engines by vm.engines.collectAsStateWithLifecycle()
    val nanoMessage by vm.nanoMessage.collectAsStateWithLifecycle()
    val nanoSupported by vm.nanoSupported.collectAsStateWithLifecycle()
    val fx by vm.fxInfo.collectAsStateWithLifecycle()
    val fxBusy by vm.fxBusy.collectAsStateWithLifecycle()
    val trashCount by vm.trashCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val updates = (context.applicationContext as MiApp).container.updates
    val permission = rememberNotificationPermission { ok -> if (ok) vm.checkRemindersNow() }
    LaunchedEffect(Unit) { vm.messages.collect { (text, done) -> Notices.show(text, done = done) } }

    // confirmations (each for something Undo can't bring back)
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmWipe by rememberSaveable { mutableStateOf(false) }
    var confirmEmptyTrash by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteFx by rememberSaveable { mutableStateOf(false) }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteSafety by rememberSaveable { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> if (uri != null) vm.export(uri) }

    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    // a search result opened: its row, scrolled to and briefly lit up (heading, label)
    var found by remember { mutableStateOf<Pair<String?, String>?>(null) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(openId) {
        // scanner rows (Gemini Nano's setup) refresh only while their page shows; the currency info loads with Tools
        if (openId == "tools") vm.loadFxInfo()
        if (openId == "scanner") lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) { vm.watchEngines() } else vm.refreshEnginesNow()
    }
    val closePage: () -> Unit = { openId = null }
    BackHandler(enabled = openId != null) { closePage() }
    BackHandler(enabled = openId == null && query.isNotEmpty()) { query = "" }

    val ctx = SettingsCtx(
        s, vm, permission, engines, nanoMessage, nanoSupported, fx, fxBusy, counts, safety, trashCount,
        openPaymentMethods = onPaymentMethods, openImport = onImport, openRecentlyDeleted = onRecentlyDeleted,
        replayWalkthrough = onReplayWalkthrough, exportBackup = { exporter.launch(vm.exportFileName()) },
        openUrl = { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
        askResetSettings = { confirmReset = true }, askWipe = { confirmWipe = true }, askEmptyTrash = { confirmEmptyTrash = true },
        askDeleteFx = { confirmDeleteFx = true },
        askRestoreSafety = { confirmRestore = true }, askDeleteSafety = { confirmDeleteSafety = true },
    )
    // every section, each in its own file, and the main page's headings over them: mi. Budget's own first, then the
    // same places every mi. app has (Appearance, Navigation, Saved, Protection, General)
    val sections = listOf(
        budgetingSection(ctx), notificationsSection(ctx), scannerSection(ctx), toolsSection(ctx),
        lookAndFeelSection(ctx), pillSection(ctx), startScreenSection(ctx), gesturesSection(ctx),
        recentlyDeletedSection(ctx), privacySection(ctx), backupSection(ctx), aboutSection(ctx, updates::checkNow),
    )
    val byId = sections.associateBy { it.id }
    val headings = listOf(
        "Budget" to listOf("budgeting", "notifications", "scanner", "tools"),
        "Appearance" to listOf("look", "pill", "home"),
        "Protection" to listOf("privacy"),
        "Navigation" to listOf("gestures"),
        "Saved" to listOf("deleted"),
        "General" to listOf("data", "about"),
    ).map { (title, ids) -> title to ids.mapNotNull { byId[it] } }
    val results = searchSettings(sections, query)
    val focusManager = LocalFocusManager.current
    val mainList = rememberLazyListState()
    LaunchedEffect(query) { mainList.scrollToItem(0) }

    PanIn(PanArea.SETTINGS) {
        MiScaffold(
            title = "Settings",
            // searching shows results from the top level, so typing leaves any section that's open
            pill = PillSpec("Search", onTap = {}, query = query, onQuery = { query = it; if (it.isNotEmpty()) openId = null }),
        ) { padding ->
            val bottom = padding.calculateBottomPadding()
            AnimatedContent(
                openId, Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()), label = "settings page",
                transitionSpec = {
                    val forward = targetState != null
                    (slideInHorizontally(tween(220)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(160))) togetherWith
                        (slideOutHorizontally(tween(220)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(120)))
                },
            ) { shown ->
                val section = shown?.let { byId[it] }
                if (section == null) LazyColumn(Modifier.fillMaxSize(), state = mainList, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottom)) {
                    if (query.isBlank()) {
                        headings.forEach { (title, list) ->
                            item(key = "g:$title") { SectionHeader(null, title) }
                            itemsIndexed(list, key = { _, g -> "s:${g.id}" }) { i, g ->
                                Box(Modifier.segment(i, list.size)) { LinkItem(g.icon, g.title, g.summary) { openId = g.id } }
                            }
                        }
                    } else {
                        if (results.isEmpty()) item(key = "empty") {
                            Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Sym.Search, null, Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("No settings match \"${query.trim()}\"", Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        // results grouped by section; each opens the setting on its own page, among its neighbours
                        results.forEach { (g, hits) ->
                            item(key = "h:${g.id}") { SectionHeader(g.icon, g.title) }
                            itemsIndexed(hits, key = { _, h -> "${g.id}:${h.heading}:${h.row.label}" }) { i, h ->
                                Box(Modifier.animateItem().segment(i, hits.size)) {
                                    LinkItem(g.icon, h.row.label, h.heading) { focusManager.clearFocus(); found = h.heading to h.row.label; openId = g.id }
                                }
                            }
                        }
                    }
                    item(key = "footer") {
                        Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                            Text("mi. Budget v${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                } else TopBarOver(bar = { SectionBar(section.icon, section.title, closePage) }) { top ->
                    val placed = placeRows(section.rows)
                    val pageList = rememberLazyListState()
                    // opened from a search result: scroll to the setting and light it up for a moment
                    val flash = remember { Animatable(0f) }
                    val target = found?.let { (heading, label) -> placed.indexOfFirst { !it.row.header && it.heading == heading && it.row.label == label } }?.takeIf { it >= 0 }
                    LaunchedEffect(section.id, target) {
                        if (target == null) return@LaunchedEffect
                        pageList.animateScrollToItem(target)
                        flash.animateTo(1f, tween(250)); flash.animateTo(0f, tween(900, delayMillis = 400))
                        found = null
                    }
                    val primary = MaterialTheme.colorScheme.primary
                    LazyColumn(Modifier.fillMaxSize(), state = pageList, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top, bottom = bottom)) {
                        itemsIndexed(placed, key = { _, p -> "${section.id}:${p.heading}:${p.row.label}" }) { i, p ->
                            if (p.row.header) SectionHeader(null, p.row.label)
                            else Box(Modifier.segment(p.index, p.count).then(
                                if (i == target) Modifier.drawWithContent { drawContent(); drawRect(primary.copy(alpha = 0.18f * flash.value)) } else Modifier,
                            )) { p.row.content() }
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) ConfirmDialog(
        "Are You Sure?", "All settings go back to their defaults. Your purchases, subscriptions and budgets are kept.",
        "Reset", onConfirm = vm::resetSettings, onDismiss = { confirmReset = false }, destructive = true,
    )
    if (confirmWipe) ConfirmDialog(
        "Are You Sure?",
        "This deletes ${counts.purchases} purchases, ${counts.subscriptions} subscriptions, ${counts.categories} categories and budgets, every receipt picture and your settings. A copy stays on this phone for 7 days.",
        "Wipe App", onConfirm = vm::eraseAll, onDismiss = { confirmWipe = false }, destructive = true,
    )
    if (confirmEmptyTrash) ConfirmDialog(
        "Empty Recently Deleted?", "Everything in Recently Deleted is deleted for good, with its receipt pictures. This can't be undone.",
        "Empty", onConfirm = vm::emptyTrash, onDismiss = { confirmEmptyTrash = false }, destructive = true,
    )
    if (confirmDeleteFx) ConfirmDialog(
        "Delete the Update Pack?", "The converter goes back to the built-in rates, and daily updates stop.", "Delete",
        onConfirm = vm::deleteFxPack, onDismiss = { confirmDeleteFx = false }, destructive = true,
    )
    if (confirmDeleteSafety) ConfirmDialog(
        "Delete the Safety Copy?", "The data you wiped can't be brought back after this.", "Delete",
        onConfirm = vm::deleteSafetyBackup, onDismiss = { confirmDeleteSafety = false }, destructive = true,
    )
    if (confirmRestore) ConfirmDialog(
        "Restore Wiped Data?", "Brings back everything from the safety copy. Anything you've added since stays, and nothing is duplicated.",
        "Restore", onConfirm = vm::restoreSafetyBackup, onDismiss = { confirmRestore = false },
    )
}
