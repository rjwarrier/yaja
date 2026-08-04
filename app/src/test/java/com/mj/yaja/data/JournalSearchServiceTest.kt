package com.mj.yaja.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JournalSearchServiceTest {
    @Test
    fun blankQueryDoesNotTouchProviders() {
        var metadataLoaded = false
        val service = JournalSearchService(
            ensureSearchMetadataReady = { metadataLoaded = true },
            isCachePopulated = { error("cache state should not be checked") },
            cachedEntriesForDate = { error("cache should not be read") },
            rebuildSnapshotProvider = { error("snapshot should not be read") },
            allJournalDatesProvider = { error("dates should not be read") },
            labelForDate = { error("labels should not be read") },
            entriesForDateProvider = { error("entries should not be read") }
        )

        assertEquals(emptyList<SearchResult>(), service.searchEntries("   "))
        assertFalse(metadataLoaded)
    }

    @Test
    fun searchMatchesLabelsAndEntriesNewestFirst() {
        val older = LocalDate.of(2026, 3, 1)
        val newer = LocalDate.of(2026, 3, 2)
        var metadataLoaded = false
        val cachedEntries = mapOf(
            newer to listOf("<!--time:09:30-->\nMorning coffee at home"),
            older to listOf("Morning walk in park")
        )
        val service = JournalSearchService(
            ensureSearchMetadataReady = { metadataLoaded = true },
            isCachePopulated = { true },
            cachedEntriesForDate = { date -> cachedEntries[date] },
            rebuildSnapshotProvider = { error("snapshot should not be used when cache is populated") },
            allJournalDatesProvider = { setOf(older, newer) },
            labelForDate = { date -> if (date == newer) "Morning mood" else null },
            entriesForDateProvider = { error("disk entries should not be read when cache has the day") }
        )

        val results = service.searchEntries("morning")

        assertTrue(metadataLoaded)
        assertEquals(
            listOf(
                SearchResult(newer, "Label: Morning mood"),
                SearchResult(newer, "Morning coffee at home"),
                SearchResult(older, "Morning walk in park")
            ),
            results
        )
    }

    @Test
    fun searchUsesRebuildSnapshotBeforeReadingDayEntries() {
        val date = LocalDate.of(2026, 3, 3)
        var snapshotReads = 0
        val service = JournalSearchService(
            ensureSearchMetadataReady = {},
            isCachePopulated = { false },
            cachedEntriesForDate = { null },
            rebuildSnapshotProvider = {
                snapshotReads++
                mapOf(date to listOf("Snapshot only result"))
            },
            allJournalDatesProvider = { setOf(date) },
            labelForDate = { null },
            entriesForDateProvider = { error("entries should not be read when snapshot has the day") }
        )

        assertEquals(listOf(SearchResult(date, "Snapshot only result")), service.searchEntries("snapshot"))
        assertEquals(1, snapshotReads)
    }
}
