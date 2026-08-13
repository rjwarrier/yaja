package com.mj.yaja.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchMatcherTest {
    private val appearanceAction =
        SettingsSearchAction.OpenDestination(SettingsDestinationId.APPEARANCE)
    private val languageAction =
        SettingsSearchAction.OpenDestination(SettingsDestinationId.LANGUAGE)

    @Test
    fun search_prefersExactTitleOverPrefixAndKeywordMatches() {
        val entries = listOf(
            entry(SettingsSearchEntryId.COLORS, "Theme Colors", "Appearance", "theme"),
            entry(SettingsSearchEntryId.THEME, "Theme", "Appearance", "dark"),
            entry(SettingsSearchEntryId.FONT, "Font", "Appearance", "theme font")
        )

        val results = SettingsSearchMatcher.search("theme", entries)

        assertEquals(SettingsSearchEntryId.THEME, results.first().id)
        assertEquals(listOf(SettingsSearchEntryId.THEME, SettingsSearchEntryId.COLORS, SettingsSearchEntryId.FONT), results.map { it.id })
    }

    @Test
    fun search_isCaseAndAccentInsensitive() {
        val entries = listOf(
            entry(SettingsSearchEntryId.LANGUAGE, "Language", "Language", "espanol", action = languageAction)
        )

        val results = SettingsSearchMatcher.search("ESPAÑOL", entries)

        assertEquals(SettingsSearchEntryId.LANGUAGE, results.single().id)
    }

    @Test
    fun search_returnsEmptyForBlankQuery() {
        val results = SettingsSearchMatcher.search("   ", listOf(entry(SettingsSearchEntryId.THEME, "Theme", "Appearance")))

        assertTrue(results.isEmpty())
    }

    @Test
    fun registry_hasStableUniqueIdsAndActions() {
        val entries = SettingsSearchRegistry.entries

        assertEquals(entries.size, entries.map { it.id }.toSet().size)
        assertTrue(entries.all { it.keywords.isNotEmpty() })
        assertTrue(entries.all { it.action is SettingsSearchAction.OpenDestination })
    }

    private fun entry(
        id: SettingsSearchEntryId,
        title: String,
        section: String,
        vararg keywords: String,
        action: SettingsSearchAction = appearanceAction
    ) = LocalizedSettingsSearchEntry(
        id = id,
        title = title,
        section = section,
        keywords = keywords.toList(),
        action = action
    )
}
