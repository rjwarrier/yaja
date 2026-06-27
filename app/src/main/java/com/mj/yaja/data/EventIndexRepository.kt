package com.mj.yaja.data

import android.content.Context
import android.util.Log
import com.mj.yaja.data.database.JournalDatabase
import com.mj.yaja.data.database.EventIndexEntity
import com.mj.yaja.data.storage.JournalStorageFingerprint
import com.mj.yaja.ui.utils.MarkdownUtils
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventIndexRepository private constructor(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _entries = MutableStateFlow<List<EventItem>>(emptyList())
    val entries: StateFlow<List<EventItem>> = _entries.asStateFlow()
    private val versionCounter = AtomicLong(System.currentTimeMillis())
    val liveVersion: Long
        get() = versionCounter.get()

    private val database = JournalDatabase.getDatabase(context)
    private val eventDao = database.eventIndexDao()

    companion object {
        private const val TAG = "YajaEventPipeline"
        private const val PREFS_NAME = "event_index_cache"
        private const val KEY_BUILT = "built"
        private const val KEY_FINGERPRINT = "fingerprint"
        private val TIME_REGEX = Regex("""<!--time:(\d{2}:\d{2})-->""")

        @Volatile private var instance: EventIndexRepository? = null

        fun getInstance(context: Context): EventIndexRepository =
            instance ?: synchronized(this) {
                instance ?: EventIndexRepository(context.applicationContext).also { instance = it }
            }
    }

    init {
        persistenceScope.launch {
            eventDao.observeAllEvents().collect { entities ->
                val parsed = entities.map { it.toEventItem() }
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
    fun getEntries(): List<EventItem> =
        _entries.value.distinctBy { it.identityKey() }

    @Synchronized
    fun replaceDate(
        date: LocalDate,
        entries: List<String>,
        fingerprint: JournalStorageFingerprint? = null
    ) {
        val parsed = parseDate(date, entries).map { it.toEventIndexEntity() }
        persistenceScope.launch {
            eventDao.deleteByDate(date.toString())
            if (parsed.isNotEmpty()) {
                eventDao.insertAll(parsed)
            }
        }
        writeFingerprint(fingerprint)
    }

    @Synchronized
    fun removeDate(date: LocalDate, fingerprint: JournalStorageFingerprint? = null) {
        persistenceScope.launch {
            eventDao.deleteByDate(date.toString())
        }
        writeFingerprint(fingerprint)
    }

    @Synchronized
    fun rebuild(
        dates: Iterable<LocalDate>,
        entriesForDate: (LocalDate) -> List<String>,
        fingerprint: JournalStorageFingerprint? = null
    ) {
        val rebuilt = mutableListOf<EventIndexEntity>()
        dates.toSet().forEach { date ->
            rebuilt += parseDate(date, entriesForDate(date)).map { it.toEventIndexEntity() }
        }
        persistenceScope.launch {
            val dateStrings = dates.map { it.toString() }
            if (dateStrings.isNotEmpty()) {
                eventDao.deleteByDates(dateStrings)
            }
            if (rebuilt.isNotEmpty()) {
                eventDao.insertAll(rebuilt)
            }
        }
        writeFingerprint(fingerprint)
    }

    @Synchronized
    fun clearAll() {
        persistenceScope.launch {
            eventDao.deleteAll()
        }
        prefs.edit().putBoolean(KEY_BUILT, false).remove(KEY_FINGERPRINT).apply()
    }

    private fun writeFingerprint(fingerprint: JournalStorageFingerprint?, built: Boolean = true) {
        val key = fingerprintKey(fingerprint)
        val editor = prefs.edit().putBoolean(KEY_BUILT, built)
        if (key.isNotBlank()) {
            editor.putString(KEY_FINGERPRINT, key)
        }
        editor.apply()
    }

    private fun parseDate(date: LocalDate, entries: List<String>): List<EventItem> =
        entries.mapIndexedNotNull { entryIndex, entry ->
            if (parseEntryKind(entry) != EntryKind.EVENT) return@mapIndexedNotNull null
            val cleanText = MarkdownUtils.stripMetadata(stripEntryKindMetadata(entry)).trim()
            val displayText = cleanText.ifBlank { "Event" }
            EventItem(
                date = date,
                entryIndex = entryIndex,
                recordedTime = TIME_REGEX.find(entry)?.groupValues?.getOrNull(1),
                mentionedTime = extractMentionedEventTime(displayText),
                displayText = displayText,
                lineHash = "event-${date}-${entryIndex}-${displayText.lowercase()}"
            )
        }

    private fun sortEntries(entries: List<EventItem>): List<EventItem> =
        entries.distinctBy { it.identityKey() }
            .sortedWith(compareByDescending<EventItem> { it.date }.thenBy { it.entryIndex })

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

    private fun EventItem.identityKey(): String =
        listOf(date, entryIndex, lineHash.ifBlank { displayText.lowercase() }).joinToString("|")

    private fun EventIndexEntity.toEventItem() = EventItem(
        date = LocalDate.parse(date),
        entryIndex = entryIndex,
        recordedTime = recordedTime,
        mentionedTime = mentionedTime,
        displayText = displayText,
        lineHash = lineHash
    )

    private fun EventItem.toEventIndexEntity() = EventIndexEntity(
        date = date.toString(),
        entryIndex = entryIndex,
        recordedTime = recordedTime,
        mentionedTime = mentionedTime,
        displayText = displayText,
        lineHash = lineHash
    )
}
