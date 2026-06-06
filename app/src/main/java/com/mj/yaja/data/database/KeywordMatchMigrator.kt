package com.mj.yaja.data.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object KeywordMatchMigrator {
    private const val TAG = "KeywordMatchMigrator"
    private const val CACHE_FILE_NAME = "keyword_matches_cache_v1.json"

    suspend fun migrateIfNeeded(context: Context, database: JournalDatabase) = withContext(Dispatchers.IO) {
        val dao = database.keywordMatchDao()
        val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
        val backupFile = File(context.filesDir, "$CACHE_FILE_NAME.bak")

        try {
            // Only migrate if Room table is completely empty
            if (dao.getCount() > 0) {
                Log.d(TAG, "Room keyword_matches table already has entries. Skipping legacy JSON migration.")
                return@withContext
            }

            val targetFile = when {
                cacheFile.exists() -> cacheFile
                backupFile.exists() -> backupFile
                else -> null
            }

            if (targetFile == null) {
                Log.d(TAG, "No legacy keyword matches cache JSON file found. Skipping migration.")
                return@withContext
            }

            Log.i(TAG, "Starting legacy keyword matches file to Room migration...")

            val json = targetFile.readText(Charsets.UTF_8)
            if (json.isBlank()) {
                Log.d(TAG, "Legacy keyword matches cache file is empty. Skipping migration.")
                return@withContext
            }

            val root = JSONObject(json)
            val entities = mutableListOf<KeywordMatchEntity>()

            for (keywordId in root.keys()) {
                val dateObj = root.optJSONObject(keywordId) ?: continue
                for (dateStr in dateObj.keys()) {
                    val arr = dateObj.optJSONArray(dateStr) ?: continue
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        entities.add(
                            KeywordMatchEntity(
                                keywordId = o.getString("ki"),
                                date = o.getString("d"),
                                entryIndex = o.getInt("ei"),
                                matchedText = o.getString("mt"),
                                confidence = o.getDouble("c").toFloat(),
                                matchType = o.getString("tp"),
                                snippet = o.optString("sn", ""),
                                startIndex = o.optInt("si", -1),
                                endExclusive = o.optInt("ee", -1)
                            )
                        )
                    }
                }
            }

            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
                Log.i(TAG, "Successfully migrated ${entities.size} keyword matches to Room database.")
            }

            // Rename files to .bak to clean up active directory
            renameToBak(cacheFile)
            renameToBak(backupFile)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to run legacy keyword matches migration", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "Keyword matches migration failed. Using database defaults.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun renameToBak(file: File) {
        if (file.exists()) {
            try {
                val bakFile = File(file.parentFile, "${file.name}.bak")
                if (bakFile.exists()) {
                    bakFile.delete()
                }
                file.renameTo(bakFile)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to rename legacy file: ${file.name}", e)
            }
        }
    }
}
