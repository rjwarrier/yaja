# Code Optimisations Status

This file tracks the optimization ideas that are still worth considering after the recent cache, startup, widget, and UI passes.

## Already Addressed

1. `JournalCacheStore.kt` no longer deserializes the freshly generated JSON on every save just to validate its own output.
2. `KeywordMatcher.kt` no longer allocates a full 2D Levenshtein matrix for every fuzzy comparison. It now uses a rolling-row implementation with bounded early exit.
3. Major screens have largely moved from `collectAsState()` to `collectAsStateWithLifecycle()`, including the keyword screens.
4. Startup no longer performs aggressive full refresh verification in the critical path for normal app launches.
5. Statistics now leans on prepared snapshots and daily metrics caches instead of repeatedly recounting raw entry data.

## Still Relevant

1. `MarkdownFileManager.kt`
   - SAF path resolution can still be expensive in import-heavy flows.
   - If import or migration performance becomes a pain point again, a small directory-level cache is still a good candidate.

2. `JournalViewModel.kt`
   - The three import flows are still structurally similar.
   - Extracting a shared import runner would be a maintainability win and reduce bug surface in long-running flows.

3. `KeywordsScreen.kt` / `KeywordDetailScreen.kt`
   - Both still define identical `DateTimeFormatter` patterns locally.
   - This is a small cleanup, not a performance bottleneck.

4. `MarkdownFileManager.kt`
   - Batched cache population can still be simplified further if full population becomes hot again.
   - Lower priority now because the app is incremental-first and avoids full eager population in normal use.

## Lower Priority / Reprioritized

1. `StateFlow.value = ...` from background threads
   - Thread-safe already.
   - Any cleanup here is code-style oriented, not a meaningful performance win.

2. `HomeScreen` `derivedStateOf` cleanup
   - Worth tidying only if we are already revisiting that area.
   - Not a top-tier optimization target anymore.

3. Lookback cache memory concerns
   - The cache is now bounded and invalidated more deliberately.
   - Still worth watching, but not urgent.

4. Singleton / constructor cleanup in `MarkdownFileManager`
   - Maintainability improvement only.

## Best Next Performance Candidates

1. SAF directory caching for bulk import and migration paths only
2. Import-flow consolidation in `JournalViewModel`
3. Small shared formatting/util cleanup in keyword screens

## Notes

- This file intentionally reflects the current incremental-first architecture, not the older refactor snapshot.
- If a new slowdown appears, prefer measuring it first with logs or targeted profiling before expanding this list.
