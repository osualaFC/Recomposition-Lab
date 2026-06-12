package com.fredrickosuala.recomposition.labs.unstableclass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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

// ⚠️ PROBLEM: `var` field makes Compose infer this class as unstable. The compiler
//             cannot prove the value won't change between reads, so any composable
//             that accepts this type as a parameter is marked unskippable.
data class UnstableProfile(var name: String, var score: Int)

// ✅ FIX: @Immutable promises all publicly accessible state is deeply immutable.
//         Compose trusts equals() to compare values, making the receiving composable skippable.
@Immutable
data class StableProfile(val name: String, val score: Int)

/**
 * Entry composable for the "Unstable class parameter" lab.
 *
 * Both variants display an identical profile card. The only difference is whether
 * the compiler can skip the child when the parent recomposes with the same data.
 *
 * Both profiles are re-created on every parent recomposition with identical field values.
 * With an unstable type, Compose falls back to reference equality (new object ≠ old object →
 * recompose). With a stable type, Compose uses equals() (same data → skip).
 */
@Composable
fun UnstableClassLab(optimized: Boolean) {
    var counter by remember { mutableStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "The profile data never changes. Tap the button to trigger " +
                "a parent recomposition and watch the child's counter.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(onClick = { counter++ }) {
            Text("Recompose parent ($counter)")
        }

        if (optimized) {
            // ✅ FIX: StableProfile is @Immutable — Compose uses equals(), sees equal data, skips.
            StableProfileCard(profile = StableProfile("Alice", 42))
        } else {
            // ⚠️ PROBLEM: UnstableProfile has a var field — Compose uses reference equality,
            //             sees a new object each time, and recomposes the child needlessly.
            UnstableProfileCard(profile = UnstableProfile("Alice", 42))
        }
    }
}

@Composable
private fun UnstableProfileCard(profile: UnstableProfile) {
    // ⚠️ PROBLEM: UnstableProfile is unstable → this composable is not skippable.
    LogRecomposition("UnstableProfileCard")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recomposeHighlight(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RecompositionCounter("UnstableProfileCard")
            Spacer(Modifier.height(8.dp))
            Text("Name: ${profile.name}", style = MaterialTheme.typography.bodyLarge)
            Text("Score: ${profile.score}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StableProfileCard(profile: StableProfile) {
    // ✅ FIX: StableProfile is @Immutable → this composable is skippable.
    LogRecomposition("StableProfileCard")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recomposeHighlight(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RecompositionCounter("StableProfileCard")
            Spacer(Modifier.height(8.dp))
            Text("Name: ${profile.name}", style = MaterialTheme.typography.bodyLarge)
            Text("Score: ${profile.score}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
