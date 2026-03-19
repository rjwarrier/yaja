package com.mj.yaja.ui.navigation

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Calendar : Route("calendar")
    data object Lookback : Route("lookback")
    data object AddEntry : Route("add_entry")
    data object Settings : Route("settings")
    data object Shortcodes : Route("shortcodes")
    data object Help : Route("help")
    data object PinLock : Route("pin_lock")
    data object PinSetup : Route("pin_setup")
    data object PinDisable : Route("pin_disable")

    companion object {
        val topLevel: Set<String> = setOf(Home.path, Calendar.path, Lookback.path)
    }
}
