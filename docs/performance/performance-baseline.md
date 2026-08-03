# Performance Baseline

Created: 2026-08-03

This project now has lightweight instrumentation for startup and selected background work. The instrumentation has two outputs:

- Android trace sections for Perfetto / Android Studio profiling.
- Existing logcat lines in the form `perf:<phase>=<elapsed>ms`.

## Instrumented Sections

Startup and app creation:

- `YajaApplication.onCreate`
- `YajaApplication.widgetAppearanceSetup`
- `MainActivity.onCreate`
- `MainActivity.setContent`
- `MainActivity.handleExternalOpenIntent`
- `startup.bootstrap`
- `startup.primeCachesFromDisk`
- `startup.readCachedState`
- `startup.useCachedDates` or `startup.loadLightweightDates`
- `startup.getTotalEntryCount`
- `startup.total`

Deferred and background startup:

- `startupQueue.<task>`
- `deferred.lookback`
- `deferred.highlights`
- `deferred.monthlyStats`
- `deferred.total`

Other currently logged paths:

- `loadEntries`
- `monthlyStats`
- `statistics.total`
- `heatmapData`
- `HeatmapWidget` phases such as `updateWidget`, `resolveWordCounts.cached`, and `resolveWordCounts.fresh`

## Local Checks

Compile and unit checks:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest --tests com.mj.yaja.ui.screens.SettingsSearchMatcherTest --tests com.mj.yaja.localization.StringResourceAuditTest
```

Compose compiler stability and recomposition reports:

```powershell
.\gradlew.bat :app:assembleDebug -PcomposeMetrics=true
```

Reports are written under:

```text
app/build/compose_compiler/
```

## Phone Baseline Capture

These commands update neither app data nor journal files. They only collect runtime observations from an installed build.

Clear logcat, launch manually from the phone, then collect perf lines:

```powershell
adb logcat -c
adb logcat -s JournalViewModel:D HeatmapWidget:D MainActivity:D YajaApplication:D *:S
```

For startup timing from the shell:

```powershell
adb shell am start -W -n com.mj.yaja/.MainActivity
```

For trace inspection, record a Perfetto or Android Studio System Trace while opening the app. Look for the trace section names listed above.

## Baseline Policy

- Use a warm-start run and a cold-start run when comparing changes.
- Record device model, Android version, journal size, selected storage type, and whether Large Journal Safe Mode is enabled.
- Compare median values across at least three launches.
- Treat `startup.bootstrap`, `loadEntries`, and `statistics.total` as the first high-signal metrics.
- Do not clear app data to create a baseline unless a separate destructive-test task explicitly allows it.
