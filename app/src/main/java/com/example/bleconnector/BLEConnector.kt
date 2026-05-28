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
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat

class BLEConnector(
    private val context: Context,
    private val deviceManager: DeviceManager
) {
    private var bluetoothGatt: BluetoothGatt? = null

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

    // GATT hierarchy
    // └ Service
    //   └ Characteristic
    //     └ Descriptor

    // 1. Service 探索 (discoverServices(), onServicesDiscovered())

    // 2. Characteristic 探索 ()

    // 3. UUID 取得
    // UUIDはServiceごと、Characteristicごと、Descriptorごと、、、に割り当てられている。
    // どのように取得するのがいいのか。。。？
    // 確認が必要。



}