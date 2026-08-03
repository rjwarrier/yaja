package com.mj.yaja.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DayOneImportParserTest {

    @Test
    fun `cleanAndSplit trims multiline text and drops blank lines`() {
        val lines =
            DayOneImportParser.cleanAndSplit(
                "First line\n\n  Second line  "
            )

        assertEquals(listOf("First line", "Second line"), lines)
    }

    @Test
    fun `cleanAndSplit removes Day One image-only entries`() {
        val lines = DayOneImportParser.cleanAndSplit("![](dayone-moment://ABCDEF)")

        assertEquals(emptyList<String>(), lines)
    }

    @Test
    fun `cleanAndSplit removes html tags while preserving text`() {
        val lines =
            DayOneImportParser.cleanAndSplit(
                """<p dir="auto">Walked <strong>five</strong> km</p>"""
            )

        assertEquals(listOf("Walked five km"), lines)
    }

    @Test
    fun `cleanAndSplit keeps non image markdown links`() {
        val lines = DayOneImportParser.cleanAndSplit("Read [docs](https://example.com)")

        assertEquals(listOf("Read [docs](https://example.com)"), lines)
    }
}
