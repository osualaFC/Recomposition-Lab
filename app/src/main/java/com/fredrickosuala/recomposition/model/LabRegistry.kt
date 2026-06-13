package com.fredrickosuala.recomposition.model

import com.fredrickosuala.recomposition.labs.backwardswrite.BackwardsWriteLab
import com.fredrickosuala.recomposition.labs.columnvslazycolumn.ColumnVsLazyColumnLab
import com.fredrickosuala.recomposition.labs.derivedstateof.DerivedStateOfLab
import com.fredrickosuala.recomposition.labs.deferredread.DeferredReadLab
import com.fredrickosuala.recomposition.labs.drawphaseread.DrawPhaseReadLab
import com.fredrickosuala.recomposition.labs.effectkeys.EffectKeysLab
import com.fredrickosuala.recomposition.labs.expensiveworkinlazy.ExpensiveWorkInLazyLab
import com.fredrickosuala.recomposition.labs.heavymainthread.HeavyMainThreadLab
import com.fredrickosuala.recomposition.labs.imageloading.ImageLoadingLab
import com.fredrickosuala.recomposition.labs.lazylistkeys.LazyListKeysLab
import com.fredrickosuala.recomposition.labs.overdraw.OverdrawLab
import com.fredrickosuala.recomposition.labs.oversubscription.OverSubscriptionLab
import com.fredrickosuala.recomposition.labs.scopereduction.ScopeReductionLab
import com.fredrickosuala.recomposition.labs.strongskipping.StrongSkippingLab
import com.fredrickosuala.recomposition.labs.unstableclass.UnstableClassLab
import com.fredrickosuala.recomposition.labs.unstablecollections.UnstableCollectionsLab
import com.fredrickosuala.recomposition.labs.viewmodelstate.ViewModelStateLab

