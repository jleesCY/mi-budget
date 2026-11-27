// Settings › About: updates, help, feedback, credits.
package com.jlees.mibudget.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jlees.mibudget.BuildConfig
import com.jlees.mibudget.R
import com.jlees.mibudget.ui.common.Sym

fun aboutSection(c: SettingsCtx, checkForUpdates: () -> Unit): SettingsSection {
    val version = BuildConfig.VERSION_NAME
    val (brands, withLogos) = c.vm.brandStats
    return SettingsSection("about", "About", Sym.Info, "Updates, help, feedback, credits", listOf(
        SettingsHeading("mi. Budget"),
        SettingsRow("Check for Updates", "update github new version") {
            Item(Sym.RestartAlt, "Check for Updates", "mi. Budget $version", onClick = checkForUpdates)
        },
        SettingsHeading("Help & Feedback"),
        SettingsRow("Gesture Walkthrough", "gestures tour pill help tutorial") {
            Item(Sym.Toolbar, "Gesture Walkthrough", "Try every swipe, step by step", onClick = c.replayWalkthrough)
        },
        SettingsRow("User Guide", "help manual how to") {
            Item(Sym.Help, "User Guide", "Every gesture, tool and setting", onClick = { c.openUrl("$DOCS_URL/user/") })
        },
        SettingsRow("Report a Bug or Suggest a Feature", "issue feedback github problem idea") {
            Item(Sym.BugReport, "Report a Bug or Suggest a Feature", "Open a new issue on GitHub", onClick = { c.openUrl("$REPO_URL/issues/new") })
        },
        SettingsHeading("More mi. Apps"),
        *MI_APPS.map { app ->
            SettingsRow(app.name, "more mi apps other family ${app.keywords}") { AppItem(app) }
        }.toTypedArray(),
        SettingsHeading("Legal & Credits"),
        SettingsRow("Privacy Policy", "data personal tracking") {
            Item(Sym.Shield, "Privacy Policy", "mi. Budget collects no personal data", onClick = { c.openUrl("$DOCS_URL/privacy/") })
        },
        SettingsRow("Source Code", "github open source license mit") {
            Item(Sym.Code, "Source Code", "github.com/mi-app-hub/mi-budget", onClick = { c.openUrl(REPO_URL) })
        },
        SettingsRow("Exchange Rates", "currency frankfurter ecb data") {
            Item(Sym.CurrencyExchange, "Exchange Rates", "Frankfurter: the European Central Bank and other central banks")
        },
        SettingsRow("Brand Logos", "logos icons merchants simple icons iconify") {
            Item(Sym.Storefront, "Brand Logos", "$brands brands, $withLogos with logos: Simple Icons (CC0) and Iconify open icon sets. Trademarks belong to their owners.")
        },
        SettingsRow("Credits", "licenses open source font icons") {
            ExpandItem(Sym.Info, "Credits", "Open-source projects mi. Budget is built with", null) {
                CreditLine("Google Sans Flex", "SIL Open Font License")
                CreditLine("Material Symbols", "Google · Apache 2.0")
                CreditLine("Jetpack Compose and AndroidX", "Google · Apache 2.0")
                CreditLine("materialkolor", "Jordon de Hoog · MIT")
                CreditLine("ML Kit Text Recognition", "Google")
                CreditLine("Gemini Nano (ML Kit GenAI)", "Google, on phones that have it")
                CreditLine("Coil", "Coil Contributors · Apache 2.0")
                CreditLine("AndroidSVG", "Paul LeBeau · Apache 2.0")
                CreditLine("Kotlin and kotlinx", "JetBrains · Apache 2.0")
            }
        },
    ))
}

/** One of the other mi. apps (mi-brand's family registry): its icon, name, one line, package and site. */
private class FamilyApp(val name: String, val icon: Int, val line: String, val pkg: String, val slug: String, val keywords: String)

// Every other mi. app, released or on its way: each one's site goes live with it, and its row works as soon as it does.
private val MI_APPS = listOf(
    FamilyApp("mi. Browser", R.drawable.ic_family_browser, "A simple, private browser built around one Pill", "com.jlees.mibrowser", "mi-browser", "browser web internet"),
    FamilyApp("mi. Notes", R.drawable.ic_family_notes, "Notes in folders on one calm page", "com.jlees.minotes", "mi-notes", "notes writing"),
    FamilyApp("mi. Edits", R.drawable.ic_family_edits, "A simple, private video and audio editor", "com.jlees.miedits", "mi-edits", "edits video audio editor"),
    FamilyApp("mi. Sports", R.drawable.ic_family_sports, "Live scores for the teams, players and leagues you follow", "com.jlees.misports", "mi-sports", "sports scores teams"),
    FamilyApp("mi. Weather", R.drawable.ic_family_weather, "Every detail of the weather where you are, all inside the app", "com.jlees.miweather", "mi-weather", "weather forecast"),
)

/** Opens the app if it's installed (the manifest's <queries> lets Android 11+ see it), else its site. */
@Composable
private fun AppItem(app: FamilyApp) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    Item(Sym.OpenInNew, app.name, app.line,
        onClick = {
            val intent = ctx.packageManager.getLaunchIntentForPackage(app.pkg)
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://mi-app-hub.github.io/${app.slug}/"))
            runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        leading = {
            // the launcher icon's glyph on its near-black, filling the circle as on the home screen
            Image(painterResource(app.icon), null, Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF0D0D0F)).scale(1.5f))
        },
        trailing = { Icon(Sym.OpenInNew, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant) },
    )
}
