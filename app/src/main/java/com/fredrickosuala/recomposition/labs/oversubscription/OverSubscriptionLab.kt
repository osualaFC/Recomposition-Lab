package com.fredrickosuala.recomposition.labs.oversubscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.core.LogRecomposition
import com.fredrickosuala.recomposition.core.RecompositionCounter
import com.fredrickosuala.recomposition.core.recomposeHighlight

@Immutable
data class DashboardState(val tickCount: Int, val label: String)

/**
 * Entry composable for the "Over-subscription to state" lab.
 *
 * The child composable only ever *displays* `tickCount`. When `label` changes, the
 * child's output is visually unchanged — yet in the naive variant it still recomposes,
 * because it subscribes to the entire `DashboardState` object.
 *
 * Tap "Toggle label" and watch the naive child's counter increment even though its
 * displayed value stays the same.
 */
@Composable
fun OverSubscriptionLab(optimized: Boolean) {
    var tickCount by remember { mutableStateOf(0) }
    var label by remember { mutableStateOf("Hello") }

    // Assembled here so the naive child can take the full object.
    val state = DashboardState(tickCount, label)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "The child only displays tick count. Toggle the label and watch " +
                "whether the child recomposes even though tick count didn't change.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { tickCount++ },
                modifier = Modifier.weight(1f),
            ) {
                Text("Tick ($tickCount)")
            }
            OutlinedButton(
                onClick = { label = if (label == "Hello") "World" else "Hello" },
                modifier = Modifier.weight(1f),
            ) {
                Text("Label: $label")
            }
        }

        if (optimized) {
            // ✅ FIX: Pass only the specific field the child needs. When label changes,
            //         tickCount is unchanged → Int equality passes → child skips.
            OptimizedTickDisplay(tickCount = state.tickCount)
        } else {
            // ⚠️ PROBLEM: Child takes the whole DashboardState. When label changes,
            //             state != oldState (equals fails) → child recomposes needlessly.
            NaiveTickDisplay(state = state)
        }
    }
}

@Composable
private fun NaiveTickDisplay(
    // ⚠️ PROBLEM: Takes the full object — any field change recomposes this composable,
    //             even fields it never reads.
    state: DashboardState,
) {
    LogRecomposition("NaiveTickDisplay")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recomposeHighlight(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            RecompositionCounter("NaiveTickDisplay")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tick count: ${state.tickCount}",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun OptimizedTickDisplay(
    // ✅ FIX: Takes only what it needs. Label changes don't affect this parameter → child skips.
    tickCount: Int,
) {
    LogRecomposition("OptimizedTickDisplay")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recomposeHighlight(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            RecompositionCounter("OptimizedTickDisplay")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tick count: $tickCount",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