val allLabs: List<Lab> = listOf(
    Lab(
        id = "unstable_class",
        title = "Unstable Class Parameter",
        category = Category.Recomposition,
        difficulty = Difficulty.Intermediate,
        description = "A class with a var field is inferred as unstable, making its receiving " +
            "composable unskippable even when the data hasn't changed.",
        problemStatement = "When a composable parameter's type has a var field (or any mutable " +
            "public state), the Compose compiler marks it as unstable. Unstable params disable " +
            "skippability: the child recomposes on every parent recomposition, regardless of " +
            "whether the actual data changed.",
        howToDetect = "Add a RecompositionCounter to the child and tap a sibling button that " +
            "changes unrelated state. If the counter increments even though the child's data " +
            "is unchanged, the parameter type is likely unstable. Confirm with Layout Inspector " +
            "or Compose compiler metrics (./gradlew assembleDebug -Pcompose.metrics=true).",
        theFix = "Mark the type @Immutable (promising all publicly accessible state is deeply " +
            "immutable) or use only val fields of stable types. This lets the compiler infer " +
            "the type as stable, enabling equals()-based comparison and making the composable " +
            "skippable.",
        content = { optimized -> UnstableClassLab(optimized) },
    ),
    Lab(
        id = "unstable_collections",
        title = "Unstable Collections",
        category = Category.Recomposition,
        difficulty = Difficulty.Basic,
        description = "List<T> is a mutable JVM interface — Compose treats it as unstable, " +
            "causing unnecessary recompositions when the list content hasn't changed.",
        problemStatement = "List<T> is an interface that provides no mutability guarantees, so " +
            "the Compose compiler always treats it as unstable. Any composable that accepts " +
            "List<T> is unskippable: even if the list contents are identical between " +
            "recompositions, a new list object means a new reference → recompose.",
        howToDetect = "Add a RecompositionCounter to the list composable and tap a sibling " +
            "button. If the counter increments on every parent recomposition — despite the " +
            "list being unchanged — the parameter type is unstable. Compose compiler metrics " +
            "will label it 'unstable'.",
        theFix = "Replace List<T> with ImmutableList<T> from kotlinx-collections-immutable " +
            "(or wrap with toImmutableList()). ImmutableList is annotated @Immutable, so " +
            "Compose uses equals() for comparison — same contents → same equality → skip.",
        content = { optimized -> UnstableCollectionsLab(optimized) },
    ),
    Lab(
        id = "strong_skipping",
        title = "Strong Skipping Mode",
        category = Category.Recomposition,
        difficulty = Difficulty.Advanced,
        description = "Strong skipping (default since Compose compiler 1.5.5) auto-remembers " +
            "unstable lambda params. Wrapping lambdas in unstable classes defeats this.",
        problemStatement = "Strong skipping automatically wraps plain () -> Unit parameters in " +
            "remember, so children skip even when the parent recomposes. This only applies to " +
            "raw lambda params. Wrapping a callback inside an unstable class bypasses the " +
            "auto-remember, creating a new object on every recomposition and forcing the child " +
            "to recompose unnecessarily.",
        howToDetect = "Add a RecompositionCounter to the child and tap 'Recompose parent'. " +
            "If the child's counter increments every time, the callback parameter is likely " +
            "an unstable class wrapper rather than a plain lambda. Compose compiler reports " +
            "will confirm the param is unstable. To observe the baseline without strong " +
            "skipping, add featureFlags = setOf(ComposeFeatureFlag.StrongSkipping.disabled()) " +
            "to the composeCompiler {} block in build.gradle.kts.",
        theFix = "Accept callbacks as plain () -> Unit (or other function types) rather than " +
            "wrapping them in unstable data-holder classes. Strong skipping's automatic lambda " +
            "remember then takes effect and the child skips when the parent recomposes.",
        content = { optimized -> StrongSkippingLab(optimized) },
    ),
    Lab(
        id = "over_subscription",
        title = "Over-subscription to State",
        category = Category.Recomposition,
        difficulty = Difficulty.Basic,
        description = "A child that reads one field from a large object recomposes whenever " +
            "any other field changes, even ones it never uses.",
        problemStatement = "When a composable takes an entire state object but only reads one " +
            "field, it is subscribed to changes in every field. Any update to the object — " +
            "even fields the composable ignores — triggers a recomposition. This is silent " +
            "waste: the UI output is identical but work is done unnecessarily.",
        howToDetect = "Add a RecompositionCounter to the child and change a field it doesn't " +
            "display. If the counter increments, the child is over-subscribed. Layout Inspector " +
            "recomposition counts confirm it. Compose compiler reports won't catch this — it " +
            "is an architectural issue, not a stability issue.",
        theFix = "Pass only the specific field(s) the composable needs (e.g. tickCount: Int " +
            "instead of state: DashboardState). The child's scope then only captures that " +
            "field; changes to unread fields no longer trigger recomposition.",
        content = { optimized -> OverSubscriptionLab(optimized) },
    ),
    Lab(
        id = "scope_reduction",
        title = "Recomposition Scope",
        category = Category.Recomposition,
        difficulty = Difficulty.Intermediate,
        description = "Reading a fast-changing state value in a parent composable makes the " +
            "entire parent the recompose scope, propagating unnecessary work to all children.",
        problemStatement = "When a composable reads a state value directly in its body, " +
            "that composable becomes the recompose scope. If the value changes frequently " +
            "(e.g. a ticker, animation, or scroll position), the entire composable body " +
            "re-executes on every change, and any child whose params change as a result " +
            "recomposes too — even siblings that don't need the value.",
        howToDetect = "Add RecompositionCounters to the parent and each child. If all counters " +
            "spike when a single fast-changing value changes, the read is happening too high in " +
            "the tree. Layout Inspector's recomposition overlay confirms which scopes are hot.",
        theFix = "Move the state read down to the smallest composable that actually needs it. " +
            "If passing the value as a plain parameter forces the parent to read it first, " +
            "pass a lambda '() -> T' instead — the parent only captures the lambda object " +
            "(stable), and the child invokes it, making the child the recompose scope.",
        content = { optimized -> ScopeReductionLab(optimized) },
    ),
    Lab(
        id = "deferred_read",
        title = "Deferred State Read",
        category = Category.Performance,
        difficulty = Difficulty.Advanced,
        description = "Reading scroll offset in a composable body triggers recomposition on " +
            "every pixel. Deferring the read to the layout phase keeps the counter flat " +
            "while the UI still moves.",
        problemStatement = "Accessing a rapidly changing value (like scroll position) in the " +
            "composable body makes that scope the recompose scope. For scroll, this means " +
            "hundreds of recompositions per second. Even passing the value as '() -> Int' " +
            "(step 1) still recomposes the leaf that invokes it.",
        howToDetect = "Add a RecompositionCounter to the composable that consumes scroll " +
            "offset. If the counter increments on every pixel of scroll (~60 fps), the read " +
            "is happening during composition. A flat counter with moving UI confirms the " +
            "read has been deferred to a later phase.",
        theFix = "Use 'Modifier.offset { IntOffset(0, scrollState.value) }' (lambda form). " +
            "The lambda runs during the layout phase; the snapshot observation is scoped " +
            "to layout, not composition. The element repositions every frame without any " +
            "recomposition. Step 1 (pass '() -> Int') prevents parent recomposition but " +
            "still recomposes the child; step 2 (layout-phase read) eliminates recomposition " +
            "entirely.",
        content = { optimized -> DeferredReadLab(optimized) },
    ),
    Lab(
        id = "draw_phase_read",
        title = "Draw-Phase State Read",
        category = Category.Performance,
        difficulty = Difficulty.Advanced,
        description = "Reading an animated color in a composable body triggers recomposition " +
            "at the display refresh rate. Reading it inside drawBehind skips composition " +
            "and layout entirely.",
        problemStatement = "Animation values update every frame (~16 ms at 60 fps). Reading " +
            "such a value with the 'by' delegate in a composable body registers the scope as " +
            "a snapshot observer. Every frame emits a new value → every frame schedules a " +
            "recomposition. This burns CPU and is entirely unnecessary when only drawing needs " +
            "the value.",
        howToDetect = "Add a RecompositionCounter to the composable applying the animated " +
            "value. If the counter increments continuously at ~60/s while the animation runs, " +
            "the read is in the composition scope. The Layout Inspector's recomposition counts " +
            "will show the composable as permanently 'hot'.",
        theFix = "Obtain the State<T> object without reading .value in the composable body " +
            "(drop the 'by' delegate). Read state.value inside 'Modifier.drawBehind { }'. " +
            "The snapshot observation is then scoped to the draw layer — composition and " +
            "layout are never invalidated. The counter goes flat while the animation continues.",
        content = { optimized -> DrawPhaseReadLab(optimized) },
    ),
    Lab(
        id = "derived_state_of",
        title = "derivedStateOf",
        category = Category.Recomposition,
        difficulty = Difficulty.Basic,
        description = "Computing a boolean directly from a frequently-changing state causes " +
            "recomposition every time the source changes, even when the result is unchanged.",
        problemStatement = "Reading 'listState.firstVisibleItemIndex > 0' directly in a " +
            "composable body registers the scope as an observer of firstVisibleItemIndex. " +
            "That index changes on every item boundary during scroll. Even though the boolean " +
            "result stays 'true' throughout items 1–59, the scope is recomposed for every " +
            "single index change.",
        howToDetect = "Add a RecompositionCounter to the composable that shows/hides the " +
            "scroll-to-top button. If the counter increments every time you scroll past an " +
            "item boundary (not just when the button appears/disappears), the boolean is " +
            "computed directly without derivedStateOf.",
        theFix = "Wrap the computation in 'remember { derivedStateOf { ... } }'. Compose " +
            "tracks the source state inside the block and only notifies the reading composable " +
            "when the *result* of the block changes. Scrolling through items 1–59 = zero " +
            "recompositions; only crossing the 0↔1 boundary counts.",
        content = { optimized -> DerivedStateOfLab(optimized) },
    ),
    Lab(
        id = "backwards_write",
        title = "Backwards Write",
        category = Category.Recomposition,
        difficulty = Difficulty.Basic,
        description = "Writing to a MutableState inside the composable body creates an " +
            "immediate recomposition loop — a correctness bug, not just a performance issue.",
        problemStatement = "Mutating a MutableState during composition tells Compose to " +
            "schedule a new recomposition of that scope. The new recomposition runs the body " +
            "again, mutates state again, schedules another recomposition, and so on — an " +
            "infinite loop. A guard breaks the cycle in this demo, but in production code " +
            "this pattern causes ANRs or Compose's own loop-detection exception.",
        howToDetect = "A RecompositionCounter that jumps immediately to a large number " +
            "without any user interaction is a strong signal. Enable the Compose recomposition " +
            "overlay in Layout Inspector — a composable that is permanently highlighted red " +
            "is a good candidate. The Compose runtime also logs 'State was written during " +
            "composition' warnings in debug builds.",
        theFix = "Never mutate state inside a composable body. All state writes must happen " +
            "in event lambdas (onClick, onValueChange, LaunchedEffect, etc.) that run " +
            "outside the composition phase. One event → one state change → one recomposition.",
        content = { optimized -> BackwardsWriteLab(optimized) },
    ),
    Lab(
        id = "lazy_list_keys",
        title = "Lazy List Keys",
        category = Category.Performance,
        difficulty = Difficulty.Basic,
        description = "A LazyColumn without item keys recomposes all displaced rows when " +
            "one item moves to the top, even though only one item's position changed.",
        problemStatement = "When items() is called without key = { it.id }, Compose uses " +
            "positional identity: the composable at index 0 is always the 'same' composable. " +
            "Moving a note from position 4 to position 0 means positions 0–4 all receive " +
            "different NoteItem data → all five rows recompose, even though no note's content " +
            "changed. The more items in the list, the more recompositions occur on each reorder.",
        howToDetect = "Add a RecompositionCounter to each row. Move an item to the top " +
            "and observe how many counters increment. Without keys, all shifted rows flash. " +
            "The Layout Inspector recomposition overlay confirms the same pattern at a glance. " +
            "Compose compiler metrics will not catch this — it is a runtime identity issue, " +
            "not a stability issue.",
        theFix = "Pass key = { it.id } to items(). Compose then matches each composable " +
            "by item identity rather than position. Moving an item only relocates its existing " +
            "composable — no data changed → zero recompositions. As a bonus, animateItem() " +
            "becomes available, giving smooth reorder animations for free. " +
            "Add contentType = { 'YourItemClass' } to group structurally identical items: " +
            "when an item leaves the viewport, its slot can be reused by a newly-visible item " +
            "of the same contentType, saving slot initialization — especially valuable for " +
            "heterogeneous lists that mix item types (headers, rows, ads, etc.).",
        content = { optimized -> LazyListKeysLab(optimized) },
    ),
    Lab(
        id = "expensive_work_in_lazy",
        title = "Expensive Work in Composition",
        category = Category.Performance,
        difficulty = Difficulty.Basic,
        description = "Filter+sorting a large list directly in a composable body re-runs on " +
            "every recomposition, even for unrelated state changes.",
        problemStatement = "Any computation outside remember re-executes every time the " +
            "composable body runs. For filter+sort over hundreds of items, this is measurably " +
            "expensive. Worse, it re-runs for every unrelated state change (e.g. a button " +
            "counter update), wasting CPU even when the query — the only input that should " +
            "trigger a re-sort — hasn't changed.",
        howToDetect = "Add a counter that increments inside the sort/filter operation. " +
            "Press a button that changes unrelated state and watch the counter increment — " +
            "the sort ran even though the query was unchanged. In production, attach Android " +
            "Studio's CPU profiler and look for repeated filter/sort stack frames during " +
            "UI interactions that have nothing to do with the list.",
        theFix = "Wrap the computation in remember(key) { ... } where key is the input " +
            "that, when changed, should trigger a re-sort (typically the query string). " +
            "The block runs once on first composition and again only when the key changes; " +
            "every other recomposition reuses the cached result. For production, go further: " +
            "move the computation to the ViewModel with a background dispatcher, producing a " +
            "StateFlow<List<T>> that Compose collects — keeping the main thread free entirely.",
        content = { optimized -> ExpensiveWorkInLazyLab(optimized) },
    ),
    Lab(
        id = "column_vs_lazy_column",
        title = "Column vs LazyColumn",
        category = Category.Performance,
        difficulty = Difficulty.Basic,
        description = "Column + verticalScroll eagerly composes every item upfront, " +
            "consuming memory and main-thread budget regardless of how many items are visible.",
        problemStatement = "Column is not lazy: adding Modifier.verticalScroll() makes the " +
            "list scrollable, but ALL items are composed immediately on first render and " +
            "remain in composition forever — even items hundreds of pixels off-screen. " +
            "For a 100-item list this is a noticeable initial composition spike; for 1000+ " +
            "items it causes jank, high memory pressure, and makes every subsequent " +
            "recomposition expensive because all items recompose, not just visible ones.",
        howToDetect = "Place a RecompositionCounter in each row. Press 'Recompose' and " +
            "count how many counters increment — all 100 in the naive case, only the " +
            "visible ~10 in the optimized case. The JankMeter spinner stutters on the " +
            "initial render of the naive variant (100 composables initialising at once). " +
            "Layout Inspector's 'Show Recomposition Count' overlay confirms the difference.",
        theFix = "Replace Column + verticalScroll with LazyColumn (or LazyRow / " +
            "LazyVerticalGrid for grids). LazyColumn only composes items that are at or " +
            "near the viewport. Off-screen items are decomposed and their resources are " +
            "freed; they recompose only when scrolled back into view. Rule of thumb: " +
            "if the item count can grow unbounded or exceeds ~20 items, use a lazy layout.",
        content = { optimized -> ColumnVsLazyColumnLab(optimized) },
    ),
    Lab(
        id = "heavy_main_thread",
        title = "Heavy Work on the Main Thread",
        category = Category.Performance,
        difficulty = Difficulty.Intermediate,
        description = "Expensive CPU work called without a dispatcher switch runs on the " +
            "main thread, blocking frame delivery and making the JankMeter stutter.",
        problemStatement = "LaunchedEffect runs on the main thread by default. Any suspend " +
            "function called inside it that does CPU-bound work — sorting, parsing, heavy " +
            "computation — without switching to Dispatchers.Default will execute on the main " +
            "thread. The main thread is the same thread Choreographer uses to deliver frames. " +
            "Blocking it for even 16 ms drops a frame; 200+ ms causes visible jank.",
        howToDetect = "Place a JankMeter (a continuously-spinning arc driven by Vsync) next " +
            "to the triggering action. If the spinner stutters or freezes when the operation " +
            "runs, the main thread is blocked. For production: Android Studio's CPU Profiler " +
            "→ 'Main thread wall clock time' shows blocking intervals. Systrace and Perfetto " +
            "both show main-thread slices exceeding 16 ms as frame drops.",
        theFix = "Hoist the computation into a ViewModel and wrap it with " +
            "withContext(Dispatchers.Default). The ViewModel's viewModelScope ensures the " +
            "coroutine is cancelled when the composable leaves the screen. Expose the result " +
            "as StateFlow<UiState> and collect it with collectAsStateWithLifecycle() — the " +
            "composable observes state changes and shows a loading indicator while work runs, " +
            "keeping the main thread free to render every frame.",
        content = { optimized -> HeavyMainThreadLab(optimized) },
    ),
    Lab(
        id = "effect_keys",
        title = "Effect Keys / Restarting Effects",
        category = Category.Recomposition,
        difficulty = Difficulty.Intermediate,
        description = "A LaunchedEffect with an unstable key (a new object on every " +
            "recomposition) restarts the effect on every recompose, cancelling any " +
            "in-flight work.",
        problemStatement = "LaunchedEffect compares its key with the previous key using " +
            "equals(). A plain class (no data modifier) uses reference equality: every new " +
            "instance is 'different', even with identical field values. Constructing such a " +
            "class in the composable body — without remember — means every recomposition " +
            "creates a fresh object. LaunchedEffect sees a new key → cancels the current " +
            "coroutine → launches a new one. Any in-flight network request, animation, or " +
            "timer is silently restarted.",
        howToDetect = "Add a restart counter inside the LaunchedEffect block and display it " +
            "in the UI. If the counter increments when an unrelated state change causes a " +
            "parent recomposition, the key is unstable. In production, unexpected duplicate " +
            "network requests or re-running animations are common symptoms. Logging inside " +
            "the LaunchedEffect with the coroutine job ID will confirm repeated launches.",
        theFix = "Key the effect on the smallest, stable value that — when it genuinely " +
            "changes — should restart the effect. For a user feed, that is the userId String, " +
            "not a config object that wraps it. Prefer primitive types, Strings, and stable " +
            "data classes (with proper equals/hashCode) as keys. If an object must be the " +
            "key, either remember it or use a data class so equals() is structural.",
        content = { optimized -> EffectKeysLab(optimized) },
    ),
    Lab(
        id = "viewmodel_state",
        title = "ViewModel State Exposure",
        category = Category.Recomposition,
        difficulty = Difficulty.Intermediate,
        description = "Exposing List<T> from a ViewModel makes receiving composables " +
            "unstable and unskippable. Wrapping state in a @Stable UiState with " +
            "ImmutableList<T> restores skippability.",
        problemStatement = "StateFlow<List<T>> is a common ViewModel pattern, but " +
            "List<T> is an unstable JVM interface. The Compose compiler cannot guarantee " +
            "its contents haven't changed between recompositions, so any composable that " +
            "accepts List<T> is inferred as unstable and unskippable. Every parent " +
            "recomposition — including ones driven by completely unrelated state — forces " +
            "the child to recompose, even when the list is identical.",
        howToDetect = "Add a RecompositionCounter to the child that displays the list. " +
            "Change unrelated state in the parent (the 'Poke' button) and watch the counter. " +
            "If it increments, the child is unskippable. Compose compiler metrics " +
            "(./gradlew assembleDebug -Pcompose.metrics=true) will report the composable's " +
            "parameter types as 'unstable', confirming the root cause.",
        theFix = "Model the ViewModel's output as a data class (e.g. ItemsUiState) whose " +
            "fields use stable types. Replace List<T> with ImmutableList<T> from " +
            "kotlinx-collections-immutable — it is annotated @Immutable, making composables " +
            "that receive it skippable. Expose the data class via StateFlow<ItemsUiState> " +
            "and collect with collectAsStateWithLifecycle(). The RecompositionCounter now " +
            "stays flat when 'Poke' is pressed; it only increments when the list actually " +
            "changes.",
        content = { optimized -> ViewModelStateLab(optimized) },
    ),
    Lab(
        id = "overdraw",
        title = "Overdraw",
        category = Category.Performance,
        difficulty = Difficulty.Basic,
        description = "Stacking multiple opaque backgrounds causes the GPU to redraw the " +
            "same pixels multiple times per frame — wasted fill-rate with no visual benefit.",
        problemStatement = "Every Modifier.background() call instructs the GPU to fill that " +
            "region with a color. When an opaque background is placed inside another opaque " +
            "background, the outer layer is completely occluded — yet the GPU still renders " +
            "it. On modern hardware this is rarely the sole bottleneck, but it compounds " +
            "with other work: each overdraw layer consumes fill-rate budget, heats the GPU, " +
            "and can contribute to frame drops on low-end devices or in complex layouts.",
        howToDetect = "Enable 'Debug GPU Overdraw' in Developer Options → Show overdraw areas. " +
            "Colors indicate how many times each pixel was drawn: true color = 1× (ideal), " +
            "blue = 2×, green = 3×, pink = 4×, red = 5×+. Red regions are severe and should " +
            "be fixed. Also inspect the View hierarchy or Compose Layout Inspector — any " +
            "node with a background whose parent already has an opaque background of the same " +
            "color is a candidate for removal.",
        theFix = "Flatten nested backgrounds to a single Modifier.background() on the " +
            "outermost container. Remove any window or theme background that is already " +
            "covered by an opaque Scaffold or Surface. In Compose, each recompose scope " +
            "should own at most one background; pass the desired color inward rather than " +
            "re-applying it at every level.",
        content = { optimized -> OverdrawLab(optimized) },
    ),
    Lab(
        id = "image_loading",
        title = "Image Loading",
        category = Category.Performance,
        difficulty = Difficulty.Basic,
        description = "Loading large images without a size constraint or placeholder causes " +
            "excessive memory use and abrupt pop-in. Coil can sample down to the display " +
            "size during decode — reducing memory 5× or more.",
        problemStatement = "A raw URL passed to AsyncImage with no ImageRequest " +
            "customization gives Coil no explicit decode-size target. If the measured layout " +
            "size is imprecise (common in lazy grids during the first pass), Coil may decode " +
            "the full-resolution source — e.g. 800×800 px — even for a 120 dp display slot " +
            "on a 3× screen (which only needs 360×360 px). Holding 10× more pixels than " +
            "needed multiplies heap usage across all grid items, increasing GC pressure and " +
            "causing stutters during fast scrolling. Additionally, no placeholder means grid " +
            "cells are blank during loading; no crossfade makes images pop in abruptly.",
        howToDetect = "Open Android Studio's Memory Profiler and compare heap snapshots " +
            "between the naive and optimized variants while scrolling. Look for " +
            "android.graphics.Bitmap allocations — the naive variant will show significantly " +
            "larger bitmaps per image. The JankMeter may stutter on memory-constrained " +
            "devices when the GC runs to reclaim oversized bitmaps.",
        theFix = "Create an ImageRequest with crossfade(true) for smooth transitions and " +
            "supply a placeholder Painter so cells are never blank. Apply ContentScale.Crop " +
            "so the image fills its bounded slot correctly. Coil reads the composable's " +
            "measured layout size from the Modifier chain to determine the down-sample target " +
            "during BitmapFactory decode — ensure the AsyncImage has explicit size modifiers " +
            "(fillMaxWidth + aspectRatio, or an explicit size()) so Coil always has an " +
            "accurate target. For very large source images, set .size() on the request " +
            "builder explicitly as a hard upper bound.",
        content = { optimized -> ImageLoadingLab(optimized) },
    ),
)
