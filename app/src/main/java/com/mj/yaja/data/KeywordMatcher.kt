package com.mj.yaja.data

import kotlin.math.abs

/**
 * Pure, deterministic keyword matcher. No Android dependencies — fully unit-testable.
 *
 * Matching rules (in priority order):
 *  1. Exact canonical name match      → confidence 1.00, EXACT_NAME
 *  2. Exact alias match               → confidence 0.95, EXACT_ALIAS
 *  3. Fuzzy name match (edit distance)→ confidence proportional, FUZZY_NAME
 *  4. Fuzzy alias match               → confidence proportional, FUZZY_ALIAS
 *
 * A match is only accepted when confidence >= [fuzzyThreshold] (default 0.90).
 * Exact matches always pass (1.0 and 0.95 are both >= 0.90).
 *
 * Matching is:
 *  - Case-insensitive
 *  - Whole-word via Unicode letter-class boundaries (?<!\p{L}) and (?!\p{L})
 *  - Phrase-aware (multi-word names match as phrases naturally)
 *  - Punctuation-tolerant (boundaries don't block "Mom," or "Arjun!")
 *  - Timestamp-stripped before scanning
 */
object KeywordMatcher {

    private val TIMESTAMP_REGEX = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val NON_LETTER_DIGIT_REGEX = Regex("[^\\p{L}\\d]")
    private val SNIPPET_MAX = 120

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Find all keyword matches inside [entry].
     *
     * @param entry        Raw entry string (may contain `<!--time:...-->` comments).
     * @param entryIndex   0-based index of this entry within its day.
     * @param date         ISO date string "YYYY-MM-DD" of the file the entry belongs to.
     * @param keyword      The keyword definition to scan for.
     * @param fuzzyThreshold Minimum confidence to accept a match (default 0.90).
     */
    fun findMatches(
        entry: String,
        entryIndex: Int,
        date: String,
        keyword: KeywordDefinition,
        fuzzyThreshold: Float = 0.90f
    ): List<KeywordMatch> {
        if (!keyword.isEnabled) return emptyList()

        val cleaned = stripTimestamps(entry)
        if (cleaned.isBlank()) return emptyList()

        val results = mutableListOf<KeywordMatch>()
        val occupiedRanges = linkedSetOf<IntRange>()

        // 1. Exact canonical name
        val namePattern = buildPattern(keyword.name)
        namePattern.findAll(cleaned).forEach { mr ->
            if (occupiedRanges.add(mr.range)) {
                results += KeywordMatch(
                    keywordId  = keyword.id,
                    date       = date,
                    entryIndex = entryIndex,
                    matchedText = mr.value,
                    confidence  = 1.0f,
                    matchType   = KeywordMatchType.EXACT_NAME,
                    snippet     = buildSnippet(cleaned, mr.range.first),
                    startIndex  = mr.range.first,
                    endExclusive = mr.range.last + 1
                )
            }
        }

        // 2. Exact alias matches
        val normalizedName = keyword.name.trim().lowercase()
        val distinctAliases = keyword.aliases
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .filter { it.lowercase() != normalizedName }
            .sortedByDescending { it.length }
            .toList()

        for (alias in distinctAliases) {
            if (alias.isBlank()) continue
            val aliasPattern = buildPattern(alias)
            aliasPattern.findAll(cleaned).forEach { mr ->
                if (!hasOverlappingRange(occupiedRanges, mr.range)) {
                    occupiedRanges.add(mr.range)
                    results += KeywordMatch(
                        keywordId   = keyword.id,
                        date        = date,
                        entryIndex  = entryIndex,
                        matchedText = mr.value,
                        confidence  = 0.95f,
                        matchType   = KeywordMatchType.EXACT_ALIAS,
                        snippet     = buildSnippet(cleaned, mr.range.first),
                        startIndex  = mr.range.first,
                        endExclusive = mr.range.last + 1
                    )
                }
            }
        }

        // 3. Fuzzy matching — only if nothing exact was found yet
        if (results.isEmpty()) {
            // Tokenise cleaned text into words for fuzzy comparison
            val words = tokenizeWithRanges(cleaned)
            // Try fuzzy name
            val nameTokens = tokenize(keyword.name)
            val fuzzyNameResult = findFuzzyPhrase(
                words, cleaned, nameTokens, keyword.id, date, entryIndex,
                fuzzyThreshold, KeywordMatchType.FUZZY_NAME
            )
            if (fuzzyNameResult != null) results += fuzzyNameResult

            // Try fuzzy aliases
            if (results.isEmpty()) {
                for (alias in distinctAliases) {
                    if (alias.isBlank()) continue
                    val aliasTokens = tokenize(alias)
                    val fuzzyAliasResult = findFuzzyPhrase(
                        words, cleaned, aliasTokens, keyword.id, date, entryIndex,
                        fuzzyThreshold, KeywordMatchType.FUZZY_ALIAS
                    )
                    if (fuzzyAliasResult != null) {
                        results += fuzzyAliasResult
                        break
                    }
                }
            }
        }

        return results
    }

