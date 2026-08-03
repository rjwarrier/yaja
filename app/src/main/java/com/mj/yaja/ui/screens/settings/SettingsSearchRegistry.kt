package com.mj.yaja.ui.screens

import androidx.annotation.StringRes
import com.mj.yaja.R
import java.text.Normalizer
import java.util.Locale

internal enum class SettingsSearchEntryId {
    THEME,
    COLORS,
    PERSONAL_THEMES,
    FONT,
    FONT_SIZE,
    LANGUAGE,
    SHOW_TIMESTAMPS,
    ALLOW_FUTURE_ENTRIES,
    CARRY_FORWARD_TODOS,
    SHOW_DAY_HEADER_COUNTS,
    RENDER_CHECKBOXES_AS_TEXT,
    TRUNCATE_LONG_ENTRIES,
    FIRST_DAY_OF_WEEK,
    DATE_ORDER,
    ENTRY_STYLE,
    ANIMATIONS,
    DATE_KEYWORDS,
    NAVIGATION_MODE,
    LOOKBACK,
    PEOPLE_PLACES_NAV,
    TODOS,
    STATISTICS,
    SWIPE_TO_NAVIGATE_DATES,
    ENABLE_DRAG_REORDER,
    ENTRY_DELETE,
    POST_WRITE_REVIEW,
    PEOPLE_PLACES_HIGHLIGHTING,
    MATCH_SENSITIVITY,
    PRIVACY_SECURITY,
    PIN,
    BIOMETRIC,
    AUTO_LOCK,
    HIDE_TEXT_MODE,
    PRIVACY_DASHBOARD,
    DATA_RECOVERY,
    STORAGE_LOCATION,
    BACKUP,
    RESTORE_BACKUP,
    IMPORT,
    REBUILD_CACHE,
    VERSION_HISTORY,
    TASKER,
    SHORTCODES,
    HELP_ABOUT,
    APP_LOG
}

internal enum class SettingsDestinationId {
    APPEARANCE,
    JOURNAL_EXPERIENCE,
    NAVIGATION_GESTURES,
    PRIVACY_SECURITY,
    PRIVACY_DASHBOARD,
    DATA_RECOVERY
}

internal enum class SettingsSearchAnchor {
    LANGUAGE,
    REVIEW_INSIGHTS,
    ADVANCED_INTEGRATIONS,
    HELP_ABOUT
}

internal sealed interface SettingsSearchAction {
    data class OpenDestination(val destination: SettingsDestinationId) : SettingsSearchAction
    data class ScrollTo(val anchor: SettingsSearchAnchor) : SettingsSearchAction
}

internal data class SettingsSearchEntry(
    val id: SettingsSearchEntryId,
    @param:StringRes val titleRes: Int,
    @param:StringRes val sectionRes: Int,
    val keywords: List<String>,
    val action: SettingsSearchAction
)

internal data class LocalizedSettingsSearchEntry(
    val id: SettingsSearchEntryId,
    val title: String,
    val section: String,
    val keywords: List<String>,
    val action: SettingsSearchAction
)

