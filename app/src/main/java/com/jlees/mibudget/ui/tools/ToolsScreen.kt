package com.jlees.mibudget.ui.tools

import com.jlees.mibudget.ui.common.Sym

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** The Tools page (☰ → Tools): handy money calculators that don't touch your budget. */
@Composable
fun ToolsScreen(onCheckSplit: () -> Unit, onTipCalculator: () -> Unit, onCurrency: () -> Unit) {
    com.jlees.mibudget.ui.common.MiScaffold(
        title = "Tools",
    ) { padding ->
        val q = (com.jlees.mibudget.ui.common.LocalSheetQuery.current ?: "").trim()
        val tools = listOf(
            Triple(Sym.CallSplit, "Check Splitter", "Scan the bill and split it evenly, by custom amounts, or item by item, with tip sharing.") to onCheckSplit,
            Triple(Sym.Percent, "Tip Calculator", "Work out the tip and each person's share, with optional rounding up.") to onTipCalculator,
            Triple(Sym.CurrencyExchange, "Currency Converter", "Convert between currencies with daily rates, saved for when you're offline.") to onCurrency,
        ).filter { (t, _) -> q.isEmpty() || t.second.contains(q, ignoreCase = true) || t.third.contains(q, ignoreCase = true) }
        val cs = MaterialTheme.colorScheme
        val colors = listOf(cs.primaryContainer to cs.onPrimaryContainer, cs.secondaryContainer to cs.onSecondaryContainer, cs.tertiaryContainer to cs.onTertiaryContainer)
        // the list scrolls on behind the Pill; the room for it goes at the end
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = padding.calculateTopPadding()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tools.isEmpty()) com.jlees.mibudget.ui.common.EmptyLine(Sym.SearchOff, "No Matches")
            tools.forEachIndexed { i, (t, open) -> ToolCard(t.first, t.second, t.third, colors[i % 3].first, colors[i % 3].second, open) }
            Spacer(Modifier.height(padding.calculateBottomPadding()))
        }
    }
}

@Composable
private fun ToolCard(icon: ImageVector, title: String, text: String, container: Color, onContainer: Color, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = container, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = onContainer, modifier = Modifier.size(28.dp)) }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Sym.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
