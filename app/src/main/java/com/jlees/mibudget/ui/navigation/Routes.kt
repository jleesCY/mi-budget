package com.jlees.mibudget.ui.navigation

import kotlinx.serialization.Serializable

/** Settings › Your Data › Recently Deleted. */
@Serializable
data object RecentlyDeletedRoute

/** Purchases list. With [categoryId] it opens pre-filtered (pushed from a category folder). */
@Serializable
data class PurchasesRoute(
    val categoryId: String? = null,
    val uncategorized: Boolean = false,
    /** Opened from Settings → Payment methods. */
    val paymentMethodId: String? = null,
)

/** Category folder browser. null = top level. */
@Serializable
data class CategoriesRoute(val folderId: String? = null)

/** The tools the Library's Tools page opens. */
@Serializable
data object CheckSplitRoute

@Serializable
data object TipCalculatorRoute

@Serializable
data object CurrencyRoute

@Serializable
data class PurchaseEditRoute(
    val id: String? = null,
    val fromScan: Boolean = false,
    val categoryId: String? = null,
    /** ISO date to pre-fill (from the calendar). */
    val date: String? = null,
    /** Reopen the unfinished new purchase. */
    val resume: Boolean = false,
)

@Serializable
data class SubscriptionEditRoute(val id: String? = null, val fromScan: Boolean = false, val resume: Boolean = false)

/**
 * mode = "scan" (camera-or-gallery chooser) | "shared" (an image URI) | "resume" (the unfinished scan
 * review for [target]); target = "auto" | "purchase" | "subscription".
 */
@Serializable
data class ScanRoute(val mode: String = "scan", val target: String = "auto", val sharedUri: String? = null)

@Serializable
data object ImportRoute

@Serializable
data object PaymentMethodsRoute
