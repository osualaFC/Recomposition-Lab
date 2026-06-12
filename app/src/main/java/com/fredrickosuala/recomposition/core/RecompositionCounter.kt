package com.fredrickosuala.recomposition.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Displays a small badge — "[tag]: N" — that increments each time this composable is
 * recomposed. Embed it anywhere you want a live recomposition count in the UI.
 *
 * The count lives in a plain [intArrayOf] (not [androidx.compose.runtime.State]), so
 * reading it during composition never registers a snapshot dependency and the increment in
 * [SideEffect] never self-triggers further recompositions. [SideEffect] runs exactly once
 * after each successfully *committed* recomposition, so the number is precise.
 *
 * **Limitation:** counts committed recompositions only — recompositions that Compose
 * abandons mid-flight (e.g. due to a newer snapshot arriving) are NOT counted. It also
 * does NOT count *skipped* recompositions; a composable whose inputs haven't changed and
 * that Compose decides to skip entirely contributes 0 to this counter.
 *
 * @param tag A short label (e.g. the composable's name) shown in the badge.
 */
@Composable
fun RecompositionCounter(tag: String, modifier: Modifier = Modifier) {
    val count = remember { intArrayOf(0) }
    // ✅ FIX: SideEffect increments the plain-array holder AFTER each committed
    //         recomposition so the increment itself never causes another one.
    SideEffect { count[0]++ }
    Text(
        text = "$tag: ${count[0]}",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * Returns an integer that increments by 1 each time the calling composable recomposes.
 *
 * Uses a plain [intArrayOf] (not [androidx.compose.runtime.State]) so incrementing it
 * does not self-trigger further recompositions. Useful when you want the raw count as a
 * value rather than a rendered badge.
 */
@Composable
fun rememberRecompositionCount(): Int {
    val count = remember { intArrayOf(0) }
    return ++count[0]
}
