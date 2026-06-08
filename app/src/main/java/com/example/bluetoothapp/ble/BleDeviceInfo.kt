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

enum class LogType {
    READ,
    WRITE,
    NOTIFY
}

data class BleLog(
    val timestamp: String,
    val uuid: String,
    val type: LogType,
    val value: ByteArray
)

object BleLogManager {
    val logs = mutableStateListOf<BleLog>()

    fun add(
        type: LogType,
        uuid: String,
        value: ByteArray
    ) {
        logs.add(
            BleLog(
                timestamp = SimpleDateFormat(
                    "HH:mm:ss",
                    Locale.JAPAN
                ).format(Date()),
                uuid = uuid,
                type = type,
                value = value.copyOf()
            )
        )
    }

    fun clear() {
        logs.clear()
    }
}