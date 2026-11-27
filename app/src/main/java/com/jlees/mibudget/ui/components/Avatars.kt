package com.jlees.mibudget.ui.components

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jlees.mibudget.data.db.CategoryEntity
import com.jlees.mibudget.icons.Brand
import com.jlees.mibudget.icons.BrandCatalog
import com.jlees.mibudget.icons.BrandIcon
import com.jlees.mibudget.icons.IconRef
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import java.io.File

val LocalBrandCatalog = staticCompositionLocalOf<BrandCatalog> { error("BrandCatalog not provided") }

/** Expressive "cookie" shape used for category badges. */
@Composable
fun cookieShape(): Shape = MaterialShapes.Cookie9Sided.toShape()


fun contentOn(bg: Color): Color = if (bg.luminance() > 0.6f) Color(0xFF1C1B1F) else Color.White

/**
 * Icon for a purchase or subscription, following its icon setting ([IconRef]):
 * auto-detected brand logo → your chosen brand / text / picture → category icon → store symbol.
 * Never falls back to a single letter.
 */
/** Shared renderer for every icon setting (merchants, subscriptions and payment methods). */
@Composable
fun ItemIcon(
    ref: String?,
    name: String,
    category: CategoryEntity?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    contentDescription: String? = null,
) {
    val catalog = LocalBrandCatalog.current
    val context = LocalContext.current
    val parsed = remember(ref) { IconRef.parse(ref) }
    val brand = remember(ref, name) { catalog.brandFor(ref, name) }
    when {
        brand != null -> BrandAvatar(brand, modifier, size)
        parsed is IconRef.Text -> TextBadge(parsed.text, Color(parsed.color), modifier, size)
        parsed is IconRef.Image -> AsyncImage(
            model = File(File(context.filesDir, "icons"), parsed.file),
            contentDescription = contentDescription ?: name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
        parsed is IconRef.Generic -> SymbolBadge(PaymentGenericIcons.get(parsed.key), modifier, size, contentDescription)
        category != null -> CategoryBadge(category, modifier, size)
        else -> SymbolBadge(Sym.Storefront, modifier, size, contentDescription ?: name)
    }
}

@Composable
fun BrandAvatar(brand: Brand, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val catalog = LocalBrandCatalog.current
    val icon = remember(brand.id) { catalog.icon(brand) }
    val density = LocalDensity.current
    when (icon) {
        is BrandIcon.Svg -> if (!icon.mono) {
            // Full-color logos are designed for a light background.
            val px = with(density) { (size * 0.62f).roundToPx() }
            val bmp = remember(brand.id, px) { catalog.svgBitmap(brand, icon.svg, px) }
            Box(
                modifier.size(size).clip(CircleShape).background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (bmp != null) Image(bmp, brand.name, Modifier.size(size * 0.62f))
            }
            return
        }
        else -> Unit
    }
    val bg = Color(brand.color)
    val tint = contentOn(bg)
    Box(modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        when (icon) {
            is BrandIcon.Glyph -> Icon(icon.vector, contentDescription = brand.name, tint = tint, modifier = Modifier.size(size * 0.55f))
            is BrandIcon.Svg -> {
                val px = with(density) { (size * 0.6f).roundToPx() }
                val bmp = remember(brand.id, px) { catalog.svgBitmap(brand, icon.svg, px) }
                if (bmp != null) Image(bmp, brand.name, Modifier.size(size * 0.6f), colorFilter = ColorFilter.tint(tint))
            }
            null -> Icon(Sym.Storefront, brand.name, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** Custom 1–3 letters on a color — only ever shown when you choose it. */
@Composable
fun TextBadge(text: String, bg: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val t = text.trim().take(IconRef.MAX_TEXT)
    Box(modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text(
            t,
            color = contentOn(bg),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * when (t.length) { 1 -> 0.46f; 2 -> 0.38f; else -> 0.3f }).sp,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
fun SymbolBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 44.dp, contentDescription: String? = null) {
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun CategoryBadge(category: CategoryEntity, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val color = Color(category.color)
    Box(
        modifier.size(size).clip(cookieShape()).background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            CategoryIcons.get(category.icon),
            contentDescription = category.name,
            tint = if (MaterialTheme.colorScheme.background.luminance() < 0.3f) color.copy(alpha = 1f).lighten() else color,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Neutral badge for "Uncategorized". */
@Composable
fun UncategorizedBadge(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(cookieShape()).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Sym.QuestionMark,
            contentDescription = "Uncategorized",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

private fun Color.lighten(): Color = Color(
    red = red + (1f - red) * 0.35f,
    green = green + (1f - green) * 0.35f,
    blue = blue + (1f - blue) * 0.35f,
    alpha = alpha,
)
