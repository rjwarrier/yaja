# i18n Externalization Handover

## What this task is

This is an Android app written in Kotlin using Jetpack Compose for the UI. The app supports 4 languages: English (default, `values/strings.xml`), Spanish (`values-es/strings.xml`), Portuguese (`values-pt/strings.xml`), and French (`values-fr/strings.xml`).

The overall effort is to find every hardcoded, user-visible English string in the Kotlin UI code and:
1. Move it into `values/strings.xml` as a `<string>` (or `<plurals>`) resource.
2. Add a matching, **directly written** translation of that same resource to `values-es/strings.xml`, `values-pt/strings.xml`, and `values-fr/strings.xml`.
3. Replace the hardcoded string in the `.kt` file with `stringResource(R.string.xxx)` (or `pluralStringResource(...)` for plurals).
4. Verify the project still compiles.

"User-visible" includes: `Text("...")`, `contentDescription = "..."`, dialog titles/messages/buttons, placeholder text, snackbar/toast messages, content of `buildAnnotatedString { append("...") }`, and string templates like `"$count items deleted"`.

"Not user-visible" (leave alone): internal route/navigation keys, log messages, `DateTimeFormatter.ofPattern(...)` pattern strings (the pattern itself, not the formatted output), map/dictionary keys used only for comparison, debug-only strings.

## Translation requirement — read this carefully

**You must write real Spanish, Portuguese, and French translations yourself, directly in the XML files, for every single new string resource.** Do not:
- leave the English text as a placeholder in es/pt/fr files
- write `TODO: translate` or similar
- ask the user to provide translations
- skip a locale "for now"

If you are unsure of a translation, use your best natural-sounding translation — a slightly imperfect translation is far better than a missing one or an English placeholder. Keep the tone casual/friendly to match the existing app strings (check existing translated strings in each locale file for tone reference).

## Step-by-step workflow for each screen/group

Work one screen (or logical group of related files) at a time. Suggested order: CalendarScreen group next, then continue to any other screens not yet done (search broadly — see "How to find remaining work" below).

For each group:

1. **Read the target `.kt` file(s) in full.** Identify every hardcoded user-visible string, as described above.

2. **Check for existing resources before adding new ones.** This is critical — search all 4 `strings.xml` files for:
   - The exact English text you're about to add (it might already exist under a different name, e.g. from the Settings, AddEntry, Home, or NavDrawer batches that are already done).
   - Common reusable resources already defined near the top of `values/strings.xml`: `action_save`, `action_cancel`, `action_delete`, `action_edit`, `action_back`, `action_undo`, `action_yes`, `action_no`, `action_remove`, `action_close`, `action_done`, `action_set`, `action_ok`, and nav-related ones like `nav_help`, `nav_journal`, `nav_calendar`, `nav_lookback`, `nav_settings`, `nav_shortcodes`.
   - If a matching resource already exists, **reuse it** — do not create a duplicate. Adding a `<string name="x">` that already exists elsewhere in the same file will make the Android build FAIL with an error like:
     ```
     ERROR: ...strings.xml: Resource and asset merger: Found item String/x more than one time
     ```

3. **Pick resource names** using a screen-prefixed naming convention, matching what's already used:
   - `home_*` for HomeScreen
   - `nav_*` for NavDrawer
   - `addentry_*` for AddEntry screens
   - `settings_*` for Settings screens
   - For Calendar, use `calendar_*`
   - For format strings (containing a variable), suffix with `_format`, e.g. `calendar_entries_count_format`
   - For content descriptions, prefix with `_cd_`, e.g. `calendar_cd_today_button`

