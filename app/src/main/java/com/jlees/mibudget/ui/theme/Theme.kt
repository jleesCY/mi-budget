package com.jlees.mibudget.ui.theme

import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.MaterialTheme
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.jlees.mibudget.data.AppFont
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.jlees.mibudget.data.AppSettings
import com.jlees.mibudget.data.ThemeMode

/** What the mi. pieces need beyond the colour scheme: the Corners setting and whether it's dark / Pure Black. */
@androidx.compose.runtime.Immutable
data class MiUi(
    val radius: Int = 28, val isDark: Boolean = true, val pureBlack: Boolean = false,
    val padding: com.jlees.mibudget.data.Spacing = com.jlees.mibudget.data.Spacing.NORMAL,
    val listStyle: com.jlees.mibudget.data.ListStyle = com.jlees.mibudget.data.ListStyle.TABLE,
    val fontScale: Float = 1f,
    /**
     * The accent itself, at full strength (the seed: #FF3B30 for Red, #34C759 for Green): only the app's two big +
     * buttons wear it (the main bar's, and the Subscriptions page's), so they're equally vivid in every accent. The
     * scheme's primary isn't: the Vibrant palette makes green's very bright and red's a muted pink. Adaptive uses primary.
     */
    val accent: Color = Color(0xFF34C759),
    /** Black or white on [accent], whichever reads better. */
    val onAccent: Color = Color(0xFF0D0D0F),
) {
    /** The Pill's bar: a step up from the page; in light mode with a little of the accent mixed in. */
    val ColorScheme.pillBar: Color get() = if (isDark) surfaceContainerHigh else androidx.compose.ui.graphics.lerp(surfaceContainerHighest, primaryContainer, 0.45f)
    /** The start screen: `surface`, with 15% of the accent's container in light mode so it isn't colourless. */
    val ColorScheme.homeBackground: Color get() = if (isDark) surface else androidx.compose.ui.graphics.lerp(surface, primaryContainer, 0.15f)
    /** The field inside the Pill. */
    val ColorScheme.pillField: Color get() = if (isDark) surfaceContainerHighest else androidx.compose.ui.graphics.lerp(surfaceDim, primaryContainer, 0.4f)
}

val LocalMi = androidx.compose.runtime.staticCompositionLocalOf { MiUi() }

private fun shapesFor(radius: Int) = Shapes(
    extraSmall = RoundedCornerShape((radius * 0.3f).dp),
    small = RoundedCornerShape((radius * 0.45f).dp),
    medium = RoundedCornerShape((radius * 0.7f).dp),
    large = RoundedCornerShape(radius.dp),
    // dialogs keep M3's 28dp unless corners are off
    extraLarge = RoundedCornerShape(if (radius == 0) 0.dp else 28.dp),
)

private val base = Typography()

