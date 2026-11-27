package com.jlees.mibudget.ui.navigation

import androidx.compose.foundation.background
import kotlinx.coroutines.flow.first
import com.jlees.mibudget.ui.common.Sym

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.jlees.mibudget.ui.common.ActionPillHost
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jlees.mibudget.MiApp
import com.jlees.mibudget.scan.ScanKind
import com.jlees.mibudget.ui.calendar.CalendarScreen
import com.jlees.mibudget.ui.categories.CategoriesScreen
import com.jlees.mibudget.ui.common.ConnectedIconChoice
import com.jlees.mibudget.ui.common.LocalSheetChips
import com.jlees.mibudget.ui.common.LocalSheetClose
import com.jlees.mibudget.ui.common.Motion
import com.jlees.mibudget.ui.common.NoticeHost
import com.jlees.mibudget.ui.common.MiPill
import com.jlees.mibudget.ui.components.rememberImageSource
import com.jlees.mibudget.ui.onboarding.Walkthrough
import com.jlees.mibudget.ui.purchases.PurchaseEditScreen
import com.jlees.mibudget.ui.purchases.PurchasesScreen
import com.jlees.mibudget.ui.scan.ScanScreen
import com.jlees.mibudget.ui.settings.ImportScreen
import com.jlees.mibudget.ui.settings.PaymentMethodsScreen
import com.jlees.mibudget.ui.settings.SettingsScreen
import com.jlees.mibudget.ui.subscriptions.SubscriptionEditScreen
import com.jlees.mibudget.ui.subscriptions.SubscriptionsScreen
import com.jlees.mibudget.ui.tools.CheckSplitScreen
import com.jlees.mibudget.ui.tools.CurrencyScreen
import com.jlees.mibudget.ui.tools.TipCalculatorScreen
import com.jlees.mibudget.ui.tools.ToolsScreen
import kotlinx.coroutines.launch

/** Places the start screen's shortcuts lead to: the Library's pages (in the order of its chips), and the Calendar. */
enum class LibraryPage(val label: String, val icon: ImageVector) {
    SUBSCRIPTIONS("Subscriptions", Sym.Autorenew),
    BUDGETS("Budgets", Sym.Savings),
    TOOLS("Tools", Sym.Handyman),
    /** Not a Library page: the main bar's Calendar (a swipe up). */
    CALENDAR("Calendar", Sym.CalendarMonth);

    companion object { val chips = listOf(SUBSCRIPTIONS, BUDGETS, TOOLS) }
}

/**
 * The Library (the main bar's left edge button, a swipe right): Subscriptions, Budgets and Tools as icon chips, with one
 * search Pill for all of them, morphing out of the main bar's Pill.
 */