    fun resolveOverlappingMatches(matches: List<KeywordMatch>): List<KeywordMatch> {
        if (matches.size < 2) return matches

        val prioritized = matches.sortedWith(
            compareByDescending<KeywordMatch> { it.matchPriority() }
                .thenByDescending { it.matchSpanLength() }
                .thenByDescending { it.confidence }
                .thenBy { it.keywordId }
                .thenBy { it.startIndex }
        )

        val kept = mutableListOf<KeywordMatch>()
        for (match in prioritized) {
            val hasValidRange = match.startIndex >= 0 && match.endExclusive > match.startIndex
            if (!hasValidRange || kept.none { overlaps(it, match) }) {
                kept += match
            }
        }

        return kept.sortedWith(compareBy<KeywordMatch> { it.entryIndex }.thenBy { it.startIndex })
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    /** Remove all HTML-comment-style timestamp/metadata markers from [text]. */
    fun stripTimestamps(text: String): String =
        TIMESTAMP_REGEX.replace(text, "").trim()

    /**
     * Build a whole-word, case-insensitive regex for [text].
     * Uses Unicode letter-class boundaries so non-Latin scripts work correctly.
     */
    fun buildPattern(text: String): Regex =
        Regex(
            """(?<!\p{L})${Regex.escape(text)}(?!\p{L})""",
            setOf(RegexOption.IGNORE_CASE)
        )

    /** Split text into lowercase words (letters and digits only, strips punctuation). */
    private fun tokenize(text: String): List<String> =
        text.lowercase().splitToSequence(WHITESPACE_REGEX)
            .mapNotNull { token -> token.replace(NON_LETTER_DIGIT_REGEX, "").takeIf { it.isNotEmpty() } }
            .toList()

    private data class TokenSpan(
        val normalized: String,
        val raw: String,
        val start: Int,
        val endExclusive: Int
    )

    /** Tokenise text while keeping original ranges so fuzzy snippets/matched text stay accurate. */
    private fun tokenizeWithRanges(text: String): List<TokenSpan> =
        Regex("""[\p{L}\d]+""")
            .findAll(text)
            .map { mr ->
                TokenSpan(
                    normalized = mr.value.lowercase(),
                    raw = mr.value,
                    start = mr.range.first,
                    endExclusive = mr.range.last + 1
                )
            }
            .toList()

    /**
     * Look for a fuzzy match of [targetTokens] as a contiguous phrase in [words].
     * For single-token targets, compute Levenshtein against each word.
     * For multi-token targets, require all tokens to fuzzy-match in order.
     * Returns the best match above [threshold], or null.
     */
    private fun findFuzzyPhrase(
        words: List<TokenSpan>,
        cleaned: String,
        targetTokens: List<String>,
        keywordId: String,
        date: String,
        entryIndex: Int,
        threshold: Float,
        matchType: KeywordMatchType
    ): KeywordMatch? {
        if (targetTokens.isEmpty()) return null

        var bestConfidence = 0f
        var bestWord = ""
        var bestStart = -1

        if (targetTokens.size == 1) {
            val target = targetTokens[0]
            if (target.length <= 2) return null  // Too short for fuzzy — too many false positives
            for (word in words) {
                if (word.normalized.length <= 2) continue
                val conf = fuzzyConfidence(word.normalized, target, threshold) ?: continue
                if (conf > bestConfidence) {
                    bestConfidence = conf
                    bestWord = word.raw
                    bestStart = word.start
                }
            }
        } else {
            // Multi-token: find best contiguous window of same length
            val windowSize = targetTokens.size
            for (i in 0..(words.size - windowSize)) {
                val window = words.subList(i, i + windowSize)
                var totalConf = 0f
                var allAboveThreshold = true
                for (j in window.indices) {
                    val target = targetTokens[j]
                    val w = window[j]
                    if (target.length <= 2 || w.normalized.length <= 2) {
                        // Short tokens: require exact match
                        if (!w.normalized.equals(target, ignoreCase = true)) { allAboveThreshold = false; break }
                        totalConf += 1.0f
                        continue
                    }
                    val conf = fuzzyConfidence(w.normalized, target, threshold)
                    if (conf == null) { allAboveThreshold = false; break }
                    totalConf += conf
                }
                if (allAboveThreshold) {
                    val avgConf = totalConf / windowSize
                    if (avgConf > bestConfidence) {
                        bestConfidence = avgConf
                        bestWord = cleaned.substring(window.first().start, window.last().endExclusive)
                        bestStart = window.first().start
                    }
                }
            }
        }

        if (bestConfidence < threshold || bestWord.isEmpty() || bestStart < 0) return null

        return KeywordMatch(
            keywordId   = keywordId,
            date        = date,
            entryIndex  = entryIndex,
            matchedText = bestWord,
            confidence  = bestConfidence,
            matchType   = matchType,
            snippet     = buildSnippet(cleaned, bestStart),
            startIndex  = bestStart,
            endExclusive = bestStart + bestWord.length
        )
    }

    /** Build a ~120-char snippet around [matchPos] in [text]. */
    private fun buildSnippet(text: String, matchPos: Int): String {
        val start = maxOf(0, matchPos - 40)
        val end = minOf(text.length, start + SNIPPET_MAX)
        val raw = text.substring(start, end).replace('\n', ' ').trim()
        return if (start > 0) "…$raw" else raw
    }

    private fun hasOverlappingRange(existing: Set<IntRange>, candidate: IntRange): Boolean =
        existing.any { range -> range.first <= candidate.last && candidate.first <= range.last }

    private fun overlaps(a: KeywordMatch, b: KeywordMatch): Boolean =
        a.startIndex < b.endExclusive && b.startIndex < a.endExclusive

    private fun KeywordMatch.matchSpanLength(): Int =
        if (startIndex >= 0 && endExclusive > startIndex) {
            endExclusive - startIndex
        } else {
            matchedText.length
        }

    private fun KeywordMatch.matchPriority(): Int {
        val isExact = matchType == KeywordMatchType.EXACT_NAME || matchType == KeywordMatchType.EXACT_ALIAS
        val isMultiWord = wordCount() > 1
        return when {
            isExact && isMultiWord -> 4
            isExact -> 3
            isMultiWord -> 2
            else -> 1
        }
    }

    private fun KeywordMatch.wordCount(): Int =
        Regex("""[\p{L}\d]+""").findAll(matchedText).count().coerceAtLeast(1)

    private fun fuzzyConfidence(candidate: String, target: String, threshold: Float): Float? {
        val maxLen = maxOf(candidate.length, target.length)
        if (maxLen == 0) return null

        val maxDistance = ((1f - threshold) * maxLen).toInt()
        if (abs(candidate.length - target.length) > maxDistance) return null

        val distance = levenshteinDistance(candidate, target, maxDistance)
        if (distance > maxDistance) return null

        return 1.0f - distance / maxLen.toFloat()
    }

    /**
     * Standard Levenshtein edit distance between [a] and [b].
     * Both should be pre-lowercased.
     */
    fun levenshteinDistance(a: String, b: String, maxDistance: Int = Int.MAX_VALUE): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        if (maxDistance != Int.MAX_VALUE && abs(a.length - b.length) > maxDistance) {
            return maxDistance + 1
        }

        val longer: String
        val shorter: String
        if (a.length >= b.length) {
            longer = a
            shorter = b
        } else {
            longer = b
            shorter = a
        }

        var previousRow = IntArray(shorter.length + 1) { it }
        var currentRow = IntArray(shorter.length + 1)

        for (i in 1..longer.length) {
            currentRow[0] = i
            var rowMin = currentRow[0]

            for (j in 1..shorter.length) {
                val substitutionCost = if (longer[i - 1] == shorter[j - 1]) 0 else 1
                currentRow[j] = minOf(
                    previousRow[j] + 1,
                    currentRow[j - 1] + 1,
                    previousRow[j - 1] + substitutionCost
                )
                rowMin = minOf(rowMin, currentRow[j])
            }

            if (maxDistance != Int.MAX_VALUE && rowMin > maxDistance) {
                return maxDistance + 1
            }

            val temp = previousRow
            previousRow = currentRow
            currentRow = temp
        }
        return previousRow[shorter.length]
    }
}
