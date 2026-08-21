package com.deepak.flow.core.notification

import android.content.Context

/**
 * Calm, rotating bodies for the H₂O drink reminder.
 * Title stays H₂O; only the line under it changes each post.
 */
internal object WaterNotificationCopy {
    private const val PREFS = "flow_water_notification"
    private const val KEY_BODY_INDEX = "body_index"

    val Bodies = listOf(
        "A little water when you can.",
        "Time for a sip.",
        "Drink when you're ready.",
        "A glass would help.",
        "Hydrate when you can.",
        "Water helps. Take a moment.",
        "A sip counts. Come back to it.",
        "Stay with yourself. Drink a little.",
    )

    fun nextBody(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val index = prefs.getInt(KEY_BODY_INDEX, 0).coerceIn(0, Bodies.lastIndex)
        val body = Bodies[index]
        prefs.edit().putInt(KEY_BODY_INDEX, (index + 1) % Bodies.size).apply()
        return body
    }
}
