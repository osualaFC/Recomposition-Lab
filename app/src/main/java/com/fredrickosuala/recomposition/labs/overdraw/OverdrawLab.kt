package com.fredrickosuala.recomposition.labs.overdraw

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Lab entry point: renders the naive or optimized variant from the [optimized] flag.
 *
 * **Naive:** four opaque `background()` modifiers are stacked on top of each other.
 * The GPU draws each layer in full — but only the topmost layer is ever visible.
 * The three hidden layers represent pure wasted GPU fill-rate.
 *
 * **Optimized:** a single `background()` modifier produces exactly the same visual result.
 * Each pixel in the region is drawn exactly once.
 *
 * Enable "Debug GPU Overdraw" in Developer Options to see the overdraw heat-map on a
 * real device. The naive variant will show red/pink regions; the optimized variant shows
 * the true display color (no overdraw indicator).
 */
@Composable
fun OverdrawLab(optimized: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Each stacked opaque background redraws the same pixels — wasted GPU work. " +
                "Naive: 4 background layers (3 hidden). Optimized: 1 layer, identical result.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        if (optimized) {
            OptimizedBackground()
        } else {
            NaiveBackground()
        }

        Spacer(Modifier.height(16.dp))
        OverdrawColorGuide()
    }
}

// ─── Naive variant ────────────────────────────────────────────────────────────

@Composable
private fun NaiveBackground() {
    // ⚠️ PROBLEM: 4 opaque backgrounds stacked. Layers 1–3 are painted and immediately
    // occluded by the layer above — the GPU fills those pixels for nothing.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE53935)), // layer 1 — completely hidden
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFF9800)), // layer 2 — completely hidden
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF4CAF50)), // layer 3 — completely hidden
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer), // layer 4 — visible
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "4 backgrounds drawn\n(3 are completely hidden)",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    LayerBadge(layers = 4)
}

// ─── Optimized variant ────────────────────────────────────────────────────────

@Composable
private fun OptimizedBackground() {
    // ✅ FIX: single background — the GPU touches each pixel in this region exactly once.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "1 background drawn",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    LayerBadge(layers = 1)
}

// ─── Shared UI ────────────────────────────────────────────────────────────────

@Composable
private fun LayerBadge(layers: Int, modifier: Modifier = Modifier) {
    Spacer(Modifier.height(8.dp))
    val color = when (layers) {
        1 -> MaterialTheme.colorScheme.primary
        2 -> Color(0xFF388E3C)
        3 -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.error
    }
    Text(
        text = "Background layers drawn: $layers",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(color = color, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ─── Debug GPU Overdraw guide ─────────────────────────────────────────────────

private data class OverdrawLevel(
    val label: String,
    val swatch: Color,
    val description: String,
)

private val OVERDRAW_LEVELS = listOf(
    OverdrawLevel("True color", Color(0xFFDDDDDD), "No overdraw — 1 draw call (ideal)"),
    OverdrawLevel("Blue", Color(0xFF90CAF9), "1× overdraw — drawn twice"),
    OverdrawLevel("Green", Color(0xFF66BB6A), "2× overdraw — drawn three times"),
    OverdrawLevel("Pink", Color(0xFFEF9A9A), "3× overdraw — drawn four times (investigate)"),
    OverdrawLevel("Red", Color(0xFFB71C1C), "4×+ overdraw — drawn 5+ times (fix immediately)"),
)

@Composable
private fun OverdrawColorGuide() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🎨 Debug GPU Overdraw colors",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Enable in Developer Options → Debug GPU overdraw → Show overdraw areas.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            OVERDRAW_LEVELS.forEach { level ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(level.swatch, RoundedCornerShape(4.dp))
                            .border(
                                width = 1.dp,
                                color = Color.Black.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = level.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = level.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
