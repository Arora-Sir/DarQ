package com.kieronquinn.app.darq.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.darq.service.autodark.DarqAutoDarkForegroundService

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED
        ) {
            try {
                context.startForegroundService(Intent(context, DarqAutoDarkForegroundService::class.java).apply {
                    putExtra(DarqAutoDarkForegroundService.KEY_JUST_RESCHEDULE, true)
                })
            } catch (e: Exception) {
                android.util.Log.e("TimeChangeReceiver", "Failed to reschedule auto-dark service on time change", e)
            }
        }
    }

}
