// 参考にしたURL
// - https://developer.android.com/develop/connectivity/bluetooth/bt-permissions?hl=ja
// - https://developer.android.com/reference/android/bluetooth/BluetoothGattConnectionSettings?_gl=1*198a4a8*_up*MQ..*_ga*Nzk0Njg4NjA1LjE3Nzk3NTcwNjg.*_ga_6HH9YJMN9M*czE3Nzk3NTcwNjgkbzEkZzAkdDE3Nzk3NTcwNjgkajYwJGwwJGg0ODcyMDc0MDE.
// - https://developer.android.com/reference/android/bluetooth/BluetoothGatt?_gl=1*eqrkmx*_up*MQ..*_ga*Nzk0Njg4NjA1LjE3Nzk3NTcwNjg.*_ga_6HH9YJMN9M*czE3Nzk3NjQxNzkkbzIkZzAkdDE3Nzk3NjQxNzkkajYwJGwwJGgxMjg3ODI5ODIw
// - https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback?_gl=1*1jdstr9*_up*MQ..*_ga*Nzk0Njg4NjA1LjE3Nzk3NTcwNjg.*_ga_6HH9YJMN9M*czE3Nzk3NzY0MjIkbzMkZzAkdDE3Nzk3NzY0MjIkajYwJGwwJGgyMDI2ODk1NzE5
// - https://developer.android.com/develop/connectivity/bluetooth/ble/connect-gatt-server?hl=ja
// - https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback?_gl=1*fql9ki*_up*MQ..*_ga*MTg4ODUwNDE0MS4xNzc5ODU2NjI5*_ga_6HH9YJMN9M*czE3Nzk4NTY2MjkkbzEkZzAkdDE3Nzk4NTc0NjUkajYwJGwwJGgxNjExNDQ1OTAw#onServicesDiscovered(android.bluetooth.BluetoothGatt,%20int)

// ■ BLE接続フロー
// 1. Advertising受信（Scan）
// 2. ScanResult -> BluetoothDevice取得
// 3. GATT Client接続確立（connectGatt）
// 4. GATT Connection State Callback受信
// 5. GATT Service Discovery（discoverServices）
// 6. GATT Characteristic Read / Write / Notification

package com.example.bleconnector

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat

