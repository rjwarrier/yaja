# Timeline Feature — Implementation Plan

## Overview

Vertical scrollable timeline showing all journal dates with day labels. Accessible from Calendar screen. Dates with entries shown as nodes on a vertical line. Starred dates highlighted. Month separators as sticky headers. Filter chips for All / Starred / Labeled.

---

## Phase 1 — Route + Navigation Wiring

**Files to modify:**

**`Route.kt`** — Add new route:
```kotlin
data object Timeline : Route("timeline")
```
Don't add to `topLevel` set — Timeline is a child screen accessed from Calendar, not a peer tab.

**`JournalNavHost.kt`** — Add composable block:
```kotlin
composable(Route.Timeline.path) {
    TimelineScreen(
        viewModel = viewModel,
        onOpenDrawer = { ... },
        onNavigateToDate = { date ->
            viewModel.selectDate(date)
            navController.navigate(Route.Home.path) { popUpTo(Route.Home.path) { inclusive = true } }
        },
        onNavigateBack = { navController.popBackStack() },
        // ...standard nav lambdas
    )
}
```

**`CalendarScreen.kt`** — Add timeline button in top bar `actions` slot. Currently has one `FilledTonalIconButton` (EditCalendar for jump-to-date). Add second button:
```kotlin
FilledTonalIconButton(onClick = onNavigateToTimeline) {
    Icon(Icons.Rounded.Timeline, contentDescription = "Timeline")
}
```
Add `onNavigateToTimeline: () -> Unit` param to `CalendarScreen`.

---

## Phase 2 — ViewModel State

**Already available — no new ViewModel work needed:**

| Data | Source | Type |
|---|---|---|
| All dates with entries | `uiState.datesWithEntries` | `Set<LocalDate>` |
| Starred dates | `viewModel.favoritedDates` | `StateFlow<Set<String>>` |
| Day labels (all) | `viewModel.starredLabels` | `StateFlow<Map<LocalDate, String>>` |

All three are already computed and cached. Timeline just consumes them differently — no new data fetching needed.

**One optional addition** — expose entry count per month for month header badges. Could derive from `datesWithEntries` in the composable itself via `groupBy { YearMonth.from(it) }`. Cheap operation, no ViewModel change needed.

---

## Phase 3 — TimelineScreen Composable

**New file:** `app/src/main/java/com/mj/yaja/ui/screens/TimelineScreen.kt`

**Signature** (matches existing screen pattern exactly):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: JournalViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToDate: (LocalDate) -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToLookback: () -> Unit,
    onNavigateToShortcodes: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit
)
```

**Structure:**

```
Scaffold(
    topBar = CenterAlignedTopAppBar("Timeline", navigationIcon = AnimatedMenuButton)
) {
    Column {
        FilterChipRow(selectedFilter)     // All | Starred | Labeled
        LazyColumn(timelineItems)         // Main timeline
    }
}
```

---

## Phase 4 — Timeline Data Model + Grouping

**Sealed class for timeline items** (internal to TimelineScreen or separate file):

```kotlin
sealed class TimelineItem {
    data class MonthHeader(
        val yearMonth: YearMonth,
        val entryCount: Int
    ) : TimelineItem()

