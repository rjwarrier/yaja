# JournalViewModel ownership matrix

Created: 2026-08-04  
Scope: Stage 8 refactor tracking for moving feature ownership out of `JournalViewModel`

`JournalViewModel` remains the compatibility facade for existing screens. New or extracted feature ownership should live behind focused controllers/coordinators, with public properties and methods delegated from the facade until screens can migrate directly.

| Feature area | Current owner | State owned | Jobs owned | Notes |
|---|---|---|---|---|
| Settings state and direct settings actions | `SettingsFeatureController` | Settings repository flows, six settings screen UI states, keyword fuzzy threshold read state | None; settings state uses `stateIn` on `viewModelScope` | Extracted first because settings screens already depend on facade properties and have low storage risk. Heavy workflows such as storage migration and custom font file copying still remain in `JournalViewModel`. |
| Entry add/edit/delete/reorder | `EntryCoordinator` plus `JournalViewModel` facade | Entry mutation state is applied to `JournalUiState` through callbacks | Mutation work runs through caller scope | Existing coordinator remains the entry ownership seam. |
| Keywords and keyword matching | `KeywordCoordinator` | Keyword indexing IDs, match counts, match cache state | Keyword rebuild jobs through `viewModelScope` | Existing coordinator remains the keyword ownership seam. |
| Backup, import, restore | `JournalViewModel` with helper workflows | Import state, restore summary, importer instances | Import, restore, storage migration jobs | Next high-value extraction after settings, but keep separate because it touches user data and cancellation rules. |
| Startup/cache refresh/lifecycle | `JournalViewModel` with startup helpers | Background labels, cache anomaly state, startup refresh decisions | Deferred startup, background maintenance, refresh jobs | Should become a lifecycle/startup controller after import/restore is isolated. |
| Selected date and calendar | `JournalViewModel` with selected-date helpers | Selected date, entries, labels, calendar dates | Selected-date load and calendar jobs | Needs careful request-id preservation so stale loads do not overwrite newer state. |
| Todos and events | `TodoIndexRepository`, `EventIndexRepository`, `JournalViewModel` facade | Todo/event lists and selected filters | Repository persistence scopes plus facade jobs | Keep repository ownership; extract facade actions later. |
| Revisits, favorites, labels | `JournalViewModel` with revisit/label helpers | Starred dates, labels, revisit markers, due revisits | Lookback/highlight refresh jobs | Should move after selected-date ownership is clearer. |
| Statistics and heatmap | `JournalViewModel` with statistics helpers | Monthly/all-time stats, heatmap, progress, comparison, contribution cache | Statistics, heatmap, monthly stats jobs | Larger extraction because it owns freshness budgets and incremental update mutexes. |
| Recurring tasks | `RecurringTaskRepository` plus `JournalViewModel` facade | Active recurring tasks | Repository IO work through facade launches | Repository already owns core recurrence logic; facade extraction can be small. |

## Guardrails

- Keep existing screen-facing API stable until a screen is intentionally migrated.
- Each feature owner should own its mutable state, jobs, cancellation rules, and repository references.
- Keep ViewModel-scoped feature work in `viewModelScope`; use application scope only for work that must survive ViewModel destruction.
- Preserve shared toast and external navigation event streams while the facade remains.