4. **Add the new resources to all 4 `strings.xml` files**, each in a clearly-commented section (e.g. `<!-- Calendar: month header -->`), in this order: `values`, then `values-es`, `values-pt`, `values-fr`. Keep the same resource names and order across all 4 files so they stay easy to compare.

   - **Plurals**: if a string changes based on a count (singular vs plural, e.g. "1 entry" vs "3 entries"), use `<plurals>`:
     ```xml
     <plurals name="calendar_entries_count">
         <item quantity="one">%1$d entry</item>
         <item quantity="other">%1$d entries</item>
     </plurals>
     ```
     Note: Spanish/Portuguese/French also support `quantity="one"`/`"other"` — write natural singular/plural forms for each language.

   - **Format strings with variables**: use positional placeholders `%1$s` (string), `%1$d` (integer), `%2$s` for a second argument, etc. If you need a literal `%` character in the text, write it as `%%`.

   - **French apostrophes**: ANY apostrophe (`'`) you write inside `values-fr/strings.xml` — including inside `<plurals><item>` elements — MUST be escaped as `\'` (backslash-apostrophe). For example, write `l\'index`, not `l'index`. If you forget this, the build will fail later with a confusing error like:
     ```
     java.lang.NullPointerException: Cannot invoke "javax.xml.stream.events.Attribute.getValue()" because the return value of "javax.xml.stream.events.StartElement.getAttributeByName(...)" is null
     ```
     during the `mergeDebugResources` Gradle task. If you ever see this exact error, the cause is almost always an unescaped `'` you just added to the French file — search your recent edits to `values-fr/strings.xml` for stray `'` characters and escape them.

   - **`&` characters**: in XML, write `&amp;` instead of a bare `&` (e.g. "People & Places" → "People &amp; Places").

5. **Edit the `.kt` file(s)**:
   - Add these imports if not already present:
     ```kotlin
     import androidx.compose.ui.res.stringResource
     import androidx.compose.ui.res.pluralStringResource // only if you used <plurals>
     import com.mj.yaja.R
     ```
   - Replace each hardcoded string:
     - Plain text: `Text("Some text")` → `Text(stringResource(R.string.calendar_some_text))`
     - With a variable: `Text("Hello $name")` → `Text(stringResource(R.string.calendar_hello_format, name))` where `calendar_hello_format` = `Hello %1$s`
     - Plural: `Text(if (count == 1) "1 item" else "$count items")` → `Text(pluralStringResource(R.plurals.calendar_items_count, count, count))`
     - Content description: `contentDescription = "Today"` → `contentDescription = stringResource(R.string.calendar_cd_today)`

   - **Composable-context rule**: `stringResource()` and `pluralStringResource()` can ONLY be called directly inside a `@Composable` function body — NOT inside a plain lambda like `onClick: () -> Unit`, `onShareApp: () -> Unit`, or inside `Intent.apply { ... }` blocks, even if that lambda is defined inside a composable. If you need a string inside one of these plain lambdas:
     1. In the composable function's body, BEFORE the lambda is defined, add a line like:
        ```kotlin
        val shareTitle = stringResource(R.string.calendar_share_title)
        ```
     2. Then inside the lambda, reference `shareTitle` (the captured local variable) instead of calling `stringResource()` again.

   - **Internal keys used for comparison, not just display**: sometimes a string is BOTH used as an internal key (e.g. compared with `if (label == "Vers")`) AND shown to the user via `Text(label)`. In this case:
     - Do NOT change the internal key/comparison — leave `"Vers"` (or whatever the English key is) exactly as-is in the comparison logic.
     - Instead, write a small private `@Composable` helper function that maps the internal key to a translated display string, and call that helper only where the value is displayed. Example pattern (already used in `HomeScreenSections.kt` as `statLabel()`):
       ```kotlin
       @Composable
       private fun statLabel(label: String): String =
           when (label) {
               "Entries" -> stringResource(R.string.home_stat_entries)
               "Words" -> stringResource(R.string.home_stat_words)
               else -> label
           }
       ```

   - **Non-composable helper functions that need to return localized text**: if a plain Kotlin function (not `@Composable`) builds a display string (e.g. an "X minutes left" estimator), and all of its call sites are inside `@Composable` functions, you can mark the function itself `@Composable` and use `stringResource()` inside it directly. (This was done for `estimateNavRemainingTimeText` in `NavDrawerSupport.kt`.)

6. **Build verification.** From the repo root (`Z:\OfficeDropbox\Dropbox\Ranjith Jayadevan\Yaja\Yaja_V2`), in PowerShell, run:
   ```
   .\gradlew.bat :app:compileDebugKotlin --console=plain
   ```
   Run this as a background/async task if your tooling supports it — the `kspDebugKotlin` step can take 1-2 minutes. Check for exit code 0.

   - If it fails on `packageDebugResources` with "Found item String/xxx more than one time" → you created a duplicate resource name. Find the duplicate (search the file for `name="xxx"`) and remove your new copy, reusing the existing one instead, then update the `.kt` reference if needed.
   - If it fails on `mergeDebugResources` with the `NullPointerException`/`StartElement.getAttributeByName` error described above → you have an unescaped apostrophe in `values-fr/strings.xml`. Find and fix it (escape as `\'`).
   - If it fails with a Kotlin compile error (unresolved reference, type mismatch, etc.) → fix the Kotlin code directly.

   After a full screen/group is done and compiles cleanly, you can optionally also run:
   ```
   .\gradlew.bat :app:installDebug --console=plain
   ```
   to install the updated build on the connected device/emulator (this also re-runs the full resource merge as an extra check).

