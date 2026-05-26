// 参考にしたURL
// - https://developer.android.com/develop/connectivity/bluetooth/bt-permissions?hl=ja
// - https://developer.android.com/reference/android/bluetooth/BluetoothGattConnectionSettings?_gl=1*198a4a8*_up*MQ..*_ga*Nzk0Njg4NjA1LjE3Nzk3NTcwNjg.*_ga_6HH9YJMN9M*czE3Nzk3NTcwNjgkbzEkZzAkdDE3Nzk3NTcwNjgkajYwJGwwJGg0ODcyMDc0MDE.
// - https://developer.android.com/reference/android/bluetooth/BluetoothGatt?_gl=1*eqrkmx*_up*MQ..*_ga*Nzk0Njg4NjA1LjE3Nzk3NTcwNjg.*_ga_6HH9YJMN9M*czE3Nzk3NjQxNzkkbzIkZzAkdDE3Nzk3NjQxNzkkajYwJGwwJGgxMjg3ODI5ODIw
// - https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback?_gl=1*1jdstr9*_up*MQ..*_ga*Nzk0Njg4NjA1LjE3Nzk3NTcwNjg.*_ga_6HH9YJMN9M*czE3Nzk3NzY0MjIkbzMkZzAkdDE3Nzk3NzY0MjIkajYwJGwwJGgyMDI2ODk1NzE5

package com.example.bleconnector

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.RequiresPermission

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
                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            val connected = device.copy(
                                connectState = ConnectState.CONNECTED
                            )
                            deviceManager.upsert(connected)
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            val disconnected = device.copy(
                                connectState = ConnectState.DISCONNECTED
                            )
                            deviceManager.upsert(disconnected)
                        }
                    }
                }
            }
        )
    }

    // TODO: 未使用
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}