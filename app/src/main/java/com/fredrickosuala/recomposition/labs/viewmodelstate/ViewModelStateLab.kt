package com.fredrickosuala.recomposition.labs.viewmodelstate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fredrickosuala.recomposition.core.RecompositionCounter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ─── Data model ───────────────────────────────────────────────────────────────

data class ListItem(val id: Int, val label: String)

// ─── Naive ViewModel ──────────────────────────────────────────────────────────

class NaiveStateViewModel : ViewModel() {
    // ⚠️ PROBLEM: StateFlow<List<T>> exposes List<T>, a mutable JVM interface.
    // The Compose compiler infers List<T> as unstable — any composable that receives
    // it as a parameter cannot skip, even when the list content is unchanged.
    private val _items = MutableStateFlow<List<ListItem>>(emptyList())
    val items: StateFlow<List<ListItem>> = _items.asStateFlow()

    fun addItem() {
        val next = _items.value.size + 1
        _items.value = _items.value + ListItem(next, "Item #$next")
    }
}

// ─── Optimized ViewModel ──────────────────────────────────────────────────────

data class ItemsUiState(val items: ImmutableList<ListItem> = persistentListOf())

class OptimizedStateViewModel : ViewModel() {
    // ✅ FIX: StateFlow<UiState> where UiState wraps ImmutableList<T>.
    // ImmutableList<T> is @Immutable — Compose can use equals() to decide whether a
    // child needs to recompose. Same list instance = skip. New list instance = recompose.
    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    fun addItem() {
        _uiState.update { state ->
            val next = state.items.size + 1
            state.copy(items = (state.items + ListItem(next, "Item #$next")).toImmutableList())
        }
    }
}

// ─── Entry composable ─────────────────────────────────────────────────────────

/**
 * Lab entry point: renders the naive or optimized variant from the [optimized] flag.
 *
 * **Naive:** the ViewModel exposes `StateFlow<List<ListItem>>`. `List<T>` is an unstable
 * JVM interface — the Compose compiler cannot guarantee its contents haven't changed.
 * Any composable that accepts `List<T>` is inferred as unstable and unskippable: pressing
 * 'Poke' recomposes the parent, which calls the child with the same list reference, but
 * the child recomposes anyway because its parameter type is unstable.
 *
 * **Optimized:** the ViewModel exposes `StateFlow<ItemsUiState>` where `ItemsUiState`
 * holds an `ImmutableList<T>`. `ImmutableList` is `@Immutable` — the compiler infers the
 * child as stable and skippable. The same list instance → equals check passes → child
 * skips. The RecompositionCounter stays flat when 'Poke' is pressed.
 */
@Composable
fun ViewModelStateLab(optimized: Boolean) {
    if (optimized) {
        OptimizedStateLab()
    } else {
        NaiveStateLab()
    }
}

// ─── Naive variant ────────────────────────────────────────────────────────────

@Composable
private fun NaiveStateLab(vm: NaiveStateViewModel = viewModel()) {
    val items by vm.items.collectAsStateWithLifecycle()
    var pokeCount by remember { mutableStateOf(0) }

    LabContent(
        pokeCount = pokeCount,
        onPoke = { pokeCount++ },
        onAddItem = vm::addItem,
    ) {
        // items: List<ListItem> — unstable. NaiveItemDisplay is inferred unskippable:
        // it recomposes whenever the parent recomposes, even when items is unchanged.
        NaiveItemDisplay(items = items)
    }
}

// ─── Optimized variant ────────────────────────────────────────────────────────

@Composable
private fun OptimizedStateLab(vm: OptimizedStateViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var pokeCount by remember { mutableStateOf(0) }

    LabContent(
        pokeCount = pokeCount,
        onPoke = { pokeCount++ },
        onAddItem = vm::addItem,
    ) {
        // uiState.items: ImmutableList<ListItem> — stable. OptimizedItemDisplay is skippable:
        // same list instance → equals passes → child skips when parent recomposes.
        OptimizedItemDisplay(items = uiState.items)
    }
}

// ─── Shared scaffold ──────────────────────────────────────────────────────────

@Composable
private fun LabContent(
    pokeCount: Int,
    onPoke: () -> Unit,
    onAddItem: () -> Unit,
    itemDisplay: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "'Poke' changes unrelated state. Watch the counter. " +
                "Naive: counter increments on every Poke (List<T> is unstable). " +
                "Optimized: counter stays flat on Poke — list unchanged → child skips.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onPoke, modifier = Modifier.weight(1f)) {
                Text("Poke ($pokeCount)")
            }
            Button(onClick = onAddItem, modifier = Modifier.weight(1f)) {
                Text("Add item")
            }
        }

        itemDisplay()
    }
}

// ─── Naive item display (List<T> parameter — unstable, unskippable) ───────────

@Composable
private fun NaiveItemDisplay(items: List<ListItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RecompositionCounter(
            tag = "ItemList",
            modifier = Modifier.padding(bottom = 4.dp),
        )
        ItemContent(items = items)
    }
}

// ─── Optimized item display (ImmutableList<T> parameter — stable, skippable) ──

@Composable
private fun OptimizedItemDisplay(items: ImmutableList<ListItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RecompositionCounter(
            tag = "ItemList",
            modifier = Modifier.padding(bottom = 4.dp),
        )
        ItemContent(items = items)
    }
}

@Composable
private fun ItemContent(items: List<ListItem>) {
    if (items.isEmpty()) {
        Text(
            text = "No items yet — press 'Add item'.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        items.takeLast(5).forEach { item ->
            Text(item.label, style = MaterialTheme.typography.bodySmall)
        }
        if (items.size > 5) {
            Text(
                "… and ${items.size - 5} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
