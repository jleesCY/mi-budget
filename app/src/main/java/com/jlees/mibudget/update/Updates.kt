// App updates and What's New, from GitHub releases (mi. apps update themselves, no store needed). The rules are pure
// functions (unit-tested); the network calls send nothing but the request.
package com.jlees.mibudget.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

data class Release(val version: String, val apkUrl: String)

/** What to do at launch about What's New. */
enum class WhatsNewStep { SHOW, RECORD, NONE }

sealed interface NoteBlock {
    data class Heading(val text: String) : NoteBlock
    data class Paragraph(val text: String) : NoteBlock
    data class Bullet(val text: String) : NoteBlock
}

/** A release's notes: its tagline (from the "# mi. X.Y.Z: tagline" title) and the rest, in blocks. */
data class ReleaseNotes(val version: String, val tagline: String?, val blocks: List<NoteBlock>)

object Updates {
    private const val REPO = "https://api.github.com/repos/mi-app-hub/mi-budget/releases"

    /** Whether a launch check should ask GitHub now: 12 hours after a check last found nothing new. */
    fun dueForCheck(now: Long, lastNothingNew: Long) = now - lastNothingNew >= 12 * 3_600_000L

    /** True when [candidate] ("v1.2.3", "1.2.3-beta") is a higher x.y.z than [current]. */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /**
     * [lastSeen]: the version whose notes were last shown ("" none yet). An update shows the new version's notes; a new
     * install (walkthrough not done yet at its first start) only records its version: nothing is new to it.
     */
    fun whatsNewStep(lastSeen: String, current: String, walkthroughDone: Boolean): WhatsNewStep = when {
        lastSeen == current -> WhatsNewStep.NONE
        lastSeen.isEmpty() && !walkthroughDone -> WhatsNewStep.RECORD
        else -> WhatsNewStep.SHOW
    }

    private val LINK = Regex("""\[([^\]]+)]\(([^)]+)\)""")

    /**
     * Release notes markdown as blocks: "#" is the title (its tagline kept), "##"/"###" headings, "-"/"*" bullets, other
     * lines paragraphs. Links keep their text. Lines for developers go: a line that's only a link, and the APK's path.
     */
    fun parseNotes(version: String, markdown: String): ReleaseNotes {
        var tagline: String? = null
        val blocks = mutableListOf<NoteBlock>()
        val para = StringBuilder()
        fun clean(s: String) = LINK.replace(s.trim()) { it.groupValues[1] }
        fun endPara() { if (para.isNotBlank()) blocks += NoteBlock.Paragraph(para.toString().trim()); para.clear() }
        for (raw in markdown.lines()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> endPara()
                LINK.matches(line) || line.contains(".apk") -> endPara()
                line.startsWith("# ") -> { endPara(); tagline = line.removePrefix("# ").substringAfter(": ", "").ifBlank { null } }
                line.startsWith("#") -> { endPara(); blocks += NoteBlock.Heading(clean(line.trimStart('#'))) }
                line.startsWith("- ") || line.startsWith("* ") -> { endPara(); blocks += NoteBlock.Bullet(clean(line.drop(2))) }
                else -> para.append(clean(line)).append(' ')
            }
        }
        endPara()
        return ReleaseNotes(version, tagline, blocks)
    }

    private fun get(url: String): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        return JSONObject(conn.inputStream.use { it.readBytes().decodeToString() })
    }

    /** The latest release if newer than [current] and it ships an APK; a failure (offline) is reported, not "up to date". */
    suspend fun check(current: String): Result<Release?> = withContext(Dispatchers.IO) {
        runCatching {
            val json = get("$REPO/latest")
            val tag = json.getString("tag_name")
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it).getString("browser_download_url") }
                // only APKs served from GitHub itself
                .firstOrNull { it.endsWith(".apk") && URL(it).host.equals("github.com", ignoreCase = true) }
            if (apk != null && isNewer(tag, current)) Release(tag.removePrefix("v"), apk) else null
        }
    }

    /** The notes of the release tagged "v[version]", null when there's no such release (or it has no notes). */
    suspend fun notes(version: String): ReleaseNotes? = withContext(Dispatchers.IO) {
        try {
            get("$REPO/tags/v$version").optString("body").takeIf { it.isNotBlank() }?.let { parseNotes(version, it) }
        } catch (_: FileNotFoundException) { null }
    }

    /**
     * Downloads the APK into the cache and hands it to Android's installer, which asks first (and, the first time, to
     * allow installs from mi. Budget). It only accepts an update signed with the same key, and the app's data stays.
     */
    suspend fun downloadAndInstall(ctx: Context, release: Release) {
        val file = withContext(Dispatchers.IO) {
            val dir = File(ctx.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
            File(dir, "mi-budget-${release.version}.apk").also { f ->
                URL(release.apkUrl).openStream().use { input -> f.outputStream().use { input.copyTo(it) } }
            }
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