    data class DateNode(
        val date: LocalDate,
        val dayOfWeek: String,       // "Sat", "Mon"
        val label: String?,          // day label or null
        val isStarred: Boolean,
        val isToday: Boolean
    ) : TimelineItem()
}
```

**Filter enum:**
```kotlin
enum class TimelineFilter { ALL, STARRED, LABELED }
```

**Grouping logic** (inside composable, derived from state):

```kotlin
val timelineItems = remember(datesWithEntries, starredLabels, favoritedDates, selectedFilter) {
    val filteredDates = datesWithEntries.filter { date ->
        when (selectedFilter) {
            ALL -> true
            STARRED -> favoritedDates.contains(date.toString())
            LABELED -> starredLabels.containsKey(date)
        }
    }.sortedDescending()  // newest first

    buildList {
        var currentMonth: YearMonth? = null
        for (date in filteredDates) {
            val ym = YearMonth.from(date)
            if (ym != currentMonth) {
                currentMonth = ym
                add(MonthHeader(ym, filteredDates.count { YearMonth.from(it) == ym }))
            }
            add(DateNode(
                date = date,
                dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                label = starredLabels[date],
                isStarred = favoritedDates.contains(date.toString()),
                isToday = date == LocalDate.now()
            ))
        }
    }
}
```

---

## Phase 5 — LazyColumn Rendering

**Key composables:**

```kotlin
LazyColumn(state = listState) {
    items(timelineItems, key = { item ->
        when (item) {
            is MonthHeader -> "month_${item.yearMonth}"
            is DateNode -> "date_${item.date}"
        }
    }) { item ->
        when (item) {
            is MonthHeader -> TimelineMonthHeader(item)
            is DateNode -> TimelineDateRow(
                item = item,
                isFirst = ...,
                isLast = ...,
                onClick = { onNavigateToDate(item.date) }
            )
        }
    }
}
```

**`TimelineDateRow` layout:**
```
Row {
    // Track column (fixed 32.dp width)
    Box(width = 32.dp) {
        VerticalLine(top)      // 2.dp wide, teal at 12% opacity
        Dot(center)            // 10dp normal, 12dp starred (filled teal), 14dp today
        VerticalLine(bottom)
    }

    // Content row
    Row(padding start 12.dp, clickable → onNavigateToDate) {
        Text(date.dayOfMonth)      // 18sp, mono weight 800, teal if starred
        Text(dayOfWeek)            // 13sp, muted
        if (label != null) LabelChip(label)
        if (isStarred) StarIcon()
        if (isToday) TodayBadge()
    }
}
```

**`TimelineMonthHeader` layout:**
```
Row(sticky header behavior) {
    Text("APRIL 2026")        // 13sp, uppercase, teal at 60%
    HorizontalDivider()       // teal at 12%
    Text("9 entries")          // 11sp, muted
}
```

Use `stickyHeader` in LazyColumn for month headers so they pin while scrolling.

---

## Phase 6 — Visual Specs

| Element | Normal | Starred | Today |
|---|---|---|---|
| Dot size | 10dp | 12dp | 14dp |
| Dot fill | teal 25% | teal solid | white |
| Dot border | teal 40% | lighter teal + glow | teal 3dp + glow |
| Date text | white 70% | teal | white |
| Day text | white 40% | teal 60% | white 60% |
| Vertical line | 2dp, teal 12% | same | same |

**Filter chips:** M3 `FilterChip` composable. Selected = tonal fill. Row below top bar, horizontal scroll if needed.

**Tap action:** Tap any date row → `onNavigateToDate(date)` → selects date in ViewModel → navigates to Home screen showing that date's entries.

**Scroll to today:** On first open, `LazyListState.scrollToItem()` to today's position (or nearest date). Use `LaunchedEffect(Unit)` with index lookup.

---

## Phase 7 — Performance Considerations

- **`datesWithEntries` is already cached** — no disk reads needed
- **`starredLabels` is already cached** — loaded from `journal_labels_cache_v1.json`
- **Grouping is O(n log n)** for sort + O(n) for grouping — fine for even 3000+ dates
- **LazyColumn** handles virtualization — only visible items composed
- **`key` parameter** on items enables efficient diffing
- **`remember` with keys** on `timelineItems` prevents recomputation unless data changes
- **Sticky headers** via `stickyHeader {}` in LazyColumn DSL — native, no custom impl

---

## Files Summary

| Action | File | Change |
|---|---|---|
| **New** | `ui/screens/TimelineScreen.kt` | ~250-300 lines |
| Modify | `ui/navigation/Route.kt` | +1 route |
| Modify | `ui/app/JournalNavHost.kt` | +15 lines (composable block) |
| Modify | `ui/screens/CalendarScreen.kt` | +1 param, +1 icon button in top bar |
| Modify | `ui/components/NavDrawer.kt` | +1 nav item (optional, if drawer access wanted) |

**Zero ViewModel changes.** All needed state already exposed.
