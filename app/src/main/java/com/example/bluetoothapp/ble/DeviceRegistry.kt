package com.example.bluetoothapp.ble

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
}