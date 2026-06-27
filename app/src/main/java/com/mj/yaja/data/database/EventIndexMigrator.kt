package com.mj.yaja.data.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object EventIndexMigrator {
    private const val TAG = "EventIndexMigrator"
    private const val PREFS_NAME = "event_index_cache"
    private const val KEY_ENTRIES = "entries"

    suspend fun migrateIfNeeded(context: Context, database: JournalDatabase) = withContext(Dispatchers.IO) {
        val dao = database.eventIndexDao()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            // Only migrate if Room event table is completely empty
            if (dao.getCount() > 0) {
                Log.d(TAG, "Room event_index table already has entries. Skipping legacy JSON migration.")
                return@withContext
            }

            val rawJson = prefs.getString(KEY_ENTRIES, null)
            if (rawJson.isNullOrBlank()) {
                Log.d(TAG, "No legacy event index JSON found in SharedPreferences. Skipping migration.")
                return@withContext
            }

            Log.i(TAG, "Starting legacy event SharedPreferences to Room migration...")

            val array = JSONArray(rawJson)
            val entities = mutableListOf<EventIndexEntity>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                entities.add(
                    EventIndexEntity(
                        date = obj.getString("date"),
                        entryIndex = obj.getInt("entryIndex"),
                        recordedTime = obj.optString("recordedTime").takeIf { it.isNotBlank() },
                        mentionedTime = obj.optString("mentionedTime").takeIf { it.isNotBlank() },
                        displayText = obj.getString("displayText"),
                        lineHash = obj.optString("lineHash", "")
                    )
                )
            }

            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
                Log.i(TAG, "Successfully migrated ${entities.size} event index entries to Room database.")
            }

            // Backup and clear the key
            prefs.edit()
                .putString("${KEY_ENTRIES}_migrated_bak", rawJson)
                .remove(KEY_ENTRIES)
                .apply()
            Log.d(TAG, "Backed up legacy SharedPreferences event JSON and cleaned key.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to run legacy event migration", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "Event index migration failed. Using database defaults.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
