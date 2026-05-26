package com.example.blescanner
import androidx.compose.runtime.mutableStateListOf

// スキャン時のFilterはMACアドレスでも、デバイス名でもいけるよう。
// 詳細は以下のリンクを参照。
// - https://developer.android.com/reference/android/bluetooth/le/ScanFilter?_gl=1*1momvtl*_up*MQ..*_ga*MTI1Mjc0Nzg2MC4xNzc5NDI3NzQw*_ga_6HH9YJMN9M*czE3Nzk0Mjc3NDAkbzEkZzAkdDE3Nzk0Mjc3NDAkajYwJGwwJGgxNTMwNDg1NDQz
// 今回は、デバイス名でフィルターをかけることが目的だが、
// 検証対象のデバイスに名前がないことを見越してMACアドレスでもフィルターをかけられるようにしておく。

class DeviceManager {
    val devices = mutableStateListOf<BLEDevice>()
    private var lastUpdateTime = 0L

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
        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < 500) {
            return
        }
        lastUpdateTime = now

        val index = devices.indexOfFirst {
            it.address == device.address
        }
        if (index == -1) {
            devices.add(device)
        } else {
            devices[index] = device
        }
        devices.sortByDescending {
            it.rssi
        }
    }

    fun clear() {
        devices.clear()
    }
}