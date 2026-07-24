package com.kieronquinn.app.darq.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.darq.service.autodark.DarqAutoDarkForegroundService

class DarqAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val serviceIntent = Intent(context, DarqAutoDarkForegroundService::class.java).apply {
                if (intent.hasExtra(DarqAutoDarkForegroundService.KEY_ENABLE_DARK)) {
                    putExtra(
                        DarqAutoDarkForegroundService.KEY_ENABLE_DARK,
                        intent.getBooleanExtra(DarqAutoDarkForegroundService.KEY_ENABLE_DARK, false)
                    )
                }
                putExtra(DarqAutoDarkForegroundService.KEY_JUST_RESCHEDULE, false)
            }
            context.startForegroundService(serviceIntent)
        } catch (e: Exception) {
            android.util.Log.e("DarqAlarmReceiver", "Failed to start auto-dark service on alarm trigger", e)
        }
    }

}
