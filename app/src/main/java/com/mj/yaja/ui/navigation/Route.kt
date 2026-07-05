package com.mj.yaja.ui.navigation

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Calendar : Route("calendar")
    data object Timeline : Route("timeline")
    data object Lookback : Route("lookback")
    data object Review : Route("review/{period}") {
        const val ARG_PERIOD = "period"
        fun create(period: String) = "review/$period"
    }
    data object Statistics : Route("statistics")
    data object Keywords : Route("keywords")
    data object AddEntry : Route("add_entry")
    data object Onboarding : Route("onboarding")
    data object Settings : Route("settings")
    data object Appearance : Route("appearance")
    data object RebuildCache : Route("rebuild_cache")
    data object VersionHistory : Route("version_history")
    data object VersionSnapshots : Route("version_snapshots")
    data object TaskerIntegration : Route("tasker_integration")
    data object ComplianceMaster : Route("compliance_master")
    data object AppLog : Route("app_log")
    data object Shortcodes : Route("shortcodes")
    data object Todos : Route("todos")
    data object Help : Route("help")
    data object PinLock : Route("pin_lock")
    data object PinSetup : Route("pin_setup")
    data object PinDisable : Route("pin_disable")
    data object KeywordDetail : Route("keyword_detail/{keywordId}") {
        const val ARG_KEYWORD_ID = "keywordId"
        fun create(keywordId: String) = "keyword_detail/$keywordId"
    }

    companion object {
        val topLevel: Set<String> by lazy { setOf(Home.path, Calendar.path, Lookback.path, Statistics.path) }
    }
}
