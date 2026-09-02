package com.deepak.flow.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.deepak.flow.core.gym.GymWorkoutType
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
    data class ReuseReminder(val reminderId: Long) : FlowRoute

    @Serializable
    data class HistoryExpiredReminder(val reminderId: Long) : FlowRoute

    @Serializable
    data object Water : FlowRoute

    @Serializable
    data object Gym : FlowRoute

    @Serializable
    data object GymRoutine : FlowRoute

    @Serializable
    data object GymRoutineCatalog : FlowRoute

    @Serializable
    data class GymRoutineBuilder(val routineId: Long = 0L) : FlowRoute

    @Serializable
    data object GymFreeWorkout : FlowRoute

    @Serializable
    data object GymExerciseLibrary : FlowRoute

    @Serializable
    data object GymRoutineWorkout : FlowRoute

    @Serializable
    data object History : FlowRoute

    @Serializable
    data class HistoryDay(val dateEpochDay: Long) : FlowRoute

    @Serializable
    data class HistoryTasks(val dateEpochDay: Long) : FlowRoute

    @Serializable
    data class HistoryWater(val dateEpochDay: Long) : FlowRoute

    @Serializable
    data class HistoryGym(val dateEpochDay: Long) : FlowRoute

    @Serializable
    data class HistoryGymWorkout(val workoutId: Long) : FlowRoute

    @Serializable
    data class HistoryGymEditExercise(val workoutId: Long, val exerciseId: Long) : FlowRoute

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
    GYM("Gym", isFeature = true),
    HISTORY("History"),
    SETTINGS("Settings"),
    ABOUT("About"),
}

fun FlowDrawerDestination.isEnabled(
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean = true,
): Boolean = when (this) {
    FlowDrawerDestination.REMINDERS -> remindersEnabled
    FlowDrawerDestination.WATER -> waterEnabled
    FlowDrawerDestination.GYM -> gymEnabled
    else -> true
}

/** Feature rows stay openable when off so the recovery screen can be reached. */
fun FlowDrawerDestination.canNavigate(): Boolean = true

fun FlowDrawerDestination.featureChecked(
    remindersEnabled: Boolean,
    waterEnabled: Boolean,
    gymEnabled: Boolean = true,
): Boolean? = when (this) {
    FlowDrawerDestination.REMINDERS -> remindersEnabled
    FlowDrawerDestination.WATER -> waterEnabled
    FlowDrawerDestination.GYM -> gymEnabled
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

/** Open the active workout screen from a notification tap. */
fun NavController.navigateToActiveWorkout(type: GymWorkoutType = GymWorkoutType.FREE) {
    navigate(FlowRoute.Gym) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
    val route = if (type == GymWorkoutType.ROUTINE) {
        FlowRoute.GymRoutineWorkout
    } else {
        FlowRoute.GymFreeWorkout
    }
    navigate(route) {
        launchSingleTop = true
    }
}

fun NavController.navigateToActiveFreeWorkout() {
    navigateToActiveWorkout(GymWorkoutType.FREE)
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
