package com.linqi.triplog.remind

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.linqi.triplog.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 开机后重建所有未过期的提醒。 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = AppContainer.of(context)
                val trips = container.tripRepository.getAllSnapshot()
                ReminderScheduler.rescheduleAll(context, trips)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
