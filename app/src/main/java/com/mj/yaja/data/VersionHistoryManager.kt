package com.mj.yaja.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VersionHistoryManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope? = null
) {
    data class Snapshot(
        val date: LocalDate,
        val file: File,
        val createdAt: Long
    )

    private val historyRoot: File by lazy {
        File(context.noBackupFilesDir, HISTORY_DIR_NAME).also { migrateLegacyHistoryRoot(it) }
    }
    @Volatile private var lastGlobalPruneAt: Long = 0L

    fun snapshotBeforeMutation(date: LocalDate, reason: String, currentContent: String?): Boolean {
        if (!settingsRepository.versionHistoryEnabled.value) return true
        val content = currentContent?.takeIf { it.isNotBlank() } ?: return true

        return try {
            val dayDir = getDayDir(date).apply { mkdirs() }
            val latest = listSnapshots(date).firstOrNull()
            if (latest != null && runCatching { latest.file.readText(Charsets.UTF_8) }.getOrNull() == content) {
                pruneDateAsync(date)
                pruneAllIfDue()
                return true
            }

            val snapshotFile = File(dayDir, "${TIMESTAMP_FORMATTER.format(LocalDateTime.now())}_${sanitizeReason(reason)}.md")
            if (!writeSnapshotAtomically(snapshotFile, content)) {
                throw IOException("Unable to write version-history snapshot")
            }
            pruneDateAsync(date)
            pruneAllIfDue()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create version-history snapshot for $date", e)
            false
        }
    }

    fun listSnapshots(date: LocalDate): List<Snapshot> {
        val dir = getDayDir(date)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file -> file.isFile && file.extension == "md" }
            ?.map { file -> Snapshot(date = date, file = file, createdAt = file.lastModified()) }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    fun restoreSnapshot(snapshot: Snapshot): String? {
        return runCatching { snapshot.file.readText(Charsets.UTF_8) }
            .onFailure { Log.e(TAG, "Failed to read version-history snapshot", it) }
            .getOrNull()
    }

    fun pruneAll() {
        if (!historyRoot.exists()) return
        try {
            historyRoot.walkTopDown()
                .filter { it.isDirectory && DATE_DIR_PATTERN.matches(it.name) }
                .forEach { dateDir ->
                    runCatching {
                        LocalDate.parse(dateDir.name)
                    }.onSuccess { date ->
                        pruneDate(date)
                    }
                }
            deleteEmptyDirs(historyRoot)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prune version history", e)
        }
    }

    private fun pruneDate(date: LocalDate) {
        val dir = getDayDir(date)
        if (!dir.exists()) return

        val now = System.currentTimeMillis()
        val retentionMillis = settingsRepository.versionHistoryRetentionDays.value * ONE_DAY_MS
        val cutoff = now - retentionMillis
        val maxVersions = settingsRepository.versionHistoryMaxVersions.value

        val snapshots = listSnapshots(date)
        snapshots
            .filter { it.createdAt < cutoff }
            .forEach { it.file.delete() }

        listSnapshots(date)
            .drop(maxVersions)
            .forEach { it.file.delete() }

        deleteEmptyDirs(dir)
    }

    private fun pruneDateAsync(date: LocalDate) {
        val runner = { pruneDate(date) }
        if (scope != null) {
            scope.launch(Dispatchers.IO) { runner() }
        } else {
            runner()
        }
    }

    private fun pruneAllIfDue() {
        val now = System.currentTimeMillis()
        if (now - lastGlobalPruneAt < GLOBAL_PRUNE_INTERVAL_MS) return
        lastGlobalPruneAt = now
        val runner = { pruneAll() }
        if (scope != null) {
            scope.launch(Dispatchers.IO) { runner() }
        } else {
            runner()
        }
    }

    private fun getDayDir(date: LocalDate): File {
        val yearDir = File(historyRoot, date.year.toString())
        val monthDir = File(yearDir, "%02d".format(date.monthValue))
        return File(monthDir, date.toString())
    }

    private fun migrateLegacyHistoryRoot(newRoot: File) {
        val oldRoot = File(context.filesDir, HISTORY_DIR_NAME)
        if (!oldRoot.exists() || oldRoot.absolutePath == newRoot.absolutePath) return
        try {
            newRoot.mkdirs()
            val copied = copyDirectoryContentsSafely(oldRoot, newRoot)
            if (copied && hasAllRelativeFiles(oldRoot, newRoot)) {
                oldRoot.deleteRecursively()
            } else {
                Log.w(TAG, "Legacy version history kept in place because migration was incomplete")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not move legacy version history into no-backup storage", e)
        }
    }

    private fun writeSnapshotAtomically(target: File, content: String): Boolean {
        val parent = target.parentFile ?: return false
        parent.mkdirs()
        val tempFile = File(parent, ".${target.name}.${System.nanoTime()}.tmp")
        return try {
            FileOutputStream(tempFile).use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                output.fd.sync()
            }
            try {
                Files.move(
                    tempFile.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    tempFile.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            true
        } catch (e: Exception) {
            tempFile.delete()
            Log.e(TAG, "Failed to write version-history snapshot ${target.name}", e)
            false
        }
    }

    private fun copyDirectoryContentsSafely(source: File, target: File): Boolean {
        if (!source.isDirectory) return false
        var success = true
        source.listFiles().orEmpty().forEach { child ->
            val destination = File(target, child.name)
            success =
                if (child.isDirectory) {
                    destination.mkdirs()
                    copyDirectoryContentsSafely(child, destination) && success
                } else {
                    runCatching {
                        if (!destination.exists()) {
                            child.copyTo(destination, overwrite = false)
                        }
                    }.onFailure {
                        Log.w(TAG, "Failed to migrate history file ${child.name}", it)
                    }.isSuccess && success
                }
        }
        return success
    }

    private fun hasAllRelativeFiles(oldRoot: File, newRoot: File): Boolean {
        return oldRoot.walkTopDown()
            .filter { it.isFile }
            .all { oldFile ->
                val relative = oldFile.relativeTo(oldRoot)
                val newFile = File(newRoot, relative.path)
                newFile.exists() && newFile.length() == oldFile.length()
            }
    }

    private fun sanitizeReason(reason: String): String =
        reason.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "edit" }
            .take(32)

    private fun deleteEmptyDirs(root: File) {
        if (!root.isDirectory) return
        root.listFiles()?.forEach { child -> deleteEmptyDirs(child) }
        if (root != historyRoot && root.listFiles()?.isEmpty() == true) {
            root.delete()
        }
    }

    companion object {
        const val HISTORY_DIR_NAME = "History Files"
        private const val TAG = "VersionHistory"
        private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
        private const val GLOBAL_PRUNE_INTERVAL_MS = 6L * 60L * 60L * 1000L
        private val TIMESTAMP_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
        private val DATE_DIR_PATTERN = Regex("\\d{4}-\\d{2}-\\d{2}")
    }
}
