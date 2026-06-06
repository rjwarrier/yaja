package com.mj.yaja.domain.keywords

import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordMatch
import com.mj.yaja.data.KeywordMatchCache
import com.mj.yaja.data.KeywordRepository
import com.mj.yaja.data.KeywordStats
import com.mj.yaja.data.KeywordType
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.ui.viewmodel.JournalUiState
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeywordCoordinator(
    private val fileManager: MarkdownFileManager,
    private val keywordRepository: KeywordRepository,
    private val keywordMatchCache: KeywordMatchCache,
    private val scope: CoroutineScope,
    private val getUiState: () -> JournalUiState,
    private val updateUiState: ((JournalUiState) -> JournalUiState) -> Unit,
    private val emitToast: suspend (String) -> Unit
) {
    private val _keywordIndexingIds = MutableStateFlow<Set<String>>(emptySet())
    val keywordIndexingIds: StateFlow<Set<String>> = _keywordIndexingIds.asStateFlow()
    val keywordMatchCounts: StateFlow<Map<String, Int>> = keywordMatchCache.matchCounts
    val keywordMatchState = keywordMatchCache.rebuildState
    val keywordLastIndexedAt = keywordMatchCache.lastIndexedAt
    val keywordRebuildProgress = keywordMatchCache.rebuildProgress
    val keywordEstimatedRemainingMillis = keywordMatchCache.estimatedRemainingMillis

    private val keywordIndexingCounts = mutableMapOf<String, Int>()
    private val keywordIndexingLock = Any()
    private var keywordRebuildJob: Job? = null

    fun addKeyword(
        name: String,
        type: KeywordType,
        relation: String = "",
        aliases: List<String> = emptyList(),
        enabled: Boolean = true
    ) {
        keywordRepository.addKeyword(name, type, relation, aliases, enabled = false)
        scope.launch {
            emitToast("Keyword saved disabled. Toggle it on when you're ready to index it.")
        }
    }

    fun updateKeyword(keyword: KeywordDefinition) {
        val updatedKeyword = keyword.copy(isEnabled = false)
        keywordRepository.updateKeyword(updatedKeyword)
        keywordMatchCache.removeMatchesForKeyword(updatedKeyword.id)
        refreshActiveKeywordFilter()
        scope.launch {
            emitToast("Keyword updated and turned off. Toggle it on to reindex with the new values.")
        }
    }

    fun deleteKeyword(keywordId: String) {
        keywordRepository.deleteKeyword(keywordId)
        keywordMatchCache.removeMatchesForKeyword(keywordId)
        if (getUiState().activeKeywordFilter?.id == keywordId) {
            clearKeywordFilter()
        }
        rebuildKeywordIndex()
    }

    fun setKeywordEnabled(keywordId: String, enabled: Boolean) {
        keywordRepository.setKeywordEnabled(keywordId, enabled)
        if (enabled) {
            markKeywordIndexing(setOf(keywordId))
            rebuildKeywordIndex(trackedKeywordIds = setOf(keywordId))
        } else {
            keywordMatchCache.removeMatchesForKeyword(keywordId)
            refreshActiveKeywordFilter()
        }
    }

    fun setKeywordFuzzyThreshold(threshold: Float) {
        keywordRepository.setFuzzyThreshold(threshold)
        rebuildKeywordIndex()
    }

    fun reindexAllKeywords() {
        val allKeywordIds = keywordRepository.keywords.value.map { it.id }.toSet()
        if (allKeywordIds.isEmpty()) return
        markKeywordIndexing(allKeywordIds)
        rebuildKeywordIndex(immediate = true, trackedKeywordIds = allKeywordIds)
    }

    fun importKeywords(importedKeywords: List<KeywordDefinition>) {
        if (importedKeywords.isEmpty()) return

        val currentKeywords = keywordRepository.keywords.value
        val existingByKey = currentKeywords.associateBy { keyword ->
            keywordImportKey(keyword.name, keyword.type)
        }

        val mergedByKey = linkedMapOf<String, KeywordDefinition>()
        val activationQueue = mutableListOf<String>()
        val shouldStageActivation = importedKeywords.size > 1
        val affectedKeywordIds = mutableSetOf<String>()

        currentKeywords.forEach { keyword ->
            mergedByKey[keywordImportKey(keyword.name, keyword.type)] = keyword
        }

        importedKeywords.forEach { imported ->
            val key = keywordImportKey(imported.name, imported.type)
            val existing = existingByKey[key]
            val normalizedAliases = imported.aliases
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            mergedByKey[key] = if (existing != null) {
                existing.copy(
                    name = imported.name.trim(),
                    type = imported.type,
                    relation = imported.relation.trim(),
                    aliases = normalizedAliases,
                    isEnabled = imported.isEnabled
                ).also { affectedKeywordIds += it.id }
            } else {
                val created = imported.copy(
                    id = if (imported.id.isBlank()) UUID.randomUUID().toString() else imported.id,
                    name = imported.name.trim(),
                    relation = imported.relation.trim(),
                    aliases = normalizedAliases,
                    isEnabled = if (shouldStageActivation && imported.isEnabled) false else imported.isEnabled,
                    createdAt = imported.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                )
                if (shouldStageActivation && imported.isEnabled) {
                    activationQueue += created.id
                }
                affectedKeywordIds += created.id
                created
            }
        }

        keywordRepository.replaceAllKeywords(
            mergedByKey.values.sortedBy { it.name.lowercase() }
        )
        markKeywordIndexing(affectedKeywordIds)

        if (activationQueue.isEmpty()) {
            rebuildKeywordIndex(immediate = true, trackedKeywordIds = affectedKeywordIds)
            scope.launch { emitToast("Imported ${importedKeywords.size} keyword(s)") }
            return
        }

        scope.launch {
            emitToast(
                "Imported ${importedKeywords.size} keyword(s). Activating ${activationQueue.size} one by one"
            )

            activationQueue.forEach { keywordId ->
                delay(80)
                keywordRepository.setKeywordEnabled(keywordId, true)
            }

            rebuildKeywordIndex(immediate = true, trackedKeywordIds = affectedKeywordIds)
            emitToast("Keyword import finished")
        }
    }

    fun rebuildKeywordIndex(
        immediate: Boolean = false,
        trackedKeywordIds: Set<String> = emptySet()
    ) {
        keywordRebuildJob?.cancel()
        keywordRebuildJob = scope.launch {
            try {
                if (!immediate) delay(250)
                val dates = withContext(Dispatchers.IO) {
                    fileManager.getAllJournalDatesLightweight().toList()
                }
                keywordMatchCache.rebuildAllStreaming(
                    dates = dates,
                    entryLoader = { date -> fileManager.getEntriesForDate(date) },
                    keywords = keywordRepository.keywords.value,
                    fuzzyThreshold = keywordRepository.fuzzyThreshold.value
                )
                refreshActiveKeywordFilter()
            } finally {
                clearKeywordIndexing(trackedKeywordIds)
            }
        }
    }

    suspend fun rebuildKeywordIndexForDate(
        date: LocalDate,
        entries: List<String>
    ) {
        keywordMatchCache.rebuildForDate(
            date = date,
            entries = entries,
            keywords = keywordRepository.keywords.value,
            fuzzyThreshold = keywordRepository.fuzzyThreshold.value
        )
        refreshActiveKeywordFilter()
    }

    fun filterByKeyword(keywordId: String?) {
        if (keywordId.isNullOrBlank()) {
            clearKeywordFilter()
            return
        }
        val keyword = keywordRepository.getKeywordById(keywordId) ?: run {
            clearKeywordFilter()
            return
        }
        updateUiState {
            it.copy(
                activeKeywordFilter = keyword,
                keywordFilteredEntries = keywordMatchCache.getGroupedMatchesForKeyword(keywordId)
            )
        }
    }

    fun filterByKeywordType(type: KeywordType?) {
        if (type == null) {
            clearKeywordFilter()
            return
        }
        updateUiState {
            it.copy(
                activeKeywordFilter = null,
                keywordFilteredEntries = keywordMatchCache.getGroupedMatchesForType(
                    keywordRepository.keywords.value,
                    type
                )
            )
        }
    }

    fun clearKeywordFilter() {
        updateUiState { it.copy(activeKeywordFilter = null, keywordFilteredEntries = null) }
    }

    fun refreshActiveKeywordFilter() {
        getUiState().activeKeywordFilter?.let { active ->
            val latest = keywordRepository.getKeywordById(active.id)
            if (latest == null) {
                clearKeywordFilter()
            } else {
                updateUiState {
                    it.copy(
                        activeKeywordFilter = latest,
                        keywordFilteredEntries = keywordMatchCache.getGroupedMatchesForKeyword(latest.id)
                    )
                }
            }
        }
    }

    fun getKeywordById(keywordId: String): KeywordDefinition? =
        keywordRepository.getKeywordById(keywordId)

    fun getKeywordStats(keywordId: String): KeywordStats? {
        val keyword = keywordRepository.getKeywordById(keywordId) ?: return null
        return keywordMatchCache.computeStats(keyword, keywordRepository.keywords.value)
    }

    fun getMatchesForKeyword(keywordId: String): List<KeywordMatch> =
        keywordMatchCache.getMatchesForKeyword(keywordId)

    fun getKeywordMatchCounts(): Map<String, Int> =
        keywordMatchCache.getMatchCountMap()

    fun getTopKeywords(type: KeywordType? = null, limit: Int = 5): List<Pair<KeywordDefinition, Int>> =
        keywordMatchCache.getTopKeywords(keywordRepository.keywords.value, type, limit)

    private fun markKeywordIndexing(keywordIds: Set<String>) {
        if (keywordIds.isEmpty()) return
        synchronized(keywordIndexingLock) {
            keywordIds.forEach { keywordId ->
                keywordIndexingCounts[keywordId] = (keywordIndexingCounts[keywordId] ?: 0) + 1
            }
            _keywordIndexingIds.value = keywordIndexingCounts.keys.toSet()
        }
    }

    private fun clearKeywordIndexing(keywordIds: Set<String>) {
        if (keywordIds.isEmpty()) return
        synchronized(keywordIndexingLock) {
            keywordIds.forEach { keywordId ->
                val remaining = (keywordIndexingCounts[keywordId] ?: 0) - 1
                if (remaining > 0) {
                    keywordIndexingCounts[keywordId] = remaining
                } else {
                    keywordIndexingCounts.remove(keywordId)
                }
            }
            _keywordIndexingIds.value = keywordIndexingCounts.keys.toSet()
        }
    }

    private fun keywordImportKey(name: String, type: KeywordType): String =
        "${type.name}:${name.trim().lowercase()}"
}
