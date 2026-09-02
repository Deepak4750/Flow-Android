package com.deepak.flow.core.notification

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Whether Flow is the visible foreground app with an interactive display.
 *
 * Uses [ProcessLifecycleOwner] (process-wide activity lifecycle) plus [PowerManager.isInteractive]
 * rather than guessing from a single Activity reference.
 */
object FlowAppVisibility {
    fun isResumed(context: Context): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

    fun isScreenInteractive(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isInteractive
    }

    fun isInteractiveForeground(context: Context): Boolean =
        isResumed(context) && isScreenInteractive(context)
}
