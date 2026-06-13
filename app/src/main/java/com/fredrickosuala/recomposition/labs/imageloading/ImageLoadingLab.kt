package com.fredrickosuala.recomposition.labs.imageloading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.fredrickosuala.recomposition.core.JankMeter

private const val IMAGE_COUNT = 18

// Stable seeds so URLs are consistent across recompositions.
private val IMAGE_SEEDS = (1..IMAGE_COUNT).map { "recomposelab$it" }

// Grid height: 6 rows × ~100 dp per cell + 5 × 4 dp spacing ≈ 620 dp.
private val GRID_HEIGHT = 620.dp

/**
 * Lab entry point: renders the naive or optimized variant from the [optimized] flag.
 *
 * **Naive:** `AsyncImage` with a raw URL, no placeholder, no crossfade, no `ContentScale`.
 * Grid cells are blank during loading then images pop in with no transition. Without an
 * explicit size constraint in the `ImageRequest`, Coil may decode the full-resolution
 * source bitmap regardless of how small the display slot is — wasting memory and
 * increasing GC pressure, which can stutter the JankMeter on fast scrolls.
 *
 * **Optimized:** `crossfade(true)` in the `ImageRequest` gives a smooth fade-in.
 * A `ColorPainter` placeholder fills the slot while loading. `ContentScale.Crop` fills
 * the bounded cell correctly. Coil infers the target decode size from the composable's
 * measured layout (the `fillMaxWidth().aspectRatio(1f)` chain), so it down-samples during
 * decode — a 120 dp slot on a 3× screen targets 360 px, not 800 px — roughly 5× less
 * memory per image. Compare heap usage with Android Studio's Memory Profiler.
 */
@Composable
fun ImageLoadingLab(optimized: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (optimized) {
                "Optimized: crossfade + placeholder + ContentScale.Crop. " +
                    "Coil samples bitmaps to the display size — much less memory per image."
            } else {
                "Naive: no placeholder, no crossfade, no ContentScale. " +
                    "Images pop in abruptly; full-res bitmaps waste memory."
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        JankMeter(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        // Fixed-height box so the LazyVerticalGrid inside can measure itself correctly
        // when it lives inside the scaffold's outer LazyColumn.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(GRID_HEIGHT)
                .padding(horizontal = 4.dp),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    count = IMAGE_COUNT,
                    key = { it },
                ) { index ->
                    if (optimized) {
                        OptimizedImage(seed = IMAGE_SEEDS[index])
                    } else {
                        NaiveImage(seed = IMAGE_SEEDS[index])
                    }
                }
            }
        }
    }
}

// ─── Naive variant ────────────────────────────────────────────────────────────

@Composable
private fun NaiveImage(seed: String) {
    // ⚠️ PROBLEM: raw URL string with no ImageRequest customization.
    // No placeholder → blank slot during loading. No crossfade → abrupt pop-in.
    // No ContentScale → image may not fill the cell correctly.
    // Coil receives no explicit decode-size hint beyond layout measurement, which
    // is imprecise in lazy grids, risking full-resolution decodes.
    AsyncImage(
        model = "https://picsum.photos/seed/$seed/800/800",
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    )
}

// ─── Optimized variant ────────────────────────────────────────────────────────

private val PLACEHOLDER_PAINTER = ColorPainter(Color(0xFFCCCCCC))

@Composable
private fun OptimizedImage(seed: String) {
    val context = LocalContext.current
    // ✅ FIX: ImageRequest with crossfade — smooth fade-in replaces abrupt pop-in.
    // Coil reads the composable's measured size from the Modifier chain below
    // (fillMaxWidth + aspectRatio inside a 3-column grid) to down-sample during decode.
    // A 3× screen in a 3-column grid gives ~360 px target → Coil decodes at 360×360
    // instead of 800×800 → ~5× less memory per image → lower GC pressure → smoother scrolling.
    val request = remember(seed) {
        ImageRequest.Builder(context)
            .data("https://picsum.photos/seed/$seed/800/800")
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            placeholder = PLACEHOLDER_PAINTER,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
