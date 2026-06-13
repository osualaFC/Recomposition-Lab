package com.fredrickosuala.recomposition.labs.backwardswrite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.core.LogRecomposition
import com.fredrickosuala.recomposition.core.RecompositionCounter
import com.fredrickosuala.recomposition.core.recomposeHighlight

/**
 * Maximum recompositions the naive backwards-write guard allows before stopping.
 *
 * Without this cap the write → recompose cycle loops until Compose throws
 * an "infinite recomposition" exception. The cap keeps the demo usable while
 * still showing the counter explode on every render of the naive variant.
 */
private const val BACKWARDS_WRITE_CAP = 20

/**
 * Entry composable for the "Backwards write (infinite recomposition)" lab.
 *
 * **Naive:** a state variable is mutated *inside the composable body* (not in an event
 * lambda). Writing state during composition immediately schedules another recomposition,
 * which runs the body again, which writes state again — an infinite loop. A guard
 * (`counter < CAP`) breaks the cycle after [BACKWARDS_WRITE_CAP] iterations so the
 * demo app remains usable, but the counter clearly shows the explosion.
 *
 * **Why does it loop?** Compose's snapshot system schedules recomposition whenever a
 * `MutableState` is written. If the write happens *during* composition, Compose queues
 * a fresh recomposition of that scope immediately after the current one commits. The
 * new recomposition writes state again → queues another → and so on.
 *
 * **Optimized:** state is mutated only inside `onClick` (an event lambda). Event lambdas
 * run *after* composition, outside the snapshot observation window. One tap → one state
 * change → one recomposition → stable.
 *
 * Note: unlike the other labs, the naive variant deliberately shows incorrect behaviour
 * (a bug), not just a performance issue. The visible output intentionally differs.
 */
@Composable
fun BackwardsWriteLab(optimized: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = if (optimized) {
                "Each button tap triggers exactly one recomposition."
            } else {
                "On each render the counter is incremented inside the composable body, " +
                    "triggering another recomposition immediately. The loop is capped at " +
                    "$BACKWARDS_WRITE_CAP to prevent a crash."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))

        if (optimized) {
            OptimizedWrite()
        } else {
            NaiveWrite()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NaiveWrite() {
    var counter by remember { mutableIntStateOf(0) }

    // ⚠️ PROBLEM: writing state DURING composition.
    // Write → schedule recompose → body runs again → write → schedule recompose → …
    // The guard `counter < CAP` breaks the cycle so the demo app stays usable.
    if (counter < BACKWARDS_WRITE_CAP) {
        counter++ // ⚠️ PROBLEM: state mutation in the composable body, not in an event lambda.
    }

    LogRecomposition("NaiveWrite")
    Card(
        modifier = Modifier.fillMaxWidth().recomposeHighlight(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            RecompositionCounter("NaiveWrite")
            Text(
                text = "Counter: $counter",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = if (counter >= BACKWARDS_WRITE_CAP) {
                    "Loop stopped by guard after $BACKWARDS_WRITE_CAP rewrites"
                } else {
                    "Looping…"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedButton(onClick = { counter = 0 }) {
                Text("Reset (re-triggers the loop)")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptimizedWrite() {
    var counter by remember { mutableIntStateOf(0) }

    // ✅ FIX: no state mutation in the composable body. Mutations only happen in onClick,
    //         which runs after composition. One event → one state change → one recomposition.
    LogRecomposition("OptimizedWrite")
    Card(
        modifier = Modifier.fillMaxWidth().recomposeHighlight(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            RecompositionCounter("OptimizedWrite")
            Text(
                text = "Counter: $counter",
                style = MaterialTheme.typography.headlineMedium,
            )
            Button(
                // ✅ FIX: state mutation is inside an event lambda — outside composition.
                onClick = { counter++ },
            ) {
                Text("Tap to increment")
            }
            OutlinedButton(onClick = { counter = 0 }) {
                Text("Reset")
            }
        }
    }
}