/** Default type scale with heavier display/headline weights for the expressive feel. */
val AppTypography = base.copy(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.SemiBold),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** The same see-through 3-button navigation bar backgrounds Android's edge-to-edge default uses. */
private val LightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@Composable
fun MiTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    // Status / navigation bar icons follow the app's theme, not the phone's: with the phone in dark mode and the app
    // set to Light, the clock and battery icons would otherwise be white on white.
    val activity = remember(context) {
        var c: android.content.Context? = context
        while (c is android.content.ContextWrapper && c !is androidx.activity.ComponentActivity) c = c.baseContext
        c as? androidx.activity.ComponentActivity
    }
    androidx.compose.runtime.DisposableEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(LightScrim, DarkScrim) { dark },
        )
        onDispose { }
    }
    // The whole scheme from the accent seed (Vibrant; Monochrome for Gray, so gray really is gray), or the wallpaper's
    // colours for Adaptive. An invalid stored colour falls back to the app's green (hexColor).
    val seeded = rememberDynamicColorScheme(
        seedColor = Color(com.jlees.mibudget.data.hexColor(settings.accentColor)), isDark = dark, isAmoled = false,
        style = if (settings.accentColor == com.jlees.mibudget.data.AppSettings.GRAY_ACCENT) PaletteStyle.Monochrome else PaletteStyle.Vibrant,
    )
    // Adaptive needs Android 12+ (before that, the app's own colour: hexColor falls back to it)
    val baseScheme = if (settings.accentColor == com.jlees.mibudget.data.DYNAMIC_ACCENT && android.os.Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else seeded
    // As mi. Browser: the scheme itself is left alone in light mode; only the start screen takes the accent tint
    // (MiUi.homeBackground), so M3 components that draw with `surface` keep their own colour.
    val scheme = if (dark && settings.pureBlack) remember(baseScheme) { baseScheme.pureBlack() } else baseScheme

    val typography = remember(settings.font) {
        AppTypography.withFontFamily(if (settings.font == AppFont.SYSTEM) null else AppFonts.googleSansFlex(context, rounded = settings.font == AppFont.ROUNDED))
    }
    val shapes = remember(settings.cornerRadius) { shapesFor(settings.cornerRadius) }
    val accent = if (settings.accentColor == com.jlees.mibudget.data.DYNAMIC_ACCENT) scheme.primary else Color(com.jlees.mibudget.data.hexColor(settings.accentColor))
    val onAccent = if (settings.accentColor == com.jlees.mibudget.data.DYNAMIC_ACCENT) scheme.onPrimary
        else if (ColorUtils.calculateContrast(Color.Black.toArgb(), accent.toArgb()) >= ColorUtils.calculateContrast(Color.White.toArgb(), accent.toArgb())) Color(0xFF0D0D0F) else Color.White
    val mi = MiUi(settings.cornerRadius, dark, dark && settings.pureBlack, settings.uiPadding, settings.listStyle, settings.fontScale, accent, onAccent)

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = shapes,
        typography = typography,
    ) {
        CompositionLocalProvider(LocalMi provides mi) {
            // Look & Feel › Font Size multiplies every sp size (on top of the phone's own font scale). At 100% the
            // phone's own Density is passed on unchanged, so Android 14's non-linear font scaling keeps working; other
            // sizes are capped at 2×. Always the same provider either way: switching between "provide" and "don't" would
            // rebuild everything below it, losing what's open (the Settings sheet closed when Font Size crossed 100%).
            val scale = settings.fontScale
            val density = LocalDensity.current
            val scaled = remember(density, scale) {
                if (scale == 1f) density else Density(density.density, (density.fontScale * scale).coerceAtMost(2f))
            }
            CompositionLocalProvider(LocalDensity provides scaled, content = content)
        }
    }
}

/**
 * Pure Black: everything that fills the screen is true black (pixels off); cards, menus, dialogs and the Pill stay a
 * step above it, with 4% of the accent, so they still read as surfaces.
 */
private fun ColorScheme.pureBlack(): ColorScheme {
    fun step(v: Int) = androidx.compose.ui.graphics.lerp(Color(0xFF000000.toInt() or (v shl 16) or (v shl 8) or v), primary, 0.04f)
    return copy(
        background = Color.Black, surface = Color.Black, surfaceDim = Color.Black, surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black, surfaceContainer = step(0x12), surfaceContainerHigh = step(0x1C),
        surfaceContainerHighest = step(0x26), surfaceBright = step(0x2E), inverseOnSurface = Color.Black,
    )
}

/**
 * Colours with a meaning, picked for readable contrast (at least 4.5:1 on the app's surfaces) in
 * both light and dark themes. Use these instead of hard-coded greens/ambers so every screen agrees.
 */
object AppColors {
    private val dark: Boolean
        @Composable get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    /** Money coming back (refunds, returns). */
    val refund: Color
        @Composable get() = if (dark) Color(0xFF8BD69A) else Color(0xFF1B6E2B)

    /** "Careful": a budget ahead of pace, a trial about to charge. Text-safe amber. */
    val warning: Color
        @Composable get() = if (dark) Color(0xFFFFC870) else Color(0xFF8A4F00)

    /** Amber for bars and dots (not text), where a brighter colour reads better. */
    val warningGraphic: Color
        @Composable get() = if (dark) Color(0xFFFFB74D) else Color(0xFFF9A825)
}

/**
 * This colour, moved toward [ink] just enough to reach 3:1 contrast against [background] (what large text needs) — so
 * a light category colour can still be used for text.
 */
fun Color.readableOn(background: Color, ink: Color): Color {
    val minContrast = 3.0
    val bg = background.copy(alpha = 1f).toArgb()
    var c = copy(alpha = 1f)
    var t = 0f
    while (t < 1f && ColorUtils.calculateContrast(c.toArgb(), bg) < minContrast) {
        t += 0.1f
        c = androidx.compose.ui.graphics.lerp(copy(alpha = 1f), ink, t.coerceAtMost(1f))
    }
    return c
}
