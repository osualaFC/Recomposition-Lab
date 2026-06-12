package com.fredrickosuala.recomposition.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay

// A palette of visually distinct colors that cycle on each recomposition.
private val highlightColors = listOf(
    Color(0xFFE53935), // red
    Color(0xFF8E24AA), // purple
    Color(0xFF1E88E5), // blue
    Color(0xFF00ACC1), // cyan
    Color(0xFF43A047), // green
    Color(0xFFFB8C00), // orange
    Color(0xFFE91E63), // pink
    Color(0xFF00897B), // teal
)

/**
 * Draws a colored border that flashes a **new color** on every recomposition, making
 * recompositions "ripple" visibly through the UI tree.
 *
 * How it avoids adding recompositions of its own:
 * - The recomposition count (`count[0]`) and current color (`colorHolder[0]`) are stored in
 *   plain arrays — not [androidx.compose.runtime.State] — so writing to them during
 *   composition does not register snapshot writes and cannot self-trigger further
 *   recompositions.
 * - [alpha] IS [androidx.compose.runtime.State], but it is read only inside [drawBehind]
 *   (the draw phase). Compose tracks that read in the draw layer's snapshot scope, so a
 *   change to [alpha] invalidates only drawing — never composition.
 *
 * **Limitation:** a composable that Compose *skips* entirely (inputs unchanged) will not
 * flash, because its body never runs. For a complete picture use the Layout Inspector's
 * "Recomposition counts" overlay.
 *
 * @param enabled Pass `false` to disable the highlight without removing the call site.
 */
@Composable
fun Modifier.recomposeHighlight(enabled: Boolean = true): Modifier {
    // Plain arrays — not State — so writes here never trigger recomposition.
    val count = remember { intArrayOf(0) }
    val colorHolder = remember { arrayOf(highlightColors[0]) }
    // ⚠️ PROBLEM (if read in composition): mutableStateOf causes recomposition on write.
    // ✅ FIX: alpha is read ONLY inside drawBehind (draw phase), so writes only invalidate
    //         drawing, not composition.
    var alpha by remember { mutableFloatStateOf(0f) }

    if (enabled) {
        count[0]++
        // Advance to the next color each recomposition.
        colorHolder[0] = highlightColors[count[0] % highlightColors.size]
        // Each new count value cancels the previous coroutine and starts a fresh flash.
        LaunchedEffect(count[0]) {
            alpha = 0.8f
            delay(300L)
            alpha = 0f
        }
    }

    return this then Modifier.drawBehind {
        // colorHolder[0] is not State, so this read is untracked — that is intentional.
        // It already holds the correct new color because composition (which set it) always
        // runs before the draw phase.
        if (alpha > 0f) {
            drawRect(
                color = colorHolder[0].copy(alpha = alpha),
                style = Stroke(width = 6f),
            )
        }
    }
}
