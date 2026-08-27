package com.deepak.flow.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.serialization.Serializable

sealed interface FlowRoute {
    @Serializable
    data object Home : FlowRoute

    @Serializable
    data object Reminders : FlowRoute

    @Serializable
    data object CreateReminder : FlowRoute

    @Serializable
    data class EditReminder(val reminderId: Long) : FlowRoute

    @Serializable
    data object Water : FlowRoute

    @Serializable
    data object Gym : FlowRoute

    @Serializable
    data object GymNewRoutine : FlowRoute

    @Serializable
    data object GymFreeWorkout : FlowRoute

    @Serializable
    data object History : FlowRoute

    @Serializable
    data class HistoryDay(val dateEpochDay: Long) : FlowRoute

    @Serializable
    data class HistoryTasks(val dateEpochDay: Long) : FlowRoute

    @Serializable
    data class HistoryWater(val dateEpochDay: Long) : FlowRoute

    @Serializable
    data object Settings : FlowRoute

    @Serializable
    data object About : FlowRoute
}

/** Destinations reachable from the navigation drawer, in display order. */
enum class FlowDrawerDestination(
    val label: String,
    val isFeature: Boolean = false,
) {
    HOME("Home"),
    REMINDERS("Tasks", isFeature = true),
    WATER("H₂O", isFeature = true),
    GYM("Gym"),
    HISTORY("History"),
    SETTINGS("Settings"),
    ABOUT("About"),
}

fun FlowDrawerDestination.isEnabled(
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
): Boolean = when (this) {
    FlowDrawerDestination.REMINDERS -> remindersEnabled
    FlowDrawerDestination.WATER -> waterEnabled
    else -> true
}

fun FlowDrawerDestination.featureChecked(
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
): Boolean? = when (this) {
    FlowDrawerDestination.REMINDERS -> remindersEnabled
    FlowDrawerDestination.WATER -> waterEnabled
    else -> null
}

fun NavController.navigateFromDrawer(destination: FlowDrawerDestination) {
    when (destination) {
        FlowDrawerDestination.HOME -> popBackStack<FlowRoute.Home>(inclusive = false)
        FlowDrawerDestination.REMINDERS -> navigateDrawerRoute(FlowRoute.Reminders)
        FlowDrawerDestination.WATER -> navigateDrawerRoute(FlowRoute.Water)
        FlowDrawerDestination.GYM -> navigateDrawerRoute(FlowRoute.Gym)
        FlowDrawerDestination.HISTORY -> navigateDrawerRoute(FlowRoute.History)
        FlowDrawerDestination.SETTINGS -> navigate(FlowRoute.Settings)
        FlowDrawerDestination.ABOUT -> navigate(FlowRoute.About)
    }
}

private fun NavController.navigateDrawerRoute(route: FlowRoute) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
