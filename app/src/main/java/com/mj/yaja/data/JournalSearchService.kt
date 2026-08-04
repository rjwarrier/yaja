package com.mj.yaja.data

import java.time.LocalDate

internal class JournalSearchService(
    private val ensureSearchMetadataReady: () -> Unit,
    private val isCachePopulated: () -> Boolean,
    private val cachedEntriesForDate: (LocalDate) -> List<String>?,
    private val rebuildSnapshotProvider: () -> Map<LocalDate, List<String>>?,
    private val allJournalDatesProvider: () -> Set<LocalDate>,
    private val labelForDate: (LocalDate) -> String?,
    private val entriesForDateProvider: (LocalDate) -> List<String>
) {
    fun searchEntries(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        ensureSearchMetadataReady()
        val results = mutableListOf<SearchResult>()
        val cachedSnapshot =
            if (isCachePopulated()) {
                null
            } else {
                rebuildSnapshotProvider()
            }
        // Split into words for AND-logic: all words must appear in the entry or label
        val words = query.trim().lowercase().splitToSequence(Regex("\\s+")).filter { it.isNotBlank() }.toList()

        for (date in allJournalDatesProvider().sortedDescending()) {
            // Check day label — surfaces dates whose label matches even if no entry does
            val label = labelForDate(date) ?: ""
            if (label.isNotEmpty()) {
                val labelLower = label.lowercase()
                if (words.all { word -> labelLower.contains(word) }) {
                    results.add(SearchResult(date, "Label: $label"))
                }
            }

            // Check each entry's content
            val entries =
                cachedEntriesForDate(date)
                    ?: cachedSnapshot?.get(date)
                    ?: entriesForDateProvider(date)
            entries.forEach { entry ->
                val cleanEntry = entry.replace(SEARCH_METADATA_REGEX, "").trim()
                val lower = cleanEntry.lowercase()
                if (words.all { word -> lower.contains(word) }) {
                    val snippet =
                        cleanEntry.replace("\n", " ").take(100).let {
                            if (cleanEntry.length > 100) "$it..." else it
                        }
                    results.add(SearchResult(date, snippet))
                }
            }

            // Newest dates are scanned first, so capping keeps the most relevant matches.
            if (results.size >= MAX_SEARCH_RESULTS) break
        }
        return results
    }

    private companion object {
        private const val MAX_SEARCH_RESULTS = 200
        private val SEARCH_METADATA_REGEX = Regex("<!--.*?-->")
    }
}
