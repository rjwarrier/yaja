# Yaja V2 Refactoring Roadmap

Status: Planning only  
Created: 2026-08-03  
Scope: App-wide optimization, maintainability, performance, and data-safety work

This roadmap is ordered by a combined effort-and-risk score. Where effort and risk conflict, data safety receives more weight.

## Current Baseline

- The six settings screens contain 57 lifecycle collectors:
  - Root Settings: 4
  - Appearance: 15
  - Journal Experience: 14
  - Navigation & Gestures: 11
  - Privacy & Security: 4
  - Data & Recovery: 9
- Settings search is a private list of hardcoded English strings, callbacks, and scroll requesters inside `SettingsScreen.kt`.
- Five settings destination cards duplicate nearly identical Compose code.
- Base resources contain 1,303 strings across 44 translated locales. Six locales are missing 175 strings; the other 38 are missing 191. No locale has unknown extra keys.
- `JournalViewModel.kt` has roughly 3,000 lines, 188 functions, 30 local mutable flows, and 83 direct `SettingsRepository` flow pass-throughs.
- `MarkdownFileManager.kt` is about 2,200 lines and owns storage, caching, frontmatter, mutations, backup, search, indexes, migration, fingerprints, and version-history coordination.
- Only five JVM test files exist; there is currently no `androidTest` suite.
- `JournalDatabase.kt` uses both `allowMainThreadQueries()` and `fallbackToDestructiveMigration()`.
- Migrations cover database versions 2 through 8, but the repository does not contain a 1-to-2 migration.
- Recurring-task definitions in `compliance_masters` are authoritative user data stored only in Room. The current explicit backup ZIP does not include them. Therefore, the existing destructive fallback can lose recurring schedules, not merely disposable caches.

## Ordered Roadmap

Effort scale: 1 = hours; 5 = major multi-stage refactor.  
Risk scale: 1 = cosmetic/tooling; 5 = startup or user-data risk.

| Order | Work | Effort | Risk | Combined |
|---:|---|---:|---:|---:|
| 1 | Shared settings destination card | 1 | 1 | 2 |
| 2 | Translation and string-health audit | 2 | 1 | 3 |
| 3 | Structured settings search/navigation registry | 2 | 2 | 4 |
| 4 | Performance instrumentation and baselines | 3 | 1 | 4 |
| 5 | Per-screen settings UI-state holders | 3 | 2 | 5 |
| 6 | Focused data-safety regression tests | 4 | 1 | 5 |
| 7 | Remove main-thread Room access | 4 | 3 | 7 |
| 8 | Split `JournalViewModel` by feature ownership | 5 | 3 | 8 |
| 9 | Remove destructive database fallback safely | 4 | 5 | 9 |
| 10 | Split `MarkdownFileManager` responsibilities | 5 | 5 | 10 |

## 1. Shared Settings Destination Card

Estimated effort: half a day. Risk: very low.

### Implementation

- Introduce one `SettingsDestinationCard` composable in the common settings components package.
- Accept an icon, title resource, subtitle resource, click callback, and optional modifier.
- Centralize padding, shape, colors, elevation, typography, icon sizing, and expressive press animation.
- Treat the icon as decorative with `contentDescription = null`; the visible title and subtitle already provide accessibility context.
- Replace the five duplicated destination cards:
  - Appearance
  - Journal Experience
  - Navigation & Gestures
  - Privacy & Security
  - Data & Recovery
- Let the parent layout own vertical spacing instead of each card adding a trailing spacer.
- Preserve the current order, padding, colors, and navigation behavior.

### Verification

- Compare Compose previews or screenshots in light, dark, and large-font modes.
- Verify identical press animation and tap targets across all five cards.
- Verify TalkBack announces each card once.
- Compile, run unit tests, and assemble the debug APK.

### Exit Criterion

All five destinations use the shared component and no duplicate destination-card implementation remains.

## 2. Translation and String-Health Audit

Estimated effort: one to two days for the audit and mechanical corrections. Risk: very low. Human-quality translation of every missing string is a separate localization workload.

### Implementation

