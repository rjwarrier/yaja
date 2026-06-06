package com.mj.yaja.data.database

import android.content.Context
import android.util.Log
import com.mj.yaja.data.countWordsIgnoringChecklistMarkers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

object JsonToRoomMigrator {
    private const val TAG = "JsonToRoomMigrator"

    suspend fun migrateIfNeeded(context: Context, database: JournalDatabase) = withContext(Dispatchers.IO) {
        val dao = database.journalCacheDao()
        val filesDir = context.filesDir
        val diskCacheFile = File(filesDir, "journal_cache_v1.json")
        val labelsFile = File(filesDir, "journal_labels_cache_v1.json")
        val entryCountFile = File(filesDir, "journal_entry_count_cache_v1.json")
        val wordCountFile = File(filesDir, "journal_word_count_cache_v1.json")
        val metadataFile = File(filesDir, "journal_date_metadata_cache_v1.json")
        var migrationAttempted = false

        try {
            // Only migrate if Room cache is completely empty for the default journal
            if (dao.getCount("default") > 0) {
                Log.d(TAG, "Room database already has cache entries. Skipping legacy JSON migration.")
                return@withContext
            }

            if (!diskCacheFile.exists()) {
                Log.d(TAG, "Legacy journal cache JSON file does not exist. Skipping migration.")
                return@withContext
            }

            migrationAttempted = true
            Log.i(TAG, "Starting legacy JSON to Room database migration...")

            // Parse legacy cache: Map of date String -> List<String>
            val cacheData = parseJsonCache(diskCacheFile)
            if (cacheData.isEmpty()) {
                Log.d(TAG, "Legacy cache file is empty. Skipping migration.")
                return@withContext
            }

            // Load optional associated cache files
            val labelsSnapshot = parseLabelsCache(labelsFile)
            val entryCountMap = parseIntegerMap(entryCountFile)
            val wordCountMap = parseIntegerMap(wordCountFile)
            val metadataMap = parseMetadataCache(metadataFile)

            val entities = mutableListOf<JournalDayCacheEntity>()

            cacheData.forEach { (dateStr, entries) ->
                val isStarred = labelsSnapshot.starredDates.containsKey(dateStr)
                // Get label from dayLabels, fallback to starred label
                val label = labelsSnapshot.dayLabels[dateStr] ?: labelsSnapshot.starredDates[dateStr] ?: ""
                val revisitOn = labelsSnapshot.revisitDates[dateStr]
                val revisitNote = labelsSnapshot.revisitNotes[dateStr] ?: ""
                val entryCount = entryCountMap[dateStr] ?: entries.size
                val wordCount = wordCountMap[dateStr] ?: countWordsIgnoringChecklistMarkers(entries)

                val meta = metadataMap[dateStr]
                val fileSize = meta?.first ?: 0L
                val fileModifiedAt = meta?.second ?: 0L

                entities.add(
                    JournalDayCacheEntity(
                        date = dateStr,
                        journalId = "default",
                        entries = entries,
                        isStarred = isStarred,
                        label = label,
                        revisitOn = revisitOn,
                        revisitNote = revisitNote,
                        wordCount = wordCount,
                        entryCount = entryCount,
                        fileModifiedAt = fileModifiedAt,
                        fileSize = fileSize
                    )
                )
            }

            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
                Log.i(TAG, "Successfully migrated ${entities.size} journal cache entries to Room database.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to run legacy JSON to Room database migration", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "Cache migration failed. Re-indexing journal from Markdown files...",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } finally {
            if (migrationAttempted) {
                // Rename legacy files to .bak to avoid repeating migration and clean up active directory
                renameLegacyFileToBak(diskCacheFile)
                renameLegacyFileToBak(labelsFile)
                renameLegacyFileToBak(entryCountFile)
                renameLegacyFileToBak(wordCountFile)
                renameLegacyFileToBak(metadataFile)
            }
        }
    }

    private fun parseJsonCache(file: File): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        if (file.exists()) {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            json.keys().forEach { key ->
                val array = json.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                result[key] = list
            }
        }
        return result
    }

    private data class LabelsSnapshot(
        val dayLabels: Map<String, String>,
        val starredDates: Map<String, String>,
        val revisitDates: Map<String, String>,
        val revisitNotes: Map<String, String>
    )

    private fun parseLabelsCache(file: File): LabelsSnapshot {
        val dayLabels = mutableMapOf<String, String>()
        val starredDates = mutableMapOf<String, String>()
        val revisitDates = mutableMapOf<String, String>()
        val revisitNotes = mutableMapOf<String, String>()

        if (file.exists()) {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val dayLabelsObj = json.optJSONObject("dayLabels")
            val starredDatesObj = json.optJSONObject("starredDates")
            val revisitDatesObj = json.optJSONObject("revisitDates")
            val revisitNotesObj = json.optJSONObject("revisitNotes")

            dayLabelsObj?.keys()?.forEach { key -> dayLabels[key] = dayLabelsObj.getString(key) }
            starredDatesObj?.keys()?.forEach { key -> starredDates[key] = starredDatesObj.getString(key) }
            revisitDatesObj?.keys()?.forEach { key -> revisitDates[key] = revisitDatesObj.getString(key) }
            revisitNotesObj?.keys()?.forEach { key -> revisitNotes[key] = revisitNotesObj.getString(key) }
        }

        return LabelsSnapshot(dayLabels, starredDates, revisitDates, revisitNotes)
    }

    private fun parseIntegerMap(file: File): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        if (file.exists()) {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            json.keys().forEach { key ->
                result[key] = json.getInt(key)
            }
        }
        return result
    }

    private fun parseMetadataCache(file: File): Map<String, Pair<Long, Long>> {
        val result = mutableMapOf<String, Pair<Long, Long>>()
        if (file.exists()) {
            val json = JSONObject(file.readText(Charsets.UTF_8))
            json.keys().forEach { key ->
                val obj = json.getJSONObject(key)
                val size = obj.optLong("size", 0L)
                val modifiedAt = obj.optLong("modifiedAt", 0L)
                result[key] = Pair(size, modifiedAt)
            }
        }
        return result
    }

    private fun renameLegacyFileToBak(file: File) {
        if (file.exists()) {
            try {
                val bakFile = File(file.parentFile, "${file.name}.bak")
                if (bakFile.exists()) {
                    bakFile.delete()
                }
                file.renameTo(bakFile)
                Log.d(TAG, "Renamed legacy file ${file.name} to ${bakFile.name}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to rename legacy file: ${file.name}", e)
            }
        }
    }
}
