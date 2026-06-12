package com.example.bluetoothapp.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission

class ShutdownBroadcastReceiver(private val service: BleService) : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SHUTDOWN) {
            Log.d("ShutdownReceiver", "ACTION_SHUTDOWN received")
            service.cleanupBle()
            Handler(Looper.getMainLooper()).postDelayed({
                service.stopSelf()
            }, 500)
        }
    }
}