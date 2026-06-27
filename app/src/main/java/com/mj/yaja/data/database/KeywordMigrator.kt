package com.mj.yaja.data.database

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object KeywordMigrator {
    private const val TAG = "KeywordMigrator"
    private const val PREFS_NAME = "keyword_prefs"
    private const val KEY_KEYWORDS_JSON = "keywords_json"

    suspend fun migrateIfNeeded(context: Context, database: JournalDatabase) = withContext(Dispatchers.IO) {
        val dao = database.keywordDao()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        try {
            // Only migrate if Room keywords table is completely empty
            if (dao.getCount() > 0) {
                Log.d(TAG, "Room keywords table already has entries. Skipping legacy JSON migration.")
                return@withContext
            }

            val rawJson = prefs.getString(KEY_KEYWORDS_JSON, null)
            if (rawJson.isNullOrBlank()) {
                Log.d(TAG, "No legacy keywords JSON found in SharedPreferences. Skipping migration.")
                return@withContext
            }

            Log.i(TAG, "Starting legacy keywords SharedPreferences to Room migration...")

            val array = JSONArray(rawJson)
            val entities = mutableListOf<KeywordEntity>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val aliasArr = obj.optJSONArray("a") ?: JSONArray() // "a" is F_ALIASES
                val aliasesList = (0 until aliasArr.length()).map { aliasArr.getString(it) }

                entities.add(
                    KeywordEntity(
                        id = obj.getString("id"),
                        name = obj.getString("n"), // "n" is F_NAME
                        type = obj.getString("t"), // "t" is F_TYPE
                        relation = obj.optString("r", ""), // "r" is F_RELATION
                        aliases = aliasesList,
                        isEnabled = obj.optBoolean("e", true), // "e" is F_ENABLED
                        createdAt = obj.optLong("c", 0L) // "c" is F_CREATED_AT
                    )
                )
            }

            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
                Log.i(TAG, "Successfully migrated ${entities.size} keyword definitions to Room database.")
            }

            // Rename key to avoid double migration
            prefs.edit()
                .putString("${KEY_KEYWORDS_JSON}_migrated_bak", rawJson)
                .remove(KEY_KEYWORDS_JSON)
                .apply()
            Log.d(TAG, "Backed up legacy SharedPreferences keyword JSON and cleaned key.")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to run legacy keywords migration", e)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "Keywords migration failed. Using database defaults.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
