package com.fredrickosuala.recomposition.labs.unstablecollections

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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ListItem(val id: Int, val label: String)

/**
 * Entry composable for the "Unstable collections" lab.
 *
 * Both variants render the same three-item list. Each parent recomposition re-creates
 * the list with identical contents. Whether the child skips depends on whether
 * Compose can trust the collection type's equals() implementation.
 *
 * - `List<T>` is a plain JVM interface — Compose treats it as unstable and falls back
 *   to reference equality. New list object → new reference → child recomposes.
 * - `ImmutableList<T>` from kotlinx.collections.immutable is annotated @Immutable.
 *   Compose uses equals() — same contents → child skips.
 */
@Composable
fun UnstableCollectionsLab(optimized: Boolean) {
    var counter by remember { mutableStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "The list never changes. Tap the button to trigger a parent " +
                "recomposition and watch the child's counter.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Button(onClick = { counter++ }) {
            Text("Recompose parent ($counter)")
        }

        if (optimized) {
            // ✅ FIX: ImmutableList is @Immutable — Compose uses equals(), same contents → skips.
            StableItemList(
                items = persistentListOf(
                    ListItem(1, "Compose"),
                    ListItem(2, "Kotlin"),
                    ListItem(3, "Android"),
                ),
            )
        } else {
            // ⚠️ PROBLEM: listOf() returns List<T> — unstable interface — Compose uses reference
            //             equality. New list object each recomposition → child always recomposes.
            UnstableItemList(
                items = listOf(
                    ListItem(1, "Compose"),
                    ListItem(2, "Kotlin"),
                    ListItem(3, "Android"),
                ),
            )
        }
    }
}

@Composable
private fun UnstableItemList(
    // ⚠️ PROBLEM: List<T> is treated as unstable → this composable is not skippable.
    items: List<ListItem>,
) {
    LogRecomposition("UnstableItemList")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recomposeHighlight(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RecompositionCounter("UnstableItemList")
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                Text("• ${item.label}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun StableItemList(
    // ✅ FIX: ImmutableList<T> is @Immutable → Compose uses equals() → skippable.
    items: ImmutableList<ListItem>,
) {
    LogRecomposition("StableItemList")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .recomposeHighlight(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            RecompositionCounter("StableItemList")
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                Text("• ${item.label}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
