package com.example.bluetoothapp.ble

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

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

object BatteryMonitor {
    var level by mutableStateOf<Int?>(null)

    fun update(value: ByteArray) {
        level = value[0].toInt() and 0xFF
    }
}

data class NotifyLog(
    val timestamp: String,
    val uuid: String,
    val valueHex: String
)

object NotifyLogManager {
    val logs = mutableStateListOf<NotifyLog>()

    fun addLog(uuid: String, value: ByteArray) {
        logs.add(
            NotifyLog(
                timestamp = SimpleDateFormat("HH:mm:ss", Locale.JAPAN).format(Date()),
                uuid = uuid,
                valueHex = value.joinToString(" ") { "%02X".format(it) }
            )
        )
    }

    fun clear() {
        logs.clear()
    }
}