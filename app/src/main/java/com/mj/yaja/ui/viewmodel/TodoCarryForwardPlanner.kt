package com.mj.yaja.ui.viewmodel

import com.mj.yaja.data.TodoParser
import java.time.LocalDate

internal object TodoCarryForwardPlanner {
    private val carryForwardMarkerRegex = Regex("""\s*\(from \d{4}-\d{2}-\d{2}\)$""")

    fun stripCarryForwardMarker(text: String): String =
        text.trim().replace(carryForwardMarkerRegex, "").trim()

    fun normalizeTodoText(text: String): String =
        stripCarryForwardMarker(text).lowercase()

    fun buildCarryForwardLine(displayText: String, sourceDate: LocalDate): String =
        "[ ] ${stripCarryForwardMarker(displayText)} (from $sourceDate)"

    fun missingTodosForToday(
        openTodosFromPreviousDay: List<String>,
        todaysEntries: List<String>
    ): List<String> {
        val existingTodoTexts =
            todaysEntries.flatMap { entry ->
                entry.lines().mapNotNull(TodoParser::parseLine)
            }.map { normalizeTodoText(it.displayText) }.toSet()

        return openTodosFromPreviousDay
            .map(::stripCarryForwardMarker)
            .filter { it.isNotBlank() }
            .distinctBy(::normalizeTodoText)
            .filter { todo -> normalizeTodoText(todo) !in existingTodoTexts }
    }

    fun removeMovedTodosFromEntries(
        entries: List<String>,
        movedTodos: List<String>
    ): List<String> {
        if (movedTodos.isEmpty()) return entries

        val remainingCounts =
            movedTodos
                .groupingBy(::normalizeTodoText)
                .eachCount()
                .toMutableMap()

        return entries.mapNotNull { entry ->
            val keptLines =
                entry.lines().filter { line ->
                    val parsedTodo = TodoParser.parseLine(line)
                    if (parsedTodo == null || parsedTodo.isChecked) {
                        true
                    } else {
                        val normalized = normalizeTodoText(parsedTodo.displayText)
                        val remaining = remainingCounts[normalized] ?: 0
                        if (remaining > 0) {
                            remainingCounts[normalized] = remaining - 1
                            false
                        } else {
                            true
                        }
                    }
                }

            keptLines.joinToString("\n").trim().takeIf { it.isNotBlank() }
        }
    }

    fun buildCarryForwardBlock(
        todaysEntries: List<String>,
        missingTodos: List<String>,
        sourceDate: LocalDate
    ): String {
        val heading = "### Carried Forward"
        return buildList {
            if (todaysEntries.none { it.contains(heading) }) add(heading)
            addAll(missingTodos.map { todo -> buildCarryForwardLine(todo, sourceDate) })
        }.joinToString("\n")
    }
}