- Add a reproducible resource-audit task or script reporting:
  - missing keys by locale
  - unexpected and duplicate keys
  - format-placeholder mismatches
  - invalid apostrophe or escape usage
  - untranslated values identical to English
  - missing plurals
- Record the current baseline of 1,303 base strings, 44 translated locales, and 175 to 191 missing strings per locale.
- Classify hardcoded strings:
  - Move user-visible UI strings to resources.
  - Move search labels and section names to resources as part of Stage 3.
  - Move toasts, chooser titles, error messages, and privacy-dashboard summaries to resources.
  - Keep internal logs, file-format tokens, Markdown markers, and developer diagnostics unlocalized.
- Mark true technical literals with `translatable="false"`.
- Validate `%s`, `%d`, positional parameters, and markup across locales.
- Run Android lint with `MissingTranslation`, `HardcodedText`, and relevant resource checks.
- Exercise expanded-text and RTL pseudo-locales.
- Generate a translation worksheet grouped by feature and locale.
- Do not ship bulk unreviewed machine translations. Missing locale strings should continue to fall back safely to base English until reviewed.

### Verification

- Compile resources for every locale.
- Run `lintDebug`.
- Inspect Search and Language screens in pseudo-locales.
- Confirm no broken format placeholders.
- Commit the audit automation and baseline report.

### Exit Criterion

Localization debt is measurable, visible hardcoded strings are catalogued or corrected, and new missing keys are detected automatically.

## 3. Structured Settings Search and Navigation Registry

Estimated effort: one to two days. Risk: low.

### Implementation

- Replace the private `SettingsSearchTarget` containing lambdas and requesters with stable models such as:
  - `SettingsDestinationId`
  - `SettingsSearchEntry`
  - `SettingsSearchAction`
- Support action types such as:
  - `OpenRoute(destination)`
  - `ScrollTo(anchor)`
  - `OpenDirectDestination(destination)`
- Store string-resource IDs for titles, sections, and searchable synonyms instead of hardcoded English.
- Store search history by stable destination ID rather than display title so language changes do not invalidate history.
- Move matching into a pure Kotlin search engine that normalizes case and whitespace, optionally strips accents, and ranks exact or prefix matches above substring matches.
- Keep `BringIntoViewRequester` instances inside the screen and map `ScrollTo(anchor)` actions to them.
- Resolve route actions to navigation callbacks at the screen boundary.
- Preserve routing to the new Appearance, Journal Experience, Navigation & Gestures, Privacy & Security, and Data & Recovery screens.
- Remove the unkeyed remembered list of callbacks to avoid stale callback capture.

### Tests

- Exact, prefix, substring, case-insensitive, and accent-insensitive matching.
- Stable search history across language changes.
- Every registry entry resolves to a valid action.
- Representative results for Appearance, Data & Recovery, Privacy, Language, Review, integrations, and Help.
- No-result and duplicate-key behavior.

### Exit Criterion

Search data is centralized, localized, pure-testable, and independent of Compose requesters or navigation lambdas.

## 4. Performance Instrumentation and Baselines

Estimated effort: two to three days. Risk: low.

### Implementation

- Build on the existing startup `logPerf` hooks instead of introducing a competing logging system.
- Add a small structured `PerformanceRecorder` using monotonic elapsed time.
- Record operation name, duration, item or date count, cache/disk source, and success/cancel/error category.
- Never log journal text, keyword contents, custom-storage paths, or imported data.
- Instrument:
  - cold startup and cache priming
  - selected-date loading
  - calendar refresh
  - todo and event index rebuilding
  - keyword indexing
  - search
  - statistics and heatmap generation
  - backup creation
  - restore and import processing
  - storage-location migration
- Add debug-only StrictMode detection for disk and database access on the main thread.
- Continue using the existing opt-in Compose compiler metrics.
- Generate isolated synthetic journal fixtures of approximately 100, 1,000, and 5,000 dates.
- Establish baselines for startup to first usable content, date switching, calendar refresh, index rebuilds, search, and import/restore memory use.
- Initially flag changes that regress measured operations by more than approximately 10 to 15 percent rather than inventing unsupported absolute targets.

### Verification

