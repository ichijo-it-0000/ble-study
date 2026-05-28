package com.example.bluetoothapp.ble

import android.bluetooth.BluetoothDevice

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    DISCOVERING_SERVICES,
    CONNECTED,
    DISCONNECTING
}

data class BleDevice(
    val bluetoothDevice: BluetoothDevice,
    val address: String,
    var name: String,
    var rssi: Int,
    var lastSeen: Long,
    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
)