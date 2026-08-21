package com.deepak.flow.core.notification

import android.content.Context

internal object WaterNotificationIntents {
    const val ACTION_ADD = "com.deepak.flow.action.WATER_ADD"
    const val ACTION_BUSY = "com.deepak.flow.action.WATER_BUSY"
    const val ACTION_RESTORE = "com.deepak.flow.action.WATER_RESTORE"
    const val EXTRA_AMOUNT_ML = "extra_water_notification_ml"
}

internal const val WATER_NOTIFICATION_FILL_ML = 1000
internal const val WATER_NOTIFICATION_GUARD_ID = 0x6C6F7702L

/** Add amounts shown on the drink notification (Android caps actions at three with Dismiss). */
internal val WaterNotificationAddAmountsMl = intArrayOf(250, 500)

internal fun waterNotificationIsFilled(sessionMl: Int): Boolean =
    sessionMl >= WATER_NOTIFICATION_FILL_ML

internal object WaterNotificationSession {
    private const val PREFS = "flow_water_notification"
    private const val KEY_ML = "session_ml"

    fun reset(context: Context) {
        prefs(context).edit().putInt(KEY_ML, 0).apply()
    }

    fun millilitres(context: Context): Int =
        prefs(context).getInt(KEY_ML, 0).coerceAtLeast(0)

    fun add(context: Context, amount: Int): Int {
        val next = (millilitres(context) + amount).coerceAtLeast(0)
        prefs(context).edit().putInt(KEY_ML, next).apply()
        return next
    }

    fun isFilled(context: Context): Boolean =
        waterNotificationIsFilled(millilitres(context))

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
