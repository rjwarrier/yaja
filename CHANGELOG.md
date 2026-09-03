# Changelog

## 3.1.2 - Unreleased

### Highlights
- Added a Home dashboard screen — a hero "today" card, week strip, overview stats, and recent entries — as an alternative landing screen to the per-day Today view, selectable via a new Default Screen setting.
- Reworked the entry editor and navigation chrome so they hold up at large display sizes and large font scales, where the app has fewer dp to lay out and text outgrows the boxes holding it.
- Focus mode now turns itself on when the keyboard would leave the editor with barely a line of text, and says so.
- Added a UI size setting that scales the app's layout independently of its text.

### New
- Added a Home dashboard screen (`Route.Dashboard`) with a today hero card (greeting, status line, write/continue action, todos/events glance), a 7-day activity strip, flat overview stat tiles, and a Recent entries list with real preview/word/todo data pulled from the timeline metrics cache.
- Added a `Default Screen` setting to Journal Experience, choosing whether the app opens to Home or Today on launch.
- Long-pressing the bottom nav's journal icon toggles between Home and Today and persists the choice as the Default Screen setting, so it survives navigating away and app relaunch.
- Added a "Home" entry to the navigation drawer, and a small "Go to Today" FAB on the dashboard.
- Added a `UI Size` slider to Appearance, scaling every dp the way the system Display Size setting does while leaving text to the font size slider. Searchable under "display size", "ui scale", "zoom" and "bigger".
- Focus mode is enabled automatically, with a toast, when the keyboard leaves the editor under three lines of text. It applies once per entry, so turning it back off keeps it off.
- The focus button is tinted while focus mode is the app's doing rather than the user's, and returns to its plain style as soon as the toggle is touched.

### Improved
- Rebuilt the entry top bar as a three-slot row, so the title takes the width left by the action icons and ellipsizes rather than overlapping them.
- Turned the quick-insert chips into a single scrolling row sized to their labels, instead of fixed-width tiles that wrapped to a second row and clipped their text.
- Derived every bottom-panel height from one font-scale factor, so the bar, its indicator and its icons grow together and the indicator stays centred.
- Let the drawer version pill, the shortcodes button, the delete-selected pill, the palette cards and the appearance slider labels size to their own content.
- Focus mode now hides the date header card, which it previously left on screen.
- The focus-mode choice survives rotation instead of resetting and re-announcing itself.

### Fixed
- Fixed the current-streak stat showing 0 whenever today had no entry yet, even with an unbroken run of prior days; it now counts back from yesterday instead of zeroing out the moment today is unwritten.
- Fixed the dashboard's Recent list ranking future-dated entries (allowed by Allow Future Entries) above genuinely recent ones; future dates are now excluded.
- Fixed the Edit Entry title being drawn over the focus, help, template and delete icons; the centred title reserved a fixed 96dp per side against up to four action buttons.
- Fixed bottom-navigation indicators overlapping their neighbours: the tappable-size floor ignored how wide each slot actually was, which six enabled destinations on a large display size made visible.
- Fixed quick-insert chip labels clipping inside fixed-height tiles.

### Verification
- Added `AppDensityTest` covering the density math behind UI size: that UI size moves layout without moving text, that the font sliders still move text without moving layout, that the compensation cancels correctly through the journal-text path, and that a zero or non-finite scale cannot produce a zero or NaN density.
- Added `AddEntryFocusModeTest` covering the auto focus-mode decision, including its threshold boundary and its independence from the chrome it hides.
- Verified with `:app:testDebugUnitTest` (122 tests) and `:app:assembleDebug`.
- Verified the Home dashboard with `:app:compileDebugKotlin` and `:app:installDebug` plus manual on-device checks of the Default Screen setting, the long-press toggle surviving navigation and relaunch, and PIN-unlock respecting the setting; no new automated tests were added for it.

### Notes
- Requires a `versionCode` bump to 95; `3.1.1` shipped as 94.
- `settings_ui_size_label` and `addentry_focus_mode_hint` are base-locale only and still need translating, as does `settings_storage_move_confirm` for es, fr and pt.
- Translated the new dashboard/Default Screen strings (`dashboard_*`, `settings_default_screen_*`, `nav_home`) into all 44 shipped locales; the less common ones (am, gu, kn, mr, or, pa, sw, ta, te, zu) haven't been reviewed by a native speaker yet.

## 3.1.1 - August 28, 2026

### Highlights
- Rebuilt shortcode placeholder expansion so format codes resolve against a real date type and preview their output as you type.
- Added a dedicated share screen and moved the app's outbound links into a reorganized About screen.

