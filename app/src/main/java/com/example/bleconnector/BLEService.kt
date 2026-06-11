package com.example.bleconnector

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

class BLEService : Service() {
    private val binder = LocalBinder()

    // === Core ===
    val deviceManager = DeviceManager()
    val notifyStore = NotifyStore()
    val writeStore = WriteStore()
    lateinit var scanner: BLEScanner
    lateinit var connector: BLEConnector

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()
        Log.d("BLE_SERVICE", "onCreate called (Service started)")

        // Foreground Service:
        // - システムから重要な実行中プロセスとして扱われるService。
        // - OSによる強制停止を避け、終了処理を実行する時間を確保するために使用する。
        // - 実行中であることを通知(Notification)としてユーザーに明示する必要がある。
        // - Android 14+: manifest側にも同じforegroundServiceTypeの宣言が必須。
        startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)

        // Broadcast:
        // - https://developer.android.com/develop/background-work/background-tasks/broadcasts
        //   - Android 8+ 以降では implicit broadcast の制約があり、manifest登録より context登録（registerReceiver）が推奨される。
        //   - Android 13+（TIRAMISU）以降では exported / not_exported の明示が必須。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                shutdownReceiver,
                IntentFilter(Intent.ACTION_SHUTDOWN),
                RECEIVER_NOT_EXPORTED
            )
        }
        else registerReceiver(shutdownReceiver, IntentFilter(Intent.ACTION_SHUTDOWN))

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        scanner = BLEScanner(
            bluetoothManager.adapter.bluetoothLeScanner
        )

        connector = BLEConnector(
            context = this,
            deviceManager = deviceManager,
            notifyStore = notifyStore,
            writeStore = writeStore
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("BLE_SERVICE", "onBind called")
        return binder
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("BLE_SERVICE", "onTaskRemoved called (app swiped from recents)")
        cleanup()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        Log.d("BLE_SERVICE", "onDestroy (final cleanup)")
        cleanup()
        unregisterReceiver(shutdownReceiver)
        super.onDestroy()
    }

    inner class LocalBinder : Binder() {
        fun getService(): BLEService {
            Log.d("BLE_SERVICE", "getService() accessed from UI")
            return this@BLEService
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun cleanup() {
        val address = deviceManager.devices
            .firstOrNull { it.connectState == ConnectState.CONNECTED }
            ?.address

        if (address != null) {
            connector.disconnect(this, address)
        }
        scanner.stopScanBLEDevices(this)
    }

    // Notification
    // - https://developer.android.com/develop/ui/compose/notifications/create-notification?hl=ja
    private fun createNotification(): Notification {
        val channelId = "ble_channel"
        val channel = NotificationChannel(
            channelId,
            "BLE Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("BLE connecting")
            .setContentText("Keeping the device connected")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()
    }

    private val shutdownReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SHUTDOWN) {
                Log.d("BLE_SERVICE", "ACTION_SHUTDOWN received in service")
                cleanup()
                stopSelf()
            }
        }
    }
}