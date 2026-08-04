package com.mj.yaja.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.MarkdownFileManager
import com.mj.yaja.data.RecurringTaskEndMode
import com.mj.yaja.data.RecurringTaskFrequency
import com.mj.yaja.data.RecurringTaskItem
import com.mj.yaja.data.RecurringTaskItemType
import com.mj.yaja.data.RecurringTaskScheduleMode
import com.mj.yaja.data.SettingsRepository
import com.mj.yaja.data.keywords.KeywordCsvCodec
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

class BackupService(
    private val context: Context,
    private val logTag: String
) {
    data class BackupJournalDay(
        val content: String,
        val entries: List<String>,
        val isStarred: Boolean,
        val label: String,
        val revisitOn: LocalDate? = null,
        val revisitNote: String = ""
    )

    data class BackupBundle(
        val journalDays: Map<LocalDate, BackupJournalDay>,
        val shortcodes: Map<String, String>,
        val dateKeywords: List<DateKeywordEntry>,
        val keywords: List<KeywordDefinition>,
        val recurringTasks: List<RecurringTaskItem> = emptyList()
    )

    data class BackupZipResult(
        val uri: Uri?,
        val sizeBytes: Long
    )

    fun createBackupZipFromJournalFiles(
        shortcodes: Map<String, String>,
        dateKeywords: List<DateKeywordEntry>,
        keywords: List<KeywordDefinition>,
        recurringTasks: List<RecurringTaskItem> = emptyList(),
        writeJournalFiles: (ZipOutputStream) -> Int
    ): BackupZipResult? {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val dateStr = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val zipFile = File(backupDir, "journal_backup_$dateStr.zip")

        var journalDayCount = 0
        val zipCreated = try {
            Log.d(logTag, "Backup ZIP direct create started: shortcodes=${shortcodes.size} dateKeywords=${dateKeywords.size} keywords=${keywords.size} recurringTasks=${recurringTasks.size}")
            ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                if (shortcodes.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("settings/shortcodes.yaja"))
                    zos.write(SettingsRepository.serializeShortcodes(shortcodes).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                if (dateKeywords.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("settings/date_keywords.json"))
                    zos.write(SettingsRepository.serializeDateKeywords(dateKeywords).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                if (keywords.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("settings/keywords.csv"))
                    zos.write(KeywordCsvCodec.encode(keywords).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                if (recurringTasks.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("settings/recurring_tasks.json"))
                    zos.write(serializeRecurringTasks(recurringTasks).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                journalDayCount = writeJournalFiles(zos)

                val manifest = JSONObject().apply {
                    put("format", "yaja-backup")
                    put("version", 3)
                    put("createdAt", System.currentTimeMillis())
                    put("journalDays", journalDayCount)
                    put("shortcodes", shortcodes.size)
                    put("dateKeywords", dateKeywords.size)
                    put("keywords", keywords.size)
                    put("recurringTasks", recurringTasks.size)
                }
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            Log.d(logTag, "Backup ZIP direct written: path=${zipFile.absolutePath} size=${zipFile.length()} journalDays=$journalDayCount")
            true
        } catch (e: Exception) {
            Log.e(logTag, "Failed to create direct backup zip", e)
            zipFile.delete()
            false
        }
        if (!zipCreated) return null

        return backupZipResultForFile(zipFile)
    }

    fun createBackupZip(
        journalDays: Map<LocalDate, BackupJournalDay>,
        shortcodes: Map<String, String>,
        dateKeywords: List<DateKeywordEntry>,
        keywords: List<KeywordDefinition>,
        recurringTasks: List<RecurringTaskItem> = emptyList()
    ): BackupZipResult? {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val dateStr = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
        val zipFile = File(backupDir, "journal_backup_$dateStr.zip")

        val zipCreated = try {
            Log.d(logTag, "Backup ZIP create started: journalDays=${journalDays.size} shortcodes=${shortcodes.size} dateKeywords=${dateKeywords.size} keywords=${keywords.size}")
            ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                val manifest = JSONObject().apply {
                    put("format", "yaja-backup")
                    put("version", 3)
                    put("createdAt", System.currentTimeMillis())
                    put("journalDays", journalDays.size)
                    put("shortcodes", shortcodes.size)
                    put("dateKeywords", dateKeywords.size)
                    put("keywords", keywords.size)
                    put("recurringTasks", recurringTasks.size)
                }
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                if (shortcodes.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("settings/shortcodes.yaja"))
                    zos.write(SettingsRepository.serializeShortcodes(shortcodes).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                if (dateKeywords.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("settings/date_keywords.json"))
                    zos.write(SettingsRepository.serializeDateKeywords(dateKeywords).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                if (keywords.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("settings/keywords.csv"))
                    zos.write(KeywordCsvCodec.encode(keywords).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                if (recurringTasks.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("settings/recurring_tasks.json"))
                    zos.write(serializeRecurringTasks(recurringTasks).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }

                journalDays.keys.sorted().forEach { date ->
                    val day = journalDays[date] ?: return@forEach
                    if (day.content.isNotBlank()) {
                        val entryName = "journal/${date.year}/${date.format(DateTimeFormatter.ofPattern("MM"))}/$date.md"
                        zos.putNextEntry(ZipEntry(entryName))
                        zos.write(day.content.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
            }
            Log.d(logTag, "Backup ZIP written: path=${zipFile.absolutePath} size=${zipFile.length()}")
            true
        } catch (e: Exception) {
            Log.e(logTag, "Failed to create backup zip", e)
            zipFile.delete()
            false
        }
        if (!zipCreated) return null

        return backupZipResultForFile(zipFile)
    }

    private fun backupZipResultForFile(zipFile: File): BackupZipResult {
        return try {
            BackupZipResult(
                uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile),
                sizeBytes = zipFile.length()
            )
        } catch (e: Exception) {
            Log.e(logTag, "Backup ZIP was created but could not be shared", e)
            BackupZipResult(uri = null, sizeBytes = zipFile.length())
        }
    }

    fun readBackupZip(uri: Uri): BackupBundle? {
        return try {
            val journalDays = linkedMapOf<LocalDate, BackupJournalDay>()
            var shortcodes = emptyMap<String, String>()
            var dateKeywords = emptyList<DateKeywordEntry>()
            var keywords = emptyList<KeywordDefinition>()
            var recurringTasks = emptyList<RecurringTaskItem>()
            var sawManifest = false
            var manifestValid = false

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Couldn't open the selected backup ZIP")
            inputStream.use {
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val raw = readCurrentZipEntry(zip)
                            when {
                                entry.name.equals("manifest.json", ignoreCase = true) -> {
                                    sawManifest = true
                                    manifestValid = isValidBackupManifest(raw)
                                }
                                entry.name.equals("settings/shortcodes.yaja", ignoreCase = true) -> {
                                    shortcodes = SettingsRepository.deserializeShortcodes(raw)
                                }
                                entry.name.equals("shortcodes.csv", ignoreCase = true) -> {
                                    shortcodes = parseLegacyShortcodesCsv(raw)
                                }
                                entry.name.equals("settings/date_keywords.json", ignoreCase = true) -> {
                                    dateKeywords = SettingsRepository.deserializeDateKeywords(raw)
                                }
                                entry.name.equals("settings/keywords.csv", ignoreCase = true) ||
                                    entry.name.equals("keywords.csv", ignoreCase = true) -> {
                                    val parsed = mutableListOf<KeywordDefinition>()
                                    raw.lineSequence()
                                        .dropWhile { it.isBlank() }
                                        .forEachIndexed { index, line ->
                                            if (index == 0 && KeywordCsvCodec.isHeader(line)) return@forEachIndexed
                                            KeywordCsvCodec.parseLine(line)?.let(parsed::add)
                                        }
                                    keywords = parsed
                                }
                                entry.name.equals("settings/recurring_tasks.json", ignoreCase = true) -> {
                                    recurringTasks = deserializeRecurringTasks(raw)
                                }
                                entry.name.endsWith(".md", ignoreCase = true) -> {
                                    val date = extractDateFromPath(entry.name) ?: run {
                                        zip.closeEntry()
                                        entry = zip.nextEntry
                                        continue
                                    }
                                    val lines = raw.lines()
                                    val entries = MarkdownFileManager.parseEntries(lines)
                                    val frontmatter = MarkdownFileManager.parseFrontmatter(lines)
                                    journalDays[date] = BackupJournalDay(
                                        content = raw,
                                        entries = entries,
                                        isStarred = frontmatter.isStarred,
                                        label = frontmatter.label,
                                        revisitOn = frontmatter.revisitOn,
                                        revisitNote = frontmatter.revisitNote
                                    )
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            if (sawManifest && !manifestValid) {
                throw IllegalStateException("This ZIP doesn't look like a valid Yaja backup.")
            }

            if (!sawManifest) {
                val hasLegacyShape = journalDays.isNotEmpty() || shortcodes.isNotEmpty() || keywords.isNotEmpty()
                if (!hasLegacyShape) {
                    throw IllegalStateException("This ZIP doesn't contain a Yaja backup.")
                }
            }

            BackupBundle(
                journalDays = journalDays,
                shortcodes = shortcodes,
                dateKeywords = dateKeywords,
                keywords = keywords,
                recurringTasks = recurringTasks
            )
        } catch (e: Exception) {
            Log.e(logTag, "Failed to read backup zip", e)
            null
        }
    }

    private fun extractDateFromPath(path: String): LocalDate? {
        val fileName = path.substringAfterLast('/').removeSuffix(".md")
        return runCatching { LocalDate.parse(fileName) }.getOrNull()
    }

    private fun isValidBackupManifest(raw: String): Boolean {
        return runCatching {
            val manifest = JSONObject(raw)
            manifest.optString("format") == "yaja-backup" &&
                manifest.optInt("version", -1) >= 2
        }.getOrDefault(false)
    }

    private fun readCurrentZipEntry(zip: ZipInputStream): String {
        val buffer = ByteArray(8 * 1024)
        val output = ByteArrayOutputStream()
        while (true) {
            val read = zip.read(buffer)
            if (read <= 0) break
            output.write(buffer, 0, read)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private fun parseLegacyShortcodesCsv(raw: String): Map<String, String> {
        val rows = parseCsvRows(raw)
        return rows.mapNotNull { row ->
            if (row.isEmpty()) return@mapNotNull null
            val code = row.firstOrNull()?.trim().orEmpty()
            if (code.isBlank()) return@mapNotNull null
            val value = if (row.size >= 2) row.drop(1).joinToString(",") else ""
            code to value
        }.toMap()
    }

    private fun parseCsvRows(raw: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < raw.length && raw[i + 1] == '"') {
                        currentField.append('"')
                        i += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    currentRow += currentField.toString()
                    currentField.clear()
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < raw.length && raw[i + 1] == '\n') {
                        i += 1
                    }
                    currentRow += currentField.toString()
                    currentField.clear()
                    if (currentRow.any { it.isNotEmpty() }) {
                        rows += currentRow.toList()
                    }
                    currentRow.clear()
                }
                else -> currentField.append(ch)
            }
            i += 1
        }
        currentRow += currentField.toString()
        if (currentRow.any { it.isNotEmpty() }) {
            rows += currentRow.toList()
        }
        return rows
    }

    companion object {
        fun serializeRecurringTasks(tasks: List<RecurringTaskItem>): String {
            val array = JSONArray()
            tasks.forEach { task ->
                array.put(
                    JSONObject().apply {
                        put("id", task.id)
                        put("title", task.title)
                        put("description", task.description)
                        put("isActive", task.isActive)
                        put("itemType", task.itemType.name)
                        put("scheduleMode", task.scheduleMode.name)
                        put("frequency", task.frequency.name)
                        put("dueDayOfMonth", task.dueDayOfMonth)
                        put("dueDayOfWeek", task.dueDayOfWeek)
                        put("leadDays", task.leadDays)
                        put("anchorDate", task.anchorDate)
                        put("startMonth", task.startMonth)
                        put("startTime", task.startTime)
                        put("endMode", task.endMode.name)
                        put("endDate", task.endDate)
                        put("endCount", task.endCount)
                        put("retiredOn", task.retiredOn)
                        put("createdAt", task.createdAt)
                    }
                )
            }
            return array.toString()
        }

        fun deserializeRecurringTasks(raw: String): List<RecurringTaskItem> {
            if (raw.isBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        val obj = array.optJSONObject(index) ?: continue
                        val id = obj.optString("id").trim()
                        val title = obj.optString("title").trim()
                        if (id.isBlank() || title.isBlank()) continue
                        val scheduleMode = enumValueOrNull<RecurringTaskScheduleMode>(
                            obj.optString("scheduleMode")
                        ) ?: continue
                        val frequency = enumValueOrNull<RecurringTaskFrequency>(
                            obj.optString("frequency")
                        ) ?: continue
                        add(
                            RecurringTaskItem(
                                id = id,
                                title = title,
                                description = obj.optString("description", ""),
                                isActive = obj.optBoolean("isActive", true),
                                itemType = enumValueOrNull<RecurringTaskItemType>(
                                    obj.optString("itemType")
                                ) ?: RecurringTaskItemType.TASK,
                                scheduleMode = scheduleMode,
                                frequency = frequency,
                                dueDayOfMonth = obj.optionalInt("dueDayOfMonth"),
                                dueDayOfWeek = obj.optionalInt("dueDayOfWeek"),
                                leadDays = obj.optInt("leadDays", 0).coerceIn(0, 30),
                                anchorDate = obj.optString("anchorDate"),
                                startMonth = obj.optString("startMonth"),
                                startTime = obj.optionalString("startTime"),
                                endMode = enumValueOrNull<RecurringTaskEndMode>(
                                    obj.optString("endMode")
                                ) ?: RecurringTaskEndMode.NEVER,
                                endDate = obj.optionalString("endDate"),
                                endCount = obj.optionalInt("endCount"),
                                retiredOn = obj.optionalString("retiredOn"),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
            runCatching { enumValueOf<T>(value) }.getOrNull()

        private fun JSONObject.optionalString(name: String): String? =
            if (isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

        private fun JSONObject.optionalInt(name: String): Int? =
            if (isNull(name) || !has(name)) null else optInt(name)
    }
}
