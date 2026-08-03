package com.mj.yaja.ui.viewmodel

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TodoCarryForwardPlannerTest {

    @Test
    fun `missingTodosForToday strips markers removes blanks and avoids duplicates`() {
        val missing =
            TodoCarryForwardPlanner.missingTodosForToday(
                openTodosFromPreviousDay =
                    listOf(
                        "Buy milk (from 2026-03-29)",
                        "buy milk",
                        "Call mom",
                        "   "
                    ),
                todaysEntries = listOf("[ ] Call Mom")
            )

        assertEquals(listOf("Buy milk"), missing)
    }

    @Test
    fun `removeMovedTodosFromEntries removes only matching unchecked todos`() {
        val updated =
            TodoCarryForwardPlanner.removeMovedTodosFromEntries(
                entries =
                    listOf(
                        "[ ] Buy milk\n[x] Call mom\nJournal note",
                        "[ ] Buy milk\n[ ] Pay rent"
                    ),
                movedTodos = listOf("buy milk")
            )

        assertEquals(
            listOf(
                "[x] Call mom\nJournal note",
                "[ ] Buy milk\n[ ] Pay rent"
            ),
            updated
        )
    }

    @Test
    fun `removeMovedTodosFromEntries preserves source entries when nothing moved`() {
        val entries = listOf("[ ] Buy milk", "Journal note")

        assertEquals(entries, TodoCarryForwardPlanner.removeMovedTodosFromEntries(entries, emptyList()))
    }

    @Test
    fun `removeMovedTodosFromEntries drops entries that become blank`() {
        val updated =
            TodoCarryForwardPlanner.removeMovedTodosFromEntries(
                entries = listOf("[ ] Buy milk"),
                movedTodos = listOf("Buy milk")
            )

        assertEquals(emptyList<String>(), updated)
    }

    @Test
    fun `buildCarryForwardBlock adds heading when today has no carried forward section`() {
        val block =
            TodoCarryForwardPlanner.buildCarryForwardBlock(
                todaysEntries = listOf("Morning notes"),
                missingTodos = listOf("Buy milk (from 2026-03-20)", "Call mom"),
                sourceDate = LocalDate.of(2026, 3, 29)
            )

        assertEquals(
            "### Carried Forward\n[ ] Buy milk (from 2026-03-29)\n[ ] Call mom (from 2026-03-29)",
            block
        )
    }

    @Test
    fun `buildCarryForwardBlock omits duplicate heading when section already exists today`() {
        val block =
            TodoCarryForwardPlanner.buildCarryForwardBlock(
                todaysEntries = listOf("### Carried Forward\n[ ] Existing task"),
                missingTodos = listOf("Buy milk"),
                sourceDate = LocalDate.of(2026, 3, 29)
            )

        assertEquals("[ ] Buy milk (from 2026-03-29)", block)
    }
}