### New
- Added a dedicated share screen with an editable message and an image toggle, and routed the hamburger-menu share button to it.
- Added other-apps cross-promo tiles plus GitHub and Share buttons to the About screen.
- Added Yata to the Other Apps cross-promo tiles.
- Added a version, build, and timestamp line to the About screen.
- Added live samples for format codes so a shortcode shows the value it will produce.

### Improved
- Restyled the Shortcodes screen and editor to match the app's M3 expressive language.
- Scoped placeholder expansion to shortcode values and seeded the format scaffold from the code text.
- Kept the expansion scaffold tracking the full shortcode as it is typed.
- Seeded the expansion only for codes that name a real date type, and warned on unresolvable types.
- Completed the shortcode translations.
- Moved the brand and footer block from the FAQ screen into the About screen, below the App Log.
- Reordered the About screen so cross-promo, GitHub, and Share sit between the App Log and Website, and renamed the Help card.
- Completed translations and hardened loading states.
- Shrank the search hint on narrow widths.

### Fixed
- Fixed the `@now` shortcode leaking literal placeholder text.
- Fixed shortcode rename leaving a stale duplicate entry.

### Release
- Bumped to `versionCode` 94, `versionName` 3.1.1, keeping `targetSdk` 36 and `minSdk` 26.
- Verified the release build with `:app:bundleRelease` and `:app:assembleRelease`.
- Confirmed the signed APK and AAB both report versionCode 94 and target API level 36 before publishing.
- Published the signed APK and AAB to the `v3.1.1` GitHub release.

## 3.1 - August 4, 2026

### Highlights
- Reworked Settings into a lighter hub so larger settings groups now open in their own focused screens.
- Preserved existing settings behavior while reducing how much state the main Settings screen has to load.

### New
- Added dedicated screens for `Journal Experience`, `Navigation & Gestures`, `Privacy & Security`, and `Data & Recovery`.
- Settings search now opens the matching dedicated settings screen for the newly extracted groups.
- Data & Recovery now owns its storage picker, backup restore picker, import pickers, import progress, cancel action, and restore summary dialog inside its own screen.

### Improved
- Removed the `Suggested Next Tweaks` card from Settings.
- Removed duplicate large section headings above the tappable settings destination cards.
- Moved the remaining smaller Settings root sections — Language, Review & Insights, Advanced Integrations, and Help & About — into their own focused screens.
- Promoted the Language settings screen into a direct searchable language list, removing the extra picker tap while keeping expressive selected-row animations.
- Reduced `SettingsScreen.kt` from about 1,020 lines to about 413 lines, and removed lifecycle state collection from the Settings root.
- Removed Room's main-thread query bypass and moved remaining synchronous cache, keyword, and recurring-task database access off the UI thread.
- Started the `JournalViewModel` feature-ownership split by extracting settings state composition and direct settings actions into `SettingsFeatureController`.
- Removed Room's destructive migration fallback, enabled schema export, added a defensive old-cache migration, and expanded backup ZIPs to preserve recurring-task schedules.
- Started the Stage 10 `MarkdownFileManager` responsibility split by moving journal search into a dedicated `JournalSearchService` facade dependency.
- Continued the Stage 10 split by moving read-only journal query snapshots and metrics calculations into `JournalQueryService`.
- Continued the Stage 10 split by moving backup ZIP creation, backup parsing, backup journal snapshots, and storage migration orchestration into `JournalBackupGateway`.
- Continued the Stage 10 split by moving frontmatter read queries and revisit aggregation into `JournalMetadataRepository`.
- Continued the Stage 10 split by moving entry add, insert, delete, update, set, and todo-line toggle mutation logic into `JournalMutationService` while preserving date-level locks in the facade.
- Continued the Stage 10 split by moving frontmatter write mutations for revisits, day labels, and starred dates into `JournalMetadataRepository` while preserving date-level locks in the facade.
- Continued the Stage 10 split by introducing `JournalCacheCoordinator` for cache statistics, lightweight date discovery, cached entry/label reads, fingerprint persistence, and fingerprint refresh scheduling.
- Continued the Stage 10 split by moving version-history snapshot listing and restore orchestration into `JournalMutationService`.
- Continued the Stage 10 split by moving Room cache priming, full cache population, hot-window refresh, incremental warmup, cache invalidation, and cache day persistence into `JournalCacheCoordinator`.
- Completed the Stage 10 `MarkdownFileManager` split by moving entry read/revalidation paths into `JournalCacheCoordinator`, todo/event index synchronization into `JournalIndexCoordinator`, and frontmatter scanning into `JournalMetadataRepository`.
- Removed the no-op disk-cache stubs left over from the Stage 10 split (and their now-dead constructor parameters in `JournalQueryService`/`JournalIndexCoordinator`).
- Hoisted three regexes that were being recompiled on every call in `AddEntryComponents`, `EntryRevisitCodec`, and `EntryCoordinator`, and cached the per-keyword mention-match regex used while typing.
- Guarded a per-mutation debug log and stripped `Log.d`/`v`/`i` calls from release builds via ProGuard.
- Fixed Appearance, Language, and Recurring Tasks animations that bypassed the user's Animation preference (Full/Reduced/Off).
- Fixed a linear keyword-lookup scan in `ObsidianExporter` that reran per match instead of once per export.

