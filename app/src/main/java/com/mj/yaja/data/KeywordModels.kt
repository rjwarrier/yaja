package com.mj.yaja.data

import java.time.LocalDate
import java.time.YearMonth

/** Whether a keyword represents a person or a place. */
enum class KeywordType { PERSON, PLACE }

/** How a particular match was found. */
enum class KeywordMatchType {
    EXACT_NAME,
    EXACT_ALIAS,
    FUZZY_NAME,
    FUZZY_ALIAS
}

/**
 * A user-defined keyword to detect in journal entries.
 *
 * @param id        Stable UUID string, generated at creation time.
 * @param name      The canonical name, e.g. "Arjun" or "Mumbai".
 * @param type      PERSON or PLACE.
 * @param relation  Optional relationship label, e.g. "Son", "Friend", "Office". Empty string if unset.
 * @param aliases   Alternative spellings or names, e.g. ["Aju", "Arj"] or ["Bombay"].
 * @param isEnabled Whether this keyword participates in matching.
 * @param createdAt Epoch millis when the keyword was created.
 */
data class KeywordDefinition(
    val id: String,
    val name: String,
    val type: KeywordType,
    val relation: String,
    val aliases: List<String>,
    val isEnabled: Boolean,
    val createdAt: Long
)

/**
 * A single match found for a keyword inside a journal entry.
 *
 * @param keywordId    The [KeywordDefinition.id] of the matched keyword.
 * @param date         ISO date string "YYYY-MM-DD" of the entry's file.
 * @param entryIndex   0-based index within that day's entry list.
 * @param matchedText  The exact text span that was matched, preserving original casing.
 * @param confidence   Match confidence: 1.0 exact name, 0.95 exact alias, 0.90 fuzzy.
 * @param matchType    How the match was found.
 * @param snippet      ~100-char excerpt of cleaned entry text around the match.
 */
data class KeywordMatch(
    val keywordId: String,
    val date: String,
    val entryIndex: Int,
    val matchedText: String,
    val confidence: Float,
    val matchType: KeywordMatchType,
    val snippet: String,
    val startIndex: Int = -1,
    val endExclusive: Int = -1
)

data class KeywordCoOccurrence(
    val keywordId: String,
    val name: String,
    val type: KeywordType,
    val relation: String,
    val daysTogether: Int,
    val sharedEntries: Int,
    val lastSeenTogether: LocalDate?,
    val score: Float
)

/**
 * Aggregated statistics for one keyword, computed from the match cache.
 *
 * @param keyword         The keyword these stats belong to.
 * @param totalMentions   Total number of individual matches across all entries.
 * @param uniqueDays      Number of distinct calendar days with at least one match.
 * @param firstSeen       Earliest date a match was found, or null if no matches.
 * @param lastSeen        Most recent date a match was found, or null if no matches.
 * @param mentionsByMonth Monthly breakdown: list of (YearMonth, count) sorted ascending.
 * @param coOccurring     Other keywords that appeared on the same day, with their co-occurrence count, sorted descending.
 */
data class KeywordStats(
    val keyword: KeywordDefinition,
    val totalMentions: Int,
    val uniqueDays: Int,
    val firstSeen: LocalDate?,
    val lastSeen: LocalDate?,
    val mentionsByMonth: List<Pair<YearMonth, Int>>,
    val coOccurring: List<KeywordCoOccurrence>
)
