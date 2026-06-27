package com.mj.yaja.data

import android.content.Context
import android.content.SharedPreferences
import com.mj.yaja.data.database.JournalDatabase
import com.mj.yaja.data.database.KeywordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Persists [KeywordDefinition] objects locally and exposes them as a [StateFlow].
 *
 * Storage: Room Database table `keywords`.
 * Singleton pattern mirrors [SettingsRepository].
 */
class KeywordRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("keyword_prefs", Context.MODE_PRIVATE)

    private val database = JournalDatabase.getDatabase(context)
    private val keywordDao = database.keywordDao()

    private val _keywords = MutableStateFlow(loadKeywords())
    val keywords: StateFlow<List<KeywordDefinition>> = _keywords.asStateFlow()

    private val _fuzzyThreshold = MutableStateFlow(
        prefs.getFloat(KEY_FUZZY_THRESHOLD, DEFAULT_FUZZY_THRESHOLD)
    )
    val fuzzyThreshold: StateFlow<Float> = _fuzzyThreshold.asStateFlow()

    // ── CRUD ──────────────────────────────────────────────────────────────

    /** Add a new keyword. Returns the created [KeywordDefinition]. */
    fun addKeyword(
        name: String,
        type: KeywordType,
        relation: String = "",
        aliases: List<String> = emptyList(),
        enabled: Boolean = true
    ): KeywordDefinition {
        val keyword = KeywordDefinition(
            id        = UUID.randomUUID().toString(),
            name      = name.trim(),
            type      = type,
            relation  = relation.trim(),
            aliases   = aliases.map { it.trim() }.filter { it.isNotEmpty() },
            isEnabled = enabled,
            createdAt = System.currentTimeMillis()
        )
        keywordDao.insertOrUpdate(keyword.toKeywordEntity())
        val updated = _keywords.value + keyword
        _keywords.value = updated
        return keyword
    }

    /** Replace an existing keyword (matched by [KeywordDefinition.id]). */
    fun updateKeyword(keyword: KeywordDefinition) {
        keywordDao.insertOrUpdate(keyword.toKeywordEntity())
        val updated = _keywords.value.map { if (it.id == keyword.id) keyword else it }
        _keywords.value = updated
    }

    /** Remove a keyword by id. */
    fun deleteKeyword(id: String) {
        keywordDao.delete(id)
        val updated = _keywords.value.filter { it.id != id }
        _keywords.value = updated
    }

    /** Toggle enabled state without touching other fields. */
    fun setKeywordEnabled(id: String, enabled: Boolean) {
        val updated = _keywords.value.map {
            if (it.id == id) {
                val kw = it.copy(isEnabled = enabled)
                keywordDao.insertOrUpdate(kw.toKeywordEntity())
                kw
            } else {
                it
            }
        }
        _keywords.value = updated
    }

    /** Replace the full keyword list in one write. */
    fun replaceAllKeywords(keywords: List<KeywordDefinition>) {
        val normalized = keywords.map { keyword ->
            keyword.copy(
                name = keyword.name.trim(),
                relation = keyword.relation.trim(),
                aliases = keyword.aliases.map { it.trim() }.filter { it.isNotEmpty() }
            )
        }
        keywordDao.deleteAll()
        keywordDao.insertAll(normalized.map { it.toKeywordEntity() })
        _keywords.value = normalized
    }

    /** Look up a keyword by id. Returns null if not found. */
    fun getKeywordById(id: String): KeywordDefinition? =
        _keywords.value.firstOrNull { it.id == id }

    /** Update the fuzzy-match confidence threshold (0.0–1.0). Default 0.90. */
    fun setFuzzyThreshold(threshold: Float) {
        val clamped = threshold.coerceIn(0f, 1f)
        prefs.edit().putFloat(KEY_FUZZY_THRESHOLD, clamped).apply()
        _fuzzyThreshold.value = clamped
    }

    // ── Serialisation ─────────────────────────────────────────────────────

    private fun loadKeywords(): List<KeywordDefinition> {
        return try {
            keywordDao.getAllKeywordsSync().map { it.toKeywordDefinition() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun KeywordEntity.toKeywordDefinition(): KeywordDefinition = KeywordDefinition(
        id = id,
        name = name,
        type = KeywordType.valueOf(type),
        relation = relation,
        aliases = aliases,
        isEnabled = isEnabled,
        createdAt = createdAt
    )

    private fun KeywordDefinition.toKeywordEntity(): KeywordEntity = KeywordEntity(
        id = id,
        name = name,
        type = type.name,
        relation = relation,
        aliases = aliases,
        isEnabled = isEnabled,
        createdAt = createdAt
    )

    // ── Singleton ─────────────────────────────────────────────────────────

    companion object {
        @Volatile private var instance: KeywordRepository? = null

        fun getInstance(context: Context): KeywordRepository =
            instance ?: synchronized(this) {
                instance ?: KeywordRepository(context.applicationContext).also { instance = it }
            }

        private const val KEY_FUZZY_THRESHOLD = "fuzzy_threshold"
        private const val DEFAULT_FUZZY_THRESHOLD = 0.90f
    }
}
