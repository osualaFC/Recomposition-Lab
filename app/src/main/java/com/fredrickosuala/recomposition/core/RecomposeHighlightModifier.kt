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
import kotlinx.coroutines.delay

/**
 * Flashes a red overlay on the composable whenever it recomposes, then fades out.
 *
 * Uses a plain int array (not State) to count recompositions so the count increment
 * never self-triggers additional recompositions. The alpha State is only read inside
 * drawBehind (draw phase), so LaunchedEffect writes to it cause a redraw, not a
 * full recomposition.
 */
@Composable
fun Modifier.recomposeHighlighter(): Modifier {
    val count = remember { intArrayOf(0) }
    count[0]++
    val snapshot = count[0]

    var alpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(snapshot) {
        alpha = 0.5f
        delay(300L)
        alpha = 0f
    }

    return this then Modifier.drawBehind {
        if (alpha > 0f) drawRect(Color.Red.copy(alpha = alpha))
    }
}
