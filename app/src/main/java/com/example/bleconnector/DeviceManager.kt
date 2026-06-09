package com.example.bleconnector
import android.util.Log
import androidx.compose.runtime.mutableStateListOf

// スキャン時のFilterはMACアドレスでも、デバイス名でもいけるよう。
// 詳細は以下のリンクを参照。
// - https://developer.android.com/reference/android/bluetooth/le/ScanFilter?_gl=1*1momvtl*_up*MQ..*_ga*MTI1Mjc0Nzg2MC4xNzc5NDI3NzQw*_ga_6HH9YJMN9M*czE3Nzk0Mjc3NDAkbzEkZzAkdDE3Nzk0Mjc3NDAkajYwJGwwJGgxNTMwNDg1NDQz
// 今回は、デバイス名でフィルターをかけることが目的だが、
// 検証対象のデバイスに名前がないことを見越してMACアドレスでもフィルターをかけられるようにしておく。

class DeviceManager {
    val devices = mutableStateListOf<BLEDevice>()
    var selectedDevice: BLEDevice? = null

    fun upsert(
        address: String,
        name: String?,
        rssi: Int
    ) {
        val index = devices.indexOfFirst { it.address == address }
        if (index == -1) {
            devices.add(
                BLEDevice(
                    name = name ?: "Unknown",
                    address = address,
                    rssi = rssi,
                    connectState = ConnectState.DISCONNECTED
                )
            )
            return
        }

        val old = devices[index]
        devices[index] = old.copy(
            name = name ?: old.name,
            rssi = rssi
        )
    }

    fun updateConnectState(
        address: String,
        state: ConnectState
    ) {
        update(address) {old -> old.copy(connectState = state)}
    }

    fun updateService(
        address: String,
        services: List<BLEServiceInfo>
    ) {
        update(address) {old -> old.copy(services = services)}
    }

    fun update(
        address: String,
        block: (BLEDevice) -> BLEDevice
    ) {
        Log.d("DEVICE_DEBUG", "updateConnectionState $address -> $devices.state")
        val index = devices.indexOfFirst { it.address == address }
        Log.d("DEVICE_DEBUG", "index=$index")
        if (index == -1) return

        val old = devices[index]
        devices[index] = block(old)
    }

    fun clear() {
        devices.clear()
    }
}