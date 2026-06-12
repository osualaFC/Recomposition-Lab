package com.fredrickosuala.recomposition.labs.strongskipping

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

/**
 * Unstable wrapper around a lambda. Because this class has no @Stable annotation and its
 * field type is a function, Compose cannot infer stability — it is treated as unstable.
 *
 * Strong skipping automatically remembers plain `() -> Unit` parameters, but it does NOT
 * auto-remember parameters of unstable *class* types. Wrapping a callback here bypasses
 * that benefit and forces the child to recompose whenever the parent does.
 */
// ⚠️ PROBLEM: An unstable class wrapper around a callback defeats strong skipping's
//             automatic lambda-remember, causing the child to recompose needlessly.
class ActionHandler(val onAction: () -> Unit)

/**
 * Entry composable for the "Strong skipping mode" lab.
 *
 * Strong skipping (on by default since Compose compiler 1.5.5) makes two guarantees:
 *   1. Composables with unstable parameters are still skippable if the runtime equality
 *      check passes (reference equality for unstable types).
 *   2. Unstable *lambda* parameters (`() -> Unit`, etc.) are automatically wrapped in
 *      `remember`, so the child sees the same lambda reference across parent recompositions
 *      and skips.
 *
 * This lab shows that guarantee (2) only applies to raw lambda params — wrapping the
 * lambda in an unstable class breaks it.
 *
 * Note: to observe how recompositions change without strong skipping, add
 *   `featureFlags = setOf(ComposeFeatureFlag.StrongSkipping.disabled())`
 * inside the `composeCompiler {}` block in app/build.gradle.kts and rebuild.
 */
@Composable
fun StrongSkippingLab(optimized: Boolean) {
    var parentRecomposes by remember { mutableStateOf(0) }
    var actionCount by remember { mutableStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "Tap 'Recompose parent' and watch the child counter. " +
                "The action button's behavior is unchanged in both variants.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Button(
                onClick = { parentRecomposes++ },
                modifier = Modifier.weight(1f),
            ) {
                Text("Recompose parent ($parentRecomposes)")
            }
        }

        Text(
            text = "Actions performed: $actionCount",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (optimized) {
            // ✅ FIX: Pass the lambda directly as () -> Unit. Strong skipping auto-wraps it in
            //         remember, so the child receives the same reference every recomposition → skips.
            OptimizedActionCard(onClick = { actionCount++ })
        } else {
            // ⚠️ PROBLEM: ActionHandler is unstable. A new instance is created each parent
            //             recomposition; reference equality fails → child always recomposes.
            NaiveActionCard(handler = ActionHandler { actionCount++ })
        }
    }
}

@Composable
private fun NaiveActionCard(handler: ActionHandler) {
    // ⚠️ PROBLEM: ActionHandler is unstable + new instance each recompose → not skippable.
    LogRecomposition("NaiveActionCard")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recomposeHighlight(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            RecompositionCounter("NaiveActionCard")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = handler.onAction) {
                Text("Do action")
            }
        }
    }
}

@Composable
private fun OptimizedActionCard(onClick: () -> Unit) {
    // ✅ FIX: () -> Unit param → strong skipping auto-remembers → same reference → skippable.
    LogRecomposition("OptimizedActionCard")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recomposeHighlight(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            RecompositionCounter("OptimizedActionCard")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClick) {
                Text("Do action")
            }
        }
    }
}
