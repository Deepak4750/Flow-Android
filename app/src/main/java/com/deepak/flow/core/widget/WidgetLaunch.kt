package com.deepak.flow.core.widget

import android.content.Intent

/** Deep-link style extras so home-screen widgets open the matching Flow page. */
object WidgetLaunch {
    const val EXTRA_DESTINATION = "com.deepak.flow.widget.DESTINATION"
    const val DEST_WATER = "water"
    const val DEST_REMINDERS = "reminders"
    const val DEST_GYM_FREE_WORKOUT = "gym_free_workout"
}

fun Intent.putWidgetDestination(destination: String): Intent =
    putExtra(WidgetLaunch.EXTRA_DESTINATION, destination)

fun Intent.widgetDestinationOrNull(): String? =
    getStringExtra(WidgetLaunch.EXTRA_DESTINATION)?.takeIf { it.isNotBlank() }
