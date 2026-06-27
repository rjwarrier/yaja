package com.mj.yaja.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class AddEntryComponentsTest {

    @Test
    fun buildReviewTodoSuggestions_splitsBulletSeparatedTodos() {
        val suggestions =
            buildReviewTodoSuggestions("TODO buy milk \u2022 call mom \u2022 send invoice")

        assertEquals(
            listOf("buy milk", "call mom", "send invoice"),
            suggestions.map { it.sourceFragment }
        )
    }

    @Test
    fun convertReviewTodosToChecklist_splitsBulletSeparatedTodos() {
        val checklist = convertReviewTodosToChecklist("TODO buy milk \u2022 call mom")

        assertEquals(
            "[ ] buy milk\n[ ] call mom",
            checklist
        )
    }
}
