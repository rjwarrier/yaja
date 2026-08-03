package com.mj.yaja.ui.utils

import com.mj.yaja.data.DateKeywordEntry
import com.mj.yaja.data.DateOrderPreference
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DateLinkUtilsTest {

    @Test
    fun `resolveMonthFirst honors explicit date order preference`() {
        assertFalse(DateLinkUtils.resolveMonthFirst(DateOrderPreference.DMY))
        assertEquals(true, DateLinkUtils.resolveMonthFirst(DateOrderPreference.MDY))
    }

    @Test
    fun `numeric full dates respect day first and month first preference`() {
        val entryDate = LocalDate.of(2026, 5, 10)

        val dayFirst =
            DateLinkUtils.detectDateLinks(
                text = "Follow up on 03/04/2026",
                entryDate = entryDate,
                monthFirst = false
            )
        val monthFirst =
            DateLinkUtils.detectDateLinks(
                text = "Follow up on 03/04/2026",
                entryDate = entryDate,
                monthFirst = true
            )

        assertEquals(listOf(LocalDate.of(2026, 4, 3)), dayFirst.map { it.date })
        assertEquals(listOf(LocalDate.of(2026, 3, 4)), monthFirst.map { it.date })
    }

    @Test
    fun `numeric full dates fall back when preferred ordering is invalid`() {
        val links =
            DateLinkUtils.detectDateLinks(
                text = "Invoice due 28/03/2026",
                entryDate = LocalDate.of(2026, 5, 10),
                monthFirst = true
            )

        assertEquals(listOf(LocalDate.of(2026, 3, 28)), links.map { it.date })
    }

    @Test
    fun `relative phrases resolve around entry date and do not duplicate overlaps`() {
        val entryDate = LocalDate.of(2026, 3, 30)
        val links =
            DateLinkUtils.detectDateLinks(
                text = "day before yesterday, yesterday, today, tomorrow, day after tomorrow",
                entryDate = entryDate
            )

        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 28),
                LocalDate.of(2026, 3, 29),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 1)
            ),
            links.map { it.date }
        )
    }

    @Test
    fun `weekday references resolve last next and nearest variants`() {
        val links =
            DateLinkUtils.detectDateLinks(
                text = "Friday, next Tuesday, last Monday, this Wednesday",
                entryDate = LocalDate.of(2026, 3, 30)
            )

        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 27),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 4, 1)
            ),
            links.map { it.date }
        )
    }

    @Test
    fun `custom date keywords support unicode boundaries and filter self links`() {
        val links =
            DateLinkUtils.detectDateLinks(
                text = "ഇന്നലെ ഇന്ന് നാളെ ഇന്നലെക്കുറിച്ച്",
                entryDate = LocalDate.of(2026, 3, 30),
                customKeywords =
                    listOf(
                        DateKeywordEntry(keyword = "ഇന്നലെ", meaning = "yesterday"),
                        DateKeywordEntry(keyword = "ഇന്ന്", meaning = "today"),
                        DateKeywordEntry(keyword = "നാളെ", meaning = "tomorrow")
                    )
            )

        assertEquals(
            listOf(
                LocalDate.of(2026, 3, 29),
                LocalDate.of(2026, 3, 31)
            ),
            links.map { it.date }
        )
    }

    @Test
    fun `custom date keywords ignore unknown meanings`() {
        val links =
            DateLinkUtils.detectDateLinks(
                text = "someday",
                entryDate = LocalDate.of(2026, 3, 30),
                customKeywords = listOf(DateKeywordEntry(keyword = "someday", meaning = "later"))
            )

        assertEquals(emptyList<LocalDate>(), links.map { it.date })
    }
}
