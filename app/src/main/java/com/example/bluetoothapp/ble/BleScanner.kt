package com.example.bluetoothapp.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import androidx.annotation.RequiresPermission

class BleScanner(
    private val bluetoothAdapter: BluetoothAdapter,
    private val onDeviceFound: (BleDevice) -> Unit
) {
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var scanning = false
    @SuppressLint("MissingPermission")
    fun startScan() {
        if (scanning) {
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanning = true
        bluetoothLeScanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!scanning) {
            return
        }
        if (bluetoothAdapter.isEnabled) {
            bluetoothLeScanner.stopScan(scanCallback)
        }
        scanning = false
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val bleDevice = BleDevice(
                address = result.device.address,
                name = result.device.name ?: "UNKNOWN",
                rssi = result.rssi,
                lastSeen = System.currentTimeMillis()
            )
            onDeviceFound(bleDevice)
        }
    }
}