# Recompose Lab

An educational Android app (Jetpack Compose) demonstrating recomposition and
performance issues, how to detect them, and side-by-side naive vs optimized fixes.
Audience: developers learning Compose performance. This will be published publicly,
so code must be exemplary and well-commented.

## Build & verify
- Build: `./gradlew :app:assembleDebug`
- Lint:  `./gradlew :app:lintDebug`
- Unit tests: `./gradlew :app:testDebugUnitTest`
- ALWAYS run the build and confirm it compiles before declaring a task done.

## Tech
- Kotlin 2.x, Compose Compiler Gradle plugin, Compose BOM (latest stable), Material 3
- kotlinx-collections-immutable for stable collections
- Coil for images
- Single `:app` module. minSdk 24, target latest stable SDK. Gradle Kotlin DSL.

## Architecture rules
- Each lab is self-contained in its own package under `labs/`.
- Every lab exposes ONE entry composable taking `(optimized: Boolean)` and renders
  the naive or optimized variant from that flag. Visible behavior must be identical
  between variants — only performance differs.
- No over-engineering. No DI framework. Keep ViewModels minimal and only where a lab
  needs them. Prefer the simplest code that teaches the point.

## Teaching rules (non-negotiable)
- Every lab includes: a one-line problem statement, a "How to detect it" note, and a
  "The fix" note, surfaced in the UI (not just code comments).
- Put an inline comment on the EXACT line that causes the problem and the EXACT line
  that fixes it, prefixed `// ⚠️ PROBLEM:` and `// ✅ FIX:`.
- Prefer clarity over cleverness. This is reference code people will copy.

## Principles
- Make every change as simple as possible; delete code rather than add where you can.
- Find root causes, no band-aids. Hold to senior-engineer standards.
- Only touch what's necessary; no unrelated refactors.