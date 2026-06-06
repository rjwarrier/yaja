package com.mj.yaja.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLogRepository private constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val logScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun logInfo(event: String, details: String = "") {
        appendAsync(level = "INFO", event = event, details = details)
    }

    fun logWarning(event: String, details: String = "") {
        appendAsync(level = "WARN", event = event, details = details)
    }

    fun logError(event: String, throwable: Throwable? = null, details: String = "") {
        appendAsync(
            level = "ERROR",
            event = event,
            details = buildString {
                if (details.isNotBlank()) append(details.trim())
                if (throwable != null) {
                    if (isNotEmpty()) append('\n')
                    append(stackTraceText(throwable))
                }
            }
        )
    }

    fun logCrashBlocking(
        event: String,
        throwable: Throwable,
        details: String = "",
        threadName: String = ""
    ) {
        val crashCount = incrementCrashCountBlocking()
        appendBlocking(
            level = "ERROR",
            event = event,
            details = crashReportText(
                throwable = throwable,
                details = details,
                threadName = threadName,
                crashCount = crashCount
            )
        )
    }

    fun readLog(): String =
        runCatching {
            val storageUri = settingsRepository.storageUri.value
            if (storageUri != null) {
                getSafLogFile(storageUri, createIfMissing = false)?.let { file ->
                    context.contentResolver.openInputStream(file.uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                }.orEmpty()
            } else {
                localLogFile().takeIf { it.exists() }?.readText(Charsets.UTF_8).orEmpty()
            }
        }.getOrElse {
            Log.w(TAG, "Failed to read app log", it)
            ""
        }

    fun clearLog(): Boolean =
        runCatching {
            val storageUri = settingsRepository.storageUri.value
            if (storageUri != null) {
                val file = getSafLogFile(storageUri, createIfMissing = false) ?: return@runCatching true
                context.contentResolver.openOutputStream(file.uri, "wt")
                    ?.use { it.write("".toByteArray(Charsets.UTF_8)) }
                    ?: return@runCatching false
                true
            } else {
                val file = localLogFile()
                if (file.exists()) file.writeText("", Charsets.UTF_8)
                true
            }
        }.getOrElse {
            Log.w(TAG, "Failed to clear app log", it)
            false
        }

    fun pruneLogBlocking() {
        runCatching {
            val storageUri = settingsRepository.storageUri.value
            if (storageUri != null) {
                val file = getSafLogFile(storageUri, createIfMissing = false) ?: return@runCatching
                val current =
                    context.contentResolver.openInputStream(file.uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()
                context.contentResolver.openOutputStream(file.uri, "wt")
                    ?.use { it.write(pruneByRetention(current).toByteArray(Charsets.UTF_8)) }
            } else {
                val file = localLogFile()
                if (file.exists()) file.writeText(pruneByRetention(file.readText(Charsets.UTF_8)), Charsets.UTF_8)
            }
        }.onFailure {
            Log.w(TAG, "Failed to prune app log", it)
        }
    }

    private fun appendAsync(level: String, event: String, details: String) {
        logScope.launch {
            appendBlocking(level, event, details)
        }
    }

    private fun appendBlocking(level: String, event: String, details: String) {
        runCatching {
            val line = formatLine(level, event, sanitize(details))
            val storageUri = settingsRepository.storageUri.value
            if (storageUri != null) {
                appendSaf(storageUri, line)
            } else {
                appendLocal(line)
            }
        }.onFailure {
            Log.w(TAG, "Failed to append app log event=$event", it)
        }
    }

    private fun appendLocal(line: String) {
        val file = localLogFile()
        file.parentFile?.mkdirs()
        val current = if (file.exists()) file.readText(Charsets.UTF_8) else ""
        file.writeText(trimForAppend(pruneByRetention(current), line), Charsets.UTF_8)
    }

    private fun appendSaf(storageUri: String, line: String) {
        val file = getSafLogFile(storageUri, createIfMissing = true) ?: return
        val current =
            context.contentResolver.openInputStream(file.uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
        context.contentResolver.openOutputStream(file.uri, "wt")
            ?.use { it.write(trimForAppend(pruneByRetention(current), line).toByteArray(Charsets.UTF_8)) }
    }

    private fun trimForAppend(current: String, line: String): String {
        val next = buildString {
            if (current.isNotBlank()) append(current.trimEnd()).append('\n')
            append(line)
        }
        if (next.length <= MAX_LOG_CHARS) return next
        return next.takeLast(MAX_LOG_CHARS).substringAfter('\n', next.takeLast(MAX_LOG_CHARS))
    }

    private fun formatLine(level: String, event: String, details: String): String =
        buildString {
            append(Instant.now()).append(" | ").append(level).append(" | ").append(event)
            append(" | app=").append(BuildConfigShim.versionName())
            append(" | android=").append(Build.VERSION.SDK_INT)
            if (details.isNotBlank()) append('\n').append(details)
            append('\n')
        }

    private fun pruneByRetention(raw: String): String {
        if (raw.isBlank()) return ""
        val cutoff = Instant.now().minus(settingsRepository.appLogRetentionDays.value.toLong(), ChronoUnit.DAYS)
        val events = splitEvents(raw)
        return events
            .filter { event ->
                val timestamp = event.substringBefore(" | ", missingDelimiterValue = "")
                val instant = runCatching { Instant.parse(timestamp) }.getOrNull()
                instant == null || !instant.isBefore(cutoff)
            }
            .joinToString("\n") { it.trimEnd() }
    }

    private fun splitEvents(raw: String): List<String> {
        val starts = EVENT_START_REGEX.findAll(raw).map { it.range.first }.toList()
        if (starts.isEmpty()) return listOf(raw.trim())
        return starts.indices.map { index ->
            val start = starts[index]
            val end = starts.getOrNull(index + 1) ?: raw.length
            raw.substring(start, end).trim()
        }
    }

    private fun sanitize(raw: String): String =
        raw.lineSequence()
            .map { it.take(MAX_LINE_CHARS) }
            .take(MAX_DETAIL_LINES)
            .joinToString("\n")

    private fun stackTraceText(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    private fun crashReportText(
        throwable: Throwable,
        details: String,
        threadName: String,
        crashCount: Int
    ): String =
        buildString {
            append("Crash report")
            append("\ncrashCount=").append(crashCount)
            if (details.isNotBlank()) append('\n').append(details.trim())
            if (threadName.isNotBlank()) append("\nthread=").append(threadName)
            append("\nexception=").append(throwable::class.java.name)
            throwable.message?.takeIf { it.isNotBlank() }?.let { message ->
                append("\nmessage=").append(message.take(MAX_LINE_CHARS))
            }
            append("\nappVersion=").append(BuildConfigShim.versionName())
            append(" (").append(BuildConfigShim.versionCode()).append(')')
            append("\nandroidSdk=").append(Build.VERSION.SDK_INT)
            append("\ndevice=")
                .append(Build.MANUFACTURER)
                .append(' ')
                .append(Build.MODEL)
                .append(" / ")
                .append(Build.DEVICE)
            append("\ncauses=").append(causeSummary(throwable))
            append("\nstackTrace:\n").append(stackTraceText(throwable))
        }

    private fun incrementCrashCountBlocking(): Int {
        val prefs = context.getSharedPreferences(CRASH_PREFS_NAME, Context.MODE_PRIVATE)
        val nextCount = prefs.getInt(KEY_CRASH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_CRASH_COUNT, nextCount).commit()
        return nextCount
    }

    private fun causeSummary(throwable: Throwable): String {
        val causes = generateSequence(throwable) { it.cause }
            .take(6)
            .map { cause ->
                buildString {
                    append(cause::class.java.simpleName)
                    cause.message?.takeIf { it.isNotBlank() }?.let { message ->
                        append(": ").append(message.take(180))
                    }
                }
            }
            .toList()
        return causes.joinToString(" -> ")
    }

    private fun localLogFile(): File = File(context.filesDir, LOG_FILE_NAME)

    private fun getSafLogFile(storageUri: String, createIfMissing: Boolean): DocumentFile? {
        val root = DocumentFile.fromTreeUri(context, Uri.parse(storageUri)) ?: return null
        val existing = root.findFile(LOG_FILE_NAME)
        if (existing != null) return existing
        if (!createIfMissing) return null
        return root.createFile("text/plain", LOG_FILE_NAME)
    }

    private object BuildConfigShim {
        fun versionName(): String = runCatching { com.mj.yaja.BuildConfig.VERSION_NAME }.getOrDefault("unknown")
        fun versionCode(): Int = runCatching { com.mj.yaja.BuildConfig.VERSION_CODE }.getOrDefault(0)
    }

    companion object {
        private const val TAG = "AppLogRepository"
        private const val LOG_FILE_NAME = "Yaja App Log.txt"
        private const val CRASH_PREFS_NAME = "yaja_crash_log"
        private const val KEY_CRASH_COUNT = "crash_count"
        private const val MAX_LOG_CHARS = 180_000
        private const val MAX_LINE_CHARS = 600
        private const val MAX_DETAIL_LINES = 220
        private val EVENT_START_REGEX = Regex("""(?m)^\d{4}-\d{2}-\d{2}T""")

        @Volatile private var instance: AppLogRepository? = null

        fun getInstance(context: Context, settingsRepository: SettingsRepository): AppLogRepository =
            instance ?: synchronized(this) {
                instance ?: AppLogRepository(context.applicationContext, settingsRepository).also {
                    instance = it
                }
            }
    }
}