- Debug logs contain timings without user content.
- Release builds have negligible or disabled instrumentation overhead.
- Benchmarks use generated data in an emulator or test environment, never the connected personal phone.
- Commit the baseline report separately from later performance changes.

### Exit Criterion

Architectural optimization work can be evaluated against measured bottlenecks and regression budgets.

## 5. Per-Screen Settings UI-State Holders

Estimated effort: two to three days. Risk: low to moderate.

### Implementation

- Introduce immutable state models:
  - `RootSettingsUiState`
  - `AppearanceSettingsUiState`
  - `JournalExperienceSettingsUiState`
  - `NavigationGesturesSettingsUiState`
  - `PrivacySecuritySettingsUiState`
  - `DataRecoverySettingsUiState`
- Combine repository and ViewModel flows and expose lifecycle-aware `StateFlow` objects.
- Reduce each settings screen to normally collecting one state object.
- Keep transient UI state in Compose, including sheets, search text, dialogs, pending SAF selection, and expanded controls.
- Keep durable preferences and work progress in repositories or the ViewModel.
- Preserve current action methods initially so this stage does not become an architecture rewrite.
- Pass smaller state slices to subsections to retain Compose skipping behavior.
- Mark stable state models `@Immutable`.
- Implement one screen at a time in this order:
  1. Privacy & Security
  2. Root Settings
  3. Navigation & Gestures
  4. Journal Experience
  5. Data & Recovery
  6. Appearance

### Verification

- Reduce the settings collector count from 57 to approximately six.
- Confirm defaults match current saved settings.
- Verify a preference update does not reset dialogs or scroll state.
- Preserve Appearance previews and Data & Recovery progress states.
- Inspect Compose stability reports for newly unstable parameters.

### Exit Criterion

Each settings screen has one coherent state contract without changing persisted preference behavior.

## 6. Focused Data-Safety Regression Tests

Estimated effort: three to five days. Risk: low. This is the mandatory gate for Stages 7 through 10.

### Backup and Restore

- Current ZIP round trip and legacy compatibility.
- Valid, invalid, truncated, and corrupt manifests or ZIPs.
- Duplicate journal paths.
- Existing-day merge without overwriting unique entries.
- Frontmatter, stars, labels, and revisit metadata preservation.
- Shortcode, date-keyword, and People & Places conflict behavior.
- Cancellation, failure handling, and temporary-output cleanup.
- Recurring-task coverage before Stage 9.

### Imports

- Day One: valid entries, malformed dates, timezone boundaries, multiline text, duplicates, and cancellation.
- Journalistic: valid and malformed variants, duplicate handling, and merge counts.
- Markdown folder: valid date paths, unrelated files, malformed Markdown, nested folders, and existing-day merges.

### Date Keywords

- Today, tomorrow, and weekday calculations.
- Date-order preference.
- Custom keyword collisions.
- Case and whitespace normalization.
- Invalid definitions.
- Month/year boundaries and leap day.

### Todos and Carry-Forward

- Carry unchecked todos only and preserve checked todos.
- Idempotent repeated execution.
- Preserve multiline entries and stable source-date markers.
- Avoid duplicates after interrupted refresh.
- Ensure mutation failure leaves the source day unchanged.
- Confirm the todo index matches resulting Markdown.

### Room Migrations

- Add Room migration-test support and schema exports.
- Test every supported version-to-current path.
- Verify authoritative rows, not merely table existence.

### Testing Environment

- Use JVM tests wherever Android dependencies can be separated.
- Use Robolectric or an emulator for Room and ContentResolver behavior.
- Never run instrumentation against the connected personal phone.

### Exit Criterion

Backup, import, migration, and carry-forward tests pass before threading or persistence changes begin.

## 7. Remove Main-Thread Room Access

Estimated effort: three to five days. Risk: moderate.

### Implementation

- Inventory every synchronous DAO path, including cache snapshots, counts, keyword-match reads, recurring-task reads, and immediate cache writes.
- Classify operations:
  - Authoritative writes must be awaited on `Dispatchers.IO`.
  - Cache/index writes may be queued after the canonical Markdown write succeeds.
  - UI-facing reads should come from StateFlow, memory state, or suspend repository APIs.
