package com.kieronquinn.app.darq.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.darq.components.settings.DarqSharedPreferences
import com.kieronquinn.app.darq.service.background.DarqPersistentService
import com.kieronquinn.app.darq.service.boot.BootForegroundService
import org.koin.core.context.GlobalContext

class BootReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if(context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = GlobalContext.get().get<DarqSharedPreferences>()
        try {
            if (settings.persistentService) {
                context.startForegroundService(Intent(context, DarqPersistentService::class.java))
            } else {
                context.startForegroundService(Intent(context, BootForegroundService::class.java))
            }
        } catch (e: Exception) {
            android.util.Log.e("BootReceiver", "Failed to start service on boot", e)
        }
    }

}