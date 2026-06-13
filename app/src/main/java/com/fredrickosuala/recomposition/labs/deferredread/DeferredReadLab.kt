package com.fredrickosuala.recomposition.labs.deferredread

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.core.LogRecomposition
import com.fredrickosuala.recomposition.core.RecompositionCounter
import com.fredrickosuala.recomposition.core.recomposeHighlight

/**
 * Entry composable for the "Deferred state read (lambda parameter)" lab.
 *
 * A scrollable list drives a parallax header that floats above it. The header
 * needs the scroll offset — how that offset is consumed determines whether
 * composition runs on every pixel of scroll.
 *
 * **Naive:** `scrollState.value` is read in the composable body → every pixel of scroll
 * creates a state change that invalidates the composition scope → the function recomposes
 * hundreds of times per second while scrolling.
 *
 * **Optimized (step 1):** pass `() -> Int` so only the child reads the value.
 * The parent scope is no longer in the observation set → parent stays flat.
 * The child composable that calls `scrollProvider()` in its body still recomposes.
 *
 * **Optimized (step 2 — shown here):** move the read into the LAYOUT phase with
 * `Modifier.offset { IntOffset(0, scrollState.value) }`. Neither the parent nor the
 * child recomposes — only the layout pass re-runs. The counter goes FLAT while the
 * header still moves.
 */
@Composable
fun DeferredReadLab(optimized: Boolean) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "Scroll the list below and watch the recomposition counter. " +
                "The header moves the same way in both variants.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))

        if (optimized) {
            // ✅ FIX: scrollState.value is NOT read in this scope. Reading is deferred to
            //         the layout-phase lambda inside Modifier.offset { }.
            OptimizedScrollDemo(scrollState)
        } else {
            // ⚠️ PROBLEM: NaiveScrollDemo reads scrollState.value in its body, making it
            //             the recompose scope for every scroll-pixel state change.
            NaiveScrollDemo(scrollState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NaiveScrollDemo(scrollState: ScrollState) {
    // ⚠️ PROBLEM: reading scrollState.value here registers this composable as a snapshot
    //             observer. Every pixel of scroll emits a new value → this function and
    //             its entire subtree are scheduled for recomposition on every scroll event.
    val scrollOffset = scrollState.value

    LogRecomposition("NaiveScrollDemo")

    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        // Scrollable list — same in both variants.
        ScrollableListContent(scrollState)

        // ⚠️ PROBLEM: the offset is applied as a plain Dp value calculated from a
        //             composition-scope read → triggers recomposition to update position.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .offset(y = -(scrollOffset / 4).dp)
                .recomposeHighlight(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                RecompositionCounter("NaiveScrollDemo")
                Text(
                    text = "↕ Parallax header — scroll the list",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Scroll offset: $scrollOffset px",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptimizedScrollDemo(scrollState: ScrollState) {
    // ✅ FIX: scrollState.value is NOT read here. The composable body has no snapshot
    //         observation for the scroll position → it never recomposes during scroll.

    LogRecomposition("OptimizedScrollDemo")

    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        ScrollableListContent(scrollState)

        // ✅ FIX: Modifier.offset { } takes a lambda that runs during the LAYOUT phase.
        //         scrollState.value is read inside that lambda → the snapshot observation
        //         is scoped to layout, not composition. Only the layout pass is re-run
        //         when the scroll position changes; composition never runs.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .offset { IntOffset(0, -(scrollState.value / 4)) }
                .recomposeHighlight(),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                RecompositionCounter("OptimizedScrollDemo")
                Text(
                    text = "↕ Parallax header — scroll the list",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "Counter stays flat — UI still moves!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

/** Shared scrollable list used by both variants. */
@Composable
private fun ScrollableListContent(scrollState: ScrollState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(top = 80.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
    ) {
        repeat(25) { index ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "List item ${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
