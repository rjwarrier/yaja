package com.mj.yaja.data

import android.content.Context
import android.util.Log
import com.mj.yaja.data.database.JournalDatabase
import com.mj.yaja.data.database.ComplianceMasterEntity
import com.mj.yaja.data.database.ComplianceGenerationEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ComplianceScheduleMode {
    DAY_OF_MONTH,
    DAY_OF_WEEK,
    FIRST_DAY_OF_MONTH,
    LAST_DAY_OF_MONTH
}

enum class ComplianceFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY,
    QUARTERLY,
    HALF_YEARLY,
    ANNUAL
}

enum class ComplianceEndMode {
    NEVER,
    ON_DATE,
    AFTER_OCCURRENCES
}

enum class ComplianceItemType {
    TASK,
    EVENT
}

@androidx.compose.runtime.Immutable
data class CardSchedule(
    val nextDate: LocalDate?,
    val endDate: LocalDate?,
    val remaining: Int?
)

@androidx.compose.runtime.Immutable
data class ComplianceMasterItem(
    val id: String,
    val title: String,
    val description: String = "",
    val isActive: Boolean = true,
    val itemType: ComplianceItemType = ComplianceItemType.TASK,
    val scheduleMode: ComplianceScheduleMode,
    val frequency: ComplianceFrequency,
    val dueDayOfMonth: Int?,
    val dueDayOfWeek: Int?,
    val leadDays: Int,
    val anchorDate: String,
    val startMonth: String,
    val startTime: String? = null,
    val endMode: ComplianceEndMode = ComplianceEndMode.NEVER,
    val endDate: String? = null,
    val endCount: Int? = null,
    val retiredOn: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

class ComplianceMasterRepository private constructor(context: Context) {
    private val database = JournalDatabase.getDatabase(context)
    private val dao = database.complianceDao()
    private val journalId = "default"

    val items: StateFlow<List<ComplianceMasterItem>> = dao.observeActiveMasters(journalId)
        .map { entities -> entities.map { it.toItem() } }
        .stateIn(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    companion object {
        @Volatile private var instance: ComplianceMasterRepository? = null

        fun getInstance(context: Context): ComplianceMasterRepository =
            instance ?: synchronized(this) {
                instance ?: ComplianceMasterRepository(context.applicationContext).also { instance = it }
            }
    }

    suspend fun upsert(
        id: String?,
        title: String,
        description: String = "",
        isActive: Boolean = true,
        itemType: ComplianceItemType = ComplianceItemType.TASK,
        scheduleMode: ComplianceScheduleMode,
        frequency: ComplianceFrequency,
        dueDayOfMonth: Int?,
        dueDayOfWeek: Int?,
        leadDays: Int,
        endMode: ComplianceEndMode = ComplianceEndMode.NEVER,
        endDate: LocalDate? = null,
        endCount: Int? = null,
        anchorDate: LocalDate = LocalDate.now(),
        startMonth: YearMonth = YearMonth.now(),
        startTime: String? = null,
        fileManager: MarkdownFileManager
    ): Boolean {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isBlank()) return false
        val safeDueDay = dueDayOfMonth?.coerceIn(1, 31)
        val safeDueWeekday = dueDayOfWeek?.coerceIn(1, 7)
        val safeLeadDays = leadDays.coerceIn(0, 30)
        // End condition is editable: only persist the payload relevant to the chosen mode.
        val safeEndDate = endDate?.toString().takeIf { endMode == ComplianceEndMode.ON_DATE }
        val safeEndCount = endCount?.coerceIn(1, 999).takeIf { endMode == ComplianceEndMode.AFTER_OCCURRENCES }

        val existing = if (id != null) dao.getAllMastersSync(journalId).firstOrNull { it.id == id } else null
        val futureGeneratedDates =
            existing
                ?.let { dao.getFutureGenerationDates(it.id, LocalDate.now().toString()) }
                .orEmpty()

        val entity = ComplianceMasterEntity(
            id = id ?: UUID.randomUUID().toString(),
            journalId = journalId,
            title = normalizedTitle,
            description = description.trim(),
            isActive = isActive,
            itemType = itemType.name,
            scheduleMode = scheduleMode.name,
            frequency = frequency.name,
            dueDayOfMonth = safeDueDay,
            dueDayOfWeek = safeDueWeekday,
            leadDays = safeLeadDays,
            anchorDate = anchorDate.toString(),
            startMonth = startMonth.toString(),
            startTime = startTime,
            endMode = endMode.name,
            endDate = safeEndDate,
            endCount = safeEndCount,
            retiredOn = existing?.retiredOn,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        dao.insertOrUpdateMaster(entity)
        if (existing != null) {
            dao.clearFutureGenerations(entity.id, LocalDate.now().toString())
            removeObsoleteFutureEntries(
                fileManager = fileManager,
                itemId = entity.id,
                previousGeneratedDates = futureGeneratedDates,
                retainedDates = getDueDates(entity.toItem(), LocalDate.now()).toSet()
            )
        }
        return true
    }

    suspend fun delete(
        itemId: String,
        fileManager: MarkdownFileManager,
        retiredOn: LocalDate = LocalDate.now()
    ) {
        removeFutureGeneratedEntries(fileManager, itemId, retiredOn)
        dao.clearFutureGenerations(itemId, LocalDate.now().toString())
        dao.retireMaster(itemId, retiredOn.toString())
    }

    suspend fun setActive(
        itemId: String,
        isActive: Boolean,
        fileManager: MarkdownFileManager,
        today: LocalDate = LocalDate.now()
    ) {
        val existing = dao.getAllMastersSync(journalId).firstOrNull { it.id == itemId } ?: return
        val entity = existing.copy(isActive = isActive)
        dao.insertOrUpdateMaster(entity)
        if (!isActive) {
            removeFutureGeneratedEntries(fileManager, itemId, today)
            dao.clearFutureGenerations(itemId, today.toString())
        }
    }

    suspend fun generateTodos(fileManager: MarkdownFileManager, today: LocalDate = LocalDate.now()) {
        val masters = dao.getAllMastersSync(journalId)
        
        masters.forEach { entity ->
            if (!entity.isActive || entity.retiredOn != null) return@forEach
            val item = entity.toItem()
            val dueDates = getDueDates(item, today)
            
            dueDates.forEach { dueDate ->
                val hasGen = dao.hasGeneration(item.id, dueDate.toString())
                if (!hasGen) {
                    val entryText = generatedEntryText(item)
                    val existingEntryIndex = fileManager.getEntriesForDate(dueDate)
                        .indexOfFirst { it.contains(complianceMarker(item.id)) }
                    if (existingEntryIndex >= 0) {
                        // Schedule edits update future generated entries in place. Today's entry
                        // remains untouched so a user can complete or annotate it safely.
                        val updated =
                            if (dueDate.isAfter(today)) {
                                fileManager.tryUpdateEntryForDate(dueDate, existingEntryIndex, entryText).success
                            } else {
                                true
                            }
                        if (!updated) {
                            Log.e("ComplianceGen", "Failed to update recurring entry on $dueDate.md")
                            return@forEach
                        }
                        dao.insertGeneration(
                            ComplianceGenerationEntity(
                                itemId = item.id,
                                targetDate = dueDate.toString()
                            )
                        )
                        return@forEach
                    }
                    val result = fileManager.tryAddEntryForDate(dueDate, entryText)
                    if (result.success) {
                        dao.insertGeneration(
                            ComplianceGenerationEntity(
                                itemId = item.id,
                                targetDate = dueDate.toString()
                            )
                        )
                        Log.d("ComplianceGen", "Generated recurring todo: '${item.title}' written to $dueDate.md")
                    } else {
                        Log.e("ComplianceGen", "Failed to write recurring todo to $dueDate.md")
                    }
                }
            }

            // Auto-retire once all scheduled occurrences have passed.
            if (item.endMode == ComplianceEndMode.AFTER_OCCURRENCES) {
                val rem = remainingOccurrences(item, today) ?: 0
                if (rem == 0) {
                    dao.retireMaster(item.id, today.toString())
                    Log.d("ComplianceGen", "Auto-retired '${item.title}': all ${item.endCount} occurrences exhausted")
                }
            }
        }
    }

    private fun getDueDates(item: ComplianceMasterItem, today: LocalDate): List<LocalDate> {
        val startMonth = runCatching { YearMonth.parse(item.startMonth) }.getOrDefault(YearMonth.from(today))
        val anchorDate = runCatching { LocalDate.parse(item.anchorDate) }.getOrDefault(today)
        return when (item.scheduleMode) {
            ComplianceScheduleMode.DAY_OF_MONTH,
            ComplianceScheduleMode.FIRST_DAY_OF_MONTH,
            ComplianceScheduleMode.LAST_DAY_OF_MONTH -> {
                generateMonthBasedDueDates(item, today, startMonth)
            }
            ComplianceScheduleMode.DAY_OF_WEEK -> {
                generateWeekdayBasedDueDates(item, today, anchorDate)
            }
        }
    }

    private fun generateMonthBasedDueDates(
        item: ComplianceMasterItem,
        today: LocalDate,
        startMonth: YearMonth
    ): List<LocalDate> {
        val monthsStep = when (item.frequency) {
            ComplianceFrequency.MONTHLY -> 1
            ComplianceFrequency.QUARTERLY -> 3
            ComplianceFrequency.HALF_YEARLY -> 6
            ComplianceFrequency.ANNUAL -> 12
            else -> 1
        }
        val evaluationLimitMonth = YearMonth.from(today.plusDays(item.leadDays.toLong()))
        val createdOn = creationDate(item)
        val retiredOn = retirementDate(item)
        val endDate = effectiveEndDate(item)

        return generateSequence(startMonth) { month ->
            month.takeIf { it.isBefore(evaluationLimitMonth) }?.plusMonths(monthsStep.toLong())
        }
            .takeWhile { !it.isAfter(evaluationLimitMonth) }
            .mapNotNull { month ->
                if (!matchesAnnualAnchor(item, month, startMonth)) return@mapNotNull null
                val dueDate = dueDateForMonth(item, month)
                val generationDate = dueDate.minusDays(item.leadDays.toLong())

                if (today.isBefore(generationDate)) {
                    null
                } else {
                    dueDate.takeIf {
                        !it.isBefore(today) &&
                            withinEnd(it, endDate) &&
                            isWithinLifecycle(
                                dueDate = it,
                                generatedOn = maxOf(generationDate, createdOn),
                                createdOn = createdOn,
                                retiredOn = retiredOn
                            )
                    }
                }
            }
            .toList()
    }

    private fun generateWeekdayBasedDueDates(
        item: ComplianceMasterItem,
        today: LocalDate,
        anchorDate: LocalDate
    ): List<LocalDate> {
        val dueWeekday = item.dueDayOfWeek ?: anchorDate.dayOfWeek.value
        val createdOn = creationDate(item)
        val retiredOn = retirementDate(item)
        val endDate = effectiveEndDate(item)
        val evaluationLimitDate = today.plusDays(item.leadDays.toLong())

        return when (item.frequency) {
            ComplianceFrequency.WEEKLY,
            ComplianceFrequency.BIWEEKLY -> {
                val stepDays = if (item.frequency == ComplianceFrequency.BIWEEKLY) 14L else 7L
                val firstDueDate = firstWeekdayAfter(createdOn, dueWeekday)
                val lookbackDate = today.minusDays(item.leadDays.toLong())
                val daysBetween = lookbackDate.toEpochDay() - firstDueDate.toEpochDay()
                val stepsBack = maxOf(0L, daysBetween / stepDays)
                val safeStart = firstDueDate.plusDays(stepsBack * stepDays)
                generateSequence(safeStart) { current ->
                    current.plusDays(stepDays)
                }
                    .takeWhile { !it.isAfter(evaluationLimitDate) }
                    .filter { dueDate ->
                        val generationDate = dueDate.minusDays(item.leadDays.toLong())
                        !today.isBefore(generationDate) &&
                            !dueDate.isBefore(today) &&
                            withinEnd(dueDate, endDate) &&
                            isWithinLifecycle(
                                dueDate = dueDate,
                                generatedOn = maxOf(generationDate, createdOn),
                                createdOn = createdOn,
                                retiredOn = retiredOn
                            )
                    }
                    .toList()
            }
            ComplianceFrequency.MONTHLY -> {
                val evaluationLimitMonth = YearMonth.from(evaluationLimitDate)
                val anchorOrdinal = ((anchorDate.dayOfMonth - 1) / 7) + 1
                val startMonth = YearMonth.from(anchorDate)
                generateSequence(startMonth) { month ->
                    month.takeIf { it.isBefore(evaluationLimitMonth) }?.plusMonths(1)
                }
                    .takeWhile { !it.isAfter(evaluationLimitMonth) }
                    .mapNotNull { month ->
                        val dueDate = nthWeekdayOfMonth(month, dueWeekday, anchorOrdinal)
                        if (dueDate != null) {
                            val generationDate = dueDate.minusDays(item.leadDays.toLong())
                            if (!today.isBefore(generationDate) &&
                                !dueDate.isBefore(today) &&
                                withinEnd(dueDate, endDate) &&
                                isWithinLifecycle(
                                    dueDate = dueDate,
                                    generatedOn = maxOf(generationDate, createdOn),
                                    createdOn = createdOn,
                                    retiredOn = retiredOn
                                )
                            ) {
                                dueDate
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    }
                    .toList()
            }
            else -> emptyList()
        }
    }

    private fun dueDateForMonth(item: ComplianceMasterItem, month: YearMonth): LocalDate =
        when (item.scheduleMode) {
            ComplianceScheduleMode.FIRST_DAY_OF_MONTH -> month.atDay(1)
            ComplianceScheduleMode.LAST_DAY_OF_MONTH -> month.atEndOfMonth()
            ComplianceScheduleMode.DAY_OF_MONTH -> month.atDay((item.dueDayOfMonth ?: 1).coerceAtMost(month.lengthOfMonth()))
            ComplianceScheduleMode.DAY_OF_WEEK -> month.atDay(1)
        }

    private fun matchesAnnualAnchor(
        item: ComplianceMasterItem,
        month: YearMonth,
        startMonth: YearMonth
    ): Boolean =
        when (item.frequency) {
            ComplianceFrequency.ANNUAL -> month.month == startMonth.month
            else -> true
        }

    private fun nthWeekdayOfMonth(
        month: YearMonth,
        weekdayValue: Int,
        ordinal: Int
    ): LocalDate? {
        var found = 0
        var day = month.atDay(1)
        while (day.month == month.month) {
            if (day.dayOfWeek.value == weekdayValue) {
                found += 1
                if (found == ordinal) return day
            }
            day = day.plusDays(1)
        }
        return null
    }

    fun previewUpcomingDates(item: ComplianceMasterItem, count: Int = 5): List<LocalDate> {
        val today = LocalDate.now()
        val horizon = today.plusYears(3)
        val endDate = effectiveEndDate(item)
        return occurrenceSequence(item)
            .dropWhile { it.isBefore(today) }
            .takeWhile { !it.isAfter(horizon) }
            .filter { withinEnd(it, endDate) }
            .take(count)
            .toList()
    }

    /** Public read for UI: resolves the schedule's effective end date (null = recurs forever). */
    fun resolveEndDate(item: ComplianceMasterItem): LocalDate? = effectiveEndDate(item)

    /**
     * For [ComplianceEndMode.AFTER_OCCURRENCES]: how many of the N scheduled occurrences are still
     * to come (today counts as remaining). Decreases as due dates pass, giving the card a live
     * countdown. Returns null for any other end mode.
     */
    fun remainingOccurrences(item: ComplianceMasterItem, today: LocalDate = LocalDate.now()): Int? {
        if (item.endMode != ComplianceEndMode.AFTER_OCCURRENCES) return null
        val n = item.endCount?.takeIf { it > 0 } ?: return null
        return occurrenceSequence(item).take(n).count { !it.isBefore(today) }
    }

    /**
     * Computes all card display fields in one pass — avoids walking the occurrence sequence
     * three separate times when the card renders.
     */
    fun cardSchedule(item: ComplianceMasterItem, today: LocalDate = LocalDate.now()): CardSchedule {
        val endDate: LocalDate?
        val remaining: Int?
        when (item.endMode) {
            ComplianceEndMode.NEVER -> {
                endDate = null
                remaining = null
            }
            ComplianceEndMode.ON_DATE -> {
                endDate = item.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                remaining = null
            }
            ComplianceEndMode.AFTER_OCCURRENCES -> {
                val n = item.endCount?.takeIf { it > 0 }
                if (n != null) {
                    // Walk occurrence sequence once to derive both the cut-off and the live count.
                    val occurrences = occurrenceSequence(item).take(n).toList()
                    endDate = occurrences.lastOrNull()
                    remaining = occurrences.count { !it.isBefore(today) }
                } else {
                    endDate = null
                    remaining = null
                }
            }
        }
        val nextDate = previewUpcomingDates(item, 1).firstOrNull()
        return CardSchedule(nextDate = nextDate, endDate = endDate, remaining = remaining)
    }

    private fun firstWeekdayAfter(date: LocalDate, weekdayValue: Int): LocalDate {
        var candidate = date.plusDays(1)
        while (candidate.dayOfWeek.value != weekdayValue) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun creationDate(item: ComplianceMasterItem): LocalDate =
        runCatching { LocalDate.parse(item.anchorDate) }.getOrDefault(LocalDate.now())

    private fun retirementDate(item: ComplianceMasterItem): LocalDate? =
        item.retiredOn?.let { value ->
            runCatching { LocalDate.parse(value) }.getOrNull()
        }

    private fun isWithinLifecycle(
        dueDate: LocalDate,
        generatedOn: LocalDate,
        createdOn: LocalDate,
        retiredOn: LocalDate?
    ): Boolean =
        !dueDate.isBefore(createdOn) &&
            (retiredOn == null || !generatedOn.isAfter(retiredOn))

    /**
     * Lazy, unbounded sequence of every due date this schedule would ever produce, counted from the
     * schedule's own origin (anchor / start month) — independent of [leadDays] and "today". Used to
     * resolve the absolute cut-off for [ComplianceEndMode.AFTER_OCCURRENCES]. Enumeration order and
     * dates match the generators exactly, so the Nth element here is the Nth todo that gets written.
     */
    private fun occurrenceSequence(item: ComplianceMasterItem): Sequence<LocalDate> {
        val anchor = creationDate(item)
        val startMonth = runCatching { YearMonth.parse(item.startMonth) }
            .getOrDefault(YearMonth.from(anchor))
        return when (item.scheduleMode) {
            ComplianceScheduleMode.DAY_OF_MONTH,
            ComplianceScheduleMode.FIRST_DAY_OF_MONTH,
            ComplianceScheduleMode.LAST_DAY_OF_MONTH -> {
                val monthsStep = when (item.frequency) {
                    ComplianceFrequency.MONTHLY -> 1L
                    ComplianceFrequency.QUARTERLY -> 3L
                    ComplianceFrequency.HALF_YEARLY -> 6L
                    ComplianceFrequency.ANNUAL -> 12L
                    else -> 1L
                }
                generateSequence(startMonth) { it.plusMonths(monthsStep) }
                    .filter { matchesAnnualAnchor(item, it, startMonth) }
                    .map { dueDateForMonth(item, it) }
                    .filter { !it.isBefore(anchor) }
            }
            ComplianceScheduleMode.DAY_OF_WEEK -> {
                val dueWeekday = item.dueDayOfWeek ?: anchor.dayOfWeek.value
                when (item.frequency) {
                    ComplianceFrequency.WEEKLY,
                    ComplianceFrequency.BIWEEKLY -> {
                        val stepDays = if (item.frequency == ComplianceFrequency.BIWEEKLY) 14L else 7L
                        val first = firstWeekdayAfter(anchor, dueWeekday)
                        generateSequence(first) { it.plusDays(stepDays) }
                    }
                    ComplianceFrequency.MONTHLY -> {
                        val anchorOrdinal = ((anchor.dayOfMonth - 1) / 7) + 1
                        generateSequence(YearMonth.from(anchor)) { it.plusMonths(1) }
                            .mapNotNull { nthWeekdayOfMonth(it, dueWeekday, anchorOrdinal) }
                            .filter { !it.isBefore(anchor) }
                    }
                    else -> emptySequence()
                }
            }
        }
    }

    /** Date of the Nth occurrence (1-based) from schedule origin, or null if [n] is invalid. */
    private fun nthOccurrenceDate(item: ComplianceMasterItem, n: Int): LocalDate? =
        if (n <= 0) null else occurrenceSequence(item).drop(n - 1).firstOrNull()

    /**
     * Absolute last allowed due date for this schedule, or null when it recurs indefinitely.
     * For [ComplianceEndMode.AFTER_OCCURRENCES] this resolves the count to the concrete date of the
     * final occurrence, so the generators only need a simple date comparison.
     */
    private fun effectiveEndDate(item: ComplianceMasterItem): LocalDate? =
        when (item.endMode) {
            ComplianceEndMode.NEVER -> null
            ComplianceEndMode.ON_DATE ->
                item.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ComplianceEndMode.AFTER_OCCURRENCES ->
                item.endCount?.takeIf { it > 0 }?.let { nthOccurrenceDate(item, it) }
        }

    private fun withinEnd(dueDate: LocalDate, endDate: LocalDate?): Boolean =
        endDate == null || !dueDate.isAfter(endDate)

    private fun complianceMarker(itemId: String): String = "<!--compliance:$itemId-->"

    private fun generatedEntryText(item: ComplianceMasterItem): String = buildString {
        val timeHeader = item.startTime?.let { "<!--time:$it-->\n" } ?: ""
        when (item.itemType) {
            ComplianceItemType.EVENT -> {
                append(timeHeader)
                append("<!--type:event-->\n")
                append("${item.title} ${complianceMarker(item.id)}")
            }
            ComplianceItemType.TASK -> {
                append(timeHeader)
                append("[ ] ${item.title} ${complianceMarker(item.id)}")
            }
        }
        if (item.description.isNotBlank()) {
            append("\n\n")
            append(item.description)
        }
    }

    private fun removeObsoleteFutureEntries(
        fileManager: MarkdownFileManager,
        itemId: String,
        previousGeneratedDates: List<String>,
        retainedDates: Set<LocalDate>
    ) {
        previousGeneratedDates
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .filter { it !in retainedDates }
            .forEach { removeGeneratedEntryForDate(fileManager, itemId, it) }
    }

    private fun removeFutureGeneratedEntries(
        fileManager: MarkdownFileManager,
        itemId: String,
        fromDate: LocalDate
    ) {
        dao.getFutureGenerationDates(itemId, fromDate.toString())
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .forEach { removeGeneratedEntryForDate(fileManager, itemId, it) }
    }

    private fun removeGeneratedEntryForDate(
        fileManager: MarkdownFileManager,
        itemId: String,
        date: LocalDate
    ) {
        val marker = complianceMarker(itemId)
        fileManager.getEntriesForDate(date)
            .mapIndexedNotNull { index, entry -> index.takeIf { entry.contains(marker) } }
            .asReversed()
            .forEach { index ->
                if (!fileManager.tryDeleteEntryForDate(date, index).success) {
                    Log.e("ComplianceGen", "Failed to remove obsolete recurring entry on $date.md")
                }
            }
    }

    private fun ComplianceMasterEntity.toItem() = ComplianceMasterItem(
        id = id,
        title = title,
        description = description,
        isActive = isActive,
        itemType = runCatching { ComplianceItemType.valueOf(itemType) }.getOrDefault(ComplianceItemType.TASK),
        scheduleMode = ComplianceScheduleMode.valueOf(scheduleMode),
        frequency = ComplianceFrequency.valueOf(frequency),
        dueDayOfMonth = dueDayOfMonth,
        dueDayOfWeek = dueDayOfWeek,
        leadDays = leadDays,
        anchorDate = anchorDate,
        startMonth = startMonth,
        startTime = startTime,
        endMode = runCatching { ComplianceEndMode.valueOf(endMode) }.getOrDefault(ComplianceEndMode.NEVER),
        endDate = endDate,
        endCount = endCount,
        retiredOn = retiredOn,
        createdAt = createdAt
    )
}
