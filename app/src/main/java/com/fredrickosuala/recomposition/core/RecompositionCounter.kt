package com.fredrickosuala.recomposition.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Returns an integer that increments by 1 each time the calling composable recomposes.
 * Uses a plain array (not State) so incrementing it doesn't self-trigger further recompositions.
 */
@Composable
fun rememberRecompositionCount(): Int {
    val count = remember { intArrayOf(0) }
    return ++count[0]
}