@Composable
private fun LibrarySheet(sheets: com.jlees.mibudget.ui.common.BarSheets, content: @Composable (LibraryPage) -> Unit) {
    val page = LibraryPage.chips.getOrElse(sheets.libraryPage) { LibraryPage.SUBSCRIPTIONS }
    var query by rememberSaveable(page) { mutableStateOf("") }
    // how chosen each page's button is, kept here (each page draws its own header with the buttons, inside the
    // crossfade between pages), so the buttons widen and shrink across the switch instead of reappearing in place
    val chosen = remember { LibraryPage.chips.map { p -> androidx.compose.animation.core.Animatable(if (p == page) 1f else 0f) } }
    LaunchedEffect(page) {
        LibraryPage.chips.forEachIndexed { i, p ->
            // one growing and one shrinking at the same rate, so the row's width stays put (no bounce), as the browser's
            launch { chosen[i].animateTo(if (p == page) 1f else 0f, Motion.enter(220)) }
        }
    }
    val chips: @Composable () -> Unit = {
        ConnectedIconChoice(LibraryPage.chips.map { Triple(it, it.label, it.icon) }, page, chosen = { chosen[it].value }) { sheets.libraryPage = LibraryPage.chips.indexOf(it) }
    }
    SideEffect { sheets.sheetPillText = query.ifEmpty { "Search ${page.label}" } to query.isNotEmpty() }
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalSheetChips provides chips, com.jlees.mibudget.ui.common.LocalSheetQuery provides query) {
            // The first open after a launch builds only the sheet's header while it slides in, and the page itself (every
            // subscription, the coming-up cards, the totals) once it's all the way open, fading in: building it all
            // mid-slide, with nothing compiled or cached yet, stalled the slide for about a second. Later opens build it
            // straight away, as it's quick by then.
            var ready by remember { mutableStateOf(sheets.libraryBuilt) }
            if (!ready) LaunchedEffect(Unit) {
                androidx.compose.runtime.snapshotFlow { sheets.slide.value == 0f && !sheets.slide.isRunning }.first { it }
                ready = true
                sheets.libraryBuilt = true
            }
            val shown = remember { androidx.compose.animation.core.MutableTransitionState(ready) }
            shown.targetState = ready
            // the header (the same one the page draws) and the page's skeleton stand in until the page has faded in over them
            if (!shown.currentState || !shown.isIdle) androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                com.jlees.mibudget.ui.common.SheetHeader(page.label, null, com.jlees.mibudget.ui.common.LocalSheetClose.current)
                if (page == LibraryPage.SUBSCRIPTIONS) com.jlees.mibudget.ui.common.SubscriptionsSkeleton() else com.jlees.mibudget.ui.common.BoneRows()
            }
            androidx.compose.animation.AnimatedVisibility(shown, enter = fadeIn(Motion.enter(180)), exit = androidx.compose.animation.ExitTransition.None) {
                AnimatedContent(page, transitionSpec = { fadeIn(Motion.enter(180)) togetherWith fadeOut(Motion.exit(90)) }, label = "library page") { p -> content(p) }
            }
        }
        MiPill(
            com.jlees.mibudget.ui.common.PillSpec("Search ${page.label}", onTap = {}, query = query, onQuery = { query = it }),
            morph = com.jlees.mibudget.ui.common.LocalSheetMorph.current,
        )
    }
}

/**
 * mi. Budget's one screen and its sheets. The start screen is the month's purchases with the Pill; swiping the Pill up
 * opens the Library (rising from the bottom), Settings slides in from the side its button is on, and everything opened
 * from a sheet slides in from the right. Closing is always quicker than opening.
 */
