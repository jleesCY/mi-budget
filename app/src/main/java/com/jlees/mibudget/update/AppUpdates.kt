// The update offer and What's New, held app-wide (AppContainer) and shown by UpdateDialogs over the app.
package com.jlees.mibudget.update


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.AppContainer
import com.jlees.mibudget.BuildConfig
import com.jlees.mibudget.ui.common.DialogAction
import com.jlees.mibudget.ui.common.DialogCard
import com.jlees.mibudget.ui.common.ModalFrame
import com.jlees.mibudget.ui.common.Notices
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class AppUpdates(private val c: AppContainer) {
    var offer by mutableStateOf<Release?>(null)
        private set
    var notes by mutableStateOf<ReleaseNotes?>(null)
        private set
    /** A plain message (title, text): "Up to Date", "Couldn't Check for Updates". */
    var message by mutableStateOf<Pair<String, String>?>(null)

    private val current get() = BuildConfig.VERSION_NAME.removeSuffix("-dev")

    /** At launch: What's New after an update, then a quiet update check (if it's on). Never in dev builds. */
    fun atLaunch() = c.appScope.launch {
        val s = c.settings.current()
        when (Updates.whatsNewStep(s.lastWhatsNew, current, s.walkthroughDone)) {
            WhatsNewStep.NONE -> {}
            WhatsNewStep.RECORD -> record()
            WhatsNewStep.SHOW -> {
                // offline: try again next launch; no release for this version: nothing to show, ever
                val r = runCatching { Updates.notes(current) }
                if (r.isSuccess) r.getOrNull()?.takeIf { it.blocks.isNotEmpty() }?.let { notes = it } ?: record()
            }
        }
        // at most every 12 hours once a check found nothing new; a found update is offered at every launch
        val now = System.currentTimeMillis()
        if (!BuildConfig.DEBUG && Updates.dueForCheck(now, s.lastUpdateCheck)) {
            val r = Updates.check(current)
            r.getOrNull()?.let { offer = it } ?: if (r.isSuccess) c.settings.update { it.copy(lastUpdateCheck = now) } else Unit
        }
    }

    /** Settings › About › Check for Updates: says so when there's nothing new. */
    fun checkNow() = c.appScope.launch {
        if (BuildConfig.DEBUG) { message = "Development Build" to "mi. dev is a separate app for development and doesn't update from GitHub releases."; return@launch }
        Updates.check(current)
            .onSuccess { r -> if (r != null) offer = r else message = "Up to Date" to "mi. Budget $current is the latest version." }
            .onFailure { message = "Couldn't Check for Updates" to "GitHub couldn't be reached. Check your connection and try again." }
    }

    fun install() {
        val r = offer ?: return
        offer = null
        Notices.show("Downloading update…")
        c.appScope.launch {
            runCatching { Updates.downloadAndInstall(c.context, r) }
                .onFailure { message = "Update Failed" to "Couldn't download the update. Try again later." }
        }
    }

    fun dismissOffer() { offer = null }

    fun dismissNotes() { notes = null; c.appScope.launch { record() } }

    private suspend fun record() = c.settings.update { it.copy(lastWhatsNew = current) }
}

/** The update offer, What's New ("Got It") and update messages, over the whole app, as mi. dialogs. */
@Composable
fun UpdateDialogs(u: AppUpdates) {
    val cs = MaterialTheme.colorScheme
    u.offer?.let { r ->
        ModalFrame(u::dismissOffer) {
            DialogCard("Update Available", actions = {
                DialogAction("Not Now", onClick = u::dismissOffer)
                DialogAction("Install", onClick = u::install)
            }) { Text("mi. Budget ${r.version} is available. Your purchases, budgets and settings stay as they are.") }
        }
    }
    u.message?.let { (title, text) ->
        ModalFrame({ u.message = null }) {
            DialogCard(title, actions = { DialogAction("Close") { u.message = null } }) { Text(text) }
        }
    }
    u.notes?.let { n ->
        ModalFrame(u::dismissNotes) {
            DialogCard("What's New in ${n.version}", actions = { DialogAction("Got It", onClick = u::dismissNotes) }) {
                n.tagline?.let { Text(it.replaceFirstChar(Char::uppercase), Modifier.padding(bottom = 12.dp), color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp) }
                n.blocks.forEach { b ->
                    when (b) {
                        is NoteBlock.Heading -> Text(b.text, Modifier.padding(top = 14.dp, bottom = 4.dp), color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        is NoteBlock.Paragraph -> NoteText(b.text, Modifier.padding(bottom = 6.dp))
                        is NoteBlock.Bullet -> Row(Modifier.padding(vertical = 3.dp)) {
                            Text("•", Modifier.width(16.dp), color = cs.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            NoteText(b.text, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** A line of the notes, its **bold** parts in bold. */
@Composable
private fun NoteText(text: String, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            text.split("**").forEachIndexed { i, part -> if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) { append(part) } else append(part) }
        },
        modifier, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
