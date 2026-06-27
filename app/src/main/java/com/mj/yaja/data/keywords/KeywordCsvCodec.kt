package com.mj.yaja.data.keywords

import com.mj.yaja.data.KeywordDefinition
import com.mj.yaja.data.KeywordType
import java.util.UUID

object KeywordCsvCodec {
    private val headerRow = listOf("name", "type", "relation", "aliases", "enabled")

    fun encode(keywords: List<KeywordDefinition>): String {
        val rows = buildList {
            add(headerRow)
            keywords.sortedBy { it.name.lowercase() }.forEach { keyword ->
                add(
                    listOf(
                        keyword.name,
                        keyword.type.name,
                        keyword.relation,
                        keyword.aliases.joinToString("|"),
                        keyword.isEnabled.toString()
                    )
                )
            }
        }

        return rows.joinToString(separator = "\n") { row ->
            row.joinToString(separator = ",") { field -> escapeField(field) }
        } + "\n"
    }

    fun template(): String = listOf(
        headerRow,
        listOf("Ved", "PERSON", "Son", "Vedu|Mon", "true"),
        listOf("Vyas", "PERSON", "Friend", "Vyasu", "true"),
        listOf("Bengaluru", "PLACE", "Office", "Bangalore|BLR", "true")
    ).joinToString(separator = "\n") { row ->
        row.joinToString(separator = ",") { field -> escapeField(field) }
    } + "\n"

    fun isHeader(line: String): Boolean {
        val header = parseCsvLine(line).map { it.trim().lowercase() }
        return header == headerRow
    }

    fun parseLine(
        line: String,
        idFactory: () -> String = { UUID.randomUUID().toString() },
        createdAtProvider: () -> Long = { System.currentTimeMillis() }
    ): KeywordDefinition? {
        if (line.isBlank()) return null

        val fields = parseCsvLine(line)
        if (fields.size < 5) return null

        val name = fields[0].trim()
        val type = parseKeywordType(fields[1]) ?: return null
        val relation = fields[2].trim()
        val aliases = fields[3]
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val enabled = when (fields[4].trim().lowercase()) {
            "true", "1", "yes", "y" -> true
            "false", "0", "no", "n" -> false
            else -> true
        }

        if (name.isBlank()) return null

        return KeywordDefinition(
            id = idFactory(),
            name = name,
            type = type,
            relation = relation,
            aliases = aliases,
            isEnabled = enabled,
            createdAt = createdAtProvider()
        )
    }

    private fun parseKeywordType(raw: String): KeywordType? = when (raw.trim().uppercase()) {
        "PERSON", "PEOPLE" -> KeywordType.PERSON
        "PLACE", "PLACES" -> KeywordType.PLACE
        else -> null
    }

    private fun escapeField(field: String): String {
        val escaped = field.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }

        result.add(current.toString())
        return result
    }
}
