package com.example.bluetoothapp.ble

import android.util.Log
class DeviceRegistry {
    private val devices = mutableMapOf<String, BleDevice>()
    fun addOrUpdate(device: BleDevice) {
        val current = devices[device.address]
        if (current == null) {
            devices[device.address] = device
        } else {
            current.name = device.name
            current.rssi = device.rssi
            current.lastSeen = device.lastSeen
        }
    }

    fun getDevices(): List<BleDevice> {
        return devices.values.toList()
    }

    fun setConnectionState(address: String, state: ConnectionState) {
        Log.d(
            "Registry",
            "address=$address state=$state"
        )
        devices[address]?.connectionState = state
    }
}