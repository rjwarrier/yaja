package com.mj.yaja.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownFileManagerTest {

    // ── parseEntries ─────────────────────────────────────────────────────

    @Test
    fun `parseEntries returns empty list for blank input`() {
        val result = MarkdownFileManager.parseEntries(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseEntries skips date heading lines`() {
        val lines = listOf("# 2024-01-15", "- Hello world")
        val result = MarkdownFileManager.parseEntries(lines)
        assertEquals(listOf("Hello world"), result)
    }

    @Test
    fun `parseEntries parses single entry`() {
        val lines = listOf("- Went for a walk today")
        val result = MarkdownFileManager.parseEntries(lines)
        assertEquals(1, result.size)
        assertEquals("Went for a walk today", result[0])
    }

    @Test
    fun `parseEntries parses multiple entries`() {
        val lines = listOf(
            "- First entry",
            "- Second entry",
            "- Third entry"
        )
        val result = MarkdownFileManager.parseEntries(lines)
        assertEquals(3, result.size)
        assertEquals("First entry", result[0])
        assertEquals("Second entry", result[1])
        assertEquals("Third entry", result[2])
    }

    @Test
    fun `parseEntries joins continuation lines into a single entry`() {
        val lines = listOf(
            "- Line one",
            "continuation of line one",
            "still the same entry"
        )
        val result = MarkdownFileManager.parseEntries(lines)
        assertEquals(1, result.size)
        assertEquals("Line one\ncontinuation of line one\nstill the same entry", result[0])
    }

    @Test
    fun `parseEntries trims trailing whitespace from entries`() {
        val lines = listOf("- Entry with trailing space   ")
        val result = MarkdownFileManager.parseEntries(lines)
        assertEquals("Entry with trailing space", result[0])
    }

    @Test
    fun `parseEntries skips leading blank lines`() {
        val lines = listOf("", "  ", "- Actual entry")
        val result = MarkdownFileManager.parseEntries(lines)
        assertEquals(listOf("Actual entry"), result)
    }

    @Test
    fun `parseEntries handles timestamp comment prefix`() {
        val lines = listOf("- <!--time:09:30-->\nWent to the gym")
        val result = MarkdownFileManager.parseEntries(lines)
        assertEquals(1, result.size)
        assertEquals("<!--time:09:30-->\nWent to the gym", result[0])
    }

    @Test
    fun `parseEntries handles entry with markdown formatting`() {
        val lines = listOf("- **Bold text** and _italic_")
        val result = MarkdownFileManager.parseEntries(lines)
        assertEquals("**Bold text** and _italic_", result[0])
    }

    @Test
    fun `parseEntries handles mixed headings entries and blank lines`() {
        val lines = listOf(
            "# 2024-03-15",
            "",
            "- First entry",
            "- Second entry",
            "",
            "- Third entry"
        )
        val result = MarkdownFileManager.parseEntries(lines)
        // Blank line between entries doesn't split continuation — it's just ignored if
        // currentEntry is empty. Second and third entries start new bullets so they're separate.
        assertEquals(3, result.size)
    }
}
