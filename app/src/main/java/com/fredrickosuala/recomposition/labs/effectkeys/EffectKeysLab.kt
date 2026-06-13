package com.fredrickosuala.recomposition.labs.effectkeys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A plain class — no `data` modifier means no generated `equals`/`hashCode`.
 * Two instances with identical field values are NOT equal (reference equality only).
 * In the naive variant this is constructed fresh on every recomposition, making every
 * new instance a brand-new key from LaunchedEffect's perspective.
 */
private class FeedConfig(val userId: String, val pageSize: Int)

/**
 * Lab entry point: renders the naive or optimized variant from the [optimized] flag.
 *
 * **Naive:** `FeedConfig` is a plain class (reference equality). A new instance is created
 * in the composable body on every recomposition. `LaunchedEffect(config)` sees a new key
 * each time → cancels the in-flight coroutine → restarts. Every unrelated state change
 * that causes a recomposition also restarts the effect.
 *
 * **Optimized:** the key is `userId`, a `String` constant. Its value never changes, so
 * `LaunchedEffect(userId)` runs exactly once regardless of how many times the parent
 * recomposes.
 */
@Composable
fun EffectKeysLab(optimized: Boolean) {
    var unrelatedCounter by remember { mutableStateOf(0) }
    val effectRunCount = remember { intArrayOf(0) }
    var statusMessage by remember { mutableStateOf("Waiting for first run…") }

    if (optimized) {
        val userId = "user-42"
        // ✅ FIX: key on the minimal, stable value. userId is a String constant — it never
        // changes reference or value, so the effect is launched once and stays running.
        LaunchedEffect(userId) {
            effectRunCount[0]++
            statusMessage = "Run #${effectRunCount[0]} — fetching feed for $userId…"
            delay(600)
            statusMessage = "Run #${effectRunCount[0]} complete for $userId ✓"
        }
    } else {
        // ⚠️ PROBLEM: FeedConfig is a plain class with reference equality. A new instance
        // is created on every recomposition — even when userId/pageSize haven't changed.
        // LaunchedEffect sees a different object → considers the key changed → restarts.
        val config = FeedConfig(userId = "user-42", pageSize = 20)
        LaunchedEffect(config) {
            effectRunCount[0]++
            statusMessage = "Run #${effectRunCount[0]} — fetching feed for ${config.userId}…"
            delay(600)
            statusMessage = "Run #${effectRunCount[0]} complete for ${config.userId} ✓"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Press 'Recompose' to trigger an unrelated state change. " +
                "Naive: effect restarts on every recompose. " +
                "Optimized: effect runs exactly once.",
            style = MaterialTheme.typography.bodyMedium,
        )

        EffectCountBadge(count = effectRunCount[0])

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { unrelatedCounter++ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Recompose (unrelated counter: $unrelatedCounter)")
        }
    }
}

@Composable
private fun EffectCountBadge(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = "Effect started: ${count}×",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(
                // Red badge signals a problem: more than one start means restarts are happening.
                color = if (count > 1) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
