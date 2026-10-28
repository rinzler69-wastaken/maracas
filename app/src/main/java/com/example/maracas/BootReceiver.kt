package com.example.maracas

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (c.getSharedPreferences("p", Context.MODE_PRIVATE).getBoolean("enabled", false))
            c.startForegroundService(Intent(c, WakeService::class.java))
    }
}