### Verification
- Created a source-only backup zip before implementation: `_code_backups/yaja-v2-source-20260803-194508.zip`.
- Created a source-only backup zip before the Room threading stage: `_code_backups/yaja-v2-source-20260804-062820.zip`.
- Created a source-only backup zip before the ViewModel ownership stage: `_code_backups/yaja-v2-source-20260804-063741.zip`.
- Created a source-only backup zip before the destructive-migration removal stage: `_code_backups/yaja-v2-source-20260804-065619.zip`.
- Created a source-only backup zip before the Stage 10 search extraction: `_code_backups/yaja-v2-source-stage10-20260804-073025.zip`.
- Created a source-only backup zip before continuing Stage 10: `_code_backups/yaja-v2-source-stage10-continuation-20260804-074548.zip`.
- Created a source-only backup zip before completing Stage 10: `_code_backups/yaja-v2-source-stage10-rest-20260804-083634.zip`.
- Created a source-only backup zip before moving the remaining Settings items to dedicated screens: `_code_backups/yaja-v2-source-settings-rest-20260804-085620.zip`.
- Created a source-only backup zip before promoting the Language screen picker: `_code_backups/yaja-v2-source-language-list-20260804-091717.zip`.
- Verified each extraction with local Kotlin compilation.
- Verified the Stage 10 search extraction with `JournalSearchServiceTest` and `:app:assembleDebug`.
- Verified the Stage 10 query/metrics extraction with `:app:compileDebugKotlin`.
- Verified the Stage 10 backup/migration gateway extraction with `:app:compileDebugKotlin`.
- Verified the Stage 10 metadata read/revisit extraction with `:app:compileDebugKotlin`.
- Verified the Stage 10 entry mutation extraction with `:app:compileDebugKotlin`.
- Verified the Stage 10 frontmatter mutation extraction with `:app:compileDebugKotlin`.
- Verified the Stage 10 cache coordinator extraction with `:app:compileDebugKotlin`.
- Verified the Stage 10 version-history mutation extraction with `:app:compileDebugKotlin`.
- Verified the Stage 10 cache warmup/population extraction with `:app:compileDebugKotlin`.
- Verified the completed Stage 10 split with `:app:compileDebugKotlin` and `:app:assembleDebug`.
- Verified the remaining Settings-screen extraction with `:app:compileDebugKotlin` and `:app:assembleDebug`.
- Verified the direct Language list screen with `:app:compileDebugKotlin` and `:app:assembleDebug`.
- Verified the final debug APK build with `:app:assembleDebug`.
- Installed the latest debug build to the connected device as an update, preserving app data.

## 2.87 - June 1, 2026

### Highlights
- Added `Events`, a new entry type for personal plans, reminders, appointments, and meaningful day markers.
- Events now feel native across Yaja instead of being treated like plain notes.

### New
- Add, edit, and save entries as `Event`.
- View events with distinct styling in the journal timeline.
- Browse events from the redesigned `Todos` screen using a dedicated `Events` mode.
- Event cards can show a time chip when a time is detected from the event text.
- Quick add now supports both `Todo` and `Event` with a chosen date.
- Tasker Quick Capture now supports `Add Event`.
- Todo widget can optionally include events.

### Improved
- Redesigned journal hero, date scroller, and day-entry layout.
- Redesigned calendar screen, including updated stats/progress visuals.
- Refined theme system with improved custom colors, personal themes, intensity, and background tint controls.
- Applied `Color intensity` and `Background tint` controls to Material theme colors as well.
- Reworked `Todos` screen with better summary cards, filters, and event-aware presentation.
- Improved expressive checkbox visuals and todo state animations.
- Updated help text to reflect Yaja's personal-journal focus and the new Events feature.

### Widgets And Shortcuts
- Quick add dialog now supports `Todo` or `Event`.
- Todo widget can mix todos and events in chronological order.
- Event rows in the widget are more visually distinct.
- Widget rows can open the exact date or entry in Yaja more reliably.
- Todo widget respects the `Render Checkboxes as Text` setting.

### Tasker
- Added `Append` mode for grouped capture flows such as missed calls.
- Added `Add Event` support to Yaja Quick Capture.

### Polish And Fixes
- Excluded `[ ]` and `[x]` from word and character counts.
- Improved settings organization, search, and help naming consistency.
- Cleaned up navigation, motion, and several M3-style UI details across the app.
