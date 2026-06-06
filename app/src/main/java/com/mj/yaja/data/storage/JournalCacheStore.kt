package com.mj.yaja.data.storage

import java.io.File
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class JournalCacheStore(
    private val cache: ConcurrentHashMap<LocalDate, List<String>>,
    private val cacheFile: File,
    private val scope: CoroutineScope,
    private val logError: (String, Exception) -> Unit
) {
    private val backupFile: File
        get() = File(cacheFile.parentFile ?: File("."), "${cacheFile.name}.bak")
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
                val snapshot = snapshot()
                val json = JSONObject()
                snapshot.forEach { (date, entries) ->
                    val array = JSONArray()
                    entries.forEach { array.put(it) }
                    json.put(date.toString(), array)
                }
                val content = json.toString()
                writeAtomically(cacheFile, backupFile, content)
            }
        } catch (e: Exception) {
            logError("Failed to save cache to disk", e)
        }
    }

    fun loadFromDisk(): Boolean {
        val candidateFiles = listOf(cacheFile, backupFile).filter { it.exists() }
        if (candidateFiles.isEmpty()) return false

        candidateFiles.forEachIndexed { index, file ->
            try {
                val newCache = parseCacheJson(file.readText(Charsets.UTF_8))
                cache.clear()
                cache.putAll(newCache)
                if (index > 0) {
                    logError(
                        "Recovered journal cache from backup file ${file.name}",
                        IllegalStateException("Journal cache primary file was not usable at startup")
                    )
                }
                return true
            } catch (e: Exception) {
                val source = if (index == 0) "primary" else "backup"
                logError("Failed to load $source journal cache from disk", e)
            }
        }
        return false
    }

    fun snapshot(): Map<LocalDate, List<String>> =
        cache.mapValues { (_, entries) -> entries.toList() }.toMap()

    private fun parseCacheJson(raw: String): Map<LocalDate, List<String>> {
        val json = JSONObject(raw)
        val newCache = mutableMapOf<LocalDate, List<String>>()
        json.keys().forEach { key ->
            val date = LocalDate.parse(key)
            val array = json.getJSONArray(key)
            val entries = mutableListOf<String>()
            for (i in 0 until array.length()) {
                entries.add(array.getString(i))
            }
            newCache[date] = entries
        }
        return newCache
    }

    private fun writeAtomically(target: File, backup: File, content: String) {
        JournalCacheFileOps.writeTextCrashSafely(target, backup, content)
    }
}
