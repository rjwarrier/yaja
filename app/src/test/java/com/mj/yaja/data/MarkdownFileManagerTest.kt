package com.mj.yaja.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ── parseFrontmatter ─────────────────────────────────────────────────

    @Test
    fun `parseFrontmatter returns defaults when no frontmatter present`() {
        val result = MarkdownFileManager.parseFrontmatter(listOf("# 2024-01-15", "- Entry"))
        assertFalse(result.isStarred)
        assertEquals("", result.label)
    }

    @Test
    fun `parseFrontmatter reads starred true`() {
        val lines = listOf("---", "starred: true", "---", "# 2024-01-15")
        assertTrue(MarkdownFileManager.parseFrontmatter(lines).isStarred)
    }

    @Test
    fun `parseFrontmatter does not star when label merely contains starred and true`() {
        val lines = listOf("---", "label: \"starred true story\"", "---", "# 2024-01-15")
        val result = MarkdownFileManager.parseFrontmatter(lines)
        assertFalse(result.isStarred)
        assertEquals("starred true story", result.label)
    }

    @Test
    fun `parseFrontmatter reads starred false as not starred`() {
        val lines = listOf("---", "starred: false", "---", "# 2024-01-15")
        assertFalse(MarkdownFileManager.parseFrontmatter(lines).isStarred)
    }

    @Test
    fun `parseFrontmatter reads label starred and revisit fields together`() {
        val lines = listOf(
            "---",
            "starred: true",
            "label: \"Big day\"",
            "revisit_on: \"2024-02-01\"",
            "revisit_note: \"check progress\"",
            "---",
            "# 2024-01-15"
        )
        val result = MarkdownFileManager.parseFrontmatter(lines)
        assertTrue(result.isStarred)
        assertEquals("Big day", result.label)
        assertEquals(LocalDate.of(2024, 2, 1), result.revisitOn)
        assertEquals("check progress", result.revisitNote)
    }
}
