package com.example.bluetoothapp.ble

import android.bluetooth.BluetoothAdapter

class BleManager(
    bluetoothAdapter: BluetoothAdapter,
    private val onDevicesUpdated: () -> Unit
) {
    private val registry = DeviceRegistry()
    private val scanner = BleScanner(
        bluetoothAdapter = bluetoothAdapter,
        onDeviceFound = { device ->
            registry.addOrUpdate(device)
            onDevicesUpdated()
        }
    )

    fun startScan() {
        scanner.startScan()
    }

    fun stopScan() {
        scanner.stopScan()
    }

    fun getDevices(): List<BleDevice> {
        return registry.getDevices()
    }

}