@Composable
fun AppNav(
    sharedImage: String?,
    onSharedImageConsumed: () -> Unit,
    openSubscriptionId: String?,
    onOpenSubscriptionConsumed: () -> Unit,
    openLibrary: Boolean = false,
    onOpenLibraryConsumed: () -> Unit = {},
    nav: NavHostController = rememberNavController(),
) {
    val container = (LocalContext.current.applicationContext as MiApp).container

    /** Opens a screen only if the current one is settled (a fast double tap would open it twice). */
    fun go(route: Any) {
        if (nav.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) != false) nav.navigate(route)
    }
    // the main bar's pages, kept above the start screen so one stays open while something is opened from it
    val sheetScope = androidx.compose.runtime.rememberCoroutineScope()
    val sheets = com.jlees.mibudget.ui.common.rememberBarSheets(sheetScope)
    // one item's page (a purchase's or subscription's summary and editor) has a split Pill grown out of the Pill of the
    // page that opened it; it turns back into that Pill as soon as navigation goes anywhere else (Back, a back swipe)
    val actionPills = remember { com.jlees.mibudget.ui.common.ActionPills(sheetScope) }
    sheets.coveredBy = { actionPills.covers(com.jlees.mibudget.ui.common.PillOrigin.SHEET) }
    androidx.compose.runtime.DisposableEffect(nav) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, dest, _ ->
            if (!dest.hasRoute(PurchaseEditRoute::class) && !dest.hasRoute(SubscriptionEditRoute::class)) actionPills.leave()
        }
        nav.addOnDestinationChangedListener(listener)
        onDispose { nav.removeOnDestinationChangedListener(listener) }
    }
    /** Opens [route] with its split Pill growing out of the main bar's Pill. */
    fun fromBar(route: Any) { actionPills.openFrom(sheets.barPill, com.jlees.mibudget.ui.common.PillOrigin.BAR); go(route) }
    /** Opens [route] with its split Pill growing out of the open sheet's search Pill. */
    fun fromSheet(route: Any) { actionPills.openFrom(sheets.sheetPill, com.jlees.mibudget.ui.common.PillOrigin.SHEET, sheets.sheetPillText); go(route) }
    /** Opens [route] from somewhere without a Pill to grow from (its Pill is simply there). */
    fun plain(route: Any) { actionPills.openFrom(null, com.jlees.mibudget.ui.common.PillOrigin.NONE); go(route) }
    fun openLibrary(p: LibraryPage) {
        if (p == LibraryPage.CALENDAR) sheets.open(com.jlees.mibudget.ui.common.BarPage.UP)
        else { sheets.libraryPage = LibraryPage.chips.indexOf(p).coerceAtLeast(0); sheets.open(com.jlees.mibudget.ui.common.BarPage.LEFT) }
    }

    // Guard against double-taps popping past the first screen (which would leave a blank screen).
    val goBack: () -> Unit = { if (nav.previousBackStackEntry != null) nav.popBackStack() }

    // Image shared into the app from another app (e.g. a screenshot) → straight to scanning.
    LaunchedEffect(sharedImage) {
        if (sharedImage != null) {
            nav.navigate(ScanRoute(mode = "shared", sharedUri = sharedImage))
            onSharedImageConsumed()
        }
    }
    // Tapped a renewal notification → open that subscription.
    LaunchedEffect(openSubscriptionId) {
        if (openSubscriptionId != null) {
            nav.navigate(SubscriptionEditRoute(id = openSubscriptionId))
            onOpenSubscriptionConsumed()
        }
    }

    // The widget's heading → the Library.
    LaunchedEffect(openLibrary) {
        if (openLibrary) {
            nav.popBackStack(nav.graph.startDestinationId, inclusive = false)
            sheets.open(com.jlees.mibudget.ui.common.BarPage.LEFT); onOpenLibraryConsumed()
        }
    }

    // Scanning opens Android's camera-or-files chooser right away; the scan screen appears once a picture is picked.
    var scanTarget by rememberSaveable { mutableStateOf("auto") }
    val pickForScan = rememberImageSource { uri ->
        if (uri != null) nav.navigate(ScanRoute(mode = "shared", target = scanTarget, sharedUri = uri.toString()))
    }

    // "Resume": an unfinished purchase / subscription (left on the scan review or in the editor).
    val pending by container.pendingAdds.state.collectAsStateWithLifecycle()
    fun resumeFor(kind: ScanKind): (() -> Unit)? {
        val p = pending[kind] ?: return null
        val target = if (kind == ScanKind.SUBSCRIPTION) "subscription" else "purchase"
        return {
            when {
                p.scan != null -> nav.navigate(ScanRoute(mode = "resume", target = target))
                kind == ScanKind.SUBSCRIPTION -> nav.navigate(SubscriptionEditRoute(resume = true))
                else -> nav.navigate(PurchaseEditRoute(resume = true))
            }
        }
    }

    fun openScanResult(kind: ScanKind) {
        val target: Any = if (kind == ScanKind.SUBSCRIPTION) SubscriptionEditRoute(fromScan = true) else PurchaseEditRoute(fromScan = true)
        nav.navigate(target) { popUpTo<ScanRoute> { inclusive = true } }
    }

    // ---- motion: sheets slide in from the side (the Library rises from the bottom); closing is quicker than opening
    // sheets slide in from the side on the switcher spring (fast, no long tail); Back slides them out in exit(200)
    val sideIn: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = { slideInHorizontally(androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 900f)) { it } }
    // (while a finger drives Back on a split Pill, linearly: the page stays under the finger)
    val sideOut: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(if (actionPills.seeking) androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.LinearEasing) else Motion.exit(200)) { it }
    }
    val stay: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = { fadeOut(Motion.exit(300), targetAlpha = 0.6f) }
    val back: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(if (actionPills.seeking) androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.LinearEasing) else Motion.enter(200), initialAlpha = 0.6f)
    }

    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    var replayWalkthrough by rememberSaveable { mutableStateOf(false) }

    androidx.compose.runtime.CompositionLocalProvider(com.jlees.mibudget.ui.common.LocalActionPills provides actionPills) {
    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = PurchasesRoute(),
            enterTransition = sideIn, exitTransition = stay, popEnterTransition = back, popExitTransition = sideOut,
        ) {
            composable<PurchasesRoute> { entry ->
                val route = entry.toRoute<PurchasesRoute>()
                val scoped = route.categoryId != null || route.uncategorized || route.paymentMethodId != null
                Box(Modifier.fillMaxSize()) {
                    com.jlees.mibudget.ui.common.PanIn(com.jlees.mibudget.data.PanArea.PURCHASES) { PurchasesScreen(
                        onBack = if (scoped) goBack else null,
                        onAdd = { cat -> if (scoped) plain(PurchaseEditRoute(categoryId = cat)) else fromBar(PurchaseEditRoute(categoryId = cat)) },
                        onOpen = { id -> if (scoped) plain(PurchaseEditRoute(id = id)) else fromBar(PurchaseEditRoute(id = id)) },
                        onScan = { scanTarget = "purchase"; pickForScan() },
                        onOpenSubscription = { id -> if (scoped) plain(SubscriptionEditRoute(id = id)) else fromBar(SubscriptionEditRoute(id = id)) },
                        onResume = resumeFor(ScanKind.PURCHASE),
                        sheets = if (scoped) null else sheets,
                        onLibrary = ::openLibrary,
                        onPaymentMethods = { go(PaymentMethodsRoute) },
                        onCheckSplit = { go(CheckSplitRoute) },
                        onCurrency = { go(CurrencyRoute) },
                        onTipCalculator = { go(TipCalculatorRoute) },
                    ) }
                    // the Subscriptions page's data, loaded in the background once the start screen has settled (the page
                    // shares this model), so its first open draws the page whole at once instead of empty and then again
                    // with every row, while it's sliding in
                    if (!scoped) {
                        val subscriptions: com.jlees.mibudget.ui.subscriptions.SubscriptionsViewModel =
                            androidx.lifecycle.viewmodel.compose.viewModel(factory = com.jlees.mibudget.ui.AppViewModels.Factory)
                        LaunchedEffect(subscriptions) {
                            kotlinx.coroutines.delay(1_000)
                            subscriptions.state.first { !it.loading }
                        }
                    }
                    // the main bar's pages: the Library (its left edge button, a swipe right), Settings (right edge, a
                    // swipe left) and the Calendar (the Pill's left button, a swipe up), each sliding in over the start screen
                    if (!scoped) com.jlees.mibudget.ui.common.BarSheetHost(
                        sheets, morphContent = { com.jlees.mibudget.ui.common.MainBarPillLook(Sym.Add, Sym.CalendarMonth, Sym.MoreVert) },
                    ) { page ->
                        when (page) {
                            com.jlees.mibudget.ui.common.BarPage.LEFT -> LibrarySheet(sheets) { p ->
                                when (p) {
                                    LibraryPage.SUBSCRIPTIONS -> com.jlees.mibudget.ui.common.PanIn(com.jlees.mibudget.data.PanArea.SUBSCRIPTIONS) { SubscriptionsScreen(
                                        onAdd = { fromSheet(SubscriptionEditRoute()) },
                                        onOpen = { id -> fromSheet(SubscriptionEditRoute(id = id)) },
                                        onScan = { scanTarget = "subscription"; pickForScan() },
                                        onResume = resumeFor(ScanKind.SUBSCRIPTION),
                                    ) }
                                    LibraryPage.BUDGETS -> com.jlees.mibudget.ui.common.PanIn(com.jlees.mibudget.data.PanArea.BUDGETS) { CategoriesScreen(
                                        onBack = null,
                                        onOpenFolder = { id -> go(CategoriesRoute(id)) },
                                        onJumpToFolder = { id -> if (!nav.popBackStack(CategoriesRoute(id), inclusive = false)) go(CategoriesRoute(id)) },
                                        onViewPurchases = { id, uncategorized -> go(PurchasesRoute(categoryId = id, uncategorized = uncategorized)) },
                                        onOpenPurchase = { id -> fromSheet(PurchaseEditRoute(id = id)) },
                                        onOpenSubscription = { id -> fromSheet(SubscriptionEditRoute(id = id)) },
                                    ) }
                                    else -> ToolsScreen(
                                        onCheckSplit = { go(CheckSplitRoute) },
                                        onTipCalculator = { go(TipCalculatorRoute) },
                                        onCurrency = { go(CurrencyRoute) },
                                    )
                                }
                            }
                            com.jlees.mibudget.ui.common.BarPage.RIGHT -> SettingsScreen(
                                onImport = { go(ImportRoute) },
                                onPaymentMethods = { go(PaymentMethodsRoute) },
                                onRecentlyDeleted = { go(RecentlyDeletedRoute) },
                                onReplayWalkthrough = { sheets.close(); replayWalkthrough = true },
                            )
                            com.jlees.mibudget.ui.common.BarPage.UP -> CalendarScreen(
                                onOpenPurchase = { id -> fromSheet(PurchaseEditRoute(id = id)) },
                                onOpenSubscription = { id -> fromSheet(SubscriptionEditRoute(id = id)) },
                                onAddPurchase = { date -> fromSheet(PurchaseEditRoute(date = date.toString())) },
                            )
                        }
                    }
                }
            }
            composable<CategoriesRoute> { entry ->
                val route = entry.toRoute<CategoriesRoute>()
                CategoriesScreen(
                    onBack = if (route.folderId != null) goBack else null,
                    onOpenFolder = { id -> go(CategoriesRoute(id)) },
                    onJumpToFolder = { id -> if (!nav.popBackStack(CategoriesRoute(id), inclusive = false)) go(CategoriesRoute(id)) },
                    onViewPurchases = { id, uncategorized -> go(PurchasesRoute(categoryId = id, uncategorized = uncategorized)) },
                    onOpenPurchase = { id -> plain(PurchaseEditRoute(id = id)) },
                    onOpenSubscription = { id -> plain(SubscriptionEditRoute(id = id)) },
                )
            }
            composable<RecentlyDeletedRoute> { com.jlees.mibudget.ui.common.PanIn(com.jlees.mibudget.data.PanArea.DELETED) { com.jlees.mibudget.ui.trash.RecentlyDeletedScreen(onBack = goBack) } }
            composable<PurchaseEditRoute> { PurchaseEditScreen(onBack = goBack) }
            composable<SubscriptionEditRoute> {
                SubscriptionEditScreen(onBack = goBack, onOpenPurchase = { id -> plain(PurchaseEditRoute(id = id)) })
            }
            composable<ScanRoute> {
                ScanScreen(onBack = goBack, onContinue = ::openScanResult)
            }
            composable<CheckSplitRoute> { CheckSplitScreen(onBack = goBack) }
            composable<TipCalculatorRoute> { TipCalculatorScreen(onBack = goBack) }
            composable<CurrencyRoute> { CurrencyScreen(onBack = goBack) }
            composable<ImportRoute> { ImportScreen(onBack = goBack) }
            composable<PaymentMethodsRoute> {
                PaymentMethodsScreen(onBack = goBack, onViewPurchases = { id -> go(PurchasesRoute(paymentMethodId = id)) })
            }
        }

        // the split Pill of one item's page, above every screen so it morphs without sliding with them
        ActionPillHost(actionPills) { com.jlees.mibudget.ui.common.MainBarPillLook(Sym.Add, Sym.CalendarMonth, Sym.MoreVert) }

        // mi.'s notices, just above the Pill
        NoticeHost(Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 92.dp - 28.dp))

        // The gesture walkthrough: once for every new user, and again from Settings › About.
        val s = settings
        if (replayWalkthrough || (s != null && !s.walkthroughDone)) {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            Walkthrough(onDone = {
                replayWalkthrough = false
                scope.launch { container.settings.update { it.copy(walkthroughDone = true) } }
            })
        }
    }
    }
}
