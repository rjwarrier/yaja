package com.mj.yaja.data.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object TodoIndexMigrator {
    private const val TAG = "TodoIndexMigrator"
    private const val PREFS_NAME = "todo_index_cache"
    private const val KEY_ENTRIES = "entries"

    suspend fun migrateIfNeeded(context: Context, database: JournalDatabase) = withContext(Dispatchers.IO) {
        val dao = database.todoIndexDao()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            // Only migrate if Room todo table is completely empty
            if (dao.getCount() > 0) {
                Log.d(TAG, "Room todo_index table already has entries. Skipping legacy JSON migration.")
                return@withContext
            }

            val rawJson = prefs.getString(KEY_ENTRIES, null)
            if (rawJson.isNullOrBlank()) {
                Log.d(TAG, "No legacy todo index JSON found in SharedPreferences. Skipping migration.")
                return@withContext
            }

            Log.i(TAG, "Starting legacy todo SharedPreferences to Room migration...")

            val array = JSONArray(rawJson)
            val entities = mutableListOf<TodoIndexEntity>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                entities.add(
                    TodoIndexEntity(
                        date = obj.getString("date"),
                        entryIndex = obj.getInt("entryIndex"),
                        lineIndexInEntry = obj.getInt("lineIndexInEntry"),
                        displayText = obj.getString("displayText"),
                        isChecked = obj.getBoolean("isChecked"),
                        dayLabel = obj.optString("dayLabel", ""),
                        lineHash = obj.optString("lineHash", "")
                    )
                )
            }

            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
                Log.i(TAG, "Successfully migrated ${entities.size} todo index entries to Room database.")
            }

            // Backup and clear the key
            prefs.edit()
                .putString("${KEY_ENTRIES}_migrated_bak", rawJson)
                .remove(KEY_ENTRIES)
                .apply()
            Log.d(TAG, "Backed up legacy SharedPreferences todo JSON and cleaned key.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to run legacy todo migration", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "Todo index migration failed. Using database defaults.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
