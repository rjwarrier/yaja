package com.mj.yaja.data.storage

import java.io.File
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class JournalLabelsStore(
    private val dayLabels: ConcurrentHashMap<LocalDate, String>,
    private val starredDates: ConcurrentHashMap<LocalDate, String>,
    private val revisitDates: ConcurrentHashMap<LocalDate, LocalDate>,
    private val revisitNotes: ConcurrentHashMap<LocalDate, String>,
    private val labelsCacheFile: File,
    private val scope: CoroutineScope,
    private val logError: (String, Exception) -> Unit
) {
    private val backupFile: File
        get() = File(labelsCacheFile.parentFile ?: File("."), "${labelsCacheFile.name}.bak")
    private val saveLock = Any()
    @Volatile private var saveJob: Job? = null

    fun saveToDisk() {
        synchronized(saveLock) {
            saveJob?.cancel()
            saveJob = scope.launch {
                delay(300)
                writeSnapshotToDisk()
            }
        }
    }

    fun saveToDiskBlocking() {
        synchronized(saveLock) {
            saveJob?.cancel()
            saveJob = null
        }
        writeSnapshotToDisk()
    }

    private fun writeSnapshotToDisk() {
        try {
            synchronized(saveLock) {
                val dayLabelSnapshot = dayLabels.toMap()
                val starredSnapshot = starredDates.toMap()
                val labelsObj = JSONObject()
                dayLabelSnapshot.forEach { (date, label) -> labelsObj.put(date.toString(), label) }
                val starredObj = JSONObject()
                starredSnapshot.forEach { (date, label) -> starredObj.put(date.toString(), label) }
                val revisitDateSnapshot = revisitDates.toMap()
                val revisitNoteSnapshot = revisitNotes.toMap()
                val revisitDatesObj = JSONObject()
                revisitDateSnapshot.forEach { (date, revisitOn) ->
                    revisitDatesObj.put(date.toString(), revisitOn.toString())
                }
                val revisitNotesObj = JSONObject()
                revisitNoteSnapshot.forEach { (date, note) ->
                    revisitNotesObj.put(date.toString(), note)
                }
                val json = JSONObject()
                json.put("dayLabels", labelsObj)
                json.put("starredDates", starredObj)
                json.put("revisitDates", revisitDatesObj)
                json.put("revisitNotes", revisitNotesObj)
                val content = json.toString()
                parseLabelsJson(content)
                writeAtomically(labelsCacheFile, backupFile, content)
            }
        } catch (e: Exception) {
            logError("Failed to save labels cache to disk", e)
        }
    }

    fun loadFromDisk(): Boolean {
        val candidateFiles = listOf(labelsCacheFile, backupFile).filter { it.exists() }
        if (candidateFiles.isEmpty()) return false
        candidateFiles.forEachIndexed { index, file ->
            try {
                val parsed = parseLabelsJson(file.readText(Charsets.UTF_8))
                dayLabels.clear()
                dayLabels.putAll(parsed.dayLabels)
                starredDates.clear()
                starredDates.putAll(parsed.starredDates)
                revisitDates.clear()
                revisitDates.putAll(parsed.revisitDates)
                revisitNotes.clear()
                revisitNotes.putAll(parsed.revisitNotes)
                if (index > 0) {
                    logError(
                        "Recovered labels cache from backup file ${file.name}",
                        IllegalStateException("Labels cache primary file was not usable at startup")
                    )
                }
                return true
            } catch (e: Exception) {
                val source = if (index == 0) "primary" else "backup"
                logError("Failed to load $source labels cache from disk", e)
            }
        }
        return false
    }

    private fun parseLabelsJson(raw: String): JournalRevisitAndLabelSnapshot {
        val json = JSONObject(raw)
        val labelsObj = json.optJSONObject("dayLabels")
        val starredObj = json.optJSONObject("starredDates")
        val revisitDatesObj = json.optJSONObject("revisitDates")
        val revisitNotesObj = json.optJSONObject("revisitNotes")
        val newDayLabels = mutableMapOf<LocalDate, String>()
        val newStarredDates = mutableMapOf<LocalDate, String>()
        val newRevisitDates = mutableMapOf<LocalDate, LocalDate>()
        val newRevisitNotes = mutableMapOf<LocalDate, String>()
        labelsObj?.keys()?.forEach { key ->
            newDayLabels[LocalDate.parse(key)] = labelsObj.getString(key)
        }
        starredObj?.keys()?.forEach { key ->
            newStarredDates[LocalDate.parse(key)] = starredObj.getString(key)
        }
        revisitDatesObj?.keys()?.forEach { key ->
            newRevisitDates[LocalDate.parse(key)] = LocalDate.parse(revisitDatesObj.getString(key))
        }
        revisitNotesObj?.keys()?.forEach { key ->
            newRevisitNotes[LocalDate.parse(key)] = revisitNotesObj.getString(key)
        }
        return JournalRevisitAndLabelSnapshot(
            dayLabels = newDayLabels,
            starredDates = newStarredDates,
            revisitDates = newRevisitDates,
            revisitNotes = newRevisitNotes
        )
    }

    private fun writeAtomically(target: File, backup: File, content: String) {
        JournalCacheFileOps.writeTextCrashSafely(target, backup, content)
    }
}

private data class JournalRevisitAndLabelSnapshot(
    val dayLabels: Map<LocalDate, String>,
    val starredDates: Map<LocalDate, String>,
    val revisitDates: Map<LocalDate, LocalDate>,
    val revisitNotes: Map<LocalDate, String>
)
