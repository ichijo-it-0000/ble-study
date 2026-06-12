package com.example.bluetoothapp.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.bluetoothapp.ble.BleManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class BleService : Service() {
    private lateinit var bleManager: BleManager
    private val binder = LocalBinder()
    private lateinit var shutdownReceiver: ShutdownBroadcastReceiver

    inner class LocalBinder : Binder() {
        fun getService(): BleService = this@BleService
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        Log.d("BleService", "onCreate called")
        
        initializeBle()
        registerShutdownReceiver()
    }

    private fun initializeBle() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
        
        if (bluetoothAdapter != null) {
            bleManager = BleManager(
                context = this,
                bluetoothAdapter = bluetoothAdapter
            )
            Log.d("BleService", "BleManager initialized")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerShutdownReceiver() {
        shutdownReceiver = ShutdownBroadcastReceiver(this)
        val filter = IntentFilter(Intent.ACTION_SHUTDOWN)
        registerReceiver(shutdownReceiver, filter, Context.RECEIVER_EXPORTED)
        Log.d("BleService", "Shutdown receiver registered")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BleService", "onStartCommand called")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun getBleManager(): BleManager = bleManager

    @RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
    fun cleanupBle() {
        if (::bleManager.isInitialized) {
            if (bleManager.getScannerState()) {
                Log.d("BleService", "Stopping scan")
                bleManager.stopScan()
            }
            
            val devices = bleManager.getDevices()

            devices.forEach { device ->
                val state = device.connectionState.toString()
                if (state != "DISCONNECTED") {
                    bleManager.disconnect(device)
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        super.onDestroy()
        Log.d("BleService", "onDestroy called")
        cleanupBle()
        if (::shutdownReceiver.isInitialized) {
            unregisterReceiver(shutdownReceiver)
        }
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("LIFECYCLE", "onTaskRemoved called")
        cleanupBle()
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 500)
    }
}