package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.MarkdownFileManager
import java.time.LocalDate

internal fun buildVersionHistorySummaryText(content: String): String {
        val entryTimeRegex = Regex("""<!--time:(\d{2}:\d{2})(?:, added on .*?)?-->""")
        var entryCount = 0
        var lastEntryTime: String? = null
        var inFrontmatter = false
        content.lineSequence().forEachIndexed { index, rawLine ->
                val line = rawLine.trimEnd()
                if (line.trim() == "---") {
                        if (index == 0) {
                                inFrontmatter = true
                        } else if (inFrontmatter) {
                                inFrontmatter = false
                        }
                        return@forEachIndexed
                }
                if (inFrontmatter || line.startsWith("# ")) return@forEachIndexed
                entryTimeRegex.find(line)?.groupValues?.getOrNull(1)?.let { lastEntryTime = it }
                if (line.startsWith("- ")) {
                        entryCount++
                }
        }
        if (entryCount == 0) return "No entries"
        val entryLabel = if (entryCount == 1) "1 entry" else "$entryCount entries"
        return if (!lastEntryTime.isNullOrBlank()) {
                "$entryLabel • last at $lastEntryTime"
        } else {
                entryLabel
        }
}

internal fun mapVersionHistorySnapshotsUi(
        snapshots: List<MarkdownFileManager.VersionHistorySnapshotInfo>
): List<JournalViewModel.VersionHistorySnapshotUi> {
        return snapshots.map { snapshot ->
                JournalViewModel.VersionHistorySnapshotUi(
                        id = snapshot.id,
                        createdAt = snapshot.createdAt,
                        summary = buildVersionHistorySummaryText(snapshot.content),
                        content = snapshot.content
                )
        }
}

internal suspend fun restoreVersionHistorySnapshotAndRefresh(
        fileManager: MarkdownFileManager,
        date: LocalDate,
        snapshotId: String,
        reloadEntries: suspend (LocalDate) -> Unit,
        refreshCalendarDates: () -> Unit,
        refreshStarredLabels: () -> Unit,
        refreshRevisitState: (LocalDate) -> Unit,
        refreshFavoritedHighlights: () -> Unit,
        refreshTodos: (Boolean) -> Unit,
        updateHeatmap: () -> Unit,
        ensureMonthlyStatsLoaded: () -> Unit,
        reloadVersionHistorySnapshots: (LocalDate) -> Unit,
        emitToast: suspend (String) -> Unit
) {
        val restored = fileManager.restoreVersionHistorySnapshot(date, snapshotId)
        if (!restored) {
                emitToast("Version restore failed. Yaja kept the current file unchanged.")
                return
        }

        reloadEntries(date)
        refreshCalendarDates()
        refreshStarredLabels()
        refreshRevisitState(date)
        refreshFavoritedHighlights()
        refreshTodos(true)
        updateHeatmap()
        ensureMonthlyStatsLoaded()
        reloadVersionHistorySnapshots(date)
        emitToast("Restored version for $date.")
}
