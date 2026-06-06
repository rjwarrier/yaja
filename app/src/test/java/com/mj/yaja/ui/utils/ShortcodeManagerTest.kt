package com.mj.yaja.ui.utils

import com.mj.yaja.data.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcodeManagerTest {

    // ── expand — basic ────────────────────────────────────────────────────

    @Test
    fun `expand returns unchanged text when no shortcodes and no at-x`() {
        val result = ShortcodeManager.expand("Hello world")
        assertEquals("Hello world", result)
    }

    @Test
    fun `expand returns unchanged text for empty input`() {
        val result = ShortcodeManager.expand("")
        assertEquals("", result)
    }

    // ── custom shortcode expansion ────────────────────────────────────────

    @Test
    fun `expand replaces custom shortcode`() {
        val codes = mapOf("!hw" to "Hello, world!")
        val result = ShortcodeManager.expand("!hw", codes)
        assertEquals("Hello, world!", result)
    }

    @Test
    fun `expand replaces multiple custom shortcodes in one pass`() {
        val codes = mapOf("!m" to "morning", "!g" to "great")
        val result = ShortcodeManager.expand("Good !m, feeling !g today", codes)
        assertEquals("Good morning, feeling great today", result)
    }

    @Test
    fun `expand does not modify text without matching shortcode`() {
        val codes = mapOf("!x" to "expanded")
        val result = ShortcodeManager.expand("no shortcode here", codes)
        assertEquals("no shortcode here", result)
    }

    @Test
    fun `expand handles shortcode whose value contains special characters`() {
        val codes = mapOf("!s" to "value:with:colons;and;semicolons")
        val result = ShortcodeManager.expand("!s", codes)
        assertEquals("value:with:colons;and;semicolons", result)
    }

    @Test
    fun `expand handles empty custom shortcodes map`() {
        val result = ShortcodeManager.expand("some text", emptyMap())
        assertEquals("some text", result)
    }

    // ── @x todo toggle ────────────────────────────────────────────────────

    @Test
    fun `expand toggles unchecked todo to checked`() {
        val result = ShortcodeManager.expand("[ ] Buy milk @x")
        assertEquals("[x] Buy milk ", result)
    }

    @Test
    fun `expand toggles checked todo to unchecked`() {
        val result = ShortcodeManager.expand("[x] Buy milk @x")
        assertEquals("[ ] Buy milk ", result)
    }

    @Test
    fun `expand inserts checked marker when at-x is on plain line`() {
        val result = ShortcodeManager.expand("Buy milk @x")
        assertEquals("Buy milk [x]", result)
    }

    @Test
    fun `expand toggles todo on second line only`() {
        val text = "[ ] Task 1\n[ ] Task 2 @x"
        val result = ShortcodeManager.expand(text)
        assertEquals("[ ] Task 1\n[x] Task 2 ", result)
    }

    @Test
    fun `expand toggles todo on first line when at-x present`() {
        val text = "[ ] Task 1 @x\n[ ] Task 2"
        val result = ShortcodeManager.expand(text)
        assertEquals("[x] Task 1 \n[ ] Task 2", result)
    }

    @Test
    fun `expand does not modify text without at-x`() {
        val text = "[ ] Task without toggle"
        val result = ShortcodeManager.expand(text)
        assertEquals("[ ] Task without toggle", result)
    }

    // ── @x absent → no toggle side effects ───────────────────────────────

    @Test
    fun `expand with custom shortcode and no at-x performs only substitution`() {
        val codes = mapOf("!d" to "Done")
        val result = ShortcodeManager.expand("[ ] Task !d", codes)
        assertEquals("[ ] Task Done", result)
        assertFalse(result.contains("@x"))
    }

    // ── serialization round-trip (via SettingsRepository) ─────────────────
    // These tests verify that shortcode keys/values with special characters survive
    // the JSON serialization introduced to replace the old colon/semicolon format.

    @Test
    fun `shortcode map with colon in key survives shortcode codec round-trip`() {
        val original = mapOf("key:colon" to "value")
        val serialized = SettingsRepository.serializeShortcodes(original)
        val restored = SettingsRepository.deserializeShortcodes(serialized)
        assertEquals(original, restored)
    }

    @Test
    fun `shortcode map with semicolon in value survives shortcode codec round-trip`() {
        val original = mapOf("!s" to "one;two;three")
        val serialized = SettingsRepository.serializeShortcodes(original)
        val restored = SettingsRepository.deserializeShortcodes(serialized)
        assertEquals(original, restored)
    }

    @Test
    fun `shortcode map with unicode value survives shortcode codec round-trip`() {
        val original = mapOf("!emoji" to "\uD83D\uDE00 Happy")
        val serialized = SettingsRepository.serializeShortcodes(original)
        val restored = SettingsRepository.deserializeShortcodes(serialized)
        assertEquals(original, restored)
    }

    @Test
    fun `empty shortcode map serializes to empty shortcode payload`() {
        val original = emptyMap<String, String>()
        val serialized = SettingsRepository.serializeShortcodes(original)
        val restored = SettingsRepository.deserializeShortcodes(serialized)
        assertTrue(serialized.isNotBlank())
        assertTrue(restored.isEmpty())
    }
}