internal object SettingsSearchRegistry {
    val entries: List<SettingsSearchEntry> = listOf(
        entry(SettingsSearchEntryId.THEME, R.string.settings_theme, R.string.settings_appearance, "light", "dark", "amoled", "system", destination = SettingsDestinationId.APPEARANCE),
        entry(SettingsSearchEntryId.COLORS, R.string.settings_colors_label, R.string.settings_appearance, "material you", "custom", "palette", destination = SettingsDestinationId.APPEARANCE),
        entry(SettingsSearchEntryId.PERSONAL_THEMES, R.string.settings_personal_themes_title, R.string.settings_appearance, "personal", "theme slots", "generated accents", destination = SettingsDestinationId.APPEARANCE),
        entry(SettingsSearchEntryId.FONT, R.string.settings_font, R.string.settings_appearance, "sans", "serif", "mono", destination = SettingsDestinationId.APPEARANCE),
        entry(SettingsSearchEntryId.FONT_SIZE, R.string.settings_font_size_label, R.string.settings_appearance, "text size", "scale", destination = SettingsDestinationId.APPEARANCE),
        entry(SettingsSearchEntryId.LANGUAGE, R.string.settings_language, R.string.settings_language, "espanol", "portugues", "francais", "translate", "locale", anchor = SettingsSearchAnchor.LANGUAGE),
        entry(SettingsSearchEntryId.SHOW_TIMESTAMPS, R.string.settings_journal_show_timestamps_title, R.string.settings_section_journal_experience, "time", "timeline", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.ALLOW_FUTURE_ENTRIES, R.string.settings_journal_allow_future_title, R.string.settings_section_journal_experience, "future dates", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.CARRY_FORWARD_TODOS, R.string.settings_journal_carry_forward_title, R.string.settings_section_journal_experience, "unchecked tasks", "yesterday todo", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.SHOW_DAY_HEADER_COUNTS, R.string.settings_journal_day_header_counts_title, R.string.settings_section_journal_experience, "header stats", "counts", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.RENDER_CHECKBOXES_AS_TEXT, R.string.settings_journal_render_checkboxes_title, R.string.settings_section_journal_experience, "todo checkbox", "text checkbox", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.TRUNCATE_LONG_ENTRIES, R.string.settings_journal_truncate_title, R.string.settings_section_journal_experience, "preview", "character limit", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.FIRST_DAY_OF_WEEK, R.string.settings_journal_first_day_title, R.string.settings_section_journal_experience, "calendar", "sunday", "monday", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.DATE_ORDER, R.string.settings_journal_date_order_title, R.string.settings_section_journal_experience, "dd/mm", "mm/dd", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.ENTRY_STYLE, R.string.settings_journal_entry_style_title, R.string.settings_section_journal_experience, "cards", "flat", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.ANIMATIONS, R.string.settings_journal_animations_title, R.string.settings_section_journal_experience, "motion", "reduced", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.DATE_KEYWORDS, R.string.settings_journal_date_keywords_title, R.string.settings_section_journal_experience, "keywords", "today", "tomorrow", destination = SettingsDestinationId.JOURNAL_EXPERIENCE),
        entry(SettingsSearchEntryId.NAVIGATION_MODE, R.string.settings_navigation_mode_title, R.string.settings_section_navigation_gestures, "floating", "panel", "bottom panel", destination = SettingsDestinationId.NAVIGATION_GESTURES),
        entry(SettingsSearchEntryId.LOOKBACK, R.string.nav_lookback, R.string.settings_section_navigation_gestures, "nav bar", destination = SettingsDestinationId.NAVIGATION_GESTURES),
        entry(SettingsSearchEntryId.PEOPLE_PLACES_NAV, R.string.settings_review_people_places, R.string.settings_section_navigation_gestures, "keywords", "nav bar", destination = SettingsDestinationId.NAVIGATION_GESTURES),
        entry(SettingsSearchEntryId.TODOS, R.string.settings_nav_todos_title, R.string.settings_section_navigation_gestures, "nav bar", destination = SettingsDestinationId.NAVIGATION_GESTURES),
        entry(SettingsSearchEntryId.STATISTICS, R.string.settings_nav_statistics_title, R.string.settings_section_navigation_gestures, "nav bar", destination = SettingsDestinationId.NAVIGATION_GESTURES),
        entry(SettingsSearchEntryId.SWIPE_TO_NAVIGATE_DATES, R.string.settings_swipe_navigate_title, R.string.settings_section_navigation_gestures, "gestures", "swipe", destination = SettingsDestinationId.NAVIGATION_GESTURES),
        entry(SettingsSearchEntryId.ENABLE_DRAG_REORDER, R.string.settings_drag_reorder_title, R.string.settings_section_navigation_gestures, "drag", "reorder", destination = SettingsDestinationId.NAVIGATION_GESTURES),
        entry(SettingsSearchEntryId.ENTRY_DELETE, R.string.settings_entry_delete_title, R.string.settings_section_navigation_gestures, "selection", "delete", destination = SettingsDestinationId.NAVIGATION_GESTURES),
        entry(SettingsSearchEntryId.POST_WRITE_REVIEW, R.string.settings_review_post_write_title, R.string.settings_section_review_insights, "save sheet", "review", anchor = SettingsSearchAnchor.REVIEW_INSIGHTS),
        entry(SettingsSearchEntryId.PEOPLE_PLACES_HIGHLIGHTING, R.string.settings_review_highlighting_title, R.string.settings_section_review_insights, "highlighting", "people places", anchor = SettingsSearchAnchor.REVIEW_INSIGHTS),
        entry(SettingsSearchEntryId.MATCH_SENSITIVITY, R.string.settings_review_match_sensitivity_title, R.string.settings_section_review_insights, "keyword matching", "fuzzy", anchor = SettingsSearchAnchor.REVIEW_INSIGHTS),
        entry(SettingsSearchEntryId.PRIVACY_SECURITY, R.string.settings_privacy_security_title, R.string.settings_privacy_security_title, "pin", "biometric", "lock", destination = SettingsDestinationId.PRIVACY_SECURITY),
        entry(SettingsSearchEntryId.PIN, R.string.settings_pin_lock_title, R.string.settings_privacy_security_title, "password", "lock", destination = SettingsDestinationId.PRIVACY_SECURITY),
        entry(SettingsSearchEntryId.BIOMETRIC, R.string.settings_biometric_unlock_title, R.string.settings_privacy_security_title, "fingerprint", "face unlock", destination = SettingsDestinationId.PRIVACY_SECURITY),
        entry(SettingsSearchEntryId.AUTO_LOCK, R.string.settings_autolock_timeout_title, R.string.settings_privacy_security_title, "timeout", destination = SettingsDestinationId.PRIVACY_SECURITY),
        entry(SettingsSearchEntryId.HIDE_TEXT_MODE, R.string.settings_hide_text_mode_title, R.string.settings_privacy_security_title, "privacy", "panic blur", "hide text", destination = SettingsDestinationId.PRIVACY_SECURITY),
        entry(SettingsSearchEntryId.PRIVACY_DASHBOARD, R.string.settings_privacy_dashboard_title, R.string.settings_privacy_security_title, "transparency", "data dashboard", "widgets", "tasker", destination = SettingsDestinationId.PRIVACY_DASHBOARD),
        entry(SettingsSearchEntryId.DATA_RECOVERY, R.string.settings_data_recovery_title, R.string.settings_data_recovery_title, "backup", "restore", "storage", destination = SettingsDestinationId.DATA_RECOVERY),
        entry(SettingsSearchEntryId.STORAGE_LOCATION, R.string.settings_storage_location_label, R.string.settings_data_recovery_title, "folder", "storage", destination = SettingsDestinationId.DATA_RECOVERY),
        entry(SettingsSearchEntryId.BACKUP, R.string.settings_backup, R.string.settings_data_recovery_title, "backup now", "backup reminder", destination = SettingsDestinationId.DATA_RECOVERY),
        entry(SettingsSearchEntryId.RESTORE_BACKUP, R.string.settings_restore_button, R.string.settings_data_recovery_title, "restore zip", destination = SettingsDestinationId.DATA_RECOVERY),
        entry(SettingsSearchEntryId.IMPORT, R.string.settings_import_label, R.string.settings_data_recovery_title, "day one", "journalistic", destination = SettingsDestinationId.DATA_RECOVERY),
        entry(SettingsSearchEntryId.REBUILD_CACHE, R.string.rebuild_tools_title, R.string.settings_data_recovery_title, "refresh cache", "rebuild", destination = SettingsDestinationId.DATA_RECOVERY),
        entry(SettingsSearchEntryId.VERSION_HISTORY, R.string.settings_version_history_title, R.string.settings_data_recovery_title, "snapshots", "history", destination = SettingsDestinationId.DATA_RECOVERY),
        entry(SettingsSearchEntryId.TASKER, R.string.settings_tasker_integration_title, R.string.settings_advanced_integrations_title, "tasker integration", "automation", anchor = SettingsSearchAnchor.ADVANCED_INTEGRATIONS),
        entry(SettingsSearchEntryId.SHORTCODES, R.string.shortcodes_title, R.string.settings_advanced_integrations_title, "snippets", "text expansion", anchor = SettingsSearchAnchor.ADVANCED_INTEGRATIONS),
        entry(SettingsSearchEntryId.HELP_ABOUT, R.string.nav_help, R.string.nav_help, "help", "about", "faq", anchor = SettingsSearchAnchor.HELP_ABOUT),
        entry(SettingsSearchEntryId.APP_LOG, R.string.settings_app_log_title, R.string.nav_help, "logs", "crash", anchor = SettingsSearchAnchor.HELP_ABOUT)
    )