class BLEConnector(
    private val context: Context,
    private val deviceManager: DeviceManager
) {
    private var bluetoothGatt: BluetoothGatt? = null

    private companion object {
        const val TAG = "BLE"
        // CCCD(Client Characteristic Configuration Descriptor)
        //   - UUID = 0x2902
        //   - Notify/Indicateの有効・無効を切り替えるDescriptor
        const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    }

     // Descriptor書き込み要求
     // Android BLEはGATT Operationを並列実行できない。
     // writeDescriptor()完了を待って次を送る必要があるため、Queueで管理する。
    private class DescriptorWriteRequest(
        val descriptor: BluetoothGattDescriptor,
        val value: ByteArray
    )
    private val pendingDescriptorWrites = ArrayDeque<DescriptorWriteRequest>()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BLEDevice) {
        val connecting = device.copy(
            connectState = ConnectState.CONNECTING
        )
        deviceManager.upsert(connecting)
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        val adapter = bluetoothManager.adapter
        val remoteDevice = adapter.getRemoteDevice(device.address)

        bluetoothGatt = remoteDevice.connectGatt(
            context,
            false,
            object : BluetoothGattCallback() {
                // https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback?_gl=1*1jdstr9*_up*MQ..*_ga*Nzk0Njg4NjA1LjE3Nzk3NTcwNjg.*_ga_6HH9YJMN9M*czE3Nzk3NzY0MjIkbzMkZzAkdDE3Nzk3NzY0MjIkajYwJGwwJGgyMDI2ODk1NzE5
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {

                    Log.d("BLE", "STATE CHANGE: $newState status=$status")
                    Log.d("BLE", "ADDRESS=${device.address}")
                    Log.d("BLE", "THREAD=${Thread.currentThread().name}")

                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        Log.e("BLE", "Connection failed status=$status")
                        disconnect(context)
                        return
                    }

                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            Log.d("BLE", "CONNECTED")
                            val connected = device.copy(
                                connectState = ConnectState.CONNECTED
                            )
                            deviceManager.upsert(connected)
                            gatt.discoverServices()
                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.d("BLE", "DISCONNECTED")
                            deviceManager.upsert(
                                device.copy(connectState = ConnectState.DISCONNECTED)
                            )
                            disconnect(context)
                        }
                    }
                }

                override fun onServicesDiscovered(
                    gatt: BluetoothGatt,
                    status: Int
                ) {
                    if (status != BluetoothGatt.GATT_SUCCESS) return
                    val services = gatt.services.map { service ->
                        BLEServiceInfo(
                            uuid = service.uuid.toString(),
                            characteristics = service.characteristics.map { ch ->
                                BLECharacteristicInfo(
                                    uuid = ch.uuid.toString(),
                                    properties = buildString {
                                        append("R=${ch.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0}, ")
                                        append("W=${ch.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0}, ")
                                        append("N=${ch.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0}")
                                    },
                                    descriptors = ch.descriptors.map { it.uuid.toString() }
                                )
                            }
                        )
                    }

                    val updated = device.copy(
                        connectState = ConnectState.CONNECTED,
                        services = services
                    )

                    deviceManager.upsert(updated)

                    // Permission check.
                    if (
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    enableNotify(gatt)
                }

                private fun handleNotify(uuid: String, value: ByteArray?) {
                    Log.d(TAG, "Notify uuid=$uuid value=${value?.joinToString()}")
                }
                // コールバック：リモートデバイスからCharacteristic通知を受信したときに呼ばれる。
                // https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback?_gl=1*1pt425o*_up*MQ..*_ga*NzA3NzMxMDYyLjE3ODAzNzQxMTI.*_ga_6HH9YJMN9M*czE3ODAzNzQxMTEkbzEkZzAkdDE3ODAzNzQxMTEkajYwJGwwJGg4MjM1MTY2Nzg.#onCharacteristicChanged(android.bluetooth.BluetoothGatt,%20android.bluetooth.BluetoothGattCharacteristic,%20byte[])
                // APIレベル32以下
                @Deprecated("Deprecated in Java")
                @Suppress("DEPRECATION")
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    handleNotify(characteristic.uuid.toString(), characteristic.value)
                }

                // APIレベル33以上
                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray
                ) {
                    handleNotify(characteristic.uuid.toString(), value)
                }
            }
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect(context: Context) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableNotify(gatt: BluetoothGatt) {
        pendingDescriptorWrites.clear()
        // BLEデバイスのGATTからNotify対応の通信チャネルだけ抜き出す
        val notifyCharacteristics = gatt.services
            .flatMap { it.characteristics }
            .filter { it.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 }
        Log.d(
            TAG,
            "Notify characteristic count=${notifyCharacteristics.size}"
        )
        notifyCharacteristics.forEach { characteristic ->
            Log.d(
                TAG,
                "Notify characteristic=${characteristic.uuid}"
            )
            // 特定のcharacteristicに対してNotifyやIndicateを有効/無効にする。
            // https://developer.android.com/develop/connectivity/bluetooth/ble/transfer-ble-data?hl=ja#notification
            // https://developer.android.com/reference/android/bluetooth/BluetoothGatt?_gl=1*1lcs5j6*_up*MQ..*_ga*NzY2NTk0NzU2LjE3ODAzNzM2OTU.*_ga_6HH9YJMN9M*czE3ODAzNzM2OTQkbzEkZzAkdDE3ODAzNzM2OTQkajYwJGwwJGgxNDc1OTEwNTI5#setCharacteristicNotification(android.bluetooth.BluetoothGattCharacteristic,%20boolean)
            gatt.setCharacteristicNotification(
                characteristic,
                true    // 今回は有効化
            )
            // Characteristic配下のCCCD検索
            val cccd = characteristic.descriptors.firstOrNull {
                it.uuid.toString().equals(
                    CCCD_UUID,
                    ignoreCase = true
                )
            }
            if (cccd == null) {
                Log.w(
                    TAG,
                    "CCCD not found: ${characteristic.uuid}"
                )
                return@forEach
            }

            pendingDescriptorWrites.addLast(
                DescriptorWriteRequest(
                    descriptor = cccd,
                    // https://developer.android.com/reference/android/bluetooth/BluetoothGattDescriptor#ENABLE_NOTIFICATION_VALUE
                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                )
            )
        }
        // Android BLEはGATT Operationを並列実行できないため、書き込み完了を待って次を送る。
        writeNextDescriptor(gatt)
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun writeNextDescriptor(gatt: BluetoothGatt) {
        val request =
            pendingDescriptorWrites.removeFirstOrNull()
                ?: return
        Log.d(
            TAG,
            "Write CCCD=${request.descriptor.uuid}"
        )

        // 指定されたDescriptorの値を関連するリモートデバイスに書き込む。
        // https://developer.android.com/reference/android/bluetooth/BluetoothGatt?_gl=1*romksb*_up*MQ..*_ga*NzA3NzMxMDYyLjE3ODAzNzQxMTI.*_ga_6HH9YJMN9M*czE3ODAzNzQxMTEkbzEkZzAkdDE3ODAzNzQxMTEkajYwJGwwJGg4MjM1MTY2Nzg.#writeDescriptor(android.bluetooth.BluetoothGattDescriptor)
        // Android 13(API33)以降
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(
                request.descriptor,
                request.value
            )
        // Android 12以前
        } else {
            @Suppress("DEPRECATION")
            request.descriptor.value = request.value

            @Suppress("DEPRECATION")
            gatt.writeDescriptor(
                request.descriptor
            )
        }
    }
}