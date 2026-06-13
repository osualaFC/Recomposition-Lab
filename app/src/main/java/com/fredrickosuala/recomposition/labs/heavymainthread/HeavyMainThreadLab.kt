package com.fredrickosuala.recomposition.labs.heavymainthread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fredrickosuala.recomposition.core.JankMeter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Shared computation (identical work in both variants) ─────────────────────

/**
 * Generates and reverse-sorts 40 000 strings — roughly 150–250 ms on a mid-range device.
 * No coroutine dispatcher switching here; callers decide where this runs.
 */
private fun doHeavyWork(): String {
    val data = (1..40_000).map { i -> "entry_${i}_${"payload".repeat(3)}" }
    return data.sortedDescending().take(5).joinToString("\n")
}

// ─── Optimized ViewModel ──────────────────────────────────────────────────────

sealed interface HeavyState {
    data object Idle : HeavyState
    data object Loading : HeavyState
    data class Done(val preview: String) : HeavyState
}

class HeavyMainThreadViewModel : ViewModel() {
    private val _state = MutableStateFlow<HeavyState>(HeavyState.Idle)
    val state: StateFlow<HeavyState> = _state.asStateFlow()

    fun compute() {
        viewModelScope.launch {
            _state.value = HeavyState.Loading
            // ✅ FIX: withContext(Dispatchers.Default) offloads the CPU work to a background
            // thread pool. The main thread is free to render every frame while sorting runs.
            val result = withContext(Dispatchers.Default) { doHeavyWork() }
            _state.value = HeavyState.Done(result)
        }
    }
}

// ─── Entry composable ─────────────────────────────────────────────────────────

/**
 * Lab entry point: renders the naive or optimized variant from the [optimized] flag.
 *
 * **Naive:** `doHeavyWork()` is called inside a `LaunchedEffect` with no dispatcher switch.
 * `LaunchedEffect` uses the main-thread dispatcher by default, so the ~200 ms CPU loop
 * runs on the main thread — blocking frame delivery and making the JankMeter stutter.
 *
 * **Optimized:** a ViewModel launches a coroutine and immediately suspends into
 * `Dispatchers.Default`. The main thread never touches the computation; it keeps rendering
 * frames, and the JankMeter stays smooth. A loading state is shown while work runs.
 */
@Composable
fun HeavyMainThreadLab(optimized: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Watch the Jank Meter while computation runs. " +
                "Naive: main thread is blocked — spinner stutters or freezes. " +
                "Optimized: work runs on Dispatchers.Default — spinner stays smooth, " +
                "with a proper loading state.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        JankMeter(modifier = Modifier.padding(bottom = 16.dp))

        if (optimized) {
            OptimizedHeavyWork()
        } else {
            NaiveHeavyWork()
        }
    }
}

// ─── Naive variant ────────────────────────────────────────────────────────────

@Composable
private fun NaiveHeavyWork() {
    var trigger by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }
    var computing by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        computing = true
        // ⚠️ PROBLEM: doHeavyWork() has no withContext — it executes on the main thread
        // (LaunchedEffect's default dispatcher is the main thread). The ~200 ms CPU loop
        // blocks the Choreographer from delivering frames → JankMeter stutters.
        result = doHeavyWork()
        computing = false
    }

    ResultPanel(
        result = result,
        computing = computing,
        onCompute = { trigger++ },
    )
}

// ─── Optimized variant ────────────────────────────────────────────────────────

@Composable
private fun OptimizedHeavyWork(vm: HeavyMainThreadViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    ResultPanel(
        result = (state as? HeavyState.Done)?.preview,
        computing = state is HeavyState.Loading,
        onCompute = vm::compute,
    )
}

// ─── Shared UI ────────────────────────────────────────────────────────────────

@Composable
private fun ResultPanel(
    result: String?,
    computing: Boolean,
    onCompute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onCompute,
            enabled = !computing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (computing) "Computing…" else "Run computation (generates & sorts 40 000 strings)")
        }

        when {
            computing -> CircularProgressIndicator()
            result != null -> Text(
                text = "Top 5 sorted results:\n$result",
                style = MaterialTheme.typography.bodySmall,
            )
            else -> Text(
                text = "Press the button, then watch the Jank Meter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
