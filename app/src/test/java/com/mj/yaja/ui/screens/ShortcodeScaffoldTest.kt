package com.mj.yaja.ui.screens

import com.mj.yaja.ui.utils.ShortcodeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcodeScaffoldTest {

    @Test
    fun `scaffold echoes the code text without its at sign`() {
        assertEquals("{{2day:}}", dynamicScaffoldValue("@2day").text)
        assertEquals("{{today:}}", dynamicScaffoldValue("@today").text)
        assertEquals("{{yday:}}", dynamicScaffoldValue("@yday").text)
    }

    @Test
    fun `scaffold parks the caret directly after the colon`() {
        val result = dynamicScaffoldValue("@2day")

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
    fun `echoed code that is not a date type is reported as unresolvable`() {
        val scaffold = dynamicScaffoldValue("@2day")
        assertEquals("2day", ShortcodeManager.unresolvedPlaceholderType(scaffold.text))
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
