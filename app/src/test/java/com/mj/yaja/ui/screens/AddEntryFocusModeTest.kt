package com.mj.yaja.ui.screens

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEntryFocusModeTest {

    private val roomy = 900.dp
    private val cramped = 300.dp
    private val keyboard = 320.dp

    @Test
    fun `stays off while the keyboard is down`() {
        assertFalse(
            shouldAutoEnableFocusMode(
                isEditingMode = true,
                imeHeight = 0.dp,
                availableHeight = cramped,
                fontScale = 1f
            )
        )
    }

    @Test
    fun `stays off when reading an entry rather than editing it`() {
        assertFalse(
            shouldAutoEnableFocusMode(
                isEditingMode = false,
                imeHeight = keyboard,
                availableHeight = cramped,
                fontScale = 1f
            )
        )
    }

    @Test
    fun `stays off when the editor still has room`() {
        assertFalse(
            shouldAutoEnableFocusMode(
                isEditingMode = true,
                imeHeight = keyboard,
                availableHeight = roomy,
                fontScale = 1f
            )
        )
    }

    @Test
    fun `turns on when the keyboard leaves the editor under three lines`() {
        assertTrue(
            shouldAutoEnableFocusMode(
                isEditingMode = true,
                imeHeight = keyboard,
                availableHeight = cramped,
                fontScale = 1f
            )
        )
    }

    @Test
    fun `a larger font scale raises the height the editor needs`() {
        val height = EDITOR_CHROME_HEIGHT + EDITOR_MIN_VISIBLE_HEIGHT + 40.dp

        assertFalse(
            "comfortable at font scale 1",
            shouldAutoEnableFocusMode(
                isEditingMode = true,
                imeHeight = keyboard,
                availableHeight = height,
                fontScale = 1f
            )
        )
        assertTrue(
            "same height is cramped once the font scale grows",
            shouldAutoEnableFocusMode(
                isEditingMode = true,
                imeHeight = keyboard,
                availableHeight = height,
                fontScale = 1.5f
            )
        )
    }

    @Test
    fun `the threshold itself counts as enough room`() {
        val threshold = EDITOR_CHROME_HEIGHT + EDITOR_MIN_VISIBLE_HEIGHT

        assertFalse(
            "exactly three lines is still three lines",
            shouldAutoEnableFocusMode(
                isEditingMode = true,
                imeHeight = keyboard,
                availableHeight = threshold,
                fontScale = 1f
            )
        )
        assertTrue(
            "a hair under the threshold switches",
            shouldAutoEnableFocusMode(
                isEditingMode = true,
                imeHeight = keyboard,
                availableHeight = threshold - 1.dp,
                fontScale = 1f
            )
        )
    }
}