## How to find remaining work

Already fully done (do not redo): Settings screens, AddEntry screens, HomeScreen, NavDrawer, CalendarScreen group.

### Known pending areas — work through these first, in roughly this order:

1. **`HelpContent.kt`** (and any related Help screen files, e.g. `HelpScreen.kt`) — likely contains long blocks of help/explanation text, FAQ-style content, and section headers. Read the whole file; there may be a lot of text here, so budget extra time. Use sensible `help_*` resource names, grouping by section (e.g. `help_section_backup_title`, `help_section_backup_body`).

2. **`statistics/StatisticsComponents.kt`** — statistics screen labels, chart axis labels/legends, stat card titles, empty-state text, content descriptions for icons/charts. Use `statistics_*` resource names. Watch for internal comparison keys here too (similar to the `statLabel()` pattern in HomeScreenSections.kt) — statistics screens often reuse the same label strings as keys for filtering/grouping logic.

3. **Todo components — remaining strings**: look for hardcoded strings like `"Entry"`, `"Event"`, and content descriptions for the add-todo button/FAB (e.g. `contentDescription = "Add Todo"`). Search files under `app/src/main/java/com/mj/yaja/ui/` whose names contain `Todo`. Use `todo_*` resource names, e.g. `todo_cd_add_todo`, `todo_label_entry`, `todo_label_event`.

4. **Scattered content-description leftovers across multiple screens**: search broadly for `contentDescription = "Back"`, `contentDescription = "Delete"`, `contentDescription = "Import"`, `contentDescription = "Export"` (and similar verbs) across the whole `ui/` tree — these are likely simple, reusable, and may already have equivalents in `action_*` resources (e.g. `action_delete`, `action_back` may already exist as text strings — check whether a `_cd_` variant is needed or whether the existing `action_*` string can be reused directly as a content description too). Fix each occurrence file-by-file.

5. **Timeline / rebuild-cache / keywords "shell" leftovers**: there are a few remaining hardcoded strings in:
   - Timeline-related screen/components (search for files with `Timeline` in the name)
   - The "rebuild cache" screen/dialog (search for `RebuildCache` or `rebuild_cache` in `ui/`)
   - The Keywords/People & Places screen "shell" wrapper components (search for files with `Keyword` in the name)

   These are likely small, isolated strings (titles, button labels, status messages, empty states) — handle each as its own mini-batch.

### General search strategy for anything not listed above

After working through the list above, do a final sweep: grep across `app/src/main/java/com/mj/yaja/ui/` for patterns like:
  - `Text("` (a literal string passed directly to `Text`)
  - `contentDescription = "`
  - `"...$` (string templates with interpolation that look user-facing)
  - `title = "`, `message = "`, `placeholder = "`, `label = "`

For each match, read the surrounding code to confirm it's genuinely user-visible (not a route name, log tag, or internal key) before externalizing it. Skip files entirely inside test directories (`app/src/test/...`) — those don't need translation.

## Task tracking

Use your task-tracking tool (TaskCreate/TaskUpdate or equivalent) to track progress per group, using this naming pattern (tasks #1-30 already exist and cover Settings/AddEntry/Home/NavDrawer, all completed):
- "Add <ScreenName> string resources to 4 locale files"
- "Externalize <FileName>.kt strings"
- "Build verification for <ScreenName> i18n"

Mark each task `in_progress` when starting, `completed` only once the build verification for that group passes.

## Standing constraints (do not violate)

- Write all es/pt/fr translations directly and completely — no placeholders, no "leave for review", no skipping a locale.
- Do not add code comments, refactors, or new abstractions beyond what's strictly needed to externalize the string (the `statLabel()`-style helper described above is the one exception, and only when genuinely needed for an internal-key situation).
- Do not change any non-string logic, layout, or behavior.
- Keep resource naming consistent with the existing `home_*` / `nav_*` / `addentry_*` / `settings_*` conventions.
