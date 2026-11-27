import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Collections
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// ===== Release version: bump both for every release =====
// versionName is what people see ("0.1.1") and names the APK. versionCode must go up by at least 1
// every release, or Android won't install the new APK over the old one.
val appVersionName = "0.2.0"
val appVersionCode = 3

// ===== Release signing =====
// Put a keystore.properties file in the project root (it's git-ignored, never commit it) with:
//   storeFile=/absolute/path/to/mi-budget-release.jks
//   storePassword=…
//   keyAlias=mibudget
//   keyPassword=…
// Every release must be signed with the SAME key, or phones refuse to update. Without the file,
// release builds fall back to this computer's debug key (fine for trying things out).
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.jlees.mibudget"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jlees.mibudget"
        // Android 8.0+, as every mi. app (newer features check the version: Adaptive colours on 12+, and so on)
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Your release key from keystore.properties; this computer's debug key if there isn't one.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            // 64-bit phones only: ML Kit ships native code, and 32-bit/x86 copies more than doubled
            // the APK. 32-bit phones are old budget models; the emulator is arm64 too.
            ndk { abiFilters += "arm64-v8a" }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    // Native code compressed in the APK (unpacked at install): roughly halves the download.
    packaging { jniLibs { useLegacyPackaging = true } }
    // mi. Budget is in English only: drop the libraries' own strings in other languages (AndroidX ships ~80), which
    // otherwise sit in resources.arsc unused
    androidResources { localeFilters += "en" }
}

// APKs are named after the release: mi-budget-0.2.0-release.apk (and mi-budget-0.2.0-debug.apk).
base {
    archivesName.set("mi-budget-$appVersionName")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "androidx.compose.ui.text.ExperimentalTextApi",
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    // The mi. colour scheme: a whole Material 3 scheme from one seed accent.
    implementation(libs.materialkolor)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.coil.compose)
    // Renders the full-color / outline brand logos fetched from Iconify (SVG).
    implementation(libs.androidsvg)
    // Gemini Nano on phones that have it built in (Pixel 9+, Galaxy S26, OPPO Find X8/X9…).
    implementation(libs.genai.prompt)
    // Installs the baseline profiles that ship inside Compose/AndroidX — big startup & scroll win in release builds.
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)

}

// ===== BEGIN brand icon fetcher (shared with build.gradle.kts) =====
/**
 * Build-time downloader for brand icons. Sources, in order:
 *  1. Simple Icons (CC0) — single-path monochrome logos, drawn on the brand color.
 *  2. Iconify open icon sets, searched by name — brand sets first (CoreUI Brands, Font Awesome
 *     Brands, Boxicons logos, Remix, MDI, Custom Brand Icons), then full-color "logos", then
 *     Arcticons (outline app icons, which cover many banks & utilities).
 * A brand can pin an exact icon in brands.json with "icon": "prefix:name".
 */
class BrandIconFetcher(private val log: (String) -> Unit) {
    private val OLD_SIMPLE_ICONS = "11"
    private val slurper = JsonSlurper()

    fun get(url: String): String? = runCatching {
        val c = URI(url).toURL().openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "mi-budget-build/1.0 (brand icon fetch)")
        if (c.responseCode != 200) null else c.inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()

