package com.mj.yaja.data

import com.mj.yaja.data.backup.BackupService
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsBackupCodecsTest {

    @Test
    fun `shortcode codec round trips delimiters unicode and multiline values`() {
        val shortcodes =
            linkedMapOf(
                "!m" to "Good morning",
                "!complex=key&" to "Line 1\nLine 2 & more = yes",
                "!ml" to "മലയാളം"
            )

        val restored = SettingsRepository.deserializeShortcodes(
            SettingsRepository.serializeShortcodes(shortcodes)
        )

        assertEquals(shortcodes, restored)
    }

    @Test
    fun `shortcode codec returns empty map for empty payload`() {
        assertEquals(emptyMap<String, String>(), SettingsRepository.deserializeShortcodes("v3|"))
        assertEquals(emptyMap<String, String>(), SettingsRepository.deserializeShortcodes(""))
    }

    @Test
    fun `shortcode codec ignores malformed tokens and preserves valid tokens`() {
        val restored = SettingsRepository.deserializeShortcodes("v3|!ok=value&broken-token&!next=two")

        assertEquals(
            mapOf("!ok" to "value", "!next" to "two"),
            restored
        )
    }

    @Test
    fun `date keyword codec round trips ordered keyword meanings`() {
        val entries =
            listOf(
                DateKeywordEntry(keyword = "ഇന്നലെ", meaning = "yesterday"),
                DateKeywordEntry(keyword = "mañana", meaning = "tomorrow"),
                DateKeywordEntry(keyword = "next wk", meaning = "next week")
            )

        val restored = SettingsRepository.deserializeDateKeywords(
            SettingsRepository.serializeDateKeywords(entries)
        )

        assertEquals(entries, restored)
    }

    @Test
    fun `date keyword codec returns empty list for blank invalid or truncated json`() {
        assertEquals(emptyList<DateKeywordEntry>(), SettingsRepository.deserializeDateKeywords(""))
        assertEquals(emptyList<DateKeywordEntry>(), SettingsRepository.deserializeDateKeywords("not-json"))
        assertEquals(
            emptyList<DateKeywordEntry>(),
            SettingsRepository.deserializeDateKeywords("""[{"k":"soon","m":"tomorrow"}""")
        )
    }

    @Test
    fun `date keyword codec rejects malformed item shapes safely`() {
        assertEquals(
            emptyList<DateKeywordEntry>(),
            SettingsRepository.deserializeDateKeywords("""[{"k":"soon"}]""")
        )
    }

    @Test
    fun `recurring task codec preserves stable ids and schedule fields`() {
        val tasks = listOf(
            RecurringTaskItem(
                id = "task-1",
                title = "Water plants",
                description = "Balcony first",
                isActive = true,
                itemType = RecurringTaskItemType.TASK,
                scheduleMode = RecurringTaskScheduleMode.DAY_OF_WEEK,
                frequency = RecurringTaskFrequency.WEEKLY,
                dueDayOfMonth = null,
                dueDayOfWeek = 6,
                leadDays = 1,
                anchorDate = "2026-08-01",
                startMonth = "2026-08",
                startTime = "09:00",
                endMode = RecurringTaskEndMode.AFTER_OCCURRENCES,
                endDate = null,
                endCount = 12,
                retiredOn = null,
                createdAt = 1234L
            )
        )

        val restored = BackupService.deserializeRecurringTasks(
            BackupService.serializeRecurringTasks(tasks)
        )

        assertEquals(tasks, restored)
    }

    @Test
    fun `recurring task codec skips malformed rows safely`() {
        val restored = BackupService.deserializeRecurringTasks(
            """[
              {"id":"","title":"Missing id","scheduleMode":"DAY_OF_WEEK","frequency":"WEEKLY"},
              {"id":"bad-mode","title":"Bad mode","scheduleMode":"NOPE","frequency":"WEEKLY"},
              {"id":"task-2","title":"Valid","scheduleMode":"FIRST_DAY_OF_MONTH","frequency":"MONTHLY","anchorDate":"2026-08-01","startMonth":"2026-08"}
            ]"""
        )

        assertEquals(1, restored.size)
        assertEquals("task-2", restored.single().id)
    }
}
