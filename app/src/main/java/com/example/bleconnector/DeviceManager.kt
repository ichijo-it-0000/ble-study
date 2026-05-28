package com.example.bleconnector
import androidx.compose.runtime.mutableStateListOf

// スキャン時のFilterはMACアドレスでも、デバイス名でもいけるよう。
// 詳細は以下のリンクを参照。
// - https://developer.android.com/reference/android/bluetooth/le/ScanFilter?_gl=1*1momvtl*_up*MQ..*_ga*MTI1Mjc0Nzg2MC4xNzc5NDI3NzQw*_ga_6HH9YJMN9M*czE3Nzk0Mjc3NDAkbzEkZzAkdDE3Nzk0Mjc3NDAkajYwJGwwJGgxNTMwNDg1NDQz
// 今回は、デバイス名でフィルターをかけることが目的だが、
// 検証対象のデバイスに名前がないことを見越してMACアドレスでもフィルターをかけられるようにしておく。

class DeviceManager {
    val devices = mutableStateListOf<BLEDevice>()
    var selectedDevice: BLEDevice? = null

    //fun upsert(device: BLEDevice) {
    //    val index = devices.indexOfFirst {
    //        it.address == device.address
    //    }
    //    if (index == -1) {
    //        devices.add(device)
    //    } else {
    //        devices[index] = device
    //    }
    //}
    fun upsert(device: BLEDevice) {
        val index = devices.indexOfFirst { it.address == device.address }

        if (index == -1) {
            devices.add(device)
            return
        }

        val old = devices[index]

        // 完全一致なら更新しない（軽量化）
        if (old == device) return

        devices[index] = device
    }

    fun clear() {
        devices.clear()
    }
}