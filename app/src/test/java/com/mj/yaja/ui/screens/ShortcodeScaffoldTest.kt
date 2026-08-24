package com.mj.yaja.ui.screens

import com.mj.yaja.ui.utils.ShortcodeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcodeScaffoldTest {

    @Test
    fun `scaffold parks the caret directly after the colon`() {
        val result = dynamicScaffoldValue("@T")

        assertEquals("{{today:}}", result.text)
        assertEquals(
            "caret must sit in the format slot, not after the closing braces",
            result.text.indexOf(':') + 1,
            result.selection.start
        )
        assertTrue(result.selection.collapsed)
    }

    @Test
    fun `typing the format at the caret produces a placeholder that expands`() {
        val scaffold = dynamicScaffoldValue("@T")
        val typed = scaffold.text.replaceRange(
            scaffold.selection.start,
            scaffold.selection.end,
            "dd-MMM"
        )

        assertEquals("{{today:dd-MMM}}", typed)

        val expanded = ShortcodeManager.expandValue(typed)
        assertFalse("scaffold must yield a placeholder that actually resolves", expanded.contains("{{"))
    }

    @Test
    fun `bare at sign is not enough to seed a scaffold`() {
        assertEquals("", dynamicScaffoldValue("@").text)
    }

    @Test
    fun `code without leading at sign seeds nothing`() {
        assertEquals("", dynamicScaffoldValue("today").text)
    }
}
