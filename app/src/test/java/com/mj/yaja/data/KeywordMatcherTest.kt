package com.mj.yaja.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordMatcherTest {

    private val personKeyword =
        KeywordDefinition(
            id = "1",
            name = "Sam",
            type = KeywordType.PERSON,
            relation = "Friend",
            aliases = listOf("Sammy"),
            isEnabled = true,
            createdAt = 0L
        )

    @Test
    fun exactNameMatch_isCaseInsensitive() {
        val matches = KeywordMatcher.findMatches("met sam yesterday", 0, "2026-03-30", personKeyword)
        assertEquals(1, matches.size)
        assertEquals(KeywordMatchType.EXACT_NAME, matches.first().matchType)
    }

    @Test
    fun exactAliasMatch_hasAliasConfidence() {
        val matches = KeywordMatcher.findMatches("Sammy called me", 0, "2026-03-30", personKeyword)
        assertEquals(1, matches.size)
        assertEquals(0.95f, matches.first().confidence)
        assertEquals(KeywordMatchType.EXACT_ALIAS, matches.first().matchType)
    }

    @Test
    fun phraseMatching_worksForPlaces() {
        val keyword =
            personKeyword.copy(
                id = "2",
                name = "New York",
                type = KeywordType.PLACE,
                aliases = emptyList()
            )
        val matches = KeywordMatcher.findMatches("I landed in New York last night", 0, "2026-03-30", keyword)
        assertEquals(1, matches.size)
    }

    @Test
    fun substring_doesNotMatchWholeWordRule() {
        val matches = KeywordMatcher.findMatches("Sample text only", 0, "2026-03-30", personKeyword)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun punctuationTolerance_matchesCanonicalWord() {
        val keyword = personKeyword.copy(name = "Mom", aliases = emptyList())
        val matches = KeywordMatcher.findMatches("Mom, called today.", 0, "2026-03-30", keyword)
        assertEquals(1, matches.size)
    }

    @Test
    fun timestampStripping_removesHtmlCommentMetadata() {
        val matches = KeywordMatcher.findMatches("<!--time:09:30--> Sam came over", 0, "2026-03-30", personKeyword)
        assertEquals(1, matches.size)
        assertTrue(matches.first().snippet.contains("Sam"))
    }

    @Test
    fun fuzzyMatch_acceptsSmallEditDistance() {
        val matches = KeywordMatcher.findMatches("Spent time with Samm", 0, "2026-03-30", personKeyword, 0.70f)
        assertEquals(1, matches.size)
        assertEquals(KeywordMatchType.FUZZY_NAME, matches.first().matchType)
    }

    @Test
    fun fuzzyMatch_rejectsLowConfidence() {
        val matches = KeywordMatcher.findMatches("Spent time with Jonathan", 0, "2026-03-30", personKeyword, 0.90f)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun levenshteinDistance_keepsExactDistanceBehavior() {
        assertEquals(3, KeywordMatcher.levenshteinDistance("kitten", "sitting"))
    }

    @Test
    fun levenshteinDistance_withMaxDistanceShortCircuitsWhenImpossible() {
        assertTrue(KeywordMatcher.levenshteinDistance("kitten", "sitting", maxDistance = 1) > 1)
    }

    @Test
    fun multipleMatches_singleEntryReturnsAllExactMatches() {
        val matches = KeywordMatcher.findMatches("Sam met Sammy and later Sam left", 0, "2026-03-30", personKeyword)
        assertEquals(3, matches.size)
    }

    @Test
    fun duplicateAliasOrAliasSameAsName_doesNotDoubleCountSingleVisibleMatch() {
        val keyword = personKeyword.copy(
            aliases = listOf("Sam", "sam", "Sammy", "Sammy")
        )
        val matches = KeywordMatcher.findMatches("Sam was here", 0, "2026-03-30", keyword)
        assertEquals(1, matches.size)
        assertEquals(KeywordMatchType.EXACT_NAME, matches.first().matchType)
    }

    @Test
    fun overlappingAliases_countVisiblePhraseOnlyOnce() {
        val keyword = personKeyword.copy(
            name = "NYC",
            type = KeywordType.PLACE,
            aliases = listOf("York", "New York")
        )
        val matches = KeywordMatcher.findMatches("I visited New York yesterday", 0, "2026-03-30", keyword)
        assertEquals(1, matches.size)
        assertEquals("New York", matches.first().matchedText)
    }

    @Test
    fun fuzzyMultiWordMatch_preservesVisiblePhraseOnce() {
        val keyword = personKeyword.copy(
            name = "New York",
            type = KeywordType.PLACE,
            aliases = emptyList()
        )
        val matches = KeywordMatcher.findMatches("I visited New Yorf yesterday", 0, "2026-03-30", keyword, 0.70f)
        assertEquals(1, matches.size)
        assertEquals("New Yorf", matches.first().matchedText)
    }

    @Test
    fun resolveOverlappingMatches_prefersLongestCrossKeywordPhrase() {
        val matches = listOf(
            KeywordMatch(
                keywordId = "achan",
                date = "2026-03-30",
                entryIndex = 0,
                matchedText = "Achan",
                confidence = 0.95f,
                matchType = KeywordMatchType.EXACT_ALIAS,
                snippet = "Manju's Achan came by",
                startIndex = 8,
                endExclusive = 13
            ),
            KeywordMatch(
                keywordId = "manju",
                date = "2026-03-30",
                entryIndex = 0,
                matchedText = "Manju",
                confidence = 0.95f,
                matchType = KeywordMatchType.EXACT_ALIAS,
                snippet = "Manju's Achan came by",
                startIndex = 0,
                endExclusive = 5
            ),
            KeywordMatch(
                keywordId = "manjus_achan",
                date = "2026-03-30",
                entryIndex = 0,
                matchedText = "Manju's Achan",
                confidence = 0.95f,
                matchType = KeywordMatchType.EXACT_ALIAS,
                snippet = "Manju's Achan came by",
                startIndex = 0,
                endExclusive = 13
            )
        )

        val resolved = KeywordMatcher.resolveOverlappingMatches(matches)

        assertEquals(1, resolved.size)
        assertEquals("manjus_achan", resolved.first().keywordId)
        assertEquals("Manju's Achan", resolved.first().matchedText)
    }

    @Test
    fun disabledKeyword_returnsNoMatches() {
        val disabled = personKeyword.copy(isEnabled = false)
        val matches = KeywordMatcher.findMatches("Sam was here", 0, "2026-03-30", disabled)
        assertTrue(matches.isEmpty())
    }

    @Test
    fun emptyEntry_returnsNoMatches() {
        val matches = KeywordMatcher.findMatches("", 0, "2026-03-30", personKeyword)
        assertTrue(matches.isEmpty())
    }
}