- Convert applicable DAO methods to `suspend`.
- Use Room KTX `withTransaction` for suspendable transactions.
- Replace synchronous database access in `MarkdownFileManager` with memory snapshots or explicit suspend IO APIs.
- Redefine the cache-write `immediate` flag so it means awaited on IO rather than executed on the caller thread.
- Audit widgets, Tasker receivers, Quick Capture, Quick Todo, and startup paths as well as Compose screens.
- Enable debug StrictMode during the conversion.
- Remove `allowMainThreadQueries()` only after no call path depends on it.

### Verification

- Run the full Stage 6 suite.
- Confirm StrictMode reports no database or journal-file IO on the main thread.
- Keep startup and date switching within the Stage 4 regression budget.
- Stress rapid add, edit, delete, and todo operations for ordering and cache consistency.

### Exit Criterion

Room rejects main-thread access and the app operates without hidden exceptions or UI stalls.

## 8. Split `JournalViewModel` by Feature Ownership

Estimated effort: six to nine days. Risk: moderate to high.

The ViewModel should remain a compatibility facade while feature ownership moves behind it incrementally.

### Implementation

1. Document a state and job ownership matrix.
2. Continue using the existing `EntryCoordinator` and `KeywordCoordinator`.
3. Extract feature controllers in this order:
   - settings state and actions
   - backup, import, and restore
   - app lifecycle and startup refresh
   - selected date and calendar
   - todos and events
   - revisits, favorites, and labels
   - statistics and heatmap
   - recurring tasks
4. Give each controller ownership of its mutable state, jobs, cancellation rules, and repositories.
5. Delegate existing public properties and methods from `JournalViewModel` so screens do not need to change simultaneously.
6. Replace new singleton lookups with constructor-injected dependencies.
7. Keep feature work in `viewModelScope`; use a longer-lived application scope only where work must legitimately survive ViewModel destruction.
8. Preserve external toast and navigation event streams.
9. Move nested import/restore models into their owning feature packages.
10. Extract one feature per independently passing commit.

### Verification

- Preserve public behavior after every extraction.
- Verify ViewModel recreation cancels feature work correctly.
- Ensure imports, statistics, indexing, and date loads do not overwrite newer state.
- Keep existing screens compiling against the facade.
- Remain inside measured performance budgets.

### Exit Criterion

Feature state and jobs have clear owners, and `JournalViewModel` primarily orchestrates and delegates.

## 9. Remove Destructive Database Fallback Safely

Estimated effort: four to six days. Risk: critical because Room contains authoritative user data.

### Implementation

1. Enable Room schema export and commit schema JSON from version 8 onward.
2. Add migration tests for every supported production starting version.
3. Inspect release tags and APK history to establish the oldest database version that must upgrade. The repository currently proves only versions 3 through 8; version 1-to-2 remains unknown.
4. Classify tables:
   - Authoritative: recurring-task masters and current keyword definitions.
   - Rebuildable: journal-day cache, todos, events, and keyword matches.
   - Generation rows require an explicit reconstruction policy.
5. Extend the explicit backup format to version 3:
   - include recurring-task definitions
   - preserve stable IDs
   - restore definitions with conflict handling
   - decide whether generation rows are restored or rebuilt from Markdown compliance markers
6. Add an export and recovery path before changing fallback behavior.
7. Add every required explicit Room migration.
8. Provide an intentional cache reset/rebuild operation for derived tables after a successful database open.
9. Fail closed for unsupported or corrupt authoritative data:
   - do not silently delete the database
   - preserve or quarantine the original database
   - present a recovery explanation
10. Remove `fallbackToDestructiveMigration()` only after upgrade fixtures prove data preservation.

### Verification

- Upgrade copies from every supported database version.
- Compare keyword and recurring-task rows before and after migration.
- Ensure derived indexes survive or rebuild deterministically from Markdown.
- Verify backup/restore includes recurring schedules.
- Confirm unsupported-version handling does not delete the database.
- Keep journal Markdown untouched during all migration tests.

### Exit Criterion

