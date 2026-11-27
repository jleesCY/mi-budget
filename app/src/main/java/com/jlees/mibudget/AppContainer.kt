package com.jlees.mibudget

import android.content.Context
import com.jlees.mibudget.data.BudgetRepository
import com.jlees.mibudget.data.ReceiptStore
import com.jlees.mibudget.data.SettingsRepository
import com.jlees.mibudget.data.backup.BackupManager
import com.jlees.mibudget.data.db.AppDatabase
import com.jlees.mibudget.icons.BrandCatalog
import com.jlees.mibudget.scan.OcrEngine
import com.jlees.mibudget.scan.ReceiptParser
import com.jlees.mibudget.scan.ScanDraftHolder
import com.jlees.mibudget.scan.NanoScanner

/**
 * Manual dependency container. The app is small enough that Hilt would add more build
 * complexity than it saves; swapping later is mechanical.
 */
class AppContainer(val context: Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val brands: BrandCatalog = BrandCatalog(context)
    val receipts: ReceiptStore = ReceiptStore(context)
    /** Custom pictures chosen as icons (payment methods, merchants, subscriptions). */
    val paymentIcons: ReceiptStore = ReceiptStore(context, folder = "icons", maxDimension = 256, quality = 85)
    val settings: SettingsRepository = SettingsRepository(context)
    val repository: BudgetRepository = BudgetRepository(database, receipts, brands, paymentIcons)
    val backup: BackupManager = BackupManager(context, repository, settings)
    val ocr: OcrEngine = OcrEngine(context)
    // Lazy: building it reads the brand catalog, which shouldn't happen on the main thread at launch.
    val parser: ReceiptParser by lazy { ReceiptParser(brands.matcher) }
    /** Gemini Nano, on phones that have it built in. */
    val nanoScanner: NanoScanner = NanoScanner()
    val scanDrafts: ScanDraftHolder = ScanDraftHolder()
    /** Unfinished purchase / subscription adds ("Resume"). */
    val pendingAdds: com.jlees.mibudget.data.PendingAdds = com.jlees.mibudget.data.PendingAdds(context, receipts)
    val itemScanner: com.jlees.mibudget.scan.ItemScanner = com.jlees.mibudget.scan.ItemScanner(ocr, nanoScanner, settings)
    val fx: com.jlees.mibudget.data.FxRepository = com.jlees.mibudget.data.FxRepository(context)
    /** Update offers and What's New, from GitHub releases. */
    val updates: com.jlees.mibudget.update.AppUpdates by lazy { com.jlees.mibudget.update.AppUpdates(this) }
    /** For short clean-up work that must finish after a screen has closed (its scope is gone by then). */
    val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    fun receiptSession() = com.jlees.mibudget.data.ReceiptSession(context, receipts, repository)

}
