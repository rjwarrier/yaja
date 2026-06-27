package com.mj.yaja.data

import android.content.Context
import android.util.Log
import com.mj.yaja.data.database.JournalDatabase
import com.mj.yaja.data.database.TodoIndexEntity
import com.mj.yaja.data.storage.JournalStorageFingerprint
import java.util.concurrent.atomic.AtomicLong
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@androidx.compose.runtime.Immutable
data class TodoIndexEntry(
    val date: LocalDate,
    val entryIndex: Int,
    val lineIndexInEntry: Int,
    val displayText: String,
    val isChecked: Boolean,
    val dayLabel: String,
    val lineHash: String = "",
    val complianceId: String? = null
)

class TodoIndexRepository private constructor(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val sortOrder =
        compareByDescending<TodoIndexEntry> { it.date }
            .thenBy { it.entryIndex }
            .thenBy { it.lineIndexInEntry }
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _entries = MutableStateFlow<List<TodoIndexEntry>>(emptyList())
    val entries: StateFlow<List<TodoIndexEntry>> = _entries.asStateFlow()
    private val versionCounter = AtomicLong(System.currentTimeMillis())
    val liveVersion: Long
        get() = versionCounter.get()

    private val database = JournalDatabase.getDatabase(context)
    private val todoDao = database.todoIndexDao()

    companion object {
        private const val TAG = "YajaTodoPipeline"
        private const val PREFS_NAME = "todo_index_cache"
        private const val KEY_BUILT = "built"
        private const val KEY_FINGERPRINT = "fingerprint"

        @Volatile private var instance: TodoIndexRepository? = null

        fun getInstance(context: Context): TodoIndexRepository =
            instance ?: synchronized(this) {
                instance ?: TodoIndexRepository(context.applicationContext).also { instance = it }
            }
    }

    init {
        persistenceScope.launch {
            todoDao.observeAllTodos().collect { entities ->
                val parsed = entities.map { it.toTodoIndexEntry() }
                _entries.value = sortEntries(parsed)
                versionCounter.incrementAndGet()
            }
        }
    }

    @Synchronized
    fun isBuilt(): Boolean = prefs.getBoolean(KEY_BUILT, false)

    @Synchronized
    fun isCurrent(fingerprint: JournalStorageFingerprint?): Boolean =
        isBuilt() && fingerprintKey(fingerprint).isNotBlank() &&
            prefs.getString(KEY_FINGERPRINT, "") == fingerprintKey(fingerprint)

    @Synchronized
    fun markFingerprint(fingerprint: JournalStorageFingerprint?) {
        val key = fingerprintKey(fingerprint)
        if (key.isBlank()) return
        prefs.edit().putString(KEY_FINGERPRINT, key).apply()
    }

    @Synchronized
    fun getEntries(showCompleted: Boolean = true): List<TodoIndexEntry> =
        _entries.value
            .asSequence()
            .filter { showCompleted || !it.isChecked }
            .distinctBy { it.identityKey() }
            .toList()

    @Synchronized
    fun getTodoItems(): List<TodoItem> =
        toTodoItems(_entries.value)

    fun toTodoItems(entries: List<TodoIndexEntry>): List<TodoItem> =
        entries.distinctBy { it.identityKey() }.map {
            TodoItem(
                date = it.date,
                entryIndex = it.entryIndex,
                lineIndexInEntry = it.lineIndexInEntry,
                displayText = it.displayText,
                isChecked = it.isChecked,
                lineHash = it.lineHash,
                sourceType = if (it.complianceId != null) TodoSourceType.COMPLIANCE else TodoSourceType.JOURNAL,
                sourceId = it.complianceId
            )
        }

    @Synchronized
    fun replaceDate(
        date: LocalDate,
        entries: List<String>,
        dayLabel: String,
        fingerprint: JournalStorageFingerprint? = null
    ) {
        val parsed = parseDate(date, entries, dayLabel).map { it.toTodoIndexEntity() }
        persistenceScope.launch {
            todoDao.deleteByDate(date.toString())
            if (parsed.isNotEmpty()) {
                todoDao.insertAll(parsed)
            }
        }
        markFingerprint(fingerprint)
        prefs.edit().putBoolean(KEY_BUILT, true).apply()
    }

    @Synchronized
    fun removeDate(date: LocalDate, fingerprint: JournalStorageFingerprint? = null) {
        persistenceScope.launch {
            todoDao.deleteByDate(date.toString())
        }
        markFingerprint(fingerprint)
    }

    @Synchronized
    fun setCheckedState(
        date: LocalDate,
        entryIndex: Int,
        lineIndexInEntry: Int,
        lineHash: String?,
        displayText: String?,
        isChecked: Boolean
    ): Boolean {
        var changed = false
        val normalizedDisplayText = displayText.orEmpty().trim().lowercase()
        val matches = _entries.value.any { entry ->
            val positionMatches =
                entry.date == date &&
                    entry.entryIndex == entryIndex &&
                    entry.lineIndexInEntry == lineIndexInEntry
            val hashMatches =
                entry.date == date &&
                    !lineHash.isNullOrBlank() &&
                    entry.lineHash == lineHash
            val textMatches =
                entry.date == date &&
                    normalizedDisplayText.isNotBlank() &&
                    entry.displayText.trim().lowercase() == normalizedDisplayText
            val shouldUpdate =
                when {
                    positionMatches -> true
                    !lineHash.isNullOrBlank() -> hashMatches
                    normalizedDisplayText.isNotBlank() -> textMatches
                    else -> false
                }
            shouldUpdate
        }

        if (matches) {
            changed = true
            persistenceScope.launch {
                if (!lineHash.isNullOrBlank()) {
                    todoDao.updateCheckedStateByHash(date.toString(), lineHash, isChecked)
                } else if (!displayText.isNullOrBlank()) {
                    todoDao.updateCheckedStateByText(date.toString(), displayText, isChecked)
                } else {
                    todoDao.updateCheckedState(date.toString(), entryIndex, lineIndexInEntry, isChecked)
                }
            }
        }
        return changed
    }

    @Synchronized
    fun rebuild(
        dates: Iterable<LocalDate>,
        entriesForDate: (LocalDate) -> List<String>,
        dayLabelForDate: (LocalDate) -> String,
        fingerprint: JournalStorageFingerprint? = null
    ) {
        val rebuilt = mutableListOf<TodoIndexEntity>()
        dates.toSet().forEach { date ->
            rebuilt += parseDate(date, entriesForDate(date), dayLabelForDate(date)).map { it.toTodoIndexEntity() }
        }
        persistenceScope.launch {
            val dateStrings = dates.map { it.toString() }
            if (dateStrings.isNotEmpty()) {
                todoDao.deleteByDates(dateStrings)
            }
            if (rebuilt.isNotEmpty()) {
                todoDao.insertAll(rebuilt)
            }
        }
        markFingerprint(fingerprint)
        prefs.edit().putBoolean(KEY_BUILT, true).apply()
    }

    @Synchronized
    fun clearAll() {
        persistenceScope.launch {
            todoDao.deleteAll()
        }
        prefs.edit().putBoolean(KEY_BUILT, false).remove(KEY_FINGERPRINT).apply()
    }

    private fun fingerprintKey(fingerprint: JournalStorageFingerprint?): String =
        fingerprint?.let {
            listOf(
                it.storageKey,
                it.fileCount,
                it.newestModifiedAt,
                it.oldestModifiedAt,
                it.metadataChecksum
            ).joinToString("|")
        }.orEmpty()

    private fun parseDate(
        date: LocalDate,
        entries: List<String>,
        dayLabel: String
    ): List<TodoIndexEntry> {
        val rows = mutableListOf<TodoIndexEntry>()
        entries.forEachIndexed { entryIndex, entry ->
            entry.lines().forEachIndexed { lineIndex, line ->
                val parsed = TodoParser.parseLine(line) ?: return@forEachIndexed
                rows += TodoIndexEntry(
                    date = date,
                    entryIndex = entryIndex,
                    lineIndexInEntry = lineIndex,
                    displayText = parsed.displayText,
                    isChecked = parsed.isChecked,
                    dayLabel = dayLabel,
                    lineHash = parsed.lineHash,
                    complianceId = parsed.complianceId
                )
            }
        }
        return rows
    }

    private fun sortEntries(entries: List<TodoIndexEntry>): List<TodoIndexEntry> =
        entries.distinctBy { it.identityKey() }.sortedWith(sortOrder)

    private fun TodoIndexEntry.identityKey(): String =
        listOf(date, entryIndex, lineIndexInEntry, lineHash.ifBlank { displayText.lowercase() })
            .joinToString("|")

    private fun TodoIndexEntity.toTodoIndexEntry() = TodoIndexEntry(
        date = LocalDate.parse(date),
        entryIndex = entryIndex,
        lineIndexInEntry = lineIndexInEntry,
        displayText = displayText,
        isChecked = isChecked,
        dayLabel = dayLabel,
        lineHash = lineHash,
        complianceId = complianceId
    )

    private fun TodoIndexEntry.toTodoIndexEntity() = TodoIndexEntity(
        date = date.toString(),
        entryIndex = entryIndex,
        lineIndexInEntry = lineIndexInEntry,
        displayText = displayText,
        isChecked = isChecked,
        dayLabel = dayLabel,
        lineHash = lineHash,
        complianceId = complianceId
    )
}
