package com.deepak.flow.app.navigation

import kotlinx.serialization.Serializable

sealed interface FlowRoute {
    @Serializable
    data object Home : FlowRoute

    @Serializable
    data object CreateReminder : FlowRoute

    @Serializable
    data class EditReminder(val reminderId: Long) : FlowRoute

    @Serializable
    data object Settings : FlowRoute

    @Serializable
    data object About : FlowRoute
}

/** Destinations reachable from the navigation drawer, in display order. */
enum class FlowDrawerDestination(val label: String) {
    REMINDERS("Reminders"),
    SETTINGS("Settings"),
    ABOUT("About"),
}
