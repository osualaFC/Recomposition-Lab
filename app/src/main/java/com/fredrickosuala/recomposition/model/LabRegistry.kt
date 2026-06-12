package com.fredrickosuala.recomposition.model

import com.fredrickosuala.recomposition.labs.oversubscription.OverSubscriptionLab
import com.fredrickosuala.recomposition.labs.strongskipping.StrongSkippingLab
import com.fredrickosuala.recomposition.labs.unstableclass.UnstableClassLab
import com.fredrickosuala.recomposition.labs.unstablecollections.UnstableCollectionsLab

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
)
