package com.deepak.flow.core.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.deepak.flow.FlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimezoneChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as FlowApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.reminderRepository.rescheduleAllEnabledReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