    fun norm(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase()
        .replace("+", "plus").replace("&", "and").replace(".", "dot")
        .replace(Regex("[^a-z0-9]"), "")

    /** Loose key: also drops "dot"/"and"/"plus" so "Booking.com" ≈ "booking com" ≈ "bookingcom". */
    fun loose(s: String): String = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    // ---------- Simple Icons ----------
    private var siSlugs: Map<String, String> = emptyMap()

    fun loadSimpleIcons() {
        val text = listOf(
            "https://cdn.jsdelivr.net/npm/simple-icons@latest/data/simple-icons.json",
            "https://cdn.jsdelivr.net/npm/simple-icons@latest/_data/simple-icons.json",
        ).firstNotNullOfOrNull { get(it) }
        if (text == null) { log("Simple Icons index unavailable — skipping that source."); return }
        val parsed = slurper.parseText(text)
        @Suppress("UNCHECKED_CAST")
        val list = (if (parsed is Map<*, *>) parsed["icons"] else parsed) as List<Map<String, Any?>>
        val out = HashMap<String, String>()
        for (icon in list) {
            val title = icon["title"] as? String ?: continue
            val slug = (icon["slug"] as? String) ?: norm(title)
            out.putIfAbsent(slug, slug)
            out.putIfAbsent(norm(title), slug)
            out.putIfAbsent(loose(title), slug)
            @Suppress("UNCHECKED_CAST")
            val aka = ((icon["aliases"] as? Map<String, Any?>)?.get("aka") as? List<String>).orEmpty()
            aka.forEach { out.putIfAbsent(norm(it), slug); out.putIfAbsent(loose(it), slug) }
        }
        siSlugs = out
        log("Simple Icons: ${list.size} icons indexed")
    }

    fun simpleIconPath(slug: String, version: String = "latest"): String? {
        val svg = get("https://cdn.jsdelivr.net/npm/simple-icons@$version/icons/$slug.svg") ?: return null
        return Regex("<path[^>]*\\sd=\"([^\"]+)\"").find(svg)?.groupValues?.get(1)
    }

    // ---------- Iconify ----------
    private val monoSets = listOf("simple-icons", "cib", "fa6-brands", "fa-brands", "bxl", "ri", "mdi", "cbi", "streamline-logos", "tabler", "jam")
    private val colorSets = listOf("logos", "token-branded", "devicon", "skill-icons", "vscode-icons")
    private val outlineSets = listOf("arcticons")
    private val allSets = monoSets + colorSets + outlineSets
    private val stripSuffixes = listOf("-icon", "-logo", "-logomark", "-wordmark", "-solid", "-fill", "-filled", "-color", "-colored",
        "-original", "-plain", "-square", "-circle", "-alt", "-mark", "-rounded", "-outline", "-light", "-dark")
    private val stripPrefixes = listOf("cc-", "brand-", "logo-", "cib-")

    private fun baseName(name: String): String {
        var n = name
        var changed = true
        while (changed) {
            changed = false
            for (p in stripPrefixes) if (n.startsWith(p)) { n = n.removePrefix(p); changed = true }
            for (s in stripSuffixes) if (n.endsWith(s)) { n = n.removeSuffix(s); changed = true }
            n = n.replace(Regex("-\\d+$"), "").also { if (it != n) changed = true }
        }
        return n.replace("-", "")
    }

    /** Ranks an Iconify result for a brand; null = not the same brand. Lower is better. */
    fun rank(full: String, keys: Set<String>): Int? {
        val prefix = full.substringBefore(':')
        val name = full.substringAfter(':')
        val setRank = allSets.indexOf(prefix).takeIf { it >= 0 } ?: return null
        val base = baseName(name)
        val exact = base in keys || base.removeSuffix("bank") in keys || keys.any { it.removeSuffix("bank") == base }
        if (!exact) return null
        // Prefer the square "mark" over a wide wordmark; and the plain version over variants.
        val variant = when {
            name.endsWith("-icon") || name.endsWith("-mark") || name.endsWith("-logomark") -> 0
            baseName(name) == name.replace("-", "") -> 1
            name.contains("wordmark") -> 5
            else -> 2
        }
        return setRank * 10 + variant
    }

    fun iconifySearch(query: String, keys: Set<String>): String? {
        val q = URLEncoder.encode(query, "UTF-8")
        val text = get("https://api.iconify.design/search?query=$q&limit=96&prefixes=${allSets.joinToString(",")}")
            ?: get("https://api.iconify.design/search?query=$q&limit=96") ?: return null
        @Suppress("UNCHECKED_CAST")
        val icons = (slurper.parseText(text) as Map<String, Any?>)["icons"] as? List<String> ?: return null
        return icons.mapNotNull { n -> rank(n, keys)?.let { n to it } }.minByOrNull { it.second }?.first
    }

    /** Returns (svg, mono) for an Iconify icon id like "logos:visa". */
    fun iconifySvg(full: String): Pair<String, Boolean>? {
        val prefix = full.substringBefore(':')
        val name = full.substringAfter(':')
        val text = get("https://api.iconify.design/$prefix.json?icons=$name") ?: return null
        @Suppress("UNCHECKED_CAST")
        val root = slurper.parseText(text) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val icon = (root["icons"] as? Map<String, Map<String, Any?>>)?.get(name) ?: return null
        val body = icon["body"] as? String ?: return null
        val w = (icon["width"] ?: root["width"] ?: 16).toString()
        val h = (icon["height"] ?: root["height"] ?: 16).toString()
        val left = (icon["left"] ?: 0).toString()
        val top = (icon["top"] ?: 0).toString()
        val hasColors = Regex("(fill|stroke|stop-color)=\"#|(fill|stroke|stop-color):\\s*#|url\\(#").containsMatchIn(body)
        val mono = prefix !in colorSets && !hasColors
        val svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"$left $top $w $h\" width=\"$w\" height=\"$h\">$body</svg>"
        return svg to mono
    }

    /**
     * Resolves icons for every brand. [existing] entries are kept unless [refresh].
     * Returns id → entry map ready to serialize, plus a list of brands with no icon found.
     */
    fun run(brands: List<Map<String, Any?>>, existing: Map<String, Any?>, refresh: Boolean): Pair<Map<String, Any?>, List<String>> {
        loadSimpleIcons()
        val result = ConcurrentHashMap<String, Any?>()
        if (!refresh) result.putAll(existing.filterValues { it != null })
        val missing = Collections.synchronizedList(mutableListOf<String>())
        val pool = Executors.newFixedThreadPool(8)
        val done = AtomicInteger()
        val todo = brands.filter { !result.containsKey(it["id"] as String) }
        log("Fetching icons for ${todo.size} brands (${result.size} already present)…")
        todo.forEach { b ->
            pool.submit {
                val id = b["id"] as String
                val name = b["name"] as String
                @Suppress("UNCHECKED_CAST")
                val aliases = (b["aliases"] as? List<String>).orEmpty()
                val pinned = b["icon"] as? String
                val keys = (listOf(id, name) + aliases).flatMap { listOf(norm(it), loose(it)) }.toSet()
                var entry: Map<String, Any?>? = null
                try {
                    if (pinned != null) {
                        entry = iconifySvg(pinned)?.let { (svg, mono) -> mapOf("svg" to svg, "mono" to mono, "src" to pinned) }
                    }
                    if (entry == null) {
                        val slug = (b["slug"] as? String)?.takeIf { it in siSlugs.values }
                            ?: keys.firstNotNullOfOrNull { siSlugs[it] }
                        if (slug != null) simpleIconPath(slug)?.let { entry = mapOf("path" to it, "src" to "simple-icons:$slug") }
                    }
                    if (entry == null) {
                        // Simple Icons has dropped some big brands (Amazon, Microsoft, Slack…) over time;
                        // an older release still has them.
                        val guesses = (listOfNotNull(b["slug"] as? String) + listOf(norm(name)) + aliases.map(::norm)).distinct().take(4)
                        search@ for (v in listOf("latest", OLD_SIMPLE_ICONS)) for (g in guesses) {
                            val d = simpleIconPath(g, v) ?: continue
                            entry = mapOf("path" to d, "src" to "simple-icons@$v:$g")
                            break@search
                        }
                    }
                    if (entry == null) {
                        val hit = iconifySearch(name, keys) ?: aliases.firstNotNullOfOrNull { iconifySearch(it, keys) }
                        if (hit != null) iconifySvg(hit)?.let { (svg, mono) ->
                            if (svg.length <= 40_000) entry = mapOf("svg" to svg, "mono" to mono, "src" to hit)
                        }
                    }
                } catch (e: Exception) {
                    log("  ! $id: ${e.message}")
                }
                if (entry != null) result[id] = entry else missing += name
                val n = done.incrementAndGet()
                if (n % 50 == 0) log("  …$n / ${todo.size}")
            }
        }
        pool.shutdown()
        pool.awaitTermination(20, TimeUnit.MINUTES)
        return result.toSortedMap() to missing.sorted()
    }
}
// ===== END brand icon fetcher =====


/**
 * Downloads a logo for every brand in src/main/assets/brands.json into
 * src/main/assets/brand_icons.json (sources: Simple Icons, then Iconify's open icon sets).
 *
 *   ./gradlew :app:fetchBrandIcons              — fetch logos for brands that don't have one yet
 *   ./gradlew :app:fetchBrandIcons -Prefresh    — re-download everything
 *
 * Brands that still have no logo are listed in app/brand_icons_missing.txt; pin one by adding
 * "icon": "prefix:name" (any id from https://icon-sets.iconify.design) to that brand in brands.json.
 * The network is only used here, at build time on your computer — the app itself stays offline.
 */
tasks.register("fetchBrandIcons") {
    group = "mi budget"
    description = "Download brand logos (Simple Icons + Iconify) for the brand catalog into assets."
    val catalog = file("src/main/assets/brands.json")
    val out = file("src/main/assets/brand_icons.json")
    val report = file("brand_icons_missing.txt")
    val refresh = project.hasProperty("refresh")
    doLast {
        val slurper = JsonSlurper()
        @Suppress("UNCHECKED_CAST")
        val brands = (slurper.parse(catalog) as Map<String, Any?>)["brands"] as List<Map<String, Any?>>
        @Suppress("UNCHECKED_CAST")
        val existing = if (out.exists()) ((slurper.parse(out) as Map<String, Any?>)["icons"] as? Map<String, Any?>).orEmpty() else emptyMap()
        val (icons, missing) = BrandIconFetcher { println(it) }.run(brands, existing, refresh)
        val ids = brands.map { it["id"] as String }.toSet()
        val kept = icons.filterKeys { it in ids }
        out.writeText(JsonOutput.toJson(mapOf("version" to 1, "icons" to kept)))
        report.writeText(
            "Brands without a logo (they show their category icon instead):\n" + missing.joinToString("\n") + "\n"
        )
        println("Brand icons: ${kept.size} of ${brands.size} brands have a logo (${out.length() / 1024} KB).")
        println("${missing.size} without one — see ${report.relativeTo(rootDir)}")
    }
}

/**
 * Refreshes the currency rates built into the app (assets/fx_rates.json) from Frankfurter
 * (ECB and other central banks). Run before a release so the built-in rates are recent:
 *   ./gradlew :app:fetchFxRates
 */
tasks.register("fetchFxRates") {
    group = "mi budget"
    description = "Refresh the built-in currency rates (assets/fx_rates.json)."
    val out = file("src/main/assets/fx_rates.json")
    doLast {
        val rows = JsonSlurper().parseText(URI("https://api.frankfurter.dev/v2/rates").toURL().readText()) as List<*>
        // Metals and special units aren't currencies you pay with.
        val skip = setOf("XAU", "XAG", "XPD", "XPT", "XDR", "EUR")
        val rates = sortedMapOf<String, Any?>()
        var date = ""
        for (r in rows) {
            val m = r as Map<*, *>
            if (m["base"] != "EUR") continue
            val quote = m["quote"] as String
            if (quote in skip) continue
            rates[quote] = m["rate"]
            val d = m["date"] as String
            if (d > date) date = d
        }
        out.writeText(JsonOutput.toJson(mapOf("base" to "EUR", "date" to date, "source" to "Frankfurter (ECB and other central banks), bundled with the app", "rates" to rates)))
        println("Saved ${rates.size} currencies (rates from $date) to ${out.relativeTo(projectDir)}")
    }
}
