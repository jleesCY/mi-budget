package com.jlees.mibudget.ui.settings

import com.jlees.mibudget.data.AppSettings
import com.jlees.mibudget.data.FxRepository
import com.jlees.mibudget.scan.ScanEngine
import com.jlees.mibudget.ui.components.NotificationPermissionState

/**
 * What the sections need to build their rows: the settings, the ViewModel, where rows lead, and the confirmations
 * SettingsScreen shows for them (anything Undo can't bring back is asked first).
 */
class SettingsCtx(
    val s: AppSettings,
    val vm: SettingsViewModel,
    val permission: NotificationPermissionState,
    val engines: List<EngineRow>,
    val nanoMessage: String?,
    val nanoSupported: Boolean?,
    val fx: FxRepository.Rates?,
    val fxBusy: Boolean,
    val counts: DataCounts,
    val safety: SafetyBackupInfo?,
    val trashCount: Int,
    // where rows lead
    val openPaymentMethods: () -> Unit,
    val openImport: () -> Unit,
    val openRecentlyDeleted: () -> Unit,
    val replayWalkthrough: () -> Unit,
    val exportBackup: () -> Unit,
    val openUrl: (String) -> Unit,
    // confirmations
    val askResetSettings: () -> Unit,
    val askWipe: () -> Unit,
    val askEmptyTrash: () -> Unit,
    val askDeleteFx: () -> Unit,
    val askRestoreSafety: () -> Unit,
    val askDeleteSafety: () -> Unit,
) {
    fun set(f: (AppSettings) -> AppSettings) { vm.update(f) }
}

/** The app's GitHub repository (updates, issues, source, user guide and privacy policy). */
const val REPO_URL = "https://github.com/mi-app-hub/mi-budget"
/** The docs site (docs/ in the repo, on GitHub Pages): the User Guide and the Privacy Policy open here. */
const val DOCS_URL = "https://mi-app-hub.github.io/mi-budget"
