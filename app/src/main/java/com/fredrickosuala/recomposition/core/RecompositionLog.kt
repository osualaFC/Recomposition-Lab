package com.fredrickosuala.recomposition.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DEFAULT_CAPACITY = 20

/** A single event captured by [RecompositionLogState]. */
data class RecompositionEvent(val tag: String, val timestampMs: Long)

/**
 * An in-memory ring buffer that records "recomposed: tag" events from composables in the
 * tree. Backed by a [SnapshotStateList] so the [RecompositionLogOverlay] recomposes
 * automatically whenever a new entry arrives.
 *
 * **How to populate:** drop [LogRecomposition] inside any composable you want to monitor,
 * or call [log] directly from a [SideEffect].
 *
 * **Limitation:** [log] appends to a [SnapshotStateList], which schedules a recomposition
 * of any composable that reads [entries]. This is intentional for the overlay but means
 * the logger itself slightly perturbs the system it measures — disable it for
 * performance-sensitive benchmarks.
 *
 * @param capacity Maximum number of events retained; older entries are discarded.
 */
class RecompositionLogState(val capacity: Int = DEFAULT_CAPACITY) {
    private val _entries: SnapshotStateList<RecompositionEvent> =
        mutableListOf<RecompositionEvent>().toMutableStateList()

    /** Snapshot-backed list of recorded events, chronological (oldest first). */
    val entries: List<RecompositionEvent> get() = _entries

    /** Appends a new event, evicting the oldest entry when at capacity. */
    fun log(tag: String) {
        if (_entries.size >= capacity) _entries.removeAt(0)
        _entries.add(RecompositionEvent(tag, System.currentTimeMillis()))
    }
}

/** Creates and remembers a [RecompositionLogState] that survives recompositions. */
@Composable
fun rememberRecompositionLogState(capacity: Int = DEFAULT_CAPACITY): RecompositionLogState =
    remember(capacity) { RecompositionLogState(capacity) }

/**
 * Provides a [RecompositionLogState] to descendant composables via [LocalRecompositionLog].
 * [LabScaffold] installs this automatically; labs can then call [LogRecomposition] without
 * passing the state explicitly.
 */
val LocalRecompositionLog = compositionLocalOf<RecompositionLogState?> { null }

/**
 * A zero-output composable that records a log entry in [logState] each time it is
 * successfully recomposed. [SideEffect] guarantees that only *committed* recompositions
 * are counted — abandoned or skipped recompositions do not produce entries.
 *
 * If [logState] is null (i.e. no [LocalRecompositionLog] is provided), this is a no-op.
 *
 * @param tag Identifies which composable is being tracked in the overlay.
 * @param logState The log to write to. Defaults to [LocalRecompositionLog] if null.
 */
@Composable
fun LogRecomposition(
    tag: String,
    logState: RecompositionLogState? = LocalRecompositionLog.current,
) {
    SideEffect { logState?.log(tag) }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

// Number of events shown in the overlay at once (independent of ring-buffer capacity).
private const val OVERLAY_DISPLAY_COUNT = 8

/**
 * A translucent overlay listing the most recent recomposition events from [logState],
 * newest first. Toggle visibility with the debug switch in [LabScaffold]'s top bar.
 *
 * @param logState The source of events to display.
 * @param visible Set to `false` to hide the overlay without removing it from the tree.
 */
@Composable
fun RecompositionLogOverlay(
    logState: RecompositionLogState,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    Column(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Recomposition log",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF90CAF9),
        )
        val recentEvents = logState.entries.takeLast(OVERLAY_DISPLAY_COUNT).reversed()
        recentEvents.forEach { event ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 3.dp)) {
                Text(
                    text = timeFormat.format(Date(event.timestampMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF90CAF9),
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = event.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
        if (recentEvents.isEmpty()) {
            Text(
                text = "No events yet",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
