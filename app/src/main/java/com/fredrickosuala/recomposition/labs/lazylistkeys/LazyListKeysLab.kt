package com.fredrickosuala.recomposition.labs.lazylistkeys

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fredrickosuala.recomposition.core.RecompositionCounter
import com.fredrickosuala.recomposition.core.recomposeHighlight

@Immutable
data class NoteItem(val id: Int, val title: String)

private val INITIAL_NOTES = List(10) { NoteItem(id = it, title = "Note ${it + 1}") }

/**
 * Entry composable for the "Lazy list keys" lab.
 *
 * A 10-item list lets you promote any note to "most recently edited" (top of list).
 *
 * **Naive:** no `key =` → positional identity. Moving the item at position N to position 0
 * means positions 0..N all receive different data → all those rows recompose, even though
 * none of their data actually changed.
 *
 * **Optimized:** `key = { it.id }` → identity-based matching. Moving an item only
 * relocates its existing composable; no data changed → zero recompositions. `animateItem()`
 * also becomes available, giving smooth reorder animations at no extra cost.
 * `contentType` groups structurally-identical items so entering items can reuse the slot
 * of an exiting item — critical for heterogeneous lists (e.g. headers + rows).
 */
@Composable
fun LazyListKeysLab(optimized: Boolean) {
    var notes by remember { mutableStateOf(INITIAL_NOTES) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Tap 'Edit' on any note to promote it to the top. " +
                "Naive: all shifted rows recompose (flash). " +
                "Optimized: no rows recompose — the item slides smoothly.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OutlinedButton(
            onClick = { notes = INITIAL_NOTES },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset list")
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(520.dp)) {
            if (optimized) {
                OptimizedNoteList(
                    notes = notes,
                    onEditNote = { promoted ->
                        notes = listOf(promoted) + notes.filter { it.id != promoted.id }
                    },
                )
            } else {
                NaiveNoteList(
                    notes = notes,
                    onEditNote = { promoted ->
                        notes = listOf(promoted) + notes.filter { it.id != promoted.id }
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NaiveNoteList(
    notes: List<NoteItem>,
    onEditNote: (NoteItem) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // ⚠️ PROBLEM: no key → Compose uses positional identity. Moving Note at position N
        // to position 0 causes rows at positions 0..N to receive different NoteItem data
        // → all of them recompose, even though no note's content changed.
        items(notes) { note ->
            NoteRow(note = note, onEdit = { onEditNote(note) })
        }
    }
}

@Composable
private fun OptimizedNoteList(
    notes: List<NoteItem>,
    onEditNote: (NoteItem) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = notes,
            key = { it.id },          // ✅ FIX: stable per-item identity; moving reorders without recomposing
            contentType = { "note" }, // ✅ FIX: groups items by type for slot reuse in heterogeneous lists
        ) { note ->
            NoteRow(
                note = note,
                onEdit = { onEditNote(note) },
                modifier = Modifier.animateItem(), // smooth animation — only available because key is set
            )
        }
    }
}

@Composable
private fun NoteRow(
    note: NoteItem,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .recomposeHighlight(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            RecompositionCounter("Note ${note.id}")
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Button(onClick = onEdit) {
                Text("Edit")
            }
        }
    }
}
