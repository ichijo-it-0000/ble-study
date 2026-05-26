package com.example.bluetoothapp.ble

data class BleDevice(
    val address: String,
    var name: String,
    var rssi: Int,
    var lastSeen: Long
)