No normal upgrade path can silently destroy recurring schedules or keyword definitions.

## 10. Split `MarkdownFileManager` Responsibilities

Estimated effort: eight to twelve days. Risk: critical because this is the journal write path.

`MarkdownFileManager` should remain a delegating facade until all consumers have migrated.

### Extraction Order

1. `JournalStorage`
   - Continue isolating local and SAF file access.
   - Pass storage configuration explicitly instead of reading `SettingsRepository` internally.
2. `JournalCacheCoordinator`
   - In-memory entries
   - Room cache
   - fingerprints
   - incremental warmup
   - cache invalidation
3. `JournalMetadataRepository`
   - frontmatter
   - stars and labels
   - day labels
   - revisits
4. `JournalMutationService`
   - per-date locks
   - add, update, delete, and reorder
   - atomic writes
   - version snapshots
   - post-write cache/index refresh
5. `JournalQueryService`
   - search
   - metrics snapshots
   - calendar and date discovery
6. `JournalBackupGateway`
   - backup snapshots
   - storage migration
   - restore integration
7. Move todo, event, and keyword indexing coordination outside physical file storage where practical.
8. Migrate consumers gradually:
   - ViewModel controllers
   - widgets
   - Quick Capture
   - Quick Todo
   - Tasker receiver
9. Remove the facade only after every consumer uses narrower interfaces.

### Required Invariants

- Markdown remains canonical.
- A successful file write is not reported before the atomic write finishes.
- A cache failure cannot erase or invalidate a successful Markdown write.
- Mutations for the same date remain serialized.
- Writes to different dates may proceed independently.
- SAF permission failures return clear errors without pretending memory state was persisted.
- Widgets and Tasker continue seeing mutations made by the main app.
- Storage URI becomes an explicit dependency, preparing for but not implementing future multi-journal support.

### Verification

- Inject failures for open, write, rename, database update, and cache refresh operations.
- Test concurrent mutations.
- Compare large synthetic-journal performance with the baseline.
- Run the full backup, restore, and import suite.
- Compile and verify widget and Tasker integrations.
- Defer manual phone testing until the entire roadmap is complete.

### Exit Criterion

No single class owns storage, cache, metadata, backup, indexing, and mutations, while current data-safety behavior remains intact.

## Execution and Data-Safety Protocol

When implementation begins:

1. Create and verify a fresh source-only ZIP from the exact starting commit.
2. Preserve earlier source backups separately.
3. Exclude build output, `.gradle`, IDE caches, temporary files, APKs, and signing secrets.
4. Inspect repository status before every stage and preserve unrelated user files.
5. Implement one stage or clearly bounded substage at a time.
6. After every completed stage:
   - inspect the complete diff
   - run relevant host tests
   - compile Kotlin
   - run lint where applicable
   - assemble the debug APK
   - commit
   - push to GitHub
7. Never use the connected personal phone for automated or instrumentation tests.
8. Never run `adb uninstall`, `pm clear`, delete app storage, or restore a test backup onto the live journal.
9. Use update-in-place installation only: `adb install -r`.
10. Do not automatically launch or exercise the installed app.
11. Provide a final manual phone-test checklist after all implementation work is complete.
12. Test restore and import on an emulator, spare profile, or backed-up test journal rather than the live journal.

## Final Phone-Test Checklist

After the complete roadmap is implemented and installed through an update-in-place operation, manually verify:

- Upgrade launch and visibility of existing entries.
- Settings cards, search routing, and language selection.
- Add, edit, delete, multi-delete, and undo.
- Todos, events, carry-forward, and recurring schedules.
- Calendar, search, statistics, and heatmap.
- Stars, labels, revisits, and version history.
- Backup creation without restoring over the live journal.
- Custom SAF storage access.
- Widgets, Quick Capture, Quick Todo, and Tasker behavior.
- App restart with PIN, biometric, and auto-lock settings where enabled.

Stages 7 and 9 provide the largest immediate performance and data-safety gains, but they must not begin until Stages 4 and 6 establish measurement and regression protection. Stages 8 and 10 provide the largest long-term maintainability improvement.
