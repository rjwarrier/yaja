# Changelog

## Unreleased

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
- Kept smaller single-purpose sections, including Language, Review & Insights, Advanced Integrations, and Help & About, directly on the Settings root.
- Reduced `SettingsScreen.kt` from about 1,020 lines to about 536 lines, and reduced its lifecycle state collectors from 44 to 5.
- Removed Room's main-thread query bypass and moved remaining synchronous cache, keyword, and recurring-task database access off the UI thread.
- Started the `JournalViewModel` feature-ownership split by extracting settings state composition and direct settings actions into `SettingsFeatureController`.

### Verification
- Created a source-only backup zip before implementation: `_code_backups/yaja-v2-source-20260803-194508.zip`.
- Created a source-only backup zip before the Room threading stage: `_code_backups/yaja-v2-source-20260804-062820.zip`.
- Created a source-only backup zip before the ViewModel ownership stage: `_code_backups/yaja-v2-source-20260804-063741.zip`.
- Verified each extraction with local Kotlin compilation.
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
