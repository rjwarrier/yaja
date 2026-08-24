package com.mj.yaja.ui.screens

import androidx.compose.ui.text.TextRange
import com.mj.yaja.ui.utils.ShortcodeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcodeScaffoldTest {

    @Test
    fun `every recognized date type seeds its own placeholder`() {
        ShortcodeManager.PLACEHOLDER_TYPES.forEach { type ->
            assertEquals("{{$type:}}", dynamicScaffoldValue("@$type").text)
        }
    }

    @Test
    fun `a code that is not a date type seeds nothing`() {
        listOf("@2day", "@yday", "@this", "@t", "@todayish", "@nowhere").forEach { code ->
            assertEquals("seeding $code would only be text to delete", "", dynamicScaffoldValue(code).text)
        }
    }

    @Test
    fun `seed appears only once the code spells a whole date type`() {
        // Also a regression guard: the seed used to latch on the first character and never
        // update again, so typing past a match has to keep being re-evaluated.
        val seeds = listOf("@", "@t", "@to", "@tod", "@toda", "@today", "@todayx")
            .map { dynamicScaffoldValue(it).text }

        assertEquals(
            listOf("", "", "", "", "", "{{today:}}", ""),
            seeds
        )
    }

    @Test
    fun `type match ignores case but seeds the canonical name so it still resolves`() {
        val seeded = dynamicScaffoldValue("@Today").text

        assertEquals("{{today:}}", seeded)
        assertNull(ShortcodeManager.unresolvedPlaceholderType(seeded))
    }

    @Test
    fun `format slot lands after the colon and tolerates text with no placeholder`() {
        assertEquals(TextRange("{{this:".length), formatSlotOf("{{this:}}"))
        assertEquals(TextRange("plain".length), formatSlotOf("plain"))
    }

    @Test
    fun `scaffold parks the caret directly after the colon`() {
        val result = dynamicScaffoldValue("@today")

        assertEquals("{{today:}}", result.text)
        assertEquals(
            "caret must sit in the format slot, not after the closing braces",
            result.text.indexOf(':') + 1,
            result.selection.start
        )
        assertTrue(result.selection.collapsed)
    }

    @Test
    fun `typing the format at the caret of a recognized type yields an expanding placeholder`() {
        val scaffold = dynamicScaffoldValue("@today")
        val typed = scaffold.text.replaceRange(
            scaffold.selection.start,
            scaffold.selection.end,
            "dd-MMM"
        )

        assertEquals("{{today:dd-MMM}}", typed)
        assertFalse(ShortcodeManager.expandValue(typed).contains("{{"))
    }

    @Test
    fun `bare at sign is not enough to seed a scaffold`() {
        assertEquals("", dynamicScaffoldValue("@").text)
    }

    @Test
    fun `code without leading at sign seeds nothing`() {
        assertEquals("", dynamicScaffoldValue("today").text)
    }

    // ── unresolved placeholder detection ──────────────────────────────────

    @Test
    fun `a hand written placeholder that is not a date type is reported as unresolvable`() {
        assertEquals("2day", ShortcodeManager.unresolvedPlaceholderType("{{2day:dd-HH-mm}}"))
    }

    @Test
    fun `every recognized type resolves without warning`() {
        ShortcodeManager.PLACEHOLDER_TYPES.forEach { type ->
            assertNull(
                "$type must be accepted",
                ShortcodeManager.unresolvedPlaceholderType("{{$type:dd}}")
            )
        }
    }

    @Test
    fun `plain text without placeholders is not flagged`() {
        assertNull(ShortcodeManager.unresolvedPlaceholderType("just some text"))
        assertNull(ShortcodeManager.unresolvedPlaceholderType(""))
    }

    @Test
    fun `unresolvable type is detected alongside a valid one`() {
        assertEquals(
            "2day",
            ShortcodeManager.unresolvedPlaceholderType("{{today:dd}} and {{2day:dd}}")
        )
    }
}
