package com.mj.yaja.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEntryBodyTest {

    @Test
    fun `custom shortcode with now placeholder expands to formatted time not literal text`() {
        val codes = mapOf("@now" to "{{now:HH:mm}}")
        val current = TextFieldValue("")
        val newValue = TextFieldValue("@now", selection = TextRange(4))

        val result = handleEditorValueChange(current, newValue, codes)

        assertFalse("placeholder must not leak as literal text", result.text.contains("{{"))
        assertTrue(Regex("^\\d{2}:\\d{2}$").matches(result.text))
    }

    @Test
    fun `custom shortcode with today placeholder expands to formatted date not literal text`() {
        val codes = mapOf("@today" to "{{today:dd-MMM-yy}}")
        val current = TextFieldValue("")
        val newValue = TextFieldValue("@today", selection = TextRange(6))

        val result = handleEditorValueChange(current, newValue, codes)

        assertFalse("placeholder must not leak as literal text", result.text.contains("{{"))
    }

    @Test
    fun `placeholder typed literally into an entry is left alone`() {
        val codes = mapOf("@now" to "{{now:HH:mm}}")
        val typed = "I set @yday to {{yesterday:dd-MMM-yy}}"
        val current = TextFieldValue("")
        val newValue = TextFieldValue(typed, selection = TextRange(typed.length))

        val result = handleEditorValueChange(current, newValue, codes)

        assertTrue(
            "only substituted shortcode values expand, not text the user typed",
            result.text.contains("{{yesterday:dd-MMM-yy}}")
        )
    }

    @Test
    fun `plain shortcode without placeholder still expands verbatim`() {
        val codes = mapOf("@week" to "Week {{today:ww}}")
        val current = TextFieldValue("")
        val newValue = TextFieldValue("@week", selection = TextRange(5))

        val result = handleEditorValueChange(current, newValue, codes)

        assertTrue(result.text.startsWith("Week "))
        assertFalse(result.text.contains("{{"))
    }
}
