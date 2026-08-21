package com.deepak.flow.core.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.deepak.flow.FlowApplication
import com.deepak.flow.core.model.remindersFeatureEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as FlowApplication

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (app.profileRepository.getProfile().remindersFeatureEnabled()) {
                    app.reminderRepository.rescheduleAllEnabledReminders()
                } else {
                    app.reminderRepository.cancelAllScheduledReminders()
                }
                app.notificationScheduler.syncWaterReminder(app.profileRepository.getProfile())
                com.deepak.flow.core.widget.FlowWidgets.refresh(app)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
