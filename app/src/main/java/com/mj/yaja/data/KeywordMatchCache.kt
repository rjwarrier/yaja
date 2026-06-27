package com.mj.yaja.data

import android.content.Context
import android.util.Log
import com.mj.yaja.data.database.JournalDatabase
import com.mj.yaja.data.database.KeywordMatchEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the database and in-memory keyword match index.
 *
 * Storage: Room Database table `keyword_matches`.
 *
 * In-memory structures:
 *  - [matchesByKeyword]: keywordId → (dateString → list of matches)
 *  - [matchesByDate]:    dateString → list of matches (inverted index, for fast per-day lookup)
 *
 * Rebuild triggers (called externally):
 *  - Full:        [rebuildAll]    — on storage change, import complete, manual refresh
 *  - Incremental: [rebuildForDate] — on single entry add/edit/delete
 */
class KeywordMatchCache private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private val mutationMutex = Mutex()

    private val database = JournalDatabase.getDatabase(context)
    private val matchDao = database.keywordMatchDao()

    // In-memory indexes
    private val matchesByKeyword = ConcurrentHashMap<String, MutableMap<String, MutableList<KeywordMatch>>>()
    private val matchesByDate    = ConcurrentHashMap<String, MutableList<KeywordMatch>>()
    private val flatMatchesByKeyword = ConcurrentHashMap<String, List<KeywordMatch>>()
    private val groupedMatchesByKeyword =
        ConcurrentHashMap<String, List<Pair<LocalDate, List<KeywordMatch>>>>()
    private val matchCountByKeyword = ConcurrentHashMap<String, Int>()

    sealed class RebuildState {
        data object Idle       : RebuildState()
        data object Rebuilding : RebuildState()
        data object Ready      : RebuildState()
    }

    private val _rebuildState = MutableStateFlow<RebuildState>(RebuildState.Idle)
    val rebuildState: StateFlow<RebuildState> = _rebuildState.asStateFlow()
    private val _lastIndexedAt = MutableStateFlow<Long?>(null)
    val lastIndexedAt: StateFlow<Long?> = _lastIndexedAt.asStateFlow()
    private val _rebuildProgress = MutableStateFlow<Float?>(null)
    val rebuildProgress: StateFlow<Float?> = _rebuildProgress.asStateFlow()
    private val _estimatedRemainingMillis = MutableStateFlow<Long?>(null)
    val estimatedRemainingMillis: StateFlow<Long?> = _estimatedRemainingMillis.asStateFlow()
    private val _matchCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val matchCounts: StateFlow<Map<String, Int>> = _matchCounts.asStateFlow()

    private fun emitMatchCounts() {
        _matchCounts.value = matchCountByKeyword.toMap()
    }

    // ── Public API ────────────────────────────────────────────────────────

    /** Load the cache from database into memory. Call once on startup. */
    fun loadFromDisk() {
        scope.launch {
            try {
                val dbMatches = matchDao.getAllMatchesSync()
                if (dbMatches.isEmpty()) return@launch

                mutationMutex.withLock {
                    if (_rebuildState.value == RebuildState.Rebuilding || matchesByKeyword.isNotEmpty()) {
                        return@withLock
                    }

                    matchesByKeyword.clear()
                    matchesByDate.clear()

                    dbMatches.forEach { entity ->
                        val m = entity.toKeywordMatch()
                        matchesByKeyword.getOrPut(m.keywordId) { ConcurrentHashMap() }
                            .getOrPut(m.date) { mutableListOf() }.add(m)
                        matchesByDate.getOrPut(m.date) { mutableListOf() }.add(m)
                    }

                    rebuildDerivedIndexes()
                }
                emitMatchCounts()
                _lastIndexedAt.value = System.currentTimeMillis()
                _rebuildState.value = RebuildState.Ready
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load keyword cache from database", e)
            }
        }
    }

    /**
     * Full rebuild: iterate every entry in [allEntries] against all [keywords].
     * Runs entirely on [Dispatchers.IO].
     */
    suspend fun rebuildAll(
        allEntries: Map<LocalDate, List<String>>,
        keywords: List<KeywordDefinition>,
        fuzzyThreshold: Float = 0.90f
    ) {
        _rebuildState.value = RebuildState.Rebuilding
        _rebuildProgress.value = 0f
        _estimatedRemainingMillis.value = null
        try {
            withContext(Dispatchers.IO) {
                val newByKeyword = HashMap<String, MutableMap<String, MutableList<KeywordMatch>>>()
                val newByDate = HashMap<String, MutableList<KeywordMatch>>()
                val newFlatByKeyword = HashMap<String, MutableList<KeywordMatch>>()
                val newMatchCounts = HashMap<String, Int>()

                val enabledKeywords = keywords.filter { it.isEnabled }
                val totalEntries = allEntries.values.sumOf { it.size }.coerceAtLeast(1)
                var processedEntries = 0
                val rebuildStartedAt = System.currentTimeMillis()

                for ((date, entries) in allEntries) {
                    ensureActive()
                    val dateStr = date.toString()
                    for ((index, entry) in entries.withIndex()) {
                        ensureActive()
                        val entryMatches = matchEntryAcrossKeywords(
                            entry = entry,
                            entryIndex = index,
                            date = dateStr,
                            keywords = enabledKeywords,
                            fuzzyThreshold = fuzzyThreshold
                        )
                        for (match in entryMatches) {
                            newByKeyword
                                .getOrPut(match.keywordId) { HashMap() }
                                .getOrPut(dateStr) { mutableListOf() }
                                .add(match)
                            newByDate.getOrPut(dateStr) { mutableListOf() }.add(match)
                            newFlatByKeyword.getOrPut(match.keywordId) { mutableListOf() }.add(match)
                            newMatchCounts[match.keywordId] = (newMatchCounts[match.keywordId] ?: 0) + 1
                        }
                        processedEntries += 1
                        if (processedEntries == totalEntries || processedEntries % FULL_REBUILD_PROGRESS_EMIT_EVERY == 0) {
                            val progress = processedEntries.toFloat() / totalEntries.toFloat()
                            _rebuildProgress.value = progress
                            val elapsed = (System.currentTimeMillis() - rebuildStartedAt).coerceAtLeast(1L)
                            val estimatedTotal = (elapsed / progress).toLong().coerceAtLeast(elapsed)
                            _estimatedRemainingMillis.value =
                                (estimatedTotal - elapsed).coerceAtLeast(0L)
                        }
                    }
                }

                mutationMutex.withLock {
                    matchesByKeyword.clear()
                    matchesByKeyword.putAll(newByKeyword)
                    matchesByDate.clear()
                    matchesByDate.putAll(newByDate)
                    flatMatchesByKeyword.clear()
                    flatMatchesByKeyword.putAll(
                        newFlatByKeyword.mapValues { (_, matches) ->
                            matches.sortedWith(compareByDescending<KeywordMatch> { it.date }.thenBy { it.entryIndex })
                        }
                    )
                    groupedMatchesByKeyword.clear()
                    groupedMatchesByKeyword.putAll(
                        newByKeyword.mapValues { (_, dateMap) -> buildGroupedMatches(dateMap) }
                    )
                    matchCountByKeyword.clear()
                    matchCountByKeyword.putAll(newMatchCounts)
                    scheduleSaveToDiskLocked()
                }
                emitMatchCounts()
            }
            _lastIndexedAt.value = System.currentTimeMillis()
            _rebuildState.value = RebuildState.Ready
        } catch (e: Exception) {
            Log.e(TAG, "Keyword index rebuild failed", e)
            _rebuildState.value = RebuildState.Ready
        } finally {
            _rebuildProgress.value = null
            _estimatedRemainingMillis.value = null
        }
    }

    /**
     * Full rebuild without requiring all journal entries to be materialized in one snapshot first.
     * This is safer for very large journals because entries are loaded date-by-date.
     */
    suspend fun rebuildAllStreaming(
        dates: List<LocalDate>,
        entryLoader: suspend (LocalDate) -> List<String>,
        keywords: List<KeywordDefinition>,
        fuzzyThreshold: Float = 0.90f
    ) {
        _rebuildState.value = RebuildState.Rebuilding
        _rebuildProgress.value = 0f
        _estimatedRemainingMillis.value = null
        try {
            withContext(Dispatchers.IO) {
                val newByKeyword = HashMap<String, MutableMap<String, MutableList<KeywordMatch>>>()
                val newByDate = HashMap<String, MutableList<KeywordMatch>>()
                val newFlatByKeyword = HashMap<String, MutableList<KeywordMatch>>()
                val newMatchCounts = HashMap<String, Int>()

                val enabledKeywords = keywords.filter { it.isEnabled }
                val orderedDates = dates.sortedDescending()
                val totalDates = orderedDates.size.coerceAtLeast(1)
                var processedDates = 0
                val rebuildStartedAt = System.currentTimeMillis()

                for (date in orderedDates) {
                    ensureActive()
                    val dateStr = date.toString()
                    val entries = entryLoader(date)
                    for ((index, entry) in entries.withIndex()) {
                        ensureActive()
                        val entryMatches = matchEntryAcrossKeywords(
                            entry = entry,
                            entryIndex = index,
                            date = dateStr,
                            keywords = enabledKeywords,
                            fuzzyThreshold = fuzzyThreshold
                        )
                        for (match in entryMatches) {
                            newByKeyword
                                .getOrPut(match.keywordId) { HashMap() }
                                .getOrPut(dateStr) { mutableListOf() }
                                .add(match)
                            newByDate.getOrPut(dateStr) { mutableListOf() }.add(match)
                            newFlatByKeyword.getOrPut(match.keywordId) { mutableListOf() }.add(match)
                            newMatchCounts[match.keywordId] = (newMatchCounts[match.keywordId] ?: 0) + 1
                        }
                    }
                    processedDates += 1
                    if (processedDates == totalDates || processedDates % STREAMING_REBUILD_PROGRESS_EMIT_EVERY == 0) {
                        val progress = processedDates.toFloat() / totalDates.toFloat()
                        _rebuildProgress.value = progress
                        val elapsed = (System.currentTimeMillis() - rebuildStartedAt).coerceAtLeast(1L)
                        val estimatedTotal = (elapsed / progress).toLong().coerceAtLeast(elapsed)
                        _estimatedRemainingMillis.value =
                            (estimatedTotal - elapsed).coerceAtLeast(0L)
                    }
                }

                mutationMutex.withLock {
                    matchesByKeyword.clear()
                    matchesByKeyword.putAll(newByKeyword)
                    matchesByDate.clear()
                    matchesByDate.putAll(newByDate)
                    flatMatchesByKeyword.clear()
                    flatMatchesByKeyword.putAll(
                        newFlatByKeyword.mapValues { (_, matches) ->
                            matches.sortedWith(compareByDescending<KeywordMatch> { it.date }.thenBy { it.entryIndex })
                        }
                    )
                    groupedMatchesByKeyword.clear()
                    groupedMatchesByKeyword.putAll(
                        newByKeyword.mapValues { (_, dateMap) -> buildGroupedMatches(dateMap) }
                    )
                    matchCountByKeyword.clear()
                    matchCountByKeyword.putAll(newMatchCounts)
                    scheduleSaveToDiskLocked()
                }
                emitMatchCounts()
            }
            _lastIndexedAt.value = System.currentTimeMillis()
            _rebuildState.value = RebuildState.Ready
        } catch (e: Exception) {
            Log.e(TAG, "Streaming keyword index rebuild failed", e)
            _rebuildState.value = RebuildState.Ready
        } finally {
            _rebuildProgress.value = null
            _estimatedRemainingMillis.value = null
        }
    }

    /**
     * Incremental rebuild for a single [date].
     * Replaces all matches for that date; other dates are untouched.
     */
    suspend fun rebuildForDate(
        date: LocalDate,
        entries: List<String>,
        keywords: List<KeywordDefinition>,
        fuzzyThreshold: Float = 0.90f
    ) {
        _rebuildState.value = RebuildState.Rebuilding
        _rebuildProgress.value = 0f
        _estimatedRemainingMillis.value = null
        try {
            withContext(Dispatchers.IO) {
                val dateStr = date.toString()
                val enabledKeywords = keywords.filter { it.isEnabled }
                val newMatchesForDate = mutableListOf<KeywordMatch>()
                val totalEntries = entries.size.coerceAtLeast(1)
                var processedEntries = 0
                val rebuildStartedAt = System.currentTimeMillis()

                for ((index, entry) in entries.withIndex()) {
                    ensureActive()
                    newMatchesForDate += matchEntryAcrossKeywords(
                        entry = entry,
                        entryIndex = index,
                        date = dateStr,
                        keywords = enabledKeywords,
                        fuzzyThreshold = fuzzyThreshold
                    )
                    processedEntries += 1
                    if (processedEntries == totalEntries || processedEntries % FULL_REBUILD_PROGRESS_EMIT_EVERY == 0) {
                        val progress = processedEntries.toFloat() / totalEntries.toFloat()
                        _rebuildProgress.value = progress
                        val elapsed = (System.currentTimeMillis() - rebuildStartedAt).coerceAtLeast(1L)
                        val estimatedTotal = (elapsed / progress).toLong().coerceAtLeast(elapsed)
                        _estimatedRemainingMillis.value =
                            (estimatedTotal - elapsed).coerceAtLeast(0L)
                    }
                }

                mutationMutex.withLock {
                    val oldMatches = matchesByDate.remove(dateStr).orEmpty()
                    val affectedKeywordIds = oldMatches.mapTo(mutableSetOf()) { it.keywordId }
                    affectedKeywordIds += newMatchesForDate.mapTo(mutableSetOf()) { it.keywordId }

                    for (keywordId in affectedKeywordIds) {
                        matchesByKeyword[keywordId]?.remove(dateStr)
                    }

                    if (newMatchesForDate.isNotEmpty()) {
                        matchesByDate[dateStr] = newMatchesForDate.toMutableList()
                        newMatchesForDate.groupBy { it.keywordId }.forEach { (keywordId, matches) ->
                            matchesByKeyword
                                .getOrPut(keywordId) { ConcurrentHashMap() }
                                .getOrPut(dateStr) { mutableListOf() }
                                .addAll(matches)
                        }
                    }

                    affectedKeywordIds.forEach(::refreshKeywordAggregates)
                    
                    // Direct database incremental updates
                    scope.launch {
                        try {
                            matchDao.deleteByDate(dateStr)
                            if (newMatchesForDate.isNotEmpty()) {
                                matchDao.insertAll(newMatchesForDate.map { it.toKeywordMatchEntity() })
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to incrementally save matches for $dateStr to database", e)
                        }
                    }
                }
                emitMatchCounts()
            }
            _lastIndexedAt.value = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w(TAG, "Incremental keyword rebuild failed for $date", e)
        } finally {
            _rebuildState.value = RebuildState.Ready
            _rebuildProgress.value = null
            _estimatedRemainingMillis.value = null
        }
    }

    /** Remove all matches for [keywordId] (after keyword deletion). */
    fun removeMatchesForKeyword(keywordId: String) {
        scope.launch {
            mutationMutex.withLock {
                matchesByKeyword.remove(keywordId)
                flatMatchesByKeyword.remove(keywordId)
                groupedMatchesByKeyword.remove(keywordId)
                matchCountByKeyword.remove(keywordId)
                for (dateMatches in matchesByDate.values) {
                    dateMatches.removeAll { it.keywordId == keywordId }
                }
                matchesByDate.entries.removeAll { it.value.isEmpty() }
                
                try {
                    matchDao.deleteByKeyword(keywordId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete keyword matches for $keywordId", e)
                }
            }
            emitMatchCounts()
        }
    }

    /** Clear all in-memory data and wipe database table. */
    fun invalidate() {
        _rebuildState.value = RebuildState.Idle
        _lastIndexedAt.value = null
        scope.launch {
            mutationMutex.withLock {
                saveJob?.cancel()
                saveJob = null
                matchesByKeyword.clear()
                matchesByDate.clear()
                flatMatchesByKeyword.clear()
                groupedMatchesByKeyword.clear()
                matchCountByKeyword.clear()
                
                try {
                    matchDao.deleteAll()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete all keyword matches from database", e)
                }
            }
            emitMatchCounts()
        }
    }

    /** All matches for a given keyword id, sorted by date descending. */
    fun getMatchesForKeyword(keywordId: String): List<KeywordMatch> =
        flatMatchesByKeyword[keywordId].orEmpty()

    fun getGroupedMatchesForKeyword(keywordId: String): List<Pair<LocalDate, List<KeywordMatch>>> =
        groupedMatchesByKeyword[keywordId].orEmpty()

    fun getGroupedMatchesForType(
        keywords: List<KeywordDefinition>,
        type: KeywordType
    ): List<Pair<LocalDate, List<KeywordMatch>>> {
        val allowedIds = keywords.asSequence()
            .filter { it.type == type }
            .map { it.id }
            .toSet()
        if (allowedIds.isEmpty()) return emptyList()

        return matchesByDate.entries.mapNotNull { (date, matches) ->
            val filtered = matches.filter { it.keywordId in allowedIds }
            if (filtered.isEmpty()) {
                null
            } else {
                runCatching { LocalDate.parse(date) }.getOrNull()?.let { parsed ->
                    parsed to filtered.sortedBy { it.entryIndex }
                }
            }
        }.sortedByDescending { it.first }
    }

    /** All matches on a given date. */
    fun getMatchesForDate(date: LocalDate): List<KeywordMatch> =
        matchesByDate[date.toString()] ?: emptyList()

    /** All matches in the entire index. */
    fun getAllMatches(): List<KeywordMatch> =
        matchesByDate.values.flatten()

    /** Returns true if any matches exist for [keywordId]. */
    fun hasMatchesForKeyword(keywordId: String): Boolean =
        matchesByKeyword[keywordId]?.isNotEmpty() == true

    /** Total match count for [keywordId]. */
    fun matchCountForKeyword(keywordId: String): Int =
        matchCountByKeyword[keywordId] ?: 0

    fun getMatchCountMap(): Map<String, Int> =
        matchCountByKeyword.toMap()

    // ── Statistics ────────────────────────────────────────────────────────

    /**
     * Compute [KeywordStats] for [keyword] from the in-memory index.
     * [allKeywords] is used to resolve co-occurring keyword names.
     */
    fun computeStats(
        keyword: KeywordDefinition,
        allKeywords: List<KeywordDefinition>
    ): KeywordStats {
        val dateMap = matchesByKeyword[keyword.id] ?: emptyMap()
        val uniqueDays = dateMap.keys.size
        val totalMentions = matchCountForKeyword(keyword.id)

        val sortedDates = dateMap.keys.sorted()
        val firstSeen = sortedDates.firstOrNull()?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val lastSeen  = sortedDates.lastOrNull() ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        // Monthly breakdown
        val byMonth = mutableMapOf<YearMonth, Int>()
        for (dateStr in dateMap.keys) {
            val d = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
            val ym = YearMonth.of(d.year, d.month)
            byMonth[ym] = (byMonth[ym] ?: 0) + (dateMap[dateStr]?.size ?: 0)
        }
        val mentionsByMonth = byMonth.entries.sortedBy { it.key }.map { it.key to it.value }

        // Co-occurring: other keywords that appear on the same days
        val keywordsById = allKeywords.associateBy { it.id }
        val coStats = mutableMapOf<String, RelationshipAccumulator>()
        for (dateStr in dateMap.keys) {
            val dayMatches = matchesByDate[dateStr] ?: continue
            val parsedDate = runCatching { LocalDate.parse(dateStr) }.getOrNull()
            val currentEntryIndexes = dayMatches.asSequence()
                .filter { it.keywordId == keyword.id }
                .map { it.entryIndex }
                .toSet()
            if (currentEntryIndexes.isEmpty()) continue

            val otherMatchesByKeyword = dayMatches.asSequence()
                .filter { it.keywordId != keyword.id }
                .groupBy { it.keywordId }

            for ((otherId, otherMatches) in otherMatchesByKeyword) {
                if (keywordsById[otherId] == null) continue
                val sharedEntries = otherMatches.asSequence()
                    .map { it.entryIndex }
                    .toSet()
                    .intersect(currentEntryIndexes)
                    .size
                val accumulator = coStats.getOrPut(otherId) { RelationshipAccumulator() }
                accumulator.daysTogether += 1
                accumulator.sharedEntries += sharedEntries
                if (parsedDate != null && (accumulator.lastSeenTogether == null || parsedDate.isAfter(accumulator.lastSeenTogether))) {
                    accumulator.lastSeenTogether = parsedDate
                }
            }
        }
        val coOccurring = coStats.entries
            .mapNotNull { (otherId, accumulator) ->
                keywordsById[otherId]?.let { other ->
                    val score = accumulator.daysTogether.toFloat() +
                        (accumulator.sharedEntries * 0.35f) +
                        recencyBonus(accumulator.lastSeenTogether)
                    KeywordCoOccurrence(
                        keywordId = other.id,
                        name = other.name,
                        type = other.type,
                        relation = other.relation,
                        daysTogether = accumulator.daysTogether,
                        sharedEntries = accumulator.sharedEntries,
                        lastSeenTogether = accumulator.lastSeenTogether,
                        score = score
                    )
                }
            }
            .sortedWith(
                compareByDescending<KeywordCoOccurrence> { it.score }
                    .thenByDescending { it.daysTogether }
                    .thenByDescending { it.sharedEntries }
                    .thenBy { it.name.lowercase() }
            )

        return KeywordStats(
            keyword         = keyword,
            totalMentions   = totalMentions,
            uniqueDays      = uniqueDays,
            firstSeen       = firstSeen,
            lastSeen        = lastSeen,
            mentionsByMonth = mentionsByMonth,
            coOccurring     = coOccurring
        )
    }

    /** Top N keywords by total mention count, optionally filtered by [type]. */
    fun getTopKeywords(
        keywords: List<KeywordDefinition>,
        type: KeywordType? = null,
        limit: Int = 5
    ): List<Pair<KeywordDefinition, Int>> {
        val filtered = if (type != null) keywords.filter { it.type == type } else keywords
        return filtered
            .map { kw -> kw to matchCountForKeyword(kw.id) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
    }

    // ── Database serialisation ───────────────────────────────────────────

    private fun scheduleSaveToDiskLocked() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            saveToDisk()
        }
    }

    private suspend fun saveToDisk() {
        try {
            val entities = mutationMutex.withLock {
                matchesByDate.values.flatten().map { it.toKeywordMatchEntity() }
            }
            withContext(Dispatchers.IO) {
                matchDao.deleteAll()
                if (entities.isNotEmpty()) {
                    matchDao.insertAll(entities)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save keyword matches to database", e)
        }
    }

    private fun matchEntryAcrossKeywords(
        entry: String,
        entryIndex: Int,
        date: String,
        keywords: List<KeywordDefinition>,
        fuzzyThreshold: Float
    ): List<KeywordMatch> {
        val rawMatches = buildList {
            keywords.forEach { keyword ->
                addAll(KeywordMatcher.findMatches(entry, entryIndex, date, keyword, fuzzyThreshold))
            }
        }
        return KeywordMatcher.resolveOverlappingMatches(rawMatches)
    }

    private fun rebuildDerivedIndexes() {
        flatMatchesByKeyword.clear()
        groupedMatchesByKeyword.clear()
        matchCountByKeyword.clear()

        matchesByKeyword.forEach { (keywordId, dateMap) ->
            val flat = dateMap.values.flatten()
                .sortedWith(compareByDescending<KeywordMatch> { it.date }.thenBy { it.entryIndex })
            flatMatchesByKeyword[keywordId] = flat
            groupedMatchesByKeyword[keywordId] = buildGroupedMatches(dateMap)
            matchCountByKeyword[keywordId] = flat.size
        }
    }

    private fun refreshKeywordAggregates(keywordId: String) {
        val dateMap = matchesByKeyword[keywordId]
        if (dateMap.isNullOrEmpty()) {
            matchesByKeyword.remove(keywordId)
            flatMatchesByKeyword.remove(keywordId)
            groupedMatchesByKeyword.remove(keywordId)
            matchCountByKeyword.remove(keywordId)
            return
        }

        val flat = dateMap.values.flatten()
            .sortedWith(compareByDescending<KeywordMatch> { it.date }.thenBy { it.entryIndex })
        flatMatchesByKeyword[keywordId] = flat
        groupedMatchesByKeyword[keywordId] = buildGroupedMatches(dateMap)
        matchCountByKeyword[keywordId] = flat.size
    }

    private fun buildGroupedMatches(
        dateMap: Map<String, MutableList<KeywordMatch>>
    ): List<Pair<LocalDate, List<KeywordMatch>>> =
        dateMap.entries.mapNotNull { (date, matches) ->
            runCatching { LocalDate.parse(date) }.getOrNull()?.let { parsed ->
                parsed to matches.sortedBy { it.entryIndex }
            }
        }.sortedByDescending { it.first }

    private fun KeywordMatchEntity.toKeywordMatch() = KeywordMatch(
        keywordId   = keywordId,
        date        = date,
        entryIndex  = entryIndex,
        matchedText = matchedText,
        confidence  = confidence,
        matchType   = KeywordMatchType.valueOf(matchType),
        snippet     = snippet,
        startIndex  = startIndex,
        endExclusive = endExclusive
    )

    private fun KeywordMatch.toKeywordMatchEntity() = KeywordMatchEntity(
        keywordId   = keywordId,
        date        = date,
        entryIndex  = entryIndex,
        matchedText = matchedText,
        confidence  = confidence,
        matchType   = matchType.name,
        snippet     = snippet,
        startIndex  = startIndex,
        endExclusive = endExclusive
    )

    // ── Singleton ─────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "KeywordMatchCache"
        private const val FULL_REBUILD_PROGRESS_EMIT_EVERY = 20
        private const val STREAMING_REBUILD_PROGRESS_EMIT_EVERY = 3

        @Volatile private var instance: KeywordMatchCache? = null

        fun getInstance(context: Context): KeywordMatchCache =
            instance ?: synchronized(this) {
                instance ?: KeywordMatchCache(context.applicationContext).also { instance = it }
            }
    }

    private data class RelationshipAccumulator(
        var daysTogether: Int = 0,
        var sharedEntries: Int = 0,
        var lastSeenTogether: LocalDate? = null
    )

    private fun recencyBonus(lastSeenTogether: LocalDate?): Float {
        if (lastSeenTogether == null) return 0f
        val daysAgo = kotlin.runCatching { java.time.temporal.ChronoUnit.DAYS.between(lastSeenTogether, LocalDate.now()) }
            .getOrDefault(Long.MAX_VALUE)
        return when {
            daysAgo <= 30 -> 1.5f
            daysAgo <= 90 -> 1.0f
            daysAgo <= 365 -> 0.5f
            else -> 0f
        }
    }
}