    private fun entry(
        id: SettingsSearchEntryId,
        @StringRes titleRes: Int,
        @StringRes sectionRes: Int,
        vararg keywords: String,
        destination: SettingsDestinationId? = null,
        anchor: SettingsSearchAnchor? = null
    ): SettingsSearchEntry {
        val action = when {
            destination != null -> SettingsSearchAction.OpenDestination(destination)
            anchor != null -> SettingsSearchAction.ScrollTo(anchor)
            else -> error("Settings search entry $id needs an action")
        }
        return SettingsSearchEntry(
            id = id,
            titleRes = titleRes,
            sectionRes = sectionRes,
            keywords = keywords.toList(),
            action = action
        )
    }
}

internal object SettingsSearchMatcher {
    fun search(
        query: String,
        entries: List<LocalizedSettingsSearchEntry>
    ): List<LocalizedSettingsSearchEntry> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) {
            return emptyList()
        }
        return entries
            .mapNotNull { entry ->
                val rank = entry.matchRank(normalizedQuery) ?: return@mapNotNull null
                rank to entry
            }
            .sortedWith(
                compareBy<Pair<Int, LocalizedSettingsSearchEntry>> { it.first }
                    .thenBy { it.second.title.lowercase(Locale.US) }
            )
            .map { it.second }
    }

    private fun LocalizedSettingsSearchEntry.matchRank(query: String): Int? {
        val title = normalize(title)
        val section = normalize(section)
        val keywords = keywords.map(::normalize)
        return when {
            title == query -> 0
            title.startsWith(query) -> 1
            section == query -> 2
            section.startsWith(query) -> 3
            keywords.any { it == query } -> 4
            keywords.any { it.startsWith(query) } -> 5
            title.contains(query) -> 6
            section.contains(query) -> 7
            keywords.any { it.contains(query) } -> 8
            else -> null
        }
    }

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.US)
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
