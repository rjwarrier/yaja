package com.mj.yaja.data.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class JournalCacheMetadataStore(
    private val metadataFile: File,
    private val scope: CoroutineScope,
    private val logError: (String, Exception) -> Unit
) {
    @Volatile
    private var pendingSaveJob: Job? = null

    fun saveFingerprint(fingerprint: JournalStorageFingerprint?) {
        pendingSaveJob?.cancel()
        pendingSaveJob = scope.launch {
            delay(400)
            saveFingerprintBlocking(fingerprint)
        }
    }

    fun saveFingerprintBlocking(fingerprint: JournalStorageFingerprint?) {
        try {
            metadataFile.parentFile?.mkdirs()
            val backupFile = File(metadataFile.parentFile, "${metadataFile.name}.bak")
            if (metadataFile.exists()) {
                metadataFile.copyTo(backupFile, overwrite = true)
            }
            val tempFile = File(metadataFile.parentFile, "${metadataFile.name}.tmp")
            if (fingerprint == null) {
                if (tempFile.exists()) tempFile.delete()
                if (metadataFile.exists()) metadataFile.delete()
                if (backupFile.exists()) backupFile.delete()
                return
            }
            JournalCacheFileOps.writeTextCrashSafely(
                target = metadataFile,
                backup = backupFile,
                content = serializeFingerprint(fingerprint)
            )
        } catch (e: Exception) {
            logError("Failed to save journal cache metadata", e as? Exception ?: Exception(e))
        }
    }

    fun loadFingerprint(): JournalStorageFingerprint? {
        return loadFingerprintFrom(metadataFile)
            ?: loadFingerprintFrom(File(metadataFile.parentFile, "${metadataFile.name}.bak"))
    }

    fun clear() {
        pendingSaveJob?.cancel()
        if (metadataFile.exists()) metadataFile.delete()
        File(metadataFile.parentFile, "${metadataFile.name}.bak").delete()
    }

    private fun loadFingerprintFrom(file: File): JournalStorageFingerprint? {
        if (!file.exists()) return null
        return try {
            deserializeFingerprint(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            logError("Failed to load journal cache metadata from ${file.name}", e)
            null
        }
    }

    private fun serializeFingerprint(fingerprint: JournalStorageFingerprint): String =
        JSONObject()
            .put("storageKey", fingerprint.storageKey)
            .put("fileCount", fingerprint.fileCount)
            .put("newestModifiedAt", fingerprint.newestModifiedAt)
            .put("oldestModifiedAt", fingerprint.oldestModifiedAt)
            .put("metadataChecksum", fingerprint.metadataChecksum)
            .put("computedAt", fingerprint.computedAt)
            .toString()

    private fun deserializeFingerprint(serialized: String): JournalStorageFingerprint {
        val json = JSONObject(serialized)
        return JournalStorageFingerprint(
            storageKey = json.getString("storageKey"),
            fileCount = json.getInt("fileCount"),
            newestModifiedAt = json.optLong("newestModifiedAt", 0L),
            oldestModifiedAt = json.optLong("oldestModifiedAt", 0L),
            metadataChecksum = json.getLong("metadataChecksum"),
            computedAt = json.optLong("computedAt", 0L)
        )
    }
}
